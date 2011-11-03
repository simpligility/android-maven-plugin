package com.jayway.maven.plugins.android.phase04processclasses;

import com.jayway.maven.plugins.android.AbstractAndroidMojo;
import com.jayway.maven.plugins.android.CommandExecutor;
import com.jayway.maven.plugins.android.ExecutionException;
import com.jayway.maven.plugins.android.common.AndroidExtension;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Proguard mojo
 *
 * @goal proguard
 * @phase process-classes
 * @requiresDependencyResolution compile
 */
public class ProguardMojo extends AbstractAndroidMojo {

    private String[] jvmArguments = new String[] {"Xmx256m"};

    public void execute() throws MojoExecutionException, MojoFailureException {

        if (isProguardEnabled()) {
            executeProguard();
        }
    }

    private void executeProguard() throws MojoExecutionException {

        String proguardJar = getAndroidSdk().getPathForTool("proguard/lib/proguard.jar");

        File proguardDir = new File(project.getBuild().getDirectory(), "proguard");
        if (!proguardDir.exists() && !proguardDir.mkdir()) {
            throw new MojoExecutionException("Cannot create proguard output directory");
        } else if (proguardDir.exists() && !proguardDir.isDirectory()) {
            throw new MojoExecutionException("Non-directory exists at " + proguardDir.getAbsolutePath());
        }

        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger(this.getLog());
        List<String> commands = new ArrayList<String>();

        if (jvmArguments != null) {
            for (String jvmArgument : jvmArguments) {
                 // preserve backward compatibility allowing argument with or without dash (e.g. Xmx512m as well as
                 // -Xmx512m should work) (see http://code.google.com/p/maven-android-plugin/issues/detail?id=153)
                 if (!jvmArgument.startsWith("-")) {
                        jvmArgument = "-" + jvmArgument;
                 }
                commands.add(jvmArgument);
            }
        }
        commands.add("-jar");
        commands.add(proguardJar);

        commands.add("@" + proguard.getConfig());

        for (File file : getInputFiles()) {
            // don't add android packaging files, these are not input to proguard
            if (!AndroidExtension.isAndroidPackaging(FileUtils.extension(file.getAbsolutePath()))) {
                commands.add("-injars");
                commands.add(file.getAbsolutePath());
            }
        }

        commands.add("-outjars");
        commands.add(project.getBuild().getDirectory() + "/obfuscated.jar");

        commands.add("-libraryjars");
        commands.add(getAndroidSdk().getAndroidJar().getAbsolutePath());

        commands.add("-dump");
        commands.add(proguardDir + File.separator + "dump.txt");
        commands.add("-printseeds");
        commands.add(proguardDir + File.separator + "seeds.txt");
        commands.add("-printusage");
        commands.add(proguardDir + File.separator + "usage.txt");
        commands.add("-printmapping");
        commands.add(proguardDir + File.separator + "mapping.txt");

        final String javaExecutable = getJavaExecutable().getAbsolutePath();
        getLog().info(javaExecutable + " " + commands.toString());
        try {
            executor.executeCommand(javaExecutable, commands, project.getBasedir(), false);
        } catch (ExecutionException e) {
            throw new MojoExecutionException("", e);
        }
    }

    /**
     * Figure out the full path to the current java executable.
     *
     * @return the full path to the current java executable.
     */
    private static File getJavaExecutable() {
        final String javaHome = System.getProperty("java.home");
        final String slash = File.separator;
        return new File(javaHome + slash + "bin" + slash + "java");
    }


    private Set<File> getInputFiles() {

        Set<File> inputs = new HashSet<File>();

        inputs.add(new File(project.getBuild().getOutputDirectory()));
        for (Artifact artifact : getAllRelevantDependencyArtifacts()) {
            inputs.add(artifact.getFile().getAbsoluteFile());
        }

        return inputs;
    }




}
