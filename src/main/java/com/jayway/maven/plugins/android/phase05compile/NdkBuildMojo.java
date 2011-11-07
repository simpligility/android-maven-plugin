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

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jayway.maven.plugins.android.*;
import com.jayway.maven.plugins.android.common.AetherHelper;
import com.jayway.maven.plugins.android.common.NativeHelper;
import com.jayway.maven.plugins.android.configuration.HeaderFilesDirective;
import com.jayway.maven.plugins.android.configuration.Ndk;
import org.apache.commons.io.FileUtils;
import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.plugin.*;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.util.IOUtil;

import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * @author Johan Lindquist <johanlindquist@gmail.com>
 * @goal ndk-build
 * @phase compile
 * @requiresProject true
 */
public class NdkBuildMojo extends AbstractAndroidMojo {
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
    private Ndk ndk;

    /** Allows for overriding the default ndk-build executable.
     *
     * @parameter expression="${android.ndk.ndk-build-executable}"
     */
    private String ndkBuildExecutable;

    /**
     *
     * @parameter expression="${android.ndk.ndk-build-directory}" default="${basedir}";
     */
    private String ndkBuildDirectory = "";

    /**
     * <p>Parameter designed to pick up <code>-Dandroid.ndk.path</code> in case there is no pom with an
     * <code>&lt;ndk&gt;</code> configuration tag.</p>
     * <p>Corresponds to {@link com.jayway.maven.plugins.android.configuration.Ndk#path}.</p>
     *
     * @parameter expression="${android.ndk.path}"
     * @readonly
     */
    private File ndkPath;

    /**
     * Specifies the classifier with which the artifact should be stored in the repository
     *
     * @parameter expression="${android.ndk.build.native-classifier}"
     */
    protected String ndkClassifier;

    /**
     * Specifies additional command line parameters to pass to ndk-build
     *
     * @parameter expression="${android.ndk.build.command-line}"
     */
    protected String ndkBuildAdditionalCommandline;

    /**
     * Flag indicating whether the NDK output directory (libs/&lt;architecture&gt;) should be cleared after build.  This
     * will essentially 'move' all the native artifacts (.so) to the ${project.build.directory}/libs/&lt;architecture&gt;.
     * If an APK is built as part of the invocation, the libraries will be included from here.
     *
     * @parameter expression="${android.ndk.build.clear-native-artifacts}" default="false"
     */
    protected boolean clearNativeArtifacts = false;

    /**
     * Flag indicating whether the resulting native library should be attached as an artifact to the build.  This
     * means the resulting .so is installed into the repository as well as being included in the final APK.
     *
     * @parameter expression="${android.ndk.build.attach-native-artifact}" default="false"
     */
    protected boolean attachNativeArtifacts;

    /**
     * The <code>ANDROID_NDK_HOME</code> environment variable name.
     */
    public static final String ENV_ANDROID_NDK_HOME = "ANDROID_NDK_HOME";

    /**
     * Build folder to place built native libraries into
     *
     * @parameter expression="${android.ndk.build.ndk-output-directory}" default-value="${project.build.directory}/ndk-libs"
     */
    protected File ndkOutputDirectory;

    /** <p>Folder containing native, shared libraries compiled and linked by the NDK.</p>
     *
     * @parameter expression="${android.nativeLibrariesDirectory}" default-value="${project.basedir}/libs"
     */
    private File nativeSharedLibrariesDirectory;

    /** <p>Folder containing native, static libraries compiled and linked by the NDK.</p>
     *
     * @parameter expression="${android.nativeStaticLibrariesDirectory}" default-value="${project.basedir}/obj/local"
     */
    private File nativeStaticLibrariesDirectory;

    /** <p>Target to invoke on the native makefile.</p>
     *
     * @parameter expression="${android.nativeTarget}"
     */
    private String target;

    /**
     * Defines the architecture for the NDK build
     *
     * @parameter expression="${android.ndk.build.architecture}" default="armeabi"
     */
    protected String ndkArchitecture = "armeabi";

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
     * @parameter expression="${android.ndk.build.attach-header-files}" default="true"
     */
    private boolean attachHeaderFiles = true;

    /** Flag indicating whether the make files last LOCAL_SRC_INCLUDES should be used for determing what header
     * files to include.  Setting this flag to true, overrides any defined header files directives.
     * <strong>Note: </strong> By setting this flag to true, all header files used in the project will be
     * added to the resulting header archive.  This may be undesirable in most cases and is therefore turned off by default.
     *
     * @parameter expression="${android.ndk.build.use-local-src-include-paths}" default="false"
     */
    private boolean useLocalSrcIncludePaths = false;

    /**  Specifies the set of header files includes/excludes which should be used for bundling the exported header
     * files.  The below shows an example of how this can be used.
     *
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
    private List<HeaderFilesDirective> headerFilesDirectives;


    /** The Jar archiver.
     *
     * @component role="org.codehaus.plexus.archiver.Archiver" roleHint="jar"
     */
    private JarArchiver jarArchiver;

    /**
     * Flag indicating whether the header files for native, static library dependencies should be used.  If true,
     * the header archive for each statically linked dependency will be resolved.
     *
     * @parameter expression="${android.ndk.build.use-header-archives}" default="true"
     */
    private boolean useHeaderArchives = true;

    /** Defines additional system properties which should be exported to the ndk-build script.  This
     * <br/>
     * <pre>
     * &lt;systemProperties&gt;
     *   &lt;propertyName&gt;propertyValue&lt;/propertyName&gt;
     *   &lt;build-target&gt;android&lt;/build-target&gt;
     *   [..]
     * &lt;/systemProperties&gt;
     * </pre>     *
     * @parameter
     */
    private Map<String, String> systemProperties;

     /**
      * Flag indicating whether warnings should be ignored while compiling.  If true,
      * the build will not fail if warning are found during compile.
      *
      * @parameter expression="${android.ndk.build.ignore-build-warnings}" default="true"
      */
    private boolean ignoreBuildWarnings = true;

    /**
     * Defines the regular expression used to detect whether error/warning output from ndk-build is a minor compile warning
     * or is actually an error which should cause the build to fail.
     *
     * @parameter expression="${android.ndk.build.build-warnings-regular-expression}" default=".*[warning|note]: .*"
     */
    private String buildWarningsRegularExpression = ".*[warning|note]: .*";

    public void execute() throws MojoExecutionException, MojoFailureException {

        // This points 
        File nativeLibDirectory = new File((project.getPackaging().equals("a") ? nativeStaticLibrariesDirectory : nativeSharedLibrariesDirectory), ndkArchitecture );

        final boolean libsDirectoryExists = nativeLibDirectory.exists();

        File directoryToRemove = nativeLibDirectory;

        if ( !libsDirectoryExists ) {
            getLog().info( "Creating native output directory " + nativeLibDirectory );

            if ( nativeLibDirectory.getParentFile().exists() ) {
                nativeLibDirectory.mkdir();
            } else {
                nativeLibDirectory.mkdirs();
                directoryToRemove = nativeLibDirectory.getParentFile();
            }

        }

        final CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();

        executor.setErrorListener(new CommandExecutor.ErrorListener() {
            @Override
            public boolean isError(String error) {

                Pattern pattern = Pattern.compile(buildWarningsRegularExpression);
                Matcher matcher = pattern.matcher(error);

                if ( ignoreBuildWarnings && matcher.matches() ) {
                    return false;
                }
                return true;
            }
        });

        final Set<Artifact> nativeLibraryArtifacts = findNativeLibraryDependencies();
        // If there are any static libraries the code needs to link to, include those in the make file
        final Set<Artifact> resolveNativeLibraryArtifacts = AetherHelper.resolveArtifacts( nativeLibraryArtifacts, repoSystem, repoSession, projectRepos );

        try {
            File f = File.createTempFile( "android_maven_plugin_makefile", ".mk" );
            f.deleteOnExit();

            String makeFile = MakefileHelper.createMakefileFromArtifacts( f.getParentFile(), resolveNativeLibraryArtifacts, useHeaderArchives, repoSession, projectRepos, repoSystem);
            IOUtil.copy( makeFile, new FileOutputStream( f ));

            // Add the path to the generated makefile
            executor.addEnvironment( "ANDROID_MAVEN_PLUGIN_MAKEFILE", f.getAbsolutePath() );

            // Only add the LOCAL_STATIC_LIBRARIES
            if ( NativeHelper.hasStaticNativeLibraryArtifact(resolveNativeLibraryArtifacts) )
            {
                executor.addEnvironment( "ANDROID_MAVEN_PLUGIN_LOCAL_STATIC_LIBRARIES", MakefileHelper.createStaticLibraryList(resolveNativeLibraryArtifacts, true ));
            }
            if ( NativeHelper.hasSharedNativeLibraryArtifact(resolveNativeLibraryArtifacts) )
            {
                executor.addEnvironment( "ANDROID_MAVEN_PLUGIN_LOCAL_SHARED_LIBRARIES", MakefileHelper.createStaticLibraryList(resolveNativeLibraryArtifacts, false ));
            }

        } catch ( IOException e ) {
            throw new MojoExecutionException(e.getMessage());
        }

        File localCIncludesFile = null;
        //
        try {
            localCIncludesFile = File.createTempFile("android_maven_plugin_makefile_captures", ".tmp");
            localCIncludesFile.deleteOnExit();
            executor.addEnvironment( "ANDROID_MAVEN_PLUGIN_LOCAL_C_INCLUDES_FILE", localCIncludesFile.getAbsolutePath());
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage());
        }

        // Add any defined system properties
        if (systemProperties != null && !systemProperties.isEmpty()) {
            for ( Map.Entry<String, String> entry : systemProperties.entrySet() ) {
                executor.addEnvironment( entry.getKey(), entry.getValue() );
            }
        }

        executor.setLogger( this.getLog() );
        final List<String> commands = new ArrayList<String>();

        commands.add( "-C" );
        if (ndkBuildDirectory == null)
        {
            ndkBuildDirectory = project.getBasedir().getAbsolutePath();
        }
        commands.add( ndkBuildDirectory );

        if ( ndkBuildAdditionalCommandline != null ) {
            String[] additionalCommands = ndkBuildAdditionalCommandline.split( " " );
            for ( final String command : additionalCommands ) {
                commands.add( command );
            }
        }

        // If a build target is specified, tag that onto the command line as the
        // very last of the parameters
        if ( target != null ) {
            commands.add(target);
        }
        else if ( "a".equals( project.getPackaging() ) ) {
            commands.add( project.getArtifactId() );
        }

        final String ndkBuildPath = resolveNdkBuildExecutable();
        getLog().info( ndkBuildPath + " " + commands.toString() );

        try {
            executor.executeCommand( ndkBuildPath, commands, project.getBasedir(), true );
        }
        catch ( ExecutionException e ) {
            throw new MojoExecutionException( e.getMessage(), e );
        }

        // Cleanup libs/armeabi directory if needed - this implies moving any native artifacts into target/libs
        if ( clearNativeArtifacts ) {

            final File destinationDirectory = new File( ndkOutputDirectory.getAbsolutePath(), "/" + ndkArchitecture );

            try {
                if ( !libsDirectoryExists ) {
                    FileUtils.moveDirectory( nativeLibDirectory, destinationDirectory );
                } else {
                    FileUtils.copyDirectory( nativeLibDirectory, destinationDirectory );
                    FileUtils.cleanDirectory( nativeLibDirectory );
                }

                nativeLibDirectory = destinationDirectory;

            }
            catch ( IOException e ) {
                throw new MojoExecutionException( e.getMessage(), e );
            }
        }

        if ( !libsDirectoryExists ) {
            getLog().info( "Cleaning up native library output directory after build" );
            getLog().debug( "Removing directory: " + directoryToRemove );
            if ( !directoryToRemove.delete() ) {
                getLog().warn( "Could not remove directory, marking as delete on exit" );
                directoryToRemove.deleteOnExit();
            }
        }

        // Attempt to attach the native library if the project is defined as a "pure" native Android library
        // (packaging is 'so' or 'a') or if the plugin has been configured to attach the native library to the build
        if ( "so".equals(project.getPackaging()) || "a".equals(project.getPackaging()) || attachNativeArtifacts ) {

            File[] files = nativeLibDirectory.listFiles( new FilenameFilter() {
                public boolean accept( final File dir, final String name ) {
                    if ( "a".equals( project.getPackaging() ) ) {
                        return name.startsWith("lib" + (target != null ? target : project.getArtifactId())) && name.endsWith(".a");
                    }
                    else {
                        return name.startsWith("lib" + (target != null ? target : project.getArtifactId())) && name.endsWith(".so");
                    }
                }
            } );

            // slight limitation at this stage - we only handle a single .so artifact
            if ( files == null || files.length != 1 ) {
                getLog().warn( "Error while detecting native compile artifacts: " + ( files == null || files.length == 0 ? "None found" : "Found more than 1 artifact" ) );
                if ( files != null ) {
                    getLog().warn( "Currently, only a single, final native library is supported by the build" );
                }
            } else {
                getLog().debug( "Adding native compile artifact: " + files[ 0 ] );
                final String artifactType = resolveArtifactType(files[0]);
                projectHelper.attachArtifact( this.project, artifactType, ( ndkClassifier != null ? ndkClassifier : ndkArchitecture ), files[ 0 ] );
            }

        }

        // Process conditionally any of the headers to include into the header archive file
        processHeaderFileIncludes(localCIncludesFile);


    }

    private String resolveNdkBuildExecutable() throws MojoExecutionException {
        if (ndkBuildExecutable != null)
        {
            getLog().debug("ndk-build overriden, using " + ndkBuildExecutable);
            return ndkBuildExecutable;
        }
        return getAndroidNdk().getNdkBuildPath();
    }

    private void processHeaderFileIncludes(File localCIncludesFile) throws MojoExecutionException {

        try
        {
            if ( attachHeaderFiles ) {

                final List<HeaderFilesDirective> finalHeaderFilesDirectives = new ArrayList<HeaderFilesDirective>();

                if (useLocalSrcIncludePaths) {
                    Properties props = new Properties();
                    props.load(new FileInputStream(localCIncludesFile));
                    String localCIncludes = props.getProperty("LOCAL_C_INCLUDES");
                    if (localCIncludes != null && !localCIncludes.trim().isEmpty())
                    {
                        String[] includes = localCIncludes.split(" ");
                        for (String include : includes) {
                            final HeaderFilesDirective headerFilesDirective = new HeaderFilesDirective();
                            headerFilesDirective.setDirectory(include);
                            headerFilesDirective.setIncludes(new String[]{"**/*.h"});
                            finalHeaderFilesDirectives.add(headerFilesDirective);
                        }
                    }
                }
                else {
                    if ( headerFilesDirectives != null ) {
                        finalHeaderFilesDirectives.addAll(headerFilesDirectives);
                    }
                }
                if (finalHeaderFilesDirectives.isEmpty()) {
                    getLog().debug("No header files included, will add default set");
                    final HeaderFilesDirective e = new HeaderFilesDirective();
                    e.setDirectory(new File(project.getBasedir() + "/jni").getAbsolutePath());
                    e.setIncludes(new String[]{"**/*.h"});
                    finalHeaderFilesDirectives.add(e);
                }
                createHeaderArchive(finalHeaderFilesDirectives);
            }
        } catch ( Exception e ) {
            throw new MojoExecutionException("Error while processing headers to include: " + e.getMessage(), e);
        }

    }

    private void createHeaderArchive(List<HeaderFilesDirective> finalHeaderFilesDirectives) throws MojoExecutionException {
        try {
            MavenArchiver mavenArchiver = new MavenArchiver();
            mavenArchiver.setArchiver(jarArchiver);

            final File jarFile = new File( new File(project.getBuild().getDirectory()), project.getBuild().getFinalName() +".har" );
            mavenArchiver.setOutputFile(jarFile);

            for ( HeaderFilesDirective headerFilesDirective : finalHeaderFilesDirectives ) {
                mavenArchiver.getArchiver().addDirectory( new File(headerFilesDirective.getDirectory()), headerFilesDirective.getIncludes(),headerFilesDirective.getExcludes() );
            }

            final MavenArchiveConfiguration mavenArchiveConfiguration = new MavenArchiveConfiguration();
            mavenArchiveConfiguration.setAddMavenDescriptor( false );

            mavenArchiver.createArchive( project, mavenArchiveConfiguration );
            projectHelper.attachArtifact( project, "har", jarFile );

        } catch ( Exception e ) {
            throw new MojoExecutionException( e.getMessage() );
        }
    }

    private Set<Artifact> findNativeLibraryDependencies() throws MojoExecutionException {
        NativeHelper nativeHelper = new NativeHelper( project, projectRepos, repoSession, repoSystem, artifactFactory, getLog() );
        final Set<Artifact> staticLibraryArtifacts = nativeHelper.getNativeDependenciesArtifacts(unpackedApkLibsDirectory, false);
        final Set<Artifact> sharedLibraryArtifacts = nativeHelper.getNativeDependenciesArtifacts(unpackedApkLibsDirectory, true);
        final Set<Artifact> mergedArtifacts = new LinkedHashSet<Artifact>(staticLibraryArtifacts);
        mergedArtifacts.addAll(sharedLibraryArtifacts);
        return mergedArtifacts;
    }

    /** Resolve the artifact type from the current project and the specified file.  If the project packaging is
     * either 'a' or 'so' it will use the packaging, otherwise it checks the file for the extension
     *
     * @param file The file being added as an artifact
     * @return The artifact type (so or a)
     */
    private String resolveArtifactType(File file) {
        if ("so".equals(project.getPackaging()) || "a".equals(project.getPackaging())) {
            return project.getPackaging();
        }
        else {
            // At this point, the file (as found by our filtering previously will end with either 'so' or 'a'
            return file.getName().endsWith("so") ? "so" : "a";
        }
    }

    /**
     * <p>Returns the Android NDK to use.</p>
     * <p/>
     * <p>Current implementation looks for <code>&lt;ndk&gt;&lt;path&gt;</code> configuration in pom, then System
     * property <code>android.ndk.path</code>, then environment variable <code>ANDROID_NDK_HOME</code>.
     * <p/>
     * <p>This is where we collect all logic for how to lookup where it is, and which one to choose. The lookup is
     * based on available parameters. This method should be the only one you should need to look at to understand how
     * the Android NDK is chosen, and from where on disk.</p>
     *
     * @return the Android NDK to use.
     * @throws org.apache.maven.plugin.MojoExecutionException
     *          if no Android NDK path configuration is available at all.
     */
    protected AndroidNdk getAndroidNdk() throws MojoExecutionException {
        File chosenNdkPath;

        if ( ndk != null ) {
            // An <ndk> tag exists in the pom.

            if ( ndk.getPath() != null ) {
                // An <ndk><path> tag is set in the pom.

                chosenNdkPath = ndk.getPath();
            } else {
                // There is no <ndk><path> tag in the pom.

                if ( ndkPath != null ) {
                    // -Dandroid.ndk.path is set on command line, or via <properties><ndk.path>...
                    chosenNdkPath = ndkPath;
                } else {
                    // No -Dandroid.ndk.path is set on command line, or via <properties><ndk.path>...
                    chosenNdkPath = new File( getAndroidNdkHomeOrThrow() );
                }
            }
        } else {
            // There is no <ndk> tag in the pom.
            if ( ndkPath != null ) {
                // -Dandroid.ndk.path is set on command line, or via <properties><ndk.path>...
                chosenNdkPath = ndkPath;
            } else {
                // No -Dandroid.ndk.path is set on command line, or via <properties><ndk.path>...
                chosenNdkPath = new File( getAndroidNdkHomeOrThrow() );
            }

        }

        return new AndroidNdk( chosenNdkPath );
    }


    private String getAndroidNdkHomeOrThrow() throws MojoExecutionException {
        final String androidHome = System.getenv( ENV_ANDROID_NDK_HOME );
        if ( isBlank( androidHome ) ) {
            throw new MojoExecutionException( "No Android NDK path could be found. You may configure it in the pom using <ndk><path>...</path></ndk> or <properties><ndk.path>...</ndk.path></properties> or on command-line using -Dandroid.ndk.path=... or by setting environment variable " + ENV_ANDROID_NDK_HOME );
        }
        return androidHome;
    }
}
