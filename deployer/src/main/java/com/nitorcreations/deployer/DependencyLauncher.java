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
	public static final String PROPERTY_KEY_RESOLVE_TRANSITIVE = "deployer.artifact.transitive";
	public static final String PROPERTY_KEY_MAIN_CLASS = "deployer.launch.mainclass";
	String artifactCoords;
	private String localRepo;
	private boolean transitive = false;
	private String statUri;
	private String remoteRepo;
	private AetherDownloader downloader;
	private String mainClass;
	
	@Override
	public void run() {
		File rootJar;
		String classpath;
		Dependency dependency = new Dependency( new DefaultArtifact( artifactCoords ), "runtime" );
		RepositorySystem system = GuiceRepositorySystemFactory.newRepositorySystem();
		LocalRepository local = new LocalRepository(localRepo);
		DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
		session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, local));
		RemoteRepository remote = new RemoteRepository.Builder("deployer" , "default", remoteRepo).build();
		if (transitive) {
			classpath = downloader.downloadTransitive(artifactCoords);
			rootJar = new File(classpath.split(File.pathSeparator)[0]);
		} else {
			rootJar = downloader.downloadArtifact(artifactCoords);
			classpath = rootJar.getAbsolutePath();
		}
		launchArgs.add("-Daccesslog.websocket=" + statUri.toString());
		if (transitive || !mainClass.isEmpty()) {
			launchArgs.add("-cp");
			launchArgs.add(classpath);
			launchArgs.add(mainClass);
		} else {
			launchArgs.add("-jar");
			launchArgs.add(rootJar.getAbsolutePath());
		}
		addLauncherArgs(launchProperties, PROPERTY_KEY_PREFIX_LAUNCH_ARGS);
		launch(extraEnv, getLaunchArgs());
	}

	@Override
	public void setProperties(Properties properties) {
		super.setProperties(properties);
		downloader = new AetherDownloader();
		downloader.setProperties(properties);
		artifactCoords = properties.getProperty("launch.artifact");
		transitive = Boolean.valueOf(properties.getProperty(PROPERTY_KEY_RESOLVE_TRANSITIVE, "false"));
		mainClass = properties.getProperty(PROPERTY_KEY_MAIN_CLASS, "");
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
