package com.simpligility.maven.plugins.android.standalonemojos;

import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.FileContent;
import com.google.api.services.androidpublisher.AndroidPublisher;
import com.google.api.services.androidpublisher.model.Listing;
import com.simpligility.maven.plugins.android.AbstractPublisherMojo;
import com.simpligility.maven.plugins.android.common.AndroidPublisherHelper;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    private static final String IMAGE_TYPE_TV_BANNER = "tvBanner";
    private static final String IMAGE_TYPE_TV_SCREENSHOTS = "tvScreenshots";

    @Parameter( property = "android.publisher.package.name" )
    private String packageName;

    @Parameter( property = "android.publisher.filename.full.description", defaultValue = "fulldescription.txt" )
    private String fileNameFullDescription;

    @Parameter( property = "android.publisher.filename.short.description", defaultValue = "shortdescription.txt" )
    private String fileNameShortDescription;

    @Parameter( property = "android.publisher.filename.title", defaultValue = "title.txt" )
    private String fileNameTitle;

    @Parameter( property = "android.publisher.upload.images", defaultValue = "false" )
    private boolean uploadImages;

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
        warnPlatformDefaultEncoding();

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

                    getLog().info( "Updating the listing for " + packageName );
                    final Listing listing = new Listing();
                    listing.setTitle( title );
                    listing.setFullDescription( fullDescription );
                    listing.setShortDescription( shortDescription );
                    edits.listings()
                            .update( packageName, editId, localeDir.getName(), listing )
                            .execute();

                    if ( uploadImages )
                    {
                        // Only one ContentFile allow for featureGraphic
                        uploadSingleGraphic( listingDir, localeDir.getName(), IMAGE_TYPE_FEATURE_GRAPHIC );

                        // Only one ContentFile allow for iconGraphic
                        uploadSingleGraphic( listingDir, localeDir.getName(), IMAGE_TYPE_ICON );

                        // Only one ContentFile allow for promoGraphic
                        uploadSingleGraphic( listingDir, localeDir.getName(), IMAGE_TYPE_PROMO_GRAPHIC );

                        // Upload phoneScreenshots
                        uploadScreenShots( listingDir, localeDir.getName(), IMAGE_TYPE_PHONE_SCREENSHOTS );

                        // Upload sevenInchScreenshots
                        uploadScreenShots( listingDir, localeDir.getName(), IMAGE_TYPE_SEVEN_INCH_SCREENSHOTS );

                        // Upload tenInchScreenshots
                        uploadScreenShots( listingDir, localeDir.getName(), IMAGE_TYPE_TEN_INCH_SCREENSHOTS );

                        // Only one ContentFile allow for tvBanner
                        uploadSingleGraphic( listingDir, localeDir.getName(), IMAGE_TYPE_TV_BANNER );

                        // Upload tvScreenShots
                        uploadScreenShots( listingDir, localeDir.getName(), IMAGE_TYPE_TV_SCREENSHOTS );
                    }

                }
                else
                {
                    getLog().warn( "Listing directory is missing." );
                }
            }

            edits.commit( packageName, editId ).execute();
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Problem in the listing content: " + e.getMessage(), e );
        }
    }

    private List<AbstractInputStreamContent> getImageListAsStream( File listingDir, String graphicPath )
    {
        File graphicDir = new File( listingDir, graphicPath );
        List<AbstractInputStreamContent> images = new ArrayList<AbstractInputStreamContent>();
        if ( graphicDir.exists() )
        {
            File[] imageFiles = graphicDir.listFiles();
            for ( File imageFile : imageFiles )
            {
                images.add( new FileContent( AndroidPublisherHelper.MIME_TYPE_IMAGE, imageFile ) );
            }
        }
        return images;
    }

    private AbstractInputStreamContent getImageAsStream( File listingDir, String graphicPath )
            throws MojoFailureException
    {
        File graphicDir = new File( listingDir, graphicPath );
        if ( graphicDir.exists() )
        {
            File[] files = graphicDir.listFiles();
            if ( files == null || files.length == 0 )
            {
                getLog().warn( "There are no images in " + graphicDir.getAbsolutePath() );
            }
            else if ( files.length > 1 )
            {
                throw new MojoFailureException( "There should be exactly 1 image in " + graphicDir.getAbsolutePath() );
            }
            else
            {
                File graphicFile = files[0];
                return new FileContent( AndroidPublisherHelper.MIME_TYPE_IMAGE, graphicFile );
            }
        }
        return null;
    }

    private void uploadSingleGraphic( File dir, String locale, String imageType )
            throws MojoExecutionException, MojoFailureException
    {
        AbstractInputStreamContent contentGraphic = getImageAsStream( dir, imageType );
        if ( contentGraphic == null )
        {
            return ;
        }

        AndroidPublisher.Edits.Images images = edits.images();
        try
        {
            getLog().info( "Deleting the old " + imageType );
            // Delete current image in play store
            images.deleteall( packageName, editId, locale, imageType ).execute();

            getLog().info( "Uploading the " + imageType );
            // After that upload the new image
            images.upload( packageName, editId, locale, imageType, contentGraphic ).execute();
        }
        catch ( IOException e )
        {
            getLog().error( e.getMessage(), e );
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }

    private void uploadScreenShots( File dir, String locale, String imageType )
            throws MojoFailureException, MojoExecutionException
    {
        List<AbstractInputStreamContent> contentGraphicList = getImageListAsStream( dir, imageType );
        if ( contentGraphicList == null || contentGraphicList.isEmpty() )
        {
            getLog().warn( "There are no images in " + dir.getAbsolutePath() + "/" + imageType );
            return ;
        }

        AndroidPublisher.Edits.Images images = edits.images();
        try
        {
            getLog().info( "Deleting the old " + imageType );
            // Delete all images in play store
            images.deleteall( packageName, editId, locale, imageType ).execute();

            // After that upload the new images
            if ( contentGraphicList.size() > MAX_SCREENSHOTS_SIZE )
            {
                String message = "You can only upload 8 screen shots";
                getLog().error( message );
                throw new MojoFailureException( message );
            }
            else
            {
                int i = 1;
                for ( AbstractInputStreamContent contentGraphic : contentGraphicList )
                {
                    getLog().info( "Uploading " + imageType + " " + i + " out of " + contentGraphicList.size() );
                    images.upload( packageName, editId, locale, imageType, contentGraphic ).execute();
                    i++;
                }
            }
        }
        catch ( IOException e )
        {
            getLog().error( e.getMessage(), e );
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }

}