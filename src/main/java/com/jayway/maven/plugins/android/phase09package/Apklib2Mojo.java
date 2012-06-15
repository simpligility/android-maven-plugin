/*
 * Copyright (C) 2009 Jayway AB
 * Copyright (C) 2007-2008 JVending Masa
 *
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
package com.jayway.maven.plugins.android.phase09package;

import com.jayway.maven.plugins.android.AbstractAndroidMojo;
import com.jayway.maven.plugins.android.CommandExecutor;
import com.jayway.maven.plugins.android.ExecutionException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static com.jayway.maven.plugins.android.common.AndroidExtension.APKLIB;
import static com.jayway.maven.plugins.android.common.AndroidExtension.APKLIB_PRECOMPILED;


/**
 * Creates the apklib2 file.<br/>
 *
 * @author vladimir.grachev@gmail.com
 * @goal apklib2
 * @phase package
 * @requiresDependencyResolution compile
 */
public class Apklib2Mojo extends AbstractAndroidMojo {

    /**
     * Directory containing the classes and resource files that should be packaged into the JAR.
     *
     * @parameter default-value="${project.build.outputDirectory}"
     * @required
     */
    private File classesDirectory;

    private static final String[] FAKE_BUILD_TARGETS = {"nodeps", "all", "clean", "debug", "release", "emma", "install", "uninstall", "help"};

    private static final FileFilter NOT_PNG = new FileFilter() {

        private static final String EXTENSION = ".png";
        @Override
        public boolean accept(File file) {
            if (file.getName().toLowerCase().endsWith(EXTENSION)) return false;
            return true;
        }
    };


    public void execute() throws MojoExecutionException, MojoFailureException {
        File outputFile = createPrecompiledApkLibraryFile();
        // Set the generated .apklib2 file as the main artifact (because the pom states <packaging>apklib</packaging>)
        project.getArtifact().setFile(outputFile);
    }

    protected File createPrecompiledApkLibraryFile() throws MojoExecutionException {
        final File apklibrary = new File(project.getBuild().getDirectory(), project.getBuild().getFinalName() + "." + APKLIB_PRECOMPILED);

        final File apklibRoot = new File(project.getBuild().getDirectory(), APKLIB_PRECOMPILED + "-dir");
        final File apklibBin = new File(apklibRoot, "bin");
        final File apklibRes = new File(apklibBin, "res");

        FileUtils.deleteQuietly(apklibrary);
        FileUtils.deleteQuietly(apklibRoot);

        try {

            if (!apklibRoot.exists()) {
                boolean mkdir = apklibRoot.mkdirs();
                if (!mkdir) {
                    throw new MojoExecutionException("Can't create "+apklibRoot.getAbsolutePath()+" directory.");
                }
            }

            FileUtils.copyDirectory(resourceDirectory, apklibRes, NOT_PNG);

            crunchResources(resourceDirectory, apklibRes);
            createClassesJar(apklibBin);
            File buildXml = createFakeBuildXml(apklibRoot);
            File projProps = createPropertiesFile(apklibRoot);

            ZipArchiver zipArchiver = new ZipArchiver();
            zipArchiver.setDestFile(apklibrary);

            zipArchiver.addFile(androidManifestFile, "AndroidManifest.xml");
            addDirectory(zipArchiver, assetsDirectory, "assets");
            addDirectory(zipArchiver, apklibBin, "bin");
            zipArchiver.addFile(projProps, "project.properties");
            zipArchiver.addFile(buildXml, "build.xml");

            // Lastly, add any native libraries
            addNativeLibraries(zipArchiver);

            zipArchiver.createArchive();
        } catch (ArchiverException e) {
            throw new MojoExecutionException("ArchiverException while creating ."+APKLIB_PRECOMPILED+" file.", e);
        } catch (IOException e) {
            throw new MojoExecutionException("IOException while creating ."+APKLIB_PRECOMPILED+" file.", e);
        }

        return apklibrary;
    }

    private void crunchResources(File srcDir, File outDir) throws MojoExecutionException {

        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger(this.getLog());

        List<String> commands = new ArrayList<String>();
        commands.add("crunch");
        commands.add("-S");
        commands.add(srcDir.getAbsolutePath());
        commands.add("-C");
        commands.add(outDir.getAbsolutePath());
        getLog().info(getAndroidSdk().getPathForTool("aapt") + " " + commands.toString());
        try {
            executor.executeCommand(getAndroidSdk().getPathForTool("aapt"), commands, project.getBasedir(), false);
        } catch (ExecutionException e) {
            throw new MojoExecutionException("", e);
        }

    }

    private File createPropertiesFile(File directory) throws IOException, MojoExecutionException {
        File projProps = new File(directory, "project.properties");
        BufferedWriter out = new BufferedWriter(new FileWriter(projProps));
        out.write("android.library=true");
        out.newLine();
        out.write("target=android-");
        out.write(getAndroidSdk().getApiLevel());
        out.newLine();
        out.close();
        return projProps;
    }


    private File createClassesJar(File destDir) throws MojoExecutionException {
        try {
            File result = new File(destDir, "classes.jar");
            JarArchiver jarArchiver = new JarArchiver();
            jarArchiver.setDestFile(result);

            addDirectory(jarArchiver, classesDirectory, new String[] {"**/R.class", "**/R$*.class"});

            jarArchiver.createArchive();
            return result;
        } catch (ArchiverException e) {
            throw new MojoExecutionException("ArchiverException while creating ."+APKLIB_PRECOMPILED+" file.", e);
        } catch (IOException e) {
            throw new MojoExecutionException("IOException while creating ."+APKLIB_PRECOMPILED+" file.", e);
        }
    }

    private File createFakeBuildXml(File directory) throws IOException {


        Xpp3Dom build = new Xpp3Dom("project");
        build.setAttribute("name", project.getName());

        for (String string : FAKE_BUILD_TARGETS) {
            Xpp3Dom target = new Xpp3Dom("target");
            target.setAttribute("name", string);
            build.addChild(target);
        }

        File result = new File(directory, "build.xml");
        BufferedWriter out = new BufferedWriter(new FileWriter(result));
        out.write(build.toString());
        out.close();
        return result;
    }

    private void addNativeLibraries(final ZipArchiver jarArchiver) throws MojoExecutionException {

        try {
            addDirectory(jarArchiver, nativeLibrariesDirectory, "libs");
        } catch (ArchiverException e) {
            throw new MojoExecutionException("IOException while creating ."+APKLIB+" file.", e);
        }
        // TODO: Next is to check for any:
        // TODO: - compiled in (as part of this build) libs
        // TODO:    - That is of course easy if the artifact is indeed attached
        // TODO:    - If not attached, it gets a little trickier  - check the target dir for any compiled .so files (generated by NDK mojo)
        // TODO:        - But where is that directory configured?
    }

    protected void addJavaResources(ZipArchiver zipArchiver, List<Resource> javaResources, String prefix) throws ArchiverException, IOException {
        for (Resource javaResource : javaResources) {
            addJavaResource(zipArchiver, javaResource, prefix);
        }
    }

    /**
     * Adds a Java Resources directory (typically "src/main/resources") to a {@link org.codehaus.plexus.archiver.zip.ZipArchiver}.
     *
     * @param zipArchiver
     * @param javaResource The Java resource to add.
     * @param prefix An optional prefix for where in the Jar file the directory's contents should go.
     * @throws org.codehaus.plexus.archiver.ArchiverException
     * @throws java.io.IOException in case the resource path can not be resolved
     */
    protected void addJavaResource(ZipArchiver zipArchiver, Resource javaResource, String prefix) throws ArchiverException, IOException {
        if (javaResource != null) {
            final File javaResourceDirectory = new File(javaResource.getDirectory());
            if (javaResourceDirectory.exists()) {
                final String resourcePath = javaResourceDirectory.getCanonicalPath();
                final String apkLibUnpackBasePath = unpackedApkLibsDirectory.getCanonicalPath();
                // Don't include our dependencies' resource dirs.
                if (!resourcePath.startsWith(apkLibUnpackBasePath)) {
                    final DefaultFileSet javaResourceFileSet = new DefaultFileSet();
                    javaResourceFileSet.setDirectory(javaResourceDirectory);
                    javaResourceFileSet.setPrefix(endWithSlash(prefix));
                    zipArchiver.addFileSet(javaResourceFileSet);
                }
            }
        }
    }

    /**
     * Makes sure the string ends with "/"
     *
     * @param prefix any string, or null.
     * @return the prefix with a "/" at the end, never null.
     */
    protected String endWithSlash(String prefix) {
        prefix = StringUtils.defaultIfEmpty(prefix, "/");
        if (!prefix.endsWith("/")) {
            prefix = prefix + "/";
        }
        return prefix;
    }
    /**
     * Adds a directory to a {@link org.codehaus.plexus.archiver.zip.ZipArchiver} with a directory prefix.
     *
     * @param zipArchiver
     * @param directory   The directory to add.
     * @param prefix      An optional prefix for where in the Zip file the directory's contents should go.
     * @throws org.codehaus.plexus.archiver.ArchiverException
     */
    protected void addDirectory(ZipArchiver zipArchiver, File directory, String prefix) throws ArchiverException {
        if (directory != null && directory.exists()) {
            final DefaultFileSet fileSet = new DefaultFileSet();
            fileSet.setPrefix(endWithSlash(prefix));
            fileSet.setDirectory(directory);
            zipArchiver.addFileSet(fileSet);
        }
    }

    /**
     * Adds a directory to a {@link org.codehaus.plexus.archiver.zip.ZipArchiver} with a directory prefix.
     *
     * @param zipArchiver
     * @param directory   The directory to add.
     * @param prefix      An optional prefix for where in the Zip file the directory's contents should go.
     * @throws org.codehaus.plexus.archiver.ArchiverException
     */
    protected void addDirectory(ZipArchiver zipArchiver, File directory, String[] excludes) throws ArchiverException {
        if (directory != null && directory.exists()) {
            final DefaultFileSet fileSet = new DefaultFileSet();
            fileSet.setDirectory(directory);
            fileSet.setExcludes(excludes);
            zipArchiver.addFileSet(fileSet);
        }
    }


    /**
     * Generates an intermediate apk file (actually .ap_) containing the resources and assets.
     *
     * @throws org.apache.maven.plugin.MojoExecutionException
     */
    private void generateIntermediateAp_() throws MojoExecutionException {

        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger(this.getLog());
        File[] overlayDirectories;

        if (resourceOverlayDirectories == null || resourceOverlayDirectories.length == 0) {
            overlayDirectories = new File[]{resourceOverlayDirectory};
        } else {
            overlayDirectories = resourceOverlayDirectories;
        }

        File androidJar = getAndroidSdk().getAndroidJar();
        File outputFile = new File(project.getBuild().getDirectory(), project.getBuild().getFinalName() + ".ap_");

        List<String> commands = new ArrayList<String>();
        commands.add("package");
        commands.add("-f");
        commands.add("-M");
        commands.add(androidManifestFile.getAbsolutePath());
        commands.add("--non-constant-id");
        for (File resOverlayDir : overlayDirectories) {
            if (resOverlayDir != null && resOverlayDir.exists()) {
                commands.add("-S");
                commands.add(resOverlayDir.getAbsolutePath());
            }
        }
        if (combinedRes.exists()) {
            commands.add("-S");
            commands.add(combinedRes.getAbsolutePath());
        } else {
            if (resourceDirectory.exists()) {
                commands.add("-S");
                commands.add(resourceDirectory.getAbsolutePath());
            }
        }
        for (Artifact apkLibraryArtifact: getRelevantDependencyArtifacts()) {
        	if (apkLibraryArtifact.getType().equals(APKLIB)) {
        		commands.add("-S");
        		commands.add(getLibraryUnpackDirectory(apkLibraryArtifact)+"/res");
        	}
            if (apkLibraryArtifact.getType().equals(APKLIB_PRECOMPILED)) {
                commands.add("-S");
                commands.add(getLibraryUnpackDirectory(apkLibraryArtifact)+"/bin/res");
            }
        }
		commands.add("--auto-add-overlay");
        if (assetsDirectory.exists()) {
            commands.add("-A");
            commands.add(assetsDirectory.getAbsolutePath());
        }
        if (extractedDependenciesAssets.exists()) {
            commands.add("-A");
            commands.add(extractedDependenciesAssets.getAbsolutePath());
        }
        commands.add("-I");
        commands.add(androidJar.getAbsolutePath());
        commands.add("-F");
        commands.add(outputFile.getAbsolutePath());
        if (StringUtils.isNotBlank(configurations)) {
            commands.add("-c");
            commands.add(configurations);
        }
        getLog().info(getAndroidSdk().getPathForTool("aapt") + " " + commands.toString());
        try {
            executor.executeCommand(getAndroidSdk().getPathForTool("aapt"), commands, project.getBasedir(), false);
        } catch (ExecutionException e) {
            throw new MojoExecutionException("", e);
        }
    }
}
