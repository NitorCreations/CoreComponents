package com.nitorcreations.deployer;

import java.io.File;

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
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;

public class Main {
	public static void main(String[] args) {
		Dependency dependency = new Dependency( new DefaultArtifact( args[0] ), "runtime" );
		String remoteRepo = System.getProperty("deployer.remote.repository", "http://localhost:5210/maven");
		String localRepo = System.getProperty("deployer.local.repository", System.getProperty("user.home") + File.separator + ".repository");
		RepositorySystem system = GuiceRepositorySystemFactory.newRepositorySystem();
		LocalRepository local = new LocalRepository(localRepo);
		DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
		session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, local));
		RemoteRepository remote = new RemoteRepository.Builder("deployer" , "default", remoteRepo).build();

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
	        node.accept( nlg );
	        System.out.println( nlg.getClassPath() );
		} catch (DependencyResolutionException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (DependencyCollectionException e1) {
			e1.printStackTrace();
			System.exit(1);
		}
 
	}
}
