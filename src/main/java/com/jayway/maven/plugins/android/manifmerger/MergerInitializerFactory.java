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
     * Returns the MergeStrategy for the specified version of the SDK Tools.
     * Currently supports Android SDK 20+.
     * 
     * @param log The Mojo Log
     * @param sdkMajorVersion The major version of the SDK Tools
     * @param sdkPath The path to the Android SDK
     * @return
     * @throws MojoExecutionException
     */
    public static MergeStrategy getInitializer( Log log, int sdkMajorVersion, File sdkPath )
            throws MojoExecutionException
    {
        if ( sdkMajorVersion < R20 ) {
            throw new MojoExecutionException("Manifest merger requires Android SDK " + R20 +
                    " or greater, but Android SDK " + sdkMajorVersion + " is in use.");
        }

        if ( sdkMajorVersion == R20 ) {
            return new MergeStrategyR20( log, sdkPath );
        } else {
            return new MergeStrategyR21( log, sdkPath );
        }
    }
}
