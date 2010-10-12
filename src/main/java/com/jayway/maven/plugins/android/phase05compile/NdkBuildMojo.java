package com.jayway.maven.plugins.android.phase05compile;

import java.io.*;
import java.util.*;

import com.jayway.maven.plugins.android.*;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.*;

/**
 * @author Johan Lindquist <johanlindquist@gmail.com>
 * @goal ndk-build
 * @phase compile
 * @requiresProject true
 */
public class NdkBuildMojo extends AbstractAndroidMojo
{
    /**
     * Specifies the classifer with which the artifact should be stored in the repository
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
     * Flag indicating whether the output directory (libs/armeabi) should be cleared after build.  This will essentially
     * 'move' all the native artifacts (.so) to the ${protect.build.directory}/ndk-libs/armeabi.  When the APK is built,
     * the libraries will be included from here.
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

    public void execute() throws MojoExecutionException, MojoFailureException
    {
        File nativeLibDirectory = new File(project.getBasedir(), "libs/" + ndkArchitecture);
        boolean libsDirectoryExists = nativeLibDirectory.exists();

        File directoryToRemove = nativeLibDirectory;

        if (!libsDirectoryExists)
        {
            getLog().info("Creating native output directory " + nativeLibDirectory);

            if (nativeLibDirectory.getParentFile().exists())
            {
                nativeLibDirectory.mkdir();
            }
            else
            {
                nativeLibDirectory.mkdirs();
                directoryToRemove = nativeLibDirectory.getParentFile();
            }

        }

        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger(this.getLog());

        List<String> commands = new ArrayList<String>();
        commands.add("-C");
        commands.add(project.getBasedir().getAbsolutePath());

        if (ndkBuildAdditionalCommandline != null)
        {
            String[] additionalCommands = ndkBuildAdditionalCommandline.split(" ");
            for (final String command : additionalCommands)
            {
                commands.add(command);
            }
        }

        final String ndkBuildPath = getAndroidNdk().getNdkBuildPath();
        getLog().info(ndkBuildPath + " " + commands.toString());

        try
        {
            executor.executeCommand(ndkBuildPath, commands, project.getBasedir(), true);
        }
        catch (ExecutionException e)
        {
            throw new MojoExecutionException(e.getMessage(), e);
        }

        // Cleanup libs/armeabi directory if needed - this implies moving any native artifacts into target/libs
        if (clearNativeArtifacts)
        {
            File destinationDirectory = new File(ndkOutputDirectory.getAbsolutePath(), "/" + ndkArchitecture);

            try
            {
                if (!libsDirectoryExists)
                {
                    FileUtils.moveDirectory(nativeLibDirectory, destinationDirectory);
                }
                else
                {
                    FileUtils.copyDirectory(nativeLibDirectory, destinationDirectory);
                    FileUtils.cleanDirectory(nativeLibDirectory);
                }

                nativeLibDirectory = destinationDirectory;

            }
            catch (IOException e)
            {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }

        if (!libsDirectoryExists)
        {
            getLog().info("Cleaning up native library output directory after build");
            if (!directoryToRemove.delete())
            {
                getLog().warn("Could not remove directory, marking as delete on exit");
                directoryToRemove.deleteOnExit();
            }
        }


        if (attachNativeArtifacts)
        {
            File[] files = nativeLibDirectory.listFiles(new FilenameFilter()
            {
                public boolean accept(final File dir, final String name)
                {
                    return name.endsWith(".so");
                }
            });

            // slight limitation at this stage - we only handle a single .so artifact
            if (files == null || files.length > 1)
            {
                getLog().warn("Error while detecting native compile artifacts: " + (files == null ? "None found" : "Found more than 1 artifact"));
                if (files != null)
                {
                    getLog().warn("Currently, only a single, final native library is supported by the build");
                }
            }
            else
            {
                if (attachNativeArtifacts)
                {
                    getLog().debug("Adding native compile artifact: " + files[0]);
                    projectHelper.attachArtifact(this.project, "so", ndkClassifier, files[0]);
                }
            }

        }

    }
}
