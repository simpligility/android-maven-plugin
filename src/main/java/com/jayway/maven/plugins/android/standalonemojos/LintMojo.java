package com.jayway.maven.plugins.android.standalonemojos;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.jayway.maven.plugins.android.AbstractAndroidMojo;
import com.jayway.maven.plugins.android.CommandExecutor;
import com.jayway.maven.plugins.android.ExecutionException;
import com.jayway.maven.plugins.android.config.ConfigHandler;
import com.jayway.maven.plugins.android.config.ConfigPojo;
import com.jayway.maven.plugins.android.config.PullParameter;
import com.jayway.maven.plugins.android.configuration.Lint;

/**
 * LintMojo can run the lint command against the project. Implements parsing parameters from pom or command line
 * arguments and sets useful defaults as well.
 * 
 * @author Stéphane Nicolas <snicolas@octo.com>
 * @goal lint
 * @requiresProject false
 */
public class LintMojo extends AbstractAndroidMojo
{

    /**
     * The configuration for the lint goal. As soon as a lint goal is invoked the command will be executed unless the
     * skip parameter is set.
     * 
     * <pre>
     * &lt;lint&gt;
     *     &lt;failOnError&gt;false&lt;/failOnError&gt;
     *     &lt;skip&gt;true&lt;/skip&gt;
     *     &lt;showall&gt;true&lt;/showall&gt;
     *     &lt;classpath&gt;${project.build.directory}/&lt;/classpath&gt;
     * &lt;/lint&gt;
     * </pre>
     * 
     * Values can also be configured as properties on the command line as android.lint.* or in pom or settings file as
     * properties like lint.*.
     * 
     * @parameter
     */
    @ConfigPojo
    private Lint lint;

    /**
     * Fails build on lint errors. Defaults to "false".
     * 
     * @parameter expression="${android.lint.failOnError}"
     * @see com.jayway.maven.plugins.android.configuration.Lint#failOnError
     */
    private Boolean lintFailOnError;

    @PullParameter( defaultValue = "false" )
    private Boolean parsedFailOnError;

    /**
     * Skip the lint goal execution. Defaults to "true".
     * 
     * @parameter expression="${android.lint.skip}"
     * @see com.jayway.maven.plugins.android.configuration.Lint#skip
     */
    private Boolean lintSkip;

    @PullParameter( defaultValue = "true" )
    private Boolean parsedSkip;

    // ---------------
    // Enabled Checks
    // ---------------

    /**
     * Only check for errors (ignore warnings). Defaults to "false".
     * 
     * @parameter expression="${android.lint.nowarn}"
     * @see com.jayway.maven.plugins.android.configuration.Lint#nowarn
     */
    private Boolean lintNoWarn;

    @PullParameter( defaultValue = "false" )
    private Boolean parsedNowarn;

    /**
     * Check all warnings, including those off by default. Defaults to "false".
     * 
     * @parameter expression="${android.lint.wall}"
     * @see com.jayway.maven.plugins.android.configuration.Lint#wall
     */
    private Boolean lintWall;

    @PullParameter( defaultValue = "false" )
    private Boolean parsedWall;

    /**
     * Treat all warnings as errors. Defaults to "false".
     * 
     * @parameter expression="${android.lint.werror}"
     * @see com.jayway.maven.plugins.android.configuration.Lint#werror
     */
    private Boolean lintWerror;

    @PullParameter( defaultValue = "false" )
    private Boolean parsedWerror;

    /**
     * Use the given configuration file to determine whether issues are enabled or disabled. Defaults to "lint.xml".
     * 
     * @parameter expression="${android.lint.config}"
     * @see com.jayway.maven.plugins.android.configuration.Lint#config
     */
    private String lintConfig;

    @PullParameter( defaultValueGetterMethod = "getConfig" )
    private String parsedConfig;

    // ---------------
    // Enabled Checks
    // ---------------

    /**
     * Use full paths in the error output. Defaults to "false".
     * 
     * @parameter expression="${android.lint.fullpath}"
     * @see com.jayway.maven.plugins.android.configuration.Lint#fullpath
     */
    private Boolean lintFullpath;

    @PullParameter( defaultValue = "false" )
    private Boolean parsedFullpath;

    /**
     * Do not truncate long messages, lists of alternate locations, etc. Defaults to "true".
     * 
     * @parameter expression="${android.lint.showall}"
     * @see com.jayway.maven.plugins.android.configuration.Lint#showall
     */
    private Boolean lintShowAll;

    @PullParameter( defaultValue = "true" )
    private Boolean parsedShowall;

    /**
     * Do not include the source file lines with errors in the output. By default, the error output includes snippets of
     * source code on the line containing the error, but this flag turns it off. Defaults to "false".
     * 
     * @parameter expression="${android.lint.nolines}"
     * @see com.jayway.maven.plugins.android.configuration.Lint#nolines
     */
    private Boolean lintNolines;

    @PullParameter( defaultValue = "false" )
    private Boolean parsedNolines;

    /**
     * Add links to HTML report, replacing local path prefixes with url prefix. The mapping can be a comma-separated
     * list of path prefixes to corresponding URL prefixes, such as C:\temp\Proj1=http://buildserver/sources/temp/Proj1.
     * To turn off linking to files, use --url none. Defaults to "none".
     * 
     * @parameter expression="${android.lint.url}"
     * @see com.jayway.maven.plugins.android.configuration.Lint#url
     */
    private String lintUrl;

    @PullParameter( defaultValue = "none" )
    private String parsedUrl;

    /**
     * Create an HTML report instead. If the filename is a directory (or a new filename without an extension), lint will
     * create a separate report for each scanned project. No defaults.
     * 
     * @parameter expression="${android.lint.html}"
     * @see com.jayway.maven.plugins.android.configuration.Lint#html
     */
    private String lintHtml;

    @PullParameter( defaultValue = "null" )
    private String parsedHtml;

    /**
     * Create a simple HTML report instead. If the filename is a directory (or a new filename without an extension),
     * lint will create a separate report for each scanned project. No defaults.
     * 
     * @parameter expression="${android.lint.simplehtml}"
     * @see com.jayway.maven.plugins.android.configuration.Lint#simplehtml
     */
    private String lintSimplehtml;

    @PullParameter( defaultValue = "null" )
    private String parsedSimplehtml;

    /**
     * Create an XML report instead. If the filename is a directory (or a new filename without an extension), lint will
     * create a separate report for each scanned project. No defaults.
     * 
     * @parameter expression="${android.lint.xml}"
     * @see com.jayway.maven.plugins.android.configuration.Lint#xml
     */
    private String lintXml;

    @PullParameter( defaultValue = "null" )
    private String parsedXml;

    // ---------------
    // Project Options
    // ---------------

    /**
     * Add the given folder (or path) as a source directory for the project. Only valid when running lint on asingle
     * project. No defaults.
     * 
     * @parameter expression="${android.lint.sources}"
     * @see com.jayway.maven.plugins.android.configuration.Lint#sources
     */
    private String lintSources;

    @PullParameter( defaultValue = "null" )
    private String parsedSources;

    /**
     * Add the given folder (or jar file, or path) as a class directory for the project. Only valid when running lint on
     * a single project. No defaults.
     * 
     * @parameter expression="${android.lint.classpath}"
     * @see com.jayway.maven.plugins.android.configuration.Lint#classpath
     */
    private String lintClasspath;

    @PullParameter( defaultValue = "null" )
    private String parsedClasspath;

    /**
     * Add the given folder (or jar file, or path) as a class library for the project. Only valid when running lint on a
     * single project. No defaults.
     * 
     * @parameter expression="${android.lint.libraries}"
     * @see com.jayway.maven.plugins.android.configuration.Lint#lintLibraries
     */
    private String lintLibraries;

    @PullParameter( defaultValue = "null" )
    private String parsedLibraries;

    /**
     * the config file for lint.
     */
    private File configFile;

    /**
     * Execute the mojo by parsing the config and actually doing the lint.
     * 
     * @throws MojoExecutionException
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {

        ConfigHandler configHandler = new ConfigHandler( this );
        configHandler.parseConfiguration();

        getLog().debug( "failOnError:" + parsedFailOnError );
        getLog().debug( "skip:" + parsedSkip );
        getLog().debug( "nowarn:" + parsedNowarn );
        getLog().debug( "wall:" + parsedWall );
        getLog().debug( "werror:" + parsedWerror );
        getLog().debug( "config:" + parsedConfig );

        getLog().debug( "fullPath:" + parsedFullpath );
        getLog().debug( "showall:" + parsedShowall );
        getLog().debug( "nolines:" + parsedNolines );
        getLog().debug( "html:" + parsedHtml );
        getLog().debug( "simplehtml:" + parsedSimplehtml );
        getLog().debug( "xml:" + parsedXml );

        getLog().debug( "sources:" + parsedSources );
        getLog().debug( "classpath:" + parsedClasspath );
        getLog().debug( "libraries:" + parsedLibraries );

        if ( parsedSkip )
        {
            getLog().info( "Skipping lint" );
        }
        else
        {

            CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
            executor.setLogger( this.getLog() );

            String command = getAndroidSdk().getLintPath();

            List< String > parameters = new ArrayList< String >();
            if ( parsedNowarn )
            {
                parameters.add( "-w" );
            }
            if ( parsedWall )
            {
                parameters.add( "-Wall" );
            }
            if ( parsedWerror )
            {
                parameters.add( "-Werror" );
            }

            if ( parsedConfig != null )
            {
                parameters.add( "--config" );
                parameters.add( parsedConfig );
            }

            if ( parsedFullpath )
            {
                parameters.add( "--fullpath" );
            }
            if ( parsedShowall )
            {
                parameters.add( "--showall" );
            }
            if ( parsedNolines )
            {
                parameters.add( "--nolines" );
            }

            if ( !"null".equals( parsedHtml ) )
            {
                parameters.add( "--html" );
                parameters.add( parsedHtml );
            }
            if ( !"none".equals( parsedUrl ) )
            {
                parameters.add( "--url" );
                parameters.add( parsedUrl );
            }
            if ( !"null".equals( parsedSimplehtml ) )
            {
                parameters.add( "--simplehtml" );
                parameters.add( parsedSimplehtml );
            }
            if ( !"null".equals( parsedXml ) )
            {
                parameters.add( "--xml" );
                parameters.add( parsedXml );
            }

            if ( !"null".equals( parsedSources ) )
            {
                parameters.add( "--sources" );
                parameters.add( parsedSources );
            }
            if ( !"null".equals( parsedClasspath ) )
            {
                parameters.add( "--classpath" );
                parameters.add( parsedClasspath );
            }
            if ( !"null".equals( parsedLibraries ) )
            {
                parameters.add( "--libraries" );
                parameters.add( parsedLibraries );
            }

            parameters.add( "--classpath" );
            parameters.add( project.getBuild().getDirectory() );
            parameters.add( project.getBasedir().getAbsolutePath() );

            // change return code if errors
            // see http://developer.android.com/tools/help/lint.html
            // option not provided by lint --help
            parameters.add( "--exitcode" );
            try
            {
                getLog().info( "Running command: " + command );
                getLog().info( "with parameters: " + parameters );
                executor.executeCommand( command, parameters, false );
            }
            catch ( ExecutionException e )
            {
                if ( parsedFailOnError )
                {
                    throw new MojoExecutionException( "", e );
                }
            }
        }
    }

    /**
     * Gets the apk file location from basedir/lint.xml
     * 
     * @return absolute path.
     */
    // used via PullParameter annotation - do not remove
    private String getConfig()
    {
        if ( configFile == null )
        {
            configFile = new File( project.getBasedir(), "lint.xml" );
        }
        return configFile.getAbsolutePath();
    }

}
