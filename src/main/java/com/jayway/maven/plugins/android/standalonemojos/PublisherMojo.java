package com.jayway.maven.plugins.android.standalonemojos;

import com.google.api.client.http.FileContent;
import com.google.api.services.androidpublisher.AndroidPublisher;
import com.google.api.services.androidpublisher.model.Apk;
import com.google.api.services.androidpublisher.model.ApkListing;
import com.google.api.services.androidpublisher.model.AppEdit;
import com.google.api.services.androidpublisher.model.Track;
import com.jayway.maven.plugins.android.AbstractAndroidMojo;
import com.jayway.maven.plugins.android.common.AndroidPublisherHelper;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.jayway.maven.plugins.android.common.AndroidExtension.APK;

/**
 *
 * @author Joris de Groot
 * @author Benoit Billington
 */
@Mojo( name = "publisher", requiresProject = false )
public class PublisherMojo extends AbstractAndroidMojo
{

    private static final String WHATSNEW = "whatsnew.txt";

    @Parameter( property = "android.publisher.email", required = true )
    private String publisherEmail;

    @Parameter( property = "android.publisher.track", defaultValue = "alpha" )
    private String track;

    @Parameter( property = "android.publisher.apkpath" )
    private File apkFile;

    @Parameter( property = "android.publisher.p12", required = true )
    private File p12File;

    @Parameter( property = "android.publisher.project.name" )
    private String projectName;

    @Parameter( property = "android.publisher.listing.directory", defaultValue = "${project.basedir}/src/main/play/" )
    private File listingDirectory;

    // region '419' is a special case in the play store that represents latin america
    private static final String LOCALE_DIR_PATTERN = "^[a-z]{2}(-([A-Z]{2}|419))?";

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

        if ( projectName == null || projectName.equals( "" ) )
        {
            projectName = this.session.getCurrentProject().getName();
        }


        String packageName = extractPackageNameFromApk( apkFile );
        getLog().debug( "Package name: " + packageName );

        publishApk( packageName );
    }

    private void publishApk( String packageName ) throws MojoExecutionException
    {
        try
        {
            getLog().debug( "Initializing publisher" );
            AndroidPublisher publisher = AndroidPublisherHelper.init( projectName, publisherEmail, p12File );
            AndroidPublisher.Edits edits = publisher.edits();
            AndroidPublisher.Edits.Insert editRequest = edits.insert( packageName, null );
            AppEdit edit = editRequest.execute();

            String editId = edit.getId();

            getLog().info( "Starting upload of apk " + apkFile.getAbsolutePath() );
            FileContent newApkFile = new FileContent( AndroidPublisherHelper.MIME_TYPE_APK, apkFile );
            Apk apk = edits.apks().upload( packageName, editId, newApkFile ).execute();

            List<Integer> versionCodes  = new ArrayList<Integer>();
            versionCodes.add( apk.getVersionCode() );
            Track newTrack = new Track().setVersionCodes( versionCodes );
            edits.tracks().update(packageName, editId, track, newTrack).execute();

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
        File [] localeDirs = listingDirectory.listFiles( new FilenameFilter()
        {
            @Override
            public boolean accept( File dir, String name )
            {
                Pattern pattern = Pattern.compile( LOCALE_DIR_PATTERN );
                Matcher matcher = pattern.matcher( name );
                return matcher.matches();
            }
        } );
        File whatsNewFile;
        for ( File localeDir : localeDirs )
        {
            whatsNewFile = new File( localeDir, WHATSNEW );
            if ( whatsNewFile.exists() )
            {
                ApkListing newApkListing = new ApkListing().setRecentChanges( readFile( whatsNewFile ) );
                edits.apklistings()
                        .update( packageName, editId, apk.getVersionCode(), whatsNewFile.getParentFile().getName(),
                                newApkListing )
                        .execute();
            }
        }
    }

    private String readFile( File file ) throws IOException
    {
        String everything;
        BufferedReader br = new BufferedReader( new FileReader( file ) );
        try
        {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while ( line != null )
            {
                sb.append( line );
                sb.append( "\n" );
                line = br.readLine();
            }
            everything = sb.toString();
        }
        finally
        {
            br.close();
        }
        return everything;
    }

}

