package com.jayway.maven.plugins.android.common;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.codehaus.plexus.logging.Logger;

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
     * @throws MojoExecutionException if the dependency graph can't be built.
     */
    public Set<Artifact> getProjectDependenciesFor( MavenProject project, MavenSession session )
            throws MojoExecutionException
    {
        final DependencyNode node;
        try
        {
            // No need to filter our search. We want to resolve all artifacts.
            node = dependencyGraphBuilder.buildDependencyGraph( project, null, session.getProjects() );
        }
        catch ( DependencyGraphBuilderException e )
        {
            throw new MojoExecutionException( "Could not resolve project dependency graph", e );
        }

        final DependencyCollector collector = new DependencyCollector( log, project.getArtifact() );
        collector.visit( node, false );
        return collector.getDependencies();
    }

    /**
     * Returns the Set of APKLIB, AAR, APK (direct or transitive) dependencies of the supplied artifact.
     *
     * The project is searched until artifact is found and then the library dependencies are looked for recursively.
     *
     * @param project   MavenProject that contains the artifact.
     * @param artifact  Artifact for whom to get the dependencies.
     * @return Set of APK, APKLIB and AAR dependencies.
     * @throws org.apache.maven.plugin.MojoExecutionException if it couldn't resolve any of the dependencies.
     */
    public Set<Artifact> getLibraryDependenciesFor( MavenProject project, final Artifact artifact )
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

        final DependencyNode node;
        try
        {
            node = dependencyGraphBuilder.buildDependencyGraph( project, filter );
        }
        catch ( DependencyGraphBuilderException e )
        {
            throw new MojoExecutionException( "Could not resolve project dependency graph", e );
        }

        final DependencyCollector collector = new DependencyCollector( log, artifact );
        collector.visit( node, false );
        return collector.getDependencies();
    }
}
