package com.jayway.maven.plugins.android.standalonemojos;

import com.jayway.maven.plugins.android.AbstractAndroidMojo;
import com.jayway.maven.plugins.android.CommandExecutor;
import com.jayway.maven.plugins.android.ExecutionException;
import com.jayway.maven.plugins.android.config.ConfigHandler;
import com.jayway.maven.plugins.android.config.ConfigPojo;
import com.jayway.maven.plugins.android.config.PullParameter;
import com.jayway.maven.plugins.android.configuration.Zipalign;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.jayway.maven.plugins.android.common.AndroidExtension.APK;

/**
 * ZipalignMojo can run the zipalign command against the apk. Implements parsing parameters from pom or command line
 * arguments and sets useful defaults as well.
 *
 * @author Manfred Moser <manfred@simpligility.com>
 */
@Mojo( name = "zipalign" )
public class ZipalignMojo extends AbstractAndroidMojo 
{

    /**
     * The configuration for the zipalign goal. As soon as a zipalign goal is invoked the command will be executed
     * unless the skip parameter is set. By default the input file is the apk produced by the build in target. The
     * outputApk will use the postfix -aligned.apk. The following shows a default full configuration of the zipalign
     * goal as an example for changes as plugin configuration.
     * <pre>
     * &lt;zipalign&gt;
     *     &lt;skip&gt;false&lt;/skip&gt;
     *     &lt;verbose&gt;true&lt;/verbose&gt;
     *     &lt;inputApk&gt;${project.build.directory}/${project.finalName}.apk&lt;/inputApk&gt;
     *     &lt;outputApk&gt;${project.build.directory}/${project.finalName}-aligned.apk&lt;/outputApk&gt;
     * &lt;/zipalign&gt;
     * </pre>
     *
     * Values can also be configured as properties on the command line as android.zipalign.*
     * or in pom or settings file as properties like zipalign.*.
     */
    @Parameter
    @ConfigPojo
    private Zipalign zipalign;

    /**
     * Skip the zipalign goal execution. Defaults to "true".
     * @see com.jayway.maven.plugins.android.configuration.Zipalign#skip
     */
    @Parameter( property = "android.zipalign.skip" )
    private Boolean zipalignSkip;

    @PullParameter( defaultValue = "true" )
    private Boolean parsedSkip;

    /**
     * Activate verbose output for the zipalign goal execution. Defaults to "false".
     * @see com.jayway.maven.plugins.android.configuration.Zipalign#verbose
     */
    @Parameter( property = "android.zipalign.verbose" )
    private Boolean zipalignVerbose;

    @PullParameter( defaultValue = "false" )
    private Boolean parsedVerbose;

    /**
     * The apk file to be zipaligned. Per default the file is taken from build directory (target normally) using the
     * build final name as file name and apk as extension.
     *
     * @see com.jayway.maven.plugins.android.configuration.Zipalign#inputApk
     */
    @Parameter( property = "android.zipalign.inputApk" )
    private String zipalignInputApk;

    @PullParameter ( defaultValueGetterMethod = "getInputApkPath" )
    private String parsedInputApk;

    /**
     * The apk file produced by the zipalign goal. Per default the file is placed into the build directory (target
     * normally) using the build final name appended with "-aligned" as file name and apk as extension.
     *
     * @see com.jayway.maven.plugins.android.configuration.Zipalign#outputApk
     */
    @Parameter( property = "android.zipalign.outputApk" )
    private String zipalignOutputApk;

    @PullParameter( defaultValueGetterMethod = "getOutputApkPath" )
    private String parsedOutputApk;

    /**
     * the apk file to be zipaligned.
     */
    private File apkFile;
    /**
     * the output apk file for the zipalign process.
     */
    private File alignedApkFile;

    /**
     * Execute the mojo by parsing the confign and actually doing the zipalign.
     *
     * @throws MojoExecutionException
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {

        // If we're not on a supported packaging with just skip (Issue 87)
        // http://code.google.com/p/maven-android-plugin/issues/detail?id=87
        if ( ! SUPPORTED_PACKAGING_TYPES.contains( project.getPackaging() ) )
        {
            getLog().info( "Skipping zipalign on " + project.getPackaging() );
            return;
        }

        ConfigHandler configHandler = new ConfigHandler( this, this.session, this.execution );
        configHandler.parseConfiguration();

        parsedInputApk = FilenameUtils.separatorsToSystem( parsedInputApk );
        parsedOutputApk = FilenameUtils.separatorsToSystem( parsedOutputApk );

        getLog().debug( "skip:" + parsedSkip );
        getLog().debug( "verbose:" + parsedVerbose );
        getLog().debug( "inputApk:" + parsedInputApk );
        getLog().debug( "outputApk:" + parsedOutputApk );

        if ( parsedSkip )
        {
            getLog().info( "Skipping zipalign" );
        }
        else
        {
            boolean outputToSameFile = sameOutputAsInput();

            CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
            executor.setLogger( this.getLog() );

            String command = getAndroidSdk().getZipalignPath();

            List<String> parameters = new ArrayList<String>();
            if ( parsedVerbose )
            {
                parameters.add( "-v" );
            }
            parameters.add( "-f" ); // force overwriting existing output file
            parameters.add( "4" ); // byte alignment has to be 4!
            parameters.add( parsedInputApk );
            String outputApk = outputToSameFile ? getTemporaryOutputApkFilename() : parsedOutputApk;
            parameters.add( outputApk );

            try
            {
                getLog().info( "Running command: " + command );
                getLog().info( "with parameters: " + parameters );
                executor.setCaptureStdOut( true );
                executor.executeCommand( command, parameters );

                if ( FileUtils.fileExists( outputApk ) )
                {
                    if ( outputToSameFile )
                    {
                        // No needs to attach zipaligned apk to artifacts
                        try
                        {
                            FileUtils.rename( new File( outputApk ),  new File( parsedInputApk ) );
                        }
                        catch ( IOException e )
                        {
                            getLog().error( "Failed to replace original apk with aligned "
                                    + getFullPathWithName( outputApk ), e );
                        }
                    }
                    else
                    {
                        // Attach the resulting artifact (Issue 88)
                        // http://code.google.com/p/maven-android-plugin/issues/detail?id=88
                        projectHelper.attachArtifact( project, APK, "aligned", new File( outputApk ) );
                        getLog().info( "Attach " + getFullPathWithName( outputApk ) + " to the project" );
                    }
                }
                else
                {
                    getLog().error( "Cannot attach " + getFullPathWithName( outputApk ) + " to the project"
                            + " - The file does not exist" );
                }
            }
            catch ( ExecutionException e )
            {
                throw new MojoExecutionException( "", e );
            }
        }
    }

    private String getFullPathWithName( String filename )
    {
        return FilenameUtils.getFullPath( filename ) + FilenameUtils.getName( filename );
    }

    private boolean sameOutputAsInput()
    {
        return getFullPathWithName( parsedInputApk ).equals( getFullPathWithName( parsedOutputApk ) );
    }

    // zipalign doesn't allow output file to be same as input
    private String getTemporaryOutputApkFilename()
    {
        return parsedOutputApk.substring( 0, parsedOutputApk.lastIndexOf( '.' ) ) + "-aligned-temp.apk";
    }

    /**
     * Gets the apk file location from basedir/target/finalname.apk
     *
     * @return absolute path.
     */
    // used via PullParameter annotation - do not remove
    private String getInputApkPath()
    {
        if ( apkFile == null )
        {
            apkFile = new File( project.getBuild().getDirectory(), project.getBuild().getFinalName() + "." + APK );
        }
        return apkFile.getAbsolutePath();
    }

    /**
     * Gets the apk file location from basedir/target/finalname-aligned.apk. "-aligned" is the inserted string for the
     * output file.
     *
     * @return absolute path.
     */
    // used via PullParameter annotation - do not remove
    private String getOutputApkPath()
    {
        if ( alignedApkFile == null )
        {
            alignedApkFile = new File( project.getBuild().getDirectory(),
                    project.getBuild().getFinalName() + "-aligned." + APK );
        }
        return alignedApkFile.getAbsolutePath();
    }
}
