/*
 * Copyright (C) 2009 Jayway AB
 * Copyright (C) 2007-2008 JVending Masa
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jayway.maven.plugins.android.phase_prebuild;

import com.jayway.maven.plugins.android.common.BuildHelper;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.DefaultDependencyResolutionRequest;
import org.apache.maven.project.DefaultProjectDependenciesResolver;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Test using a MavenLifecycleParticipant to alter the compile classpath.
 */
@Component( role = AbstractMavenLifecycleParticipant.class, hint = "AarMavenLifecycleListenerTest" )
public final class AarMavenLifecycleParticipantTest extends AbstractMavenLifecycleParticipant
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
     * @component
     * @readonly
     * @required
     */
    protected ProjectDependenciesResolver projectDependenciesResolver;

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

    //@Requirement
    //private BundleReader bundleReader;

    //@Requirement
    //private TychoDependencyResolver resolver;

    //@Requirement
    private PlexusContainer plexus;

    @SuppressWarnings( "unused" )
    @Requirement
    private Logger log;

    @Override
    public void afterProjectsRead( MavenSession session ) throws MavenExecutionException
    {
        log.info( "" );
        log.info( "AMLP---TEST--- afterProjectsRead" );
        log.info( "" );

        final BuildHelper helper = new BuildHelper(
                repoSystem, repoSession,
                projectRepos, combinedAssets,
                projectHelper,
                unpackedLibsDirectory, unpackedLibClassesDirectory,
                log );

        log.info( "RepoSystem=" + repoSystem );
        log.info( "RepoSystemSession=" + repoSession );

        log.info( "CurrentProject=" + session.getCurrentProject() );
        final List<MavenProject> projects = session.getProjects();
        for ( MavenProject project : projects )
        {
            log.info( "project=" + project.getArtifact() );
            log.info( "project#artifacts=" + project.getArtifacts() );

            log.info( "projectDependenciesResolver=" + projectDependenciesResolver );

            //Old Resolver
            //final ProjectDependenciesResolver resolver = new DefaultProjectDependenciesResolver();
            //final Set<Artifact> resolvedArtifacts = resolver.resolve( project, scopes, session );
            //log.info( "project#artifacts2=" + resolvedArtifacts );

            final DefaultDependencyResolutionRequest dependencyResolutionRequest =
                    new DefaultDependencyResolutionRequest( project, repoSession );
            final DependencyResolutionResult dependencyResolutionResult;

            try
            {
                final ProjectDependenciesResolver resolver = new DefaultProjectDependenciesResolver();
                dependencyResolutionResult = resolver.resolve( dependencyResolutionRequest );
            }
            catch ( DependencyResolutionException e )
            {
                throw new MavenExecutionException( e.getMessage(), e );
            }

            final Set artifacts = new LinkedHashSet();
            if ( dependencyResolutionResult.getDependencyGraph() != null )
            {
                final List<DependencyNode> deps = dependencyResolutionResult.getDependencyGraph().getChildren();
                if ( !deps.isEmpty() )
                {
                    RepositoryUtils.toArtifacts( artifacts,
                            deps,
                            Collections.singletonList( project.getArtifact().getId() ), null );
                }
            }
            log.info( "artifacts2=" + artifacts );
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
}
