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
import com.simpligility.maven.plugins.android.IncludeExcludeSet;
import com.simpligility.maven.plugins.android.CommandExecutor;
import com.simpligility.maven.plugins.android.ExecutionException;
import com.simpligility.maven.plugins.android.common.Const;
import com.simpligility.maven.plugins.android.common.ZipExtractor;
import com.simpligility.maven.plugins.android.configuration.Dex;

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
import static com.simpligility.maven.plugins.android.common.AndroidExtension.AAR;
import static com.simpligility.maven.plugins.android.common.AndroidExtension.APK;
import static com.simpligility.maven.plugins.android.common.AndroidExtension.APKLIB;

/**
 * Converts compiled Java classes to the Android dex format.
 *
 * @author hugo.josefson@jayway.com
 */
@Mojo(
        name = "dex",
        defaultPhase = LifecyclePhase.PREPARE_PACKAGE,
        requiresDependencyResolution = ResolutionScope.COMPILE
)
public class DexMojo extends AbstractAndroidMojo
{

    /**
     * Configuration for the dex command execution. It can be configured in the plugin configuration like so
     *
     * <pre>
     * &lt;dex&gt;
     *   &lt;jvmArguments&gt;
     *     &lt;jvmArgument&gt;-Xms256m&lt;/jvmArgument&gt;
     *     &lt;jvmArgument&gt;-Xmx512m&lt;/jvmArgument&gt;
     *   &lt;/jvmArguments&gt;
     *   &lt;coreLibrary&gt;true|false&lt;/coreLibrary&gt;
     *   &lt;noLocals&gt;true|false&lt;/noLocals&gt;
     *   &lt;forceJumbo&gt;true|false&lt;/forceJumbo&gt;
     *   &lt;optimize&gt;true|false&lt;/optimize&gt;
     *   &lt;preDex&gt;true|false&lt;/preDex&gt;
     *   &lt;preDexLibLocation&gt;path to predexed libraries, defaults to target/dexedLibs&lt;/preDexLibLocation&gt;
     *   &lt;incremental&gt;true|false&lt;/incremental&gt;
     *   &lt;multiDex&gt;true|false&lt;/multiDex&gt;
     *   &lt;mainDexList&gt;path to class list file&lt;/mainDexList&gt;
     *   &lt;minimalMainDex&gt;true|false&lt;/minimalMainDex&gt;
     * &lt;/dex&gt;
     * </pre>
     * <p/>
     * or via properties dex.* or command line parameters android.dex.*
     */
    @Parameter
    private Dex dex;

    /**
     * Extra JVM Arguments. Using these you can e.g. increase memory for the jvm running the build.
     */
    @Parameter( property = "android.dex.jvmArguments", defaultValue = "-Xmx1024M" )
    private String[] dexJvmArguments;

    /**
     * Decides whether to pass the --core-library flag to dx.
     */
    @Parameter( property = "android.dex.coreLibrary", defaultValue = "false" )
    private boolean dexCoreLibrary;

    /**
     * Decides whether to pass the --no-locals flag to dx.
     */
    @Parameter( property = "android.dex.noLocals", defaultValue = "false" )
    private boolean dexNoLocals;

    /**
     * Decides whether to pass the --no-optimize flag to dx.
     */
    @Parameter( property = "android.dex.optimize", defaultValue = "true" )
    private boolean dexOptimize;

    /**
     * Decides whether to predex the jars.
     */
    @Parameter( property = "android.dex.predex", defaultValue = "false" )
    private boolean dexPreDex;

    /**
     * Decides whether to use force jumbo mode.
     */
    @Parameter( property = "android.dex.forcejumbo", defaultValue = "false" )
    private boolean dexForceJumbo;

    /**
     * Path to predexed libraries.
     */
    @Parameter(
            property = "android.dex.dexPreDexLibLocation",
            defaultValue = "${project.build.directory}${file.separator}dexedLibs"
    )
    private String dexPreDexLibLocation;

    /**
     * Decides whether to pass the --incremental flag to dx.
     */
    @Parameter( property = "android.dex.incremental", defaultValue = "false" )
    private boolean dexIncremental;

    /**
     * The name of the obfuscated JAR.
     */
    @Parameter( property = "android.proguard.obfuscatedJar" )
    private File obfuscatedJar;

    /**
     * Decides whether to pass the --multi-dex flag to dx.
     */
    @Parameter( property = "android.dex.multidex", defaultValue = "false" )
    private boolean dexMultiDex;

    /**
     * Full path to class list to multi dex
     */
    @Parameter( property = "android.dex.maindexlist" )
    private String dexMainDexList;

    /**
     * Decides whether to pass the --minimal-main-dex flag to dx.
     */
    @Parameter( property = "android.dex.minimalmaindex", defaultValue = "false" )
    private boolean dexMinimalMainDex;

    /**
     * Additional command line parameters passed to dx.
     */
    @Parameter( property = "android.dex.dexarguments" )
    private String dexArguments;

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
    private boolean parsedCoreLibrary;
    private boolean parsedNoLocals;
    private boolean parsedOptimize;
    private boolean parsedPreDex;
    private boolean parsedForceJumbo;
    private String parsedPreDexLibLocation;
    private boolean parsedIncremental;
    private boolean parsedMultiDex;
    private String parsedMainDexList;
    private boolean parsedMinimalMainDex;
    private String parsedDexArguments;

    /**
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {

        if ( getJack().isEnabled() ) 
        {
            //Dexxing is handled by Jack
            return;
        }
        
        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger( this.getLog() );

        parseConfiguration();
        File outputFile;
        if ( parsedMultiDex )
        {
            outputFile = targetDirectory;
        }
        else
        {
            outputFile = new File( targetDirectory, "classes.dex" );
        }
        if ( generateApk )
        {
            runDex( executor, outputFile );
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

    /**
     * Gets the input files for dex. This is a combination of directories and jar files.
     *
     * @return
     */
    private Set< File > getDexInputFiles() throws MojoExecutionException
    {
        Set< File > inputs = new HashSet< File >();

        if ( obfuscatedJar != null && obfuscatedJar.exists() )
        {
            // proguard has been run, use this jar
            getLog().debug( "Adding dex input (obfuscatedJar) : " + obfuscatedJar );
            inputs.add( obfuscatedJar );
        }
        else
        {
            getLog().debug( "Using non-obfuscated input" );
            // no proguard, use original config
            inputs.add( projectOutputDirectory );
            getLog().debug( "Adding dex input : " + project.getBuild().getOutputDirectory() );
            for ( Artifact artifact : filterArtifacts( getTransitiveDependencyArtifacts(), skipDependencies,
                    artifactTypeSet.getIncludes(), artifactTypeSet.getExcludes(), artifactSet.getIncludes(),
                    artifactSet.getExcludes() ) )
            {
                if ( artifact.getType().equals( Const.ArtifactType.NATIVE_SYMBOL_OBJECT )
                        || artifact.getType().equals( Const.ArtifactType.NATIVE_IMPLEMENTATION_ARCHIVE ) )
                {
                    // Ignore native dependencies - no need for dexer to see those
                }
                else if ( artifact.getType().equals( APKLIB ) )
                {
                    // Any jars in the libs folder should now be
                    // automatically included because they will be a transitive dependency.
                }
                else if ( artifact.getType().equals( AAR ) )
                {
                    // The Aar classes.jar should now be automatically included
                    // because it will be a transitive dependency. As should any jars in the libs folder.
                }
                else if ( artifact.getType().equals( APK ) )
                {
                    // We need to dex the APK classes including the APK R.
                    // But we don't want to add a second instance of the embedded Rs for any of the APK's dependencies
                    // as they will already have been generated to target/classes. The R values from the APK will be
                    // the correct ones, so best solution is to extract the APK classes (including all Rs) to
                    // target/classes overwriting any generated Rs and let dex pick up the values from there.
                    getLog().debug( "Extracting APK classes to target/classes : " + artifact.getArtifactId() );
                    final File apkClassesJar = getUnpackedLibHelper().getJarFileForApk( artifact );
                    getLog().debug( "Extracting APK : " + apkClassesJar + " to " + targetDirectory );
                    final ZipExtractor extractor = new ZipExtractor( getLog() );
                    extractor.extract( apkClassesJar, targetDirectory, ".class" );
                }
                else
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
            if ( dex.isCoreLibrary() == null )
            {
                parsedCoreLibrary = dexCoreLibrary;
            }
            else
            {
                parsedCoreLibrary = dex.isCoreLibrary();
            }
            if ( dex.isNoLocals() == null )
            {
                parsedNoLocals = dexNoLocals;
            }
            else
            {
                parsedNoLocals = dex.isNoLocals();
            }
            if ( dex.isOptimize() == null )
            {
                parsedOptimize = dexOptimize;
            }
            else
            {
                parsedOptimize = dex.isOptimize();
            }
            if ( dex.isPreDex() == null )
            {
                parsedPreDex = dexPreDex;
            }
            else
            {
                parsedPreDex = dex.isPreDex();
            }
            if ( dex.getPreDexLibLocation() == null )
            {
                parsedPreDexLibLocation = dexPreDexLibLocation;
            }
            else
            {
                parsedPreDexLibLocation = dex.getPreDexLibLocation();
            }
            if ( dex.isIncremental() == null )
            {
                parsedIncremental = dexIncremental;
            }
            else
            {
                parsedIncremental = dex.isIncremental();
            }
            if ( dex.isForceJumbo() == null )
            {
                parsedForceJumbo = dexForceJumbo;
            }
            else
            {
                parsedForceJumbo = dex.isForceJumbo();
            }
            if ( dex.isMultiDex() == null )
            {
                parsedMultiDex = dexMultiDex;
            }
            else
            {
                parsedMultiDex = dex.isMultiDex();
            }
            if ( dex.getMainDexList() == null )
            {
                parsedMainDexList = dexMainDexList;
            }
            else
            {
                parsedMainDexList = dex.getMainDexList();
            }
            if ( dex.isMinimalMainDex() == null )
            {
                parsedMinimalMainDex = dexMinimalMainDex;
            }
            else
            {
                parsedMinimalMainDex = dex.isMinimalMainDex();
            }
            if ( dex.getDexArguments() == null )
            {
                parsedDexArguments = dexArguments;
            }
            else
            {
                parsedDexArguments = dex.getDexArguments();
            }

        }
        else
        {
            parsedJvmArguments = dexJvmArguments;
            parsedCoreLibrary = dexCoreLibrary;
            parsedNoLocals = dexNoLocals;
            parsedOptimize = dexOptimize;
            parsedPreDex = dexPreDex;
            parsedPreDexLibLocation = dexPreDexLibLocation;
            parsedIncremental = dexIncremental;
            parsedForceJumbo = dexForceJumbo;
            parsedMultiDex = dexMultiDex;
            parsedMainDexList = dexMainDexList;
            parsedMinimalMainDex = dexMinimalMainDex;
            parsedDexArguments = dexArguments;
        }
    }

    private Set< File > preDex( CommandExecutor executor, Set< File > inputFiles ) throws MojoExecutionException
    {
        Set< File > filtered = new HashSet< File >();
        getLog().info( "Pre dex-ing libraries for faster dex-ing of the final application." );

        for ( File inputFile : inputFiles )
        {
            if ( inputFile.getName().matches( ".*\\.jar$" ) )
            {
                List< String > commands = dexDefaultCommands();

                File predexJar = predexJarPath( inputFile );
                commands.add( "--output=" + predexJar.getAbsolutePath() );
                commands.add( inputFile.getAbsolutePath() );
                filtered.add( predexJar );

                if ( !predexJar.isFile() || predexJar.lastModified() < inputFile.lastModified() )
                {
                    getLog().info( "Pre-dex ing jar: " + inputFile.getAbsolutePath() );

                    final String javaExecutable = getJavaExecutable().getAbsolutePath();
                    getLog().debug( javaExecutable + " " + commands.toString() );
                    try
                    {
                        executor.setCaptureStdOut( true );
                        executor.executeCommand( javaExecutable, commands, project.getBasedir(), false );
                    }
                    catch ( ExecutionException e )
                    {
                        throw new MojoExecutionException( "", e );
                    }
                }

            }
            else
            {
                filtered.add( inputFile );
            }
        }

        return filtered;
    }

    private File predexJarPath( File inputFile )
    {
        final File predexLibsDirectory = new File( parsedPreDexLibLocation.trim() );
        predexLibsDirectory.mkdirs();
        return new File( predexLibsDirectory, inputFile.getName() );
    }

    private List< String > dexDefaultCommands() throws MojoExecutionException
    {

        List< String > commands = new ArrayList< String >();
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
        commands.add( "-jar" );
        commands.add( getAndroidSdk().getDxJarPath() );
        commands.add( "--dex" );

        return commands;

    }

    private void runDex( CommandExecutor executor, File outputFile )
            throws MojoExecutionException
    {
        final List< String > commands = dexDefaultCommands();
        final Set< File > inputFiles = getDexInputFiles();
        Set< File > filteredFiles = inputFiles;
        if ( parsedPreDex )
        {
            filteredFiles = preDex( executor, inputFiles );
        }
        if ( !parsedOptimize )
        {
            commands.add( "--no-optimize" );
        }
        if ( parsedCoreLibrary )
        {
            commands.add( "--core-library" );
        }
        if ( parsedIncremental )
        {
            commands.add( "--incremental" );
        }
        if ( parsedNoLocals )
        {
            commands.add( "--no-locals" );
        }
        if ( parsedForceJumbo )
        {
            commands.add( "--force-jumbo" );
        }
        if ( parsedMultiDex )
        {
            commands.add( "--multi-dex" );
            if ( parsedMainDexList == null )
            {
                File generatedMainDexClasses = generateMainDexClassesFile();
                commands.add( "--main-dex-list=" + generatedMainDexClasses );
                parsedMinimalMainDex = true;
            }
            else
            {
                commands.add( "--main-dex-list=" + parsedMainDexList );
            }
            if ( parsedMinimalMainDex )
            {
                commands.add( "--minimal-main-dex" );
            }
        }
        if ( parsedDexArguments != null )
        {
           commands.add( parsedDexArguments );
        }
        commands.add( "--output=" + outputFile.getAbsolutePath() );
        for ( File inputFile : filteredFiles )
        {
            commands.add( inputFile.getAbsolutePath() );
        }

        final String javaExecutable = getJavaExecutable().getAbsolutePath();
        getLog().debug( javaExecutable + " " + commands.toString() );
        getLog().info( "Convert classes to Dex : " + outputFile );
        try
        {
            executor.setCaptureStdOut( true );
            executor.executeCommand( javaExecutable, commands, project.getBasedir(), false );
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

    private File generateMainDexClassesFile() throws MojoExecutionException 
    {
        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger( getLog() );
        List< String> commands = new ArrayList< String>();
        commands.add( "--output" );
        
        File mainDexClasses = new File( targetDirectory, "mainDexClasses.txt" );
        commands.add( mainDexClasses.getAbsolutePath() );
        
        Set< File> inputFiles = getDexInputFiles();  
        StringBuilder sb = new StringBuilder();
        sb.append( StringUtils.join( inputFiles, File.pathSeparatorChar ) );
        commands.add( sb.toString() );
        
        String executable = getAndroidSdk().getMainDexClasses().getAbsolutePath();
        try
        {
            executor.executeCommand( executable, commands, project.getBasedir(), false );
        } 
        catch ( ExecutionException ex ) 
        {
            throw new MojoExecutionException( "Failed to execute mainDexClasses", ex );
        }
        return mainDexClasses;
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
