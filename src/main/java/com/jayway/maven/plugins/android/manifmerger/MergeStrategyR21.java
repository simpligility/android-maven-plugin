package com.jayway.maven.plugins.android.manifmerger;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

/**
 * MergeStrategy for SDK Tools R21
 * @author tombollwitt
 *
 */
public class MergeStrategyR21 implements MergeStrategy
{
    /**
     * The Mojo logger
     */
    Log log;

    /**
     * The method that does the merging.
     */
    Method processMethod;

    /**
     * The ManifestMerger class
     */
    Object merger;

    @SuppressWarnings( { "unchecked", "rawtypes" } )
    public MergeStrategyR21( Log log, File sdkPath ) throws MojoExecutionException
    {

        File mergerLib = new File( sdkPath + "/tools/lib/manifmerger.jar" );
        File commonLib = new File( sdkPath + "/tools/lib/common.jar" );

        URLClassLoader mlLoader = null;
        Class manifestMergerClass = null;
        Class mergerLogClass = null;
        try
        {
            mlLoader = new URLClassLoader( new URL[] { mergerLib.toURI().toURL() },
                    ManifestMerger.class.getClassLoader() );
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

        // Loads the StdLogger class
        Class stdSdkLogClass = null;
        Class logLevel = null;
        try
        {
            URLClassLoader child = new URLClassLoader( new URL[] { commonLib.toURI().toURL() }, mlLoader );
            stdSdkLogClass = child.loadClass( "com.android.utils.StdLogger" );
            log.debug( "StdLogger loaded " + stdSdkLogClass );
            logLevel = child.loadClass( "com.android.utils.StdLogger$Level" );
            log.debug( "Level loaded " + logLevel );
        }
        catch ( MalformedURLException e )
        {
            // This one cannot happen.
            throw new RuntimeException( "Cannot create a correct URL from file " + commonLib.getAbsolutePath() );
        }
        catch ( ClassNotFoundException e )
        {
            log.error( "Cannot find required class", e );
            throw new MojoExecutionException( "Cannot find the required class", e );
        }

        // In order to improve performance and to check that all methods are
        // available we cache used methods.
        try
        {
            processMethod = manifestMergerClass.getMethod( "process", File.class, File.class, File[].class, Map.class );
        }
        catch ( Exception e )
        {
            log.error( "Cannot find required method", e );
            throw new MojoExecutionException( "Cannot find the required method", e );
        }

        try
        {
            Enum e = Enum.valueOf( logLevel, "VERBOSE" );
            Constructor stdSdkLogConstructor = stdSdkLogClass.getDeclaredConstructors()[0];
            Object sdkLog = stdSdkLogConstructor.newInstance( e );
            Method wrapSdkLogMethod = mergerLogClass.getMethod( "wrapSdkLog", stdSdkLogClass.getInterfaces()[0] );
            Object iMergerLog = wrapSdkLogMethod.invoke( null, sdkLog.getClass().getInterfaces()[0].cast( sdkLog ) );
            Constructor manifestMergerConstructor = manifestMergerClass.getDeclaredConstructors()[0];
            merger = manifestMergerConstructor.newInstance( iMergerLog, null );
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
     * @see {@link MergeStrategy#process(File, File, File[])}
     */
    @Override
    public boolean process( File mergedFile, File apkManifest, File[] libraryManifests ) throws MojoExecutionException
    {
        try
        {
            return (Boolean) processMethod.invoke( merger, mergedFile, apkManifest, libraryManifests, null );
        }
        catch ( Exception e )
        {
            log.error( "Cannot merge the manifests", e );
            throw new MojoExecutionException( "Cannot merge the manifests", e );
        }
    }

}
