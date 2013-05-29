package com.jayway.maven.plugins.android.common;

import com.jayway.maven.plugins.android.AbstractAndroidMojo;
import com.jayway.maven.plugins.android.AndroidNdk;
import com.jayway.maven.plugins.android.phase09package.ApklibMojo;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.util.filter.AndDependencyFilter;
import org.eclipse.aether.util.filter.ExclusionsDependencyFilter;
import org.eclipse.aether.util.filter.ScopeDependencyFilter;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.jayway.maven.plugins.android.common.AndroidExtension.APKLIB;
import static org.apache.maven.RepositoryUtils.toDependency;

/**
 * @author Johan Lindquist
 */
public class NativeHelper
{

    public static final int NDK_REQUIRED_VERSION = 7;

    private MavenProject project;
    private RepositorySystemSession repoSession;
    private RepositorySystem repoSystem;
    private ArtifactFactory artifactFactory;
    private Log log;
    private List<RemoteRepository> projectRepos;

    public NativeHelper( MavenProject project, List<RemoteRepository> projectRepos, RepositorySystemSession repoSession,
                         RepositorySystem repoSystem, ArtifactFactory artifactFactory, Log log )
    {
        this.project = project;
        this.projectRepos = projectRepos;
        this.repoSession = repoSession;
        this.repoSystem = repoSystem;
        this.artifactFactory = artifactFactory;
        this.log = log;
    }

    public static boolean hasStaticNativeLibraryArtifact( Set<Artifact> resolveNativeLibraryArtifacts, 
                                                          File unpackDirectory,
                                                          String ndkArchitecture )
    {
        for ( Artifact resolveNativeLibraryArtifact : resolveNativeLibraryArtifacts )
        {
            if ( "a".equals( resolveNativeLibraryArtifact.getType() ) )
            {
                return true;
            }
            if ( APKLIB.equals( resolveNativeLibraryArtifact.getType() ) )
            {
                File[] aFiles = listNativeFiles( resolveNativeLibraryArtifact, unpackDirectory, 
                                                 ndkArchitecture, true );
                if ( aFiles != null && aFiles.length > 0 )
                {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean hasSharedNativeLibraryArtifact( Set<Artifact> resolveNativeLibraryArtifacts, 
                                                          File unpackDirectory,
                                                          String ndkArchitecture )
    {
        for ( Artifact resolveNativeLibraryArtifact : resolveNativeLibraryArtifacts )
        {
            if ( "so".equals( resolveNativeLibraryArtifact.getType() ) )
            {
                return true;
            }
            if ( APKLIB.equals( resolveNativeLibraryArtifact.getType() ) )
            {
                File[] soFiles = listNativeFiles( resolveNativeLibraryArtifact, unpackDirectory, 
                                                    ndkArchitecture, false );
                if ( soFiles != null && soFiles.length > 0 )
                {
                    return true;
                }
            }
        }
        return false;
    }

    public static File[] listNativeFiles( Artifact a, File unpackDirectory, 
                                          final String ndkArchitecture, final boolean staticLibrary )
    {
        File libsFolder = new File(
                AbstractAndroidMojo
                    .getLibraryUnpackDirectory( unpackDirectory, a ), 
                File.separator + ApklibMojo.NATIVE_LIBRARIES_FOLDER + File.separator + ndkArchitecture );
        if ( libsFolder.exists() )
        {
            File[] libFiles = libsFolder.listFiles( new FilenameFilter()
            {
                public boolean accept( final File dir, final String name )
                {
                    return name.startsWith( "lib" ) && name.endsWith( ( staticLibrary ? ".a" : ".so" ) );
                }
            } );
            return libFiles;
        }
        return null;
    }
    
    public Set<Artifact> getNativeDependenciesArtifacts( File unpackDirectory, boolean sharedLibraries )
            throws MojoExecutionException
    {
        final Set<Artifact> filteredArtifacts = new LinkedHashSet<Artifact>();
        final Set<Artifact> allArtifacts = new LinkedHashSet<Artifact>();
        
        // Add all dependent artifacts declared in the pom file
        // Note: The result of project.getDependencyArtifacts() can be an UnmodifiableSet so we 
        //       have created our own above and add to that.
        allArtifacts.addAll( project.getDependencyArtifacts() );

        // Add all attached artifacts as well - this could come from the NDK mojo for example
        allArtifacts.addAll( project.getAttachedArtifacts() );

        for ( Artifact artifact : allArtifacts )
        {
            // A null value in the scope indicates that the artifact has been attached
            // as part of a previous build step (NDK mojo)
            if ( isNativeLibrary( sharedLibraries, artifact.getType() ) && artifact.getScope() == null )
            {
                // Including attached artifact
                log.debug( "Including attached artifact: " + artifact.getArtifactId() 
                        + "(" + artifact.getGroupId() + "). Artifact scope is not set." );
                filteredArtifacts.add( artifact );
            }
            else
            {
                if ( isNativeLibrary( sharedLibraries, artifact.getType() ) && (
                        Artifact.SCOPE_COMPILE.equals( artifact.getScope() ) || Artifact.SCOPE_RUNTIME
                                .equals( artifact.getScope() ) ) )
                {
                    log.debug( "Including attached artifact: " + artifact.getArtifactId() 
                            + "(" + artifact.getGroupId() + "). Artifact scope is Compile or Runtime." );
                    filteredArtifacts.add( artifact );
                }
                else
                {
                    if ( APKLIB.equals( artifact.getType() ) )
                    {
                        // Check if the artifact contains a libs folder - if so, include it in the list
                        File libsFolder = new File(
                                AbstractAndroidMojo.getLibraryUnpackDirectory( unpackDirectory, artifact ) + "/libs" );
                        if ( libsFolder.exists() )
                        {
                            log.debug( "Including attached artifact: " + artifact.getArtifactId() 
                                    + "(" + artifact.getGroupId() + "). Artifact is APKLIB." );
                            filteredArtifacts.add( artifact );
                        }
                    }
                }
            }
        }

        Set<Artifact> transientArtifacts = processTransientDependencies( project.getDependencies(), sharedLibraries );

        filteredArtifacts.addAll( transientArtifacts );

        return filteredArtifacts;
    }

    private boolean isNativeLibrary( boolean sharedLibraries, String artifactType )
    {
        return ( sharedLibraries ? "so".equals( artifactType ) : "a".equals( artifactType ) );
    }

    private Set<Artifact> processTransientDependencies( List<org.apache.maven.model.Dependency> dependencies,
                                                        boolean sharedLibraries ) throws MojoExecutionException
    {

        Set<Artifact> transientArtifacts = new LinkedHashSet<Artifact>();
        for ( org.apache.maven.model.Dependency dependency : dependencies )
        {
            if ( ! "provided".equals( dependency.getScope() ) && ! dependency.isOptional() )
            {
                transientArtifacts.addAll(
                        processTransientDependencies( toDependency( dependency, repoSession.getArtifactTypeRegistry() ),
                                sharedLibraries ) );
            }
        }

        return transientArtifacts;

    }

    private Set<Artifact> processTransientDependencies( Dependency dependency, boolean sharedLibraries )
            throws MojoExecutionException
    {
        try
        {
            final Set<Artifact> artifacts = new LinkedHashSet<Artifact>();

            final CollectRequest collectRequest = new CollectRequest();

            collectRequest.setRoot( dependency );
            collectRequest.setRepositories( projectRepos );
            final DependencyNode node = repoSystem.collectDependencies( repoSession, collectRequest ).getRoot();

            Collection<String> exclusionPatterns = new ArrayList<String>();
            if ( dependency.getExclusions() != null && ! dependency.getExclusions().isEmpty() )
            {
                for ( Exclusion exclusion : dependency.getExclusions() )
                {
                    exclusionPatterns.add( exclusion.getGroupId() + ":" + exclusion.getArtifactId() );
                }
            }

            final DependencyRequest dependencyRequest = new DependencyRequest( node,
                    new AndDependencyFilter( new ExclusionsDependencyFilter( exclusionPatterns ),
                            new AndDependencyFilter( new ScopeDependencyFilter( Arrays.asList( "compile", "runtime" ),
                                    Arrays.asList( "test" ) ),
                                    // Also exclude any optional dependencies
                                    new DependencyFilter()
                                    {
                                        @Override
                                        public boolean accept( DependencyNode dependencyNode,
                                                               List<DependencyNode> dependencyNodes )
                                        {
                                            return ! dependencyNode.getDependency().isOptional();
                                        }
                                    } ) ) );


            repoSystem.resolveDependencies( repoSession, dependencyRequest );

            PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
            node.accept( nlg );

            final List<Dependency> dependencies = nlg.getDependencies( false );

            for ( Dependency dep : dependencies )
            {
                final org.eclipse.aether.artifact.Artifact depAetherArtifact = dep.getArtifact();
                if ( isNativeLibrary( sharedLibraries, depAetherArtifact.getExtension() ) )
                {
                    final Artifact mavenArtifact = artifactFactory
                            .createDependencyArtifact( depAetherArtifact.getGroupId(),
                                    depAetherArtifact.getArtifactId(),
                                    VersionRange.createFromVersion( depAetherArtifact.getVersion() ),
                                    depAetherArtifact.getExtension(), depAetherArtifact.getClassifier(),
                                    dep.getScope() );
                    mavenArtifact.setFile( depAetherArtifact.getFile() );
                    artifacts.add( mavenArtifact );
                }
            }

            return artifacts;
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Error while processing transient dependencies", e );
        }
    }

    public static void validateNDKVersion( File ndkHomeDir ) throws MojoExecutionException
    {
        final File ndkVersionFile = new File( ndkHomeDir, "RELEASE.TXT" );

        if ( ! ndkVersionFile.exists() )
        {
            throw new MojoExecutionException(
                    "Could not locate RELEASE.TXT in the Android NDK base directory '" + ndkHomeDir.getAbsolutePath()
                            + "'.  Please verify your setup! " + AndroidNdk.PROPER_NDK_HOME_DIRECTORY_MESSAGE );
        }

        try
        {
            String versionStr = FileUtils.readFileToString( ndkVersionFile );
            validateNDKVersion( NDK_REQUIRED_VERSION, versionStr );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Error while extracting NDK version from '"
                    + ndkVersionFile.getAbsolutePath() + "'. Please verify your setup! "
                    + AndroidNdk.PROPER_NDK_HOME_DIRECTORY_MESSAGE );
        }
    }

    public static void validateNDKVersion( int desiredVersion, String versionStr ) throws MojoExecutionException
    {

        int version = 0;

        if ( versionStr != null )
        {
            versionStr = versionStr.trim();
            Pattern pattern = Pattern.compile( "[r]([0-9]{1,3})([a-z]{0,1}).*" );
            Matcher m = pattern.matcher( versionStr );
            if ( m.matches() )
            {
                final String group = m.group( 1 );
                version = Integer.parseInt( group );
            }
        }

        if ( version < desiredVersion )
        {
            throw new MojoExecutionException( "You are running an old NDK (version " + versionStr + "), please update "
                    + "to at least r'" + desiredVersion + "' or later" );
        }
    }

    public static String[] getAppAbi( File applicationMakefile )
    {
        Scanner scanner = null;
        try
        {
            if ( applicationMakefile != null && applicationMakefile.exists() )
            {
                scanner = new Scanner( applicationMakefile );
                while ( scanner.hasNextLine( ) )
                {
                    String line = scanner.nextLine().trim();
                    if ( line.startsWith( "APP_ABI" ) )
                    {
                        return line.substring( line.indexOf( ":=" ) + 2 ).trim().split( " " );
                    }
                }
            }
        }
        catch ( FileNotFoundException e )
        {
            // do nothing
        }
        finally
        {
            if ( scanner != null )
            {
                scanner.close();
            }
        }
        return null;
    }

    public static String[] getNdkArchitectures( final String ndkClassifier, final String ndkArchitecture,
                                                final String applicationMakefile, final File basedir )
        throws MojoExecutionException
    {
        // if there is a classifier, return it
        if ( ndkClassifier != null )
        {
            return new String[] { ndkClassifier };
        }

        // if there is a specified ndk architecture, return it
        if ( ndkArchitecture != null )
        {
            return new String[] { ndkArchitecture };
        }

        // if there is no application makefile specified, let's use the default one
        String applicationMakefileToUse = applicationMakefile;
        if ( applicationMakefileToUse == null )
        {
            applicationMakefileToUse = "jni/Application.mk";
        }

        // now let's see if the application file exists
        File appMK = new File( basedir, applicationMakefileToUse );
        if ( appMK.exists() )
        {
            String[] foundNdkArchitectures = getAppAbi( appMK );
            if ( foundNdkArchitectures != null )
            {
                return foundNdkArchitectures;
            }
        }

        // return a default ndk architecture
        return new String[] { "armeabi" };
    }
}
