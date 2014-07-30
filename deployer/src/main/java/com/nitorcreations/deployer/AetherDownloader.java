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

public class AetherDownloader {
	public static final String PROPERTY_KEY_LOCAL_REPOSITORY = "deployer.local.repository";
	public static final String PROPERTY_KEY_REMOTE_REPOSITORY = "deployer.remote.repository";
	private String localRepo;
	private String remoteRepo;
	private LocalRepository local;
	private RemoteRepository remote;
	private RepositorySystem system;

	public File downloadArtifact(String artifactCoords) {
		Dependency dependency = new Dependency( new DefaultArtifact( artifactCoords ), "runtime" );
		DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
		session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, local));
		ArtifactRequest req = new ArtifactRequest();
		req.setArtifact(dependency.getArtifact());
		req.addRepository(remote);
		File rootJar = null;
		try {
			ArtifactResult result = system.resolveArtifact(session, req);
			rootJar = result.getArtifact().getFile();
		} catch (ArtifactResolutionException e) {
			throw new RuntimeException("Failed to resolve " + artifactCoords, e);
		}
		return rootJar;
	}

	public String downloadTransitive(String artifactCoords) {
		Dependency dependency = new Dependency( new DefaultArtifact( artifactCoords ), "runtime" );
		CollectRequest collectRequest = new CollectRequest();
		collectRequest.setRoot( dependency );
		collectRequest.addRepository( remote );
		DependencyNode node;
		DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
		session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, local));
		try {
			node = system.collectDependencies( session, collectRequest ).getRoot();
			DependencyRequest dependencyRequest = new DependencyRequest();
			dependencyRequest.setRoot( node );
			system.resolveDependencies( session, dependencyRequest  );
			PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
			node.accept( nlg );
			return nlg.getClassPath();
		}  catch (DependencyResolutionException | DependencyCollectionException e) {
			throw new RuntimeException("Failed to resolve (transitively) " + artifactCoords, e);
		}
	}

	public void setProperties(Properties properties) {
		localRepo = properties.getProperty(PROPERTY_KEY_LOCAL_REPOSITORY, System.getProperty("user.home") + File.separator + ".deployer" + File.separator + "repository");
		remoteRepo = properties.getProperty(PROPERTY_KEY_REMOTE_REPOSITORY, "http://localhost:5120/maven");
		system = GuiceRepositorySystemFactory.newRepositorySystem();
		local = new LocalRepository(localRepo);
		remote = new RemoteRepository.Builder("deployer" , "default", remoteRepo).build();
	}
}
