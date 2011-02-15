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

import com.jayway.maven.plugins.android.*;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.*;

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

    /**
     * <p>Parameter designed to pick up <code>-Dandroid.ndk.path</code> in case there is no pom with an
     * <code>&lt;ndk&gt;</code> configuration tag.</p>
     * <p>Corresponds to {@link Ndk#path}.</p>
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

    /** <p>Folder containing native libraries compiled and linked by the NDK.</p>
     *
     * @parameter expression="${android.nativeLibrariesDirectory}" default-value="${project.basedir}/libs"
     */
    private File nativeLibrariesDirectory;

    /**
     * Defines the architecture for the NDK build
     *
     * @parameter expression="${android.ndk.build.architecture}" default="armeabi"
     */
    protected String ndkArchitecture = "armeabi";


    public void execute() throws MojoExecutionException, MojoFailureException {

        // This points 
        File nativeLibDirectory = new File( nativeLibrariesDirectory , ndkArchitecture );

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

        executor.setLogger( this.getLog() );

        final List<String> commands = new ArrayList<String>();

        commands.add( "-C" );
        commands.add( project.getBasedir().getAbsolutePath() );

        if ( ndkBuildAdditionalCommandline != null ) {
            String[] additionalCommands = ndkBuildAdditionalCommandline.split( " " );
            for ( final String command : additionalCommands ) {
                commands.add( command );
            }
        }

        final String ndkBuildPath = getAndroidNdk().getNdkBuildPath();
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
            if ( !directoryToRemove.delete() ) {
                getLog().warn( "Could not remove directory, marking as delete on exit" );
                directoryToRemove.deleteOnExit();
            }
        }

        // Attempt to attach the native library if the project is defined as a "pure" native Android library
        // (packaging is 'so') or if the plugin has been configured to attach the native library to the build
        if ( "so".equals(project.getPackaging()) || attachNativeArtifacts ) {

            File[] files = nativeLibDirectory.listFiles( new FilenameFilter() {
                public boolean accept( final File dir, final String name ) {
                    return name.endsWith( ".so" );
                }
            } );

            // slight limitation at this stage - we only handle a single .so artifact
            if ( files == null || files.length > 1 ) {
                getLog().warn( "Error while detecting native compile artifacts: " + ( files == null ? "None found" : "Found more than 1 artifact" ) );
                if ( files != null ) {
                    getLog().warn( "Currently, only a single, final native library is supported by the build" );
                }
            } else {
                getLog().debug( "Adding native compile artifact: " + files[ 0 ] );
                projectHelper.attachArtifact( this.project, "so", ( ndkClassifier != null ? ndkClassifier : ndkArchitecture ), files[ 0 ] );
            }

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
