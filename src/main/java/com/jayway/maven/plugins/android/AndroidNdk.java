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
package com.jayway.maven.plugins.android;

import com.jayway.maven.plugins.android.phase05compile.NdkBuildMojo;
import org.apache.commons.lang.SystemUtils;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;

/**
 * Represents an Android NDK.
 *
 * @author Johan Lindquist <johanlindquist@gmail.com>
 */
public class AndroidNdk
{

    public static final String PROPER_NDK_HOME_DIRECTORY_MESSAGE =
            "Please provide a proper Android NDK directory path as configuration parameter <ndk><path>...</path></ndk> in the plugin <configuration/>. As an alternative, you may add the parameter to commandline: -Dandroid.ndk.path=... or set environment variable " +
                    NdkBuildMojo.ENV_ANDROID_NDK_HOME + ".";

    private final File ndkPath;

    public AndroidNdk(File ndkPath)
    {
        assertPathIsDirectory( ndkPath );
        this.ndkPath = ndkPath;
    }

    private void assertPathIsDirectory(final File path)
    {
        if ( path == null )
        {
            throw new InvalidNdkException( PROPER_NDK_HOME_DIRECTORY_MESSAGE );
        }
        if ( ! path.isDirectory() )
        {
            throw new InvalidNdkException(
                    "Path \"" + path + "\" is not a directory. " + PROPER_NDK_HOME_DIRECTORY_MESSAGE );
        }
    }

    public File getStripper(String toolchain) throws MojoExecutionException
    {
        final File stripper;
        if ( SystemUtils.IS_OS_LINUX )
        {
            stripper = new File( ndkPath,
                    "toolchains/" + toolchain + "/prebuilt/linux-x86/bin/arm-linux-androideabi-strip" );
        } else if ( SystemUtils.IS_OS_WINDOWS )
        {
            stripper = new File( ndkPath,
                    "toolchains/" + toolchain + "/prebuilt/windows/bin/arm-linux-androideabi-strip.exe" );
        } else if ( SystemUtils.IS_OS_MAC || SystemUtils.IS_OS_MAC_OSX )
        {
            stripper = new File( ndkPath,
                    "toolchains/" + toolchain + "/prebuilt/darwin-x86/bin/arm-linux-androideabi-strip" );
        } else
        {
            throw new MojoExecutionException( "Could not resolve stripper for current OS: " + SystemUtils.OS_NAME );
        }

        // Some basic validation
        if ( ! stripper.exists() )
        {
            throw new MojoExecutionException( "Strip binary " + stripper.getAbsolutePath() +
                    " does not exist, please double check the toolchain and OS used" );
        }

        // We should be good to go
        return stripper;
    }

    /**
     * Returns the complete path for the ndk-build tool, based on this NDK.
     *
     * @return the complete path as a <code>String</code>, including the tool's filename.
     */
    public String getNdkBuildPath()
    {
        if ( SystemUtils.IS_OS_WINDOWS )
        {
            return new File( ndkPath, "/ndk-build.cmd" ).getAbsolutePath();
        } else
        {
            return new File( ndkPath, "/ndk-build" ).getAbsolutePath();
        }
    }


    public File getGdbServer(String toolchain) throws MojoExecutionException
    {
        final File gdbServerFile;

        gdbServerFile = new File( ndkPath, "toolchains/" + toolchain + "/prebuilt/gdbserver" );

        // Some basic validation
        if ( ! gdbServerFile.exists() )
        {
            throw new MojoExecutionException( "gdbserver binary " + gdbServerFile.getAbsolutePath() +
                    " does not exist, please double check the toolchain and OS used" );
        }

        // We should be good to go
        return gdbServerFile;
    }
}