package com.jayway.maven.plugins.android.phase00clean;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * @author Johan Lindquist <johanlindquist@gmail.com>
 * @goal clean
 * @requiresProject true
 * @requiresOnline false
 * @phase clean
 */
public class NdkCleanMojo extends AbstractMojo
{

    /**
     * @parameter property="android.nativeBuildLibsOutputDirectory" default-value="${project.basedir}/libs"
     */
    File ndkBuildLibsOutputDirectory;

    /**
     * @parameter property="$android.nativeBuildObjOutputDirectory" default-value="${project.basedir}/obj"
     */
    File ndkBuildObjOutputDirectory;

    /**
     * Forces the clean process to be skipped.
     *
     * @parameter property="android.nativeBuildSkipClean" default-value="false"
     */
    boolean skipClean = false;

    /**
     * Specifies whether the deletion of the libs/ folder structure should be skipped.  This is by default set to
     * skip (true) to avoid unwanted deletions of libraries already present in this structure.
     *
     * @parameter property="android.nativeBuildSkipCleanLibsOutputDirectory" default-value="true"
     */
    boolean skipBuildLibsOutputDirectory = true;

    /**
     * Specifies whether the obj/ build folder structure should be deleted.
     *
     * @parameter property="android.nativeBuildSkipCleanLibsOutputDirectory" default-value="false"
     */
    boolean skipBuildObjsOutputDirectory = false;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        if ( ndkBuildLibsOutputDirectory.exists() )
        {
            if ( ! skipBuildLibsOutputDirectory )
            {
                getLog().debug( "Cleaning out native library code directory : " + ndkBuildLibsOutputDirectory
                        .getAbsolutePath() );
                try
                {
                    FileUtils.deleteDirectory( ndkBuildLibsOutputDirectory );
                }
                catch ( IOException e )
                {
                    getLog().error( "Error deleting directory: " + e.getMessage(), e );
                }
            }
        }

        if ( ndkBuildObjOutputDirectory.exists() )
        {
            if ( ! skipBuildObjsOutputDirectory )
            {
                getLog().debug(
                        "Cleaning out native object code directory: " + ndkBuildObjOutputDirectory.getAbsolutePath() );
                try
                {
                    FileUtils.deleteDirectory( ndkBuildObjOutputDirectory );
                }
                catch ( IOException e )
                {
                    getLog().error( "Error deleting directory: " + e.getMessage(), e );
                }
            }
        }

    }

}
