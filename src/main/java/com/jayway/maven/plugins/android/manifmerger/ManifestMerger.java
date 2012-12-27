package com.jayway.maven.plugins.android.manifmerger;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

/**
 * This class is a front for the ManifestMerger contained in the
 * {@code manifmerger.jar}. It is dynamically loaded and reflection is used to
 * delegate the methods
 * 
 * @author tombollwitt
 * @author Manfred Moser <manfred@simpligility.com>
 */
public class ManifestMerger
{
    /**
     * The Manifest Merger instance
     */
    private static MergeStrategy merger;

    /**
     * Before being able to use the ManifestMerger, an initialization is
     * required.
     * 
     * @param log the Mojo Logger
     * @param sdkPath the path to the Android SDK
     * @param sdkMajorVersion the major mergerLib the File pointing on {@code manifmerger.jar}
     * @throws MojoExecutionException if the ManifestMerger class cannot be
     *         loaded
     */
    public void initialize( Log log, File sdkPath, int sdkMajorVersion ) throws MojoExecutionException
    {
        if ( merger != null )
        {
            // Already initialized
            return;
        }

        merger = MergerInitializerFactory.getInitializer( log, sdkMajorVersion, sdkPath );

    }

    /**
     * Creates a new ManifestMerger. The class must be initialized before
     * calling this constructor.
     */
    public ManifestMerger( Log log, File sdkPath, int sdkMajorVersion ) throws MojoExecutionException
    {
        initialize( log, sdkPath, sdkMajorVersion );
    }

    /**
     * Merge the AndroidManifests
     * 
     * @param mergedFile The destination File for the merged content
     * @param apkManifest The original AndroidManifest to merge into
     * @param libraryManifests The array of APKLIB manifests to merge
     * @return
     * @throws MojoExecutionException if there is a problem merging
     */
    public boolean process( File mergedFile, File apkManifest, File[] libraryManifests ) throws MojoExecutionException
    {
        return merger.process( mergedFile, apkManifest, libraryManifests );
    }
}
