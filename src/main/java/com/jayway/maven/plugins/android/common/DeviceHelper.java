package com.jayway.maven.plugins.android.common;

import com.android.ddmlib.IDevice;
import org.apache.commons.lang.StringUtils;

/**
 * A bunch of helper methods for dealing with IDevice instances.
 *
 * @author Manfred Moser <manfred@simpligility.com>
 */
public class DeviceHelper
{

    private static final String MANUFACTURER_PROPERTY = "ro.product.manufacturer";
    private static final String MODEL_PROPERTY = "ro.product.model";
    private static final String SEPARATOR = "_";
    /**
     * Get a device identifier string that is suitable for filenames as well as log messages.
     * This means it is human readable and contains no spaces.
     * Used for instrumentation test report file names so see more at
     * AbstractInstrumentationMojo#testCreateReport javadoc since
     * that is the public documentation.
     */
    public static String getDescriptiveName( IDevice device )
    {
        // if any of this logic changes update javadoc for
        // AbstractInstrumentationMojo#testCreateReport
        StringBuilder identifier = new StringBuilder().append( device.getSerialNumber() );
        if ( device.getAvdName() != null )
        {
            identifier.append( SEPARATOR ).append( device.getAvdName() );
        }
        String manufacturer = getManufacturer( device );
        if ( StringUtils.isNotBlank( manufacturer ) )
        {
            identifier.append( SEPARATOR ).append( manufacturer );
        }
        String model = getModel( device );
        if ( StringUtils.isNotBlank( model ) )
        {
            identifier.append( SEPARATOR ).append( model );
        }

        return FileNameHelper.fixFileName( identifier.toString() );
    }
    
    public static String getDeviceLogLinePrefix( IDevice device ) 
    {
        return getDescriptiveName( device ) + " :   ";
    }

    /**
     * @return the manufacturer of the device as set in #MANUFACTURER_PROPERTY, typically "unknown" for emulators
     */
    public static String getManufacturer( IDevice device )
    {
        return StringUtils.deleteWhitespace( device.getProperty( MANUFACTURER_PROPERTY ) );
    }

    /**
     * @return the model of the device as set in #MODEL_PROPERTY, typically "sdk" for emulators
     */
    public static String getModel( IDevice device )
    {
        return StringUtils.deleteWhitespace( device.getProperty( MODEL_PROPERTY ) );
    }

    /**
     * @return the descriptive name with online/offline/unknown status string appended.
     */
    public static String getDescriptiveNameWithStatus( IDevice device )
    {
        String status;
        if ( device.isOnline() )
        {
            status = "Online";
        }
        else
        {
            if ( device.isOffline() )
            {
                status = "Offline";
            }
            else
            {
                status = "Unknown";
            }
        }
        return getDescriptiveName( device ) + " " + status;
    }
}
