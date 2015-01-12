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
package com.jayway.maven.plugins.android.phase01generatesources;

import com.android.manifmerger.ManifestMerger;
import com.android.manifmerger.MergerLog;
import com.android.utils.StdLogger;
import com.jayway.maven.plugins.android.AbstractAndroidMojo;
import com.jayway.maven.plugins.android.CommandExecutor;
import com.jayway.maven.plugins.android.ExecutionException;
import com.jayway.maven.plugins.android.common.AaptCommandBuilder;
import com.jayway.maven.plugins.android.common.AaptCommandBuilder.AaptPackageCommandBuilder;
import com.jayway.maven.plugins.android.common.DependencyResolver;
import com.jayway.maven.plugins.android.common.FileRetriever;
import com.jayway.maven.plugins.android.configuration.BuildConfigConstant;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static com.jayway.maven.plugins.android.common.AndroidExtension.AAR;
import static com.jayway.maven.plugins.android.common.AndroidExtension.APK;
import static com.jayway.maven.plugins.android.common.AndroidExtension.APKLIB;
import static com.jayway.maven.plugins.android.common.AndroidExtension.APKSOURCES;

/**
 * Generates <code>R.java</code> based on resources specified by the <code>resources</code> configuration parameter.
 * Generates java files based on aidl files.
 *
 * @author hugo.josefson@jayway.com
 * @author Manfred Moser <manfred@simpligility.com>
 * @author William Ferguson <william.ferguson@xandar.com.au>
 * @author Malachi de AElfweald malachid@gmail.com
 */
@Mojo(
        name = "generate-sources",
        defaultPhase = LifecyclePhase.GENERATE_SOURCES,
        requiresDependencyResolution = ResolutionScope.COMPILE
)
public class GenerateSourcesMojo extends AbstractAndroidMojo
{
    /**
     * <p>
     * Override default merging. You must have SDK Tools r20+
     * </p>
     * 
     * <p>
     * <b>IMPORTANT:</b> The resource plugin needs to be disabled for the
     * <code>process-resources</code> phase, so the "default-resources"
     * execution must be added. Without this the non-merged manifest will get
     * re-copied to the build directory.
     * </p>
     * 
     * <p>
     * The <code>destinationManifestFile</code> should also be configured to pull
     * from the build directory so that later phases will pull the merged
     * manifest file.
     * </p>
     * <p>
     * Example POM Setup:
     * </p>
     * 
     * <pre>
     * &lt;build&gt;
     *     ...
     *     &lt;plugins&gt;
     *         ...
     *         &lt;plugin&gt;
     *             &lt;artifactId&gt;maven-resources-plugin&lt;/artifactId&gt;
     *             &lt;version&gt;2.6&lt;/version&gt;
     *             &lt;executions&gt;
     *                 &lt;execution&gt;
     *                     &lt;phase&gt;initialize&lt;/phase&gt;
     *                     &lt;goals&gt;
     *                         &lt;goal&gt;resources&lt;/goal&gt;
     *                     &lt;/goals&gt;
     *                 &lt;/execution&gt;
     *                 <b>&lt;execution&gt;
     *                     &lt;id&gt;default-resources&lt;/id&gt;
     *                     &lt;phase&gt;DISABLED&lt;/phase&gt;
     *                 &lt;/execution&gt;</b>
     *             &lt;/executions&gt;
     *         &lt;/plugin&gt;
     *         &lt;plugin&gt;
     *             &lt;groupId&gt;com.jayway.maven.plugins.android.generation2&lt;/groupId&gt;
     *             &lt;artifactId&gt;android-maven-plugin&lt;/artifactId&gt;
     *             &lt;configuration&gt;
     *                 <b>&lt;destinationManifestFile&gt;
     *                     ${project.build.directory}/AndroidManifest.xml
     *                 &lt;/destinationManifestFile&gt;
     *                 &lt;mergeManifests&gt;true&lt;/mergeManifests&gt;</b>
     *             &lt;/configuration&gt;
     *             &lt;extensions&gt;true&lt;/extensions&gt;
     *         &lt;/plugin&gt;
     *         ...
     *     &lt;/plugins&gt;
     *     ...
     * &lt;/build&gt;
     * </pre>
     * <p>
     * You can filter the pre-merged APK manifest. One important note about Eclipse, Eclipse will
     * replace the merged manifest with a filtered pre-merged version when the project is refreshed.
     * If you want to review the filtered merged version then you will need to open it outside Eclipse
     * without refreshing the project in Eclipse. 
     * </p>
     * <pre>
     * &lt;resources&gt;
     *     &lt;resource&gt;
     *         &lt;targetPath&gt;${project.build.directory}&lt;/targetPath&gt;
     *         &lt;filtering&gt;true&lt;/filtering&gt;
     *         &lt;directory&gt;${basedir}&lt;/directory&gt;
     *         &lt;includes&gt;
     *             &lt;include&gt;AndroidManifest.xml&lt;/include&gt;
     *         &lt;/includes&gt;
     *     &lt;/resource&gt;
     * &lt;/resources&gt;
     * </pre>
     * @deprecated Use ManifestMerger v2 instead
     * {@link com.jayway.maven.plugins.android.standalonemojos.ManifestMergerMojo}
     */
    @Deprecated
    @Parameter( property = "android.mergeManifests", defaultValue = "false" )
    protected boolean mergeManifests;

    /**
     * <p>Whether to produce a warning if there is an aar dependency that has an apklib artifact in its dependency
     * tree. The combination of aar library including or depending on an apklib has been deprecated and may not be
     * supported by future plugin versions. Traversing the dependency graph is done for all project dependencies
     * present in build classpath.</p>
     * <p/>
     * <p>It is recommended to keep this set to <code>true</code> to catch possible issues as soon as possible.</p>
     */
    @Parameter( defaultValue = "true" )
    protected boolean warnOnApklibDependencies;

    /**
     * Whether to fail the build if one of the dependencies and/or the project duplicate a layout file.
     *
     * Such a scenario generally means that the build will fail with a compilation error due to missing resource files.
     * This is because any Ids contained in the duplicate layout files can never be generated by aapt, as appt
     * only picks up the first resource file it finds in its path.
     */
    @Parameter( defaultValue = "true" )
    private boolean failOnConflictingLayouts;

    /**
     * Whether to fail the build if one of the dependencies and/or the project have similar package in the 
     * AndroidManifest.
     *
     * Such scenario generally means that the build will fail with a compilation error due to
     * missing resources in the R file generated.
     */
    @Parameter( defaultValue = "true" )
    private boolean failOnDuplicatePackages;

    /**
     * Override default generated folder containing aidl classes
     */
    @Parameter(
            property = "android.genDirectoryAidl",
            defaultValue = "${project.build.directory}/generated-sources/aidl"
    )
    protected File genDirectoryAidl;
    
    /**
     * The directory containing the aidl files.
     */
    @Parameter( property = "android.aidlSourceDirectory", defaultValue = "${project.build.sourceDirectory}" )
    protected File aidlSourceDirectory;

    /**
     * <p>Parameter designed to generate custom BuildConfig constants
     */
    @Parameter( property = "android.buildConfigConstants", readonly = true )
    protected BuildConfigConstant[] buildConfigConstants;

    /**
     */
    @Component
    private RepositorySystem repositorySystem;

    /**
     * Pre AMP-4 AndroidManifest file.
     */
    @Parameter( readonly = true, defaultValue = "${project.basedir}/AndroidManifest.xml" )
    private File androidManifestFilePre4;

    /**
     * Pre AMP-4 android resources folder.
     */
    @Parameter( readonly = true, defaultValue = "${project.basedir}/res" )
    private File resourceDirectoryPre4;

    /**
     * Pre AMP-4 android assets directory.
     */
    @Parameter( readonly = true, defaultValue = "${project.basedir}/assets" )
    private File assetsDirectoryPre4;

    /**
     * Pre AMP-4 native libraries folder.
     */
    @Parameter( readonly = true, defaultValue = "${project.basedir}/libs" )
    private File nativeLibrariesDirectoryPre4;

    /**
     * If any non-standard files/folders exist and have NOT been explicitly configured then fail the build.
     */
    @Parameter( defaultValue = "true" )
    private boolean failOnNonStandardStructure;

    /**
     * Which dependency scopes should not be included when unpacking dependencies
     */
    protected static final List<String> EXCLUDED_DEPENDENCY_SCOPES_FOR_EXTRACTION = Arrays.asList(
            Artifact.SCOPE_SYSTEM, Artifact.SCOPE_IMPORT
    );

    /**
     * Generates the sources.
     *
     * @throws MojoExecutionException if it fails.
     * @throws MojoFailureException if it fails.
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        // If the current POM isn't an Android-related POM, then don't do
        // anything. This helps work with multi-module projects.
        if ( ! isCurrentProjectAndroid() )
        {
            return;
        }

        validateStandardLocations();

        try
        {
            targetDirectory.mkdirs();
            copyManifest();

            // Check for a deprecated AAR > APKLIB artifact combination
            if ( warnOnApklibDependencies )
            {
                checkForApklibDependencies();
            }

            // TODO Do we really want to continue supporting APKSOURCES? How long has it been deprecated
            extractSourceDependencies();

            // Extract the apklib and aar dependencies into unpacked-libs so that they can be referenced in the build.
            extractLibraryDependencies();

            // Copy project assets to combinedAssets so that aapt has a single assets folder to load.
            copyFolder( assetsDirectory, combinedAssets );

            final String[] relativeAidlFileNames1 = findRelativeAidlFileNames( aidlSourceDirectory );
            final String[] relativeAidlFileNames2 = findRelativeAidlFileNames( extractedDependenciesJavaSources );
            final Map<String, String[]> relativeApklibAidlFileNames = new HashMap<String, String[]>();

            if ( !isInstrumentationTest() )
            {
                // Only add transitive APKLIB deps if we are building an APK and not an instrumentation test apk.
                for ( Artifact artifact : getTransitiveDependencyArtifacts( APKLIB ) )
                {
                    final File libSourceFolder = getUnpackedApkLibSourceFolder( artifact );
                    final String[] apklibAidlFiles = findRelativeAidlFileNames( libSourceFolder );
                    relativeApklibAidlFileNames.put( artifact.getId(), apklibAidlFiles );
                }
            }

            mergeManifests();

            checkPackagesForDuplicates();
            checkForConflictingLayouts();
            generateR();
            generateBuildConfig();

            // When compiling AIDL for this project,
            // make sure we compile AIDL for dependencies as well.
            // This is so project A, which depends on project B, can
            // use AIDL info from project B in its own AIDL
            final Map<File, String[]> files = new HashMap<File, String[]>();
            files.put( aidlSourceDirectory, relativeAidlFileNames1 );
            files.put( extractedDependenciesJavaSources, relativeAidlFileNames2 );

            if ( !isInstrumentationTest() )
            {
                // Only add transitive APKLIB deps if we are building an APK and not an instrumentation test apk.
                for ( Artifact artifact : getTransitiveDependencyArtifacts( APKLIB ) )
                {
                    final File unpackedLibSourceFolder = getUnpackedApkLibSourceFolder( artifact );
                    files.put( unpackedLibSourceFolder, relativeApklibAidlFileNames.get( artifact.getId() ) );
                }
            }
            generateAidlFiles( files );
        }
        catch ( MojoExecutionException e )
        {
            getLog().error( "Error when generating sources.", e );
            throw e;
        }
    }

    /**
     * Performs some level of validation on the configured files and folders in light of the change
     * to AndroidStudio/Gradle style folder structures instead of the original Ant/Eclipse structures.
     *
     * Essentially we will be noisy if we see an artifact that looks like before 
     * Android Maven Plugin 4 and is not explicitly configured.
     */
    private void validateStandardLocations() throws MojoExecutionException
    {
        boolean hasNonStandardStructure = false;
        if ( androidManifestFilePre4.exists() && !androidManifestFilePre4.equals( androidManifestFile ) )
        {
            getLog().warn( "Non-standard location of AndroidManifest.xml file found, but not configured:\n " 
                + androidManifestFilePre4 + "\nMove to the standard location src/main/AndroidManifest.xml\n"
                + "Or configure androidManifestFile. \n" );
            hasNonStandardStructure = true;
        }
        if ( resourceDirectoryPre4.exists() && !resourceDirectoryPre4.equals( resourceDirectory ) )
        {
            getLog().warn( "Non-standard location of Android res folder found, but not configured:\n " 
                + resourceDirectoryPre4 + "\nMove to the standard location src/main/res/\n"
                + "Or configure resourceDirectory. \n" );
            hasNonStandardStructure = true;
        }
        if ( assetsDirectoryPre4.exists() && !assetsDirectoryPre4.equals( assetsDirectory ) )
        {
            getLog().warn( "Non-standard location assets folder found, but not configured:\n " 
                + assetsDirectoryPre4 + "\nMove to the standard location src/main/assets/\n"
                + "Or configure assetsDirectory. \n" );
            hasNonStandardStructure = true;
        }
        if ( nativeLibrariesDirectoryPre4.exists() && !nativeLibrariesDirectoryPre4.equals( nativeLibrariesDirectory ) )
        {
            getLog().warn( "Non-standard location native libs folder found, but not configured:\n " 
                + nativeLibrariesDirectoryPre4 + "\nMove to the standard location src/main/libs/\n"
                + "Or configure nativeLibrariesDirectory. \n" );
            hasNonStandardStructure = true;
        }

        if ( hasNonStandardStructure && failOnNonStandardStructure )
        {
            throw new MojoExecutionException(
                "\n\nFound files or folders in non-standard locations in the project!\n"
                + "....This might be a side-effect of a migration to Android Maven Plugin 4+.\n"
                + "....Please observe the warnings for specific files and folders above.\n"
                + "....Ideally you should restructure your project.\n"
                + "....Alternatively add explicit configuration overrides for files or folders.\n"
                + "....Finally you could set failOnNonStandardStructure to false, potentially "
                + "resulting in other failures.\n\n\n"
            );
        }
    }

    /**
     * Copy the AndroidManifest.xml from androidManifestFile to destinationManifestFile
     *
     * @throws MojoExecutionException
     */
    protected void copyManifest() throws MojoExecutionException
    {
        getLog().debug( "copyManifest: " + androidManifestFile + " -> " + destinationManifestFile );
        if ( androidManifestFile == null )
        {
            getLog().debug( "Manifest copying disabled. Using default manifest only" );
            return;
        }

        try
        {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse( androidManifestFile );
            Source source = new DOMSource( doc );

            TransformerFactory xfactory = TransformerFactory.newInstance();
            Transformer xformer = xfactory.newTransformer();
            xformer.setOutputProperty( OutputKeys.OMIT_XML_DECLARATION, "yes" );

            FileWriter writer = null;
            try
            {
                destinationManifestFile.getParentFile().mkdirs();

                writer = new FileWriter( destinationManifestFile, false );
                if ( doc.getXmlEncoding() != null && doc.getXmlVersion() != null )
                {
                    String xmldecl = String.format( "<?xml version=\"%s\" encoding=\"%s\"?>%n",
                            doc.getXmlVersion(), doc.getXmlEncoding() );

                    writer.write( xmldecl );
                }
                Result result = new StreamResult( writer );
                xformer.transform( source, result );
                getLog().info( "Manifest copied from " + androidManifestFile + " to " + destinationManifestFile );
            }
            finally
            {
                IOUtils.closeQuietly( writer );
            }
        }
        catch ( Exception e )
        {
            getLog().error( "Error during copyManifest" );
            throw new MojoExecutionException( "Error during copyManifest", e );
        }
    }

    /**
     * Extract the source dependencies.
     *
     * @throws MojoExecutionException if it fails.
     */
    protected void extractSourceDependencies() throws MojoExecutionException
    {
        for ( Artifact artifact : getDirectDependencyArtifacts() )
        {
            String type = artifact.getType();
            if ( type.equals( APKSOURCES ) )
            {
                getLog().debug( "Detected apksources dependency " + artifact + " with file " + artifact.getFile()
                        + ". Will resolve and extract..." );

                final File apksourcesFile = resolveArtifactToFile( artifact );
                getLog().debug( "Extracting " + apksourcesFile + "..." );
                extractApksources( apksourcesFile );
            }
        }

        if ( extractedDependenciesJavaResources.exists() )
        {
            projectHelper.addResource( project, extractedDependenciesJavaResources.getAbsolutePath(), null, null );
            project.addCompileSourceRoot( extractedDependenciesJavaSources.getAbsolutePath() );
        }
    }

    /**
     * @deprecated Support <code>APKSOURCES</code> artifacts has been deprecated. Use APKLIB instead.
     */
    @Deprecated
    private void extractApksources( File apksourcesFile ) throws MojoExecutionException
    {
        if ( apksourcesFile.isDirectory() )
        {
            getLog().warn( "The apksources artifact points to '" + apksourcesFile
                    + "' which is a directory; skipping unpacking it." );
            return;
        }
        final UnArchiver unArchiver = new ZipUnArchiver( apksourcesFile )
        {
            @Override
            protected Logger getLogger()
            {
                return new ConsoleLogger( Logger.LEVEL_DEBUG, "dependencies-unarchiver" );
            }
        };
        extractedDependenciesDirectory.mkdirs();
        unArchiver.setDestDirectory( extractedDependenciesDirectory );
        try
        {
            unArchiver.extract();
        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException( "ArchiverException while extracting " + apksourcesFile.getAbsolutePath()
                    + ". Message: " + e.getLocalizedMessage(), e );
        }
    }

    private void extractLibraryDependencies() throws MojoExecutionException
    {
        final Collection<Artifact> artifacts = getTransitiveDependencyArtifacts(
                EXCLUDED_DEPENDENCY_SCOPES_FOR_EXTRACTION );

        getLog().info( "Extracting libs" );

        // The only library dependencies we want included in an instrumentation test are APK and AARs.
        // Any APKLIB classes have already been compiled into the APK.
        final boolean instrumentationTest = isInstrumentationTest();

        for ( Artifact artifact : artifacts )
        {
            final String type = artifact.getType();
            if ( type.equals( APKLIB ) && !instrumentationTest )
            {
                getLog().info( "Extracting apklib " + artifact.getArtifactId() + "..." );
                extractApklib( artifact );
            }
            else if ( type.equals( AAR ) )
            {
                getLog().info( "Extracting aar " + artifact.getArtifactId() + "..." );
                extractAarLib( artifact );
            }
            else if ( type.equals( APK ) )
            {
                getLog().info( "Extracting apk " + artifact.getArtifactId() + "..." );
                extractApkClassesJar( artifact );
            }
            else
            {
                getLog().debug( "Not extracting " + artifact.getArtifactId() + "..." );
            }
        }
    }

    /**
     * Extracts ApkLib and adds the assets and apklib sources and resources to the build.
     */
    private void extractApklib( Artifact apklibArtifact ) throws MojoExecutionException
    {
        getUnpackedLibHelper().extractApklib( apklibArtifact );

        // Copy the assets to the the combinedAssets folder.
        // Add the apklib source and resource to the compile.
        // NB apklib sources are added to compileSourceRoot because we may need to compile against them.
        //    This means the apklib classes will be compiled into target/classes and packaged with this build.
        copyFolder( getUnpackedLibAssetsFolder( apklibArtifact ), combinedAssets );

        final File apklibSourceFolder = getUnpackedApkLibSourceFolder( apklibArtifact );
        final List<String> resourceExclusions = Arrays.asList( "**/*.java", "**/*.aidl" );
        projectHelper.addResource( project, apklibSourceFolder.getAbsolutePath(), null, resourceExclusions );
        project.addCompileSourceRoot( apklibSourceFolder.getAbsolutePath() );
    }

    /**
     * Extracts AarLib and if this is an APK build then adds the assets and resources to the build.
     */
    private void extractAarLib( Artifact aarArtifact ) throws MojoExecutionException
    {
        getUnpackedLibHelper().extractAarLib( aarArtifact );

        // Copy the assets to the the combinedAssets folder, but only if an APK build.
        // Ie we only want to package assets that we own.
        // Assets should only live within their owners or the final APK.
        if ( isAPKBuild() )
        {
            copyFolder( getUnpackedLibAssetsFolder( aarArtifact ), combinedAssets );
        }

        // Aar lib resources should only be included if we are building an apk.
        // So we need to extract them into a folder that we then add to the resource classpath.
        if ( isAPKBuild() )
        {
            // (If we are including SYSTEM_SCOPE artifacts when considering resources for APK packaging)
            // then adding the AAR resources to the project would have them added twice.
            getLog().debug( "Not adding AAR resources to resource classpath : " + aarArtifact );
        }
    }

    /**
     * Copies a dependent APK jar over the top of the placeholder created for it in AarMavenLifeCycleParticipant.
     *
     * This is necessary because we need the classes of the APK added to the compile classpath.
     * NB APK dependencies are uncommon as they should really only be used in a project that tests an apk.
     *
     * @param artifact  APK dependency for this project whose classes will be copied over.
     */
    private void extractApkClassesJar( Artifact artifact ) throws MojoExecutionException
    {
        final File apkClassesJar = getUnpackedLibHelper().getJarFileForApk( artifact );
        final File unpackedClassesJar = getUnpackedLibHelper().getUnpackedClassesJar( artifact );
        try
        {
            FileUtils.copyFile( apkClassesJar, unpackedClassesJar );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException(
                    "Could not copy APK classes jar " + apkClassesJar + " to " + unpackedClassesJar, e );
        }
        getLog().debug( "Copied APK classes jar into compile classpath : " + unpackedClassesJar );

    }

    /**
     * Traverses the list of project dependencies looking for &quot;AAR depends on APKLIB&quot; artifact combination
     * that has been deprecated. For each occurrence of an AAR artifact with APKLIB direct or transitive dependency,
     * produces a warning message to inform the user. Future plugin versions may default to skipping or not handling
     * unsupported artifacts during build lifecycle.
     *
     * @throws MojoExecutionException
     */
    private void checkForApklibDependencies() throws MojoExecutionException
    {
        final boolean isAarBuild = project.getPackaging().equals( AAR );

        final DependencyResolver dependencyResolver = getDependencyResolver();

        final Set<Artifact> allArtifacts = project.getArtifacts();
        Set<Artifact> dependencyArtifacts = getArtifactResolverHelper().getFilteredArtifacts( allArtifacts );

        boolean foundApklib = false;

        for ( Artifact artifact : dependencyArtifacts )
        {
            final String type = artifact.getType();
            if ( type.equals( APKLIB ) && isAarBuild )
            {
                getLog().warn( "Detected APKLIB transitive dependency: " + artifact.getId() );
                foundApklib = true;
            }
            else if ( type.equals( AAR ) )
            {
                final Set<Artifact> dependencies = dependencyResolver
                        .getLibraryDependenciesFor( session, repositorySystem, artifact );
                for ( Artifact dependency : dependencies )
                {
                    if ( dependency.getType().equals( APKLIB ) )
                    {
                        getLog().warn( "Detected " + artifact.getId() + " that depends on APKLIB: "
                                + dependency.getId() );
                        foundApklib = true;
                    }
                }
            }
        }

        if ( foundApklib )
        {
            getLog().warn( "AAR libraries should not depend or include APKLIB artifacts.\n"
                + "APKLIBs have been deprecated and the combination of the two may yield unexpected results.\n"
                + "Check the problematic AAR libraries for newer versions that use AAR packaging." );
        }
    }

    /**
     * Checks packages in AndroidManifest.xml file of project and all dependent libraries for duplicates.
     * <p>Generate warning if duplicates presents.
     * <p>(in case of packages similarity R.java and BuildConfig files will be overridden)
     *
     * @throws MojoExecutionException
     */
    private void checkPackagesForDuplicates() throws MojoExecutionException
    {
        Set<Artifact> dependencyArtifacts = getTransitiveDependencyArtifacts( AAR, APKLIB );

        if ( dependencyArtifacts.isEmpty() )
        {
            //if no AAR or APKLIB dependencies presents than only one package presents
            return;
        }

        Map<String, Set<Artifact>> packageCompareMap = getPackageCompareMap( dependencyArtifacts );

        List<String> duplicatesMessageList = new ArrayList<String>();
        for ( Map.Entry<String, Set<Artifact>> entry: packageCompareMap.entrySet() )
        {
            Set<Artifact> artifacts = entry.getValue();
            if ( artifacts != null && artifacts.size() > 1 )
            {
                StringBuilder messageBuilder = new StringBuilder();
                for ( Artifact item: artifacts )
                {
                    messageBuilder
                            .append( messageBuilder.length() > 0 ? ", " : "    [" )
                            .append( item.getArtifactId() );
                }
                messageBuilder
                        .append( "] have similar package='" )
                        .append( entry.getKey() )
                        .append( "'" );
                duplicatesMessageList.add( messageBuilder.toString() );
            }
        }
        if ( !duplicatesMessageList.isEmpty() )
        {
            List<String> messageList = new ArrayList<String>();
            messageList.add( "" );
            messageList.add( "Duplicate packages detected in AndroidManifest.xml files" );
            messageList.add( "" );
            messageList.add( "Such scenario generally means that the build will fail with a compilation error due to"
                    + " missing resources in R file." );
            messageList.add( "You should consider renaming some of the duplicate packages listed below"
                    + " to avoid the conflict." );
            messageList.add( "" );
            messageList.add( "Conflicting artifacts:" );
            messageList.addAll( duplicatesMessageList );
            messageList.add( "" );

            if ( failOnDuplicatePackages )
            {
                StringBuilder builder = new StringBuilder();
                for ( String line : messageList )
                {
                    builder.append( line );
                    builder.append( "\n" );
                }
                builder.append( "\n" );
                builder.append( "You can downgrade the failure to a warning " );
                builder.append( "by setting the 'failOnDuplicatePackages' plugin property to false." );
                throw new MojoExecutionException( builder.toString() );
            }
            for ( String messageLine: messageList )
            {
                getLog().warn( messageLine );
            }
        }
    }

    /**
     * Looks for dependencies that have a layout file with the same name as either one of the project layout files
     * or one of the other dependencies. If such a duplicate occurs then it will fail the build or generate a warning
     * depending upon the value of the <strong>failOnConflictingLayouts</strong> build parameter.
     *
     * @throws MojoExecutionException
     */
    private void checkForConflictingLayouts() throws MojoExecutionException
    {
        final ConflictingLayoutDetector detector = new ConflictingLayoutDetector();

        // Add layout files for this project
        final FileRetriever retriever = new FileRetriever( "layout*/*.xml" );
        detector.addLayoutFiles( getAndroidManifestPackageName(), retriever.getFileNames( resourceDirectory ) );

        // Add layout files for all dependencies.
        for ( final Artifact dependency : getTransitiveDependencyArtifacts( AAR, APKLIB ) )
        {
            final String packageName = extractPackageNameFromAndroidArtifact( dependency );
            final String[] layoutFiles = retriever.getFileNames( getUnpackedLibResourceFolder( dependency ) );
            detector.addLayoutFiles( packageName, layoutFiles );
        }

        final Collection<ConflictingLayout> conflictingLayouts = detector.getConflictingLayouts();
        getLog().debug( "checkConflictingLayouts - conflicts : " + conflictingLayouts );
        if ( !conflictingLayouts.isEmpty() )
        {
            final List<String> sb = new ArrayList<String>();
            sb.add( "" );
            sb.add( "" );
            sb.add( "Duplicate layout files have been detected across more than one Android package." );
            sb.add( "" );
            sb.add( "Such a scenario generally means that the build will fail with a compilation error due to" );
            sb.add( "missing resource files. You should consider renaming some of the layout files listed below" );
            sb.add( "to avoid the conflict." );
            sb.add( "" );
            sb.add( "However, if you believe you know better, then you can downgrade the failure to a warning" );
            sb.add( "by setting the failOnConflictingLayouts plugin property to false." );
            sb.add( "But you really don't want to do that." );
            sb.add( "" );
            sb.add( "Conflicting Layouts:" );
            for ( final ConflictingLayout layout : conflictingLayouts )
            {
                sb.add( "    " + layout.getLayoutFileName() + "  packages=" + layout.getPackageNames().toString() );
            }
            sb.add( "" );

            if ( failOnConflictingLayouts )
            {
                final StringBuilder builder = new StringBuilder();
                for ( final String line : sb )
                {
                    builder.append( line );
                    builder.append( "\n" );
                }
                throw new MojoExecutionException( builder.toString() );
            }

            for ( final String line : sb )
            {
                getLog().warn( line );
            }
        }
    }

    /**
     * Provides map with all provided dependencies or project itself grouped by package name
     *
     * @param dependencyArtifacts artifacts that should be grouped by package name
     * @return map of with package names(String) and sets of artifacts (Set<Artifact>)
     *          that have similar package names
     * @throws MojoExecutionException
     */
    Map<String, Set<Artifact>> getPackageCompareMap( Set<Artifact> dependencyArtifacts ) throws MojoExecutionException
    {
        if ( dependencyArtifacts == null )
        {
            throw new IllegalArgumentException( "dependencies must be initialized" );
        }

        Map<String, Set<Artifact>> packageCompareMap = new HashMap<String, Set<Artifact>>();

        Set<Artifact> artifactSet = new HashSet<Artifact>();
        artifactSet.add( project.getArtifact() );
        packageCompareMap.put( getAndroidManifestPackageName(), artifactSet );

        for ( Artifact artifact: dependencyArtifacts )
        {
            String libPackage = extractPackageNameFromAndroidArtifact( artifact );

            Set<Artifact> artifacts = packageCompareMap.get( libPackage );
            if ( artifacts == null )
            {
                artifacts = new HashSet<Artifact>();
                packageCompareMap.put( libPackage, artifacts );
            }
            artifacts.add( artifact );
        }
        return packageCompareMap;
    }

    private void generateR() throws MojoExecutionException
    {
        getLog().info( "Generating R file for " + project.getArtifact() );

        genDirectory.mkdirs();

        final AaptPackageCommandBuilder commandBuilder = AaptCommandBuilder
                .packageResources( getLog() )
                .makePackageDirectories()
                .setResourceConstantsFolder( genDirectory )
                .forceOverwriteExistingFiles()
                .disablePngCrunching()
                .generateRIntoPackage( customPackage )
                .setPathToAndroidManifest( destinationManifestFile )
                .addResourceDirectoryIfExists( resourceDirectory )
                .addResourceDirectoriesIfExists( getResourceOverlayDirectories() )
                    // Need to include any AAR or APKLIB dependencies when generating R because if any local
                    // resources directly reference dependent resources then R generation will crash.
                .addResourceDirectoriesIfExists( getLibraryResourceFolders() )
                .autoAddOverlay()
                .addRawAssetsDirectoryIfExists( combinedAssets )
                .addExistingPackageToBaseIncludeSet( getAndroidSdk().getAndroidJar() )
                .addConfigurations( configurations )
                .addExtraArguments( aaptExtraArgs )
                .setVerbose( aaptVerbose )
                    // We need to generate R.txt for all projects as it needs to be consumed when generating R class.
                    // It also needs to be consumed when packaging aar.
                .generateRTextFile( targetDirectory )
                // If a proguard file is defined then output Proguard options to it.
                .setProguardOptionsOutputFile( proguardFile )
                .makeResourcesNonConstant( AAR.equals( project.getArtifact().getType() ) );

        getLog().debug( getAndroidSdk().getAaptPath() + " " + commandBuilder.toString() );
        try
        {
            final CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
            executor.setLogger( getLog() );
            executor.setCaptureStdOut( true );
            final List<String> commands = commandBuilder.build();
            executor.executeCommand( getAndroidSdk().getAaptPath(), commands, project.getBasedir(), false );
        }
        catch ( ExecutionException e )
        {
            throw new MojoExecutionException( "", e );
        }

        final ResourceClassGenerator resGenerator = new ResourceClassGenerator( this, targetDirectory, genDirectory );
        generateCorrectRJavaForApklibDependencies( resGenerator );
        generateCorrectRJavaForAarDependencies( resGenerator );

        getLog().info( "Adding R gen folder to compile classpath: " + genDirectory );
        project.addCompileSourceRoot( genDirectory.getAbsolutePath() );
    }

    /**
     * Generate correct R.java for apklibs dependencies of a current project
     *
     * @throws MojoExecutionException
     */
    private void generateCorrectRJavaForApklibDependencies( ResourceClassGenerator resourceGenerator )
            throws MojoExecutionException
    {
        getLog().debug( "" );
        getLog().debug( "#generateCorrectRJavaFoApklibDeps" );

        // Generate R.java for apklibs
        // Compatibility with Apklib which isn't present in AndroidBuilder
        getLog().debug( "Generating Rs for apklib deps of project " + project.getArtifact() );
        final Set<Artifact> apklibDependencies = getTransitiveDependencyArtifacts( APKLIB );
        for ( final Artifact artifact : apklibDependencies )
        {
            getLog().debug( "Generating apklib R.java for " + artifact.getArtifactId() + "..." );
            generateRForApkLibDependency( artifact );
        }

        // Generate corrected R.java for APKLIB dependencies, but only if this is an APK build.
        if ( !apklibDependencies.isEmpty() && APK.equals( project.getArtifact().getType() ) )
        {
            // Generate R.java for each APKLIB based on R.txt
            getLog().debug( "" );
            getLog().debug( "Rewriting R files for APKLIB dependencies : " + apklibDependencies );
            resourceGenerator.generateLibraryRs( apklibDependencies, "apklib" );
        }
    }

    /**
     * Generate correct R.java for aar dependencies of a current project
     *
     * @throws MojoExecutionException
     */
    private void generateCorrectRJavaForAarDependencies( ResourceClassGenerator resourceGenerator )
            throws MojoExecutionException
    {
        // Generate corrected R.java for AAR dependencies.
        final Set<Artifact> aarLibraries = getTransitiveDependencyArtifacts( AAR );
        if ( !aarLibraries.isEmpty() )
        {
            // Generate R.java for each AAR based on R.txt
            getLog().debug( "Generating R file for AAR dependencies" );
            resourceGenerator.generateLibraryRs( aarLibraries, "aar" );
        }
    }

    private List<File> getLibraryResourceFolders()
    {
        final List<File> resourceFolders = new ArrayList<File>();
        for ( Artifact artifact : getTransitiveDependencyArtifacts( AAR, APKLIB ) )
        {
            getLog().debug( "Considering dep artifact : " + artifact );
            final File resourceFolder = getUnpackedLibResourceFolder( artifact );
            if ( resourceFolder.exists() )
            {
                getLog().debug( "Adding apklib or aar resource folder : " + resourceFolder );
                resourceFolders.add( resourceFolder );
            }
        }
        return resourceFolders;
    }

    /**
     * Executes aapt to generate the R class for the given apklib.
     *
     * @param apklibArtifact    apklib for which to generate the R class.
     * @throws MojoExecutionException if it fails.
     */
    private void generateRForApkLibDependency( Artifact apklibArtifact ) throws MojoExecutionException
    {
        final File unpackDir = getUnpackedLibFolder( apklibArtifact );
        getLog().debug( "Generating incomplete R file for apklib: " + apklibArtifact.getGroupId()
                + ":" + apklibArtifact.getArtifactId() );
        final File apklibManifest = new File( unpackDir, "AndroidManifest.xml" );
        final File apklibResDir = new File( unpackDir, "res" );

        List<File> dependenciesResDirectories = new ArrayList<File>();
        final Set<Artifact> apklibDeps = getDependencyResolver()
                .getLibraryDependenciesFor( this.session, this.repositorySystem, apklibArtifact );
        getLog().debug( "apklib=" + apklibArtifact + "  dependencies=" + apklibDeps );
        for ( Artifact dependency : apklibDeps )
        {
            // Add in the resources that are dependencies of the apklib.
            final String extension = dependency.getType();
            final File dependencyResDir = getUnpackedLibResourceFolder( dependency );
            if ( ( extension.equals( APKLIB ) || extension.equals( AAR ) ) && dependencyResDir.exists() )
            {
                dependenciesResDirectories.add( dependencyResDir );
            }
        }

        // Create combinedAssets for this apklib dependency - can't have multiple -A args
        final File apklibCombAssets = new File( getUnpackedLibFolder( apklibArtifact ), "combined-assets" );
        for ( Artifact dependency : apklibDeps )
        {
            // Accumulate assets for dependencies of the apklib (if they exist).
            final String extension = dependency.getType();
            final File dependencyAssetsDir = getUnpackedLibAssetsFolder( dependency );
            if ( ( extension.equals( APKLIB ) || extension.equals( AAR ) ) )
            {
                copyFolder( dependencyAssetsDir, apklibCombAssets );
            }
        }
        // Overlay the apklib dependency assets (if they exist)
        final File apkLibAssetsDir = getUnpackedLibAssetsFolder( apklibArtifact );
        copyFolder( apkLibAssetsDir, apklibCombAssets );

        final CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger( getLog() );

        final AaptCommandBuilder commandBuilder = AaptCommandBuilder
                .packageResources( getLog() )
                .makeResourcesNonConstant()
                .makePackageDirectories()
                .setResourceConstantsFolder( genDirectory )
                .generateRIntoPackage( extractPackageNameFromAndroidManifest( apklibManifest ) )
                .setPathToAndroidManifest( apklibManifest )
                .addResourceDirectoryIfExists( apklibResDir )
                .addResourceDirectoriesIfExists( dependenciesResDirectories )
                .autoAddOverlay()
                .addRawAssetsDirectoryIfExists( apklibCombAssets )
                .addExistingPackageToBaseIncludeSet( getAndroidSdk().getAndroidJar() )
                .addConfigurations( configurations )
                .addExtraArguments( aaptExtraArgs )
                .setVerbose( aaptVerbose )
                // We need to generate R.txt for all projects as it needs to be consumed when generating R class.
                // It also needs to be consumed when packaging aar.
                .generateRTextFile( unpackDir );

        getLog().debug( getAndroidSdk().getAaptPath() + " " + commandBuilder.toString() );
        try
        {
            executor.setCaptureStdOut( true );
            final List<String> commands = commandBuilder.build();
            executor.executeCommand( getAndroidSdk().getAaptPath(), commands, project.getBasedir(), false );
        }
        catch ( ExecutionException e )
        {
            throw new MojoExecutionException( "", e );
        }
    }

    /**
     * @deprecated Use ManifestMerger v2 instead
     * {@link com.jayway.maven.plugins.android.standalonemojos.ManifestMergerMojo}
     * @throws MojoExecutionException
     */
    @Deprecated
    private void mergeManifests() throws MojoExecutionException
    {
        getLog().debug( "mergeManifests: " + mergeManifests );

        if ( !mergeManifests )
        {
            getLog().debug( "Manifest merging disabled. Using project manifest only" );
            return;
        }

        getLog().info( "Getting manifests of dependent apklibs" );
        List<File> libManifests = new ArrayList<File>();
        for ( Artifact artifact : getTransitiveDependencyArtifacts( APKLIB, AAR ) )
        {
            final File libManifest = new File( getUnpackedLibFolder( artifact ), "AndroidManifest.xml" );
            if ( !libManifest.exists() )
            {
                throw new MojoExecutionException( artifact.getArtifactId() + " is missing AndroidManifest.xml" );
            }

            libManifests.add( libManifest );
        }

        if ( !libManifests.isEmpty() )
        {
            final File mergedManifest = new File( destinationManifestFile.getParent(), "AndroidManifest-merged.xml" );
            final StdLogger stdLogger = new StdLogger( StdLogger.Level.VERBOSE );
            final ManifestMerger merger = new ManifestMerger( MergerLog.wrapSdkLog( stdLogger ), null );

            getLog().info( "Merging manifests of dependent apklibs" );

            final boolean mergeSuccess = merger.process( mergedManifest, destinationManifestFile,
                libManifests.toArray( new File[libManifests.size()] ),  null, null );

            if ( mergeSuccess )
            {
                // Replace the original manifest file with the merged one so that
                // the rest of the build will pick it up.
                destinationManifestFile.delete();
                mergedManifest.renameTo( destinationManifestFile );
                getLog().info( "Done Merging Manifests of APKLIBs" );
            }
            else
            {
                getLog().error( "Manifests were not merged!" );
                throw new MojoExecutionException( "Manifests were not merged!" );
            }
        }
        else
        {
            getLog().info( "No APKLIB manifests found. Using project manifest only." );
        }
    }

    private void generateBuildConfig() throws MojoExecutionException
    {
        getLog().debug( "Generating BuildConfig file" );

        // Create the BuildConfig for our package.
        String packageName = extractPackageNameFromAndroidManifest( destinationManifestFile );
        if ( StringUtils.isNotBlank( customPackage ) )
        {
            packageName = customPackage;
        }
        generateBuildConfigForPackage( packageName );

        // Skip BuildConfig generation for dependencies if this is an AAR project
        if ( project.getPackaging().equals( AAR ) )
        {
            return;
        }

        // Generate the BuildConfig for any APKLIB and some AAR dependencies.
        // Need to generate for AAR, because some old AARs like ActionBarSherlock do not have BuildConfig (or R)
        for ( Artifact artifact : getTransitiveDependencyArtifacts( APKLIB, AAR ) )
        {
            if ( skipBuildConfigGeneration( artifact ) )
            {
                getLog().info( "Skip BuildConfig.java generation for "
                        + artifact.getGroupId() + " " + artifact.getArtifactId() );
                continue;
            }

            final String depPackageName = extractPackageNameFromAndroidArtifact( artifact );

            generateBuildConfigForPackage( depPackageName );
        }
    }

    private boolean skipBuildConfigGeneration( Artifact artifact ) throws MojoExecutionException
    {
        if ( artifact.getType().equals( AAR ) )
        {
            String depPackageName = extractPackageNameFromAndroidArtifact( artifact );

            if ( isBuildConfigPresent( artifact, depPackageName ) )
            {
                return true;
            }

            Set< Artifact > transitiveDep = getArtifactResolverHelper()
                    .getFilteredArtifacts( project.getArtifacts(), AAR );

            for ( Artifact transitiveArtifact : transitiveDep )
            {
                if ( isBuildConfigPresent( transitiveArtifact, depPackageName ) )
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if given artifact includes a matching BuildConfig class
     * 
     * @throws MojoExecutionException
     */
    private boolean isBuildConfigPresent( Artifact artifact ) throws MojoExecutionException
    {
        String depPackageName = extractPackageNameFromAndroidArtifact( artifact );

        return isBuildConfigPresent( artifact, depPackageName );
    }

    /**
     * Check whether the artifact includes a BuildConfig located in a given package.
     * 
     * @param artifact an AAR artifact to look for BuildConfig in
     * @param packageName BuildConfig package name
     * @throws MojoExecutionException
     */
    private boolean isBuildConfigPresent( Artifact artifact, String packageName ) throws MojoExecutionException
    {
        try
        {
            JarFile jar = new JarFile( getUnpackedAarClassesJar( artifact ) );
            JarEntry entry = jar.getJarEntry( packageName.replace( '.', '/' ) + "/BuildConfig.class" );

            return ( entry != null );
        }
        catch ( IOException e )
        {
            getLog().error( "Error generating BuildConfig ", e );
            throw new MojoExecutionException( "Error generating BuildConfig", e );
        }
    }

    private void generateBuildConfigForPackage( String packageName ) throws MojoExecutionException
    {
        getLog().debug( "Creating BuildConfig for " + packageName );

        File outputFolder = new File( genDirectory, packageName.replace( ".", File.separator ) );
        outputFolder.mkdirs();

        StringBuilder buildConfig = new StringBuilder();
        buildConfig.append( "package " ).append( packageName ).append( ";\n\n" );
        buildConfig.append( "public final class BuildConfig {\n" );
        buildConfig.append( "  public static final boolean DEBUG = " ).append( !release ).append( ";\n" );
        for ( BuildConfigConstant constant : buildConfigConstants )
        {
            String value = constant.getValue();
            if ( "String".equals( constant.getType() ) )
            {
                value = "\"" + value + "\"";
            }

            buildConfig.append( "  public static final " )
                       .append( constant.getType() )
                       .append( " " )
                       .append( constant.getName() )
                       .append( " = " )
                       .append( value )
                       .append( ";\n" );
        }
        buildConfig.append( "}\n" );

        File outputFile = new File( outputFolder, "BuildConfig.java" );
        try
        {
            FileUtils.writeStringToFile( outputFile, buildConfig.toString() );
        }
        catch ( IOException e )
        {
            getLog().error( "Error generating BuildConfig ", e );
            throw new MojoExecutionException( "Error generating BuildConfig", e );
        }
    }

    /**
     * Given a map of source directories to list of AIDL (relative) filenames within each,
     * runs the AIDL compiler for each, such that all source directories are available to
     * the AIDL compiler.
     *
     * @param files Map of source directory File instances to the relative paths to all AIDL files within
     * @throws MojoExecutionException If the AIDL compiler fails
     */
    private void generateAidlFiles( Map<File /*sourceDirectory*/, String[] /*relativeAidlFileNames*/> files )
            throws MojoExecutionException
    {
        List<String> protoCommands = new ArrayList<String>();
        protoCommands.add( "-p" + getAndroidSdk().getPathForFrameworkAidl() );

        genDirectoryAidl.mkdirs();
        getLog().info( "Adding AIDL gen folder to compile classpath: " + genDirectoryAidl );
        project.addCompileSourceRoot( genDirectoryAidl.getPath() );
        Set<File> sourceDirs = files.keySet();
        for ( File sourceDir : sourceDirs )
        {
            protoCommands.add( "-I" + sourceDir );
        }
        for ( File sourceDir : sourceDirs )
        {
            for ( String relativeAidlFileName : files.get( sourceDir ) )
            {
                File targetDirectory = new File( genDirectoryAidl, new File( relativeAidlFileName ).getParent() );
                targetDirectory.mkdirs();

                final String shortAidlFileName = new File( relativeAidlFileName ).getName();
                final String shortJavaFileName = shortAidlFileName.substring( 0, shortAidlFileName.lastIndexOf( "." ) )
                                                 + ".java";
                final File aidlFileInSourceDirectory = new File( sourceDir, relativeAidlFileName );

                List<String> commands = new ArrayList<String>( protoCommands );
                commands.add( aidlFileInSourceDirectory.getAbsolutePath() );
                commands.add( new File( targetDirectory, shortJavaFileName ).getAbsolutePath() );
                try
                {
                    CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
                    executor.setLogger( this.getLog() );
                    executor.setCaptureStdOut( true );
                    executor.executeCommand( getAndroidSdk().getAidlPath(), commands, project.getBasedir(),
                            false );
                }
                catch ( ExecutionException e )
                {
                    throw new MojoExecutionException( "", e );
                }
            }
        }
    }

    private String[] findRelativeAidlFileNames( File sourceDirectory )
    {
        final FileRetriever retriever = new FileRetriever( "**/*.aidl" );
        final String[] relativeAidlFileNames = retriever.getFileNames( sourceDirectory );
        if ( relativeAidlFileNames.length == 0 )
        {
            getLog().debug( "ANDROID-904-002: No aidl files found" );
        }
        else
        {
            getLog().info( "ANDROID-904-002: Found aidl files: Count = " + relativeAidlFileNames.length );
        }
        return relativeAidlFileNames;
    }

    /**
     * @return true if the pom type is APK, APKLIB, or APKSOURCES
     */
    private boolean isCurrentProjectAndroid()
    {
        Set<String> androidArtifacts = new HashSet<String>()
        {
            {
                addAll( Arrays.asList( APK, APKLIB, APKSOURCES, AAR ) );
            }
        };
        return androidArtifacts.contains( project.getArtifact().getType() );
    }

}
