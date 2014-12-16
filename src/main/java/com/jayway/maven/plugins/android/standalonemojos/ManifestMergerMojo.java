package com.jayway.maven.plugins.android.standalonemojos;

import com.android.builder.core.AndroidBuilder;
import com.android.builder.dependency.ManifestDependency;
import com.android.manifmerger.ManifestMerger2;
import com.jayway.maven.plugins.android.AbstractAndroidMojo;
import com.jayway.maven.plugins.android.common.AndroidExtension;
import com.jayway.maven.plugins.android.configuration.ManifestMerger;
import com.jayway.maven.plugins.android.configuration.UsesSdk;
import com.jayway.maven.plugins.android.phase01generatesources.MavenILogger;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

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
     *             &lt;usesSdk&gt;
     *               &lt;minSdkVersion&gt;14&lt;/minSdkVersion&gt;
     *               &lt;targetSdkVersion&gt;21&lt;/targetSdkVersion&gt;
     *             &lt;/versionCode&gt;
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
    @Parameter( property = "android.manifestMerger.versionCode" )
    protected Integer manifestVersionCode;

    /**
     *  Update the uses-sdk tag. It can be configured to change: <code>android:minSdkVersion</code>,
     *  <code>android:maxSdkVersion</code> and <code>android:targetSdkVersion</code>
     */
    protected UsesSdk manifestUsesSdk;

    private String parsedVersionName;
    private Integer parsedVersionCode;
    private UsesSdk parsedUsesSdk;

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

        if ( sourceManifestFile == null )
        {
            return; // skip, no androidmanifest.xml defined (rare case)
        }

        parseConfiguration();

        getLog().info( "Attempting to update manifest " + sourceManifestFile );
        getLog().debug( "    usesSdk=" + parsedUsesSdk );
        getLog().debug( "    versionName=" + parsedVersionName );
        getLog().debug( "    versionCode=" + parsedVersionCode );
        getLog().debug( "    parsedUsesSdk=" + parsedUsesSdk );

        if ( ! sourceManifestFile.exists() )
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

            if ( manifestMerger.getUsesSdk() != null )
            {
                parsedUsesSdk = manifestMerger.getUsesSdk();
            }
            else
            {
                parsedUsesSdk = manifestUsesSdk;
            }
        }
        else
        {
            parsedVersionName = manifestVersionName;
            parsedVersionCode = manifestVersionCode;
            parsedUsesSdk = manifestUsesSdk;
        }
    }


    public void manifestMergerV2() throws MojoExecutionException, MojoFailureException
    {
        AndroidBuilder builder = new AndroidBuilder( project.toString(), "created by Android Maven Plugin",
                new MavenILogger( getLog() ), false );

        getLog().debug( "sourceManifestFile " + sourceManifestFile );
        getLog().debug( "androidManifestFile " + androidManifestFile );

        builder.mergeManifests(
                sourceManifestFile, new ArrayList<File>(), new ArrayList<ManifestDependency>(), "",
                parsedVersionCode, parsedVersionName,
                parsedUsesSdk.getMinSdkVersion(), parsedUsesSdk.getTargetSdkVersion(), null,
                androidManifestFile.getPath(), ManifestMerger2.MergeType.APPLICATION,
                new HashMap<String, String>(), null );
                /* @NonNull List<File> manifestOverlays,
                @NonNull List<? extends ManifestDependency> libraries,
            String packageOverride, int versionCode,
        String versionName, @Nullable
    String minSdkVersion,
        @Nullable String targetSdkVersion,
        @Nullable Integer maxSdkVersion,
        @NonNull String outManifestLocation,
        MergeType mergeType,
        Map<String, String> placeHolders) {);*/
    }

}
