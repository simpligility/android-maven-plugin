/*
 * Copyright (C) 2009 Jayway AB
 * Copyright (C) 2007-2008 JVending Masa
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jayway.maven.plugins.android.phase09package;

import com.android.SdkConstants;
import com.jayway.maven.plugins.android.AbstractAndroidMojo;
import com.jayway.maven.plugins.android.CommandExecutor;
import com.jayway.maven.plugins.android.ExecutionException;
import com.jayway.maven.plugins.android.common.AaptCommandBuilder;
import com.jayway.maven.plugins.android.common.NativeHelper;
import com.jayway.maven.plugins.android.config.PullParameter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.codehaus.plexus.archiver.zip.ZipArchiver;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.jayway.maven.plugins.android.common.AndroidExtension.AAR;
import static com.jayway.maven.plugins.android.common.AndroidExtension.APKLIB;


/**
 * Creates an Android Archive (aar) file.<br/>
 */
@Mojo(
        name = "aar",
        defaultPhase = LifecyclePhase.VERIFY,
        requiresDependencyResolution = ResolutionScope.COMPILE
)
public class AarMojo extends AbstractAndroidMojo
{
    /**
     * The name of the top level folder in the AAR where native libraries are found.
     * NOTE: This is inconsistent with APK where the folder is called "lib", and does not match APKLIB
     * layout either, where the folder is called "libs".
     */
    public static final String NATIVE_LIBRARIES_FOLDER = "jni";

    /**
     * <p>Classifier to add to the artifact generated. If given, the artifact will be an attachment instead.</p>
     */
    @Parameter
    private String classifier;

    /**
     * Specifies the application makefile to use for the build (if other than the default Application.mk).
     */
    @Parameter
    @PullParameter
    private String applicationMakefile;

    /**
     * Defines the architecture for the NDK build
     */
    @Parameter( property = "android.ndk.build.architecture" )
    @PullParameter
    private String ndkArchitecture;

    /**
     * Specifies the classifier with which the artifact should be stored in the repository
     */
    @Parameter( property = "android.ndk.build.native-classifier" )
    @PullParameter
    private String ndkClassifier;

    /**
     * Specifies the files that should be included in the classes.jar within the aar
     */
    @Parameter
    @PullParameter
    private String[] classesJarIncludes = new String[]{"**/*"};

    /**
     * Specifies the files that should be excluded from the classes.jar within the aar
     */
    @Parameter
    @PullParameter
    private String[] classesJarExcludes = new String[]{"**/R.class", "**/R$*.class"};

    @Parameter(
            property = "android.proguard.obfuscatedJar",
            defaultValue = "${project.build.directory}/${project.build.finalName}_obfuscated.jar"
    )
    private String obfuscatedJar;

    private List<String> sourceFolders = new ArrayList<String>();

    /**
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        String out = targetDirectory.getPath();
        for ( String src : project.getCompileSourceRoots() )
        {
            if ( !src.startsWith( out ) )
            {
                sourceFolders.add( src );
            }
        }

        getLog().info( "Generating AAR file : " + project.getArtifactId() );
        generateIntermediateApk();

        final File outputFile = createAarLibraryFile( createAarClassesJar() );

        if ( classifier == null )
        {
            // Set the generated file as the main artifact (because the pom states <packaging>aar</packaging>)
            project.getArtifact().setFile( outputFile );
        }
        else
        {
            // If there is a classifier specified, attach the artifact using that
            projectHelper.attachArtifact( project, outputFile, classifier );
        }
    }

    /**
     * Creates an appropriate aar/classes.jar that does not include R
     *
     * @return File which is the AAR classes jar.
     * @throws MojoExecutionException
     */
    protected File createAarClassesJar() throws MojoExecutionException
    {
        final File obfuscatedJarFile = new File( obfuscatedJar );
        if ( obfuscatedJarFile.exists() )
        {
            return obfuscatedJarFile;
        }

        final File classesJar = new File( targetDirectory, finalName + ".aar.classes.jar" );
        try
        {
            JarArchiver jarArchiver = new JarArchiver();
            jarArchiver.setDestFile( classesJar );
            jarArchiver.addDirectory( projectOutputDirectory,
                    classesJarIncludes,
                    classesJarExcludes );
            jarArchiver.createArchive();
            return classesJar;
        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException( "ArchiverException while creating ." + classesJar + " file.", e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "IOException while creating ." + classesJar + " file.", e );
        }

    }

    /**
     * @return AAR file.
     * @throws MojoExecutionException
     */
    protected File createAarLibraryFile( File classesJar ) throws MojoExecutionException
    {
        final File aarLibrary = new File( targetDirectory,
                finalName + "." + AAR );
        FileUtils.deleteQuietly( aarLibrary );

        try
        {
            final ZipArchiver zipArchiver = new ZipArchiver();
            zipArchiver.setDestFile( aarLibrary );

            zipArchiver.addFile( destinationManifestFile, "AndroidManifest.xml" );
            addDirectory( zipArchiver, assetsDirectory, "assets" );
            addDirectory( zipArchiver, resourceDirectory, "res" );
            zipArchiver.addFile( classesJar, SdkConstants.FN_CLASSES_JAR );

            final File[] overlayDirectories = getResourceOverlayDirectories();
            for ( final File resOverlayDir : overlayDirectories )
            {
                if ( resOverlayDir != null && resOverlayDir.exists() )
                {
                    addDirectory( zipArchiver, resOverlayDir, "res" );
                }
            }

            addR( zipArchiver );

            // Lastly, add any native libraries
            addNativeLibraries( zipArchiver );

            zipArchiver.createArchive();
        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException( "ArchiverException while creating ." + AAR + " file.", e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "IOException while creating ." + AAR + " file.", e );
        }

        return aarLibrary;
    }

    private void addR( ZipArchiver zipArchiver ) throws MojoExecutionException
    {
        final File rFile = new File( targetDirectory + "/R.txt" );
        if ( rFile.exists() )
        {
            zipArchiver.addFile( rFile, "R.txt" );
            getLog().debug( "Packaging R.txt in AAR" );
        }
        else
        {
            getLog().debug( "Not packaging R.txt in AAR - it does not exist (no resources??)" );
        }
    }

    private void addNativeLibraries( final ZipArchiver zipArchiver ) throws MojoExecutionException
    {
        try
        {
            if ( nativeLibrariesDirectory.exists() )
            {
                getLog().info( nativeLibrariesDirectory + " exists, adding libraries." );
                addDirectory( zipArchiver, nativeLibrariesDirectory, NATIVE_LIBRARIES_FOLDER );
            }
            else
            {
                getLog().info( nativeLibrariesDirectory
                        + " does not exist, looking for libraries in target directory." );
                // Add native libraries built and attached in this build
                String[] ndkArchitectures = NativeHelper.getNdkArchitectures( ndkArchitecture,
                                                                              applicationMakefile,
                                                                              project.getBasedir() );
                for ( String architecture : ndkArchitectures )
                {
                    final File ndkLibsDirectory = new File( ndkOutputDirectory, architecture );
                    addSharedLibraries( zipArchiver, ndkLibsDirectory, architecture );

                    // Add native library dependencies
                    // FIXME: Remove as causes duplicate libraries when building final APK if this set includes
                    //        libraries from dependencies of the AAR
                    //final File dependentLibs = new File( ndkOutputDirectory.getAbsolutePath(), ndkArchitecture );
                    //addSharedLibraries( jarArchiver, dependentLibs, prefix );
                }
            }
        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException( "IOException while creating ." + AAR + " file.", e );
        }
        // TODO: Next is to check for any:
        // TODO: - compiled in (as part of this build) libs
        // TODO:    - That is of course easy if the artifact is indeed attached
        // TODO:    - If not attached, it gets a little trickier  - check the target dir for any compiled .so files (generated by NDK mojo)
        // TODO:        - But where is that directory configured?
    }

    /**
     * Makes sure the string ends with "/"
     *
     * @param prefix any string, or null.
     * @return the prefix with a "/" at the end, never null.
     */
    protected String endWithSlash( String prefix )
    {
        prefix = StringUtils.defaultIfEmpty( prefix, "/" );
        if ( ! prefix.endsWith( "/" ) )
        {
            prefix = prefix + "/";
        }
        return prefix;
    }

    /**
     * Adds a directory to a {@link JarArchiver} with a directory prefix.
     *
     * @param zipArchiver   ZipArchiver to use to archive the file.
     * @param directory     The directory to add.
     * @param prefix        An optional prefix for where in the Jar file the directory's contents should go.
     */
    protected void addDirectory( ZipArchiver zipArchiver, File directory, String prefix )
    {
        if ( directory != null && directory.exists() )
        {
            final DefaultFileSet fileSet = new DefaultFileSet();
            fileSet.setPrefix( endWithSlash( prefix ) );
            fileSet.setDirectory( directory );
            zipArchiver.addFileSet( fileSet );
            getLog().debug( "Added files from " + directory );
        }
    }

    /**
     * Adds all shared libraries (.so) to a {@link JarArchiver} under 'jni'.
     *
     * @param zipArchiver The jarArchiver to add files to
     * @param directory   The directory to scan for .so files
     * @param architecture      The prefix for where in the jar the .so files will go.
     */
    protected void addSharedLibraries( ZipArchiver zipArchiver, File directory, String architecture )
    {
        getLog().debug( "Searching for shared libraries in " + directory );
        File[] libFiles = directory.listFiles( new FilenameFilter()
        {
            public boolean accept( final File dir, final String name )
            {
                return name.startsWith( "lib" ) && name.endsWith( ".so" );
            }
        } );

        if ( libFiles != null )
        {
            for ( File libFile : libFiles )
            {
                String dest = NATIVE_LIBRARIES_FOLDER + "/" + architecture + "/" + libFile.getName();
                getLog().debug( "Adding " + libFile + " as " + dest );
                zipArchiver.addFile( libFile, dest );
            }
        }
    }

    /**
     * Generates an intermediate apk file (actually .ap_) containing the resources and assets.
     *
     * @throws MojoExecutionException
     */
    private void generateIntermediateApk() throws MojoExecutionException
    {
        // Have to generate the AAR against the dependent resources or build will fail if any local resources
        // directly reference any of the dependent resources. NB this does NOT include the dep resources in the AAR.
        List<File> dependenciesResDirectories = new ArrayList<File>();
        for ( Artifact libraryArtifact : getTransitiveDependencyArtifacts( APKLIB, AAR ) )
        {
            final File apkLibResDir = getUnpackedLibResourceFolder( libraryArtifact );
            if ( apkLibResDir.exists() )
            {
                dependenciesResDirectories.add( apkLibResDir );
            }
        }

        final CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger( this.getLog() );

        File outputFile = new File( targetDirectory, finalName + ".ap_" );

        final AaptCommandBuilder commandBuilder = AaptCommandBuilder
                .packageResources( getLog() )
                .makePackageDirectories()
                .forceOverwriteExistingFiles()
                .setPathToAndroidManifest( destinationManifestFile )
                .addResourceDirectoriesIfExists( getResourceOverlayDirectories() )
                .addResourceDirectoryIfExists( resourceDirectory )
                .addResourceDirectoriesIfExists( dependenciesResDirectories )
                .autoAddOverlay()
                .addRawAssetsDirectoryIfExists( combinedAssets )
                .addExistingPackageToBaseIncludeSet( getAndroidSdk().getAndroidJar() )
                .setOutputApkFile( outputFile )
                .addConfigurations( configurations )
                .setResourceConstantsFolder( genDirectory )
                .makeResourcesNonConstant()
                .generateRTextFile( targetDirectory )
                .setVerbose( aaptVerbose );

        getLog().debug( getAndroidSdk().getAaptPath() + " " + commandBuilder.toString() );
        getLog().info( "Generating aar" );
        try
        {
            executor.setCaptureStdOut( true );
            final List<String> commands = commandBuilder.build();
            executor.executeCommand( getAndroidSdk().getAaptPath(), commands, project.getBasedir(), false );
        }
        catch ( ExecutionException e )
        {
            throw new MojoExecutionException( "", e );
        }
    }
}
