package com.jayway.maven.plugins.android.common;

import com.jayway.maven.plugins.android.AbstractAndroidMojo;
import com.jayway.maven.plugins.android.AndroidNdk;
import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyFilter;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.graph.Exclusion;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.DependencyRequest;
import org.sonatype.aether.util.filter.AndDependencyFilter;
import org.sonatype.aether.util.filter.ExclusionsDependencyFilter;
import org.sonatype.aether.util.filter.ScopeDependencyFilter;
import org.sonatype.aether.util.graph.PreorderNodeListGenerator;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.jayway.maven.plugins.android.common.AndroidExtension.APKLIB;
import static com.jayway.maven.plugins.android.common.AndroidExtension.APKLIB_PRECOMPILED;
import static org.apache.maven.RepositoryUtils.toDependency;

/**
 * @author Johan Lindquist
 */
public class NativeHelper {

    public static final int NDK_REQUIRED_VERSION = 7;

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
        final Set<Artifact> filteredArtifacts = new LinkedHashSet<Artifact>();

        // Add all dependent artifacts declared in the pom file
        @SuppressWarnings( "unchecked" )
        final Set<Artifact> allArtifacts = project.getDependencyArtifacts();

        // Add all attached artifacts as well - this could come from the NDK mojo for example
        boolean result = allArtifacts.addAll( project.getAttachedArtifacts() );

        for ( Artifact artifact : allArtifacts ) {
            // A null value in the scope indicates that the artifact has been attached
            // as part of a previous build step (NDK mojo)
            if ( isNativeLibrary( sharedLibraries, artifact.getType() ) && artifact.getScope() == null ) {
                // Including attached artifact
                log.debug( "Including attached artifact: "+artifact.getArtifactId()+"("+artifact.getGroupId()+")" );
                filteredArtifacts.add( artifact );
            } else if ( isNativeLibrary( sharedLibraries, artifact.getType() ) && ( Artifact.SCOPE_COMPILE.equals( artifact.getScope() ) || Artifact.SCOPE_RUNTIME.equals( artifact.getScope() ) ) ) {
                filteredArtifacts.add( artifact );
            } else if ( APKLIB.equals( artifact.getType() ) || APKLIB_PRECOMPILED.equals( artifact.getType() ) ) {
                // Check if the artifact contains a libs folder - if so, include it in the list
                File libsFolder = new File( AbstractAndroidMojo.getLibraryUnpackDirectory( unpackDirectory, artifact )+"/libs" );
                if ( libsFolder.exists() ) {
                    filteredArtifacts.add( artifact );
                }
            }
        }

        Set<Artifact> transientArtifacts = processTransientDependencies( project.getDependencies(), sharedLibraries );

        filteredArtifacts.addAll( transientArtifacts );

        return filteredArtifacts;
    }

    private boolean isNativeLibrary( boolean sharedLibraries, String artifactType ) {
        return (sharedLibraries ? "so".equals( artifactType ) : "a".equals( artifactType ));
    }

    private Set<Artifact> processTransientDependencies( List<org.apache.maven.model.Dependency> dependencies, boolean sharedLibraries ) throws MojoExecutionException {

        Set<Artifact> transientArtifacts = new LinkedHashSet<Artifact>();
        for ( org.apache.maven.model.Dependency dependency : dependencies ) {
            if ( !"provided".equals( dependency.getScope() ) && !dependency.isOptional()) {
                transientArtifacts.addAll( processTransientDependencies( toDependency(dependency, repoSession.getArtifactTypeRegistry()), sharedLibraries) );
            }
        }

        return transientArtifacts;

    }

    private Set<Artifact> processTransientDependencies( Dependency dependency, boolean sharedLibraries ) throws MojoExecutionException {
        try {
            final Set<Artifact> artifacts = new LinkedHashSet<Artifact>();

            final CollectRequest collectRequest = new CollectRequest();

            collectRequest.setRoot( dependency );
            collectRequest.setRepositories( projectRepos );
            final DependencyNode node = repoSystem.collectDependencies( repoSession, collectRequest ).getRoot();

            Collection<String> exclusionPatterns = new ArrayList<String>();
            if (dependency.getExclusions() != null && !dependency.getExclusions().isEmpty())
            {
                for (Exclusion exclusion : dependency.getExclusions()) {
                    exclusionPatterns.add(exclusion.getGroupId()+ ":" +  exclusion.getArtifactId());
                }
            }

            final DependencyRequest dependencyRequest = new DependencyRequest( node, new AndDependencyFilter(
             new ExclusionsDependencyFilter(exclusionPatterns)
            ,new AndDependencyFilter(
                    new ScopeDependencyFilter( Arrays.asList( "compile", "runtime" ), Arrays.asList( "test" ) ),
                    // Also exclude any optional dependencies
                    new DependencyFilter() {
                        @Override
                        public boolean accept( DependencyNode dependencyNode, List<DependencyNode> dependencyNodes ) {
                            return !dependencyNode.getDependency().isOptional();
                        }
                    }
            )));


            repoSystem.resolveDependencies( repoSession, dependencyRequest );

            PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
            node.accept( nlg );

            final List<Dependency> dependencies = nlg.getDependencies( false );

            for ( Dependency dep : dependencies ) {
                final org.sonatype.aether.artifact.Artifact depAetherArtifact = dep.getArtifact();
                if ( isNativeLibrary( sharedLibraries, depAetherArtifact.getExtension() ) ) {
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

    public static void validateNDKVersion(File ndkHomeDir) throws MojoExecutionException {
        final File ndkVersionFile = new File(ndkHomeDir, "RELEASE.TXT");

        if (!ndkVersionFile.exists()) {
            throw new MojoExecutionException("Could not locate RELEASE.TXT in the Android NDK base directory '" + ndkHomeDir.getAbsolutePath() + "'.  Please verify your setup! " + AndroidNdk.PROPER_NDK_HOME_DIRECTORY_MESSAGE);
        }

        try {
            String versionStr = FileUtils.readFileToString(ndkVersionFile);
            validateNDKVersion(NDK_REQUIRED_VERSION, versionStr);
        } catch (Exception e) {
            throw new MojoExecutionException("Error while extracting NDK version from '" + ndkVersionFile.getAbsolutePath() + "'. Please verify your setup! " + AndroidNdk.PROPER_NDK_HOME_DIRECTORY_MESSAGE);
        }
    }

    public static void validateNDKVersion(int desiredVersion, String versionStr) throws MojoExecutionException {

        int version = 0;

        if (versionStr != null) {
            versionStr = versionStr.trim();
            Pattern pattern = Pattern.compile("[r]([0-9]{1,3})([a-z]{0,1}).*");
            Matcher m = pattern.matcher(versionStr);
            if (m.matches()) {
                final String group = m.group(1);
                version = Integer.parseInt(group);
            }
        }

        if (version < desiredVersion) {
            throw new MojoExecutionException("You are running an old NDK (version " + versionStr + "), please update to at least r'" + desiredVersion + "' or later");
        }
    }



}
