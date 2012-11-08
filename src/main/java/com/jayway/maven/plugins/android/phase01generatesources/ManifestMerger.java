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
     * The Mojo logger
     */
    private static Log log;

    /**
     * The Manifest Merger instance
     */
    private static Object merger;

    /**
     * The ManifestMerger.process Method
     */
    private static Method processMethod;

    /**
     * Before being able to use the ManifestMerger, an initialization is required.
     * 
     * @param log the Mojo Logger
     * @param sdkLibs the File pointing on {@code sdklib.jar}
     * @param mergerLib the File pointing on {@code manifmerger.jar}
     * @throws MojoExecutionException if the ManifestMerger class cannot be loaded
     */
    @SuppressWarnings( {
    "unchecked", "rawtypes"
    } )
    public void initialize( Log log, File sdkLibs, File mergerLib ) throws MojoExecutionException
    {
        if ( processMethod != null && merger != null )
        {
            // Already initialized
            return;
        }

        ManifestMerger.log = log;

        // Load the ManifestMerger and MergerLog classes
        URLClassLoader mlLoader = null;
        Class manifestMergerClass = null;
        Class mergerLogClass = null;
        try
        {
            mlLoader = new URLClassLoader( new URL[] {
                mergerLib.toURI().toURL()
            }, ManifestMerger.class.getClassLoader() );
            manifestMergerClass = mlLoader.loadClass( "com.android.manifmerger.ManifestMerger" );
            log.debug( "ManifestMerger loaded " + manifestMergerClass );
            mergerLogClass = mlLoader.loadClass( "com.android.manifmerger.MergerLog" );
            log.debug( "ManifestMerger loaded " + mergerLogClass );
        }
        catch ( MalformedURLException e )
        {
            // This one cannot happen.
            throw new RuntimeException( "Cannot create a correct URL from file " + mergerLib.getAbsolutePath() );
        }
        catch ( ClassNotFoundException e )
        {
            log.error( "Cannot find required class", e );
            throw new MojoExecutionException( "Cannot find the required class", e );
        }

        // Loads the NullSdkLog class
        Class stdSdkLogClass = null;
        try
        {
            URLClassLoader child = new URLClassLoader( new URL[] { sdkLibs.toURI().toURL() }, mlLoader );
            stdSdkLogClass = child.loadClass( "com.android.sdklib.StdSdkLog" );
            log.debug( "StdSdkLog loaded " + stdSdkLogClass );
        }
        catch ( MalformedURLException e )
        {
            // This one cannot happen.
            throw new RuntimeException( "Cannot create a correct URL from file " + sdkLibs.getAbsolutePath() );
        }
        catch ( ClassNotFoundException e )
        {
            log.error( "Cannot find required class", e );
            throw new MojoExecutionException( "Cannot find the required class", e );
        }

        // In order to improve performance and to check that all methods are available we cache used methods.
        try
        {
            processMethod = manifestMergerClass.getMethod( "process", File.class, File.class, File[].class );
        }
        catch ( Exception e )
        {
            log.error( "Cannot find required method", e );
            throw new MojoExecutionException( "Cannot find the required method", e );
        }

        try
        {
            Constructor stdSdkLogConstructor = stdSdkLogClass.getDeclaredConstructors()[0];
            Object sdkLog = stdSdkLogConstructor.newInstance();
            Method wrapSdkLogMethod = mergerLogClass.getMethod( "wrapSdkLog", stdSdkLogClass.getInterfaces()[0] );
            Object iMergerLog = wrapSdkLogMethod.invoke( null, sdkLog.getClass().getInterfaces()[0].cast( sdkLog ) );
            Constructor manifestMergerConstructor = manifestMergerClass.getDeclaredConstructors()[0];
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
     * Creates a new ManifestMerger. The class must be initialized before calling this constructor.
     */
    public ManifestMerger( Log log, File sdkLibs, File mergerLib ) throws MojoExecutionException
    {
        initialize( log, sdkLibs, mergerLib );
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
        catch ( Exception e )
        {
            log.error( "Cannot merge the manifests", e );
            throw new MojoExecutionException( "Cannot merge the manifests", e );
        }
    }
}
