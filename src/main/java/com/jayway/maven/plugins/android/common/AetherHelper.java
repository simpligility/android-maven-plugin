package com.jayway.maven.plugins.android.common;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.util.artifact.DefaultArtifact;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Helper class to convert between Maven and Aether beans and provide helper methods for resolving artifacts.
 *
 * @author Johan Lindquist
 */
public class AetherHelper
{

    /**
     * Converts the specified Maven artifact to a Aether artifact.  This method takes into consideration situations
     * where some of the artifact properties (classifier, etc) are null.
     *
     * @param artifact The Maven artifact to convert
     * @return The resulting Aether artifact
     */
    public static org.sonatype.aether.artifact.Artifact createAetherArtifact( Artifact artifact )
    {
        DefaultArtifact defaultArtifact;
        if ( artifact.getClassifier() != null )
        {
            defaultArtifact =
                    new DefaultArtifact( artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(),
                            artifact.getType(), artifact.getVersion() );
        }
        else
        {
            defaultArtifact = new DefaultArtifact( artifact.getGroupId(), artifact.getArtifactId(), artifact.getType(),
                    artifact.getVersion() );
        }
        defaultArtifact.setFile( artifact.getFile() );
        return defaultArtifact;
    }

    public static Set<Artifact> resolveArtifacts( Set<Artifact> artifacts, RepositorySystem repositorySystem,
                                                  RepositorySystemSession repositorySystemSession,
                                                  List<RemoteRepository> repositories ) throws MojoExecutionException
    {
        try
        {

            final Set<Artifact> resolvedArtifacts = new LinkedHashSet<Artifact>();

            for ( Artifact artifact : artifacts )
            {
                final Artifact resolvedArtifact = AetherHelper
                        .resolveArtifact( artifact, repositorySystem, repositorySystemSession, repositories );
                resolvedArtifacts.add( resolvedArtifact );
            }
            return resolvedArtifacts;
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Error resolving dependencies: " + e.getMessage(), e );
        }
    }

    public static Artifact resolveArtifact( Artifact artifact, RepositorySystem repositorySystem,
                                            RepositorySystemSession repositorySystemSession,
                                            List<RemoteRepository> repositories ) throws MojoExecutionException
    {
        try
        {
            final ArtifactRequest artifactRequest = new ArtifactRequest();
            org.sonatype.aether.artifact.Artifact aetherArtifact = AetherHelper.createAetherArtifact( artifact );
            artifactRequest.setArtifact( aetherArtifact );
            artifactRequest.setRepositories( repositories );
            final ArtifactResult artifactResult =
                    repositorySystem.resolveArtifact( repositorySystemSession, artifactRequest );

            final org.apache.maven.artifact.DefaultArtifact defaultArtifact =
                    new org.apache.maven.artifact.DefaultArtifact( artifact.getGroupId(), artifact.getArtifactId(),
                            artifact.getVersion(), artifact.getScope(), artifact.getType(), artifact.getClassifier(),
                            artifact.getArtifactHandler() );
            defaultArtifact.setFile( artifactResult.getArtifact().getFile() );
            return defaultArtifact;
        }
        catch ( ArtifactResolutionException e )
        {
            throw new MojoExecutionException( "Error while resolving artifact: " + e.getMessage(), e );
        }
    }

}
