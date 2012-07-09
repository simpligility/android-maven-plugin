package com.jayway.maven.plugins.android;

import com.android.ddmlib.IDevice;
import com.github.rtyley.android.screenshot.paparazzo.OnDemandScreenshotService;
import com.github.rtyley.android.screenshot.paparazzo.processors.AnimatedGifCreator;
import com.github.rtyley.android.screenshot.paparazzo.processors.ImageSaver;
import com.github.rtyley.android.screenshot.paparazzo.processors.ImageScaler;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;

import static com.github.rtyley.android.screenshot.paparazzo.processors.util.Dimensions.square;
import static com.jayway.maven.plugins.android.common.DeviceHelper.getDescriptiveName;
import static org.apache.commons.io.FileUtils.forceMkdir;

public class ScreenshotServiceWrapper implements DeviceCallback
{

    private final DeviceCallback delegate;
    private final Log log;
    private final File screenshotParentDir;

    public ScreenshotServiceWrapper( DeviceCallback delegate, MavenProject project, Log log )
    {
        this.delegate = delegate;
        this.log = log;
        screenshotParentDir = new File( project.getBuild().getDirectory(), "screenshots" );
        create( screenshotParentDir );
    }


    @Override
    public void doWithDevice( final IDevice device ) throws MojoExecutionException, MojoFailureException
    {
        String deviceName = getDescriptiveName( device );

        File deviceGifFile = new File( screenshotParentDir, deviceName + ".gif" );
        File deviceScreenshotDir = new File( screenshotParentDir, deviceName );
        create( deviceScreenshotDir );

        OnDemandScreenshotService screenshotService =
                new OnDemandScreenshotService( device, new ImageSaver( deviceScreenshotDir ),
                        new ImageScaler( new AnimatedGifCreator( deviceGifFile ), square( 320 ) ) );

        screenshotService.start();

        delegate.doWithDevice( device );

        screenshotService.finish();
    }

    private void create( File dir )
    {
        try
        {
            forceMkdir( dir );
        }
        catch ( IOException e )
        {
            log.warn( "Unable to create screenshot directory: " + screenshotParentDir.getAbsolutePath(), e );
        }
    }
}
