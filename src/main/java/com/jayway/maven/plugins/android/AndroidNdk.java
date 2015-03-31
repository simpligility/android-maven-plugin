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

import com.jayway.maven.plugins.android.configuration.NDKArchitectureToolchainMappings;
import com.jayway.maven.plugins.android.phase05compile.NdkBuildMojo;
import org.apache.commons.lang.SystemUtils;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents an Android NDK.
 *
 * @author Johan Lindquist <johanlindquist@gmail.com>
 * @author Manfred Moser <manfred@simpligility.com>
 */
public class AndroidNdk
{

    public static final String PROPER_NDK_HOME_DIRECTORY_MESSAGE = "Please provide a proper Android NDK directory path"
            + " as configuration parameter <ndk><path>...</path></ndk> in the plugin <configuration/>. As an "
            + "alternative, you may add the parameter to commandline: -Dandroid.ndk.path=... or set environment "
            + "variable " + NdkBuildMojo.ENV_ANDROID_NDK_HOME + ".";

    public static final String[] NDK_ARCHITECTURES = { "armeabi", "armeabi-v7a", "mips", "x86" };

    /**
     * Arm toolchain implementations.
     */
    public static final String[] ARM_TOOLCHAIN = {  "arm-linux-androideabi-4.9", "arm-linux-androideabi-4.8",
            "arm-linux-androideabi-4.7", "arm-linux-androideabi-4.6", "arm-linux-androideabi-4.4.3" };

    /**
     * Arm64 toolchain implementations.
     */
    public static final String[] ARM64_TOOLCHAIN = {  "aarch64-linux-android-4.9" };

    /**
     * x86 toolchain implementations.
     */
    public static final String[] X86_TOOLCHAIN = { "x86-4.9", "x86-4.8", "x86-4.7", "x86-4.6", "x86-4.4.3" };

    /**
     * Mips toolchain implementations.
     */
    public static final String[] MIPS_TOOLCHAIN = { "mipsel-linux-android-4.9", "mipsel-linux-android-4.8",
            "mipsel-linux-android-4.7", "mipsel-linux-android-4.6", "mipsel-linux-android-4.4.3" };

    /**
     * Possible locations for the gdbserver file.
     */
    private static final String[] GDB_SERVER_LOCATIONS = { "toolchains/%s/prebuilt/gdbserver",
                                                           "prebuilt/%s/gdbserver/gdbserver" };

    private final File ndkPath;

    public AndroidNdk( File ndkPath )
    {
        assertPathIsDirectory( ndkPath );
        this.ndkPath = ndkPath;
    }

    private void assertPathIsDirectory( final File path )
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

    private File findStripper( String toolchain )
    {
        List<String> osDirectories = new ArrayList<String>();
        String extension = "";

        if ( SystemUtils.IS_OS_LINUX )
        {
            osDirectories.add( "linux-x86" );
            osDirectories.add( "linux-x86_64" );
        }
        else if ( SystemUtils.IS_OS_WINDOWS )
        {
            osDirectories.add( "windows" );
            osDirectories.add( "windows-x86_64" );
            extension = ".exe";
        }
        else if ( SystemUtils.IS_OS_MAC || SystemUtils.IS_OS_MAC_OSX )
        {
            osDirectories.add( "darwin-x86" );
            osDirectories.add( "darwin-x86_64" );
        }

        String fileName = "";
        if ( toolchain.startsWith( "arm" ) )
        {
            fileName = "arm-linux-androideabi-strip" + extension;
        }
        else if ( toolchain.startsWith( "aarch64" ) )
        {
            fileName = "aarch64-linux-android-strip" + extension;
        }
        else if ( toolchain.startsWith( "x86" ) )
        {
            fileName = "i686-linux-android-strip" + extension;
        }
        else if ( toolchain.startsWith( "mips" ) )
        {
            fileName = "mipsel-linux-android-strip" + extension;
        }

        for ( String osDirectory : osDirectories )
        {
            String stripperLocation =
                String.format( "toolchains/%s/prebuilt/%s/bin/%s", toolchain, osDirectory, fileName );
            final File stripper = new File( ndkPath, stripperLocation );
            if ( stripper.exists() )
            {
                return stripper;
            }
        }
        return null;
    }

    public File getStripper( String toolchain ) throws MojoExecutionException
    {
        final File stripper = findStripper( toolchain );
        if ( stripper == null )
        {
            throw new MojoExecutionException( "Could not resolve stripper for current OS: " + SystemUtils.OS_NAME );
        }

        // Some basic validation
        if ( ! stripper.exists() )
        {
            throw new MojoExecutionException( "Strip binary " + stripper.getAbsolutePath()
                    + " does not exist, please double check the toolchain and OS used" );
        }

        // We should be good to go
        return stripper;
    }

    private String resolveNdkToolchain( String[] toolchains )
    {
        for ( String toolchain : toolchains )
        {
            File f = findStripper( toolchain );
            if ( f != null && f.exists() )
            {
                return toolchain;
            }
        }
        return null;
    }

    /**
     * Tries to resolve the toolchain based on the path of the file.
     *
     * @param file Native library
     * @return String
     * @throws MojoExecutionException When no toolchain is found
     */
    public String getToolchain( File file ) throws MojoExecutionException
    {
        String resolvedNdkToolchain = null;

        // try to resolve the toolchain now
        String ndkArchitecture = file.getParentFile().getName();
        if ( ndkArchitecture.startsWith( "armeabi" ) )
        {
            resolvedNdkToolchain = resolveNdkToolchain( ARM_TOOLCHAIN );
        }
        else if ( ndkArchitecture.startsWith( "arm64" ) )
        {
            resolvedNdkToolchain = resolveNdkToolchain( ARM64_TOOLCHAIN );
        }
        else if ( ndkArchitecture.startsWith( "x86" ) )
        {
            resolvedNdkToolchain = resolveNdkToolchain( X86_TOOLCHAIN );
        }
        else if ( ndkArchitecture.startsWith( "mips" ) )
        {
            resolvedNdkToolchain = resolveNdkToolchain( MIPS_TOOLCHAIN );
        }

        // if no toolchain can be found
        if ( resolvedNdkToolchain == null )
        {
            throw new MojoExecutionException(
                "Can not resolve automatically a toolchain to use. Please specify one." );
        }
        return resolvedNdkToolchain;
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
        }
        else
        {
            return new File( ndkPath, "/ndk-build" ).getAbsolutePath();
        }
    }

    public File getGdbServer( String ndkArchitecture ) throws MojoExecutionException
    {
        // create a list of possible gdb server parent folder locations
        List<String> gdbServerLocations = new ArrayList<String>();
        if ( ndkArchitecture.startsWith( "armeabi" ) )
        {
            gdbServerLocations.add( "android-armeabi" );
            gdbServerLocations.addAll( Arrays.asList( ARM_TOOLCHAIN ) );
        }
        else if ( ndkArchitecture.startsWith( "arm64" ) )
        {
            gdbServerLocations.add( "android-arm64" );
            gdbServerLocations.addAll( Arrays.asList( ARM64_TOOLCHAIN ) );
        }
        else if ( ndkArchitecture.startsWith( "x86" ) )
        {
            gdbServerLocations.add( "android-x86" );
            gdbServerLocations.addAll( Arrays.asList( X86_TOOLCHAIN ) );
        }
        else if ( ndkArchitecture.startsWith( "mips" ) )
        {
            gdbServerLocations.add( "android-mips" );
            gdbServerLocations.addAll( Arrays.asList( MIPS_TOOLCHAIN ) );
        }

        // check for the gdb server
        for ( String location : GDB_SERVER_LOCATIONS )
        {
            for ( String gdbServerLocation : gdbServerLocations )
            {
                File gdbServerFile = new File( ndkPath, String.format( location, gdbServerLocation ) );
                if ( gdbServerFile.exists() )
                {
                    return gdbServerFile;
                }
            }
        }

        //  if we got here, throw an error
        throw new MojoExecutionException( "gdbserver binary for architecture " + ndkArchitecture
            + " does not exist, please double check the toolchain and OS used" );
    }

    /** Retrieves, based on the architecture and possibly toolchain mappings, the toolchain for the architecture.
     * <br/>
     * <strong>Note:</strong> This method will return the <strong>default</strong> toolchain as defined by the NDK if
     * not specified in the <code>NDKArchitectureToolchainMappings</code>.
     *
     * @param ndkArchitecture Architecture to resolve toolchain for
     * @param ndkArchitectureToolchainMappings User mappings of architecture to toolchain
     *
     * @return Toolchain to be used for the architecture
     *
     * @throws MojoExecutionException If a toolchain can not be resolved
     */
    public String getToolchainFromArchitecture( final String ndkArchitecture,
                                                final NDKArchitectureToolchainMappings ndkArchitectureToolchainMappings
    ) throws MojoExecutionException
    {
        if ( ndkArchitecture.startsWith( "armeabi" ) )
        {
            if ( ndkArchitectureToolchainMappings != null )
            {
                return ndkArchitectureToolchainMappings.getArmeabi();
            }
            return AndroidNdk.ARM_TOOLCHAIN[0];
        }
        else if ( ndkArchitecture.startsWith( "arm64" ) )
        {
            if ( ndkArchitectureToolchainMappings != null )
            {
                return ndkArchitectureToolchainMappings.getArm64();
            }
            return AndroidNdk.ARM64_TOOLCHAIN[0];
        }
        else if ( ndkArchitecture.startsWith( "x86" ) )
        {
            if ( ndkArchitectureToolchainMappings != null )
            {
                return ndkArchitectureToolchainMappings.getX86();
            }
            return AndroidNdk.X86_TOOLCHAIN[0];
        }
        else if ( ndkArchitecture.startsWith( "mips" ) )
        {
            if ( ndkArchitectureToolchainMappings != null )
            {
                return ndkArchitectureToolchainMappings.getMips();
            }
            return AndroidNdk.MIPS_TOOLCHAIN[0];
        }

        //  if we got here, throw an error
        throw new MojoExecutionException( "Toolchain for architecture " + ndkArchitecture
                + " does not exist, please double check the setup" );
    }
}

