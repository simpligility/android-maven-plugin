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
 * @author Manfred Moser <manfred@simpligility.com>
 * @goal lint
 * @requiresProject false
 */
public class LintMojo extends AbstractAndroidMojo
{

    /**
     * The configuration for the lint goal. As soon as a lint goal is invoked the command will be executed unless the
     * skip parameter is set. A minimal configuration is
     * 
     * <pre>
     * &lt;lint&gt;
     *     &lt;skip&gt;true&lt;/skip&gt;
     * &lt;/lint&gt;
     * </pre>
     * 
     * Full configuration can look can use these parameters.
     * 
     * <pre>
     * &lt;lint&gt;
     *     &lt;failOnError&gt;true|false&lt;/failOnError&gt;
     *     &lt;skip&gt;true|false&lt;/skip&gt;
     *     &lt;showall&gt;true|false&lt;/showall&gt;
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
     * Only check for errors and ignore warnings. Defaults to "false".
     * 
     * @parameter expression="${android.lint.ignoreWarning}"
     * @see com.jayway.maven.plugins.android.configuration.Lint#ignoreWarnings
     */
    private Boolean lintIgnoreWarnings;

    @PullParameter( defaultValue = "false" )
    private Boolean parsedIgnoreWarnings;

    /**
     * Check all warnings, including those off by default. Defaults to "false".
     * 
     * @parameter expression="${android.lint.warnAll}"
     * @see com.jayway.maven.plugins.android.configuration.Lint#warnAll
     */
    private Boolean lintWarnAll;

    @PullParameter( defaultValue = "false" )
    private Boolean parsedWarnAll;

    /**
     * Report all warnings as errors. Defaults to "false".
     * 
     * @parameter expression="${android.lint.warningsAsErrors}"
     * @see com.jayway.maven.plugins.android.configuration.Lint#warningsAsErrors
     */
    private Boolean lintWarningsAsErrors;

    @PullParameter( defaultValue = "false" )
    private Boolean parsedWarningsAsErrors;

    /**
     * Use the given configuration file to determine whether issues are enabled or disabled. Defaults to "lint.xml".
     * 
     * @parameter expression="${android.lint.config}"
     * @see com.jayway.maven.plugins.android.configuration.Lint#config
     */
    private String lintConfig;

    @PullParameter( defaultValue = "null" )
    private String parsedConfig;

    // ---------------
    // Enabled Checks
    // ---------------

    /**
     * Use full paths in the error output. Defaults to "false".
     * 
     * @parameter expression="${android.lint.fullPath}"
     * @see com.jayway.maven.plugins.android.configuration.Lint#fullPath
     */
    private Boolean lintFullPath;

    @PullParameter( defaultValue = "false" )
    private Boolean parsedFullPath;

    /**
     * Do not truncate long messages, lists of alternate locations, etc. Defaults to "true".
     * 
     * @parameter expression="${android.lint.showAll}"
     * @see com.jayway.maven.plugins.android.configuration.Lint#showAll
     */
    private Boolean lintShowAll;

    @PullParameter( defaultValue = "true" )
    private Boolean parsedShowAll;

    /**
     * Do not include the source file lines with errors in the output. By default, the error output includes snippets of
     * source code on the line containing the error, but this flag turns it off. Defaults to "false".
     * 
     * @parameter expression="${android.lint.disableSourceLines}"
     * @see com.jayway.maven.plugins.android.configuration.Lint#disableSourceLines
     */
    private Boolean lintDisableSourceLines;

    @PullParameter( defaultValue = "false" )
    private Boolean parsedDisableSourceLines;

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
     * Skip the lint goal execution. Defaults to "true".
     * 
     * @parameter expression="${android.lint.html}"
     * @see com.jayway.maven.plugins.android.configuration.Lint#html
     */
    private Boolean lintHtml;

    @PullParameter( defaultValue = "true" )
    private Boolean parsedHtml;

//    
//    
//    
//    /**
//     * Enable the creation of a HTML report.
//     * @parameter expression="${android.lint.htmlOutput}:
//     * @see com.jayway.maven.plugins.android.configuration.Lint#htmlOutput
//     */
//    private Boolean lintHtmlOutput;
//    
//    @PullParameter( defaultValue = "true" )
//    private Boolean parsedHtmlOutput;
//    
    /**
     * Path for the HTML report. If the filename is a directory (or a new filename without an extension), lint will
     * create a separate report for each scanned project. No defaults.
     * 
     * @parameter expression="${android.lint.htmlOutputPath}"
     * @see com.jayway.maven.plugins.android.configuration.Lint#htmlOutputPath
     */
    private String lintHtmlOutputPath;

    @PullParameter( defaultValueGetterMethod = "getHtmlOutputPath" )
    private String parsedHtmlOutputPath;


    /**
     * Enable the creation of a simple HTML report.
     * @parameter expression="${android.lint.enableSimpleHtmlOutput}:
     * @see com.jayway.maven.plugins.android.configuration.Lint#enableSimpleHtmlOutput
     */
    private Boolean lintEnableSimpleHtmlOutput;
    
    @PullParameter( defaultValue = "false" )
    private Boolean parsedEnableSimpleHtmlOutput;

    
    /**
     * Create a simple HTML report. If the filename is a directory (or a new filename without an extension),
     * lint will create a separate report for each scanned project. No defaults.
     * 
     * @parameter expression="${android.lint.simpleHtml}"
     * @see com.jayway.maven.plugins.android.configuration.Lint#simpleHtml
     */
    private String lintSimpleHtmlOutputPath;

    @PullParameter( defaultValueGetterMethod = "getSimpleHtmlOutputPath" )
    private String parsedSimpleHtmlOutputPath;

    /**
     * Enable the creation of a XML report. Defaults to false.
     * @parameter expression="${android.lint.enableHtmlOutput}:
     * @see com.jayway.maven.plugins.android.configuration.Lint#enableHtmlOutput
     */
    private Boolean lintEnableXmlOutput;
    
    @PullParameter( defaultValue = "false" )
    private Boolean parsedEnableXmlOutput;

    /**
     * Create an XML report. If the filename is a directory (or a new filename without an extension), lint will
     * create a separate report for each scanned project. No defaults.
     * 
     * @parameter expression="${android.lint.xml}"
     * @see com.jayway.maven.plugins.android.configuration.Lint#xml
     */
    private String lintXmlOutputPath;

    @PullParameter( defaultValueGetterMethod = "getXmlOutputPath"  )
    private String parsedXmlOutputPath;

    // ---------------
    // Project Options
    // ---------------

    /**
     * Add the given folder (or path) as a source directory for the project. Only valid when running lint on a single
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

    private File htmlPath;
    private File simpleHtmlPath;
    private File xmlPath;

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
        getLog().debug( "ignoreWarnings:" + parsedIgnoreWarnings );
        getLog().debug( "warnAll:" + parsedWarnAll );
        getLog().debug( "warningsAsErrors:" + parsedWarningsAsErrors );
        getLog().debug( "config:" + parsedConfig );

        getLog().debug( "fullPath:" + parsedFullPath );
        getLog().debug( "showAll:" + parsedShowAll );
        getLog().debug( "disableSourceLines:" + parsedDisableSourceLines );
        
        getLog().debug( "html: " + parsedHtml );
        getLog().debug( "htmlOutputPath:" + parsedHtmlOutputPath );
        
        getLog().debug( "enableSimpleHtmlOutput: " + parsedEnableSimpleHtmlOutput );
        getLog().debug( "simpleHtmlOutputPath:" + parsedSimpleHtmlOutputPath );
        
        getLog().debug( "enableXmlOutput: " + parsedEnableXmlOutput );
        getLog().debug( "xmlOutputPath:" + parsedXmlOutputPath );

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
            if ( parsedIgnoreWarnings )
            {
                parameters.add( "-w" );
            }
            if ( parsedWarnAll )
            {
                parameters.add( "-Wall" );
            }
            if ( parsedWarningsAsErrors )
            {
                parameters.add( "-Werror" );
            }

            if ( !"null".equals( parsedConfig ) )
            {
                parameters.add( "--config" );
                parameters.add( parsedConfig );
            }

            if ( parsedFullPath )
            {
                parameters.add( "--fullpath" );
            }
            if ( parsedShowAll )
            {
                parameters.add( "--showall" );
            }
            if ( parsedDisableSourceLines )
            {
                parameters.add( "--nolines" );
            }
            if ( parsedHtml )
            {
                parameters.add( "--html" );
                parameters.add( parsedHtmlOutputPath );
                getLog().info( "Writing Lint HTML report in " + parsedHtmlOutputPath );
            }
            if ( !"none".equals( parsedUrl ) )
            {
                parameters.add( "--url" );
                parameters.add( parsedUrl );
            }
            if ( parsedEnableSimpleHtmlOutput )
            {
                parameters.add( "--simplehtml" );
                parameters.add( parsedSimpleHtmlOutputPath );
                getLog().info( "Writing Lint simple HTML report in " + parsedSimpleHtmlOutputPath );
            }
            if ( parsedEnableXmlOutput )
            {
                parameters.add( "--xml" );
                parameters.add( parsedXmlOutputPath );
                getLog().info( "Writing Lint XML report in " + parsedXmlOutputPath );
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
                    getLog().info( "Lint analysis produced errors and project is configured to fail on error." );
                    getLog().info( "Inspect lint reports or re-run with -X to see lint errors in log" );
                    getLog().info( "Failing build as configured. Ignore following error message." );
                    throw new MojoExecutionException( "" , e );
                }
            }
            getLog().info( "Lint analysis completed successfully." );
        }
    }
    
    // used via PullParameter annotation - do not remove
    private String getHtmlOutputPath()
    {
        if ( parsedHtmlOutputPath == null )
        {
            htmlPath = new File( project.getBuild().getDirectory(), "lint-html" );
        }
        return htmlPath.getAbsolutePath();
    }
    
    // used via PullParameter annotation - do not remove
    private String getSimpleHtmlOutputPath()
    {
        if ( parsedSimpleHtmlOutputPath == null )
        {
            simpleHtmlPath = new File( project.getBuild().getDirectory(), "lint-simple-html" );
        }
        return simpleHtmlPath.getAbsolutePath();
    }

    // used via PullParameter annotation - do not remove
    private String getXmlOutputPath()
    {
        if ( parsedXmlOutputPath == null )
        {
            xmlPath = new File( project.getBuild().getDirectory(), "lint-xml" );
        }
        return xmlPath.getAbsolutePath();
    }
}
