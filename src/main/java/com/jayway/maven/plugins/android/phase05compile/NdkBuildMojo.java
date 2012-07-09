/*
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
package com.jayway.maven.plugins.android.phase05compile;

import com.jayway.maven.plugins.android.AbstractAndroidMojo;
import com.jayway.maven.plugins.android.CommandExecutor;
import com.jayway.maven.plugins.android.ExecutionException;
import com.jayway.maven.plugins.android.common.AetherHelper;
import com.jayway.maven.plugins.android.common.NativeHelper;
import com.jayway.maven.plugins.android.config.ConfigPojo;
import com.jayway.maven.plugins.android.config.PullParameter;
import com.jayway.maven.plugins.android.configuration.HeaderFilesDirective;
import com.jayway.maven.plugins.android.configuration.Ndk;
import org.apache.commons.io.FileUtils;
import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Johan Lindquist <johanlindquist@gmail.com>
 * @goal ndk-build
 * @phase compile
 * @requiresProject true
 */
public class NdkBuildMojo extends AbstractAndroidMojo
{
    /**
     * <p>The Android NDK to use.</p>
     * <p>Looks like this:</p>
     * <pre>
     * &lt;ndk&gt;
     *     &lt;path&gt;/opt/android-ndk-r4&lt;/path&gt;
     * &lt;/ndk&gt;
     * </pre>
     * <p>The <code>&lt;path&gt;</code> parameter is optional. The default is the setting of the ANDROID_NDK_HOME environment
     * variable. The parameter can be used to override this setting with a different environment variable like this:</p>
     * <pre>
     * &lt;ndk&gt;
     *     &lt;path&gt;${env.ANDROID_NDK_HOME}&lt;/path&gt;
     * &lt;/ndk&gt;
     * </pre>
     * <p>or just with a hardcoded absolute path. The parameters can also be configured from command-line with parameter
     * <code>-Dandroid.ndk.path</code>.</p>
     *
     * @parameter
     */
    @ConfigPojo(prefix = "ndk")
    private Ndk ndk;

    /**
     * Allows for overriding the default ndk-build executable.
     *
     * @parameter expression="${android.ndk.ndk-build-executable}"
     */
    @PullParameter
    private String ndkBuildExecutable;

    /**
     * @parameter expression="${android.ndk.ndk-build-directory}"
     */
    @PullParameter
    private String ndkBuildDirectory;

    /**
     * Specifies the classifier with which the artifact should be stored in the repository
     *
     * @parameter expression="${android.ndk.build.native-classifier}"
     */
    @PullParameter
    private String ndkClassifier;

    /**
     * Specifies additional command line parameters to pass to ndk-build
     *
     * @parameter expression="${android.ndk.build.command-line}"
     */
    @PullParameter
    protected String ndkBuildAdditionalCommandline;

    /**
     * Flag indicating whether the NDK output directory (libs/&lt;architecture&gt;) should be cleared after build.  This
     * will essentially 'move' all the native artifacts (.so) to the ${project.build.directory}/libs/&lt;architecture&gt;.
     * If an APK is built as part of the invocation, the libraries will be included from here.
     *
     * @parameter expression="${android.ndk.build.clear-native-artifacts}" default-value="false"
     */
    @PullParameter(defaultValue = "false")
    private Boolean clearNativeArtifacts;

    /**
     * Flag indicating whether the resulting native library should be attached as an artifact to the build.  This
     * means the resulting .so is installed into the repository as well as being included in the final APK.
     *
     * @parameter expression="${android.ndk.build.attach-native-artifact}" default-value="false"
     */
    @PullParameter(defaultValue = "false")
    private Boolean attachNativeArtifacts;

    /**
     * Build folder to place built native libraries into
     *
     * @parameter expression="${android.ndk.build.ndk-output-directory}" default-value="${project.build.directory}/ndk-libs"
     */
    private File ndkOutputDirectory;

    /**
     * <p>Folder containing native, static libraries compiled and linked by the NDK.</p>
     *
     * @parameter expression="${android.nativeLibrariesOutputDirectory}" default-value="${project.basedir}/obj/local"
     */
    private File nativeLibrariesOutputDirectory;

    /**
     * <p>Target to invoke on the native makefile.</p>
     *
     * @parameter expression="${android.nativeTarget}"
     */
    @PullParameter
    private String target;

    /**
     * Defines the architecture for the NDK build
     *
     * @parameter expression="${android.ndk.build.architecture}" default-value="armeabi"
     */
    @PullParameter(defaultValue = "armeabi")
    private String ndkArchitecture;

    /**
     * @component
     * @readonly
     * @required
     */
    protected ArtifactFactory artifactFactory;

    /**
     * Flag indicating whether the header files used in the build should be included and attached to the build as
     * an additional artifact.
     *
     * @parameter expression="${android.ndk.build.attach-header-files}" default-value="true"
     */
    @PullParameter(defaultValue = "true")
    private Boolean attachHeaderFiles;

    /**
     * Flag indicating whether the make files last LOCAL_SRC_INCLUDES should be used for determing what header
     * files to include.  Setting this flag to true, overrides any defined header files directives.
     * <strong>Note: </strong> By setting this flag to true, all header files used in the project will be
     * added to the resulting header archive.  This may be undesirable in most cases and is therefore turned off by default.
     *
     * @parameter expression="${android.ndk.build.use-local-src-include-paths}" default-value="false"
     */
    @PullParameter(defaultValue = "false")
    private Boolean useLocalSrcIncludePaths;

    /**
     * Specifies the set of header files includes/excludes which should be used for bundling the exported header
     * files.  The below shows an example of how this can be used.
     * <p/>
     * <pre>
     * &lt;headerFilesDirectives&gt;
     *   &lt;headerFilesDirective&gt;
     *     &lt;directory&gt;${basedir}/jni/include&lt;/directory&gt;
     *     &lt;includes&gt;
     *       &lt;includes&gt;**\/*.h&lt;/include&gt;
     *     &lt;/includes&gt;
     *   &lt;headerFilesDirective&gt;
     * &lt;/headerFilesDirectives&gt;
     * </pre>
     * <br/>
     * If no <code>headerFilesDirectives</code> is specified, the default includes will be defined as shown below:
     * <br/>
     * <pre>
     * &lt;headerFilesDirectives&gt;
     *   &lt;headerFilesDirective&gt;
     *     &lt;directory&gt;${basedir}/jni&lt;/directory&gt;
     *     &lt;includes&gt;
     *       &lt;includes&gt;**\/*.h&lt;/include&gt;
     *     &lt;/includes&gt;
     *     &lt;excludes&gt;
     *       &lt;exclude&gt;**\/*.c&lt;/exclude&gt;
     *     &lt;/excludes&gt;
     *   &lt;headerFilesDirective&gt;
     *   [..]
     * &lt;/headerFilesDirectives&gt;
     * </pre>
     *
     * @parameter
     */
    @PullParameter
    private List<HeaderFilesDirective> headerFilesDirectives;


    /**
     * The Jar archiver.
     *
     * @component role="org.codehaus.plexus.archiver.Archiver" roleHint="jar"
     */
    private JarArchiver jarArchiver;

    /**
     * Flag indicating whether the header files for native, static library dependencies should be used.  If true,
     * the header archive for each statically linked dependency will be resolved.
     *
     * @parameter expression="${android.ndk.build.use-header-archives}" default-value="true"
     */
    @PullParameter(defaultValue = "true")
    private Boolean useHeaderArchives;

    /**
     * Defines additional system properties which should be exported to the ndk-build script.  This
     * <br/>
     * <pre>
     * &lt;systemProperties&gt;
     *   &lt;propertyName&gt;propertyValue&lt;/propertyName&gt;
     *   &lt;build-target&gt;android&lt;/build-target&gt;
     *   [..]
     * &lt;/systemProperties&gt;
     * </pre>     *
     *
     * @parameter
     */
    @PullParameter
    private Map<String, String> systemProperties;

    /**
     * Flag indicating whether warnings should be ignored while compiling.  If true,
     * the build will not fail if warning are found during compile.
     *
     * @parameter expression="${android.ndk.build.ignore-build-warnings}" default-value="true"
     */
    @PullParameter(defaultValue = "true")
    private Boolean ignoreBuildWarnings;

    /**
     * Defines the regular expression used to detect whether error/warning output from ndk-build is a minor compile warning
     * or is actually an error which should cause the build to fail.
     * <p/>
     * If the pattern matches, the output from the compiler will <strong>not</strong> be considered an error and compile
     * will be successful.
     *
     * @parameter expression="${android.ndk.build.build-warnings-regular-expression}" default-value=".*[warning|note]: .*"
     */
    @PullParameter(defaultValue = ".*[warning|note]: .*")
    private String buildWarningsRegularExpression;

    /**
     * @parameter expression="${android.ndk.build.skip-native-library-stripping}" default-value="false"
     */
    @PullParameter(defaultValue = "false")
    private Boolean skipStripping;

    /**
     * @parameter expression="${android.ndk.build.ndk-toolchain}" default-value="arm-linux-androideabi-4.4.3"
     */
    @PullParameter(defaultValue = "arm-linux-androideabi-4.4.3")
    private String ndkToolchain;


    /**
     * Specifies the final name of the library output by the build (this allows
     *
     * @parameter expression="${android.ndk.build.build.final-library.name}"
     */
    @PullParameter
    private String ndkFinalLibraryName;

    /**
     * Specifies the makefile to use for the build (if other than the default Android.mk).
     *
     * @parameter
     */
    @PullParameter
    private String makefile;

    public void execute() throws MojoExecutionException, MojoFailureException
    {

        //ConfigHandler cfh = new ConfigHandler( this );
        //cfh.parseConfiguration();

        try
        {

            // Validate the NDK
            final File ndkBuildFile = new File( getAndroidNdk().getNdkBuildPath() );
            NativeHelper.validateNDKVersion( ndkBuildFile.getParentFile() );

            // Validate the makefile - if our packaging type is so (for example) and there are
            // dependencies on .a files (or shared files for that matter) the makefile should include
            // the include of our Android Maven plugin generated makefile.
            validateMakefile( project, makefile );

            // This usually points to ${basedir}/obj/local
            File nativeLibDirectory = new File( nativeLibrariesOutputDirectory, ndkArchitecture );
            final boolean libsDirectoryExists = nativeLibDirectory.exists();

            // Determine how much of the output directory structure (most likely obj/...) does not exist
            // and based on what we find, determine how much of it we delete after the build
            File directoryToRemove = nativeLibDirectory;
            if ( ! libsDirectoryExists )
            {

                getLog().info( "Creating native output directory " + nativeLibDirectory );

                // This simply checks how much of the structure already exists - nothing (e.g. we make all the dirs)
                // or just a partial part (the architecture part)?
                if ( ! nativeLibrariesOutputDirectory.exists() )
                {
                    if ( nativeLibrariesOutputDirectory.getParentFile().exists() )
                    {
                        nativeLibDirectory.mkdir();
                    } else
                    {
                        nativeLibDirectory.mkdirs();
                        directoryToRemove = nativeLibrariesOutputDirectory.getParentFile();
                    }
                } else
                {
                    if ( nativeLibDirectory.getParentFile().exists() )
                    {
                        nativeLibDirectory.mkdir();
                    } else
                    {
                        nativeLibDirectory.mkdirs();
                        directoryToRemove = nativeLibDirectory.getParentFile();
                    }
                }

            }

            // Start setting up the command line to be executed
            final CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();

            // Add an error listener to the build - this allows the build to conditionally fail
            // depending on a) the output of the build b) whether or not build errors (output on stderr) should be
            // ignored and c) whether the pattern matches or not
            executor.setErrorListener( new CommandExecutor.ErrorListener()
            {
                @Override
                public boolean isError(String error)
                {

                    // Unconditionally ignore *All* build warning if configured to
                    if ( ignoreBuildWarnings )
                    {
                        return false;
                    }

                    final Pattern pattern = Pattern.compile( buildWarningsRegularExpression );
                    final Matcher matcher = pattern.matcher( error );

                    // If the the reg.exp actually matches, we can safely say this is not an error
                    // since in theory the user told us so
                    if ( matcher.matches() )
                    {
                        return false;
                    }

                    // Otherwise, it is just another error
                    return true;
                }
            } );

            final Set<Artifact> nativeLibraryArtifacts = findNativeLibraryDependencies();

            // If there are any static libraries the code needs to link to, include those in the make file
            final Set<Artifact> resolveNativeLibraryArtifacts =
                    AetherHelper.resolveArtifacts( nativeLibraryArtifacts, repoSystem, repoSession, projectRepos );

            final File androidMavenMakefile = File.createTempFile( "android_maven_plugin_makefile", ".mk" );
            androidMavenMakefile.deleteOnExit();

            final MakefileHelper.MakefileHolder makefileHolder = MakefileHelper
                    .createMakefileFromArtifacts( androidMavenMakefile.getParentFile(), resolveNativeLibraryArtifacts,
                            useHeaderArchives, repoSession, projectRepos, repoSystem );
            IOUtil.copy( makefileHolder.getMakeFile(), new FileOutputStream( androidMavenMakefile ) );

            // Add the path to the generated makefile - this is picked up by the build (by an include from the user)
            executor.addEnvironment( "ANDROID_MAVEN_PLUGIN_MAKEFILE", androidMavenMakefile.getAbsolutePath() );

            // Only add the LOCAL_STATIC_LIBRARIES
            if ( NativeHelper.hasStaticNativeLibraryArtifact( resolveNativeLibraryArtifacts ) )
            {
                executor.addEnvironment( "ANDROID_MAVEN_PLUGIN_LOCAL_STATIC_LIBRARIES",
                        MakefileHelper.createLibraryList( resolveNativeLibraryArtifacts, true ) );
            }

            // Only add the LOCAL_SHARED_LIBRARIES
            if ( NativeHelper.hasSharedNativeLibraryArtifact( resolveNativeLibraryArtifacts ) )
            {
                executor.addEnvironment( "ANDROID_MAVEN_PLUGIN_LOCAL_SHARED_LIBRARIES",
                        MakefileHelper.createLibraryList( resolveNativeLibraryArtifacts, false ) );
            }

            // Adds the location of the Makefile capturer file - this file will after the build include
            // things like header files, flags etc.  It is processed after the build to retrieve the headers
            // and also capture flags etc ...
            final File makefileCaptureFile = File.createTempFile( "android_maven_plugin_makefile_captures", ".tmp" );
            makefileCaptureFile.deleteOnExit();
            executor.addEnvironment( MakefileHelper.MAKEFILE_CAPTURE_FILE, makefileCaptureFile.getAbsolutePath() );

            // Add any defined system properties
            if ( systemProperties != null && ! systemProperties.isEmpty() )
            {
                for ( Map.Entry<String, String> entry : systemProperties.entrySet() )
                {
                    executor.addEnvironment( entry.getKey(), entry.getValue() );
                }
            }

            executor.setLogger( this.getLog() );

            // Setup the command line for the make
            final List<String> commands = new ArrayList<String>();

            // Setup the build directory (defaults to the current directory) but may be different depending
            // on user configuration
            commands.add( "-C" );
            if ( ndkBuildDirectory == null )
            {
                ndkBuildDirectory = project.getBasedir().getAbsolutePath();
            }
            commands.add( ndkBuildDirectory );

            // If the build should use a custom makefile or not - some validation is done to ensure
            // this exists and all
            if ( makefile != null )
            {
                File makeFile = new File( project.getBasedir(), makefile );
                if ( ! makeFile.exists() )
                {
                    getLog().error( "Specified makefile " + makeFile + " does not exist" );
                    throw new MojoExecutionException( "Specified makefile " + makeFile + " does not exist" );
                }
                commands.add( "-f" );
                commands.add( makefile );
            }

            // Setup the correct toolchain to use
            // FIXME: performa validation that this toolchain exists in the NDK
            commands.add( "NDK_TOOLCHAIN=" + ndkToolchain );

            // Anything else on the command line the user wants to add - simply splice it up and
            // add it one by one to the command line
            if ( ndkBuildAdditionalCommandline != null )
            {
                String[] additionalCommands = ndkBuildAdditionalCommandline.split( " " );
                for ( final String command : additionalCommands )
                {
                    commands.add( command );
                }
            }

            // If a build target is specified, tag that onto the command line as the
            // very last of the parameters
            if ( target != null )
            {
                commands.add( target );
            } else /*if ( "a".equals( project.getPackaging() ) )*/
            {
                // Hack for the moment - seems .so projects will happily use .so
                // getLog().info( "Statically linked native library being built, forcing NDK target to "+project.getArtifactId() );
                // getLog().info( "If target is not "+project.getArtifactId()+" please investigate and use the 'target' configuration parameter!" );
                commands.add( project.getArtifactId() );
            }

            final String ndkBuildPath = resolveNdkBuildExecutable();
            getLog().info( ndkBuildPath + " " + commands.toString() );

            executor.executeCommand( ndkBuildPath, commands, project.getBasedir(), true );

            try
            {
                // Cleanup libs/armeabi directory if needed - this implies moving any native artifacts into target/libs
                if ( clearNativeArtifacts )
                {
                    final File destinationDirectory =
                            new File( ndkOutputDirectory.getAbsolutePath(), "/" + ndkArchitecture );
                    if ( ! libsDirectoryExists )
                    {
                        FileUtils.moveDirectory( nativeLibDirectory, destinationDirectory );
                    } else
                    {
                        FileUtils.copyDirectory( nativeLibDirectory, destinationDirectory );
                        FileUtils.cleanDirectory( nativeLibDirectory );
                    }
                    nativeLibDirectory = destinationDirectory;
                }

                // Attempt to attach the native library if the project is defined as a "pure" native Android library
                // (packaging is 'so' or 'a') or if the plugin has been configured to attach the native library to the build
                if ( "so".equals( project.getPackaging() ) || "a".equals( project.getPackaging() ) ||
                        attachNativeArtifacts )
                {

                    final File nativeArtifactFile;
                    if ( ndkFinalLibraryName == null )
                    {
                        File[] files = nativeLibDirectory.listFiles( new FilenameFilter()
                        {
                            public boolean accept(final File dir, final String name)
                            {
                                if ( "a".equals( project.getPackaging() ) )
                                {
                                    return name.startsWith(
                                            "lib" + ( target != null ? target : project.getArtifactId() ) ) &&
                                            name.endsWith( ".a" );
                                } else
                                {
                                    return name.startsWith(
                                            "lib" + ( target != null ? target : project.getArtifactId() ) ) &&
                                            name.endsWith( ".so" );
                                }
                            }
                        } );
                        // slight limitation at this stage - we only handle a single .so artifact
                        if ( files == null || files.length != 1 )
                        {

                            getLog().warn( "Error while detecting native compile artifacts: " +
                                    ( files == null || files.length == 0 ? "None found" :
                                            "Found more than 1 artifact" ) );

                            if ( files != null && files.length > 1 )
                            {
                                getLog().debug( "List of files found: " + Arrays.asList( files ) );
                                getLog().error(
                                        "Currently, only a single, final native library is supported by the build" );
                                throw new MojoExecutionException(
                                        "Currently, only a single, final native library is supported by the build" );
                            } else
                            {
                                getLog().error(
                                        "No native compiled library found, did the native compile complete successfully?" );
                                throw new MojoExecutionException(
                                        "No native compiled library found, did the native compile complete successfully?" );
                            }
                        }
                        nativeArtifactFile = files[ 0 ];
                    } else
                    {
                        // Find the nativeArtifactFile in the nativeLibDirectory/ndkFinalLibraryName
                        nativeArtifactFile =
                                new File( nativeLibDirectory, ndkFinalLibraryName + "." + project.getPackaging() );
                        if ( ! nativeArtifactFile.exists() )
                        {
                            getLog().error(
                                    "Could not locate final native library using the provided ndkFinalLibraryName " +
                                            ndkFinalLibraryName + " (tried " + nativeArtifactFile.getAbsolutePath() +
                                            ")" );
                            throw new MojoExecutionException(
                                    "Could not locate final native library using the provided ndkFinalLibraryName " +
                                            ndkFinalLibraryName + " (tried " + nativeArtifactFile.getAbsolutePath() +
                                            ")" );
                        }
                    }

                    final String artifactType = resolveArtifactType( nativeArtifactFile );
                    if ( "so".equals( artifactType ) && ! skipStripping )
                    {
                        getLog().debug( "Post processing (stripping) native compiled artifact: " + nativeArtifactFile );
                        invokeNDKStripper( nativeArtifactFile );
                    }

                    getLog().debug( "Adding native compiled artifact: " + nativeArtifactFile );

                    File fileToAttach = nativeArtifactFile;
                    if ( ! libsDirectoryExists )
                    {
                        getLog().debug( "Moving native compiled artifact to target directory for preservation" );
                        // This indicates the output directory was created by the build (us) and that we should really
                        // move it to the target (needed to preserve the attached artifact once install is invoked)
                        final File destFile =
                                new File( project.getBuild().getDirectory(), nativeArtifactFile.getName() );
                        FileUtils.moveFile( nativeArtifactFile, destFile );
                        fileToAttach = destFile;
                    }

                    projectHelper.attachArtifact( this.project, artifactType,
                            ( ndkClassifier != null ? ndkClassifier : ndkArchitecture ), fileToAttach );

                }

                // Process conditionally any of the headers to include into the header archive file
                processMakefileCapture( makefileCaptureFile );

            } finally
            {
                // If we created any directories as part of the build, blow those away after we're done
                if ( ! libsDirectoryExists )
                {
                    getLog().info( "Cleaning up native library output directory after build" );
                    getLog().debug( "Removing directory: " + directoryToRemove );
                    FileUtils.deleteDirectory( directoryToRemove );
                }

                // If we created a makefile for the build we should be polite and remove any extracted include
                // directories after we're done
                if ( makefileHolder != null )
                {
                    getLog().info( "Cleaning up extracted include directories used for build" );
                    MakefileHelper.cleanupAfterBuild( makefileHolder );
                }

            }

        } catch ( MojoExecutionException e )
        {
            getLog().error( "Error during build: " + e.getMessage(), e );
            throw e;
        } catch ( Exception e )
        {
            getLog().error( "Error while executing: " + e.getMessage() );
            throw new MojoExecutionException( e.getMessage(), e );
        }

    }

    private void validateMakefile(MavenProject project, String makefile)
    {
        // TODO: actually perform validation
    }

    private void invokeNDKStripper(File file) throws MojoExecutionException
    {
        try
        {
            getLog().debug( "Detected shared library artifact, will now strip it" );
            // Execute the strip command
            final CommandExecutor stripCommandExecutor = CommandExecutor.Factory.createDefaultCommmandExecutor();
            stripCommandExecutor.setErrorListener( new CommandExecutor.ErrorListener()
            {
                public boolean isError(String error)
                {
                    getLog().error( "Error while stripping binary: " + error );
                    return true;
                }
            } );
            stripCommandExecutor.setLogger( getLog() );

            stripCommandExecutor
                    .executeCommand( resolveNdkStripper().getAbsolutePath(), Arrays.asList( file.getAbsolutePath() ) );
        } catch ( ExecutionException e )
        {
            getLog().error( "Error while attempting to strip shared library", e );
            throw new MojoExecutionException( "Error while attempting to strip shared library" );
        }
    }

    private String resolveNdkBuildExecutable() throws MojoExecutionException
    {
        if ( ndkBuildExecutable != null )
        {
            getLog().debug( "ndk-build overriden, using " + ndkBuildExecutable );
            return ndkBuildExecutable;
        }
        return getAndroidNdk().getNdkBuildPath();
    }

    private File resolveNdkStripper() throws MojoExecutionException
    {
        return getAndroidNdk().getStripper( ndkToolchain );
    }

    private void processMakefileCapture(File localCIncludesFile) throws MojoExecutionException
    {

        try
        {
            if ( attachHeaderFiles )
            {

                final List<HeaderFilesDirective> finalHeaderFilesDirectives = new ArrayList<HeaderFilesDirective>();

                if ( useLocalSrcIncludePaths )
                {
                    Properties props = new Properties();
                    props.load( new FileInputStream( localCIncludesFile ) );
                    String localCIncludes = props.getProperty( "LOCAL_C_INCLUDES" );
                    if ( localCIncludes != null && ! localCIncludes.trim().isEmpty() )
                    {
                        String[] includes = localCIncludes.split( " " );
                        for ( String include : includes )
                        {
                            final HeaderFilesDirective headerFilesDirective = new HeaderFilesDirective();
                            File includeDir = new File( project.getBasedir(), include );
                            headerFilesDirective.setDirectory( includeDir.getAbsolutePath() );
                            headerFilesDirective.setIncludes( new String[]{"**/*.h"} );
                            finalHeaderFilesDirectives.add( headerFilesDirective );
                        }
                    }
                } else
                {
                    if ( headerFilesDirectives != null )
                    {
                        finalHeaderFilesDirectives.addAll( headerFilesDirectives );
                    }
                }
                if ( finalHeaderFilesDirectives.isEmpty() )
                {
                    getLog().debug( "No header files included, will add default set" );
                    final HeaderFilesDirective e = new HeaderFilesDirective();
                    e.setDirectory( new File( project.getBasedir() + "/jni" ).getAbsolutePath() );
                    e.setIncludes( new String[]{"**/*.h"} );
                    finalHeaderFilesDirectives.add( e );
                }
                createHeaderArchive( finalHeaderFilesDirectives );
            }
        } catch ( Exception e )
        {
            throw new MojoExecutionException( "Error while processing headers to include: " + e.getMessage(), e );
        }

    }

    private void createHeaderArchive(List<HeaderFilesDirective> finalHeaderFilesDirectives)
            throws MojoExecutionException
    {
        try
        {
            MavenArchiver mavenArchiver = new MavenArchiver();
            mavenArchiver.setArchiver( jarArchiver );

            final File jarFile = new File( new File( project.getBuild().getDirectory() ),
                    project.getBuild().getFinalName() + ".har" );
            mavenArchiver.setOutputFile( jarFile );

            for ( HeaderFilesDirective headerFilesDirective : finalHeaderFilesDirectives )
            {
                mavenArchiver.getArchiver().addDirectory( new File( headerFilesDirective.getDirectory() ),
                        headerFilesDirective.getIncludes(), headerFilesDirective.getExcludes() );
            }

            final MavenArchiveConfiguration mavenArchiveConfiguration = new MavenArchiveConfiguration();
            mavenArchiveConfiguration.setAddMavenDescriptor( false );

            mavenArchiver.createArchive( project, mavenArchiveConfiguration );
            projectHelper.attachArtifact( project, "har", ( ndkClassifier != null ? ndkClassifier : ndkArchitecture ),
                    jarFile );

        } catch ( Exception e )
        {
            throw new MojoExecutionException( e.getMessage() );
        }
    }

    private Set<Artifact> findNativeLibraryDependencies() throws MojoExecutionException
    {
        NativeHelper nativeHelper =
                new NativeHelper( project, projectRepos, repoSession, repoSystem, artifactFactory, getLog() );
        final Set<Artifact> staticLibraryArtifacts =
                nativeHelper.getNativeDependenciesArtifacts( unpackedApkLibsDirectory, false );
        final Set<Artifact> sharedLibraryArtifacts =
                nativeHelper.getNativeDependenciesArtifacts( unpackedApkLibsDirectory, true );
        final Set<Artifact> mergedArtifacts = new LinkedHashSet<Artifact>( staticLibraryArtifacts );
        mergedArtifacts.addAll( sharedLibraryArtifacts );
        return mergedArtifacts;
    }

    /**
     * Resolve the artifact type from the current project and the specified file.  If the project packaging is
     * either 'a' or 'so' it will use the packaging, otherwise it checks the file for the extension
     *
     * @param file The file being added as an artifact
     * @return The artifact type (so or a)
     */
    private String resolveArtifactType(File file)
    {
        if ( "so".equals( project.getPackaging() ) || "a".equals( project.getPackaging() ) )
        {
            return project.getPackaging();
        } else
        {
            // At this point, the file (as found by our filtering previously will end with either 'so' or 'a'
            return file.getName().endsWith( "so" ) ? "so" : "a";
        }
    }

}
