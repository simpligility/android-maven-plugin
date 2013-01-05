package com.jayway.maven.plugins.android.standalonemojos;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
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
@SuppressWarnings( "unused" )
public class LintMojo extends AbstractAndroidMojo
{

    /**
     * The configuration for the lint goal. As soon as a lint goal is invoked the command will be executed unless the
     * skip parameter is set. A minimal configuration that will run lint and produce a HTML report in
     * ${project.build.directory}/lint-html is
     * 
     * <pre>
     * &lt;lint&gt;
     *     &lt;skip&gt;false&lt;/skip&gt;
     * &lt;/lint&gt;
     * </pre>
     * 
     * Full configuration can use these parameters.
     * 
     * <pre>
     * &lt;lint&gt;
     *     &lt;failOnError&gt;true|false&lt;/failOnError&gt;
     *     &lt;skip&gt;true|false&lt;/skip&gt;
     *     &lt;ignoreWarnings&gt;true|false&lt;/ignoreWarnings&gt;
     *     &lt;warnAll&gt;true|false&lt;/warnAll&gt;
     *     &lt;warningsAsErrors&gt;true|false&lt;/warningsAsErrors&gt;
     *     &lt;fullPath&gt;true|false&lt;/fullPath&gt;
     *     &lt;showAll&gt;true|false&lt;/showAll&gt;
     *     &lt;disableSourceLines&gt;true|false&lt;/disableSourceLines&gt;
     *     &lt;url&gt;true|false&lt;/url&gt;
     *     &lt;enableHtml&gt;true|false&lt;/enableHtml&gt;
     *     &lt;htmlOutputPath&gt;${project.build.directory}/lint-html&lt;/htmlOutputPath&gt;
     *     &lt;enableSimpleHtml&gt;true|false&lt;/enableSimpleHtml&gt;
     *     &lt;simpleHtmlOutputPath&gt;${project.build.directory}/lint-simple-html&lt;/simpleHtmlOutputPath&gt;
     *     &lt;enableXml&gt;true|false&lt;/enableXml&gt;
     *     &lt;xmlOutputPath&gt;${project.build.directory}/lint.xml&lt;/xmlOutputPath&gt;
     *     &lt;sources&gt;&lt;/sources&gt;
     *     &lt;classpath&gt;&lt;/classpath&gt;
     *     &lt;libraries&gt;&lt;/libraries&gt;
     * &lt;/lint&gt;
     * </pre>
     * 
     * 
     * Values can also be configured as properties on the command line as android.lint.* or in pom or settings file as
     * properties like lint*.
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
     * Use the given configuration file to determine whether issues are enabled or disabled. Defaults is empty. Use
     * ${project.basedir}/lint.xml for the default "lint.xml" in the project root directory.
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
     * Enable the creation of a HTML report. Defaults to "true".
     * 
     * @parameter expression="${android.lint.enableHtml}"
     * @see com.jayway.maven.plugins.android.configuration.Lint#enableHtml
     */
    private Boolean lintEnableHtml;

    @PullParameter( defaultValue = "false" )
    private Boolean parsedEnableHtml;

    /**
     * Path for the HTML report. If the filename is a directory (or a new filename without an extension), lint will
     * create a separate report for each scanned project. Defaults to ${project.build.directory}/lint-html/.
     * 
     * @parameter expression="${android.lint.htmlOutputPath}"
     * @see com.jayway.maven.plugins.android.configuration.Lint#htmlOutputPath
     */
    private String lintHtmlOutputPath;

    @PullParameter( defaultValueGetterMethod = "getHtmlOutputPath" )
    private String parsedHtmlOutputPath;

    /**
     * Enable the creation of a simple HTML report. Defaults to "false".
     * 
     * @parameter expression="${android.lint.enableSimpleHtml}"
     * @see com.jayway.maven.plugins.android.configuration.Lint#enableSimpleHtml
     */
    private Boolean lintEnableSimpleHtml;

    @PullParameter( defaultValue = "false" )
    private Boolean parsedEnableSimpleHtml;

    /**
     * Create a simple HTML report. If the filename is a directory (or a new filename without an extension), lint will
     * create a separate report for each scanned project. Defaults to ${project.build.directory}/lint-simple-html/.
     * 
     * @parameter expression="${android.lint.simpleHtmlOutputPath}"
     * @see com.jayway.maven.plugins.android.configuration.Lint#simpleHtml
     */
    private String lintSimpleHtmlOutputPath;

    @PullParameter( defaultValueGetterMethod = "getSimpleHtmlOutputPath" )
    private String parsedSimpleHtmlOutputPath;

    /**
     * Enable the creation of a XML report. Defaults to "false".
     * 
     * @parameter expression="${android.lint.enableXml}"
     * @see com.jayway.maven.plugins.android.configuration.Lint#enableXml
     */
    private Boolean lintEnableXml;

    @PullParameter( defaultValue = "true" )
    private Boolean parsedEnableXml;

    /**
     * Create an XML report. If the filename is a directory (or a new filename without an extension), lint will create a
     * separate report for each scanned project. Defaults to ${project.build.directory}/lint.xml.
     * 
     * @parameter expression="${android.lint.xmlOutputPath}"
     * @see com.jayway.maven.plugins.android.configuration.Lint#xmlOutputPath
     */
    private String lintXmlOutputPath;

    @PullParameter( defaultValueGetterMethod = "getXmlOutputPath" )
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
     * 
     */
    private String lintSources;

    @PullParameter( defaultValueGetterMethod = "getSources" )
    private String parsedSources;

    /**
     * Add the given folder (or jar file, or path) as a class directory for the project. Only valid when running lint on
     * a single project. No defaults.
     * 
     * @parameter expression="${android.lint.classpath}"
     * @see com.jayway.maven.plugins.android.configuration.Lint#classpath
     * 
     */
    private String lintClasspath;

    @PullParameter( defaultValueGetterMethod = "getClasspath" )
    private String parsedClasspath;

    /**
     * Add the given folder (or jar file, or path) as a class library for the project. Only valid when running lint on a
     * single project. No defaults.
     * 
     * @parameter expression="${android.lint.libraries}"
     * @see com.jayway.maven.plugins.android.configuration.Lint#lintLibraries
     * 
     */
    private String lintLibraries;

    @PullParameter( defaultValueGetterMethod = "getLibraries" )
    private String parsedLibraries;

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

        getLog().debug( "Parsed values for Android Lint invocation: " );
        getLog().debug( "failOnError:" + parsedFailOnError );
        getLog().debug( "skip:" + parsedSkip );
        getLog().debug( "ignoreWarnings:" + parsedIgnoreWarnings );
        getLog().debug( "warnAll:" + parsedWarnAll );
        getLog().debug( "warningsAsErrors:" + parsedWarningsAsErrors );
        getLog().debug( "android.lint.config:" + parsedConfig );

        getLog().debug( "fullPath:" + parsedFullPath );
        getLog().debug( "showAll:" + parsedShowAll );
        getLog().debug( "disableSourceLines:" + parsedDisableSourceLines );

        getLog().debug( "enablehtml: " + parsedEnableHtml );
        getLog().debug( "htmlOutputPath:" + parsedHtmlOutputPath );

        getLog().debug( "enableSimpleHtml: " + parsedEnableSimpleHtml );
        getLog().debug( "simpleHtmlOutputPath:" + parsedSimpleHtmlOutputPath );

        getLog().debug( "enableXml: " + parsedEnableXml );
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
            if ( parsedEnableHtml )
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
            if ( parsedEnableSimpleHtml )
            {
                parameters.add( "--simplehtml" );
                parameters.add( parsedSimpleHtmlOutputPath );
                getLog().info( "Writing Lint simple HTML report in " + parsedSimpleHtmlOutputPath );
            }
            if ( parsedEnableXml )
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
                    throw new MojoExecutionException( "", e );
                }
            }
            getLog().info( "Lint analysis completed successfully." );
        }
    }

    // used via PullParameter annotation - do not remove
    private String getConfig()
    {
        if ( parsedConfig == null )
        {
            return new File( project.getBuild().getDirectory(), "lint.xml" ).getAbsolutePath();
        }
        return parsedConfig;
    }

    // used via PullParameter annotation - do not remove
    private String getHtmlOutputPath()
    {
        if ( parsedHtmlOutputPath == null )
        {
            return new File( project.getBuild().getDirectory(), "lint-html" ).getAbsolutePath();
        }
        return parsedHtmlOutputPath;
    }

    // used via PullParameter annotation - do not remove
    private String getSimpleHtmlOutputPath()
    {
        if ( parsedSimpleHtmlOutputPath == null )
        {
            return new File( project.getBuild().getDirectory(), "lint-simple-html" ).getAbsolutePath();
        }
        return parsedSimpleHtmlOutputPath;
    }

    // used via PullParameter annotation - do not remove
    private String getXmlOutputPath()
    {
        getLog().debug( "get parsed xml output path:" + parsedXmlOutputPath );

        if ( parsedXmlOutputPath == null )
        {
            return new File( project.getBuild().getDirectory(), "lint.xml" ).getAbsolutePath();
        }
        return parsedXmlOutputPath;
    }

    // used via PullParameter annotation - do not remove
    private String getSources()
    {
        if ( parsedSources == null )
        {
            parsedSources = project.getBuild().getSourceDirectory();
        }
        return parsedSources;
    }

    // used via PullParameter annotation - do not remove
    private String getClasspath()
    {
        if ( parsedClasspath == null )
        {
            parsedClasspath = project.getBuild().getOutputDirectory();
        }
        return parsedClasspath;
    }

    private String getLibraries()
    {
        if ( parsedLibraries == null )
        {
            StringBuilder defaultClasspathBuilder = new StringBuilder();
            Set< Artifact > artifacts = project.getDependencyArtifacts();
            if ( artifacts != null )
            {
                for ( Artifact artifact : artifacts )
                {
                    if ( !Artifact.SCOPE_PROVIDED.equals( artifact.getScope() ) && artifact.isResolved() )
                    {
                        defaultClasspathBuilder.append( artifact.getFile().getPath() );
                        defaultClasspathBuilder.append( File.pathSeparator );
                    }
                }
                if ( defaultClasspathBuilder.length() > 0 )
                {
                    defaultClasspathBuilder.deleteCharAt( defaultClasspathBuilder.length() - 1 );
                    parsedLibraries = defaultClasspathBuilder.toString();
                }
            }
        }
        return parsedLibraries;
    }
}
