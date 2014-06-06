package com.nitorcreations.deployer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

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

import sun.jvmstat.monitor.Monitor;
import sun.jvmstat.monitor.MonitoredHost;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.VmIdentifier;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

@WebSocket
public class Main {
    private static final String LOCAL_CONNECTOR_ADDRESS_PROP = "com.sun.management.jmxremote.localConnectorAddress";

	private static String remoteRepo;
	private static String localRepo;
	private Logger log = Logger.getLogger(this.getClass().getCanonicalName());
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

	private int pid;

	public Main() throws URISyntaxException {
		URI repoUri = new URI(remoteRepo);
		String path = "/statistics";
		if (System.getenv("INSTANCE_ID") != null) {
			path += "/" + System.getenv("INSTANCE_ID");
		}
		statUri = new URI("ws", null, repoUri.getHost(), repoUri.getPort(), path, null, null);
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
			File java = new File(new File(new File(System.getProperty("java.home")), "bin"), "java");
			ProcessBuilder pb = new ProcessBuilder(java.getAbsolutePath(),
					"-Daccesslog.websocket=" + statUri.toString(), "-jar", rootJar.getAbsolutePath() );
			pb.environment().putAll(System.getenv());
			pb.environment().put("DEV", "1");
			System.out.printf("Starting %s%n", pb.command().toString());
			Process p = pb.start();
			StreamLinePumper stdout = new StreamLinePumper(p.getInputStream(), wsSession, "STDOUT");
			StreamLinePumper stderr = new StreamLinePumper(p.getErrorStream(), wsSession, "STDERR");
			new Thread(stdout, "stdout").start();
			new Thread(stderr, "stderr").start();
			Thread.sleep(5000);
			statsSender = new StatsSender(wsSession, getMBeanServerConnection(), pid);
			Thread t = new Thread(statsSender);
			t.start();
		} catch (Exception e) {
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
	
    public MBeanServerConnection getMBeanServerConnection() throws Exception {
        String host = null;
        MonitoredHost monitoredHost = MonitoredHost.getMonitoredHost(host);

        Set<Integer> vms = monitoredHost.activeVms();
        for (Integer next : vms) {
            int lvmid = next.intValue();
            log.fine("Inspecting VM " + lvmid);
            MonitoredVm vm = null;
            String vmidString = "//" + lvmid + "?mode=r";
            try {
                VmIdentifier id = new VmIdentifier(vmidString);
                vm = monitoredHost.getMonitoredVm(id, 0);
            } catch (URISyntaxException e) {
                // Should be able to generate valid URLs
                assert false;
            } catch (Exception e) {
            } finally {
                if (vm == null) {
                	log.fine("Could note get Monitor for VM " + lvmid);
                	continue;
                }
            }
            
            Monitor classPath = vm.findByName("java.property.java.class.path");
            String classPathValue = classPath.getValue().toString()
                    .split(File.pathSeparator)[0];

            log.finer("Classpath for VM " + lvmid + ": " + classPathValue);

            if (classPathValue.contains(rootJar.getName())
                && classPathValue.contains(localRepo)) {
            	pid = lvmid;
                log.finer("VM " + lvmid + " is a our vm");
                Monitor command = vm.findByName("sun.rt.javaCommand");
                String lcCommandStr = command.getValue().toString()
                        .toLowerCase();

                log.finer("Command for beanserver VM " + lvmid + ": " + lcCommandStr);
                
                try {
                	VirtualMachine attachedVm = VirtualMachine
                			.attach("" + lvmid);
                	String home = attachedVm.getSystemProperties()
                			.getProperty("java.home");

                	// Normally in ${java.home}/jre/lib/management-agent.jar but might
                	// be in ${java.home}/lib in build environments.
                	File f = Paths.get(home, "jre", "lib", "management-agent.jar").toFile();
                	if (!f.exists()) {
                    	f = Paths.get(home,  "lib", "management-agent.jar").toFile();
                		if (!f.exists()) {
                			throw new IOException("Management agent not found");
                		}
                	}

                	String agent = f.getCanonicalPath();
                	log.finer("Found agent for VM " + lvmid + ": " + agent);
                	try {
                		attachedVm
                		.loadAgent(agent,
                				"com.sun.management.jmxremote");
                	} catch (AgentLoadException x) {
                		IOException ioe = new IOException(x
                				.getMessage());
                		ioe.initCause(x);
                		throw ioe;
                	} catch (AgentInitializationException x) {
                		IOException ioe = new IOException(x
                				.getMessage());
                		ioe.initCause(x);
                		throw ioe;
                	}
                	Properties agentProps = attachedVm
                			.getAgentProperties();
                	String address = (String) agentProps
                			.get(LOCAL_CONNECTOR_ADDRESS_PROP);
                	JMXServiceURL url = new JMXServiceURL(address);
                	JMXConnector jmxc = JMXConnectorFactory
                			.connect(url, null);
                	MBeanServerConnection mbsc = jmxc
                			.getMBeanServerConnection();
                	vm.detach();                            
                	return mbsc;
                } catch (AttachNotSupportedException x) {
                	log.log(Level.WARNING,
                			"Not attachable", x);
                } catch (IOException x) {
                	log.log(Level.WARNING,
                			"Failed to get JMX connection", x);
                }
            }
        }
        return null;
    }

}
