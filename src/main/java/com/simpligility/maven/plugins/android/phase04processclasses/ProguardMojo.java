
package com.simpligility.maven.plugins.android.phase04processclasses;

import com.simpligility.maven.plugins.android.AbstractAndroidMojo;
import com.simpligility.maven.plugins.android.CommandExecutor;
import com.simpligility.maven.plugins.android.ExecutionException;
import com.simpligility.maven.plugins.android.IncludeExcludeSet;
import com.simpligility.maven.plugins.android.config.ConfigHandler;
import com.simpligility.maven.plugins.android.config.ConfigPojo;
import com.simpligility.maven.plugins.android.config.PullParameter;
import com.simpligility.maven.plugins.android.configuration.Proguard;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.interpolation.os.Os;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static com.simpligility.maven.plugins.android.InclusionExclusionResolver.filterArtifacts;
import static com.simpligility.maven.plugins.android.common.AndroidExtension.AAR;

/**
 * Processes both application and dependency classes using the ProGuard byte code obfuscator,
 * minimzer, and optimizer. For more information, see https://proguard.sourceforge.net.
 *
 * @author Jonson
 * @author Matthias Kaeppler
 * @author Manfred Moser
 * @author Michal Harakal
 */
@Mojo(
        name = "proguard",
        defaultPhase = LifecyclePhase.PROCESS_CLASSES,
        requiresDependencyResolution = ResolutionScope.COMPILE
)
public class ProguardMojo extends AbstractAndroidMojo
{

    /**
     * <p>
     * ProGuard configuration. ProGuard is disabled by default. Set the skip parameter to false to activate proguard.
     * A complete configuartion can include any of the following:
     * </p>
     * <p/>
     * <pre>
     * &lt;proguard&gt;
     *    &lt;skip&gt;true|false&lt;/skip&gt;
     *    &lt;config&gt;proguard.cfg&lt;/config&gt;
     *    &lt;configs&gt;
     *      &lt;config&gt;${env.ANDROID_HOME}/tools/proguard/proguard-android.txt&lt;/config&gt;
     *    &lt;/configs&gt;
     *    &lt;proguardJarPath&gt;someAbsolutePathToProguardJar&lt;/proguardJarPath&gt;
     *    &lt;filterMavenDescriptor&gt;true|false&lt;/filterMavenDescriptor&gt;
     *    &lt;filterManifest&gt;true|false&lt;/filterManifest&gt;
     *    &lt;customFilter&gt;filter1,filter2&lt;/customFilter&gt;
     *    &lt;jvmArguments&gt;
     *     &lt;jvmArgument&gt;-Xms256m&lt;/jvmArgument&gt;
     *     &lt;jvmArgument&gt;-Xmx512m&lt;/jvmArgument&gt;
     *   &lt;/jvmArguments&gt;
     * &lt;/proguard&gt;
     * </pre>
     * <p>
     * A good practice is to create a release profile in your POM, in which you enable ProGuard.
     * ProGuard should be disabled for development builds, since it obfuscates class and field
     * names, and it may interfere with test projects that rely on your application classes.
     * All parameters can be overridden in profiles or the the proguard* properties. Default values apply and are
     * documented with these properties.
     * </p>
     */
    @ConfigPojo
    @Parameter
    protected Proguard proguard;

    /**
     * Whether ProGuard is enabled or not. Defaults to true.
     */
    @Parameter( property = "android.proguard.skip" )
    private Boolean proguardSkip;

    @PullParameter( defaultValue = "true" )
    private Boolean parsedSkip;

    @PullParameter( defaultValue = "${project.basedir}/proguard.cfg" )
    private File parsedConfig;

    @PullParameter( defaultValueGetterMethod = "getDefaultProguardConfigs" )
    private String[] parsedConfigs;

    /**
     * Additional ProGuard options
     */
    @Parameter( property = "android.proguard.options" )
    private String[] proguardOptions;

    @PullParameter( defaultValueGetterMethod = "getDefaultProguardOptions" )
    private String[] parsedOptions;

    /**
     * Path to the proguard jar and therefore version of proguard to be used. By default this will load the jar from
     * the Android SDK install. Overriding it with an absolute path allows you to use a newer or custom proguard
     * version..
     * <p/>
     * You can also reference an external Proguard version as a plugin dependency like this:
     * <pre>
     * &lt;plugin&gt;
     *   &lt;groupId&gt;com.jayway.maven.plugins.android.generation2&lt;/groupId&gt;
     *   &lt;artifactId&gt;android-maven-plugin&lt;/artifactId&gt;
     *     &lt;dependencies&gt;
     *       &lt;dependency&gt;
     *         &lt;groupId&gt;net.sf.proguard&lt;/groupId&gt;
     *         &lt;artifactId&gt;proguard-base&lt;/artifactId&gt;
     *         &lt;version&gt;4.7&lt;/version&gt;
     *       &lt;/dependency&gt;
     *     &lt;/dependencies&gt;
     * </pre>
     * <p/>
     * which will download and use Proguard 4.7 as deployed to the Central Repository.
     */
    @Parameter( property = "android.proguard.proguardJarPath" )
    private String proguardProguardJarPath;

    @PullParameter( defaultValueGetterMethod = "getProguardJarPath" )
    private String parsedProguardJarPath;

    /**
     * Path relative to the project's build directory (target) where proguard puts folowing files:
     * <p/>
     * <ul>
     *   <li>dump.txt</li>
     *   <li>seeds.txt</li>
     *   <li>usage.txt</li>
     *   <li>mapping.txt</li>
     * </ul>
     * <p/>
     * You can define the directory like this:
     * <pre>
     * &lt;proguard&gt;
     *   &lt;skip&gt;false&lt;/skip&gt;
     *   &lt;config&gt;proguard.cfg&lt;/config&gt;
     *   &lt;outputDirectory&gt;my_proguard&lt;/outputDirectory&gt;
     * &lt;/proguard&gt;
     * </pre>
     * <p/>
     * Output directory is defined relatively so it could be also outside of the target directory.
     * <p/>
     */
    @Parameter( property = "android.proguard.outputDirectory" )
    private File outputDirectory;

    @PullParameter( defaultValue = "${project.build.directory}/proguard" )
    private File parsedOutputDirectory;

    @Parameter(
            property = "android.proguard.obfuscatedJar",
            defaultValue = "${project.build.directory}/${project.build.finalName}_obfuscated.jar"
    )
    private String obfuscatedJar;

    /**
     * Extra JVM Arguments. Using these you can e.g. increase memory for the jvm running the build.
     * Defaults to "-Xmx512M".
     */
    @Parameter( property = "android.proguard.jvmArguments" )
    private String[] proguardJvmArguments;

    @PullParameter( defaultValueGetterMethod = "getDefaultJvmArguments" )
    private String[] parsedJvmArguments;

    /**
     * If set to true will add a filter to remove META-INF/maven/* files. Defaults to false.
     */
    @Parameter( property = "android.proguard.filterMavenDescriptor" )
    private Boolean proguardFilterMavenDescriptor;

    @PullParameter( defaultValue = "true" )
    private Boolean parsedFilterMavenDescriptor;

    /**
     * If set to true will add a filter to remove META-INF/MANIFEST.MF files.  Defaults to false.
     */
    @Parameter( property = "android.proguard.filterManifest" )
    private Boolean proguardFilterManifest;

    @PullParameter( defaultValue = "true" )
    private Boolean parsedFilterManifest;

    /**
     * You can specify a custom filter which will be used to filter out unnecessary files from ProGuard input.
     *
     * @see http://proguard.sourceforge.net/manual/usage.html#filefilters
     */
    @Parameter( property = "android.proguard.customfilter" )
    private String proguardCustomFilter;

    @PullParameter
    private String parsedCustomFilter;

    /**
     * If set to true JDK jars will be included as library jars and corresponding filters
     * will be applied to android.jar.
     */
    @Parameter( property = "android.proguard.includeJdkLibs" )
    private Boolean includeJdkLibs;

    @PullParameter( defaultValue = "true" )
    private Boolean parsedIncludeJdkLibs;

    /**
     * If set to true the mapping.txt file will be attached as artifact of type <code>map</code>
     */
    @Parameter( property = "android.proguard.attachMap" )
    private Boolean attachMap;

    @PullParameter( defaultValue = "false" )
    private Boolean parsedAttachMap;

    /**
     * The plugin dependencies.
     */
    @Parameter( defaultValue = "${plugin.artifacts}", required = true, readonly = true )
    protected List< Artifact > pluginDependencies;

    /**
     * Skips transitive dependencies. May be useful if the target classes directory is populated with the
     * {@code maven-dependency-plugin} and already contains all dependency classes.
     */
    @Parameter( property = "skipDependencies", defaultValue = "false" )
    private boolean skipDependencies;

    /**
     * Allows to include or exclude artifacts by type. The {@code include} parameter has higher priority than the
     * {@code exclude} parameter. These two parameters can be overridden by the {@code artifactSet} parameter. Empty
     * strings are ignored. Example:
     * <pre>
     *     &lt;artifactTypeSet&gt;
     *         &lt;includes&gt;
     *             &lt;include&gt;aar&lt;/include&gt;
     *         &lt;includes&gt;
     *         &lt;excludes&gt;
     *             &lt;exclude&gt;jar&lt;/exclude&gt;
     *         &lt;excludes&gt;
     *     &lt;/artifactTypeSet&gt;
     * </pre>
     */
    @Parameter( property = "artifactTypeSet" )
    private IncludeExcludeSet artifactTypeSet;

    /**
     * Allows to include or exclude artifacts by {@code groupId}, {@code artifactId}, and {@code versionId}. The
     * {@code include} parameter has higher priority than the {@code exclude} parameter. These two parameters can
     * override the {@code artifactTypeSet} and {@code skipDependencies} parameters. Artifact {@code groupId},
     * {@code artifactId}, and {@code versionId} are specified by a string with the respective values separated using
     * a colon character {@code :}. {@code artifactId} and {@code versionId} can be optional covering an artifact
     * range. Empty strings are ignored. Example:
     * <pre>
     *     &lt;artifactTypeSet&gt;
     *         &lt;includes&gt;
     *             &lt;include&gt;foo-group:foo-artifact:1.0-SNAPSHOT&lt;/include&gt;
     *             &lt;include&gt;bar-group:bar-artifact:1.0-SNAPSHOT&lt;/include&gt;
     *             &lt;include&gt;baz-group:*&lt;/include&gt;
     *         &lt;includes&gt;
     *         &lt;excludes&gt;
     *             &lt;exclude&gt;qux-group:qux-artifact:*&lt;/exclude&gt;
     *         &lt;excludes&gt;
     *     &lt;/artifactTypeSet&gt;
     * </pre>
     */
    @Parameter( property = "artifactSet" )
    private IncludeExcludeSet artifactSet;

    private static final Collection< String > ANDROID_LIBRARY_EXCLUDED_FILTER = Arrays
        .asList( "org/xml/**", "org/w3c/**", "java/**", "javax/**" );

    private static final Collection< String > MAVEN_DESCRIPTOR = Arrays.asList( "META-INF/maven/**" );

    private static final Collection< String > META_INF_MANIFEST = Arrays.asList( "META-INF/MANIFEST.MF" );

    /**
     * For Proguard is required only jar type dependencies, all other like .so or .apklib can be skipped.
     */
    private static final String JAR_DEPENDENCY_TYPE = "jar";

    private static class ArtifactPrototype
    {
        private final String groupId;
        private final String artifactId;

        private ArtifactPrototype( String groupId, String artifactId )
        {
            this.groupId = groupId;
            this.artifactId = artifactId;
        }
    }

    private List< ArtifactPrototype > artifactBlacklist = new LinkedList< ArtifactPrototype>();

    private List< ArtifactPrototype > artifactsToShift = new LinkedList< ArtifactPrototype>();

    private File javaHomeDir;

    private File javaLibDir;

    private File altJavaLibDir;

    private static class ProGuardInput
    {

        private String path;

        private Collection< String > excludedFilter;

        ProGuardInput( String path, Collection< String > excludedFilter )
        {
            this.path = path;
            this.excludedFilter = excludedFilter;
        }

        public String toPath()
        {
            if ( excludedFilter != null && !excludedFilter.isEmpty() )
            {
                String middleQuote, endQuote;

                if ( !Os.isFamily( Os.FAMILY_WINDOWS ) )
                {
                    middleQuote = "(";
                    endQuote = ")";
                }
                else
                {
                    middleQuote = "(";
                    endQuote = ")";
                }

                StringBuilder sb = new StringBuilder();
                sb.append( path );
                sb.append( middleQuote );
                for ( Iterator< String > it = excludedFilter.iterator(); it.hasNext(); )
                {
                    sb.append( '!' ).append( it.next() );
                    if ( it.hasNext() )
                    {
                        sb.append( ',' );
                    }
                }
                sb.append( endQuote );
                return sb.toString();
            }
            else
            {
                return path;
            }
        }

        @Override
        public String toString()
        {
            return "ProGuardInput{"
                    + "path='" + path + '\''
                    + ", excludedFilter=" + excludedFilter
                    + '}';
        }
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        ConfigHandler configHandler = new ConfigHandler( this, this.session, this.execution );
        configHandler.parseConfiguration();

        if ( !parsedSkip )
        {
            if ( parsedConfig.exists() )
            {
                // TODO: make the property name a constant sometime after switching to @Mojo
                project.getProperties().setProperty( "android.proguard.obfuscatedJar", obfuscatedJar );

                executeProguard();
            }
            else
            {
                getLog().info( String
                    .format( "Proguard skipped because the configuration file doesn't exist: %s", parsedConfig ) );
            }
        }
    }

    private void executeProguard() throws MojoExecutionException
    {
        final File proguardDir = this.parsedOutputDirectory;

        if ( !proguardDir.exists() && !proguardDir.mkdir() )
        {
            throw new MojoExecutionException( "Cannot create proguard output directory" );
        }
        else
        {
            if ( proguardDir.exists() && !proguardDir.isDirectory() )
            {
                throw new MojoExecutionException( "Non-directory exists at " + proguardDir.getAbsolutePath() );
            }
        }

        getLog().info( "Proguarding output" );
        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger( this.getLog() );
        List< String > commands = new ArrayList< String >();

        collectJvmArguments( commands );

        commands.add( "-jar" );
        commands.add( parsedProguardJarPath );

        List<String> proguardCommands = new ArrayList<String>();

        proguardCommands.add( "@" + parsedConfig + "" );

        for ( String config : parsedConfigs )
        {
            proguardCommands.add( "@" + config );
        }

        if ( proguardFile != null )
        {
            proguardCommands.add( "@" + proguardFile.getAbsolutePath() );
        }

        for ( Artifact artifact : getTransitiveDependencyArtifacts( AAR ) )
        {
            File unpackedLibFolder = getUnpackedLibFolder( artifact );
            File proguardFile = new File( unpackedLibFolder, "proguard.txt" );
            if ( proguardFile.exists() )
            {
                proguardCommands.add( "@" + proguardFile.getAbsolutePath() );
            }
        }

        collectInputFiles( proguardCommands );

        proguardCommands.add( "-outjars" );
        proguardCommands.add( obfuscatedJar );

        proguardCommands.add( "-dump" );
        proguardCommands.add( proguardDir + File.separator + "dump.txt" );
        proguardCommands.add( "-printseeds" );
        proguardCommands.add( proguardDir + File.separator + "seeds.txt" );
        proguardCommands.add( "-printusage" );
        proguardCommands.add( proguardDir + File.separator + "usage.txt" );

        File mapFile = new File( proguardDir, "mapping.txt" );

        proguardCommands.add( "-printmapping" );
        proguardCommands.add( mapFile.toString() );

        proguardCommands.addAll( Arrays.asList( parsedOptions ) );

        final String javaExecutable = getJavaExecutable().getAbsolutePath();

        getLog().debug( javaExecutable + " " + commands.toString() + proguardCommands.toString() );

        FileOutputStream tempConfigFileOutputStream = null;
        try
        {
            File tempConfigFile = new File ( proguardDir , "temp_config.cfg" );

            StringBuilder commandStringBuilder = new StringBuilder();
            for ( String command : proguardCommands )
            {
                commandStringBuilder.append( command );
                commandStringBuilder.append( SystemUtils.LINE_SEPARATOR );
            }
            tempConfigFileOutputStream = new FileOutputStream( tempConfigFile );
            IOUtils.write( commandStringBuilder, tempConfigFileOutputStream );

            executor.setCaptureStdOut( true );
            commands.add( "@" + tempConfigFile.getAbsolutePath() + "" );
            executor.executeCommand( javaExecutable, commands, project.getBasedir(), false );
        }
        catch ( ExecutionException e )
        {
            throw new MojoExecutionException( "", e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error writing proguard commands to temporary file", e );
        }
        finally
        {
            IOUtils.closeQuietly( tempConfigFileOutputStream );
        }

        if ( parsedAttachMap )
        {
            projectHelper.attachArtifact( project, "map", mapFile );
        }
    }

    /**
     * Convert the jvm arguments in parsedJvmArguments as populated by the config in format as needed by the java
     * command. Also preserve backwards compatibility in terms of dashes required or not..
     */
    private void collectJvmArguments( List< String > commands )
    {
        if ( parsedJvmArguments != null )
        {
            for ( String jvmArgument : parsedJvmArguments )
            {
                // preserve backward compatibility allowing argument with or without dash (e.g.
                // Xmx512m as well as -Xmx512m should work) (see
                // http://code.google.com/p/maven-android-plugin/issues/detail?id=153)
                if ( !jvmArgument.startsWith( "-" ) )
                {
                    jvmArgument = "-" + jvmArgument;
                }
                commands.add( jvmArgument );
            }
        }
    }

    private void collectInputFiles( List< String > commands ) throws MojoExecutionException
    {
        // commons-logging breaks everything horribly, so we skip it from the program
        // dependencies and declare it to be a library dependency instead
        skipArtifact( "commons-logging", "commons-logging", true );

        final List< ProGuardInput > inJars = getProgramInputFiles();
        if ( isAPKBuild() )
        {
            inJars.addAll( getProjectDependencyFiles() );
        }

        for ( final ProGuardInput injar : inJars )
        {
            getLog().debug( "Added injar : " + injar );
            commands.add( "-injars" );
            commands.add( injar.toPath() );
        }

        final List< ProGuardInput > libraryJars = getLibraryInputFiles();
        if ( !isAPKBuild() )
        {
            getLog().info( "Library project - not adding project dependencies to the obfuscated JAR" );
            libraryJars.addAll( getProjectDependencyFiles() );
        }

        for ( final ProGuardInput libraryjar : libraryJars )
        {
            getLog().debug( "Added libraryJar : " + libraryjar );
            commands.add( "-libraryjars" );
            commands.add( libraryjar.toPath() );
        }
    }

    /**
     * Figure out the full path to the current java executable.
     *
     * @return the full path to the current java executable.
     */
    private static File getJavaExecutable()
    {
        final String javaHome = System.getProperty( "java.home" );
        final String slash = File.separator;
        return new File( javaHome + slash + "bin" + slash + "java" );
    }

    private void skipArtifact( String groupId, String artifactId, boolean shiftToLibraries )
            throws MojoExecutionException
    {
        final ArtifactPrototype artifact = new ArtifactPrototype( groupId, artifactId );
        artifactBlacklist.add( artifact );
        if ( shiftToLibraries )
        {
            artifactsToShift.add( artifact );
        }
    }

    private boolean isBlacklistedArtifact( Artifact artifact )
    {
        for ( ArtifactPrototype artifactToSkip : artifactBlacklist )
        {
            if ( artifactToSkip.groupId.equals( artifact.getGroupId() )
                    && artifactToSkip.artifactId.equals( artifact.getArtifactId() ) )
            {
                return true;
            }
        }
        return false;
    }

    private boolean isShiftedArtifact( Artifact artifact )
    {
        for ( ArtifactPrototype artifactToShift : artifactsToShift )
        {
            if ( artifactToShift.groupId.equals( artifact.getGroupId() )
                    && artifactToShift.artifactId.equals( artifact.getArtifactId() ) )
            {
                return true;
            }
        }
        return false;
    }

    private List< ProGuardInput > getProgramInputFiles()
    {
        final List< ProGuardInput > inJars = new LinkedList< ProguardMojo.ProGuardInput >();
        inJars.add( createProguardInput( projectOutputDirectory.getAbsolutePath() ) );
        return inJars;
    }

    private List< ProGuardInput > getProjectDependencyFiles()
    {
        final Collection< String > globalInJarExcludes = new HashSet< String >();
        final List< ProGuardInput > inJars = new LinkedList< ProguardMojo.ProGuardInput >();

        if ( parsedFilterManifest )
        {
            globalInJarExcludes.addAll( META_INF_MANIFEST );
        }
        if ( parsedFilterMavenDescriptor )
        {
            globalInJarExcludes.addAll( MAVEN_DESCRIPTOR );
        }
        if ( parsedCustomFilter != null )
        {
            globalInJarExcludes.addAll( Arrays.asList( parsedCustomFilter.split( "," ) ) );
        }

        // we then add all its dependencies (incl. transitive ones), unless they're blacklisted
        for ( Artifact artifact : filterArtifacts( getTransitiveDependencyArtifacts(), skipDependencies,
                    artifactTypeSet.getIncludes(), artifactTypeSet.getExcludes(), artifactSet.getIncludes(),
                    artifactSet.getExcludes() ) )
        {
            if ( isBlacklistedArtifact( artifact ) )
            {
                getLog().debug( "Excluding (blacklisted) dependency as input jar : " + artifact );
                continue;
            }

            if ( JAR_DEPENDENCY_TYPE.equals( artifact.getType() ) )
            {
                getLog().debug( "Including dependency as input jar : " + artifact );
                inJars.add( createProguardInput( artifact.getFile().getAbsolutePath(), globalInJarExcludes ) );
            }
            else if ( AAR.equals( artifact.getType() ) )
            {
                // The Aar classes.jar should now be automatically included
                // because it will be a transitive dependency. As should any jars in the libs folder.
            }
            else
            {
                getLog().debug( "Excluding dependency as input jar : " + artifact );
            }
        }

        return inJars;
    }

    private ProGuardInput createProguardInput( String path, Collection< String > filterExpression )
    {
        return new ProGuardInput( path, filterExpression );
    }

    private ProGuardInput createProguardInput( String path )
    {
        return createProguardInput( path, null );
    }

    private List< ProGuardInput > getLibraryInputFiles()
    {
        final List< ProGuardInput > libraryJars = new LinkedList< ProguardMojo.ProGuardInput >();

        if ( parsedIncludeJdkLibs )
        {
            // we have to add the Java framework classes to the library JARs, since they are not
            // distributed with the JAR on Central, and since we'll strip them out of the android.jar
            // that is shipped with the SDK (since that is not a complete Java distribution)
            File rtJar = getJVMLibrary( "rt.jar" );
            if ( rtJar == null )
            {
                rtJar = getJVMLibrary( "classes.jar" );
            }
            if ( rtJar != null )
            {
                libraryJars.add( createProguardInput( rtJar.getPath() ) );
            }

            // we also need to add the JAR containing e.g. javax.servlet
            File jsseJar = getJVMLibrary( "jsse.jar" );
            if ( jsseJar != null )
            {
                libraryJars.add( createProguardInput( jsseJar.getPath() ) );
            }

            // and the javax.crypto stuff
            File jceJar = getJVMLibrary( "jce.jar" );
            if ( jceJar != null )
            {
                libraryJars.add( createProguardInput( jceJar.getPath() ) );
            }
        }

        // we treat any dependencies with provided scope as library JARs
        for ( Artifact artifact : project.getArtifacts() )
        {
            if ( artifact.getScope().equals( Artifact.SCOPE_PROVIDED ) )
            {
                if ( artifact.getArtifactId().equals( "android" ) && parsedIncludeJdkLibs )
                {
                    getLog().debug( "Including dependency as (android) library jar : " + artifact );
                    libraryJars.add(
                            createProguardInput( artifact.getFile().getAbsolutePath(), ANDROID_LIBRARY_EXCLUDED_FILTER )
                    );
                }
                else
                {
                    getLog().debug( "Including dependency as (provided) library jar : " + artifact );
                    libraryJars.add( createProguardInput( artifact.getFile().getAbsolutePath() ) );
                }
            }
            else
            {
                if ( isShiftedArtifact( artifact ) )
                {
                    // this is a blacklisted artifact that should be processed as a library instead
                    getLog().debug( "Including dependency as (shifted) library jar : " + artifact );
                    libraryJars.add( createProguardInput( artifact.getFile().getAbsolutePath() ) );
                }
                else
                {
                    getLog().debug( "Excluding dependency as library jar : " + artifact );
                }
            }
        }

        return libraryJars;
    }

    /**
     * Get the path to the proguard jar.
     */
    @SuppressWarnings( "unused" ) // NB Used to populate the parsedProguardJarPath attribute via reflection.
    private String getProguardJarPath() throws MojoExecutionException
    {
        String proguardJarPath = getProguardJarPathFromDependencies();
        if ( StringUtils.isEmpty( proguardJarPath ) )
        {
            File proguardJarPathFile = new File( getAndroidSdk().getToolsPath(), "proguard/lib/proguard.jar" );
            return proguardJarPathFile.getAbsolutePath();
        }
        return proguardJarPath;
    }

    private String getProguardJarPathFromDependencies() throws MojoExecutionException
    {
        Artifact proguardArtifact = null;
        int proguardArtifactDistance = -1;
        for ( Artifact artifact : pluginDependencies )
        {
            getLog().debug( "pluginArtifact: " + artifact.getFile() );
            if ( ( "proguard".equals( artifact.getArtifactId() ) ) || ( "proguard-base"
                .equals( artifact.getArtifactId() ) ) )
            {
                int distance = artifact.getDependencyTrail().size();
                getLog().debug( "proguard DependencyTrail: " + distance );
                if ( proguardArtifactDistance == -1 )
                {
                    proguardArtifact = artifact;
                    proguardArtifactDistance = distance;
                }
                else
                {
                    if ( distance < proguardArtifactDistance )
                    {
                        proguardArtifact = artifact;
                        proguardArtifactDistance = distance;
                    }
                }
            }
        }
        if ( proguardArtifact != null )
        {
            getLog().debug( "proguardArtifact: " + proguardArtifact.getFile() );
            return proguardArtifact.getFile().getAbsoluteFile().toString();
        }
        else
        {
            return null;
        }

    }

    /**
     * Get the default JVM arguments for the proguard invocation.
     *
     * @see #parsedJvmArguments
     */
    @SuppressWarnings( "unused" ) // Provides default value for parsedJvmArguments attribute
    private String[] getDefaultJvmArguments()
    {
        return new String[] { "-Xmx512M" };
    }

    /**
     * Get the default ProGuard config files.
     *
     * @see #parsedConfigs
     */
    @SuppressWarnings( "unused" ) // Provides default value for parsedConfigs attribute
    private String[] getDefaultProguardConfigs()
    {
        return new String[0];
    }

    /**
     * Get the default ProGuard options.
     *
     * @see #parsedOptions
     */
    @SuppressWarnings( "unused" ) // Provides default value for parsedOptions attribute
    private String[] getDefaultProguardOptions()
    {
        return new String[0];
    }

    /**
     * Finds a library file in either the primary or alternate lib directory.
     * @param fileName The base name of the file.
     * @return Either a canonical filename, or {@code null} if not found.
     */
    private File getJVMLibrary( String fileName )
    {
        File libFile = new File( getJavaLibDir(), fileName );
        if ( !libFile.exists() )
        {
            libFile = new File( getAltJavaLibDir(), fileName );
            if ( !libFile.exists() )
            {
                libFile = null;
            }
        }
        return libFile;
    }

    /**
     * Determines the java.home directory.
     * @return The java.home directory, as a File.
     */
    private File getJavaHomeDir()
    {
        if ( javaHomeDir == null )
        {
            javaHomeDir = new File( System.getProperty( "java.home" ) );
        }
        return javaHomeDir;
    }

    /**
     * Determines the primary JVM library location.
     * @return The primary library directory, as a File.
     */
    private File getJavaLibDir()
    {
        if ( javaLibDir == null )
        {
            javaLibDir = new File( getJavaHomeDir(), "lib" );
        }
        return javaLibDir;
    }

    /**
     * Determines the alternate JVM library location (applies with older
     * MacOSX JVMs).
     * @return The alternate JVM library location, as a File.
     */
    private File getAltJavaLibDir()
    {
        if ( altJavaLibDir == null )
        {
            altJavaLibDir = new File( getJavaHomeDir().getParent(), "Classes" );
        }
        return altJavaLibDir;
    }
}
