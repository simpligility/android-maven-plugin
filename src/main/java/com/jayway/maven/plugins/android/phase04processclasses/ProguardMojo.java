
package com.jayway.maven.plugins.android.phase04processclasses;

import com.jayway.maven.plugins.android.AbstractAndroidMojo;
import com.jayway.maven.plugins.android.CommandExecutor;
import com.jayway.maven.plugins.android.ExecutionException;
import com.jayway.maven.plugins.android.config.ConfigHandler;
import com.jayway.maven.plugins.android.config.ConfigPojo;
import com.jayway.maven.plugins.android.config.PullParameter;
import com.jayway.maven.plugins.android.configuration.Proguard;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.interpolation.os.Os;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static com.jayway.maven.plugins.android.common.AndroidExtension.AAR;

/**
 * Processes both application and dependency classes using the ProGuard byte code obfuscator,
 * minimzer, and optimizer. For more information, see https://proguard.sourceforge.net.
 *
 * @author Jonson
 * @author Matthias Kaeppler
 * @author Manfred Moser
 * @author Michal Harakal
 * @goal proguard
 * @phase process-classes
 * @requiresDependencyResolution compile
 */
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
     *
     * @parameter
     */
    @ConfigPojo
    protected Proguard proguard;

    /**
     * Whether ProGuard is enabled or not. Defaults to true.
     *
     * @parameter expression="${android.proguard.skip}"
     * @optional
     */
    private Boolean proguardSkip;

    @PullParameter( defaultValue = "true" )
    private Boolean parsedSkip;

    /**
     * Path to the ProGuard configuration file (relative to project root). Defaults to "proguard.cfg"
     *
     * @parameter expression="${android.proguard.config}"
     * @optional
     */
    private File proguardConfig;

    @PullParameter( defaultValue = "${project.basedir}/proguard.cfg" )
    private File parsedConfig;

    /**
     * Additional ProGuard configuration files (relative to project root).
     *
     * @parameter expression="${android.proguard.configs}"
     * @optional
     */
    private String[] proguardConfigs;

    @PullParameter( defaultValueGetterMethod = "getDefaultProguardConfigs" )
    private String[] parsedConfigs;

    /**
     * Additional ProGuard options
     *
     * @parameter expression="${android.proguard.options}"
     * @optional
     */
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
     *
     * @parameter expression="${android.proguard.proguardJarPath}
     * @optional
     */
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
     *
     * @parameter expression="${android.proguard.outputDirectory}"
     * @optional
     */
    private File outputDirectory;

    @PullParameter( defaultValue = "${project.build.directory}/proguard" )
    private File parsedOutputDirectory;

    /**
     * @parameter expression="${android.proguard.obfuscatedJar}" 
     *            default-value="${project.build.directory}/${project.build.finalName}_obfuscated.jar"
     */
    private String obfuscatedJar;

    /**
     * Extra JVM Arguments. Using these you can e.g. increase memory for the jvm running the build.
     * Defaults to "-Xmx512M".
     *
     * @parameter expression="${android.proguard.jvmArguments}"
     * @optional
     */
    private String[] proguardJvmArguments;

    @PullParameter( defaultValueGetterMethod = "getDefaultJvmArguments" )
    private String[] parsedJvmArguments;

    /**
     * If set to true will add a filter to remove META-INF/maven/* files. Defaults to false.
     *
     * @parameter expression="${android.proguard.filterMavenDescriptor}"
     * @optional
     */
    private Boolean proguardFilterMavenDescriptor;

    @PullParameter( defaultValue = "true" )
    private Boolean parsedFilterMavenDescriptor;

    /**
     * If set to true will add a filter to remove META-INF/MANIFEST.MF files.  Defaults to false.
     *
     * @parameter expression="${android.proguard.filterManifest}"
     * @optional
     */
    private Boolean proguardFilterManifest;

    @PullParameter( defaultValue = "true" )
    private Boolean parsedFilterManifest;

    /**
     * If set to true JDK jars will be included as library jars and corresponding filters
     * will be applied to android.jar.  Defaults to true.
     * @parameter expression="${android.proguard.includeJdkLibs}"
     */
    private Boolean includeJdkLibs;

    @PullParameter( defaultValue = "true" )
    private Boolean parsedIncludeJdkLibs;

    /**
     * If set to true the mapping.txt file will be attached as artifact of type <code>map</code>
     * @parameter expression="${android.proguard.attachMap}"
     */
    private Boolean attachMap;

    @PullParameter( defaultValue = "false" )
    private Boolean parsedAttachMap;

    /**
     * The plugin dependencies.
     *
     * @parameter expression="${plugin.artifacts}"
     * @required
     * @readonly
     */
    protected List< Artifact > pluginDependencies;

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

        public ProGuardInput( String path, Collection< String > excludedFilter )
        {
            this.path = path;
            this.excludedFilter = excludedFilter;
        }

        public String toCommandLine()
        {
            if ( excludedFilter != null && !excludedFilter.isEmpty() )
            {
                String startQuotes, middleQuote, endQuote;

                if ( !Os.isFamily( Os.FAMILY_WINDOWS ) )
                {
                    startQuotes = "'\"";
                    middleQuote = "\"(";
                    endQuote = ")'";
                }
                else
                {
                    startQuotes = "\"'";
                    middleQuote = "'(";
                    endQuote = ")\"";
                }

                StringBuilder sb = new StringBuilder( startQuotes );
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
                String startQuotes, endQuote;

                if ( !Os.isFamily( Os.FAMILY_WINDOWS ) )
                {
                    startQuotes = "'\"";
                    endQuote = "\"'";
                }
                else
                {
                    startQuotes = "\"'";
                    endQuote = "'\"";
                }

                return startQuotes + path + endQuote;
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

        commands.add( "@\"" + parsedConfig + "\"" );

        for ( String config : parsedConfigs )
        {
            commands.add( "@\"" + config + "\"" );
        }

        if ( proguardFile != null )
        {
            commands.add( "@\"" + proguardFile.getAbsolutePath() + "\"" );
        }

        collectInputFiles( commands );

        commands.add( "-outjars" );
        commands.add( "'\"" + obfuscatedJar + "\"'" );

        commands.add( "-dump" );
        commands.add( "'\"" + proguardDir + File.separator + "dump.txt\"'" );
        commands.add( "-printseeds" );
        commands.add( "'\"" + proguardDir + File.separator + "seeds.txt\"'" );
        commands.add( "-printusage" );
        commands.add( "'\"" + proguardDir + File.separator + "usage.txt\"'" );

        File mapFile = new File( proguardDir, "mapping.txt" );

        commands.add( "-printmapping" );
        commands.add( "'\"" + mapFile + "\"'" );

        commands.addAll( Arrays.asList( parsedOptions ) );

        final String javaExecutable = getJavaExecutable().getAbsolutePath();

        getLog().debug( javaExecutable + " " + commands.toString() );

        try
        {
            executor.executeCommand( javaExecutable, commands, project.getBasedir(), false );
        }
        catch ( ExecutionException e )
        {
            throw new MojoExecutionException( "", e );
        }

        if ( parsedAttachMap )
        {
            projectHelper.attachArtifact( project, "map", mapFile );
        }
    }

    /**
     * Convert the jvm arguments in parsedJvmArguments as populated by the config in format as needed by the java
     * command. Also preserve backwards compatibility in terms of dashes required or not..
     *
     * @param commands
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
        skipArtifact( "commons-logging", "commons-logging", "[1.0,)", true );

        final List< ProGuardInput > inJars = getProgramInputFiles();
        for ( final ProGuardInput injar : inJars )
        {
            getLog().debug( "Added injar : " + injar );
            commands.add( "-injars" );
            commands.add( injar.toCommandLine() );
        }

        final List< ProGuardInput > libraryJars = getLibraryInputFiles();
        for ( final ProGuardInput libraryjar : libraryJars )
        {
            getLog().debug( "Added libraryJar : " + libraryjar );
            commands.add( "-libraryjars" );
            commands.add( libraryjar.toCommandLine() );
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

    private void skipArtifact( String groupId, String artifactId, String versionRangeSpec, boolean shiftToLibraries )
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

        // we first add the application's own class files
        inJars.add( createProguardInput( project.getBuild().getOutputDirectory() ) );

        // we then add all its dependencies (incl. transitive ones), unless they're blacklisted
        for ( Artifact artifact : getTransitiveDependencyArtifacts() )
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
                final File aarClassesJar = getUnpackedAarClassesJar( artifact );
                getLog().debug( "Including aar dependency as input jar : " + artifact );
                inJars.add( createProguardInput( aarClassesJar.getAbsolutePath(), globalInJarExcludes ) );
            }
            else
            {
                getLog().debug( "Excluding dependency as input jar : " + artifact );
                continue;
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
     *
     * @return
     * @throws MojoExecutionException
     */
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
     * @return
     * @see #parsedJvmArguments
     */
    private String[] getDefaultJvmArguments()
    {
        return new String[] { "-Xmx512M" };
    }

    /**
     * Get the default ProGuard config files.
     *
     * @return
     * @see #parsedConfigs
     */
    private String[] getDefaultProguardConfigs()
    {
        return new String[0];
    }

    /**
     * Get the default ProGuard options.
     *
     * @return
     * @see #parsedOptions
     */
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
