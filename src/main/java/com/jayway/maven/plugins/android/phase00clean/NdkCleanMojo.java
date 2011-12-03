package com.jayway.maven.plugins.android.phase00clean;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.IOException;

/**
 * @author Johan Lindquist <johanlindquist@gmail.com>
 * @goal clean
 * @requiresProject true
 * @requiresOnline false
 * @phase clean
 */
public class NdkCleanMojo extends AbstractMojo {

    /**
     * @parameter expression="${android.nativeBuildLibsOutputDirectory}" default-value="${project.basedir}/libs"
     */
    File ndkBuildLibsOutputDirectory;

    /**
     * @parameter expression="${android.nativeBuildObjOutputDirectory}" default-value="${project.basedir}/obj"
     */
    File ndkBuildObjOutputDirectory;

    /** Forces the clean process to be skipped.
     *
     * @parameter expression="${android.nativeBuildSkipClean}" default-value="false"
     */
    boolean skipClean = false;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().debug("Cleaning out native library code directory : " + ndkBuildLibsOutputDirectory.getAbsolutePath());

        if (ndkBuildLibsOutputDirectory.exists())
        {
            try {
                FileUtils.deleteDirectory( ndkBuildLibsOutputDirectory );
            } catch ( IOException e ) {
                getLog().error( "Error deleting directory: " + e.getMessage(), e);
            }
        }

        getLog().debug("Cleaning out native object code directory: " + ndkBuildObjOutputDirectory.getAbsolutePath());

        if (ndkBuildObjOutputDirectory.exists())
        {
            try {
                FileUtils.deleteDirectory( ndkBuildObjOutputDirectory );
            } catch ( IOException e ) {
                getLog().error( "Error deleting directory: " + e.getMessage(), e);
            }
        }

    }

}
