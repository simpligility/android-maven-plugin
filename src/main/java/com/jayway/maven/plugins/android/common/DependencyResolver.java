package com.jayway.maven.plugins.android.common;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.apache.maven.shared.dependency.graph.filter.DependencyNodeFilter;
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
     * @return all the dependencies for a project.
     * @throws MojoExecutionException if the dependency graph can't be built.
     */
    public Set<Artifact> getProjectDependenciesFor( MavenProject project ) throws MojoExecutionException
    {
        final DependencyNode node;
        try
        {
            // No need to filter our search. We want to resolve all artifacts.
            node = dependencyGraphBuilder.buildDependencyGraph( project, null );
        }
        catch ( DependencyGraphBuilderException e )
        {
            throw new MojoExecutionException( "Could not resolve project dependency graph", e );
        }

        // Search all the children recursively.
        // Don't want to search from the root node because then it would be included as it's own dependency.
        final Set<Artifact> dependencies = new HashSet<Artifact>();
        for ( final DependencyNode child : node.getChildren() )
        {
            resolveRecursively( dependencies, child, null );
        }
        return dependencies;
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
        // Set a filter that should only return the supplied artifact.
        final ArtifactFilter filter = new ArtifactFilter()
        {
            @Override
            public boolean include( Artifact found )
            {
                return found.getGroupId().equals( artifact.getGroupId() )
                        && found.getArtifactId().equals( artifact.getArtifactId() )
                        && found.getVersion().equals( artifact.getVersion() )
                        && found.getType().equals( artifact.getType() )
                        ;
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

        final DependencyNodeFilter libraryFilter = new DependencyNodeFilter()
        {
            @Override
            public boolean accept( DependencyNode child )
            {
                final String extension = child.getArtifact().getType();
                return ( extension.equals( APKLIB ) || extension.equals( AAR ) || extension.equals( APK ) );
            }
        };

        // Accrete any children that are libraries and any recurse down the libraries.
        final Set<Artifact> dependencies = new HashSet<Artifact>();
        resolveRecursively( dependencies, node, libraryFilter );
        return dependencies;
    }

    private void resolveRecursively( Set<Artifact> result, DependencyNode node, DependencyNodeFilter filter )
    {
        if ( ( filter != null ) && ( !filter.accept( node ) ) )
        {
            return;
        }

        log.debug( "Adding dep : " + node.getArtifact() );
        result.add( node.getArtifact() );
        for ( final DependencyNode child : node.getChildren() )
        {
            resolveRecursively( result, child, filter );
        }
    }
}
