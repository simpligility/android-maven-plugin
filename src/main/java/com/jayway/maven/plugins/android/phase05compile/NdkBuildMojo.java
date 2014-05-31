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
import com.jayway.maven.plugins.android.common.Const;
import com.jayway.maven.plugins.android.common.NativeHelper;
import com.jayway.maven.plugins.android.config.PullParameter;
import com.jayway.maven.plugins.android.configuration.HeaderFilesDirective;
import com.jayway.maven.plugins.android.configuration.NDKArchitectureToolchainMappings;
import org.apache.commons.io.FileUtils;
import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
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
     * Allows for overriding the default ndk-build executable.
     *
     * @parameter property="android.ndk.ndk-build-executable"
     */
    @PullParameter
    private String ndkBuildExecutable;

    /**
     * Folder in which the Ndk makefiles are constructed and the build is executed.
     *
     * @parameter property="android.ndk.ndk-build-directory"
     *            default-value= "${project.build.directory}/ndk-build"
     * @readonly
     */
    @PullParameter
    private File ndkBuildDirectory;

    /**
     * Specifies the classifier with which the artifact should be stored in the repository
     *
     * @parameter property="android.ndk.build.native-classifier"
     */
    @PullParameter
    private String ndkClassifier;

    /**
     * Specifies additional command line parameters to pass to ndk-build
     *
     * @parameter property="android.ndk.build.command-line"
     */
    @PullParameter
    protected String ndkBuildAdditionalCommandline;

    /**
     * Flag indicating whether the resulting native library should be attached as an artifact to the build.  This
     * means the resulting .so is installed into the repository as well as being included in the final APK.
     *
     * @parameter property="android.ndk.build.attach-native-artifact" default-value="false"
     */
    @PullParameter( defaultValue = "false" )
    private Boolean attachNativeArtifacts;

    /**
     * Build folder to place built native libraries into
     *
     * @parameter property="android.ndk.build.ndk-output-directory"
     *            default-value="${project.build.directory}/ndk-libs"
     * @readonly
     */
    @PullParameter
    private File ndkOutputDirectory;

    /**
     * <p>Folder containing native, static libraries compiled and linked by the NDK.</p>
     *
     * The NDK build executable seems determined to create the native libs in the root folder.
     * TODO work out how to create them in /target.
     *
     * @parameter property="android.nativeLibrariesOutputDirectory"
     *            default-value="${project.basedir}/obj/local"
     * @readonly
     */
    private File nativeLibrariesOutputDirectory;

    /**
     * <p>Target to invoke on the native makefile.</p>
     *
     * @parameter property="android.nativeTarget"
     */
    @PullParameter
    private String target;

    /**
     * Defines the architecture for the NDK build
     *
     * @parameter property="android.ndk.build.architecture"
     * @deprecated Use {@link NdkBuildMojo#ndkArchitectures} instead
     */
    @PullParameter
    private String ndkArchitecture;

    /**
     * Defines the architectures for the NDK build - this is a space separated list (i.e x86 armeabi)
     *
     * @parameter property="android.ndk.build.architectures"
     */
    @PullParameter
    private String ndkArchitectures;

    /**
     * Defines the architecture to toolchain mappings for the NDK build
     * &lt;ndkArchitectureToolchainMappings&gt;
     *   &lt;x86&gt;x86-4.7&lt;/x86&gt;
     *   &lt;armeabi&gt;arm-linux-androideabi-4.7&lt;/armeabi&gt;
     * &lt;/ndkArchitectureToolchainMappings&gt;
     *
     * @parameter
     */
    @PullParameter
    private NDKArchitectureToolchainMappings ndkArchitectureToolchainMappings;

    /**
     * Flag indicating whether the header files used in the build should be included and attached to the build as
     * an additional artifact.
     *
     * @parameter property="android.ndk.build.attach-header-files" default-value="true"
     */
    @PullParameter( defaultValue = "true" )
    private Boolean attachHeaderFiles;

    /**
     * Flag indicating whether the make files last LOCAL_SRC_INCLUDES should be used for determing what header
     * files to include.  Setting this flag to true, overrides any defined header files directives.
     * <strong>Note: </strong> By setting this flag to true, all header files used in the project will be
     * added to the resulting header archive.  This may be undesirable in most cases and is therefore turned off by
     * default.
     *
     * @parameter property="android.ndk.build.use-local-src-include-paths" default-value="false"
     */
    @PullParameter( defaultValue = "false" )
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
     * @component role="org.apache.maven.artifact.handler.ArtifactHandler" roleHint="har"
     */
    private ArtifactHandler harArtifactHandler;

    /**
     * Flag indicating whether the header files for native, static library dependencies should be used.  If true,
     * the header archive for each statically linked dependency will be resolved.
     *
     * @parameter property="android.ndk.build.use-header-archives" default-value="true"
     */
    @PullParameter( defaultValue = "true" )
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
     * @parameter property="android.ndk.build.ignore-build-warnings" default-value="true"
     */
    @PullParameter( defaultValue = "true" )
    private Boolean ignoreBuildWarnings;

    /**
     * Defines the regular expression used to detect whether error/warning output from ndk-build is a minor compile
     * warning or is actually an error which should cause the build to fail.
     * <p/>
     * If the pattern matches, the output from the compiler will <strong>not</strong> be considered an error and compile
     * will be successful.
     *
     * @parameter property="android.ndk.build.build-warnings-regular-expression"
     * default-value=".*[warning|note]: .*"
     */
    @PullParameter( defaultValue = ".*[warning|note]: .*" )
    private String buildWarningsRegularExpression;

    /**
     * @parameter property="android.ndk.build.skip-native-library-stripping" default-value="false"
     */
    @PullParameter( defaultValue = "false" )
    private Boolean skipStripping;

    /**
     * @parameter property="android.ndk.build.ndk-toolchain"
     */
    @PullParameter
    private String ndkToolchain;


    /**
     * Specifies the final name of the library output by the build (this allows 
     * the pom to override the default artifact name). The value should not
     * include the 'lib' prefix or filename extension (e.g. '.so').
     *
     * @parameter property="android.ndk.build.build.final-library.name"
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

    /**
     * Specifies the application makefile to use for the build (if other than the default Application.mk).
     *
     * @parameter
     */
    @PullParameter
    private String applicationMakefile;

    /**
     * Flag indicating whether to use the max available jobs for the host machine
     *
     * @parameter property="android.ndk.build.maxJobs" default-value="false"
     */
    @PullParameter( defaultValue = "false" )
    private Boolean maxJobs;

    /**
     *
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        // Validate the NDK
        final File ndkBuildFile = new File( getAndroidNdk().getNdkBuildPath() );
        NativeHelper.validateNDKVersion( ndkBuildFile.getParentFile() );

        validateMakefile( project, makefile );

        final String[] resolvedNDKArchitectures = NativeHelper.getNdkArchitectures(
            ndkArchitecture != null ? ndkArchitecture : ndkArchitectures, applicationMakefile,
            project.getBasedir() );

        for ( String architecture : resolvedNDKArchitectures )
        {
            try
            {
                compileForArchitecture( architecture );
            }
            catch ( IOException e )
            {
                getLog().error( "Error while executing: " + e.getMessage() );
                throw new MojoExecutionException( e.getMessage(), e );
            }
            catch ( ExecutionException e )
            {
                getLog().error( "Error while executing: " + e.getMessage() );
                throw new MojoExecutionException( e.getMessage(), e );
            }
        }
    }

    private void compileForArchitecture( String architecture )
        throws MojoExecutionException, IOException, ExecutionException
    {
        getLog().debug( "Resolving for NDK architecture : " + architecture );

        // Start setting up the command line to be executed
        final CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        // Add an error listener to the build - this allows the build to conditionally fail
        // depending on a) the output of the build b) whether or not build errors (output on stderr) should be
        // ignored and c) whether the pattern matches or not
        executor.setErrorListener( getNdkErrorListener() );

        final Set<Artifact> nativeLibraryArtifacts = findNativeLibraryDependencies();

        // If there are any static libraries the code needs to link to, include those in the make file
        final Set<Artifact> resolveNativeLibraryArtifacts =
            getArtifactResolverHelper().resolveArtifacts( nativeLibraryArtifacts );

        getLog().debug( "resolveArtifacts found " + resolveNativeLibraryArtifacts.size()
            + ": " + resolveNativeLibraryArtifacts.toString() );

        final File buildFolder = new File( ndkBuildDirectory, architecture );
        buildFolder.mkdirs();

        final File androidMavenMakefile = new File( buildFolder, "android_maven_plugin_makefile.mk" );
        final MakefileHelper makefileHelper = new MakefileHelper( getLog(),
            getArtifactResolverHelper(),
            harArtifactHandler, getUnpackedLibsDirectory()
        );

        final MakefileHelper.MakefileHolder makefileHolder = makefileHelper
            .createMakefileFromArtifacts( resolveNativeLibraryArtifacts, architecture, "armeabi", useHeaderArchives );

        final FileOutputStream output = new FileOutputStream( androidMavenMakefile );
        try
        {
            IOUtil.copy( makefileHolder.getMakeFile(), output );
        }
        finally
        {
            output.close();
        }

        // Add the path to the generated makefile - this is picked up by the build (by an include from the user)
        executor.addEnvironment( "ANDROID_MAVEN_PLUGIN_MAKEFILE", androidMavenMakefile.getAbsolutePath() );

        setupNativeLibraryEnvironment( makefileHelper, executor, resolveNativeLibraryArtifacts,
            architecture );

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
        commands.add( ndkBuildDirectory.getAbsolutePath() );

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

        configureApplicationMakefile( commands );
        configureMaxJobs( commands );
        configureNdkToolchain( architecture, commands );
        configureAdditionalCommands( commands );

        // If a build target is specified, tag that onto the command line as the very last of the parameters
        if ( target != null )
        {
            commands.add( target );
        }
        else /*if ( Const.ArtifactType.NATIVE_IMPLEMENTATION_ARCHIVE.equals( project.getPackaging() ) )*/
        {
            commands.add( project.getArtifactId() );
        }

        final String ndkBuildPath = resolveNdkBuildExecutable();
        getLog().debug( ndkBuildPath + " " + commands.toString() );
        getLog().info( "Executing NDK " + architecture + " make at : " + ndkBuildDirectory );

        executor.setCaptureStdOut( true );
        executor.executeCommand( ndkBuildPath, commands, ndkBuildDirectory, true );
        getLog().debug( "Executed NDK " + architecture + " make at : " + ndkBuildDirectory );

        // Where the NDK build creates the libs.
        final File nativeLibOutputDirectory = new File( nativeLibrariesOutputDirectory, architecture );
        nativeLibOutputDirectory.mkdirs();

        // Move the built native libs into the packaging folder.
        // We don't create them there to start with because the NDK build seems determined to create them in the root.
        final File destinationDirectory = new File( ndkOutputDirectory, architecture );
        FileUtils.moveDirectory( nativeLibOutputDirectory, destinationDirectory );

        // Attempt to attach the native library if the project is defined as a "pure" native Android library
        // (packaging is 'so' or 'a') or if the plugin has been configured to attach the native library to the build
        if ( Const.ArtifactType.NATIVE_SYMBOL_OBJECT.equals( project.getPackaging() )
            || Const.ArtifactType.NATIVE_IMPLEMENTATION_ARCHIVE.equals( project.getPackaging() )
            || attachNativeArtifacts )
        {
            attachNativeLib( destinationDirectory, architecture );
        }

        // Process conditionally any of the headers to include into the header archive file
        if ( attachHeaderFiles )
        {
            attachHeaderFiles( makefileCaptureFile, architecture );
        }

        // If we created a makefile for the build we should be polite and remove any extracted include
        // directories after we're done
        getLog().info( "Cleaning up extracted include directories used for build" );
        MakefileHelper.cleanupAfterBuild( makefileHolder );
    }

    private void configureAdditionalCommands( final List<String> commands )
    {
        // Anything else on the command line the user wants to add - simply splice it up and
        // add it one by one to the command line
        if ( ndkBuildAdditionalCommandline != null )
        {
            final String[] additionalCommands = ndkBuildAdditionalCommandline.split( " " );
            commands.addAll( Arrays.asList( additionalCommands ) );
        }
    }

    private void configureApplicationMakefile( List<String> commands )
        throws MojoExecutionException
    {
        if ( applicationMakefile != null )
        {
            File appMK = new File( project.getBasedir(), applicationMakefile );
            if ( ! appMK.exists() )
            {
                getLog().error( "Specified application makefile " + appMK + " does not exist" );
                throw new MojoExecutionException( "Specified application makefile " + appMK + " does not exist" );
            }
            commands.add( "NDK_APPLICATION_MK=" + applicationMakefile );
        }
    }

    private void configureMaxJobs( List<String> commands )
    {
        if ( maxJobs )
        {
            String jobs = String.valueOf( Runtime.getRuntime().availableProcessors() );
            getLog().info( "executing " + jobs + " parallel jobs" );
            commands.add( "-j" );
            commands.add( jobs );
        }
    }

    private void configureNdkToolchain( String architecture, List<String> commands )
            throws MojoExecutionException
    {
        if ( ndkToolchain != null )
        {
            // Setup the correct toolchain to use
            // FIXME: perform a validation that this toolchain exists in the NDK
            commands.add( "NDK_TOOLCHAIN=" + ndkToolchain );
        }
        else
        {
            // Resolve the toolchain from the architecture
            // <ndkArchitectures>
            //   <x86>x86-4.6</x86>
            //   <armeabi>x86-4.6</armeabi>
            // </ndkArchitectures>
            final String toolchainFromArchitecture = getAndroidNdk().getToolchainFromArchitecture(
                    architecture, ndkArchitectureToolchainMappings );
            getLog().debug( "Resolved toolchain for " + architecture + " to " + toolchainFromArchitecture );
            commands.add( "NDK_TOOLCHAIN=" + toolchainFromArchitecture );
            commands.add( "APP_ABI=" + architecture );

        }
    }

    /**
     * Attaches native libs to project.
     */
    private void attachNativeLib( File destinationDirectory, String architecture )
            throws IOException, MojoExecutionException
    {
        final File nativeArtifactFile;
        if ( ndkFinalLibraryName == null )
        {
            nativeArtifactFile = findNativeLibrary( destinationDirectory );
        }
        else
        {
            nativeArtifactFile = nativeLibraryFromName( destinationDirectory );
        }

        final String artifactType = resolveArtifactType( nativeArtifactFile );
        if ( nativeArtifactFile.getName().endsWith( ".so" ) && ! skipStripping )
        {
            getLog().debug( "Post processing (stripping) native compiled artifact: " + nativeArtifactFile );
            invokeNDKStripper( nativeArtifactFile );
        }

        getLog().debug( "Adding native compiled artifact: " + nativeArtifactFile );

        final String classifier = ( ndkClassifier == null )
                ? architecture
                : architecture + "-" + ndkClassifier;

        projectHelper.attachArtifact( this.project, artifactType, classifier, nativeArtifactFile );
    }

    /**
     * Search the specified directory for native artifacts that match the artifact Id
     */
    private File findNativeLibrary( File nativeLibDirectory ) throws MojoExecutionException
    {
        getLog().info( "Searching " + nativeLibDirectory + " for built library" );
        File[] files = nativeLibDirectory.listFiles( new FilenameFilter()
        {
            public boolean accept( final File dir, final String name )
            {
                String libraryName = ndkFinalLibraryName;
                
                if ( libraryName == null || libraryName.isEmpty() )
                {
                    libraryName = project.getArtifactId();
                }

                // FIXME: The following logic won't work for an APKLIB building a static library
                if ( Const.ArtifactType.NATIVE_IMPLEMENTATION_ARCHIVE.equals( project.getPackaging() ) )
                {
                    return name.startsWith(
                            "lib" + libraryName ) && name.endsWith( ".a" );
                }
                else
                {
                    return name.startsWith(
                            "lib" + libraryName ) && name.endsWith( ".so" );
                }
            }
        } );
        // slight limitation at this stage - we only handle a single .so artifact
        if ( files == null || files.length != 1 )
        {
            getLog().warn( "Error while detecting native compile artifacts: "
                    + ( files == null || files.length == 0
                            ? "None found"
                            : "Found more than 1 artifact" ) );
            if ( target != null )
            {
                getLog().warn( "Using the 'target' configuration option to specify the output file name "
                        + "is no longer supported, use 'ndkFinalLibraryName' instead." );
            }

            if ( files != null && files.length > 1 )
            {
                getLog().debug( "List of files found: " + Arrays.asList( files ) );
                getLog().error(
                        "Currently, only a single, final native library is supported by the build" );
                throw new MojoExecutionException(
                        "Currently, only a single, final native library is supported by the build" );
            }
            else
            {
                getLog().error( "No native compiled library found, did the native compile complete "
                        + "successfully?" );
                throw new MojoExecutionException( "No native compiled library found, did the native "
                        + "compile complete successfully?" );
            }
        }
        return files[ 0 ];
    }
    
    private File nativeLibraryFromName( File nativeLibDirectory ) throws MojoExecutionException
    {
        final File libraryFile;
        // Find the nativeArtifactFile in the nativeLibDirectory/ndkFinalLibraryName
        if ( Const.ArtifactType.NATIVE_SYMBOL_OBJECT.equals( project.getPackaging() )
                || Const.ArtifactType.NATIVE_IMPLEMENTATION_ARCHIVE.equals( project.getPackaging() ) )
        {
            libraryFile = new File( nativeLibDirectory,
                    "lib" + ndkFinalLibraryName + "." + project.getPackaging() );
        }
        else
        {
            final File staticLib = new File( nativeLibDirectory,
                    "lib" + ndkFinalLibraryName + ".a" );
            if ( staticLib.exists() )
            {
                libraryFile = staticLib;
            }
            else
            {
                libraryFile = new File( nativeLibDirectory,
                        "lib" + ndkFinalLibraryName + ".so" );
            }
        }
        if ( ! libraryFile.exists() )
        {
            getLog().error(
                    "Could not locate final native library using the provided ndkFinalLibraryName "
                            + ndkFinalLibraryName + " (tried " + libraryFile.getAbsolutePath()
                            + ")" );
            throw new MojoExecutionException(
                    "Could not locate final native library using the provided ndkFinalLibraryName "
                    + ndkFinalLibraryName + " (tried " + libraryFile.getAbsolutePath()
                    + ")" );
        }

        return libraryFile;
    }
    
    private CommandExecutor.ErrorListener getNdkErrorListener()
    {
        return new CommandExecutor.ErrorListener()
        {
            @Override
            public boolean isError( String error )
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
        };
    }

    /**
     * Validate the makefile - if our packaging type is so (for example) and there are
     * dependencies on .a files (or shared files for that matter) the makefile should include
     * the include of our Android Maven plugin generated makefile.
     */
    private void validateMakefile( MavenProject project, String file )
    {
        // TODO: actually perform validation
    }

    private void invokeNDKStripper( File file ) throws MojoExecutionException
    {
        try
        {
            getLog().debug( "Detected shared library artifact, will now strip it" );
            // Execute the strip command
            final CommandExecutor stripCommandExecutor = CommandExecutor.Factory.createDefaultCommmandExecutor();
            stripCommandExecutor.setErrorListener( new CommandExecutor.ErrorListener()
            {
                public boolean isError( String error )
                {
                    getLog().error( "Error while stripping binary: " + error );
                    return true;
                }
            } );
            stripCommandExecutor.setLogger( getLog() );
            stripCommandExecutor.setCaptureStdOut( true );
            stripCommandExecutor.executeCommand( resolveNdkStripper( file ).getAbsolutePath(),
                                                 Arrays.asList( file.getAbsolutePath() ) );
        }
        catch ( ExecutionException e )
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

    private File resolveNdkStripper( File nativeLibrary ) throws MojoExecutionException
    {
        if ( ndkToolchain != null )
        {
            return getAndroidNdk().getStripper( ndkToolchain );
        }
        else
        {
            return getAndroidNdk().getStripper( getAndroidNdk().getToolchain( nativeLibrary ) );
        }
    }

    private void attachHeaderFiles( File localCIncludesFile, String architecture )
        throws MojoExecutionException, IOException
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
                    headerFilesDirective.setIncludes( new String[]{ "**/*.h" } );
                    finalHeaderFilesDirectives.add( headerFilesDirective );
                }
            }
        }
        else
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
            e.setIncludes( new String[]{ "**/*.h" } );
            finalHeaderFilesDirectives.add( e );
        }
        createHeaderArchive( finalHeaderFilesDirectives, architecture );
    }

    private void createHeaderArchive( List<HeaderFilesDirective> finalHeaderFilesDirectives, String architecture )
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

            String classifier = architecture;
            if ( ndkClassifier != null )
            {
                classifier += "-" + ndkClassifier;
            }

            getLog().debug( "Attaching 'har' classifier=" + classifier + " file=" + jarFile );
            projectHelper.attachArtifact( project, Const.ArtifactType.NATIVE_HEADER_ARCHIVE, classifier, jarFile );

        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( e.getMessage() );
        }
    }

    private void setupNativeLibraryEnvironment( MakefileHelper makefileHelper, CommandExecutor executor,
                                                Set<Artifact> resolveNativeLibraryArtifacts, String architecture )
    {
        // Only add the LOCAL_STATIC_LIBRARIES
        if ( NativeHelper.hasStaticNativeLibraryArtifact( resolveNativeLibraryArtifacts, getUnpackedLibsDirectory(),
                                                          architecture ) )
        {
            String staticlibs = makefileHelper.createLibraryList( resolveNativeLibraryArtifacts, 
                                                                  architecture,
                                                                  true ); 
            executor.addEnvironment( "ANDROID_MAVEN_PLUGIN_LOCAL_STATIC_LIBRARIES", staticlibs );
            getLog().debug( "Set ANDROID_MAVEN_PLUGIN_LOCAL_STATIC_LIBRARIES = " + staticlibs );
        }

        // Only add the LOCAL_SHARED_LIBRARIES
        if ( NativeHelper.hasSharedNativeLibraryArtifact( resolveNativeLibraryArtifacts, getUnpackedLibsDirectory(),
                                                          architecture ) )
        {
            String sharedlibs = makefileHelper.createLibraryList( resolveNativeLibraryArtifacts, 
                                                                  architecture,
                                                                  false ); 
            executor.addEnvironment( "ANDROID_MAVEN_PLUGIN_LOCAL_SHARED_LIBRARIES", sharedlibs );
            getLog().debug( "Set ANDROID_MAVEN_PLUGIN_LOCAL_SHARED_LIBRARIES = " + sharedlibs );
        }
    }
    
    private Set<Artifact> findNativeLibraryDependencies() throws MojoExecutionException
    {
        final NativeHelper nativeHelper = getNativeHelper();
        final Set<Artifact> staticLibraryArtifacts = nativeHelper
                .getNativeDependenciesArtifacts( this, getUnpackedLibsDirectory(), false );
        final Set<Artifact> sharedLibraryArtifacts = nativeHelper
                .getNativeDependenciesArtifacts( this, getUnpackedLibsDirectory(), true );
        
        final Set<Artifact> mergedArtifacts = new LinkedHashSet<Artifact>();
        filterNativeDependencies( mergedArtifacts, staticLibraryArtifacts );
        filterNativeDependencies( mergedArtifacts, sharedLibraryArtifacts );

        getLog().debug( "findNativeLibraryDependencies found " + mergedArtifacts.size()
                + ": " + mergedArtifacts.toString() );

        return mergedArtifacts;
    }

    /**
     * Selectively add artifacts from source to target excluding any whose groupId and artifactId match
     * the current build.
     *
     * Introduced to work around an issue when the ndk-build is executed twice by maven for example when
     * invoking maven 'install site'. In this case the artifacts attached by the first invocation are
     * found but are not valid dependencies and must be excluded.
     *
     * @param targetSet artifact Set to copy in to
     * @param source artifact Set to filter
     */
    private void filterNativeDependencies( Set<Artifact> targetSet, Set<Artifact> source )
    {
        for ( Artifact a : source )
        {
            if ( project.getGroupId().equals( a.getGroupId() ) 
                    && project.getArtifactId().equals( a.getArtifactId() ) )
            {
                getLog().warn( "Excluding native dependency attached by this build" );
            }
            else
            {
                targetSet.add( a );
            }
        }
    }

    /**
     * Resolve the artifact type from the current project and the specified file.  If the project packaging is
     * either 'a' or 'so' it will use the packaging, otherwise it checks the file for the extension
     *
     * @param file The file being added as an artifact
     * @return The artifact type (so or a)
     */
    private String resolveArtifactType( File file )
    {
        if ( Const.ArtifactType.NATIVE_SYMBOL_OBJECT.equals( project.getPackaging() )
                || Const.ArtifactType.NATIVE_IMPLEMENTATION_ARCHIVE.equals( project.getPackaging() ) )
        {
            return project.getPackaging();
        }
        else
        {
            // At this point, the file (as found by our filtering previously will end with either 'so' or 'a'
            return file.getName().endsWith( Const.ArtifactType.NATIVE_SYMBOL_OBJECT )
                    ? Const.ArtifactType.NATIVE_SYMBOL_OBJECT
                    : Const.ArtifactType.NATIVE_IMPLEMENTATION_ARCHIVE;
        }
    }
}
