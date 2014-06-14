package com.jayway.maven.plugins.android.common;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.apache.maven.shared.runtime.DefaultMavenRuntime;
import org.apache.maven.shared.runtime.MavenRuntimeException;
import org.codehaus.plexus.logging.Logger;

import java.io.File;
import java.net.MalformedURLException;
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
     * @param resolverHelper  ArtifactResolverHelper to use to find the POM for the Artifact.
     * @param artifact  Artifact for whom to get the dependencies.
     * @return Set of APK, APKLIB and AAR dependencies.
     * @throws org.apache.maven.plugin.MojoExecutionException if it couldn't resolve any of the dependencies.
     */
    public Set<Artifact> getLibraryDependenciesFor( ArtifactResolverHelper resolverHelper, final Artifact artifact )
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
            // Get the MavenProject for the Artifact so that we can resolve the dependencies for the project.
            final MavenProject project = getMavenProjectForArtifact( resolverHelper, artifact );
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

    private MavenProject getMavenProjectForArtifact( ArtifactResolverHelper resolverHelper, Artifact artifact )
            throws MojoExecutionException
    {
        // Create POM Artifact from current Artifact
        final Artifact pomArtifact = new DefaultArtifact(
                artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(),
                artifact.getScope(), "pom", null, artifact.getArtifactHandler()
        );
        log.debug( "POM artifact : " + pomArtifact );

        // TODO Resolve path to POM Artifact - this isn't working. It is finding apklib in sibling module (not POM).
        final File pomArtifactFile = resolverHelper.resolveArtifactToFile( pomArtifact );
        log.debug( "POM artifact file : " + pomArtifactFile );

        try
        {
            final DefaultMavenRuntime mavenRuntime = new DefaultMavenRuntime();
            final MavenProject project = mavenRuntime.getProject( pomArtifactFile.toURI().toURL() );
            log.debug( "Resolved artifact project : " + project );
            return project;
        }
        catch ( MalformedURLException e )
        {
            throw new MojoExecutionException( "Could not resolve artifact POM : " + pomArtifactFile, e );
        }
        catch ( MavenRuntimeException e )
        {
            throw new MojoExecutionException( "Could not resolve artifact Project : " + pomArtifactFile, e );
        }
    }
}
