/*
 * Copyright (C) 2009 Jayway AB
 * Copyright (C) 2007-2008 JVending Masa
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.simpligility.maven.plugins.android.phase08preparepackage;

import com.simpligility.maven.plugins.android.AbstractAndroidMojo;
import com.simpligility.maven.plugins.android.CommandExecutor;
import com.simpligility.maven.plugins.android.ExecutionException;
import com.simpligility.maven.plugins.android.IncludeExcludeSet;
import com.simpligility.maven.plugins.android.configuration.D8;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.util.DefaultFileSet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.simpligility.maven.plugins.android.InclusionExclusionResolver.filterArtifacts;

/**
 * Converts compiled Java classes (including those containing Java 8 syntax) to the Android dex format.
 * It is a replacement for the {@link DexMojo}.
 *
 * You should only run one or the other.
 * By default D8 will run and Dex will not. But this is determined by the
 *
 * @author william.ferguson@xandar.com.au
 */
@Mojo(
    name = "d8",
    defaultPhase = LifecyclePhase.PREPARE_PACKAGE,
    requiresDependencyResolution = ResolutionScope.COMPILE
)
public class D8Mojo extends AbstractAndroidMojo
{
    private static final String JAR = "jar";

    /**
     * Configuration for the D8 command execution. It can be configured in the plugin configuration like so
     *
     * <pre>
     * &lt;dex&gt;
     *   &lt;dexMechanism&gt;d8|dex&lt;/dexMechanism&gt;
     *   &lt;jvmArguments&gt;
     *     &lt;jvmArgument&gt;-Xms256m&lt;/jvmArgument&gt;
     *     &lt;jvmArgument&gt;-Xmx512m&lt;/jvmArgument&gt;
     *   &lt;/jvmArguments&gt;
     *   &lt;intermediate&gt;true|false&lt;/intermediate&gt;
     *   &lt;mainDexList&gt;path to class list file&lt;/mainDexList&gt;
     *   &lt;release&gt;path to class list file&lt;/release&gt;
     *   &lt;minApi&gt;path to class list file&lt;/minApi&gt;
     *   &lt;arguments&gt;
     *     &lt;argument&gt;--someOtherArgA&lt;/argument&gt;
     *     &lt;argument&gt;--someOtherArgB&lt;/argument&gt;
     *   &lt;/arguments&gt;
     * &lt;/dex&gt;
     * </pre>
     * 
     * or via properties dex* or command line parameters android.dex.*
     */
    @Parameter
    private D8 dex;

    /**
     * Extra JVM Arguments. Using these you can e.g. increase memory for the jvm running the build.
     */
    @Parameter( property = "android.dex.jvmArguments", defaultValue = "-Xmx1024M" )
    private String[] dexJvmArguments;

    /**
     * Decides whether to pass the --intermediate flag to d8.
     */
    @Parameter( property = "android.dex.intermediate", defaultValue = "false" )
    private boolean dexIntermediate;

    /**
     * Full path to class list to multi dex
     */
    @Parameter( property = "android.dex.maindexlist" )
    private String dexMainDexList;

    /**
     * Whether to pass the --release flag to d8.
     */
    @Parameter( property = "android.dex.release", defaultValue = "false" )
    private boolean dexRelease;

    /**
     * The minApi (if any) to pass to d8.
     */
    @Parameter( property = "android.dex.release" )
    private Integer dexMinApi;

    /**
     * Additional command line parameters passed to d8.
     */
    @Parameter( property = "android.dex.dexarguments" )
    private String dexArguments;

    /**
     * The name of the obfuscated JAR.
     */
    @Parameter( property = "android.proguard.obfuscatedJar" )
    private File obfuscatedJar;

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

    private String[] parsedJvmArguments;
    private boolean parsedIntermediate;
    private String parsedMainDexList;
    private String parsedDexArguments;
    private DexMechanism parsedDexMechanism;
    private boolean parsedRelease;
    private Integer parsedMinApi;

    /**
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        parseConfiguration();

        getLog().debug( "DexMechanism set to " + parsedDexMechanism );
        if ( parsedDexMechanism != DexMechanism.D8 )
        {
            getLog().info( "Not executing D8Mojo because DexMechanism set to " + parsedDexMechanism );
            return;
        }

        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger( getLog() );

        if ( generateApk )
        {
            runD8( executor );
        }

        if ( attachJar )
        {
            File jarFile = new File( targetDirectory + File.separator
                + finalName + ".jar" );
            projectHelper.attachArtifact( project, "jar", project.getArtifact().getClassifier(), jarFile );
        }

        if ( attachSources )
        {
            // Also attach an .apksources, containing sources from this project.
            final File apksources = createApkSourcesFile();
            projectHelper.attachArtifact( project, "apksources", apksources );
        }
    }

    private List<File> getDependencies()
    {
        final List<File> libraries = new ArrayList<>();
        for ( Artifact artifact : filterArtifacts( getTransitiveDependencyArtifacts(), skipDependencies,
                artifactTypeSet.getIncludes(), artifactTypeSet.getExcludes(), artifactSet.getIncludes(),
                artifactSet.getExcludes() ) )
        {
            if ( "jar".equals( artifact.getType() ) )
            {
                libraries.add( artifact.getFile() );
            }
        }

        return libraries;
    }

    /**
     * @return Set of input files for dex. This is a combination of directories and jar files.
     */
    private Set< File > getDexInputFiles()
    {
        final Set< File > inputs = new HashSet< File >();

        if ( obfuscatedJar != null && obfuscatedJar.exists() )
        {
            // proguard has been run, use this jar
            getLog().debug( "Adding dex input (obfuscatedJar) : " + obfuscatedJar );
            inputs.add( obfuscatedJar );
        }
        else
        {
            getLog().debug( "Using non-obfuscated input" );
            final File classesJar = new File( targetDirectory, finalName + ".jar" );
            inputs.add( classesJar );
            getLog().debug( "Adding dex input from : " + classesJar );

            for ( Artifact artifact : filterArtifacts( getTransitiveDependencyArtifacts(), skipDependencies,
                    artifactTypeSet.getIncludes(), artifactTypeSet.getExcludes(), artifactSet.getIncludes(),
                    artifactSet.getExcludes() ) )
            {
                if ( artifact.getType().equals( JAR ) )
                {
                    getLog().debug( "Adding dex input : " + artifact.getFile() );
                    inputs.add( artifact.getFile().getAbsoluteFile() );
                }
            }
        }

        return inputs;
    }

    private void parseConfiguration()
    {
        // config in pom found
        if ( dex != null )
        {
            // the if statements make sure that properties/command line
            // parameter overrides configuration
            // and that the dafaults apply in all cases;
            if ( dex.getJvmArguments() == null )
            {
                parsedJvmArguments = dexJvmArguments;
            }
            else
            {
                parsedJvmArguments = dex.getJvmArguments();
            }
            if ( dex.isIntermediate() == null )
            {
                parsedIntermediate = dexIntermediate;
            }
            else
            {
                parsedIntermediate = dex.isIntermediate();
            }
            if ( dex.getMainDexList() == null )
            {
                parsedMainDexList = dexMainDexList;
            }
            else
            {
                parsedMainDexList = dex.getMainDexList();
            }
            if ( dex.getDexArguments() == null )
            {
                parsedDexArguments = dexArguments;
            }
            else
            {
                parsedDexArguments = dex.getDexArguments();
            }
            parsedDexMechanism = dex.getDexMechanism();
            if ( dex.isRelease() == null )
            {
                parsedRelease = dexRelease;
            }
            else
            {
                parsedRelease = dex.isRelease();
            }
            if ( dex.getMinApi() == null )
            {
                parsedMinApi = dexMinApi;
            }
            else
            {
                parsedMinApi = dex.getMinApi();
            }
        }
        else
        {
            parsedJvmArguments = dexJvmArguments;
            parsedIntermediate = dexIntermediate;
            parsedMainDexList = dexMainDexList;
            parsedDexArguments = dexArguments;
            parsedDexMechanism = DexMechanism.Dex;
            parsedRelease = dexRelease;
            parsedMinApi = dexMinApi;
        }
    }

    private List< String > dexDefaultCommands() throws MojoExecutionException
    {
        List< String > commands = jarDefaultCommands();
        commands.add( getAndroidSdk().getD8JarPath() );
        return commands;
    }

    private List<String> jarDefaultCommands()
    {
        List< String > commands = javaDefaultCommands();
        commands.add( "-jar" );
        return commands;
    }

    private List<String> javaDefaultCommands()
    {
        List< String > commands = new ArrayList< String > ();
        if ( parsedJvmArguments != null )
        {
            for ( String jvmArgument : parsedJvmArguments )
            {
                // preserve backward compatibility allowing argument with or
                // without dash (e.g. Xmx512m as well as
                // -Xmx512m should work) (see
                // http://code.google.com/p/maven-android-plugin/issues/detail?id=153)
                if ( !jvmArgument.startsWith( "-" ) )
                {
                    jvmArgument = "-" + jvmArgument;
                }
                getLog().debug( "Adding jvm argument " + jvmArgument );
                commands.add( jvmArgument );
            }
        }
        return commands;
    }

    private void runD8( CommandExecutor executor )
        throws MojoExecutionException
    {
        final List< String > commands = dexDefaultCommands();
        final Set< File > inputFiles = getDexInputFiles();
        if ( parsedIntermediate )
        {
            commands.add( "--intermediate" );
        }
        if ( parsedMainDexList != null )
        {
            commands.add( "--main-dex-list " + parsedMainDexList );
        }
        if ( parsedDexArguments != null )
        {
            commands.add( parsedDexArguments );
        }

        if ( parsedRelease )
        {
            commands.add( "--release" );
        }

        if ( parsedMinApi != null )
        {
            commands.add( "--min api" );
            commands.add( parsedMinApi.toString() );
        }

        commands.add( "--output" );
        commands.add( targetDirectory.getAbsolutePath() );

        final File androidJar = getAndroidSdk().getAndroidJar();
        commands.add( "--lib "  );
        commands.add( androidJar.getAbsolutePath() );

        // Add project classpath
        final List<File> dependencies = getDependencies();
        for ( final File file : dependencies )
        {
            commands.add( "--classpath" );
            commands.add( file.getAbsolutePath() );
        }

        for ( File inputFile : inputFiles )
        {
            commands.add( inputFile.getAbsolutePath() );
        }

        getLog().info( "Convert classes to Dex : " + targetDirectory );
        executeJava( commands, executor );
    }

    private String executeJava( final List<String> commands, CommandExecutor executor ) throws MojoExecutionException
    {
        final String javaExecutable = getJavaExecutable().getAbsolutePath();
        getLog().debug( javaExecutable + " " + commands.toString() );
        try
        {
            executor.setCaptureStdOut( true );
            executor.executeCommand( javaExecutable, commands, project.getBasedir(), false );
            return executor.getStandardOut();
        }
        catch ( ExecutionException e )
        {
            throw new MojoExecutionException( "", e );
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

    /**
     * @return
     * @throws MojoExecutionException
     */
    protected File createApkSourcesFile() throws MojoExecutionException
    {
        final File apksources = new File( targetDirectory, finalName
            + ".apksources" );
        FileUtils.deleteQuietly( apksources );

        try
        {
            JarArchiver jarArchiver = new JarArchiver();
            jarArchiver.setDestFile( apksources );

            addDirectory( jarArchiver, assetsDirectory, "assets" );
            addDirectory( jarArchiver, resourceDirectory, "res" );
            addDirectory( jarArchiver, sourceDirectory, "src/main/java" );
            addJavaResources( jarArchiver, resources );

            jarArchiver.createArchive();
        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException( "ArchiverException while creating .apksource file.", e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "IOException while creating .apksource file.", e );
        }

        return apksources;
    }

    /**
     * Makes sure the string ends with "/"
     *
     * @param prefix
     *            any string, or null.
     * @return the prefix with a "/" at the end, never null.
     */
    protected String endWithSlash( String prefix )
    {
        prefix = StringUtils.defaultIfEmpty( prefix, "/" );
        if ( !prefix.endsWith( "/" ) )
        {
            prefix = prefix + "/";
        }
        return prefix;
    }

    /**
     * Adds a directory to a {@link JarArchiver} with a directory prefix.
     *
     * @param jarArchiver
     * @param directory
     *            The directory to add.
     * @param prefix
     *            An optional prefix for where in the Jar file the directory's contents should go.
     */
    protected void addDirectory( JarArchiver jarArchiver, File directory, String prefix )
    {
        if ( directory != null && directory.exists() )
        {
            final DefaultFileSet fileSet = new DefaultFileSet();
            fileSet.setPrefix( endWithSlash( prefix ) );
            fileSet.setDirectory( directory );
            jarArchiver.addFileSet( fileSet );
        }
    }

    /**
     * @param jarArchiver
     * @param javaResources
     */
    protected void addJavaResources( JarArchiver jarArchiver, List< Resource > javaResources )
    {
        for ( Resource javaResource : javaResources )
        {
            addJavaResource( jarArchiver, javaResource );
        }
    }

    /**
     * Adds a Java Resources directory (typically "src/main/resources") to a {@link JarArchiver}.
     *
     * @param jarArchiver
     * @param javaResource
     *            The Java resource to add.
     */
    protected void addJavaResource( JarArchiver jarArchiver, Resource javaResource )
    {
        if ( javaResource != null )
        {
            final File javaResourceDirectory = new File( javaResource.getDirectory() );
            if ( javaResourceDirectory.exists() )
            {
                final DefaultFileSet javaResourceFileSet = new DefaultFileSet();
                javaResourceFileSet.setDirectory( javaResourceDirectory );
                javaResourceFileSet.setPrefix( endWithSlash( "src/main/resources" ) );
                jarArchiver.addFileSet( javaResourceFileSet );
            }
        }
    }
}
