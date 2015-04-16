package com.simpligility.maven.plugins.android.phase00clean;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * @author Johan Lindquist <johanlindquist@gmail.com>
 */
@Mojo(
        name = "clean",
        defaultPhase = LifecyclePhase.CLEAN,
        requiresOnline = true // TODO Why?
)
public class NdkCleanMojo extends AbstractMojo
{

    @Parameter( property = "android.nativeBuildLibsOutputDirectory", defaultValue = "${project.basedir}/libs" )
    File ndkBuildLibsOutputDirectory;

    @Parameter( property = "android.nativeBuildObjOutputDirectory", defaultValue = "${project.basedir}/obj" )
    File ndkBuildObjOutputDirectory;

    /**
     * Forces the clean process to be skipped.
     */
    @Parameter( property = "android.nativeBuildSkipClean", defaultValue = "false" )
    boolean skipClean = false;

    /**
     * Specifies whether the deletion of the libs/ folder structure should be skipped.  This is by default set to
     * skip (true) to avoid unwanted deletions of libraries already present in this structure.
     */
    @Parameter( property = "android.nativeBuildSkipCleanLibsOutputDirectory", defaultValue = "true" )
    boolean skipBuildLibsOutputDirectory = true;

    /**
     * Specifies whether the obj/ build folder structure should be deleted.
     */
    @Parameter( property = "android.nativeBuildSkipCleanLibsOutputDirectory", defaultValue = "false" )
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
