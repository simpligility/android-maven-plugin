package com.jayway.maven.plugins.android;

import com.android.annotations.NonNull;
import com.google.api.services.androidpublisher.AndroidPublisher;
import com.google.api.services.androidpublisher.model.AppEdit;
import com.jayway.maven.plugins.android.common.AndroidPublisherHelper;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Joris de Groot
 * @author Benoit Billington
 */
public abstract class AbstractPublisherMojo extends AbstractAndroidMojo
{
    private static final String WHATSNEW = "whatsnew.txt";

    @Parameter( property = "android.publisher.google.email", required = true )
    private String publisherEmail;

    @Parameter( property = "android.publisher.google.p12", required = true )
    private File p12File;

    @Parameter( property = "android.publisher.project.name" )
    protected String projectName;

    @Parameter( property = "android.publisher.listing.directory", defaultValue = "${project.basedir}/src/main/play/" )
    protected File listingDirectory;

    protected AndroidPublisher.Edits edits;

    protected String editId;

    // region '419' is a special case in the play store that represents latin america
    protected static final String LOCALE_DIR_PATTERN = "^[a-z]{2}(-([A-Z]{2}|419))?";

    protected void initializePublisher( @NonNull String packageName ) throws MojoExecutionException
    {
        getLog().debug( "Initializing publisher" );
        if ( projectName == null || projectName.equals( "" ) )
        {
            projectName = this.session.getCurrentProject().getName();
        }

        try
        {
            AndroidPublisher publisher = AndroidPublisherHelper.init( projectName, publisherEmail, p12File );
            edits = publisher.edits();
            AndroidPublisher.Edits.Insert editRequest = edits.insert( packageName, null );
            AppEdit edit = editRequest.execute();
            editId = edit.getId();
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }

    public String readFile( File file, int maxChars ) throws IOException
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

        if ( everything.length() > maxChars )
        {
            String message = "Too many characters in file " + file.getName() +  " max allowed is " + maxChars;
            getLog().error( message );
            throw new IOException( message );
        }
        return everything;
    }

    public File[] getLocaleDirs()
    {
        if ( ! listingDirectory.exists() )
        {
            getLog().warn( "Play directory is missing." );
            return null;
        }
        File[] localeDirs = listingDirectory.listFiles( new FilenameFilter()
        {
            @Override
            public boolean accept( File dir, String name )
            {
                Pattern pattern = Pattern.compile( LOCALE_DIR_PATTERN );
                Matcher matcher = pattern.matcher( name );
                return matcher.matches();
            }
        } );

        if ( localeDirs ==  null || localeDirs.length == 0 )
        {
            getLog().warn( "No locale directories found." );
            return null;
        }

        return localeDirs;
    }

    public String readFileWithChecks( File dir, String fileName, int maxChars, String errorMessage )
            throws IOException
    {
        File file = new File( dir, fileName );
        if ( file.exists() )
        {
            return readFile( file, maxChars );
        }
        else
        {
            getLog().warn( errorMessage + " - Filename: " + fileName );
            return null;
        }
    }

}