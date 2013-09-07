/*
 * Copyright (C) 2009 Jayway AB
 * Copyright (C) 2007-2008 JVending Masa
 * Copyright (C) 2010 akquinet A.G.
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

import org.apache.maven.plugin.MojoExecutionException;

import com.android.sdklib.build.ApkBuilder;
import com.android.sdklib.build.ApkCreationException;
import com.android.sdklib.build.DuplicateFileException;
import com.android.sdklib.build.SealedApkException;

import java.io.File;
import java.io.PrintStream;

/**
 * This class is a front end on the APKBuilder class from sdklib. 
 * It originally wrapped and loaded with reflection. This has been removed
 * and this class can be removed once all calls are refactored out..
 */
public class ApkBuilderWrapper
{
    /**
     * The APK Builder.
     */
    private ApkBuilder apkBuilder;

    /**
     * Creates a new APKBuilder. The class must be initialized before calling
     * this constructor. This creates a new builder that will create the
     * specified output file, using the two mandatory given input files. An
     * optional debug keystore can be provided. If set, it is expected that the
     * store password is 'android' and the key alias and password are
     * 'androiddebugkey' and 'android'.
     * <p/>
     * An optional {@link PrintStream} can also be provided for verbose output.
     * If null, there will be no output.
     *
     * @param apkFile       the file to create
     * @param resFile       the file representing the packaged resource file.
     * @param dexFile       the file representing the dex file. This can be null for apk
     *                      with no code.
     * @param signed        if the APK must be signed using the debug store
     * @param verboseStream the stream to which verbose output should go. If null, verbose
     *                      mode is not enabled.
     * @throws MojoExecutionException if the class was not initialized, or if the reflective calls
     *                                failed.
     */
    public ApkBuilderWrapper( File apkFile, File resFile, File dexFile, boolean signed, PrintStream verboseStream )
            throws MojoExecutionException
    {
            // We need to first get the debug key store
            String debugKeyStore;
            try 
            {
                debugKeyStore = ApkBuilder.getDebugKeystore();
                apkBuilder = 
                    new ApkBuilder( apkFile, resFile, dexFile, ( signed ) ? debugKeyStore : null, verboseStream );
            } 
            catch ( ApkCreationException e ) 
            {
                throw new MojoExecutionException( e.getMessage() );
            }
    }

    /**
     * Enables / Disables the debug mode.
     *
     * @param debug does the debug mode need to be enabled?
     * @throws MojoExecutionException if the debug mode cannot be set
     */
    public void setDebugMode( boolean debug ) throws MojoExecutionException
    {
        apkBuilder.setDebugMode( debug );
    }

    /**
     * Adds the resources from a source folder to the APK.
     *
     * @param sourceFolder the source folder
     * @throws MojoExecutionException if the source folder cannot be added
     */
    public void addSourceFolder( File sourceFolder ) throws MojoExecutionException
    {
        try 
        {
            apkBuilder.addSourceFolder( sourceFolder );
        }
        catch ( DuplicateFileException e )
        {
            throw new MojoExecutionException( e.getMessage() );
        }
        catch ( ApkCreationException e )
        {
            throw new MojoExecutionException( e.getMessage() );
        }
        catch ( SealedApkException e )
        {
            throw new MojoExecutionException( e.getMessage() );
        }
    }

    /**
     * Adds the resources from a jar file.
     *
     * @param jarFile the jar File.
     * @throws MojoExecutionException if the resources cannot be added
     */
    public void addResourcesFromJar( File jarFile ) throws MojoExecutionException
    {
        try 
        {
            apkBuilder.addResourcesFromJar( jarFile );
        }
        catch ( DuplicateFileException e )
        {
            throw new MojoExecutionException( e.getMessage() );
        }
        catch ( ApkCreationException e )
        {
            throw new MojoExecutionException( e.getMessage() );
        }
        catch ( SealedApkException e )
        {
            throw new MojoExecutionException( e.getMessage() );
        }
    }

    /**
     * Adds the native libraries from the top native folder. The content of this
     * folder must be the various ABI folders.
     * <p/>
     * This may or may not copy gdbserver into the apk based on whether the
     * debug mode is set.
     *
     * @param nativeFolder the native folder.
     * @param abiFilter    an optional filter. If not null, then only the matching ABI is
     *                     included in the final archive
     * @throws MojoExecutionException if the library cannot be added.
     */
    public void addNativeLibraries( File nativeFolder, String abiFilter ) 
        throws MojoExecutionException
    {
        try 
        {
            apkBuilder.addNativeLibraries( nativeFolder );
        }
        catch ( DuplicateFileException e )
        {
            throw new MojoExecutionException( e.getMessage() );
        }
        catch ( ApkCreationException e )
        {
            throw new MojoExecutionException( e.getMessage() );
        }
        catch ( SealedApkException e )
        {
            throw new MojoExecutionException( e.getMessage() );
        }
    }

    /**
     * Seals the APK. Once called the APK cannot be modified.
     *
     * @throws MojoExecutionException if the APK cannot be sealed
     */
    public void sealApk() throws MojoExecutionException
    {
        try 
        {
            apkBuilder.sealApk();
        }
        catch ( ApkCreationException e )
        {
            throw new MojoExecutionException( e.getMessage() );
        }
        catch ( SealedApkException e )
        {
            throw new MojoExecutionException( e.getMessage() );
        }
    }
}
