package com.jayway.maven.plugins.android.manifmerger;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

/**
 * Factory for building MergeStrategies
 * @author tombollwitt
 * 
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
     * @param toolsVersion The version of the SDK Tools
     * @param sdkPath The path to the Android SDK
     * @return
     * @throws MojoExecutionException
     */
    public static MergeStrategy getInitializer( Log log, int toolsVersion, File sdkPath ) throws MojoExecutionException
    {
        switch ( toolsVersion )
        {
        case R20:
            return new MergeStrategyR20( log, sdkPath );
        case R21:
            return new MergeStrategyR21( log, sdkPath );
        default:
            throw new MojoExecutionException( "Unsupported SDK Tools Revision: " + toolsVersion );
        }
    }
}
