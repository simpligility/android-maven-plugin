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
package com.simpligility.maven.plugins.android.phase09package;

import com.android.sdklib.build.ApkBuilder;
import com.android.sdklib.build.ApkCreationException;
import com.android.sdklib.build.DuplicateFileException;
import com.android.sdklib.build.SealedApkException;
import com.simpligility.maven.plugins.android.AbstractAndroidMojo;
import com.google.common.io.Files;
import com.simpligility.maven.plugins.android.AndroidNdk;
import com.simpligility.maven.plugins.android.AndroidSigner;
import com.simpligility.maven.plugins.android.IncludeExcludeSet;
import com.simpligility.maven.plugins.android.CommandExecutor;
import com.simpligility.maven.plugins.android.ExecutionException;
import com.simpligility.maven.plugins.android.common.AaptCommandBuilder;
import com.simpligility.maven.plugins.android.common.AndroidExtension;
import com.simpligility.maven.plugins.android.common.NativeHelper;
import com.simpligility.maven.plugins.android.config.ConfigHandler;
import com.simpligility.maven.plugins.android.config.ConfigPojo;
import com.simpligility.maven.plugins.android.config.PullParameter;
import com.simpligility.maven.plugins.android.configuration.Apk;
import com.simpligility.maven.plugins.android.configuration.MetaInf;
import com.simpligility.maven.plugins.android.configuration.Sign;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.plugins.shade.resource.ResourceTransformer;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static com.simpligility.maven.plugins.android.InclusionExclusionResolver.filterArtifacts;
import static com.simpligility.maven.plugins.android.common.AndroidExtension.AAR;
import static com.simpligility.maven.plugins.android.common.AndroidExtension.APK;
import static com.simpligility.maven.plugins.android.common.AndroidExtension.APKLIB;




/**
 * Creates the apk file. By default signs it with debug keystore.<br/>
 * Change that by setting configuration parameter
 * <code>&lt;sign&gt;&lt;debug&gt;false&lt;/debug&gt;&lt;/sign&gt;</code>.
 *
 * @author hugo.josefson@jayway.com
 */
@Mojo( name = "apk",
       defaultPhase = LifecyclePhase.PACKAGE,
       requiresDependencyResolution = ResolutionScope.COMPILE )
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
     */
    @Parameter
    private Sign sign;

    /**
     * <p>Parameter designed to pick up <code>-Dandroid.sign.debug</code> in case there is no pom with a
     * <code>&lt;sign&gt;</code> configuration tag.</p>
     * <p>Corresponds to {@link com.simpligility.maven.plugins.android.configuration.Sign#debug}.</p>
     */
    @Parameter( property = "android.sign.debug", defaultValue = "auto", readonly = true )
    private String signDebug;

    /**
     * <p>Rewrite the manifest so that all of its instrumentation components target the given package.
     * This value will be passed on to the aapt parameter --rename-instrumentation-target-package.
     * Look to aapt for more help on this. </p>
     *
     * TODO pass this into AaptExecutor
     */
    @Parameter( property = "android.renameInstrumentationTargetPackage" )
    private String renameInstrumentationTargetPackage;

    /**
     * <p>Allows to detect and extract the duplicate files from embedded jars. In that case, the plugin analyzes
     * the content of all embedded dependencies and checks they are no duplicates inside those dependencies. Indeed,
     * Android does not support duplicates, and all dependencies are inlined in the APK. If duplicates files are found,
     * the resource is kept in the first dependency and removes from others.
     */
    @Parameter( property = "android.extractDuplicates", defaultValue = "false" )
    private boolean extractDuplicates;

    /**
     * <p>Classifier to add to the artifact generated. If given, the artifact will be an attachment instead.</p>
     */
    @Parameter
    private String classifier;

    /**
     * The apk file produced by the apk goal. Per default the file is placed into the build directory (target
     * normally) using the build final name and apk as extension.
     */
    @Parameter( property = "android.outputApk",
                defaultValue = "${project.build.directory}/${project.build.finalName}.apk" )
    private String outputApk;

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
     */
    @Parameter( property = "android.sourceDirectories" )
    private File[] sourceDirectories;

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
     * @deprecated in favour of apk.metaInf
     */
    @PullParameter
    private String[] apkMetaIncludes;

    @PullParameter( defaultValueGetterMethod = "getDefaultMetaInf" )
    private MetaInf apkMetaInf;

    @Parameter( alias = "metaInf" )
    private MetaInf pluginMetaInf;

    /**
     * Defines whether or not the APK is being produced in debug mode or not.
     */
    @Parameter( property = "android.apk.debug" )
    @PullParameter( defaultValue = "false" )
    private Boolean apkDebug;

    @Parameter( property = "android.nativeToolchain" )
    @PullParameter( defaultValue = "arm-linux-androideabi-4.4.3" )
    private String apkNativeToolchain;

    /**
     * Specifies the final name of the library output by the build (this allows
     */
    @Parameter( property = "android.ndk.build.build.final-library.name" )
    private String ndkFinalLibraryName;

    /**
     * Specify a list of patterns that are matched against the names of jar file
     * dependencies. Matching jar files will not have their resources added to the
     * resulting APK.
     *
     * The patterns are standard Java regexes.
     */
    @Parameter
    private String[] excludeJarResources;

    private Pattern[] excludeJarResourcesPatterns;

    /**
     * Embedded configuration of this mojo.
     */
    @Parameter
    @ConfigPojo( prefix = "apk" )
    private Apk apk;

    /**
     * Skips transitive dependencies. May be useful if the target classes directory is populated with the
     * {@code maven-dependency-plugin} and already contains all dependency classes.
     */
    @Parameter( property = "skipDependencies", defaultValue = "false" )
    private boolean skipDependencies;

    /**
     * Allows to include or exclude artifacts by type. The {@code include} parameter has higher priority than the
     * {@code exclude} parameter. These two parameters can be overridden by the {@code artifactSet} parameter. Empty
     * strings are ignored. Example:
     * <pre>
     *     &lt;artifactTypeSet&gt;
     *         &lt;includes&gt;
     *             &lt;include&gt;aar&lt;/include&gt;
     *         &lt;includes&gt;
     *         &lt;excludes&gt;
     *             &lt;exclude&gt;jar&lt;/exclude&gt;
     *         &lt;excludes&gt;
     *     &lt;/artifactTypeSet&gt;
     * </pre>
     */
    @Parameter( property = "artifactTypeSet" )
    private IncludeExcludeSet artifactTypeSet;

    /**
     * Allows to include or exclude artifacts by {@code groupId}, {@code artifactId}, and {@code versionId}. The
     * {@code include} parameter has higher priority than the {@code exclude} parameter. These two parameters can
     * override the {@code artifactTypeSet} and {@code skipDependencies} parameters. Artifact {@code groupId},
     * {@code artifactId}, and {@code versionId} are specified by a string with the respective values separated using
     * a colon character {@code :}. {@code artifactId} and {@code versionId} can be optional covering an artifact
     * range. Empty strings are ignored. Example:
     * <pre>
     *     &lt;artifactTypeSet&gt;
     *         &lt;includes&gt;
     *             &lt;include&gt;foo-group:foo-artifact:1.0-SNAPSHOT&lt;/include&gt;
     *             &lt;include&gt;bar-group:bar-artifact:1.0-SNAPSHOT&lt;/include&gt;
     *             &lt;include&gt;baz-group:*&lt;/include&gt;
     *         &lt;includes&gt;
     *         &lt;excludes&gt;
     *             &lt;exclude&gt;qux-group:qux-artifact:*&lt;/exclude&gt;
     *         &lt;excludes&gt;
     *     &lt;/artifactTypeSet&gt;
     * </pre>
     */
    @Parameter( property = "artifactSet" )
    private IncludeExcludeSet artifactSet;

    private static final Pattern PATTERN_JAR_EXT = Pattern.compile( "^.+\\.jar$", Pattern.CASE_INSENSITIVE );

    private static final String DEX_SUFFIX = ".dex";

    private static final String CLASSES = "classes";

    /**
     * <p>Default hardware architecture for native library dependencies (with {@code &lt;type>so&lt;/type>})
     * without a classifier.</p>
     * <p>Valid values currently include {@code armeabi}, {@code armeabi-v7a}, {@code mips} and {@code x86}.</p>
     */
    @Parameter( property = "android.nativeLibrariesDependenciesHardwareArchitectureDefault", defaultValue = "armeabi" )
    private String nativeLibrariesDependenciesHardwareArchitectureDefault;

    @Parameter
    private ResourceTransformer[] transformers;

    /**
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
        File outputFile = new File( outputApk );
        final boolean signWithDebugKeyStore = getAndroidSigner().isSignWithDebugKeyStore();

        if ( getAndroidSigner().shouldCreateBothSignedAndUnsignedApk() )
        {
            getLog().info( "Creating debug key signed apk file " + outputFile );
            createApkFile( outputFile, true );
            final File unsignedOutputFile = new File( targetDirectory,
                    finalName + "-unsigned." + APK );
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
            projectHelper.attachArtifact( project, AndroidExtension.APK, classifier, outputFile );
        }
    }

    void createApkFile( File outputFile, boolean signWithDebugKeyStore ) throws MojoExecutionException
    {
        //this needs to come from DexMojo
        File dexFile = new File( targetDirectory, "classes.dex" );
        if ( !dexFile.exists() )
        {
            dexFile = new File( targetDirectory, "classes.zip" );
        }

        File zipArchive = new File( targetDirectory, finalName + ".ap_" );
        ArrayList<File> sourceFolders = new ArrayList<File>();
        if ( sourceDirectories != null )
        {
            sourceFolders.addAll( Arrays.asList( sourceDirectories ) );
        }
        ArrayList<File> jarFiles = new ArrayList<File>();

        // Process the native libraries, looking both in the current build directory as well as
        // at the dependencies declared in the pom.  Currently, all .so files are automatically included
        final Collection<File> nativeFolders = getNativeLibraryFolders();
        getLog().info( "Adding native libraries : " + nativeFolders );

        doAPKWithAPKBuilder( outputFile, dexFile, zipArchive, sourceFolders, jarFiles, nativeFolders,
                signWithDebugKeyStore );
        
        if ( this.apkMetaInf != null )
        {
            File outputJar = new File( outputApk.substring( 0, outputApk.length() - 3 ) + "jar" );
            if ( outputJar.exists() ) 
            {
                jarFiles.add( outputJar );
            } 
            else 
            {
                getLog().warn( "Output jar doesn't exist:" + outputJar );
            }
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
        JarOutputStream zos = new JarOutputStream( fos );
        Set<String> entries = new HashSet<String>();

        updateWithMetaInf( zos, outputFile, entries, false );

        for ( File f : jarFiles )
        {
            updateWithMetaInf( zos, f, entries, true );
        }

        if ( transformers != null )
        {
            for ( ResourceTransformer transformer : transformers )
            {
                if ( transformer.hasTransformedResource() )
                {
                    transformer.modifyOutputStream( zos );
                }
            }
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

                if ( ! this.apkMetaInf.isIncluded( zn ) )
                {
                    continue;
                }
            }



            boolean resourceTransformed = false;

            if ( transformers != null )
            {
                for ( ResourceTransformer transformer : transformers )
                {
                    if ( transformer.canTransformResource( zn ) )
                    {
                        getLog().info( "Transforming " + zn + " using " + transformer.getClass().getName() );
                        InputStream is = zin.getInputStream( ze );
                        transformer.processResource( zn, is, null );
                        is.close();
                        resourceTransformed = true;
                        break;
                    }
                }
            }

            if ( !resourceTransformed )
            {
                // Avoid duplicates that aren't accounted for by the resource transformers
                if ( metaInfOnly && this.extractDuplicates && ! entries.add( zn ) )
                {
                    continue;
                }

                InputStream is = zin.getInputStream( ze );

                final ZipEntry ne;
                if ( ze.getMethod() == ZipEntry.STORED )
                {
                    ne = new ZipEntry( ze );
                }
                else
                {
                    ne = new ZipEntry( zn );
                }

                zos.putNextEntry( ne );

                copyStreamWithoutClosing( is, zos );

                is.close();
                zos.closeEntry();
            }
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

    private void computeDuplicateFilesInSource( File folder )
    {
        String rPath = folder.getAbsolutePath();
        for ( File file : Files.fileTreeTraverser().breadthFirstTraversal( folder ).toList() )
        {
            String lPath = file.getAbsolutePath();
            if ( lPath.equals( rPath ) )
            {
                continue; //skip the root
            }
            lPath = lPath.substring( rPath.length() + 1 ); //strip root folder to make relative path

            if ( jars.get( lPath ) == null )
            {
                jars.put( lPath, new ArrayList<File>() );
            }
            jars.get( lPath ).add( folder );
        }
    }

    private void extractDuplicateFiles( List<File> jarFiles, Collection<File> sourceFolders ) throws IOException
    {
        getLog().debug( "Extracting duplicates" );
        List<String> duplicates = new ArrayList<String>();
        List<File> jarToModify = new ArrayList<File>();
        for ( String s : jars.keySet() )
        {
            List<File> l = jars.get( s );
            if ( l.size() > 1 )
            {
                getLog().warn( "Duplicate file " + s + " : " + l );
                duplicates.add( s );
                for ( int i = 0; i < l.size(); i++ )
                {
                    if ( ! jarToModify.contains( l.get( i ) ) )
                    {
                        jarToModify.add( l.get( i ) );
                    }
                }
            }
        }

        // Rebuild jars.  Remove duplicates from ALL jars, then add them back into a duplicate-resources.jar
        File tmp = new File( targetDirectory.getAbsolutePath(), "unpacked-embedded-jars" );
        tmp.mkdirs();
        File duplicatesJar = new File( tmp, "duplicate-resources.jar" );
        Set<String> duplicatesAdded = new HashSet<String>();

        duplicatesJar.createNewFile();
        final FileOutputStream fos = new FileOutputStream( duplicatesJar );
        final JarOutputStream zos = new JarOutputStream( fos );

        for ( File file : jarToModify )
        {
            final int index = jarFiles.indexOf( file );
            if ( index != -1 )
            {
                final File newJar = removeDuplicatesFromJar( file, duplicates, duplicatesAdded, zos, index );
                getLog().debug( "Removed duplicates from " + newJar );
                if ( newJar != null )
                {
                    jarFiles.set( index, newJar );
                }
            }
            else
            {
                removeDuplicatesFromFolder( file, file, duplicates, duplicatesAdded, zos );
                getLog().debug( "Removed duplicates from " + file );
            }
        }
        //add transformed resources to duplicate-resources.jar
        if ( transformers != null )
        {
            for ( ResourceTransformer transformer : transformers )
            {
                if ( transformer.hasTransformedResource() )
                {
                    transformer.modifyOutputStream( zos );
                }
            }
        }
        zos.close();
        fos.close();

        if ( !jarToModify.isEmpty() && duplicatesJar.length() > 0 )
        {
            jarFiles.add( duplicatesJar );
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
    private void doAPKWithAPKBuilder( File outputFile, File dexFile, File zipArchive, Collection<File> sourceFolders,
                                      List<File> jarFiles, Collection<File> nativeFolders,
                                      boolean signWithDebugKeyStore ) throws MojoExecutionException
    {
        getLog().debug( "Building APK with internal APKBuilder" );
        
        //A when jack is running the classes directory will not get filled (usually)
        // so let's skip it if it wasn't created by something else
        if ( projectOutputDirectory.exists() || !getJack().isEnabled() ) 
        {
            sourceFolders.add( projectOutputDirectory );
        }

        for ( Artifact artifact : filterArtifacts( getRelevantCompileArtifacts(), skipDependencies,
                artifactTypeSet.getIncludes(), artifactTypeSet.getExcludes(), artifactSet.getIncludes(),
                artifactSet.getExcludes() ) )
        {
            getLog().debug( "Found artifact for APK :" + artifact );
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

        for ( File src : sourceFolders )
        {
            computeDuplicateFilesInSource( src );
        }

        // Check duplicates.
        if ( extractDuplicates )
        {
            try
            {
                extractDuplicateFiles( jarFiles, sourceFolders );
            }
            catch ( IOException e )
            {
                getLog().error( "Could not extract duplicates to duplicate-resources.jar", e );
            }
        }

        try
        {
            final String debugKeyStore = signWithDebugKeyStore ? ApkBuilder.getDebugKeystore() : null;
            final ApkBuilder apkBuilder = new ApkBuilder( outputFile, zipArchive, dexFile, debugKeyStore, null );
            if ( apkDebug )
            {
                apkBuilder.setDebugMode( true );
            }

            for ( File sourceFolder : sourceFolders )
            {
                getLog().debug( "Adding source folder : " + sourceFolder );
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
                    getLog().debug( "Adding resources from jar folder : " + jarFile );
                    final String[] filenames = jarFile.list( new FilenameFilter()
                    {
                        public boolean accept( File dir, String name )
                        {
                            return PATTERN_JAR_EXT.matcher( name ).matches();
                        }
                    } );

                    for ( String filename : filenames )
                    {
                        final File innerJar = new File( jarFile, filename );
                        getLog().debug( "Adding resources from innerJar : " + innerJar );
                        apkBuilder.addResourcesFromJar( innerJar );
                    }
                }
                else
                {
                    getLog().debug( "Adding resources from : " + jarFile );
                    apkBuilder.addResourcesFromJar( jarFile );
                }
            }

            addSecondaryDexes( dexFile, apkBuilder );

            for ( File nativeFolder : nativeFolders )
            {
                getLog().debug( "Adding native library : " + nativeFolder );
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

    private void addSecondaryDexes( File dexFile, ApkBuilder apkBuilder ) throws ApkCreationException,
            SealedApkException, DuplicateFileException
    {
        int dexNumber = 2;
        String dexFileName = getNextDexFileName( dexNumber );
        File secondDexFile = createNextDexFile( dexFile, dexFileName );
        while ( secondDexFile.exists() )
        {
            apkBuilder.addFile( secondDexFile, dexFileName );
            dexNumber++;
            dexFileName = getNextDexFileName( dexNumber );
            secondDexFile = createNextDexFile( dexFile, dexFileName );
        }
    }

    private File createNextDexFile( File dexFile, String dexFileName )
    {
        return new File( dexFile.getParentFile(), dexFileName );
    }

    private String getNextDexFileName( int dexNumber )
    {
        return CLASSES + dexNumber + DEX_SUFFIX;
    }

    private File removeDuplicatesFromJar( File in, List<String> duplicates,
                                          Set<String> duplicatesAdded, ZipOutputStream duplicateZos, int num )
    {
        String target = targetDirectory.getAbsolutePath();
        File tmp = new File( target, "unpacked-embedded-jars" );
        tmp.mkdirs();
        String jarName = String.format( "%s-%d.%s",
           Files.getNameWithoutExtension( in.getName() ), num, Files.getFileExtension( in.getName() ) );
        File out = new File( tmp, jarName );

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
        final FileOutputStream fos;
        final ZipOutputStream jos;
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

        final ZipFile inZip;
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
                //if it is duplicate, check the resource transformers
                else
                {
                    boolean resourceTransformed = false;
                    if ( transformers != null )
                    {
                        for ( ResourceTransformer transformer : transformers )
                        {
                            if ( transformer.canTransformResource( entry.getName() ) )
                            {
                                getLog().info( "Transforming " + entry.getName()
                                        + " using " + transformer.getClass().getName() );
                                InputStream currIn = inZip.getInputStream( entry );
                                transformer.processResource( entry.getName(), currIn, null );
                                currIn.close();
                                resourceTransformed = true;
                                break;
                            }
                        }
                    }
                    //if not handled by transformer, add (once) to duplicates jar
                    if ( !resourceTransformed )
                    {
                        if ( !duplicatesAdded.contains( entry.getName() ) )
                        {
                            duplicatesAdded.add( entry.getName() );
                            duplicateZos.putNextEntry( entry );
                            InputStream currIn = inZip.getInputStream( entry );
                            copyStreamWithoutClosing( currIn, duplicateZos );
                            currIn.close();
                            duplicateZos.closeEntry();
                        }
                    }
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
            inZip.close();
            jos.close();
            fos.close();
        }
        catch ( IOException e )
        {
            // ignore it.
        }
        getLog().info( in.getName() + " rewritten without duplicates : " + out.getAbsolutePath() );
        return out;
    }

    private void removeDuplicatesFromFolder( File root, File in, List<String> duplicates,
       Set<String> duplicatesAdded, ZipOutputStream duplicateZos )
    {
        String rPath = root.getAbsolutePath();
        try
        {
            for ( File f : in.listFiles() )
            {
                if ( f.isDirectory() )
                {
                    removeDuplicatesFromFolder( root, f, duplicates, duplicatesAdded, duplicateZos );
                }
                else
                {
                    String lName = f.getAbsolutePath();
                    lName = lName.substring( rPath.length() + 1 ); //make relative path
                    if ( duplicates.contains( lName ) )
                    {
                        boolean resourceTransformed = false;
                        if ( transformers != null )
                        {
                            for ( ResourceTransformer transformer : transformers )
                            {
                                if ( transformer.canTransformResource( lName ) )
                                {
                                    getLog().info( "Transforming " + lName
                                       + " using " + transformer.getClass().getName() );
                                    InputStream currIn = new FileInputStream( f );
                                    transformer.processResource( lName, currIn, null );
                                    currIn.close();
                                    resourceTransformed = true;
                                    break;
                                }
                            }
                        }
                        //if not handled by transformer, add (once) to duplicates jar
                        if ( !resourceTransformed )
                        {
                            if ( !duplicatesAdded.contains( lName ) )
                            {
                                duplicatesAdded.add( lName );
                                ZipEntry entry = new ZipEntry( lName );
                                duplicateZos.putNextEntry( entry );
                                InputStream currIn = new FileInputStream( f );
                                copyStreamWithoutClosing( currIn, duplicateZos );
                                currIn.close();
                                duplicateZos.closeEntry();
                            }
                        }
                        f.delete();
                    }
                }
            }
        }
        catch ( IOException e )
        {
            getLog().error( "Cannot removing duplicates : " + e.getMessage() );
        }
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

    private Collection<File> getNativeLibraryFolders() throws MojoExecutionException
    {
        final List<File> natives = new ArrayList<File>();

        if ( nativeLibrariesDirectory.exists() )
        {
            // If we have prebuilt native libs then copy them over to the native output folder.
            // NB they will be copied over the top of any native libs generated as part of the NdkBuildMojo
            copyLocalNativeLibraries( nativeLibrariesDirectory, ndkOutputDirectory );
        }

        final Set<Artifact> artifacts = getNativeLibraryArtifacts();
        for ( Artifact resolvedArtifact : artifacts )
        {
            if ( APKLIB.equals( resolvedArtifact.getType() ) || AAR.equals( resolvedArtifact.getType() ) )
            {
                // If the artifact is an AAR or APKLIB then add their native libs folder to the result.
                final File folder = getUnpackedLibNativesFolder( resolvedArtifact );
                getLog().debug( "Adding native library folder " + folder );
                natives.add( folder );
            }

            // Copy the native lib dependencies into the native lib output folder
            for ( String ndkArchitecture : AndroidNdk.NDK_ARCHITECTURES )
            {
                if ( NativeHelper.artifactHasHardwareArchitecture( resolvedArtifact,
                        ndkArchitecture, nativeLibrariesDependenciesHardwareArchitectureDefault ) )
                {
                    // If the artifact is a native lib then copy it into the native libs output folder.
                    copyNativeLibraryArtifact( resolvedArtifact, ndkOutputDirectory, ndkArchitecture );
                }
            }
        }

        if ( apkDebug )
        {
            // Copy the gdbserver binary into the native libs output folder (for each architecture).
            for ( String ndkArchitecture : AndroidNdk.NDK_ARCHITECTURES )
            {
                copyGdbServer( ndkOutputDirectory, ndkArchitecture );
            }
        }

        if ( ndkOutputDirectory.exists() )
        {
            // If we have any native libs in the native output folder then add the output folder to the result.
            getLog().debug( "Adding built native library folder " + ndkOutputDirectory );
            natives.add( ndkOutputDirectory );
        }

        return natives;
    }

    /**
     * @return Any native dependencies or attached artifacts. This may include artifacts from the ndk-build MOJO.
     * @throws MojoExecutionException
     */
    private Set<Artifact> getNativeLibraryArtifacts() throws MojoExecutionException
    {
        return getNativeHelper().getNativeDependenciesArtifacts( this, getUnpackedLibsDirectory(), true );
    }

    private void copyNativeLibraryArtifact( Artifact artifact,
                                            File destinationDirectory,
                                            String ndkArchitecture ) throws MojoExecutionException
    {

        final File artifactFile = getArtifactResolverHelper().resolveArtifactToFile( artifact );
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

            final File folder = new File( destinationDirectory, ndkArchitecture );
            final File file = new File( folder, filename );
            getLog().debug( "Copying native dependency " + artifactId + " (" + artifact.getGroupId() + ") to " + file );
            FileUtils.copyFile( artifactFile, file );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Could not copy native dependency.", e );
        }
    }


    /**
     * Copy the Ndk GdbServer into the architecture output folder if the folder exists but the GdbServer doesn't.
     */
    private void copyGdbServer( File destinationDirectory, String architecture ) throws MojoExecutionException
    {

        try
        {
            final File destDir = new File( destinationDirectory, architecture );
            if ( destDir.exists() )
            {
                // Copy the gdbserver binary to libs/<architecture>/
                final File gdbServerFile = getAndroidNdk().getGdbServer( architecture );
                final File destFile = new File( destDir, "gdbserver" );
                if ( ! destFile.exists() )
                {
                    getLog().debug( "Copying gdbServer to " + destFile );
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
        File outputFile = new File( targetDirectory, finalName + ".ap_" );

        List<File> dependencyArtifactResDirectoryList = new ArrayList<File>();
        for ( Artifact libraryArtifact : getTransitiveDependencyArtifacts( APKLIB, AAR ) )
        {
            final File libraryResDir = getUnpackedLibResourceFolder( libraryArtifact );
            if ( libraryResDir.exists() )
            {
                dependencyArtifactResDirectoryList.add( libraryResDir );
            }
        }

        AaptCommandBuilder commandBuilder = AaptCommandBuilder
                .packageResources( getLog() )
                .forceOverwriteExistingFiles()
                .setPathToAndroidManifest( destinationManifestFile )
                .addResourceDirectoriesIfExists( overlayDirectories )
                .addResourceDirectoryIfExists( resourceDirectory )
                .addResourceDirectoriesIfExists( dependencyArtifactResDirectoryList )
                .autoAddOverlay()
                // NB aapt only accepts a single assets parameter - combinedAssets is a merge of all assets
                .addRawAssetsDirectoryIfExists( combinedAssets )
                .renameManifestPackage( renameManifestPackage )
                .renameInstrumentationTargetPackage( renameInstrumentationTargetPackage )
                .addExistingPackageToBaseIncludeSet( androidJar )
                .setOutputApkFile( outputFile )
                .addConfigurations( configurations )
                .setVerbose( aaptVerbose )
                .setDebugMode( !release )
                .addExtraArguments( aaptExtraArgs );

        getLog().debug( getAndroidSdk().getAaptPath() + " " + commandBuilder.toString() );
        try
        {
            executor.setCaptureStdOut( true );
            List<String> commands = commandBuilder.build();
            executor.executeCommand( getAndroidSdk().getAaptPath(), commands, project.getBasedir(), false );
        }
        catch ( ExecutionException e )
        {
            throw new MojoExecutionException( "", e );
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

    /**
     * Used to populated the {@link #apkMetaInf} attribute via reflection.
     */
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
