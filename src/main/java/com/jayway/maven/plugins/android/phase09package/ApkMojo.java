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

import static com.jayway.maven.plugins.android.common.AndroidExtension.APK;
import static com.jayway.maven.plugins.android.common.AndroidExtension.APKLIB;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import com.jayway.maven.plugins.android.common.AetherHelper;
import com.jayway.maven.plugins.android.common.NativeHelper;
import com.jayway.maven.plugins.android.configuration.Sign;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.AbstractScanner;
import org.codehaus.plexus.util.SelectorUtils;

import com.jayway.maven.plugins.android.AbstractAndroidMojo;
import com.jayway.maven.plugins.android.AndroidSigner;
import com.jayway.maven.plugins.android.CommandExecutor;
import com.jayway.maven.plugins.android.ExecutionException;
import org.codehaus.plexus.util.DirectoryScanner;


/**
 * Creates the apk file. By default signs it with debug keystore.<br/>
 * Change that by setting configuration parameter <code>&lt;sign&gt;&lt;debug&gt;false&lt;/debug&gt;&lt;/sign&gt;</code>.
 *
 * @author hugo.josefson@jayway.com
 * @goal apk
 * @phase package
 * @requiresDependencyResolution compile
 */
public class ApkMojo extends AbstractAndroidMojo {

    /**
     * <p>How to sign the apk.</p>
     * <p>Looks like this:</p>
     * <pre>
     * &lt;sign&gt;
     *     &lt;debug&gt;auto&lt;/debug&gt;
     * &lt;/sign&gt;
     * </pre>
     * <p>Valid values for <code>&lt;debug&gt;</code> are:
     * <ul>
     * <li><code>true</code> = sign with the debug keystore.
     * <li><code>false</code> = don't sign with the debug keystore.
     * <li><code>both</code> = create a signed as well as an unsigned apk.
     * <li><code>auto</code> (default) = sign with debug keystore, unless another keystore is defined. (Signing with
     * other keystores is not yet implemented. See
     * <a href="http://code.google.com/p/maven-android-plugin/issues/detail?id=2">Issue 2</a>.)
     * </ul></p>
     * <p>Can also be configured from command-line with parameter <code>-Dandroid.sign.debug</code>.</p>
     *
     * @parameter
     */
    private Sign sign;

    /**
     * <p>Parameter designed to pick up <code>-Dandroid.sign.debug</code> in case there is no pom with a
     * <code>&lt;sign&gt;</code> configuration tag.</p>
     * <p>Corresponds to {@link com.jayway.maven.plugins.android.configuration.Sign#debug}.</p>
     *
     * @parameter expression="${android.sign.debug}" default-value="auto"
     * @readonly
     */
    private String signDebug;

    /**
     * <p>A possibly new package name for the application. This value will be passed on to the aapt
     * parameter --rename-manifest-package. Look to aapt for more help on this. </p>
     *
     * @parameter expression="${android.renameManifestPackage}"
     */
    private String renameManifestPackage;

    /**
     * <p>Rewrite the manifest so that all of its instrumentation components target the given package.
     * This value will be passed on to the aapt parameter --rename-instrumentation-target-package.
     * Look to aapt for more help on this. </p>
     *
     * @parameter expression="${android.renameInstrumentationTargetPackage}"
     */
    private String renameInstrumentationTargetPackage;

    /**
     * <p>Allows to detect and extract the duplicate files from embedded jars. In that case, the plugin analyzes
     * the content of all embedded dependencies and checks they are no duplicates inside those dependencies. Indeed,
     * Android does not support duplicates, and all dependencies are inlined in the APK. If duplicates files are found,
     * the resource is kept in the first dependency and removes from others.
     *
     * @parameter expression="${android.extractDuplicates}" default-value="false"
     */
    private boolean extractDuplicates;

     /**
     * <p>Temporary folder for collecting native libraries.</p>
     *
     * @parameter default-value="${project.build.directory}/libs"
     * @readonly
     */
    private File nativeLibrariesOutputDirectory;

    /**
     * <p>Default hardware architecture for native library dependencies (with {@code &lt;type>so&lt;/type>}).</p>
     * <p>This value is used for dependencies without classifier, if {@code nativeLibrariesDependenciesHardwareArchitectureOverride} is not set.</p>
     * <p>Valid values currently include {@code armeabi} and {@code armeabi-v7a}.</p>
     *
     * @parameter expression="${android.nativeLibrariesDependenciesHardwareArchitectureDefault}" default-value="armeabi"
     */
    private String nativeLibrariesDependenciesHardwareArchitectureDefault;

    /**
    * <p>Classifier to add to the artifact generated. If given, the artifact will be an attachment instead.</p>
    *
    * @parameter
    */
    private String classifier;

    /**
     * <p>Override hardware architecture for native library dependencies (with {@code &lt;type>so&lt;/type>}).</p>
     * <p>This overrides any classifier on native library dependencies, and any {@code nativeLibrariesDependenciesHardwareArchitectureDefault}.</p>
     * <p>Valid values currently include {@code armeabi} and {@code armeabi-v7a}.</p>
     *
     * @parameter expression="${android.nativeLibrariesDependenciesHardwareArchitectureOverride}"
     */
    private String nativeLibrariesDependenciesHardwareArchitectureOverride;

	/**
	 * <p>Additional source directories that contain resources to be packaged into the apk.</p>
	 * <p>These are not source directories, that contain java classes to be compiled.
     * It corresponds to the -df option of the apkbuilder program. It allows you to specify directories,
     * that contain additional resources to be packaged into the apk. </p>
	 * So an example inside the plugin configuration could be:
     * <pre>
     * &lt;configuration&gt;
     * 	  ...
     *    &lt;sourceDirectories&gt;
     *      &lt;sourceDirectory&gt;${project.basedir}/additionals&lt;/sourceDirectory&gt;
     *	  &lt;/sourceDirectories&gt;
     *	  ...
     * &lt;/configuration&gt;
     * </pre>
     *
     * @parameter expression="${android.sourceDirectories}" default-value=""
     */
    private File[] sourceDirectories;

    /**
      * @component
      * @readonly
      * @required
      */
     protected ArtifactFactory artifactFactory;

    private static final Pattern PATTERN_JAR_EXT = Pattern.compile("^.+\\.jar$", 2);

    public void execute() throws MojoExecutionException, MojoFailureException {

        // Make an early exit if we're not supposed to generate the APK
        if (!generateApk) {
            return;
        }

        generateIntermediateAp_();

        // Initialize apk build configuration
        File outputFile = new File(project.getBuild().getDirectory(), project.getBuild().getFinalName() +
                "." + APK);
        final boolean signWithDebugKeyStore = getAndroidSigner().isSignWithDebugKeyStore();

        if (getAndroidSigner().shouldCreateBothSignedAndUnsignedApk()) {
            getLog().info("Creating debug key signed apk file " + outputFile);
            createApkFile(outputFile, true);
            final File unsignedOutputFile = new File(project.getBuild().getDirectory(), project.getBuild()
                    .getFinalName() + "-unsigned." + APK);
            getLog().info("Creating additional unsigned apk file " + unsignedOutputFile);
            createApkFile(unsignedOutputFile, false);
            projectHelper.attachArtifact(project, unsignedOutputFile, classifier == null ? "unsigned" : classifier + "_unsigned");
        } else {
            createApkFile(outputFile, signWithDebugKeyStore);
        }

        if (classifier == null)
        {
            // Set the generated .apk file as the main artifact (because the pom states <packaging>apk</packaging>)
            project.getArtifact().setFile(outputFile);
        }
        else
        {
            // If there is a classifier specified, attach the artifact using that
            projectHelper.attachArtifact(project, outputFile, classifier);
        }
    }

    void createApkFile(File outputFile, boolean signWithDebugKeyStore) throws MojoExecutionException {
        File dexFile = new File(project.getBuild().getDirectory(), "classes.dex");
        File zipArchive = new File(project.getBuild().getDirectory(), project.getBuild().getFinalName() + ".ap_");
        ArrayList<File> sourceFolders = new ArrayList<File>();
	if (sourceDirectories != null) {
		for(File f:sourceDirectories) {
			sourceFolders.add(f);
		}
	}
        ArrayList<File> jarFiles = new ArrayList<File>();
        ArrayList<File> nativeFolders = new ArrayList<File>();

        boolean useInternalAPKBuilder = true;
        try {
            initializeAPKBuilder();
            // Ok...
            // So we can try to use the internal ApkBuilder
        } catch (Throwable e) {
            // Not supported platform try to old way.
            useInternalAPKBuilder = false;
        }

        // Process the native libraries, looking both in the current build directory as well as
        // at the dependencies declared in the pom.  Currently, all .so files are automatically included
        processNativeLibraries(nativeFolders);

        if (useInternalAPKBuilder) {
            doAPKWithAPKBuilder(outputFile, dexFile, zipArchive, sourceFolders, jarFiles,
                nativeFolders, false, signWithDebugKeyStore, false);
        } else {
            doAPKWithCommand(outputFile, dexFile, zipArchive, sourceFolders, jarFiles,
                nativeFolders, signWithDebugKeyStore);
        }

// ISSUE-97
// vvv
//
        if( this.metaIncludes != null && this.metaIncludes.length > 0 ) {
        	try {
				addMetaInf( outputFile, jarFiles );
			}
			catch( IOException e ) {
				throw new MojoExecutionException("Could not add META-INF resources.", e);
			}
        }
//
// ^^^
// ISSUE-97
    }

// ISSUE-97
// vvv
//
	/**
	 * <p>
	 * Pattern for additional META-INF resources to be packaged into the apk.
	 * </p>
	 * <p>
	 * The APK builder filters these resources and doesn't include them into the apk.
	 * </p>
	 * <p>
	 * This leads to bad behaviour of dependent libraries relying on these resources, for instance service discovery
	 * doesn't work.
	 * </p>
	 * <p>
	 * See also <a href="http://code.google.com/p/maven-android-plugin/issues/detail?id=97">Issue 97</a>
	 * </p>
	 * 
	 * @parameter expression="${android.metaIncludes}" default-value=""
	 */
	private String[]	metaIncludes;

	private void addMetaInf( File outputFile, ArrayList<File> jarFiles ) throws IOException
	{
		File tmp = File.createTempFile( outputFile.getName(), ".add", outputFile.getParentFile() );

		FileOutputStream fos = new FileOutputStream( tmp );
		ZipOutputStream zos = new ZipOutputStream( fos );
		Set<String> entries = new HashSet<String>();

		updateWithMetaInf( zos, outputFile, entries, false );

		for( File f : jarFiles ) {
			updateWithMetaInf( zos, f, entries, true );
		}

		zos.close();

		outputFile.delete();

		if( !tmp.renameTo( outputFile ) ) {
			throw new IOException( String.format( "Cannot rename %s to %s", tmp, outputFile.getName() ) );
		}
	}

	private void updateWithMetaInf( ZipOutputStream zos, File jarFile, Set<String> entries, boolean metaInfOnly )
	throws ZipException, IOException
	{
		ZipFile zin = new ZipFile( jarFile );

		for( Enumeration<? extends ZipEntry> en = zin.entries(); en.hasMoreElements(); ) {
			ZipEntry ze = en.nextElement();

			if( ze.isDirectory() )
				continue;

			String zn = ze.getName();

			if( metaInfOnly ) {
				if( !zn.startsWith( "META-INF/" ) ) {
					continue;
				}

				if( !entries.add( zn ) ) {
					continue;
				}

				if( !metaInfMatches( zn ) ) {
					continue;
				}
			}

			zos.putNextEntry( new ZipEntry( zn ) );

			InputStream is = zin.getInputStream( ze );

			copyStreamWithoutClosing( is, zos );

			is.close();
			zos.closeEntry();
		}

		zin.close();
	}

	private boolean metaInfMatches( String path )
	{
		for( String inc : this.metaIncludes ) {
			if( SelectorUtils.matchPath( "META-INF/" + inc, path ) )
				return true;
		}

		return false;
	}
//
// ^^^
// ISSUE-97

    private Map<String, List<File>> m_jars = new HashMap<String, List<File>>();

    private void computeDuplicateFiles(File jar) throws ZipException, IOException {
        ZipFile file = new ZipFile(jar);
        Enumeration<? extends ZipEntry> list = file.entries();
        while(list.hasMoreElements()) {
            ZipEntry ze = list.nextElement();
            if (! (ze.getName().contains("META-INF/")  || ze.isDirectory())) { // Exclude META-INF and Directories
                List<File> l = m_jars.get(ze.getName());
                if (l == null) {
                    l = new ArrayList<File>();
                    m_jars.put(ze.getName(), l);
                }
                l.add(jar);
            }
        }
    }

    /**
     * Creates the APK file using the internal APKBuilder.
     * @param outputFile the output file
     * @param dexFile the dex file
     * @param zipArchive the classes folder
     * @param sourceFolders the resources
     * @param jarFiles the embedded java files
     * @param nativeFolders the native folders
     * @param verbose enables the verbose mode
     * @param signWithDebugKeyStore enables the signature of the APK using the debug key
     * @param debug enables the debug mode
     * @throws MojoExecutionException if the APK cannot be created.
     */
    private void doAPKWithAPKBuilder(File outputFile, File dexFile, File zipArchive,
            ArrayList<File> sourceFolders, ArrayList<File> jarFiles,
            ArrayList<File> nativeFolders, boolean verbose, boolean signWithDebugKeyStore,
            boolean debug) throws MojoExecutionException {
        sourceFolders.add(new File(project.getBuild().getOutputDirectory()));

        for (Artifact artifact : getRelevantCompileArtifacts()) {
            if (extractDuplicates) {
                try {
                    computeDuplicateFiles(artifact.getFile());
                } catch (Exception e) {
                    getLog().warn("Cannot compute duplicates files from " + artifact.getFile().getAbsolutePath(), e);
                }
            }
            jarFiles.add(artifact.getFile());
        }

        // Check duplicates.
        if (extractDuplicates) {
            List<String> duplicates = new ArrayList<String>();
            List<File> jarToModify = new ArrayList<File>();
            for (String s : m_jars.keySet()) {
                List<File> l = m_jars.get(s);
                if (l.size() > 1) {
                    getLog().warn("Duplicate file " + s + " : " + l);
                    duplicates.add(s);
                    for (int i = 1; i < l.size(); i++) {
                        if (! jarToModify.contains(l.get(i))) {
                            jarToModify.add(l.get(i));
                        }
                    }
                }
            }

            // Rebuild jars.
            for (File file : jarToModify) {
                File newJar;
                    newJar = removeDuplicatesFromJar(file, duplicates);
                    int index = jarFiles.indexOf(file);
                    if (newJar != null) {
                        jarFiles.set(index, newJar);
                    }

            }
        }

        ApkBuilder builder = new ApkBuilder(outputFile, zipArchive, dexFile, signWithDebugKeyStore,  (verbose) ? System.out : null);

        if (debug) {
            builder.setDebugMode(debug);
        }

        for (File sourceFolder : sourceFolders) {
            builder.addSourceFolder(sourceFolder);
        }

        for (File jarFile : jarFiles) {
            if (jarFile.isDirectory()) {
                String[] filenames = jarFile.list(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return PATTERN_JAR_EXT.matcher(name).matches();
                    }
                });

                for (String filename : filenames) {
                    builder.addResourcesFromJar(new File(jarFile, filename));
                }
            } else {
                builder.addResourcesFromJar(jarFile);
            }
        }

        for (File nativeFolder : nativeFolders) {
            builder.addNativeLibraries(nativeFolder, null);
        }

        builder.sealApk();
    }

    private File removeDuplicatesFromJar(File in, List<String> duplicates) {
//        File target = new File(project.getBasedir(), "target");
        String target = project.getBuild().getOutputDirectory();
        File tmp = new File(target, "unpacked-wembedded-jars");
        tmp.mkdirs();
        File out = new File(tmp, in.getName());

        if (out.exists()) {
            return out;
        } else {
            try {
                out.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Create a new Jar file
        FileOutputStream fos = null;
        ZipOutputStream jos = null;
        try {
            fos = new FileOutputStream(out);
            jos = new ZipOutputStream(fos);
        } catch (FileNotFoundException e1) {
            getLog().error("Cannot remove duplicates : the output file " + out.getAbsolutePath() + " does not found");
            return null;
        }

        ZipFile inZip = null;
        try {
            inZip = new ZipFile(in);
            Enumeration< ? extends ZipEntry> entries = inZip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                // If the entry is not a duplicate, copy.
                if (! duplicates.contains(entry.getName())) {
                    // copy the entry header to jos
                    jos.putNextEntry(entry);
                    InputStream currIn = inZip.getInputStream(entry);
                    copyStreamWithoutClosing(currIn, jos);
                    currIn.close();
                    jos.closeEntry();
                }
            }
        } catch (IOException e) {
            getLog().error("Cannot removing duplicates : " + e.getMessage());
            return null;
        }

        try {
            if (inZip != null) {
                inZip.close();
            }
            jos.close();
            fos.close();
            jos = null;
            fos = null;
        } catch (IOException e) {
            // ignore it.
        }
        getLog().info(in.getName() + " rewritten without duplicates : " + out.getAbsolutePath());
        return out;
    }

    /**
     * Copies an input stream into an output stream but does not close the streams.
     * @param in the input stream
     * @param out the output stream
     * @throws IOException if the stream cannot be copied
     */
    private static void copyStreamWithoutClosing(InputStream in, OutputStream out) throws IOException {
        byte[] b = new byte[4096];
        for (int n; (n = in.read(b)) != -1;) {
            out.write(b, 0, n);
        }
    }


    /**
     * Creates the APK file using the command line.
     * @param outputFile the output file
     * @param dexFile the dex file
     * @param zipArchive the classes folder
     * @param sourceFolders the resources
     * @param jarFiles the embedded java files
     * @param nativeFolders the native folders
     * @param signWithDebugKeyStore enables the signature of the APK using the debug key
     * @throws MojoExecutionException if the APK cannot be created.
     */
    private void doAPKWithCommand(File outputFile, File dexFile, File zipArchive,
            ArrayList<File> sourceFolders, ArrayList<File> jarFiles,
            ArrayList<File> nativeFolders, boolean signWithDebugKeyStore) throws MojoExecutionException {
        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger(this.getLog());

        List<String> commands = new ArrayList<String>();
        commands.add(outputFile.getAbsolutePath());

        if (! signWithDebugKeyStore) {
            commands.add("-u");
        }

        commands.add("-z");
        commands.add(new File(project.getBuild().getDirectory(), project.getBuild().getFinalName() + ".ap_").getAbsolutePath());
        commands.add("-f");
        commands.add(new File(project.getBuild().getDirectory(), "classes.dex").getAbsolutePath());
        commands.add("-rf");
        commands.add(new File(project.getBuild().getOutputDirectory()).getAbsolutePath());

        if (nativeFolders != null  && ! nativeFolders.isEmpty()) {
            for (File lib : nativeFolders) {
                commands.add("-nf");
                commands.add(lib.getAbsolutePath());
            }
        }

        for (Artifact artifact : getRelevantCompileArtifacts()) {
            commands.add("-rj");
            commands.add(artifact.getFile().getAbsolutePath());
        }


        getLog().info(getAndroidSdk().getPathForTool("apkbuilder") + " " + commands.toString());
        try {
            executor.executeCommand(getAndroidSdk().getPathForTool("apkbuilder"), commands, project.getBasedir(), false);
        } catch (ExecutionException e) {
            throw new MojoExecutionException("", e);
        }
    }


    private void initializeAPKBuilder() throws MojoExecutionException {
        File file = getAndroidSdk().getSDKLibJar();
        ApkBuilder.initialize(getLog(), file);
    }

    private void processNativeLibraries(final List<File> natives) throws MojoExecutionException
    {
        // Examine the native libraries directory for content. This will only be true if:
        // a) the directory exists
        // b) it contains at least 1 file
        final boolean hasValidNativeLibrariesDirectory = nativeLibrariesDirectory != null && nativeLibrariesDirectory.exists() && ( nativeLibrariesDirectory.listFiles() != null && nativeLibrariesDirectory.listFiles().length > 0 );

        // Retrieve any native dependencies or attached artifacts.  This may include artifacts from the ndk-build MOJO
        NativeHelper nativeHelper = new NativeHelper( project, projectRepos, repoSession, repoSystem, artifactFactory, getLog() );
        final Set<Artifact> artifacts = nativeHelper.getNativeDependenciesArtifacts(unpackedApkLibsDirectory, true);

        final boolean hasValidBuildNativeLibrariesDirectory = nativeLibrariesOutputDirectory.exists() && ( nativeLibrariesOutputDirectory.listFiles() != null && nativeLibrariesOutputDirectory.listFiles().length > 0 );

        if ( artifacts.isEmpty() && hasValidNativeLibrariesDirectory && !hasValidBuildNativeLibrariesDirectory ) {

            getLog().debug("No native library dependencies detected, will point directly to " + nativeLibrariesDirectory);

            // Point directly to the directory in this case - no need to copy files around
            natives.add(nativeLibrariesDirectory);
        }
        else if (!artifacts.isEmpty() || hasValidNativeLibrariesDirectory)
        {
            // In this case, we may have both .so files in it's normal location
            // as well as .so dependencies

            // Create the ${project.build.outputDirectory}/libs
            final File destinationDirectory = new File(nativeLibrariesOutputDirectory.getAbsolutePath());
            destinationDirectory.mkdirs();

             // Point directly to the directory
            natives.add(destinationDirectory);

            // If we have a valid native libs, copy those files - these already come in the structure required
            if ( hasValidNativeLibrariesDirectory ) {
                copyLocalNativeLibraries( nativeLibrariesDirectory, destinationDirectory );
            }

            if ( hasValidBuildNativeLibrariesDirectory ) {
                copyLocalNativeLibraries( nativeLibrariesOutputDirectory, destinationDirectory );
            }

            if (!artifacts.isEmpty())
            {
                for (Artifact resolvedArtifact : artifacts)
                {
                    if ("so".equals(resolvedArtifact.getType()))
                    {
                        final File artifactFile = resolvedArtifact.getFile();
                        try
                        {
                            final String artifactId = resolvedArtifact.getArtifactId();
                            final String filename = artifactId.startsWith("lib") ? artifactId + ".so" : "lib" + artifactId + ".so";

                            final File finalDestinationDirectory = getFinalDestinationDirectoryFor(resolvedArtifact, destinationDirectory);
                            final File file = new File(finalDestinationDirectory, filename);
                            getLog().debug("Copying native dependency " + artifactId + " (" + resolvedArtifact.getGroupId() + ") to " + file );
                            org.apache.commons.io.FileUtils.copyFile(artifactFile, file);
                        }
                        catch (Exception e)
                        {
                            throw new MojoExecutionException("Could not copy native dependency.", e);
                        }
                    }
                    else if (APKLIB.equals(resolvedArtifact.getType()))
                    {
                        natives.add(new File(getLibraryUnpackDirectory(resolvedArtifact)+"/libs"));
                    }
                }
            }
        }
    }

    private File getFinalDestinationDirectoryFor(Artifact resolvedArtifact, File destinationDirectory) {
        final String hardwareArchitecture = getHardwareArchitectureFor(resolvedArtifact);

        File finalDestinationDirectory = new File(destinationDirectory, hardwareArchitecture + "/");

        finalDestinationDirectory.mkdirs();
        return finalDestinationDirectory;
    }

    private String getHardwareArchitectureFor(Artifact resolvedArtifact) {

        if (StringUtils.isNotBlank(nativeLibrariesDependenciesHardwareArchitectureOverride)){
            return nativeLibrariesDependenciesHardwareArchitectureOverride;
        }

        final String classifier = resolvedArtifact.getClassifier();
        if (StringUtils.isNotBlank(classifier)) {
            return classifier;
        }

        return nativeLibrariesDependenciesHardwareArchitectureDefault;
    }

    private void copyLocalNativeLibraries( final File localNativeLibrariesDirectory, final File destinationDirectory ) throws MojoExecutionException {
        getLog().debug( "Copying existing native libraries from " + localNativeLibrariesDirectory );
        try {
            org.apache.commons.io.FileUtils.copyDirectory( localNativeLibrariesDirectory, destinationDirectory, new FileFilter() {
                public boolean accept( final File pathname ) {
                    return pathname.getName().endsWith( ".so" );
                }
            } );
        }
        catch ( IOException e ) {
            getLog().error( "Could not copy native libraries: " + e.getMessage(), e );
            throw new MojoExecutionException( "Could not copy native dependency.", e );
        }
    }


    /**
     * Generates an intermediate apk file (actually .ap_) containing the resources and assets.
     *
     * @throws MojoExecutionException
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

        if (extractedDependenciesRes.exists()) {
            try {
                getLog().info("Copying dependency resource files to combined resource directory.");
                if (!combinedRes.exists()) {
                    if (!combinedRes.mkdirs()) {
                        throw new MojoExecutionException("Could not create directory for combined resources at " + combinedRes.getAbsolutePath());
                    }
                }
               org.apache.commons.io.FileUtils.copyDirectory(extractedDependenciesRes, combinedRes);
            } catch (IOException e) {
                throw new MojoExecutionException("", e);
            }
        }
        if (resourceDirectory.exists() && combinedRes.exists()) {
            try {
                getLog().info("Copying local resource files to combined resource directory.");
                org.apache.commons.io.FileUtils.copyDirectory(resourceDirectory, combinedRes, new FileFilter() {

                    /**
                     * Excludes files matching one of the common file to exclude.
                     * The default excludes pattern are the ones from
                     * {org.codehaus.plexus.util.AbstractScanner#DEFAULTEXCLUDES}
                     * @see java.io.FileFilter#accept(java.io.File)
                     */
                    public boolean accept(File file) {
                        for (String pattern : DirectoryScanner.DEFAULTEXCLUDES) {
                            if (DirectoryScanner.match(pattern, file.getAbsolutePath())) {
                                getLog().debug("Excluding " + file.getName() + " from resource copy : matching " + pattern);
                                return false;
                            }
                        }
                        return true;
                    }
                });
            } catch (IOException e) {
                throw new MojoExecutionException("", e);
            }
        }

        // Must combine assets.
        // The aapt tools does not support several -A arguments.
        // We copy the assets from extracted dependencies first, and then the local assets.
        // This allows redefining the assets in the current project
        if (extractedDependenciesAssets.exists()) {
            try {
                getLog().info("Copying dependency assets files to combined assets directory.");
                org.apache.commons.io.FileUtils.copyDirectory(extractedDependenciesAssets, combinedAssets, new FileFilter() {
                    /**
                     * Excludes files matching one of the common file to exclude.
                     * The default excludes pattern are the ones from
                     * {org.codehaus.plexus.util.AbstractScanner#DEFAULTEXCLUDES}
                     * @see java.io.FileFilter#accept(java.io.File)
                     */
                    public boolean accept(File file) {
                        for (String pattern : AbstractScanner.DEFAULTEXCLUDES) {
                            if (AbstractScanner.match(pattern, file.getAbsolutePath())) {
                                getLog().debug("Excluding " + file.getName() + " from asset copy : matching " + pattern);
                                return false;
                            }
                        }

                        return true;

                    }
                });
            } catch (IOException e) {
                throw new MojoExecutionException("", e);
            }
        }

        // Next pull APK Lib assets, reverse the order to give precedence to libs higher up the chain
        List<Artifact> artifactList = new ArrayList<Artifact>(getAllRelevantDependencyArtifacts());
		for (Artifact artifact: artifactList) {
			if (artifact.getType().equals(APKLIB)) {
				File apklibAsssetsDirectory = new File(getLibraryUnpackDirectory(artifact) + "/assets");
				if (apklibAsssetsDirectory.exists()) {
		            try {
		                getLog().info("Copying dependency assets files to combined assets directory.");
		                org.apache.commons.io.FileUtils.copyDirectory(apklibAsssetsDirectory, combinedAssets, new FileFilter() {
		                    /**
		                     * Excludes files matching one of the common file to exclude.
		                     * The default excludes pattern are the ones from
		                     * {org.codehaus.plexus.util.AbstractScanner#DEFAULTEXCLUDES}
		                     * @see java.io.FileFilter#accept(java.io.File)
		                     */
		                    public boolean accept(File file) {
		                        for (String pattern : AbstractScanner.DEFAULTEXCLUDES) {
		                            if (AbstractScanner.match(pattern, file.getAbsolutePath())) {
		                                getLog().debug("Excluding " + file.getName() + " from asset copy : matching " + pattern);
		                                return false;
		                            }
		                        }

		                        return true;

		                    }
		                });
		            } catch (IOException e) {
		                throw new MojoExecutionException("", e);
		            }

				}
			}
		}

        if (assetsDirectory.exists()) {
            try {
                getLog().info("Copying local assets files to combined assets directory.");
                org.apache.commons.io.FileUtils.copyDirectory(assetsDirectory, combinedAssets, new FileFilter() {
                    /**
                     * Excludes files matching one of the common file to exclude.
                     * The default excludes pattern are the ones from
                     * {org.codehaus.plexus.util.AbstractScanner#DEFAULTEXCLUDES}
                     * @see java.io.FileFilter#accept(java.io.File)
                     */
                    public boolean accept(File file) {
                        for (String pattern : AbstractScanner.DEFAULTEXCLUDES) {
                            if (AbstractScanner.match(pattern, file.getAbsolutePath())) {
                                getLog().debug("Excluding " + file.getName() + " from asset copy : matching " + pattern);
                                return false;
                            }
                        }

                        return true;

                    }
                });
            } catch (IOException e) {
                throw new MojoExecutionException("", e);
            }
        }


        File androidJar = getAndroidSdk().getAndroidJar();
        File outputFile = new File(project.getBuild().getDirectory(), project.getBuild().getFinalName() + ".ap_");

        List<String> commands = new ArrayList<String>();
        commands.add("package");
        commands.add("-f");
        commands.add("-M");
        commands.add(androidManifestFile.getAbsolutePath());
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
		for (Artifact artifact: getAllRelevantDependencyArtifacts()) {
			if (artifact.getType().equals(APKLIB)) {
                final String apkLibResDir = getLibraryUnpackDirectory(artifact) + "/res";
                if (new File(apkLibResDir).exists()){
                    commands.add("-S");
                    commands.add(apkLibResDir);
                }
			}
		}
		commands.add("--auto-add-overlay");

        // Use the combined assets.
        // Indeed, aapt does not support several -A arguments.
        if (combinedAssets.exists()) {
            commands.add("-A");
            commands.add(combinedAssets.getAbsolutePath());
        }

        if (StringUtils.isNotBlank(renameManifestPackage)) {
          commands.add("--rename-manifest-package");
          commands.add(renameManifestPackage);
        }

        if (StringUtils.isNotBlank(renameInstrumentationTargetPackage)) {
          commands.add("--rename-instrumentation-target-package");
          commands.add(renameInstrumentationTargetPackage);
        }

        commands.add("-I");
        commands.add(androidJar.getAbsolutePath());
        commands.add("-F");
        commands.add(outputFile.getAbsolutePath());
        if (StringUtils.isNotBlank(configurations)) {
            commands.add("-c");
            commands.add(configurations);
        }

        for (String aaptExtraArg : aaptExtraArgs) {
            commands.add(aaptExtraArg);
        }

        getLog().info(getAndroidSdk().getPathForTool("aapt") + " " + commands.toString());
        try {
            executor.executeCommand(getAndroidSdk().getPathForTool("aapt"), commands, project.getBasedir(), false);
        } catch (ExecutionException e) {
            throw new MojoExecutionException("", e);
        }
    }

    protected AndroidSigner getAndroidSigner() {
        if (sign == null) {
            return new AndroidSigner(signDebug);
        } else {
            return new AndroidSigner(sign.getDebug());
        }
    }
}
