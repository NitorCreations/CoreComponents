package com.nitorcreations.deployer;

import org.eclipse.aether.RepositorySystem;

import com.google.inject.Guice;

public class GuiceRepositorySystemFactory {

    public static RepositorySystem newRepositorySystem()
    {
        return Guice.createInjector( new DeployerAetherModule() ).getInstance( RepositorySystem.class );
    }

}