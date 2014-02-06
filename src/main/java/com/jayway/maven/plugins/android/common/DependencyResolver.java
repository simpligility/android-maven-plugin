package com.jayway.maven.plugins.android.common;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;

import java.util.ArrayList;
import java.util.List;

import static com.jayway.maven.plugins.android.common.AndroidExtension.AAR;
import static com.jayway.maven.plugins.android.common.AndroidExtension.APKLIB;

/**
 * Resolves the aar and apklib dependencies for an Artifact.
 *
 * @author William Ferguson <william.ferguson@xandar.com.au>
 */
public final class DependencyResolver
{

    private final RepositorySystem repoSystem;
    private final RepositorySystemSession repoSession;
    private final List<RemoteRepository> remoteRepos;
    private final ArtifactHandler artifactHandler;

    public DependencyResolver( RepositorySystem repoSystem,
                               RepositorySystemSession repoSession,
                               List<RemoteRepository> remoteRepos,
                               ArtifactHandler artifactHandler )
    {
        this.repoSystem = repoSystem;
        this.repoSession = repoSession;
        this.remoteRepos = remoteRepos;
        this.artifactHandler = artifactHandler;
    }

    /**
     * Returns the list of transitive APKLIB or AAR dependencies of the supplied artifact.
     *
     * @param artifact  Artifact for whom to get the dependencies.
     * @return List of APKLIB and AAR dependencies.
     * @throws MojoExecutionException if it couldn't resolve any of the dependencies.
     */
    public List<Artifact> getDependenciesFor( Artifact artifact ) throws MojoExecutionException
    {
        final List<Artifact> results = new ArrayList<Artifact>();

        final org.eclipse.aether.artifact.Artifact artifactToResolve =
                new DefaultArtifact(
                        artifact.getGroupId(),
                        artifact.getArtifactId(),
                        artifact.getType(),
                        artifact.getVersion()
                );

        final List<Dependency> transitiveDeps = getDependenciesFor( artifactToResolve );
        for ( Dependency dependency : transitiveDeps )
        {
            final Artifact artifactDep = new org.apache.maven.artifact.DefaultArtifact(
                    dependency.getArtifact().getGroupId(),
                    dependency.getArtifact().getArtifactId(),
                    dependency.getArtifact().getVersion(),
                    dependency.getScope(),
                    dependency.getArtifact().getExtension(),
                    dependency.getArtifact().getClassifier(),
                    artifactHandler
            );
            results.add( artifactDep );
        }

        return results;
    }

    /**
     * Returns the list of transitive APKLIB or AAR dependencies of the supplied artifact.
     *
     * @param artifact  Artifact for whom to get the dependencies.
     * @return List of APKLIB and AAR dependencies.
     * @throws MojoExecutionException if it couldn't resolve any of the dependencies.
     */
    private List<Dependency> getDependenciesFor( org.eclipse.aether.artifact.Artifact artifact )
            throws MojoExecutionException
    {
        final List<Dependency> results = new ArrayList<Dependency>();

        final ArtifactDescriptorRequest descriptorRequest = new ArtifactDescriptorRequest();
        descriptorRequest.setArtifact( artifact );
        descriptorRequest.setRepositories( remoteRepos );

        final ArtifactDescriptorResult descriptorResult;
        try
        {
            descriptorResult = repoSystem.readArtifactDescriptor( repoSession, descriptorRequest );
        }
        catch ( ArtifactDescriptorException e )
        {
            throw new MojoExecutionException( "Could not resolve dependencies for " + artifact, e );
        }

        for ( Dependency dependency : descriptorResult.getDependencies() )
        {
            final String extension = dependency.getArtifact().getExtension();
            if ( extension.equals( APKLIB ) || extension.equals( AAR ) )
            {
                results.add( dependency );
                results.addAll( getDependenciesFor( dependency.getArtifact() ) );
            }
        }

        return results;
    }

}
