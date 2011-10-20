package com.jayway.maven.plugins.android.common;

import com.jayway.maven.plugins.android.AbstractAndroidMojo;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.DependencyRequest;
import org.sonatype.aether.util.filter.ScopeDependencyFilter;
import org.sonatype.aether.util.graph.PreorderNodeListGenerator;

import java.io.File;
import java.util.*;

import static com.jayway.maven.plugins.android.common.AndroidExtension.APKLIB;

/**
 * @author Johan Lindquist
 */
public class NativeHelper {

    private MavenProject project;
    private RepositorySystemSession repoSession;
    private RepositorySystem repoSystem;
    private ArtifactFactory artifactFactory;
    private Log log;
    private List<RemoteRepository> projectRepos;

    public NativeHelper( MavenProject project, List<RemoteRepository> projectRepos, RepositorySystemSession repoSession, RepositorySystem repoSystem, ArtifactFactory artifactFactory, Log log ) {
        this.project = project;
        this.projectRepos = projectRepos;
        this.repoSession = repoSession;
        this.repoSystem = repoSystem;
        this.artifactFactory = artifactFactory;
        this.log = log;
    }

    public static boolean hasStaticNativeLibraryArtifact(Set<Artifact> resolveNativeLibraryArtifacts) {
        for (Artifact resolveNativeLibraryArtifact : resolveNativeLibraryArtifacts) {
            if ( "a".equals(resolveNativeLibraryArtifact.getType()) ) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasSharedNativeLibraryArtifact(Set<Artifact> resolveNativeLibraryArtifacts) {
        for (Artifact resolveNativeLibraryArtifact : resolveNativeLibraryArtifacts) {
            if ( "so".equals(resolveNativeLibraryArtifact.getType()) ) {
                return true;
            }
        }
        return false;
    }

    public Set<Artifact> getNativeDependenciesArtifacts(File unpackDirectory, boolean sharedLibraries) throws MojoExecutionException {
        final Set<Artifact> filteredArtifacts = new HashSet<Artifact>();

        // Add all dependent artifacts declared in the pom file
        @SuppressWarnings( "unchecked" )
        final Set<Artifact> allArtifacts = project.getDependencyArtifacts();

        // Add all attached artifacts as well - this could come from the NDK mojo for example
        boolean result = allArtifacts.addAll( project.getAttachedArtifacts() );

        for ( Artifact artifact : allArtifacts ) {
            // A null value in the scope indicates that the artifact has been attached
            // as part of a previous build step (NDK mojo)
            if ( (sharedLibraries ? "so".equals( artifact.getType() ) : "a".equals( artifact.getType())) && artifact.getScope() == null ) {
                // Including attached artifact
                log.debug( "Including attached artifact: "+artifact.getArtifactId()+"("+artifact.getGroupId()+")" );
                filteredArtifacts.add( artifact );
            } else if ( "so".equals( artifact.getType() ) && ( Artifact.SCOPE_COMPILE.equals( artifact.getScope() ) || Artifact.SCOPE_RUNTIME.equals( artifact.getScope() ) ) ) {
                filteredArtifacts.add( artifact );
            } else if ( APKLIB.equals( artifact.getType() ) ) {
                // Check if the artifact contains a libs folder - if so, include it in the list
                File libsFolder = new File( AbstractAndroidMojo.getLibraryUnpackDirectory( unpackDirectory, artifact )+"/libs" );
                if ( libsFolder.exists() ) {
                    filteredArtifacts.add( artifact );
                }
            }
        }

        Set<Artifact> transientArtifacts = processTransientDependencies( allArtifacts, sharedLibraries );

        filteredArtifacts.addAll( transientArtifacts );

        return filteredArtifacts;
    }

    private Set<Artifact> processTransientDependencies( Set<Artifact> artifacts, boolean sharedLibraries ) throws MojoExecutionException {

        Set<Artifact> transientArtifacts = new TreeSet<Artifact>();
        for ( Artifact artifact : artifacts ) {
            if ( !"provided".equals( artifact.getScope() ) && !artifact.isOptional() && !project.getAttachedArtifacts().contains(artifact)) {
                org.sonatype.aether.artifact.Artifact aetherArtifact = AetherHelper.createAetherArtifact( artifact );
                transientArtifacts.addAll( processTransientDependencies( artifact, aetherArtifact, artifact.getArtifactHandler(), sharedLibraries) );
            }
        }

        return transientArtifacts;

    }

    private Set<Artifact> processTransientDependencies( Artifact artifact, org.sonatype.aether.artifact.Artifact aetherArtifact, ArtifactHandler artifactHandler, boolean sharedLibraries ) throws MojoExecutionException {
        try {
            final Set<Artifact> artifacts = new TreeSet<Artifact>();

            Dependency dependency = new Dependency( aetherArtifact, artifact.getScope() );

            final CollectRequest collectRequest = new CollectRequest();

            collectRequest.setRoot( dependency );
            collectRequest.setRepositories( projectRepos );
            final DependencyNode node = repoSystem.collectDependencies( repoSession, collectRequest ).getRoot();

            final DependencyRequest dependencyRequest = new DependencyRequest( node, new ScopeDependencyFilter( Arrays.asList( "compile", "runtime" ), Arrays.asList( "test" ) ) );


            repoSystem.resolveDependencies( repoSession, dependencyRequest );

            PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
            node.accept( nlg );

            final List<Dependency> dependencies = nlg.getDependencies( false );

            for ( Dependency dep : dependencies ) {
                final org.sonatype.aether.artifact.Artifact depAetherArtifact = dep.getArtifact();
                if ( (sharedLibraries ? "so".equals( depAetherArtifact.getExtension() ) : "a".equals( depAetherArtifact.getExtension() )) ) {
                    final Artifact mavenArtifact = artifactFactory.createDependencyArtifact( depAetherArtifact.getGroupId(), depAetherArtifact.getArtifactId(), VersionRange.createFromVersion( depAetherArtifact.getVersion() ), depAetherArtifact.getExtension(), depAetherArtifact.getClassifier(), dep.getScope() );
                    mavenArtifact.setFile(depAetherArtifact.getFile());
                    artifacts.add( mavenArtifact );
                }
            }

            return artifacts;
        } catch ( Exception e ) {
            throw new MojoExecutionException( "Error while processing transient dependencies", e );
        }
    }

}
