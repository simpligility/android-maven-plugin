package com.jayway.maven.plugins.android.manifmerger;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

/**
 * Factory for building MergeStrategies
 * @author tombollwitt
 * @author Manfred Moser <manfred@simpligility.com>
 */
public class MergerInitializerFactory
{

    /**
     * Constant for SDK Tools R20
     */
    private static final int R20 = 20;
    /**
     * Constant for SDK Tools R21
     */
    private static final int R21 = 21;

    /**
     * Returns the MergeStrategy for the specified version of the SDK Tools.
     * Currently supports Revisions: 20, 21.
     * 
     * @param log The Mojo Log
     * @param sdkMajorVersion The major version of the SDK Tools
     * @param sdkPath The path to the Android SDK
     * @return
     * @throws MojoExecutionException
     */
    public static MergeStrategy getInitializer( Log log, int sdkMajorVersion, File sdkPath ) throws MojoExecutionException
    {
        switch ( sdkMajorVersion )
        {
//        case R20:
//            return new MergeStrategyR20( log, sdkPath );
        case R21:
            return new MergeStrategyR21( log, sdkPath );
        default:
          log.info( "ATTENTION! Your Android SDK is outdated and not supported for the AndroidManifest merge feature" );
          log.info( "Supported major versions are " + R20 + " and " + R21 + ". You are using " + sdkMajorVersion );
          log.info( "Execution proceeding using merge procedure from " + R20 );
          return new MergeStrategyR20( log, sdkPath );
        }
    }
}
