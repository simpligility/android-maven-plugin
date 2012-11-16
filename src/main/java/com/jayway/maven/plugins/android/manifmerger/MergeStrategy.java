package com.jayway.maven.plugins.android.manifmerger;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * MergeStrategy interface
 * @author tombollwitt
 * 
 */
public interface MergeStrategy
{
    /**
     * Merges the APKLIB manifests with the APK manifest
     * 
     * @param mergedFile The final merged AndroidManifest file.
     * @param apkManifest The original AndroidManifest file of the APK.
     * @param libraryManifests Array of AndroidManifests for the APKLIBs
     * @return
     * @throws MojoExecutionException
     */
    boolean process( File mergedFile, File apkManifest, File[] libraryManifests ) throws MojoExecutionException;
}
