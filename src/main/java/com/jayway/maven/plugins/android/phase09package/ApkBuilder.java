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
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * This class is a front end on the APKBuilder contained in the
 * {@code sdklib.jar}. as there is now easy way to use this jar, it it
 * dynamically loaded and reflection is used to delegate the methods.
 */
public class ApkBuilder
{

    /**
     * The ApkBuilder class object.
     */
    @SuppressWarnings( "rawtypes" )
    private static Class apkBuilderClass;

    /**
     * The Mojo logger.
     */
    private static Log log;

    /**
     * Before being able to use the ApkBuilder, an initialization is required.
     *
     * @param log     the Mojo Logger
     * @param sdkLibs the File pointing on {@code sdklib.jar}
     * @throws MojoExecutionException if the ApkBuilder class cannot be loaded
     */
    @SuppressWarnings( "unchecked" )
    public static void initialize( Log log, File sdkLibs ) throws MojoExecutionException
    {
        if ( apkBuilderClass != null )
        {
            // Already initialized
            return;
        }

        ApkBuilder.log = log;

        // Loads the ApkBuilder class
        try
        {
            URLClassLoader child =
                    new URLClassLoader( new URL[]{ sdkLibs.toURI().toURL() }, ApkBuilder.class.getClassLoader() );
            apkBuilderClass = child.loadClass( "com.android.sdklib.build.ApkBuilder" );
        } catch ( MalformedURLException e )
        {
            // This one cannot happen.
            throw new RuntimeException( "Cannot create a correct URL from file " + sdkLibs.getAbsolutePath() );
        } catch ( ClassNotFoundException e )
        {
            log.error( e );
            throw new MojoExecutionException( "Cannot load 'com.android.sdklib.build.ApkBuilder'" );
        }
        log.debug( "ApkBuilder loaded " + apkBuilderClass );

        // In order to improve performence and to check that all methods are available
        // we cache used methods.


        try
        {
            apkBuilderConstructor = apkBuilderClass.getConstructor(
                    new Class[]{ File.class, File.class, File.class, String.class, PrintStream.class } );

            setDebugMethod = apkBuilderClass.getMethod( "setDebugMode", new Class[]{ Boolean.TYPE } );

            addResourcesFromJarMethod = apkBuilderClass.getMethod( "addResourcesFromJar", new Class[]{ File.class } );

            //The addNativeLibraries signature changed for api 14
            Method[] builderMethods = apkBuilderClass.getMethods();
            for ( Method method : builderMethods )
            {
                if ( "addNativeLibraries".equals( method.getName() ) )
                {
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    //The old method (pre v14) took a second string parameter.
                    if ( parameterTypes.length == 2 )
                    {
                        if ( parameterTypes[ 0 ] == File.class && parameterTypes[ 1 ] == String.class )
                        {
                            addNativeLibrariesMethod = method;
                            break;
                        }
                    }
                    else if ( parameterTypes.length == 1 )
                    {
                        if ( parameterTypes[ 0 ] == File.class )
                        {
                            addNativeLibrariesMethod = method;
                            break;
                        }
                    }
                }
            }

            addSourceFolderMethod = apkBuilderClass.getMethod( "addSourceFolder", new Class[]{ File.class } );

            sealApkMethod = apkBuilderClass.getMethod( "sealApk", new Class[ 0 ] );

            getDebugKeyStoreMethod = apkBuilderClass.getMethod( "getDebugKeystore", new Class[ 0 ] );
        } catch ( Exception e )
        {
            log.error( "Cannot find required method", e );
            throw new MojoExecutionException( "Cannot find the required method", e );
        }
    }

    /**
     * The APK Builder.
     */
    private Object builder;

    /**
     * The APKBuilder Constructor Method.
     */
    @SuppressWarnings( "rawtypes" )
    private static Constructor apkBuilderConstructor;

    /**
     * The ApkBuilder.addSourceFolder Method.
     */
    private static Method addSourceFolderMethod;

    /**
     * The ApkBuilder.addResourcesFromJar Method.
     */
    private static Method addResourcesFromJarMethod;

    /**
     * The ApkBuilder.addNativeLibraries Method.
     */
    private static Method addNativeLibrariesMethod;

    /**
     * The ApkBuilder.setDebug Method.
     */
    private static Method setDebugMethod;

    /**
     * The ApkBuilder.sealApk Method.
     */
    private static Method sealApkMethod;

    /**
     * The ApkBuilder.getDebugKeyStore Method.
     */
    private static Method getDebugKeyStoreMethod;

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
    public ApkBuilder( File apkFile, File resFile, File dexFile, boolean signed, PrintStream verboseStream )
            throws MojoExecutionException
    {
        if ( apkBuilderClass == null )
        {
            throw new MojoExecutionException( "The APKBuilder class was not initialized" );
        }

        try
        {
            // We need to first get the debug key store
            Object debugKeyStore = getDebugKeyStoreMethod.invoke( null, new Object[ 0 ] );

            builder = apkBuilderConstructor.newInstance(
                    new Object[]{ apkFile, resFile, dexFile, ( signed ) ? debugKeyStore : null, verboseStream } );
        } catch ( InvocationTargetException e )
        {
            log.error( "Cannot create the APKBuilder object", e.getCause() );
            throw new MojoExecutionException( "Cannot create the APKBuilder object", e.getCause() );
        } catch ( Exception e )
        {
            log.error( "Cannot create the APKBuilder object", e );
            throw new MojoExecutionException( "Cannot create the APKBuilder object", e );
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
        try
        {
            setDebugMethod.invoke( builder, new Object[]{ debug } );
        } catch ( InvocationTargetException e )
        {
            log.error( "Cannot set the debug mode", e.getCause() );
            throw new MojoExecutionException( "Cannot create the APKBuilder object", e.getCause() );
        } catch ( Exception e )
        {
            log.error( "Cannot set the debug mode", e );
            throw new MojoExecutionException( "Cannot create the APKBuilder object", e );
        }
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
            addSourceFolderMethod.invoke( builder, new Object[]{ sourceFolder } );
        } catch ( InvocationTargetException e )
        {
            log.error( "Cannot add source folder", e.getCause() );
            throw new MojoExecutionException( "Cannot add source folder", e.getCause() );
        } catch ( Exception e )
        {
            log.error( "Cannot add source folder", e );
            throw new MojoExecutionException( "Cannot add source folder", e );
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
            addResourcesFromJarMethod.invoke( builder, new Object[]{ jarFile } );
        } catch ( InvocationTargetException e )
        {
            final String message = "Cannot add resources from " + jarFile.getAbsolutePath();
            log.error( message, e.getCause() );
            throw new MojoExecutionException( message, e.getCause() );
        } catch ( Exception e )
        {
            log.error( "Cannot add source folder", e );
            throw new MojoExecutionException( "Cannot add resources from jar", e );
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
    public void addNativeLibraries( File nativeFolder, String abiFilter ) throws MojoExecutionException
    {
        try
        {
            //The method changed with version 14 of the ADK.
            //The pre-14 version took a second abiFilter string parameter.
            if ( addNativeLibrariesMethod.getParameterTypes().length == 2 )
            {
                addNativeLibrariesMethod.invoke( builder, new Object[]{ nativeFolder, abiFilter } );
            }
            else
            {
                addNativeLibrariesMethod.invoke( builder, new Object[]{ nativeFolder } );
            }
        } catch ( InvocationTargetException e )
        {
            log.error( "Cannot add native libraries", e.getCause() );
            throw new MojoExecutionException( "Cannot add native libraries", e.getCause() );
        } catch ( Exception e )
        {
            log.error( "Cannot add native libraries", e );
            throw new MojoExecutionException( "Cannot add native libraries", e );
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
            sealApkMethod.invoke( builder, new Object[ 0 ] );
        } catch ( InvocationTargetException e )
        {
            log.error( "Cannot seal the APK", e.getCause() );
            throw new MojoExecutionException( "Cannot seal the APK", e.getCause() );
        } catch ( Exception e )
        {
            log.error( "Cannot seal the APK", e );
            throw new MojoExecutionException( "Cannot seal the APK", e );
        }

    }

}
