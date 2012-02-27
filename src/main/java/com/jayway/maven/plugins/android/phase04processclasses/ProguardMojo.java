package com.jayway.maven.plugins.android.phase04processclasses;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.jayway.maven.plugins.android.configuration.Proguard;
import org.apache.commons.lang.StringUtils;
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
 * Processes both application and dependency classes using the ProGuard byte code obfuscator,
 * minimzer, and optimizer. For more information, see https://proguard.sourceforge.net.
 *
 * @author Jonson
 * @author Matthias Kaeppler
 * @author Manfred Moser
 *
 * @goal proguard
 * @phase process-classes
 * @requiresDependencyResolution compile
 */
public class ProguardMojo extends AbstractAndroidMojo {

    /**
     * <p>
     * Enables ProGuard for this build. ProGuard is disabled by default, so in order for it to run,
     * enable it like so:
     * </p>
     *
     * <pre>
     * &lt;proguard&gt;
     *    &lt;skip&gt;false&lt;/skip&gt;
     *    &lt;config&gt;proguard.cfg&lt;/config&gt;
     *    &lt;proguardJarPath&gt;someAbsolutePathToProguardJar&lt;/proguardJarPath&gt;
     *    &lt;filterMavenDescriptor&gt;true|false&lt;/filterMavenDescriptor&gt;
     *    &lt;filterManifest&gt;true|false&lt;/filterManifest&gt;
     *    &lt;jvmArguments&gt;
     *     &lt;jvmArgument&gt;-Xms256m&lt;/jvmArgument&gt;
     *     &lt;jvmArgument&gt;-Xmx512m&lt;/jvmArgument&gt;
     *   &lt;/jvmArguments&gt;
     * &lt;/proguard&gt;
     * </pre>
     * <p>
     * A good practice is to create a release profile in your POM, in which you enable ProGuard.
     * ProGuard should be disabled for development builds, since it obfuscates class and field
     * names, and since it may interfere with test projects that rely on your application classes.
     * </p>
     *
     * @parameter
     */
    protected Proguard proguard;

    /**
     * Whether ProGuard is enabled or not.
     *
     * @parameter  expression="${android.proguard.skip}" default-value=true
     * @optional
     */
    private Boolean proguardSkip;

    /**
     * Path to the ProGuard configuration file (relative to project root).
     *
     * @parameter expression="${android.proguard.config}" default-value="proguard.cfg"
     * @optional
     */
    private String proguardConfig;

    /**
     * Path to the proguard jar to be used. By default this will load the jar from the Android SDK install. Overriding it
     * with an absolute path allows you to use a newer or custom proguard version e.g. located in your local Maven repo.
     *
     * @parameter expression="${android.proguard.proguardJarPath}
     * @optional
     */
    private String proguardProguardJarPath;

    /**
     * Extra JVM Arguments. Using these you can e.g. increase memory for the jvm running the build.
     *
     * @parameter expression="${android.proguard.jvmArguments}" default-value="-Xmx512M"
     * @optional
     */
    private String[] proguardJvmArguments;

    /**
     * If set to true will add a filter to remove META-INF/maven/* files.
     * 
     * @parameter expression="${android.proguard.filterMavenDescriptor}"
     *            default-value="false"
     * @optional
     * TODO remove init?
     */
    private Boolean proguardFilterMavenDescriptor = false;

    /**
     * If set to true will add a filter to remove META-INF/MANIFEST.MF files.
     * 
     * @parameter expression="${android.proguard.filterManifest}"
     *            default-value="false"
     * @optional
     * ODO remove init?
     * 
     */
    private Boolean proguardFilterManifest = false;

    /**
     * The plugin dependencies.
     * 
     * @parameter expression="${plugin.artifacts}"
     * @required
     * @readonly
     */
    protected List<Artifact> pluginDependencies;

    private Boolean parsedSkip;
    private String parsedConfig;
    private String parsedProguardJarPath;
    private String[] parsedJvmArguments;
    private Boolean parsedFilterMavenDescriptor;
    private Boolean parsedFilterManifest;

    public static final String PROGUARD_OBFUSCATED_JAR = "proguard-obfuscated.jar";

    private static final Collection<String> ANDROID_LIBRARY_EXCLUDED_FILTER = Arrays.asList(
            "org/xml/**", "org/w3c/**", "org/apache/http/**", "java/**", "javax/**",
            "android/net/http/AndroidHttpClient.class");

    private static final Collection<String> MAVEN_DESCRIPTOR = Arrays.asList("META-INF/maven/**");
    private static final Collection<String> META_INF_MANIFEST = Arrays
            .asList("META-INF/MANIFEST.MF");

    private Collection<String> globalInJarExcludes = new HashSet<String>();

    private List<Artifact> artifactBlacklist = new LinkedList<Artifact>();
    private List<Artifact> artifactsToShift = new LinkedList<Artifact>();

    private List<ProGuardInput> inJars = new LinkedList<ProguardMojo.ProGuardInput>();
    private List<ProGuardInput> libraryJars = new LinkedList<ProguardMojo.ProGuardInput>();

    private static class ProGuardInput {

        private String path;
        private Collection<String> excludedFilter;

        public ProGuardInput(String path, Collection<String> excludedFilter) {
            this.path = path;
            this.excludedFilter = excludedFilter;
        }

        public String toCommandLine() {
            if (excludedFilter != null && !excludedFilter.isEmpty()) {
                StringBuilder sb = new StringBuilder(path);
                sb.append('(');
                for (Iterator<String> it = excludedFilter.iterator(); it.hasNext();) {
                    sb.append('!').append(it.next());
                    if (it.hasNext())
                        sb.append(',');
                }
                sb.append(')');
                return sb.toString();
            } else
                return path;
        }
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        parseConfiguration();
        
        if (!parsedSkip) {
            executeProguard();
        }
    }

    private void parseConfiguration() throws MojoExecutionException {
        proguardProguardJarPath = getProguardJarPathFromDependencies();

        if (proguard != null) {
            // take setting from pom configuration
            if (proguard.isSkip() != null) {
                parsedSkip = proguard.isSkip();
            }
            // but allow -D property from command line or settings or profile property or so to override
            if (proguardSkip != null) {
                parsedSkip = proguardSkip;
            }

            if (StringUtils.isNotEmpty(proguard.getConfig())) {
                parsedConfig = proguard.getConfig();
            }
            if (StringUtils.isNotEmpty(proguardConfig)) {
                parsedConfig = proguardConfig;
            }

            if (StringUtils.isNotEmpty(proguard.getProguardJarPath())) {
                parsedProguardJarPath = proguard.getProguardJarPath();
            }
            if (StringUtils.isNotEmpty(proguardProguardJarPath)) {
                parsedProguardJarPath = proguardProguardJarPath;
            }

            if (proguard.getJvmArguments() == null) {
                parsedJvmArguments =  proguardJvmArguments;
            }
            if (else {
                parsedJvmArguments = proguard.getJvmArguments();
            }
            if (proguard.isFilterManifest() != null) {
                parsedFilterManifest = proguard.isFilterManifest();
            } else {
                parsedFilterManifest = proguardFilterManifest;
            }
            if (proguard.isFilterMavenDescriptor() != null) {
                parsedFilterMavenDescriptor = proguard.isFilterMavenDescriptor();
            } else {
                parsedFilterMavenDescriptor = proguardFilterMavenDescriptor;
            }
        } else {
            parsedSkip = proguardSkip;
            parsedConfig = proguardConfig;
            parsedProguardJarPath = proguardProguardJarPath;
            parsedJvmArguments = proguardJvmArguments;
            parsedFilterManifest = proguardFilterManifest;
            parsedFilterMavenDescriptor = proguardFilterMavenDescriptor;
        }

        // nothing was configured - set up defaults
        if (parsedSkip == null) {
            parsedSkip = true;
        }
        if (StringUtils.isEmpty(parsedProguardJarPath)) {
            parsedProguardJarPath = getAndroidSdk().getPathForTool("proguard/lib/proguard.jar");
        }

    }

    private void executeProguard() throws MojoExecutionException {

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
        commands.add(parsedProguardJarPath);

        commands.add("@" + parsedConfig);

        collectInputFiles(commands);

        commands.add("-outjars");
        commands.add("'" + project.getBuild().getDirectory() + File.separator + PROGUARD_OBFUSCATED_JAR + "'");

        commands.add("-dump");
        commands.add("'" + proguardDir + File.separator + "dump.txt'");
        commands.add("-printseeds");
        commands.add("'" + proguardDir + File.separator + "seeds.txt'");
        commands.add("-printusage");
        commands.add("'" + proguardDir + File.separator + "usage.txt'");
        commands.add("-printmapping");
        commands.add("'" + proguardDir + File.separator + "mapping.txt'");

        final String javaExecutable = getJavaExecutable().getAbsolutePath();
        getLog().info(javaExecutable + " " + commands.toString());
        try {
            executor.executeCommand(javaExecutable, commands, project.getBasedir(), false);
        } catch (ExecutionException e) {
            throw new MojoExecutionException("", e);
        }
    }

    private void collectJvmArguments(List<String> commands) {
        if (parsedJvmArguments != null) {
            for (String jvmArgument : parsedJvmArguments) {
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
        if (parsedFilterManifest) {
            globalInJarExcludes.addAll(META_INF_MANIFEST);
        }
        if (parsedFilterMavenDescriptor) {
            globalInJarExcludes.addAll(MAVEN_DESCRIPTOR);
        }

        // we first add the application's own class files
        addInJar(project.getBuild().getOutputDirectory());

        // we then add all its dependencies (incl. transitive ones), unless they're blacklisted
        for (Artifact artifact : getAllRelevantDependencyArtifacts()) {
            if (isBlacklistedArtifact(artifact)) {
                continue;
            }
            addInJar(artifact.getFile().getAbsolutePath(), globalInJarExcludes);
        }
    }

    private void addInJar(String path, Collection<String> filterExpression) {
        inJars.add(new ProGuardInput(path, filterExpression));
    }

    private void addInJar(String path) {
        addInJar(path, null);
    }

    private void addLibraryJar(String path, Collection<String> filterExpression) {
        libraryJars.add(new ProGuardInput(path, filterExpression));
    }

    private void addLibraryJar(String path) {
        addLibraryJar(path, null);
    }

    private void collectLibraryInputFiles() {
        final String slash = File.separator;
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
            jdkLibsPath = javaHome + slash + "lib";
            addLibraryJar(jdkLibsPath + slash + "rt.jar");
        }
        // we also need to add the JAR containing e.g. javax.servlet
        addLibraryJar(jdkLibsPath + slash + "jsse.jar");
        // and the javax.crypto stuff
        addLibraryJar(jdkLibsPath + slash + "jce.jar");

        // we treat any dependencies with provided scope as library JARs
        for (Artifact artifact : project.getArtifacts()) {
            if (artifact.getScope().equals(JavaScopes.PROVIDED)) {
                if (artifact.getArtifactId().equals("android")) {
                    addLibraryJar(artifact.getFile().getAbsolutePath(),
                            ANDROID_LIBRARY_EXCLUDED_FILTER);
                } else {
                    addLibraryJar(artifact.getFile().getAbsolutePath());
                }
            } else if (isShiftedArtifact(artifact)) {
                // this is a blacklisted artifact that should be processed as a library instead
                addLibraryJar(artifact.getFile().getAbsolutePath());
            }
        }
    }

    private String getProguardJarPathFromDependencies() throws MojoExecutionException {
        Artifact proguardArtifact = null;
        int proguardArtifactDistance = -1;
        for (Artifact artifact : pluginDependencies) {
            getLog().debug("pluginArtifact: " + artifact.getFile());
            if (("proguard".equals(artifact.getArtifactId()))
                    || ("proguard-base".equals(artifact.getArtifactId()))) {
                int distance = artifact.getDependencyTrail().size();
                getLog().debug("proguard DependencyTrail: " + distance);
                if (proguardArtifactDistance == -1) {
                    proguardArtifact = artifact;
                    proguardArtifactDistance = distance;
                } else if (distance < proguardArtifactDistance) {
                    proguardArtifact = artifact;
                    proguardArtifactDistance = distance;
                }
            }
        }
        if (proguardArtifact != null) {
            getLog().debug("proguardArtifact: " + proguardArtifact.getFile());
            return proguardArtifact.getFile().getAbsoluteFile().toString();
        } else
            return null;

    }

}
