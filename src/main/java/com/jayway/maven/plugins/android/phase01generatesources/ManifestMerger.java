
package com.jayway.maven.plugins.android.phase01generatesources;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * This class is a front for the ManifestMerger contained in the {@code manifmerger.jar}. It is dynamically loaded and
 * reflection is used to delegate the methods
 */
public class ManifestMerger
{

    /**
     * The ManifestMerger class object
     */
    @SuppressWarnings( "rawtypes" )
    private static Class manifestMergerClass;

    /**
     * The NullSdkLog class object
     */
    @SuppressWarnings( "rawtypes" )
    private static Class nullSdkLogClass;

    /**
     * The MergerLog class object
     */
    @SuppressWarnings( "rawtypes" )
    private static Class mergerLogClass;

    /**
     * The Mojo logger
     */
    private static Log log;

    /**
     * Before being able to use the ManifestMerger, an initialization is required.
     * 
     * @param log the Mojo Logger
     * @param sdkLibs the File pointing on {@code sdklib.jar}
     * @param mergerLib the File pointing on {@code manifmerger.jar}
     * @throws MojoExecutionException if the ManifestMerger class cannot be loaded
     */
    @SuppressWarnings( "unchecked" )
    public static void initialize( Log log, File sdkLibs, File mergerLib ) throws MojoExecutionException
    {
        if ( manifestMergerClass != null )
        {
            // Already initialized
            return;
        }

        ManifestMerger.log = log;

        // Loads the ManifestMerger class
        URLClassLoader mlLoader = null;
        try
        {
            mlLoader = new URLClassLoader( new URL[] {
                mergerLib.toURI().toURL()
            }, ManifestMerger.class.getClassLoader() );
            try
            {
                manifestMergerClass = mlLoader.loadClass( "com.android.manifmerger.ManifestMerger" );
                log.debug( "ManifestMerger loaded " + manifestMergerClass );
            }
            catch ( ClassNotFoundException e )
            {
                log.error( e );
                throw new MojoExecutionException( "Cannot load 'com.android.manifmerger.ManifestMerger'" );
            }

            try
            {
                mergerLogClass = mlLoader.loadClass( "com.android.manifmerger.MergerLog" );
                log.debug( "ManifestMerger loaded " + mergerLogClass );
            }
            catch ( ClassNotFoundException e )
            {
                log.error( e );
                throw new MojoExecutionException( "Cannot load 'com.android.manifmerger.MergerLog'" );
            }
        }
        catch ( MalformedURLException e )
        {
            // This one cannot happen.
            throw new RuntimeException( "Cannot create a correct URL from file " + mergerLib.getAbsolutePath() );
        }

        // Loads the NullSdkLog class
        try
        {
            URLClassLoader child = new URLClassLoader( new URL[] {
                sdkLibs.toURI().toURL()
            }, mlLoader );
            nullSdkLogClass = child.loadClass( "com.android.sdklib.NullSdkLog" );
            log.debug( "NullSdkLog loaded " + nullSdkLogClass );
        }
        catch ( MalformedURLException e )
        {
            // This one cannot happen.
            throw new RuntimeException( "Cannot create a correct URL from file " + sdkLibs.getAbsolutePath() );
        }
        catch ( ClassNotFoundException e )
        {
            log.error( e );
            throw new MojoExecutionException( "Cannot load 'com.android.sdklib.NullSdkLog'" );
        }

        // In order to improve performance and to check that all methods are available we cache used methods.
        try
        {
            manifestMergerConstructor = manifestMergerClass.getDeclaredConstructors()[0];
            processMethod = manifestMergerClass.getMethod( "process", File.class, File.class, File[].class );

            getLoggerMethod = nullSdkLogClass.getMethod( "getLogger" );

            wrapSdkLogMethod = mergerLogClass.getMethod( "wrapSdkLog", nullSdkLogClass.getInterfaces()[0] );
        }
        catch ( Exception e )
        {
            log.error( "Cannot find required method", e );
            throw new MojoExecutionException( "Cannot find the required method", e );
        }
    }

    /**
     * The Manifest Merger instance
     */
    private Object merger;

    /**
     * The ManifestMerger Constructor
     */
    @SuppressWarnings( "rawtypes" )
    private static Constructor manifestMergerConstructor;

    /**
     * The ManifestMerger.process Method
     */
    private static Method processMethod;

    /**
     * The IMergerLog instance
     */
    private Object iMergerLog;

    /**
     * The MergerLog.wrapSdkLog Method
     */
    private static Method wrapSdkLogMethod;

    /**
     * The NUllSdkLog instance
     */
    private Object sdkLog;

    /**
     * NullSdkLog.getLogger Method
     */
    private static Method getLoggerMethod;

    /**
     * Creates a new ManifestMerger. The class must be initialized before calling this constructor.
     */
    public ManifestMerger() throws MojoExecutionException
    {
        if ( manifestMergerClass == null || nullSdkLogClass == null )
        {
            throw new MojoExecutionException( "The ManifestMerger class was not initialized" );
        }

        try
        {
            sdkLog = getLoggerMethod.invoke( null );

            if ( sdkLog == null )
            {
                throw new MojoExecutionException( "The NullSdkLogger class was not instantiated" );
            }

            iMergerLog = wrapSdkLogMethod.invoke( null, sdkLog.getClass().getInterfaces()[0].cast( sdkLog ) );
            if ( iMergerLog == null )
            {
                throw new MojoExecutionException( "The IMergerLog class was not instantiated" );
            }

            merger = manifestMergerConstructor.newInstance( iMergerLog );
        }
        catch ( InvocationTargetException e )
        {
            log.error( "Cannot create the ManifestMerger object", e.getCause() );
            throw new MojoExecutionException( "Cannot create the ManifestMerger object", e.getCause() );
        }
        catch ( Exception e )
        {
            log.error( "Cannot create the ManifestMerger object", e );
            throw new MojoExecutionException( "Cannot create the ManifestMerger object", e );
        }
    }

    /**
     * Merge the AndroidManifests
     * 
     * @param paramFile1 The destination File for the merged content
     * @param paramFile2 The original AndroidManifest to merge into
     * @param paramArayOfFile The array of APKLIB manifests to merge
     * @return
     * @throws MojoExecutionException if there is a problem merging
     */
    public boolean process( File paramFile1, File paramFile2, File[] paramArayOfFile ) throws MojoExecutionException
    {
        try
        {
            return (Boolean) processMethod.invoke( merger, paramFile1, paramFile2, paramArayOfFile );
        }
        catch ( IllegalArgumentException e )
        {
            log.error( "Cannot merge the manifests", e );
            throw new MojoExecutionException( "Cannot merge the manifests", e );
        }
        catch ( IllegalAccessException e )
        {
            log.error( "Cannot merge the manifests", e );
            throw new MojoExecutionException( "Cannot merge the manifests", e );
        }
        catch ( InvocationTargetException e )
        {
            log.error( "Cannot merge the manifests", e );
            throw new MojoExecutionException( "Cannot merge the manifests", e );
        }
    }
}
