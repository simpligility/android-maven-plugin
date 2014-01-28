/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package com.jayway.maven.plugins.android.phase_prebuild;

/*
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.osgitools.BundleReader;
import org.eclipse.tycho.core.osgitools.DefaultBundleReader;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.resolver.TychoDependencyResolver;
*/

import com.jayway.maven.plugins.android.common.BuildHelper;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

import java.io.File;
import java.util.List;

/**
 * Injects into life cycle
 */
//@Component( role = AbstractMavenLifecycleParticipant.class, hint = "AarMavenLifecycleListener" )
public final class AarMavenLifecycleParticipant extends AbstractMavenLifecycleParticipant
{

    /**
     * The entry point to Aether, i.e. the component doing all the work.
     *
     * @component
     */
    protected RepositorySystem repoSystem;

    /**
     * The current repository/network configuration of Maven.
     *
     * @parameter default-value="${repositorySystemSession}"
     * @readonly
     */
    protected RepositorySystemSession repoSession;

    /**
     * The project's remote repositories to use for the resolution of project dependencies.
     *
     * @parameter default-value="${project.remoteProjectRepositories}"
     * @readonly
     */
    protected List<RemoteRepository> projectRepos;

    /**
     * The combined assets directory. This will contain both the assets found in "assets" as well as any assets
     * contained in a apksources, apklib or aar dependencies.
     *
     * @parameter expression="${project.build.directory}/generated-sources/combined-assets"
     * @readonly
     */
    protected File combinedAssets;

    /**
     * Maven ProjectHelper.
     *
     * @component
     * @readonly
     */
    protected MavenProjectHelper projectHelper;

    /**
     * Extract the library (aar and apklib) dependencies here.
     *
     * @parameter expression="${project.build.directory}/unpacked-libs"
     * @readonly
     */
    protected File unpackedLibsDirectory;

    /**
     * Contains folders for each of the Android dependent libraries.
     * Each folder contains the unpacked classes for that library
     *
     * @parameter expression="${project.build.directory}/unpacked-lib-classes"
     * @readonly
     */
    protected File unpackedLibClassesDirectory;

    @SuppressWarnings( "unused" )
    @Requirement
    private Logger log;

    @Override
    public void afterProjectsRead( MavenSession session ) throws MavenExecutionException
    {
        log.info( "" );
        log.info( "AMLP afterProjectsRead" );
        log.info( "AMLP afterProjectsRead" );
        log.info( "AMLP afterProjectsRead" );
        log.info( "" );

        final BuildHelper helper = new BuildHelper(
                repoSystem, repoSession,
                projectRepos, combinedAssets,
                projectHelper,
                unpackedLibsDirectory, unpackedLibClassesDirectory,
                log );

        log.info( "CurrentProject=" + session.getCurrentProject() );
        final List<MavenProject> projects = session.getProjects();
        for ( MavenProject project : projects )
        {
            log.info( "project=" + project.getArtifact() );
            // If we extracted dep aar and dep apklibs here will that help/work
            try
            {
                log.info( "project#artifacts=" + project.getArtifacts() );
                helper.extractLibraryDependencies( project );

                // For each dependency:
                    // TODO If Aar dep then add extracted classes to compile classpath
                    // TODO If Apklib/ApkSources dep then add apklib source to compile source rot
            }
            catch ( MojoExecutionException e )
            {
                throw new MavenExecutionException( "Could not extract libraries for : " + project.getArtifact(), e );
            }
        }

/*
        List<MavenProject> projects = session.getProjects();
        for (MavenProject project : projects) {
            resolver.setupProject(session, project, DefaultReactorProject.adapt(project));
        }

        List<ReactorProject> reactorProjects = DefaultReactorProject.adapt(session);
        for (MavenProject project : projects) {
            resolver.resolveProject(session, project, reactorProjects);
        }
*/
    }

 /*
    private void configureComponents(MavenSession session) {
        // TODO why does the bundle reader need to cache stuff in the local maven repository?
        File localRepository = new File(session.getLocalRepository().getBasedir());
        ((DefaultBundleReader) bundleReader).setLocationRepository(localRepository);
    }
*/
}
