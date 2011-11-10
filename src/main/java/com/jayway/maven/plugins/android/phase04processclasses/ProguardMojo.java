package com.jayway.maven.plugins.android.phase04processclasses;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.FileUtils;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.artifact.JavaScopes;

import com.jayway.maven.plugins.android.AbstractAndroidMojo;
import com.jayway.maven.plugins.android.CommandExecutor;
import com.jayway.maven.plugins.android.ExecutionException;
import com.jayway.maven.plugins.android.common.AndroidExtension;

/**
 * Proguard mojo
 * 
 * @goal proguard
 * @phase process-classes
 * @requiresDependencyResolution compile
 */
public class ProguardMojo extends AbstractAndroidMojo {

    public static final String PROGUARD_OBFUSCATED_JAR = "proguard-obfuscated.jar";

    private static final String ANDROID_LIBRARY_FILTER = "!org/xml/**,!org/w3c/**,!org/apache/http/**,!java/**,!javax/**,!android/net/http/AndroidHttpClient.class";

    private String[] jvmArguments = new String[] { "Xmx256m" };

    private List<Artifact> artifactBlacklist = new LinkedList<Artifact>();
    private List<Artifact> artifactsToShift = new LinkedList<Artifact>();

    private List<ProGuardInput> inJars = new LinkedList<ProguardMojo.ProGuardInput>();
    private List<ProGuardInput> libraryJars = new LinkedList<ProguardMojo.ProGuardInput>();

    private static class ProGuardInput {
        private String path;
        private String filterExpression;

        public ProGuardInput(String path, String filterExpression) {
            this.path = path;
            this.filterExpression = filterExpression;
        }

        public String toCommandLine() {
            if (filterExpression != null) {
                return path + "(" + filterExpression + ")";
            }
            return path;
        }
    }

    public void execute() throws MojoExecutionException, MojoFailureException {

        if (isProguardEnabled()) {
            executeProguard();
        }
    }

    private void executeProguard() throws MojoExecutionException {

        // we should make this configurable, users may want to use a newer (or diff) version of
        // proguard
        String proguardJar = getAndroidSdk().getPathForTool("proguard/lib/proguard.jar");

        File proguardDir = new File(project.getBuild().getDirectory(), "proguard");
        if (!proguardDir.exists() && !proguardDir.mkdir()) {
            throw new MojoExecutionException("Cannot create proguard output directory");
        } else if (proguardDir.exists() && !proguardDir.isDirectory()) {
            throw new MojoExecutionException("Non-directory exists at "
                    + proguardDir.getAbsolutePath());
        }

        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger(this.getLog());
        List<String> commands = new ArrayList<String>();

        collectJvmArguments(commands);

        commands.add("-jar");
        commands.add(proguardJar);

        commands.add("@" + proguard.getConfig());

        collectInputFiles(commands);

        commands.add("-outjars");
        commands.add(project.getBuild().getDirectory() + File.separator + PROGUARD_OBFUSCATED_JAR);

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

    private void collectJvmArguments(List<String> commands) {
        if (jvmArguments != null) {
            for (String jvmArgument : jvmArguments) {
                // preserve backward compatibility allowing argument with or without dash (e.g.
                // Xmx512m as well as -Xmx512m should work) (see
                // http://code.google.com/p/maven-android-plugin/issues/detail?id=153)
                if (!jvmArgument.startsWith("-")) {
                    jvmArgument = "-" + jvmArgument;
                }
                commands.add(jvmArgument);
            }
        }
    }

    private void collectInputFiles(List<String> commands) {
        // commons-logging breaks everything horribly, so we skip it from the program
        // dependencies and declare it to be a library dependency instead
        skipArtifact("commons-logging", "commons-logging", true);

        collectProgramInputFiles();
        for (ProGuardInput injar : inJars) {
            // don't add android packaging files, these are not input to proguard
            if (!AndroidExtension.isAndroidPackaging(FileUtils.extension(injar.path))) {
                commands.add("-injars");
                commands.add(injar.toCommandLine());
            }
        }

        collectLibraryInputFiles();
        for (ProGuardInput libraryjar : libraryJars) {
            commands.add("-libraryjars");
            commands.add(libraryjar.toCommandLine());
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

    private void skipArtifact(String groupId, String artifactId, boolean shiftToLibraries) {
        artifactBlacklist.add(RepositoryUtils.toArtifact(new DefaultArtifact(groupId, artifactId,
                null, null)));
        if (shiftToLibraries) {
            artifactsToShift.add(RepositoryUtils.toArtifact(new DefaultArtifact(groupId,
                    artifactId, null, null)));
        }
    }

    private boolean isBlacklistedArtifact(Artifact artifact) {
        for (Artifact artifactToSkip : artifactBlacklist) {
            if (artifactToSkip.getGroupId().equals(artifact.getGroupId())
                    && artifactToSkip.getArtifactId().equals(artifact.getArtifactId())) {
                return true;
            }
        }
        return false;
    }

    private boolean isShiftedArtifact(Artifact artifact) {
        for (Artifact artifactToShift : artifactsToShift) {
            if (artifactToShift.getGroupId().equals(artifact.getGroupId())
                    && artifactToShift.getArtifactId().equals(artifact.getArtifactId())) {
                return true;
            }
        }
        return false;
    }

    private void collectProgramInputFiles() {
        // we first add the application's own class files
        addInJar(project.getBuild().getOutputDirectory());

        // we then add all its dependencies (incl. transitive ones), unless they're blacklisted
        for (Artifact artifact : getAllRelevantDependencyArtifacts()) {
            if (isBlacklistedArtifact(artifact)) {
                continue;
            }
            addInJar(artifact.getFile().getAbsolutePath());
        }
    }

    private void addInJar(String path, String filterExpression) {
        inJars.add(new ProGuardInput(path, filterExpression));
    }

    private void addInJar(String path) {
        addInJar(path, null);
    }

    private void addLibraryJar(String path, String filterExpression) {
        libraryJars.add(new ProGuardInput(path, filterExpression));
    }

    private void addLibraryJar(String path) {
        addLibraryJar(path, null);
    }

    private void collectLibraryInputFiles() {
        // we have to add the Java framework classes to the library JARs, since they are not
        // distributed with the JAR on Central, and since we'll strip them out of the android.jar
        // that is shipped with the SDK (since that is not a complete Java distribution)
        String javaHome = System.getProperty("java.home");
        String jdkLibsPath = null;
        if (javaHome.startsWith("/System/Library/Java")) {
            // MacOS X uses different naming conventions for JDK installations
            jdkLibsPath = javaHome + "/../Classes";
            addLibraryJar(jdkLibsPath + "/classes.jar");
        } else {
            jdkLibsPath = javaHome + "/lib";
            addLibraryJar(jdkLibsPath + "/rt.jar");
        }
        // we also need to add the JAR containing e.g. javax.servlet
        addLibraryJar(jdkLibsPath + "/jsse.jar");
        // and the javax.crypto stuff
        addLibraryJar(jdkLibsPath + "/jce.jar");

        // we treat any dependencies with provided scope as library JARs
        for (Artifact artifact : project.getArtifacts()) {
            if (artifact.getScope().equals(JavaScopes.PROVIDED)) {
                if (artifact.getArtifactId().equals("android")) {
                    addLibraryJar(artifact.getFile().getAbsolutePath(), ANDROID_LIBRARY_FILTER);
                } else {
                    addLibraryJar(artifact.getFile().getAbsolutePath());
                }
            } else if (isShiftedArtifact(artifact)) {
                // this is a blacklisted artifact that should be processed as a library instead
                addLibraryJar(artifact.getFile().getAbsolutePath());
            }
        }
    }
}
