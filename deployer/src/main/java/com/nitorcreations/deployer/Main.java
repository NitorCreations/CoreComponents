package com.nitorcreations.deployer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionHandlerFilter;

@WebSocket
public class Main {
	private static String remoteRepo;
	private static String localRepo;
	static {
		remoteRepo = System.getProperty("deployer.remote.repository", "http://localhost:5120/maven");
		localRepo = System.getProperty("deployer.local.repository", System.getProperty("user.home") + File.separator + ".deployer" + File.separator + "repository");
		File libDir = new File(new File(localRepo), "lib");
		System.setProperty("java.library.path", libDir.getAbsolutePath());
		extractNativeLib(libDir);
	}

	private final URI statUri;
	private final CountDownLatch closeLatch = new CountDownLatch(1);

	@Option( name="-t", usage="Download transitive dependencies" )
	private boolean transitive = false;
	
	@Option(name="-m", handler=LaunchMethodHandler.class, usage="Launch method" )
	private LaunchMethod method = LaunchMethod.UBERJAR;
	
	@Argument
    private List<String> arguments = new ArrayList<String>();
	private File rootJar;
	private Session wsSession;

	public Main() throws URISyntaxException {
		URI repoUri = new URI(remoteRepo);
		statUri = new URI("ws", null, repoUri.getHost(), repoUri.getPort(), "/statistics", null, null);
	}
	public static void main(String[] args) throws URISyntaxException {
		new Main().doMain(args);
	}

	public void doMain(String[] args) {
		CmdLineParser parser = new CmdLineParser(this);
		parser.setUsageWidth(80);

		try {
			parser.parseArgument(args);
			if( arguments.isEmpty() )
				throw new CmdLineException(parser,"No argument is given");
		} catch( CmdLineException e ) {
			System.err.println(e.getMessage());
			System.err.println("java SampleMain [options...] arguments...");
			parser.printUsage(System.err);
			System.err.println();
			System.err.println(" Example: java SampleMain"+parser.printExample(OptionHandlerFilter.ALL));
			return;
		}
		Dependency dependency = new Dependency( new DefaultArtifact( arguments.get(0) ), "runtime" );
		RepositorySystem system = GuiceRepositorySystemFactory.newRepositorySystem();
		LocalRepository local = new LocalRepository(localRepo);
		DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
		session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, local));
		RemoteRepository remote = new RemoteRepository.Builder("deployer" , "default", remoteRepo).build();
		if (transitive) {
			CollectRequest collectRequest = new CollectRequest();
			collectRequest.setRoot( dependency );
			collectRequest.addRepository( remote );
			DependencyNode node;
			try {
				node = system.collectDependencies( session, collectRequest ).getRoot();
				DependencyRequest dependencyRequest = new DependencyRequest();
				dependencyRequest.setRoot( node );
				system.resolveDependencies( session, dependencyRequest  );
				PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
				rootJar = node.getArtifact().getFile();
				node.accept( nlg );
			}  catch (DependencyResolutionException e) {
				e.printStackTrace();
				System.exit(1);
			} catch (DependencyCollectionException e1) {
				e1.printStackTrace();
				System.exit(1);
			}
		} else {
			ArtifactRequest req = new ArtifactRequest();
			req.setArtifact(dependency.getArtifact());
			req.addRepository(remote);
			try {
				ArtifactResult result = system.resolveArtifact(session, req);
				rootJar = result.getArtifact().getFile();
				System.out.println(rootJar.getAbsolutePath());
			} catch (ArtifactResolutionException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
        WebSocketClient client = new WebSocketClient();
        try {
            client.start();
            ClientUpgradeRequest request = new ClientUpgradeRequest();
            client.connect(this, statUri, request);
            System.out.printf("Connecting to : %s%n", statUri);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        synchronized (this) {
        	while (wsSession == null) {
        		try {
					this.wait(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
        	}
        }
		StatsSender statsSender;
		try {
	        statsSender = new StatsSender(wsSession);
			Thread t = new Thread(statsSender);
			t.start();
			File java = new File(new File(new File(System.getProperty("java.home")), "bin"), "java");
			ProcessBuilder pb = new ProcessBuilder(java.getAbsolutePath(), "-jar", rootJar.getAbsolutePath() );
			pb.environment().putAll(System.getenv());
			Process p = pb.start();
			StreamLinePumper stdout = new StreamLinePumper(p.getInputStream(), wsSession, "STDOUT");
			StreamLinePumper stderr = new StreamLinePumper(p.getErrorStream(), wsSession, "STDERR");
			new Thread(stdout, "stdout").start();
			new Thread(stderr, "stderr").start();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}
	
	private static void extractNativeLib(File libDir) {
		String arch = System.getProperty("os.arch");
		String os = System.getProperty("os.name").toLowerCase();
		//libsigar-amd64-linux-1.6.4.so
		String libInJarName = "libsigar-" + arch + "-" + os + "-1.6.4.so";
		String libName = "libsigar-" + arch + "-" + os + ".so";
		File libFile = new File(libDir, libName);
		if (!(libFile.exists() && libFile.canExecute())) {
			InputStream lib = Main.class.getClassLoader().getResourceAsStream(libInJarName);
			libDir.mkdirs();
			if (lib != null) {
				try (OutputStream out = new FileOutputStream(libFile)) {
					byte[] buffer = new byte[1024];
					int len;
					while ((len = lib.read(buffer)) != -1) {
						out.write(buffer, 0, len);
					}
					libFile.setExecutable(true, false);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					try {
						lib.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} else {
				throw new RuntimeException("Failed to find " + libName);
			}
		}
	}
    @OnWebSocketConnect
    public void onConnect(Session session) {
    	synchronized (this) {
            System.out.printf("Got connect: %s%n", session);
            this.wsSession = session;
            this.notifyAll();
		}
    }
 
    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        System.out.printf("Connection closed: %d - %s%n", statusCode, reason);
        this.wsSession = null;
        this.closeLatch.countDown();
    }
	public Session getSession() {
		return wsSession;
	}

}
