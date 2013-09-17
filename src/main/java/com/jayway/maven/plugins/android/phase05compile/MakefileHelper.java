package com.jayway.maven.plugins.android.phase05compile;

import com.jayway.maven.plugins.android.common.AetherHelper;
import com.jayway.maven.plugins.android.common.AndroidExtension;
import com.jayway.maven.plugins.android.common.JarHelper;
import com.jayway.maven.plugins.android.common.NativeHelper;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.repository.RemoteRepository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Various helper methods for dealing with Android Native makefiles.
 *
 * @author Johan Lindquist
 */
public class MakefileHelper
{
    public static final String MAKEFILE_CAPTURE_FILE = "ANDROID_MAVEN_PLUGIN_LOCAL_C_INCLUDES_FILE";
    
    public static final boolean IS_WINDOWS = System.getProperty( "os.name" ).toLowerCase().indexOf( "windows" ) >= 0;
    public static final String WINDOWS_DRIVE_ROOT_REGEX = "[a-zA-Z]:\\\\";

    /**
     * Holder for the result of creating a makefile.  This in particular keep tracks of all directories created
     * for extracted header files.
     */
    public static class MakefileHolder
    {
        String makeFile;
        List<File> includeDirectories;

        public MakefileHolder( List<File> includeDirectories, String makeFile )
        {
            this.includeDirectories = includeDirectories;
            this.makeFile = makeFile;
        }

        public List<File> getIncludeDirectories()
        {
            return includeDirectories;
        }

        public String getMakeFile()
        {
            return makeFile;
        }
    }

    private Log log;
    private final RepositorySystem repoSystem;
    private final RepositorySystemSession repoSession;
    private final List<RemoteRepository> projectRepos;
    private final File unpackedApkLibsDirectory;
    
    /**
     * Initialize the MakefileHelper by storing the supplied parameters to local variables.
     * @param log
     * @param repoSystem
     * @param repoSession
     * @param projectRepos
     * @param unpackedApkLibsDirectory
     */
    public MakefileHelper( Log log,
                           RepositorySystem repoSystem, RepositorySystemSession repoSession, 
                           List<RemoteRepository> projectRepos, 
                           File unpackedApkLibsDirectory )
    {
        this.log = log;
        this.repoSystem = repoSystem;
        this.repoSession = repoSession;
        this.projectRepos = projectRepos;
        this.unpackedApkLibsDirectory = unpackedApkLibsDirectory;
    }
    
    /**
     * Cleans up all include directories created in the temp directory during the build.
     *
     * @param makefileHolder The holder produced by the
     * {@link MakefileHelper#createMakefileFromArtifacts(java.io.File, java.util.Set,
     * boolean, org.sonatype.aether.RepositorySystemSession, java.util.List, org.sonatype.aether.RepositorySystem)}
     */
    public static void cleanupAfterBuild( MakefileHolder makefileHolder )
    {

        if ( makefileHolder.getIncludeDirectories() != null )
        {
            for ( File file : makefileHolder.getIncludeDirectories() )
            {
                try
                {
                    FileUtils.deleteDirectory( file );
                }
                catch ( IOException e )
                {
                    e.printStackTrace();
                }
            }
        }

    }

    /**
     * Creates an Android Makefile based on the specified set of static library dependency artifacts.
     *
     * @param outputDir         Directory to resolve artifact locations relative to.  Makefiles contain relative paths
     * @param artifacts         The list of (static library) dependency artifacts to create the Makefile from
     * @param useHeaderArchives If true, the Makefile should include a LOCAL_EXPORT_C_INCLUDES statement, pointing to
     *                          the location where the header archive was expanded
     * @param repoSession
     * @param projectRepos
     * @param repoSystem
     * @return The created Makefile
     */
    public MakefileHolder createMakefileFromArtifacts( File outputDir, Set<Artifact> artifacts,
                                                              String ndkArchitecture,
                                                              boolean useHeaderArchives )
            throws IOException, MojoExecutionException
    {

        final StringBuilder makeFile = new StringBuilder( "# Generated by Android Maven Plugin\n" );
        final List<File> includeDirectories = new ArrayList<File>();

        // Add now output - allows us to somewhat intelligently determine the include paths to use for the header
        // archive
        makeFile.append( "$(shell echo \"LOCAL_C_INCLUDES=$(LOCAL_C_INCLUDES)\" > $(" + MAKEFILE_CAPTURE_FILE + "))" );
        makeFile.append( '\n' );
        makeFile.append( "$(shell echo \"LOCAL_PATH=$(LOCAL_PATH)\" >> $(" + MAKEFILE_CAPTURE_FILE + "))" );
        makeFile.append( '\n' );
        makeFile.append( "$(shell echo \"LOCAL_MODULE_FILENAME=$(LOCAL_MODULE_FILENAME)\" >> $("
                + MAKEFILE_CAPTURE_FILE + "))" );
        makeFile.append( '\n' );
        makeFile.append( "$(shell echo \"LOCAL_MODULE=$(LOCAL_MODULE)\" >> $(" + MAKEFILE_CAPTURE_FILE + "))" );
        makeFile.append( '\n' );
        makeFile.append( "$(shell echo \"LOCAL_CFLAGS=$(LOCAL_CFLAGS)\" >> $(" + MAKEFILE_CAPTURE_FILE + "))" );
        makeFile.append( '\n' );

        if ( ! artifacts.isEmpty() )
        {
            for ( Artifact artifact : artifacts )
            {
                if ( artifact.hasClassifier() )
                {
                    makeFile.append( '\n' );
                    makeFile.append( "ifeq ($(TARGET_ARCH_ABI)," ).append( artifact.getClassifier() ).append( ")\n" );
                }

                makeFile.append( "#\n" );
                makeFile.append( "# Group ID: " );
                makeFile.append( artifact.getGroupId() );
                makeFile.append( '\n' );
                makeFile.append( "# Artifact ID: " );
                makeFile.append( artifact.getArtifactId() );
                makeFile.append( '\n' );
                makeFile.append( "# Artifact Type: " );
                makeFile.append( artifact.getType() );
                makeFile.append( '\n' );
                makeFile.append( "# Version: " );
                makeFile.append( artifact.getVersion() );
                makeFile.append( '\n' );
                makeFile.append( "include $(CLEAR_VARS)" );
                makeFile.append( '\n' );
                makeFile.append( "LOCAL_MODULE    := " );
                makeFile.append( artifact.getArtifactId() );
                makeFile.append( '\n' );

                final boolean apklibStatic = addLibraryDetails( makeFile, outputDir, artifact, ndkArchitecture );

                if ( useHeaderArchives )
                {
                    try
                    {
                        Artifact harArtifact = new DefaultArtifact( artifact.getGroupId(), artifact.getArtifactId(),
                                artifact.getVersion(), artifact.getScope(), "har", artifact.getClassifier(),
                                artifact.getArtifactHandler() );
                        final Artifact resolvedHarArtifact = AetherHelper
                                .resolveArtifact( harArtifact, repoSystem, repoSession, projectRepos );

                        File includeDir = new File( System.getProperty( "java.io.tmpdir" ),
                                "android_maven_plugin_native_includes" + System.currentTimeMillis() + "_"
                                        + resolvedHarArtifact.getArtifactId() );
                        includeDir.deleteOnExit();
                        includeDirectories.add( includeDir );

                        JarHelper.unjar( new JarFile( resolvedHarArtifact.getFile() ), includeDir,
                                new JarHelper.UnjarListener()
                                {
                                    @Override
                                    public boolean include( JarEntry jarEntry )
                                    {
                                        return ! jarEntry.getName().startsWith( "META-INF" );
                                    }
                                } );

                        makeFile.append( "LOCAL_EXPORT_C_INCLUDES := " );
                        final String str = includeDir.getAbsolutePath();
                        makeFile.append( str );
                        makeFile.append( '\n' );
                        
                        if ( log.isDebugEnabled() )
                        {
                            Collection<File> includes = FileUtils.listFiles( includeDir,
                                    TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE );
                            log.debug( "Listing LOCAL_EXPORT_C_INCLUDES for " + artifact.getId() + ": " + includes );
                        }
                    }
                    catch ( Exception e )
                    {
                        throw new MojoExecutionException(
                                "Error while resolving header archive file for: " + artifact.getArtifactId(), e );
                    }
                }
                if ( "a".equals( artifact.getType() ) || apklibStatic )
                {
                    makeFile.append( "include $(PREBUILT_STATIC_LIBRARY)\n" );
                }
                else
                {
                    makeFile.append( "include $(PREBUILT_SHARED_LIBRARY)\n" );
                }

                if ( artifact.hasClassifier() )
                {
                    makeFile.append( "endif #" ).append( artifact.getClassifier() ).append( '\n' );
                    makeFile.append( '\n' );
                }
            }
        }
        
        return new MakefileHolder( includeDirectories, makeFile.toString() );
    }

    private boolean addLibraryDetails( StringBuilder makeFile, File outputDir,
                                       Artifact artifact, String ndkArchitecture ) throws IOException
    {
        boolean apklibStatic = false;
        if ( AndroidExtension.APKLIB.equals( artifact.getType() ) )
        {
            String classifier = artifact.getClassifier();
            String architecture = ( classifier != null ) ? classifier : ndkArchitecture;
            // 
            // We assume that APKLIB contains a single static OR shared library
            // that we should link against. The follow code identifies that file.
            //
            File[] staticLibs = NativeHelper.listNativeFiles( artifact, unpackedApkLibsDirectory, 
                                                              architecture, true );
            if ( staticLibs != null && staticLibs.length > 0 )
            {
                int libIdx = findApklibNativeLibrary( staticLibs, artifact.getArtifactId() );
                apklibStatic = true;
                addLibraryDetails( makeFile, outputDir, staticLibs[libIdx], "" );
            }
            else
            {
                File[] sharedLibs = NativeHelper.listNativeFiles( artifact, unpackedApkLibsDirectory, 
                                                                  architecture, false );
                if ( sharedLibs == null )
                {
                    throw new IOException( "Failed to find any library file in APKLIB" );
                }
                int libIdx = findApklibNativeLibrary( sharedLibs, artifact.getArtifactId() );
                addLibraryDetails( makeFile, outputDir, sharedLibs[libIdx], "" );
            }
        }
        else
        {
            addLibraryDetails( makeFile, outputDir, artifact.getFile(), artifact.getArtifactId() );
        }

        return apklibStatic;
    }

    private void addLibraryDetails( StringBuilder makeFile, File outputDir, File libFile, String outputName )
        throws IOException
    {
        String localPath = resolveRelativePath( outputDir, libFile );
        localPath = localPath.substring( 0, localPath.indexOf( libFile.getName() ) - 1 );

        makeFile.append( "LOCAL_PATH := " );
        makeFile.append( localPath );
        makeFile.append( '\n' );
        makeFile.append( "LOCAL_SRC_FILES := " );
        makeFile.append( libFile.getName() );
        makeFile.append( '\n' );
        makeFile.append( "LOCAL_MODULE_FILENAME := " );
        if ( "".equals( outputName ) )
        {
            makeFile.append( FilenameUtils.removeExtension( libFile.getName() ) );
        }
        else
        {
            makeFile.append( outputName );
        }
        makeFile.append( '\n' );
    }

    /**
     * @param libs the array of possible library files. Must not be null.
     * @return the index in the array of the library to use
     * @throws IOException if a library cannot be identified
     */
    private int findApklibNativeLibrary( File[] libs, String artifactName ) throws IOException
    {
        int libIdx = -1;
        
        if ( libs.length == 1 )
        
        {
            libIdx = 0;
        }
        else
        {
            log.info( "Found multiple library files, looking for name match with artifact" );
            StringBuilder sb = new StringBuilder();
            for ( int i = 0; i < libs.length; i++ )
            {
                if ( sb.length() != 0 )
                {
                    sb.append( ", " );
                }
                sb.append( libs[i].getName() );
                if ( libs[i].getName().startsWith( "lib" + artifactName ) )
                {
                    if ( libIdx != -1 )
                    {
                        // We have multiple matches, tell the user we can't handle this ...
                        throw new IOException( "Found multiple libraries matching artifact name " + artifactName
                                + ". Please use unique artifact/library names." );
                        
                    }
                    libIdx = i;
                }
            }
            if ( libIdx < 0 )
            {
                throw new IOException( "Unable to determine main library from " + sb.toString()
                        + " APKLIB should contain only 1 library or a library matching the artifact name" );
            }
        }
        return libIdx;
    }
    
    /**
     * Resolves the relative path of the specified artifact
     *
     * @param outputDirectory typically the parent directory of the directory containing the makefile
     * @param file
     * @return
     */
    protected static String resolveRelativePath( File outputDirectory, File file ) throws IOException
    {
        String resolvedPath = file.getCanonicalPath();
        
        String strOutputDirectoryPath = outputDirectory.getCanonicalPath();
        String strFilePath = file.getCanonicalPath();
        //System.out.println( "Resolving " + strFilePath + " against " + strOutputDirectoryPath );

        if ( strFilePath.startsWith( strOutputDirectoryPath ) )
        {
            // Simple case where file is in a subdirectory of outputDirectory
            resolvedPath =  strFilePath.substring( strOutputDirectoryPath.length() + 1 );
        }
        else
        {
            // Look for commonality in paths
            List<String> outputDirectoryPathParts = splitPath( outputDirectory.getCanonicalFile() );
            List<String> filePathParts = splitPath( file.getCanonicalFile() );
            int commonDepth = 0;
            int maxCommonDepth = Math.min( outputDirectoryPathParts.size(), filePathParts.size() );
            for ( int i = 0; 
                    ( i < maxCommonDepth ) 
                    && outputDirectoryPathParts.get( i ).equals( filePathParts.get( i ) ); 
                    i++ )
            {
                commonDepth++;
            }
            // If there is a common root build a relative path between the common roots
            if ( commonDepth > 0 )
            {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append( ".." );
                for ( int i = 0; i < outputDirectoryPathParts.size() - commonDepth - 1; i++ )
                {
                    stringBuilder.append( File.separator );
                    stringBuilder.append( ".." );
                }
                for ( int i = commonDepth; i < filePathParts.size(); i++ )
                {
                    stringBuilder.append( File.separator );
                    stringBuilder.append( filePathParts.get( i ) );
                }
                resolvedPath = stringBuilder.toString();
            }
            else
            {
                if ( IS_WINDOWS )
                {
                    // Windows has no common root directory, cannot resolve a path
                    // across drives so ...
                    throw new IOException( "Unable to resolve relative path across windows drives" );
                }

                // no intersection between paths so calculate a path via the root directory
                final StringBuilder stringBuilder = new StringBuilder();
                
                File depthCheck = outputDirectory.getParentFile();
                while ( depthCheck != null )
                {
                    if ( stringBuilder.length() > 0 )
                    {
                        stringBuilder.append( File.separator );
                    }
                    stringBuilder.append( ".." );
                    depthCheck = depthCheck.getParentFile();
                }
    
                resolvedPath = stringBuilder.toString() + strFilePath;
            }
        }

        //System.out.println( "resolvedPath = " + resolvedPath );
        return resolvedPath;
    }

    /**
     * Method to split the path components of a file into a List
     * @param f the file to split
     * @return a new list containing the components of the path as strings
     */
    protected static List<String> splitPath( File f )
    {
        List<String> result;
        File parent = f.getParentFile();
        if ( parent == null )
        {
            result = new ArrayList<String>();
            if ( f.getName().length() > 0 )
            {
                // We're at the root but have a name so we have a relative path
                // for which we need to add the first component to the list
                result.add( f.getName() );
            }
            else if ( IS_WINDOWS ) 
            {
                String strF = f.toString();
                if ( strF.matches( WINDOWS_DRIVE_ROOT_REGEX ) )
                {
                    // We're on windows and the path is <Drive>:\ so we
                    // add the <Drive>: to the list
                    result.add( strF.substring( 0, strF.length() - 1 ).toUpperCase() );
                }
            }
        }
        else
        {
            result = splitPath( parent );
            result.add( f.getName() );
        }
        return result;
    }

    /**
     * Creates a list of artifacts suitable for use in the LOCAL_STATIC_LIBRARIES or LOCAL_SHARED_LIBRARIES 
     * variable in an Android makefile
     *
     * @param resolvedLibraryList
     * @param staticLibrary
     * @return a list of Ids for artifacts that include static or shared libraries
     */
    public String createLibraryList( Set<Artifact> resolvedLibraryList,
                                     String ndkArchitecture,
                                     boolean staticLibrary )
    {
        Set<String> libraryNames = new LinkedHashSet<String>();

        for ( Artifact a : resolvedLibraryList )
        {
            if ( staticLibrary && "a".equals( a.getType() ) )
            {
                libraryNames.add( a.getArtifactId() );
            }
            if ( ! staticLibrary && "so".equals( a.getType() ) )
            {
                libraryNames.add( a.getArtifactId() );
            }
            if ( AndroidExtension.APKLIB.equals( a.getType() ) || AndroidExtension.AAR.equals( a.getType() ) )
            {
                File[] libFiles = NativeHelper.listNativeFiles( a, unpackedApkLibsDirectory, 
                                                                ndkArchitecture, staticLibrary );
                if ( libFiles != null && libFiles.length > 0 )
                {
                    libraryNames.add( a.getArtifactId() );
                }
                
            }
        }

        StringBuilder sb = new StringBuilder();

        Iterator<String> iter = libraryNames.iterator();

        while ( iter.hasNext() )
        {
            sb.append( iter.next() );

            if ( iter.hasNext() )
            {
                sb.append( " " );
            }
        }

        return sb.toString();
    }
}
