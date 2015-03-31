package com.jayway.maven.plugins.android.standalonemojos;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.builder.core.AndroidBuilder;
import com.android.ide.common.process.JavaProcessExecutor;
import com.android.ide.common.process.ProcessExecutor;
import com.android.ide.common.process.ProcessOutputHandler;
import com.android.manifmerger.ManifestMerger2;
import com.jayway.maven.plugins.android.AbstractAndroidMojo;
import com.jayway.maven.plugins.android.common.AndroidExtension;
import com.jayway.maven.plugins.android.common.MavenManifestDependency;
import com.jayway.maven.plugins.android.configuration.ManifestMerger;
import com.jayway.maven.plugins.android.configuration.UsesSdk;
import com.jayway.maven.plugins.android.configuration.VersionGenerator;
import com.jayway.maven.plugins.android.phase01generatesources.MavenILogger;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Manifest Merger V2 <code>AndroidManifest.xml</code> file.
 * http://tools.android.com/tech-docs/new-build-system/user-guide/manifest-merger
 *
 * @author Benoit Billington <benoit.billington@gmail.com>
 */
@Mojo( name = "manifest-merger", defaultPhase = LifecyclePhase.PROCESS_RESOURCES )
public class ManifestMergerMojo extends AbstractAndroidMojo
{

    /**
     * Configuration for the manifest-update goal.
     * <p>
     * You can configure this mojo to update the following basic manifestMerger attributes:
     * </p>
     * <p>
     * <code>android:versionName</code> on the <code>manifestMerger</code> element.
     * <code>android:versionCode</code> on the <code>manifestMerger</code> element.
     * </p>
     * <p>
     * You can configure attributes in the plugin configuration like so
     * <p/>
     * <pre>
     *   &lt;plugin&gt;
     *     &lt;groupId&gt;com.jayway.maven.plugins.android.generation2&lt;/groupId&gt;
     *     &lt;artifactId&gt;android-maven-plugin&lt;/artifactId&gt;
     *     &lt;executions&gt;
     *       &lt;execution&gt;
     *         &lt;id&gt;merge-manifest&lt;/id&gt;
     *         &lt;goals&gt;
     *           &lt;goal&gt;manifest-merger&lt;/goal&gt;
     *         &lt;/goals&gt;
     *         &lt;configuration&gt;
     *           &lt;manifestMerger&gt;
     *             &lt;versionName&gt;&lt;/versionName&gt;
     *             &lt;versionCode&gt;123&lt;/versionCode&gt;
     *             &lt;versionCodeUpdateFromVersion&gt;true|false&lt;/versionCodeUpdateFromVersion&gt;
     *             &lt;mergeLibraries&gt;true|false&lt;/mergeLibraries&gt;
     *             &lt;mergeReportFile&gt;${project.build.directory}/ManifestMergeReport.txt&lt;/mergeReportFile&gt;
     *             &lt;usesSdk&gt;
     *               &lt;minSdkVersion&gt;14&lt;/minSdkVersion&gt;
     *               &lt;targetSdkVersion&gt;21&lt;/targetSdkVersion&gt;
     *             &lt;/usesSdk&gt;
     *           &lt;/manifestMerger&gt;
     *         &lt;/configuration&gt;
     *       &lt;/execution&gt;
     *     &lt;/executions&gt;
     *   &lt;/plugin&gt;
     * </pre>
     * <p/>
     * or use properties set in the pom or settings file or supplied as command line parameter. Add
     * "android." in front of the property name for command line usage. All parameters follow a
     * manifestMerger.* naming convention.
     * <p/>
     */
    @Parameter
    private ManifestMerger manifestMerger;

    /**
     * Update the <code>android:versionName</code> with the specified parameter. If left empty it
     * will use the version number of the project. Exposed via the project property
     * <code>android.manifestMerger.versionName</code>.
     */
    @Parameter( property = "android.manifestMerger.versionName", defaultValue = "${project.version}" )
    protected String manifestVersionName;

    /**
     * Update the <code>android:versionCode</code> attribute with the specified parameter. Exposed via
     * the project property <code>android.manifestMerger.versionCode</code>.
     */
    @Parameter( property = "android.manifestMerger.versionCode", defaultValue = "1" )
    protected Integer manifestVersionCode;

    /**
     * Update the <code>android:versionCode</code> attribute automatically from the project version
     * e.g 3.2.1 will become version code 3002001. As described in this blog post
     * http://www.simpligility.com/2010/11/release-version-management-for-your-android-application/
     * but done without using resource filtering. The value is exposed via the project property
     * property <code>android.manifest.versionCodeUpdateFromVersion</code> and the resulting value
     * as <code>android.manifest.versionCode</code>.
     * For the purpose of generating the versionCode, if a version element is missing it is presumed to be 0.
     * The maximum values for the version increment and version minor values are 999,
     * the version major should be no larger than 2000.  Any other suffixes do not
     * participate in the version code generation.
     */
    @Parameter( property = "android.manifest.versionCodeUpdateFromVersion", defaultValue = "false" )
    protected Boolean manifestVersionCodeUpdateFromVersion = false;

    /**
     * The number of digits per version element. Must be specified as a comma/semicolon separated list of
     * digits, one for each version element, Exposed via the project property
     * <code>android.manifestMerger.versionDigits</code>.
     */
    @Parameter( property = "android.manifestMerger.versionDigits", defaultValue = "4,3,3" )
    protected String manifestVersionDigits;

    /**
     * Merge Manifest with library projects. Exposed via the project property
     * <code>android.manifestMerger.mergeLibraries</code>.
     */
    @Parameter( property = "android.manifestMerger.mergeLibraries", defaultValue = "false" )
    protected Boolean manifestMergeLibraries;

    /**
     * Merge Manifest with library projects. Exposed via the project property
     * <code>android.manifestMerger.mergeLibraries</code>.
     */
    @Parameter( property = "android.manifestMerger.mergeReportFile" )
    protected File manifestMergeReportFile;

    /**
     *  Update the uses-sdk tag. It can be configured to change: <code>android:minSdkVersion</code>,
     *  <code>android:maxSdkVersion</code> and <code>android:targetSdkVersion</code>
     */
    protected UsesSdk manifestUsesSdk;
    private Boolean parsedVersionCodeUpdateFromVersion;
    private String parsedVersionDigits;
    private Boolean parsedMergeLibraries;
    private String parsedVersionName;
    private Integer parsedVersionCode;
    private UsesSdk parsedUsesSdk;
    private File parsedMergeReportFile;

    /**
     * @throws org.apache.maven.plugin.MojoExecutionException
     * @throws org.apache.maven.plugin.MojoFailureException
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        if ( ! AndroidExtension.isAndroidPackaging( project.getPackaging() ) )
        {
            return; // skip, not an android project.
        }

        if ( androidManifestFile == null )
        {
            getLog().debug( "skip, no androidmanifest.xml defined (androidManifestFile rare case)" );
            return; // skip, no androidmanifest.xml defined (rare case)
        }

        parseConfiguration();

        getLog().info( "Attempting to update manifest " + androidManifestFile );
        getLog().debug( "    usesSdk=" + parsedUsesSdk );
        getLog().debug( "    versionName=" + parsedVersionName );
        getLog().debug( "    versionCode=" + parsedVersionCode );
        getLog().debug( "    usesSdk=" + parsedUsesSdk );
        getLog().debug( "    versionCodeUpdateFromVersion=" + parsedVersionCodeUpdateFromVersion );
        getLog().debug( "    versionDigits=" + parsedVersionDigits );
        getLog().debug( "    mergeLibraries=" + parsedMergeLibraries );
        getLog().debug( "    mergeReportFile=" + parsedMergeReportFile );

        if ( ! androidManifestFile.exists() )
        {
            return; // skip, no AndroidManifest.xml file found.
        }

        getLog().debug( "Using manifest merger V2" );
        manifestMergerV2();
    }

    private void parseConfiguration()
    {
        // manifestMerger element found in plugin config in pom
        if ( manifestMerger != null )
        {
            if ( StringUtils.isNotEmpty( manifestMerger.getVersionName() ) )
            {
                parsedVersionName = manifestMerger.getVersionName();
            }
            else
            {
                parsedVersionName = manifestVersionName;
            }
            if ( manifestMerger.getVersionCode() != null )
            {
                parsedVersionCode = manifestMerger.getVersionCode();
            }
            else
            {
                parsedVersionCode = manifestVersionCode;
            }
            if ( manifestMerger.getVersionCodeUpdateFromVersion() != null )
            {
                parsedVersionCodeUpdateFromVersion = manifestMerger.getVersionCodeUpdateFromVersion();
            }
            else
            {
                parsedVersionCodeUpdateFromVersion = manifestVersionCodeUpdateFromVersion;
            }
            if ( manifestMerger.getVersionDigits() != null )
            {
                parsedVersionDigits = manifestMerger.getVersionDigits();
            }
            else
            {
                parsedVersionDigits = manifestVersionDigits;
            }
            if ( manifestMerger.getUsesSdk() != null )
            {
                parsedUsesSdk = manifestMerger.getUsesSdk();
            }
            else
            {
                parsedUsesSdk = manifestUsesSdk;
            }
            if ( manifestMerger.getMergeLibraries() != null )
            {
                parsedMergeLibraries = manifestMerger.getMergeLibraries();
            }
            else
            {
                parsedMergeLibraries = manifestMergeLibraries;
            }
            if ( manifestMerger.getMergeReportFile() != null )
            {
                parsedMergeReportFile = manifestMerger.getMergeReportFile();
            }
            else
            {
                parsedMergeReportFile = manifestMergeReportFile;
            }
        }
        else
        {
            parsedVersionName = manifestVersionName;
            parsedVersionCode = manifestVersionCode;
            parsedUsesSdk = manifestUsesSdk;
            parsedVersionCodeUpdateFromVersion = manifestVersionCodeUpdateFromVersion;
            parsedVersionDigits = manifestVersionDigits;
            parsedMergeLibraries = manifestMergeLibraries;
            parsedMergeReportFile = manifestMergeReportFile;
        }
    }


    public void manifestMergerV2() throws MojoExecutionException, MojoFailureException
    {
//        @NonNull ProcessExecutor processExecutor,
//        @NonNull JavaProcessExecutor javaProcessExecutor,
//        @NonNull ProcessOutputHandler processOutputHandler,
        // we might have to make one of each for the Android Maven Plugin in general and then just use them here... 
        // e.g. the class com.android.ide.common.process.DefaultProcessExecutor could be adapted
        AndroidBuilder builder = new AndroidBuilder( project.toString(), "created by Android Maven Plugin",
                null, null, null, new MavenILogger( getLog() ), false );

        String minSdkVersion = null;
        String targetSdkVersion = null;
        int versionCode;
        if ( parsedUsesSdk != null )
        {
            minSdkVersion = parsedUsesSdk.getMinSdkVersion();
            targetSdkVersion = parsedUsesSdk.getTargetSdkVersion();
        }
        if ( parsedVersionCodeUpdateFromVersion )
        {
            VersionGenerator gen = new VersionGenerator( parsedVersionDigits );
            
            versionCode = gen.generate( parsedVersionName );
        }
        else
        {
            versionCode = parsedVersionCode;
        }
        List<MavenManifestDependency> manifestDependencies = new ArrayList<MavenManifestDependency>();

        if ( parsedMergeLibraries )
        {
            final Set<Artifact> allArtifacts = project.getDependencyArtifacts();
            Set<Artifact> dependencyArtifacts = getArtifactResolverHelper().getFilteredArtifacts( allArtifacts );

            for ( Artifact dependency : dependencyArtifacts )
            {
                final File unpackedLibFolder = getUnpackedLibFolder( dependency );
                final File manifestFile = new File( unpackedLibFolder, SdkConstants.FN_ANDROID_MANIFEST_XML );
                if ( manifestFile.exists() )
                {
                    manifestDependencies.add( new MavenManifestDependency( manifestFile,
                            manifestFile.getAbsolutePath(), new ArrayList<MavenManifestDependency>() ) );
                }
            }
        }

        builder.mergeManifests(
                androidManifestFile, new ArrayList<File>(), manifestDependencies, "",
                versionCode, parsedVersionName,
                minSdkVersion, targetSdkVersion, null,
                destinationManifestFile.getPath(), ManifestMerger2.MergeType.APPLICATION,
                new HashMap<String, String>(), parsedMergeReportFile );

    }

}
