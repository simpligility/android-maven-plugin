package com.simpligility.maven.plugins.android.configuration;

import java.io.File;

import com.simpligility.maven.plugins.android.common.AndroidExtension;

/**
 * DeployApk is the configuration pojo for the DeployApk, UndeployApk and RedeployApk mojos.
 * 
 * @author Manfred Moser <manfred@simpligility.com>
 */
public class DeployApk
{
    private File filename;
    private String packagename;
    
    public File getFilename() 
    {
        return filename;
    }

    public void setFilename( File filename ) 
    {
        this.filename = filename;
    }

    public String getPackagename() 
    {
        return packagename;
    }

    public void setPackagename( String packagename ) 
    {
        this.packagename = packagename;
    }

    public static ValidationResponse validFileParameter( File parsedFilename )
    {
        ValidationResponse result;
        if ( parsedFilename == null )
        {
            result = new ValidationResponse( false, 
                    "\n\n The parameter android.deployapk.filename is missing. \n" ) ;
        }
        else if ( !parsedFilename.isFile() )
        {
            result = new ValidationResponse( false, 
                    "\n\n The file parameter does not point to a file: " 
                    + parsedFilename.getAbsolutePath() + "\n" );
        }
        else if ( !parsedFilename.getAbsolutePath().toLowerCase().endsWith( AndroidExtension.APK ) )
        {
            result = new ValidationResponse( false, 
                    "\n\n The file parameter does not point to an APK: " 
                    + parsedFilename.getAbsolutePath() + "\n" );
        } 
        else 
        {
            result = new ValidationResponse( true,
                    "\n\n Valid file parameter: " 
                    + parsedFilename.getAbsolutePath() + "\n" );
        }
        return result;
    }
}
