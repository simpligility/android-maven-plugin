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

import com.jayway.maven.plugins.android.AbstractAndroidMojo;
import com.jayway.maven.plugins.android.CommandExecutor;
import com.jayway.maven.plugins.android.ExecutionException;
import com.jayway.maven.plugins.android.config.PullParameter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.util.DefaultFileSet;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.jayway.maven.plugins.android.common.AndroidExtension.APKLIB;


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
     * Defines the architecture for the NDK build
     *
     * @parameter expression="${android.ndk.build.architecture}" default-value="armeabi"
     */
    @PullParameter( defaultValue = "armeabi" )
    private String ndkArchitecture;
    
    /**
     *
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        generateIntermediateApk();

        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger( this.getLog() );

        File outputFile = createApkLibraryFile();

        // Set the generated .apklib file as the main artifact (because the pom states <packaging>apklib</packaging>)
        project.getArtifact().setFile( outputFile );
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
            addDirectory( jarArchiver, sourceDirectory, "src" );
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
            addDirectory( jarArchiver, nativeLibrariesDirectory, NATIVE_LIBRARIES_FOLDER );

            // Add native libraries built in this build and dependencies
            final File outputDirectory = new File( project.getBuild().getDirectory() );
            String prefix = NATIVE_LIBRARIES_FOLDER + "/" + ndkArchitecture; // path in archive file must have '/'
            addSharedLibraries( jarArchiver, outputDirectory, prefix );
            final File dependentLibs = new File( ndkOutputDirectory.getAbsolutePath(), ndkArchitecture );
            addSharedLibraries( jarArchiver, dependentLibs, prefix );

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
                final String apkLibUnpackBasePath = unpackedApkLibsDirectory.getCanonicalPath();
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
            jarArchiver.addFileSet( fileSet );
            getLog().debug( "Added files from " + directory );
        }
    }
    
    /**
     * Adds all shared libraries (.so) to a {@link JarArchiver} under 'libs'.
     * 
     * @param jarArchiver The jarArchiver to add files to
     * @param directory   The directory to scan for .so files
     * @param prefix      The prefix for where in the jar the .so files will go.
     */
    protected void addSharedLibraries( JarArchiver jarArchiver, File directory, String prefix )
    {
        getLog().debug( "Searching for shared libraries in " + directory );
        File[] libFiles = directory.listFiles( new FilenameFilter()
        {
            public boolean accept( final File dir, final String name )
            {
                return name.startsWith( "lib" ) && name.endsWith( ".so" );
            }
        } );
        for ( File libFile : libFiles ) 
        {
            String dest = prefix + "/" + libFile.getName();
            getLog().debug( "Adding " + libFile + " as " + dest );
            jarArchiver.addFile( libFile, dest );
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
        File[] overlayDirectories;

        if ( resourceOverlayDirectories == null || resourceOverlayDirectories.length == 0 )
        {
            overlayDirectories = new File[]{ resourceOverlayDirectory };
        }
        else
        {
            overlayDirectories = resourceOverlayDirectories;
        }

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
        if ( combinedRes.exists() )
        {
            commands.add( "-S" );
            commands.add( combinedRes.getAbsolutePath() );
        }
        else
        {
            if ( resourceDirectory.exists() )
            {
                commands.add( "-S" );
                commands.add( resourceDirectory.getAbsolutePath() );
            }
        }
        for ( Artifact apkLibraryArtifact : getAllRelevantDependencyArtifacts() )
        {
            if ( apkLibraryArtifact.getType().equals( APKLIB ) )
            {
                commands.add( "-S" );
                commands.add( getLibraryUnpackDirectory( apkLibraryArtifact ) + "/res" );
            }
        }
        commands.add( "--auto-add-overlay" );
        if ( assetsDirectory.exists() )
        {
            commands.add( "-A" );
            commands.add( assetsDirectory.getAbsolutePath() );
        }
        if ( extractedDependenciesAssets.exists() )
        {
            commands.add( "-A" );
            commands.add( extractedDependenciesAssets.getAbsolutePath() );
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
        getLog().info( getAndroidSdk().getPathForTool( "aapt" ) + " " + commands.toString() );
        try
        {
            executor.executeCommand( getAndroidSdk().getPathForTool( "aapt" ), commands, project.getBasedir(), false );
        }
        catch ( ExecutionException e )
        {
            throw new MojoExecutionException( "", e );
        }
    }

}
