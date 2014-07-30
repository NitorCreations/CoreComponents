package com.nitorcreations.deployer;

import java.io.File;
import java.util.Properties;

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

public class DependencyLauncher extends AbstractLauncher implements LaunchMethod {
	public static final String PROPERTY_KEY_PREFIX_JAVA_ARGS = "deployer.java.arg";
	public static final String PROPERTY_KEY_LOCAL_REPOSITORY = "deployer.local.repository";
	public static final String PROPERTY_KEY_REMOTE_REPOSITORY = "deployer.remote.repository";
	public static final String PROPERTY_KEY_RESOLVE_TRANSITIVE = "deployer.artifact.transitive";
	String artifactCoords;
	private String localRepo;
	private boolean transitive = false;
	private String statUri;
	private String remoteRepo;
	
	@Override
	public void run() {
		File rootJar;
		Dependency dependency = new Dependency( new DefaultArtifact( artifactCoords ), "runtime" );
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
			}  catch (DependencyResolutionException | DependencyCollectionException e) {
				throw new RuntimeException("Failed to resolve (transitively) " + artifactCoords, e);
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
				throw new RuntimeException("Failed to resolve " + artifactCoords, e);
			}
		}
		launchArgs.add("-Daccesslog.websocket=" + statUri.toString());
		launchArgs.add("-jar");
		launchArgs.add(rootJar.getAbsolutePath());
		addLauncherArgs(launchProperties, PROPERTY_KEY_PREFIX_LAUNCH_ARGS);
		launch(extraEnv, getLaunchArgs());
	}

	@Override
	public void setProperties(Properties properties) {
		super.setProperties(properties);
		artifactCoords = properties.getProperty("launch.artifact");
		localRepo = properties.getProperty(PROPERTY_KEY_LOCAL_REPOSITORY, System.getProperty("user.home") + File.separator + ".deployer" + File.separator + "repository");
		remoteRepo = properties.getProperty(PROPERTY_KEY_REMOTE_REPOSITORY, "http://localhost:5120/maven");
		transitive = Boolean.valueOf(properties.getProperty(PROPERTY_KEY_RESOLVE_TRANSITIVE, "false"));
		File javaBin = new File(new File(System.getProperty("java.home")), "bin");
		File java = null;
		if (System.getProperty("os.name").toLowerCase().contains("win")) {
			java = new File(javaBin, "java.exe");
		} else {
			java = new File(javaBin, "java");
		}
		launchArgs.add(java.getAbsolutePath());
		addLauncherArgs(properties, PROPERTY_KEY_PREFIX_JAVA_ARGS);
	}
}
