package com.jayway.maven.plugins.android.standalonemojos;

import com.google.api.services.androidpublisher.model.Listing;
import com.jayway.maven.plugins.android.AbstractPublisherMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;

/**
 *
 * @author Joris de Groot
 * @author Benoit Billington
 */
@Mojo( name = "publish-listing", requiresProject = false )
public class PublishListingMojo extends AbstractPublisherMojo
{

    private static final int MAX_CHARS_TITLE = 30;
    private static final int MAX_CHARS_SHORT_DESCRIPTION = 80;
    private static final int MAX_CHARS_FULL_DESCRIPTION = 4000;
    private static final int MAX_SCREENSHOTS_SIZE = 8;

    private static final String LISTING_PATH = "listing/";

    private static final String IMAGE_TYPE_FEATURE_GRAPHIC = "featureGraphic";
    private static final String IMAGE_TYPE_ICON = "icon";
    private static final String IMAGE_TYPE_PHONE_SCREENSHOTS = "phoneScreenshots";
    private static final String IMAGE_TYPE_PROMO_GRAPHIC = "promoGraphic";
    private static final String IMAGE_TYPE_SEVEN_INCH_SCREENSHOTS = "sevenInchScreenshots";
    private static final String IMAGE_TYPE_TEN_INCH_SCREENSHOTS = "tenInchScreenshots";

    @Parameter( property = "android.publisher.package.name" )
    private String packageName;

    @Parameter( property = "android.publisher.filename.full.description", defaultValue = "fulldescription.txt" )
    private String fileNameFullDescription;

    @Parameter( property = "android.publisher.filename.short.description", defaultValue = "shortdescription.txt" )
    private String fileNameShortDescription;

    @Parameter( property = "android.publisher.filename.title", defaultValue = "title.txt" )
    private String fileNameTitle;

    /**
     *
     * @throws org.apache.maven.plugin.MojoExecutionException
     * @throws org.apache.maven.plugin.MojoFailureException
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        if ( packageName == null || packageName.equals( "" ) )
        {
            packageName = extractPackageNameFromAndroidManifest( androidManifestFile );
        }

        getLog().debug( "Package name: " + packageName );

        initializePublisher( packageName );
        publishListing();
    }

    private void publishListing() throws MojoExecutionException, MojoFailureException
    {

        File[] localeDirs = getLocaleDirs();
        if ( localeDirs == null )
        {
            return ;
        }
        File listingDir;
        try
        {
            for ( File localeDir : localeDirs )
            {
                listingDir = new File( localeDir, LISTING_PATH );
                if ( listingDir.exists() )
                {
                    String fullDescription = readFileWithChecks( listingDir, fileNameFullDescription,
                            MAX_CHARS_FULL_DESCRIPTION, "Full description file is missing." );

                    String shortDescription = readFileWithChecks( listingDir, fileNameShortDescription,
                            MAX_CHARS_SHORT_DESCRIPTION, "Short description file is missing." );

                    String title = readFileWithChecks( listingDir, fileNameTitle,
                            MAX_CHARS_TITLE, "Title file is missing." );

                    if ( title == null || shortDescription == null || fullDescription == null )
                    {
                        throw new MojoFailureException( "Incomplete listing" );
                    }

                    final Listing listing = new Listing();
                    listing.setTitle( title );
                    listing.setFullDescription( fullDescription );
                    listing.setShortDescription( shortDescription );
                    edits.listings()
                            .update( packageName, editId, localeDir.getName(), listing )
                            .execute();

                    edits.commit( packageName, editId ).execute();
                }
                else
                {
                    getLog().warn( "Listing directory is missing." );
                }
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Problem in the listing content: " + e.getMessage(), e );
        }
    }

}