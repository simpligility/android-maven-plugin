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

import com.android.sdklib.build.ApkBuilder;
import com.android.sdklib.build.ApkCreationException;
import com.android.sdklib.build.DuplicateFileException;
import com.android.sdklib.build.SealedApkException;
import com.jayway.maven.plugins.android.AbstractAndroidMojo;
import com.jayway.maven.plugins.android.AndroidNdk;
import com.jayway.maven.plugins.android.AndroidSigner;
import com.jayway.maven.plugins.android.CommandExecutor;
import com.jayway.maven.plugins.android.ExecutionException;
import com.jayway.maven.plugins.android.common.NativeHelper;
import com.jayway.maven.plugins.android.config.ConfigHandler;
import com.jayway.maven.plugins.android.config.ConfigPojo;
import com.jayway.maven.plugins.android.config.PullParameter;
import com.jayway.maven.plugins.android.configuration.Apk;
import com.jayway.maven.plugins.android.configuration.MetaInf;
import com.jayway.maven.plugins.android.configuration.Sign;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.AbstractScanner;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static com.jayway.maven.plugins.android.common.AndroidExtension.APK;
import static com.jayway.maven.plugins.android.common.AndroidExtension.AAR;
import static com.jayway.maven.plugins.android.common.AndroidExtension.APKLIB;


/**
 * Creates the apk file. By default signs it with debug keystore.<br/>
 * Change that by setting configuration parameter
 * <code>&lt;sign&gt;&lt;debug&gt;false&lt;/debug&gt;&lt;/sign&gt;</code>.
 *
 * @author hugo.josefson@jayway.com
 * @goal apk
 * @phase package
 * @requiresDependencyResolution compile
 */
public class ApkMojo extends AbstractAndroidMojo
{

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
     * <p>Rewrite the manifest so that all of its instrumentation components target the given package.
     * This value will be passed on to the aapt parameter --rename-instrumentation-target-package.
     * Look to aapt for more help on this. </p>
     *
     * @parameter expression="${android.renameInstrumentationTargetPackage}"
     *
     * TODO pass this into AaptExecutor
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
     * <p>Classifier to add to the artifact generated. If given, the artifact will be an attachment instead.</p>
     *
     * @parameter
     */
    private String classifier;

    /**
     * <p>Additional source directories that contain resources to be packaged into the apk.</p>
     * <p>These are not source directories, that contain java classes to be compiled.
     * It corresponds to the -df option of the apkbuilder program. It allows you to specify directories,
     * that contain additional resources to be packaged into the apk. </p>
     * So an example inside the plugin configuration could be:
     * <pre>
     * &lt;configuration&gt;
     *   ...
     *    &lt;sourceDirectories&gt;
     *      &lt;sourceDirectory&gt;${project.basedir}/additionals&lt;/sourceDirectory&gt;
     *   &lt;/sourceDirectories&gt;
     *   ...
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

    /**
     * Pattern for additional META-INF resources to be packaged into the apk.
     * <p>
     * The APK builder filters these resources and doesn't include them into the apk.
     * This leads to bad behaviour of dependent libraries relying on these resources,
     * for instance service discovery doesn't work.<br/>
     * By specifying this pattern, the android plugin adds these resources to the final apk.
     * </p>
     * <p>The pattern is relative to META-INF, i.e. one must use
     * <pre>
     * <code>
     * &lt;apkMetaIncludes&gt;
     *     &lt;metaInclude>services/**&lt;/metaInclude&gt;
     * &lt;/apkMetaIncludes&gt;
     * </code>
     * </pre>
     * ... instead of
     * <pre>
     * <code>
     * &lt;apkMetaIncludes&gt;
     *     &lt;metaInclude>META-INF/services/**&lt;/metaInclude&gt;
     * &lt;/apkMetaIncludes&gt;
     * </code>
     * </pre>
     * <p>
     * See also <a href="http://code.google.com/p/maven-android-plugin/issues/detail?id=97">Issue 97</a>
     * </p>
     *
     * @parameter expression="${android.apk.metaIncludes}"
     * @deprecated in favour of apk.metaInf
     */
    @PullParameter
    private String[] apkMetaIncludes;

    @PullParameter( defaultValueGetterMethod = "getDefaultMetaInf" )
    private MetaInf apkMetaInf;

    /**
     * @parameter alias="metaInf"
     */
    private MetaInf pluginMetaInf;

    /**
     * Defines whether or not the APK is being produced in debug mode or not.
     *
     * @parameter expression="${android.apk.debug}"
     */
    @PullParameter( defaultValue = "false" )
    private Boolean apkDebug;

    /**
     * @parameter expression="${android.nativeToolchain}"
     */
    @PullParameter( defaultValue = "arm-linux-androideabi-4.4.3" )
    private String apkNativeToolchain;

    /**
     * Specifies the final name of the library output by the build (this allows
     *
     * @parameter expression="${android.ndk.build.build.final-library.name}"
     */
    private String ndkFinalLibraryName;

    /**
     * Specify a list of patterns that are matched against the names of jar file
     * dependencies. Matching jar files will not have their resources added to the
     * resulting APK.
     * 
     * The patterns are standard Java regexes.
     * 
     * @parameter
     */
    private String[] excludeJarResources;
    private Pattern[] excludeJarResourcesPatterns;
    
    /**
     * Embedded configuration of this mojo.
     *
     * @parameter
     */
    @ConfigPojo( prefix = "apk" )
    private Apk apk;

    private static final Pattern PATTERN_JAR_EXT = Pattern.compile( "^.+\\.jar$", 2 );

    /**
     * <p>Default hardware architecture for native library dependencies (with {@code &lt;type>so&lt;/type>})
     * without a classifier.</p>
     * <p>Valid values currently include {@code armeabi}, {@code armeabi-v7a}, {@code mips} and {@code x86}.</p>
     *
     * @parameter expression="${android.nativeLibrariesDependenciesHardwareArchitectureDefault}" default-value="armeabi"
     */
    private String nativeLibrariesDependenciesHardwareArchitectureDefault;

    /**
     *
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {

        // Make an early exit if we're not supposed to generate the APK
        if ( ! generateApk )
        {
            return;
        }

        ConfigHandler cfh = new ConfigHandler( this, this.session, this.execution );

        cfh.parseConfiguration();

        generateIntermediateApk();

        // Compile resource exclusion patterns, if any
        if ( excludeJarResources != null && excludeJarResources.length > 0 ) 
        {
          getLog().debug( "Compiling " + excludeJarResources.length + " patterns" );
          
          excludeJarResourcesPatterns = new Pattern[excludeJarResources.length];
          
          for ( int index = 0; index < excludeJarResources.length; ++index ) 
          {
            excludeJarResourcesPatterns[index] = Pattern.compile( excludeJarResources[index] );
          }
        }
        
        // Initialize apk build configuration
        File outputFile = new File( project.getBuild().getDirectory(), project.getBuild().getFinalName() + "." + APK );
        final boolean signWithDebugKeyStore = getAndroidSigner().isSignWithDebugKeyStore();

        if ( getAndroidSigner().shouldCreateBothSignedAndUnsignedApk() )
        {
            getLog().info( "Creating debug key signed apk file " + outputFile );
            createApkFile( outputFile, true );
            final File unsignedOutputFile = new File( project.getBuild().getDirectory(),
                    project.getBuild().getFinalName() + "-unsigned." + APK );
            getLog().info( "Creating additional unsigned apk file " + unsignedOutputFile );
            createApkFile( unsignedOutputFile, false );
            projectHelper.attachArtifact( project, unsignedOutputFile,
                    classifier == null ? "unsigned" : classifier + "_unsigned" );
        }
        else
        {
            createApkFile( outputFile, signWithDebugKeyStore );
        }

        if ( classifier == null )
        {
            // Set the generated .apk file as the main artifact (because the pom states <packaging>apk</packaging>)
            project.getArtifact().setFile( outputFile );
        }
        else
        {
            // If there is a classifier specified, attach the artifact using that
            projectHelper.attachArtifact( project, outputFile, classifier );
        }
    }

    void createApkFile( File outputFile, boolean signWithDebugKeyStore ) throws MojoExecutionException
    {
        File dexFile = new File( project.getBuild().getDirectory(), "classes.dex" );
        File zipArchive = new File( project.getBuild().getDirectory(), project.getBuild().getFinalName() + ".ap_" );
        ArrayList<File> sourceFolders = new ArrayList<File>();
        if ( sourceDirectories != null )
        {
            for ( File f : sourceDirectories )
            {
                sourceFolders.add( f );
            }
        }
        ArrayList<File> jarFiles = new ArrayList<File>();
        ArrayList<File> nativeFolders = new ArrayList<File>();

        // Process the native libraries, looking both in the current build directory as well as
        // at the dependencies declared in the pom.  Currently, all .so files are automatically included
        processNativeLibraries( nativeFolders );
        
        doAPKWithAPKBuilder( outputFile, dexFile, zipArchive, sourceFolders, jarFiles, nativeFolders,
                    signWithDebugKeyStore );

        if ( this.apkMetaInf != null )
        {
            try
            {
                addMetaInf( outputFile, jarFiles );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Could not add META-INF resources.", e );
            }
        }
    }

    private void addMetaInf( File outputFile, ArrayList<File> jarFiles ) throws IOException
    {
        File tmp = File.createTempFile( outputFile.getName(), ".add", outputFile.getParentFile() );

        FileOutputStream fos = new FileOutputStream( tmp );
        ZipOutputStream zos = new ZipOutputStream( fos );
        Set<String> entries = new HashSet<String>();

        updateWithMetaInf( zos, outputFile, entries, false );

        for ( File f : jarFiles )
        {
            updateWithMetaInf( zos, f, entries, true );
        }

        zos.close();

        outputFile.delete();

        if ( ! tmp.renameTo( outputFile ) )
        {
            throw new IOException( String.format( "Cannot rename %s to %s", tmp, outputFile.getName() ) );
        }
    }

    private void updateWithMetaInf( ZipOutputStream zos, File jarFile, Set<String> entries, boolean metaInfOnly )
            throws IOException
    {
        ZipFile zin = new ZipFile( jarFile );

        for ( Enumeration<? extends ZipEntry> en = zin.entries(); en.hasMoreElements(); )
        {
            ZipEntry ze = en.nextElement();

            if ( ze.isDirectory() )
            {
                continue;
            }

            String zn = ze.getName();

            if ( metaInfOnly )
            {
                if ( ! zn.startsWith( "META-INF/" ) )
                {
                    continue;
                }

                if ( this.extractDuplicates && ! entries.add( zn ) )
                {
                    continue;
                }

                if ( ! this.apkMetaInf.isIncluded( zn ) )
                {
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

    private Map<String, List<File>> jars = new HashMap<String, List<File>>();

    private void computeDuplicateFiles( File jar ) throws IOException
    {
        ZipFile file = new ZipFile( jar );
        Enumeration<? extends ZipEntry> list = file.entries();
        while ( list.hasMoreElements() )
        {
            ZipEntry ze = list.nextElement();
            if ( ! ( ze.getName().contains( "META-INF/" ) || ze.isDirectory() ) )
            { // Exclude META-INF and Directories
                List<File> l = jars.get( ze.getName() );
                if ( l == null )
                {
                    l = new ArrayList<File>();
                    jars.put( ze.getName(), l );
                }
                l.add( jar );
            }
        }
    }

    /**
     * Creates the APK file using the internal APKBuilder.
     *
     * @param outputFile            the output file
     * @param dexFile               the dex file
     * @param zipArchive            the classes folder
     * @param sourceFolders         the resources
     * @param jarFiles              the embedded java files
     * @param nativeFolders         the native folders
     * @param signWithDebugKeyStore enables the signature of the APK using the debug key
     * @throws MojoExecutionException if the APK cannot be created.
     */
    private void doAPKWithAPKBuilder( File outputFile, File dexFile, File zipArchive, ArrayList<File> sourceFolders,
                                      ArrayList<File> jarFiles, ArrayList<File> nativeFolders,
                                      boolean signWithDebugKeyStore ) throws MojoExecutionException
    {
        getLog().debug( "Building APK with internal APKBuilder" );
        sourceFolders.add( new File( project.getBuild().getOutputDirectory() ) );

        for ( Artifact artifact : getRelevantCompileArtifacts() )
        {
            if ( extractDuplicates )
            {
                try
                {
                    computeDuplicateFiles( artifact.getFile() );
                }
                catch ( Exception e )
                {
                    getLog().warn( "Cannot compute duplicates files from " + artifact.getFile().getAbsolutePath(), e );
                }
            }
            jarFiles.add( artifact.getFile() );
        }

        // Check duplicates.
        if ( extractDuplicates )
        {
            List<String> duplicates = new ArrayList<String>();
            List<File> jarToModify = new ArrayList<File>();
            for ( String s : jars.keySet() )
            {
                List<File> l = jars.get( s );
                if ( l.size() > 1 )
                {
                    getLog().warn( "Duplicate file " + s + " : " + l );
                    duplicates.add( s );
                    for ( int i = 1; i < l.size(); i++ )
                    {
                        if ( ! jarToModify.contains( l.get( i ) ) )
                        {
                            jarToModify.add( l.get( i ) );
                        }
                    }
                }
            }

            // Rebuild jars.
            for ( File file : jarToModify )
            {
                File newJar;
                newJar = removeDuplicatesFromJar( file, duplicates );
                int index = jarFiles.indexOf( file );
                if ( newJar != null )
                {
                    jarFiles.set( index, newJar );
                }

            }
        }
        
        String debugKeyStore;
        ApkBuilder apkBuilder; 
        try 
        {
            debugKeyStore = ApkBuilder.getDebugKeystore();
            apkBuilder = 
                    new ApkBuilder( outputFile, zipArchive, dexFile, 
                            ( signWithDebugKeyStore ) ? debugKeyStore : null, null );
            if ( apkDebug )
            {
                apkBuilder.setDebugMode( apkDebug );
            }

            for ( File sourceFolder : sourceFolders )
            {
                apkBuilder.addSourceFolder( sourceFolder );
            }
     
            for ( File jarFile : jarFiles )
            {
                boolean excluded = false;
              
                if ( excludeJarResourcesPatterns != null )
                {
                    final String name = jarFile.getName();
                    getLog().debug( "Checking " + name + " against patterns" );
                    for ( Pattern pattern : excludeJarResourcesPatterns )
                    {
                        final Matcher matcher = pattern.matcher( name );
                        if ( matcher.matches() ) 
                        {
                            getLog().debug( "Jar " + name + " excluded by pattern " + pattern );
                            excluded = true;
                            break;
                        } 
                        else 
                        {
                            getLog().debug( "Jar " + name + " not excluded by pattern " + pattern );
                        }
                    }
                }
    
                if ( excluded )
                {
                    continue;
                }
                
                if ( jarFile.isDirectory() )
                {
                    String[] filenames = jarFile.list( new FilenameFilter()
                    {
                        public boolean accept( File dir, String name )
                        {
                            return PATTERN_JAR_EXT.matcher( name ).matches();
                        }
                    } );
    
                    for ( String filename : filenames )
                    {
                        apkBuilder.addResourcesFromJar( new File( jarFile, filename ) );
                    }
                }
                else
                {
                    apkBuilder.addResourcesFromJar( jarFile );
                }
            }

            for ( File nativeFolder : nativeFolders )
            {
                apkBuilder.addNativeLibraries( nativeFolder );
            }
            apkBuilder.sealApk();
        } 
        catch ( ApkCreationException e )
        {
            throw new MojoExecutionException( e.getMessage() );
        } 
        catch ( DuplicateFileException e )
        {
            final String msg = String.format( "Duplicated file: %s, found in archive %s and %s",
                    e.getArchivePath(), e.getFile1(), e.getFile2() );
            throw new MojoExecutionException( msg, e );
        } 
        catch ( SealedApkException e )
        {
            throw new MojoExecutionException( e.getMessage() );
        }
    }

    private File removeDuplicatesFromJar( File in, List<String> duplicates )
    {
        String target = project.getBuild().getOutputDirectory();
        File tmp = new File( target, "unpacked-embedded-jars" );
        tmp.mkdirs();
        File out = new File( tmp, in.getName() );

        if ( out.exists() )
        {
            return out;
        }
        else
        {
            try
            {
                out.createNewFile();
            }
            catch ( IOException e )
            {
                e.printStackTrace();
            }
        }

        // Create a new Jar file
        FileOutputStream fos = null;
        ZipOutputStream jos = null;
        try
        {
            fos = new FileOutputStream( out );
            jos = new ZipOutputStream( fos );
        }
        catch ( FileNotFoundException e1 )
        {
            getLog().error( "Cannot remove duplicates : the output file " + out.getAbsolutePath() + " does not found" );
            return null;
        }

        ZipFile inZip = null;
        try
        {
            inZip = new ZipFile( in );
            Enumeration<? extends ZipEntry> entries = inZip.entries();
            while ( entries.hasMoreElements() )
            {
                ZipEntry entry = entries.nextElement();
                // If the entry is not a duplicate, copy.
                if ( ! duplicates.contains( entry.getName() ) )
                {
                    // copy the entry header to jos
                    jos.putNextEntry( entry );
                    InputStream currIn = inZip.getInputStream( entry );
                    copyStreamWithoutClosing( currIn, jos );
                    currIn.close();
                    jos.closeEntry();
                }
            }
        }
        catch ( IOException e )
        {
            getLog().error( "Cannot removing duplicates : " + e.getMessage() );
            return null;
        }

        try
        {
            if ( inZip != null )
            {
                inZip.close();
            }
            jos.close();
            fos.close();
            jos = null;
            fos = null;
        }
        catch ( IOException e )
        {
            // ignore it.
        }
        getLog().info( in.getName() + " rewritten without duplicates : " + out.getAbsolutePath() );
        return out;
    }

    /**
     * Copies an input stream into an output stream but does not close the streams.
     *
     * @param in  the input stream
     * @param out the output stream
     * @throws IOException if the stream cannot be copied
     */
    private static void copyStreamWithoutClosing( InputStream in, OutputStream out ) throws IOException
    {
        final int bufferSize = 4096;
        byte[] b = new byte[ bufferSize ];
        int n;
        while ( ( n = in.read( b ) ) != - 1 )
        {
            out.write( b, 0, n );
        }
    }

    private void processNativeLibraries( final List<File> natives ) throws MojoExecutionException
    {
        for ( String ndkArchitecture : AndroidNdk.NDK_ARCHITECTURES )
        {
            processNativeLibraries( natives, ndkArchitecture );
        }
    }

    private void addNativeDirectory( final List<File> natives, final File nativeDirectory )
    {
        if ( ! natives.contains( nativeDirectory ) )
        {
            natives.add( nativeDirectory );
        }
    }

    private void processNativeLibraries( final List<File> natives, String ndkArchitecture )
            throws MojoExecutionException
    {
        // Examine the native libraries directory for content. This will only be true if:
        // a) the directory exists
        // b) it contains at least 1 file
        final boolean hasValidNativeLibrariesDirectory = hasValidNativeLibrariesDirectory();
        final boolean hasValidBuildNativeLibrariesDirectory = hasValidBuildNativeLibrariesDirectory();
        final Set<Artifact> artifacts = getNativeLibraryArtifacts();

        if ( artifacts.isEmpty() && hasValidNativeLibrariesDirectory && ! hasValidBuildNativeLibrariesDirectory )
        {

            getLog().debug(
                    "No native library dependencies detected, will point directly to " + nativeLibrariesDirectory );

            // Point directly to the directory in this case - no need to copy files around
            addNativeDirectory( natives, nativeLibrariesDirectory );

            // FIXME: This would pollute a libs folder which is under source control
            // FIXME: Would be better to not support this case?
            optionallyCopyGdbServer( nativeLibrariesDirectory, ndkArchitecture );

        }
        else
        {
            if ( ! artifacts.isEmpty() || hasValidNativeLibrariesDirectory )
            {
                // In this case, we may have both .so files in it's normal location
                // as well as .so dependencies

                final File destinationDirectory = makeNativeLibrariesOutputDirectory();

                // Point directly to the directory
                addNativeDirectory( natives, destinationDirectory );

                // If we have a valid native libs, copy those files - these already come in the structure required
                if ( hasValidNativeLibrariesDirectory )
                {
                    copyLocalNativeLibraries( nativeLibrariesDirectory, destinationDirectory );
                }

                if ( ! artifacts.isEmpty() )
                {
                    for ( Artifact resolvedArtifact : artifacts )
                    {
                        if ( NativeHelper.artifactHasHardwareArchitecture( resolvedArtifact,
                                ndkArchitecture, nativeLibrariesDependenciesHardwareArchitectureDefault ) )
                        {
                            copyNativeLibraryArtifactFileToDirectory( resolvedArtifact, destinationDirectory,
                                    ndkArchitecture );
                        }
                        else if ( APKLIB.equals( resolvedArtifact.getType() )
                                || AAR.equals( resolvedArtifact.getType() ) )
                        {
                                addNativeDirectory( natives, getUnpackedLibNativesFolder( resolvedArtifact ) );
                        }
                    }
                }

                // Finally, think about copying the gdbserver binary into the APK output as well
                optionallyCopyGdbServer( destinationDirectory, ndkArchitecture );
            }
        }
    }

    /**
     * Examine the native libraries directory for content. This will only be true if:
     * <ul>
     * <li>The directory exists</li>
     * <li>It contains at least 1 file</li>
     * </ul>
     */
    private boolean hasValidNativeLibrariesDirectory()
    {
        return nativeLibrariesDirectory != null
                && nativeLibrariesDirectory.exists()
                && ( nativeLibrariesDirectory.listFiles() != null && nativeLibrariesDirectory.listFiles().length > 0 );
    }

    private boolean hasValidBuildNativeLibrariesDirectory()
    {
        return nativeLibrariesOutputDirectory.exists() && (
                nativeLibrariesOutputDirectory.listFiles() != null
                        && nativeLibrariesOutputDirectory.listFiles().length > 0 );
    }

    /**
     * @return Any native dependencies or attached artifacts. This may include artifacts from the ndk-build MOJO.
     * @throws MojoExecutionException
     */
    private Set<Artifact> getNativeLibraryArtifacts() throws MojoExecutionException
    {
        return new NativeHelper( project, projectRepos, repoSession, repoSystem, artifactFactory, getLog() )
                .getNativeDependenciesArtifacts( unpackedLibsDirectory, true );
    }

    /**
     * Create the ${project.build.outputDirectory}/libs directory.
     *
     * @return File reference to the native libraries output directory.
     */
    private File makeNativeLibrariesOutputDirectory()
    {
        final File destinationDirectory = new File( nativeLibrariesOutputDirectory.getAbsolutePath() );
        destinationDirectory.mkdirs();
        return destinationDirectory;
    }

    private void copyNativeLibraryArtifactFileToDirectory( Artifact artifact, File destinationDirectory,
                                                           String ndkArchitecture ) throws MojoExecutionException
    {
        final File artifactFile = artifact.getFile();
        try
        {
            final String artifactId = artifact.getArtifactId();
            String filename = artifactId.startsWith( "lib" )
                    ? artifactId + ".so"
                    : "lib" + artifactId + ".so";
            if ( ndkFinalLibraryName != null
                    && artifact.getFile().getName().startsWith( "lib" + ndkFinalLibraryName ) )
            {
                // The artifact looks like one we built with the NDK in this module
                // preserve the name from the NDK build
                filename = artifact.getFile().getName();
            }

            final File finalDestinationDirectory = getFinalDestinationDirectoryFor( artifact,
                    destinationDirectory, ndkArchitecture );
            final File file = new File( finalDestinationDirectory, filename );
            getLog().debug(
                    "Copying native dependency " + artifactId + " (" + artifact.getGroupId()
                            +
                            ") to " + file );
            FileUtils.copyFile( artifactFile, file );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Could not copy native dependency.", e );
        }
    }


    private void optionallyCopyGdbServer( File destinationDirectory, String architecture ) throws MojoExecutionException
    {

        try
        {
            final File destDir = new File( destinationDirectory, architecture );
            if ( apkDebug && destDir.exists() )
            {
                // Copy the gdbserver binary to libs/<architecture>/
                final File gdbServerFile = getAndroidNdk().getGdbServer( architecture );
                final File destFile = new File( destDir, "gdbserver" );
                if ( ! destFile.exists() )
                {
                    FileUtils.copyFile( gdbServerFile, destFile );
                }
                else
                {
                    getLog().info( "Note: gdbserver binary already exists at destination, will not copy over" );
                }
            }
        }
        catch ( Exception e )
        {
            getLog().error( "Error while copying gdbserver: " + e.getMessage(), e );
            throw new MojoExecutionException( "Error while copying gdbserver: " + e.getMessage(), e );
        }

    }

    private File getFinalDestinationDirectoryFor( Artifact resolvedArtifact, File destinationDirectory,
                                                  String ndkArchitecture )
    {
        File finalDestinationDirectory = new File( destinationDirectory, ndkArchitecture + "/" );
        finalDestinationDirectory.mkdirs();
        return finalDestinationDirectory;
    }

    private void copyLocalNativeLibraries( final File localNativeLibrariesDirectory, final File destinationDirectory )
            throws MojoExecutionException
    {
        getLog().debug( "Copying existing native libraries from " + localNativeLibrariesDirectory );
        try
        {

            IOFileFilter libSuffixFilter = FileFilterUtils.suffixFileFilter( ".so" );

            IOFileFilter gdbserverNameFilter = FileFilterUtils.nameFileFilter( "gdbserver" );
            IOFileFilter orFilter = FileFilterUtils.or( libSuffixFilter, gdbserverNameFilter );

            IOFileFilter libFiles = FileFilterUtils.and( FileFileFilter.FILE, orFilter );
            FileFilter filter = FileFilterUtils.or( DirectoryFileFilter.DIRECTORY, libFiles );
            org.apache.commons.io.FileUtils
                    .copyDirectory( localNativeLibrariesDirectory, destinationDirectory, filter );

        }
        catch ( IOException e )
        {
            getLog().error( "Could not copy native libraries: " + e.getMessage(), e );
            throw new MojoExecutionException( "Could not copy native dependency.", e );
        }
    }


    /**
     * Generates an intermediate apk file (actually .ap_) containing the resources and assets.
     *
     * @throws MojoExecutionException
     */
    private void generateIntermediateApk() throws MojoExecutionException
    {
        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger( this.getLog() );
        File[] overlayDirectories = getResourceOverlayDirectories();

        File androidJar = getAndroidSdk().getAndroidJar();
        File outputFile = new File( project.getBuild().getDirectory(), project.getBuild().getFinalName() + ".ap_" );

        List<String> commands = new ArrayList<String>();
        commands.add( "package" );
        commands.add( "-f" );
        commands.add( "-M" );
        commands.add( androidManifestFile.getAbsolutePath() );
        for ( File resOverlayDir : overlayDirectories )
        {
            if ( resOverlayDir != null && resOverlayDir.exists() )
            {
                commands.add( "-S" );
                commands.add( resOverlayDir.getAbsolutePath() );
            }
        }
        if ( resourceDirectory.exists() )
        {
            commands.add( "-S" );
            commands.add( resourceDirectory.getAbsolutePath() );
        }
        for ( Artifact artifact : getAllRelevantDependencyArtifacts() )
        {
            if ( artifact.getType().equals( APKLIB ) || artifact.getType().equals( AAR ) )
            {
                final File apkLibResDir = getUnpackedLibResourceFolder( artifact );
                if ( apkLibResDir.exists() )
                {
                    commands.add( "-S" );
                    commands.add( apkLibResDir.getAbsolutePath() );
                }
            }
        }
        commands.add( "--auto-add-overlay" );

        // NB aapt only accepts a single assets parameter - combinedAssets is a merge of all assets
        if ( combinedAssets.exists() )
        {
            getLog().debug( "Adding assets folder : " + combinedAssets );
            commands.add( "-A" );
            commands.add( combinedAssets.getAbsolutePath() );
        }

        if ( StringUtils.isNotBlank( renameManifestPackage ) )
        {
            commands.add( "--rename-manifest-package" );
            commands.add( renameManifestPackage );
        }

        if ( StringUtils.isNotBlank( renameInstrumentationTargetPackage ) )
        {
            commands.add( "--rename-instrumentation-target-package" );
            commands.add( renameInstrumentationTargetPackage );
        }

        commands.add( "-I" );
        commands.add( androidJar.getAbsolutePath() );
        commands.add( "-F" );
        commands.add( outputFile.getAbsolutePath() );
        if ( StringUtils.isNotBlank( configurations ) )
        {
            commands.add( "-c" );
            commands.add( configurations );
        }

        for ( String aaptExtraArg : aaptExtraArgs )
        {
            commands.add( aaptExtraArg );
        }

        if ( !release )
        {
            getLog().info( "Enabling debug build for apk." );
            commands.add( "--debug-mode" );
        }
        else 
        {
            getLog().info( "Enabling release build for apk." );
        }

        getLog().info( getAndroidSdk().getAaptPath() + " " + commands.toString() );
        try
        {
            executor.executeCommand( getAndroidSdk().getAaptPath(), commands, project.getBasedir(), false );
        }
        catch ( ExecutionException e )
        {
            throw new MojoExecutionException( "", e );
        }
    }

    private void processApkLibAssets() throws MojoExecutionException
    {
        // Next pull APK Lib assets, reverse the order to give precedence to libs higher up the chain
        List<Artifact> artifactList = new ArrayList<Artifact>( getAllRelevantDependencyArtifacts() );
        for ( Artifact artifact : artifactList )
        {
            if ( artifact.getType().equals( APKLIB ) || artifact.getType().equals( AAR ) )
            {
                final File apklibAsssetsDirectory = getUnpackedLibAssetsFolder( artifact );
                if ( apklibAsssetsDirectory.exists() )
                {
                    try
                    {
                        getLog().info( "Copying dependency assets files to combined assets directory." );
                        org.apache.commons.io.FileUtils
                                .copyDirectory( apklibAsssetsDirectory, combinedAssets, new FileFilter()
                                {
                                    /**
                                     * Excludes files matching one of the common file to exclude.
                                     * The default excludes pattern are the ones from
                                     * {org.codehaus.plexus.util.AbstractScanner#DEFAULTEXCLUDES}
                                     * @see java.io.FileFilter#accept(java.io.File)
                                     */
                                    public boolean accept( File file )
                                    {
                                        for ( String pattern : AbstractScanner.DEFAULTEXCLUDES )
                                        {
                                            if ( AbstractScanner.match( pattern, file.getAbsolutePath() ) )
                                            {
                                                getLog().debug( "Excluding " + file.getName() + " from asset copy : "
                                                        + "matching " + pattern );
                                                return false;
                                            }
                                        }

                                        return true;

                                    }
                                } );
                    }
                    catch ( IOException e )
                    {
                        throw new MojoExecutionException( "", e );
                    }

                }
            }
        }
    }


    protected AndroidSigner getAndroidSigner()
    {
        if ( sign == null )
        {
            return new AndroidSigner( signDebug );
        }
        else
        {
            return new AndroidSigner( sign.getDebug() );
        }
    }

    private MetaInf getDefaultMetaInf()
    {
        // check for deprecated first
        if ( apkMetaIncludes != null && apkMetaIncludes.length > 0 )
        {
            return new MetaInf().include( apkMetaIncludes );
        }

        return this.pluginMetaInf;
    }
}
