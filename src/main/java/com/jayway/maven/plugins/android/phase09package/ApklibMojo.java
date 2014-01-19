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

import static com.jayway.maven.plugins.android.common.AndroidExtension.APKLIB;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.util.DefaultFileSet;

import com.jayway.maven.plugins.android.AbstractAndroidMojo;
import com.jayway.maven.plugins.android.CommandExecutor;
import com.jayway.maven.plugins.android.ExecutionException;
import com.jayway.maven.plugins.android.common.NativeHelper;
import com.jayway.maven.plugins.android.config.PullParameter;


/**
 * Creates the apklib file.<br/>
 * apklib files do not generate deployable artifacts.
 *
 * @author nmaiorana@gmail.com
 * @goal apklib
 * @phase package
 * @requiresDependencyResolution compile
 */
public class ApklibMojo extends AbstractAndroidMojo
{
    /**
     * The name of the top level folder in the APKLIB where native libraries are found.
     * NOTE: This is inconsistent with APK where the folder is called "lib"
     */
    public static final String NATIVE_LIBRARIES_FOLDER = "libs";
    
    /**
     * Build folder to place built native libraries into
     *
     * @parameter expression="${android.ndk.build.ndk-output-directory}"
     * default-value="${project.build.directory}/ndk-libs"
     */
    private File ndkOutputDirectory;
    
    /**
     * <p>Classifier to add to the artifact generated. If given, the artifact will be an attachment instead.</p>
     *
     * @parameter
     */
    private String classifier;

    /**
     * Specifies the application makefile to use for the build (if other than the default Application.mk).
     *
     * @parameter
     */
    @PullParameter
    private String applicationMakefile;

    /**
     * Defines the architecture for the NDK build
     *
     * @parameter expression="${android.ndk.build.architecture}"
     */
    @PullParameter
    private String ndkArchitecture;

    /**
     * Specifies the classifier with which the artifact should be stored in the repository
     *
     * @parameter expression="${android.ndk.build.native-classifier}"
     */
    @PullParameter
    private String ndkClassifier;
    
    private List<String> sourceFolders = new ArrayList<String>();

    /**
     *
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        String out = project.getBuild().getDirectory();
        for ( String src : project.getCompileSourceRoots() )
        {
            if ( !src.startsWith( out ) ) 
            {
                sourceFolders.add( src );
            }
        }
        
        generateIntermediateApk();

        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger( this.getLog() );

        File outputFile = createApkLibraryFile();

        if ( classifier == null )
        {
            // Set the generated file as the main artifact (because the pom states <packaging>apklib</packaging>)
            project.getArtifact().setFile( outputFile );
        }
        else
        {
            // If there is a classifier specified, attach the artifact using that
            projectHelper.attachArtifact( project, outputFile, classifier );
        }
    }

    /**
     *
     * @return
     * @throws MojoExecutionException
     */
    protected File createApkLibraryFile() throws MojoExecutionException
    {
        final File apklibrary = new File( project.getBuild().getDirectory(),
                project.getBuild().getFinalName() + "." + APKLIB );
        FileUtils.deleteQuietly( apklibrary );

        try
        {
            JarArchiver jarArchiver = new JarArchiver();
            jarArchiver.setDestFile( apklibrary );

            jarArchiver.addFile( androidManifestFile, "AndroidManifest.xml" );
            addDirectory( jarArchiver, assetsDirectory, "assets" );
            addDirectory( jarArchiver, resourceDirectory, "res" );
            
            for ( String src : sourceFolders ) 
            { 
                addDirectory( jarArchiver, new File( src ), "src" );
            }
            
            File[] overlayDirectories = getResourceOverlayDirectories();
            for ( File resOverlayDir : overlayDirectories )
            {
                if ( resOverlayDir != null && resOverlayDir.exists() )
                {
                    addDirectory( jarArchiver, resOverlayDir, "res" );
                }
            }

            addJavaResources( jarArchiver, project.getBuild().getResources(), "src" );

            // Lastly, add any native libraries
            addNativeLibraries( jarArchiver );

            jarArchiver.createArchive();
        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException( "ArchiverException while creating ." + APKLIB + " file.", e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "IOException while creating ." + APKLIB + " file.", e );
        }

        return apklibrary;
    }

    private void addNativeLibraries( final JarArchiver jarArchiver ) throws MojoExecutionException
    {

        try
        {
            if ( nativeLibrariesDirectory.exists() )
            {
                getLog().info( nativeLibrariesDirectory + " exists, adding libraries." );
                addDirectory( jarArchiver, nativeLibrariesDirectory, NATIVE_LIBRARIES_FOLDER );
            }
            else
            {
                getLog().info( nativeLibrariesDirectory 
                        + " does not exist, looking for libraries in target directory." );
                // Add native libraries built and attached in this build
                String[] ndkArchitectures = NativeHelper.getNdkArchitectures( ndkArchitecture,
                                                                              applicationMakefile,
                                                                              project.getBasedir() );
                for ( String ndkArchitecture : ndkArchitectures )
                {
                    final File ndkLibsDirectory = new File( ndkOutputDirectory, ndkArchitecture );
                    addSharedLibraries( jarArchiver, ndkLibsDirectory, ndkArchitecture );
                
                    // Add native library dependencies
                    // FIXME: Remove as causes duplicate libraries when building final APK if this set includes
                    //        libraries from dependencies of the APKLIB
                    //final File dependentLibs = new File( ndkOutputDirectory.getAbsolutePath(), ndkArchitecture );
                    //addSharedLibraries( jarArchiver, dependentLibs, prefix );

                    // get native libs from other apklibs
                    for ( Artifact apkLibraryArtifact : getAllRelevantDependencyArtifacts() )
                    {
                        if ( apkLibraryArtifact.getType().equals( APKLIB ) )
                        {
                            final File apklibLibsDirectory = new File(
                                    getUnpackedLibNativesFolder( apkLibraryArtifact ), ndkArchitecture );
                            if ( apklibLibsDirectory.exists() )
                            {
                                addSharedLibraries( jarArchiver, apklibLibsDirectory, ndkArchitecture );
                            }
                        }
                    }
                }
            }
        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException( "IOException while creating ." + APKLIB + " file.", e );
        }
        // TODO: Next is to check for any:
        // TODO: - compiled in (as part of this build) libs
        // TODO:    - That is of course easy if the artifact is indeed attached
        // TODO:    - If not attached, it gets a little trickier  - check the target dir for any compiled .so files (generated by NDK mojo)
        // TODO:        - But where is that directory configured?
    }

    protected void addJavaResources( JarArchiver jarArchiver, List<Resource> javaResources, String prefix )
            throws IOException
    {
        for ( Resource javaResource : javaResources )
        {
            addJavaResource( jarArchiver, javaResource, prefix );
        }
    }

    /**
     * Adds a Java Resources directory (typically "src/main/resources") to a {@link JarArchiver}.
     *
     * @param jarArchiver
     * @param javaResource The Java resource to add.
     * @param prefix       An optional prefix for where in the Jar file the directory's contents should go.
     * @throws IOException in case the resource path can not be resolved
     */
    protected void addJavaResource( JarArchiver jarArchiver, Resource javaResource, String prefix )
            throws IOException
    {
        if ( javaResource != null )
        {
            final File javaResourceDirectory = new File( javaResource.getDirectory() );
            if ( javaResourceDirectory.exists() )
            {
                final String resourcePath = javaResourceDirectory.getCanonicalPath();
                final String apkLibUnpackBasePath = unpackedLibsDirectory.getCanonicalPath();
                // Don't include our dependencies' resource dirs.
                if ( ! resourcePath.startsWith( apkLibUnpackBasePath ) )
                {
                    final DefaultFileSet javaResourceFileSet = new DefaultFileSet();
                    javaResourceFileSet.setDirectory( javaResourceDirectory );
                    javaResourceFileSet.setPrefix( endWithSlash( prefix ) );
                    jarArchiver.addFileSet( javaResourceFileSet );
                }
            }
        }
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
     * @param jarArchiver
     * @param directory   The directory to add.
     * @param prefix      An optional prefix for where in the Jar file the directory's contents should go.
     */
    protected void addDirectory( JarArchiver jarArchiver, File directory, String prefix )
    {
        if ( directory != null && directory.exists() )
        {
            final DefaultFileSet fileSet = new DefaultFileSet();
            fileSet.setPrefix( endWithSlash( prefix ) );
            fileSet.setDirectory( directory );

            // XXX: trying to avoid duplicated sources
            fileSet.setExcludes( new String[] { "**/R.java", "**/BuildConfig.java" } );

            jarArchiver.addFileSet( fileSet );
            getLog().debug( "Added files from " + directory );
        }
    }
    
    /**
     * Adds all shared libraries (.so) to a {@link JarArchiver} under 'libs'.
     * 
     * @param jarArchiver The jarArchiver to add files to
     * @param directory   The directory to scan for .so files
     * @param ndkArchitecture      The prefix for where in the jar the .so files will go.
     */
    protected void addSharedLibraries( JarArchiver jarArchiver, File directory, String ndkArchitecture )
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
                String dest = "libs/" + ndkArchitecture + "/" + libFile.getName();
                getLog().debug( "Adding " + libFile + " as " + dest );
                jarArchiver.addFile( libFile, dest );
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

        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger( this.getLog() );
        File[] overlayDirectories = getResourceOverlayDirectories();

        File androidJar = getAndroidSdk().getAndroidJar();
        File outputFile = new File( project.getBuild().getDirectory(), project.getBuild().getFinalName() + ".ap_" );

        List<String> commands = new ArrayList<String>();
        commands.add( "package" );
        commands.add( "-f" );
        commands.add( "-M" );
        commands.add( androidManifestFile.getAbsolutePath() );
        for ( File resOverlayDir : overlayDirectories )
        {
            if ( resOverlayDir != null && resOverlayDir.exists() )
            {
                commands.add( "-S" );
                commands.add( resOverlayDir.getAbsolutePath() );
            }
        }
        if ( resourceDirectory.exists() )
        {
            commands.add( "-S" );
            commands.add( resourceDirectory.getAbsolutePath() );
        }
        for ( Artifact apkLibraryArtifact : getAllRelevantDependencyArtifacts() )
        {
            if ( apkLibraryArtifact.getType().equals( APKLIB ) )
            {
                final File apklibResDirectory = getUnpackedLibResourceFolder( apkLibraryArtifact );
                if ( apklibResDirectory.exists() )
                {
                    commands.add( "-S" );
                    commands.add( apklibResDirectory.getAbsolutePath() );
                }
            }
        }
        commands.add( "--auto-add-overlay" );

        // NB aapt only accepts a single assets parameter - combinedAssets is a merge of all assets
        if ( combinedAssets.exists() )
        {
            getLog().debug( "Adding assets folder : " + combinedAssets );
            commands.add( "-A" );
            commands.add( combinedAssets.getAbsolutePath() );
        }

        commands.add( "-I" );
        commands.add( androidJar.getAbsolutePath() );
        commands.add( "-F" );
        commands.add( outputFile.getAbsolutePath() );
        if ( StringUtils.isNotBlank( configurations ) )
        {
            commands.add( "-c" );
            commands.add( configurations );
        }
        getLog().info( getAndroidSdk().getAaptPath() + " " + commands.toString() );
        try
        {
            executor.executeCommand( getAndroidSdk().getAaptPath(), commands, project.getBasedir(), false );
        }
        catch ( ExecutionException e )
        {
            throw new MojoExecutionException( "", e );
        }
    }

}
