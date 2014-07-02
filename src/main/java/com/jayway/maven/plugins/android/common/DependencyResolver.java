package com.jayway.maven.plugins.android.common;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.codehaus.plexus.logging.Logger;

import java.util.HashSet;
import java.util.Set;

import static com.jayway.maven.plugins.android.common.AndroidExtension.AAR;
import static com.jayway.maven.plugins.android.common.AndroidExtension.APK;
import static com.jayway.maven.plugins.android.common.AndroidExtension.APKLIB;

/**
 * Resolves the aar and apklib dependencies for an Artifact.
 *
 * @author William Ferguson <william.ferguson@xandar.com.au>
 */
public final class DependencyResolver
{
    private final Logger log;
    private final DependencyGraphBuilder dependencyGraphBuilder;

    public DependencyResolver( Logger log, DependencyGraphBuilder dependencyGraphBuilder )
    {
        this.log = log;
        this.dependencyGraphBuilder = dependencyGraphBuilder;
    }

    /**
     * @param project   MavenProject for which to return the dependencies.
     * @param session   MavenSession in which to look for reactor dependencies.
     * @return all the dependencies for a project.
     * @throws DependencyGraphBuilderException if the dependency graph can't be built.
     */
    public Set<Artifact> getProjectDependenciesFor( MavenProject project, MavenSession session )
            throws DependencyGraphBuilderException
    {
        // No need to filter our search. We want to resolve all artifacts.
        final DependencyNode node = dependencyGraphBuilder.buildDependencyGraph( project, null, session.getProjects() );

        final DependencyCollector collector = new DependencyCollector( log, project.getArtifact() );
        collector.visit( node, false );
        return collector.getDependencies();
    }

    /**
     * Returns the Set of APKLIB, AAR, APK (direct or transitive) dependencies of the supplied artifact.
     *
     * The project is searched until artifact is found and then the library dependencies are looked for recursively.
     *
     * @param session           MavenSession in which to resolve the artifacts.
     * @param repositorySystem  RepositorySystem with which to resolve the artifacts.
     * @param artifact          Artifact for whom to get the dependencies.
     * @return Set of APK, APKLIB and AAR dependencies.
     * @throws org.apache.maven.plugin.MojoExecutionException if it couldn't resolve any of the dependencies.
     */
    public Set<Artifact> getLibraryDependenciesFor( MavenSession session,
                                                    RepositorySystem repositorySystem,
                                                    Artifact artifact )
            throws MojoExecutionException
    {
        // Set a filter that should only return interesting artifacts.
        final ArtifactFilter filter = new ArtifactFilter()
        {
            @Override
            public boolean include( Artifact found )
            {
                final String type = found.getType();
                return ( type.equals( APKLIB ) || type.equals( AAR ) || type.equals( APK ) );
            }
        };

        log.debug( "MavenSession = " + session + "  repositorySystem = " + repositorySystem );

        final ArtifactResolutionRequest request = new ArtifactResolutionRequest();
        request.setArtifact( artifact );
        request.setResolveRoot( false );        // Don't include source artifact in result
        request.setResolveTransitively( true ); // Include direct plus transitive dependencies.
        request.setServers( session.getRequest().getServers() );
        request.setMirrors( session.getRequest().getMirrors() );
        request.setProxies( session.getRequest().getProxies() );
        request.setLocalRepository( session.getLocalRepository() );
        request.setRemoteRepositories( session.getRequest().getRemoteRepositories() );

        final ArtifactResolutionResult result = repositorySystem.resolve( request );

        final Set<Artifact> libraryDeps = new HashSet<Artifact>();
        for ( final Artifact depArtifact : result.getArtifacts() )
        {
            if ( filter.include( depArtifact ) )
            {
                libraryDeps.add( depArtifact );
            }
        }

        return libraryDeps;
    }
}
