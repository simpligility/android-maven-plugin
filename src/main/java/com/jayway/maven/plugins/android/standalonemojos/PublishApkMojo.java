package com.jayway.maven.plugins.android.standalonemojos;

import com.android.annotations.NonNull;
import com.google.api.client.http.FileContent;
import com.google.api.services.androidpublisher.AndroidPublisher;
import com.google.api.services.androidpublisher.model.Apk;
import com.google.api.services.androidpublisher.model.ApkListing;
import com.google.api.services.androidpublisher.model.Track;
import com.jayway.maven.plugins.android.AbstractPublisherMojo;
import com.jayway.maven.plugins.android.common.AndroidPublisherHelper;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.jayway.maven.plugins.android.common.AndroidExtension.APK;

/**
 *
 * @author Joris de Groot
 * @author Benoit Billington
 */
@Mojo( name = "publish-apk", requiresProject = false )
public class PublishApkMojo extends AbstractPublisherMojo
{

    private static final int MAX_CHARS_WHATSNEW = 500;

    @Parameter( property = "android.publisher.track", defaultValue = "alpha" )
    private String track;

    @Parameter( property = "android.publisher.apkpath" )
    private File apkFile;

    @Parameter( property = "android.publisher.filename.whatsnew", defaultValue = "whatsnew.txt" )
    private String fileNameWhatsnew;

    /**
     *
     * @throws org.apache.maven.plugin.MojoExecutionException
     * @throws org.apache.maven.plugin.MojoFailureException
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        if ( apkFile == null )
        {
            apkFile = new File( targetDirectory, finalName + "-aligned." + APK );
        }

        String packageName = extractPackageNameFromApk( apkFile );
        getLog().debug( "Package name: " + packageName );

        initializePublisher( packageName );
        publishApk( packageName );
    }

    private void publishApk( @NonNull String packageName ) throws MojoExecutionException
    {
        try
        {
            getLog().info( "Starting upload of apk " + apkFile.getAbsolutePath() );
            FileContent newApkFile = new FileContent( AndroidPublisherHelper.MIME_TYPE_APK, apkFile );
            Apk apk = edits.apks().upload( packageName, editId, newApkFile ).execute();

            List<Integer> versionCodes  = new ArrayList<Integer>();
            versionCodes.add( apk.getVersionCode() );
            Track newTrack = new Track().setVersionCodes( versionCodes );
            edits.tracks().update( packageName, editId, track, newTrack ).execute();

            publishWhatsNew( packageName, edits, editId, apk );

            edits.commit( packageName, editId ).execute();
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }

    private void publishWhatsNew( String packageName, AndroidPublisher.Edits edits, String editId, Apk apk )
            throws IOException
    {
        warnPlatformDefaultEncoding();
        
        File[] localeDirs = getLocaleDirs();
        if ( localeDirs == null )
        {
            return ;
        }

        for ( File localeDir : localeDirs )
        {
            String recentChanges = readFileWithChecks( localeDir, fileNameWhatsnew,
                    MAX_CHARS_WHATSNEW, "What's new texts are missing." );
            if ( recentChanges == null )
            {
                continue;
            }
            ApkListing newApkListing = new ApkListing().setRecentChanges( recentChanges );
            edits.apklistings()
                    .update( packageName, editId, apk.getVersionCode(), localeDir.getName(), newApkListing )
                    .execute();
        }
    }
}