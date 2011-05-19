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

import java.io.*;
import java.util.*;

import com.jayway.maven.plugins.android.phase05compile.NdkBuildMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Represents an Android NDK.
 *
 * @author Johan Lindquist <johanlindquist@gmail.com>
 */
public class AndroidNdk {
    
    private static final String PARAMETER_MESSAGE = "Please provide a proper Android NDK directory path as configuration parameter <ndk><path>...</path></ndk> in the plugin <configuration/>. As an alternative, you may add the parameter to commandline: -Dandroid.ndk.path=... or set environment variable " + NdkBuildMojo.ENV_ANDROID_NDK_HOME + ".";

    private final File ndkPath;

    public AndroidNdk( File ndkPath ) {
        assertPathIsDirectory( ndkPath );
        this.ndkPath = ndkPath;
    }

    private void assertPathIsDirectory( final File path ) {
        if ( path == null ) {
            throw new InvalidNdkException( PARAMETER_MESSAGE );
        }
        if ( !path.isDirectory() ) {
            throw new InvalidNdkException( "Path \"" + path + "\" is not a directory. " + PARAMETER_MESSAGE );
        }
    }

    /**
     * Returns the complete path for the ndk-build tool, based on this NDK.
     *
     * @return the complete path as a <code>String</code>, including the tool's filename.
     */
    public String getNdkBuildPath() {
        return new File( ndkPath, "/ndk-build" ).getAbsolutePath();
    }


}