package com.jayway.maven.plugins.android.common;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.logging.Log;

/**
 * Helper class to deal with end-of-line issues in AndroidManifest.xml files.
 * Used by ManifestMergerMojo, ManifestUpdateMojo, and GenerateSourcesMojo.
 * 
 * @see https://github.com/simpligility/android-maven-plugin/issues/511
 * 
 * @author Matthias Stevens <matthias.stevens@gmail.com>
 */
public class AndroidManifestEOLHelper
{

    private final File mainManifestFile;
    private final boolean sourceFile;
    private final File targetDirectory;
    private final Log log;
    private final boolean hasWindowsLineEndings;
    private File tempFile; 
    
    /**
     * @param mainManifestFile
     * @param sourceFile whether or not the mainManifestFile is the source file (not to be modified) or not
     * @param targetDirectory
     * @param log
     */
    public AndroidManifestEOLHelper( File mainManifestFile, boolean sourceFile, File targetDirectory, Log log )
    {
        this.mainManifestFile = mainManifestFile;
        this.sourceFile = sourceFile;
        this.targetDirectory = targetDirectory;
        this.log = log;
        boolean crlf = false;
        try
        {
            crlf = EOLUtils.hasWindowsEOL( mainManifestFile );
        }
        catch ( Exception e )
        {
            log.warn( e );
        }
        this.hasWindowsLineEndings = crlf;
    }
    
    /**
     * Returns manifest file with Unix-style line endings (LF).
     * If the mainManifest has Windows-style line endings (CRLF) it, or a temporary copy,
     * will be converted to have Unix-style line endings (LF).
     * 
     * @return manifest file with Unix-style line endings (LF)
     */
    public File getUnixEOLManifestFile()
    {
        // Convert to Unix-style line endings (LF) if needed:        
        if ( hasWindowsLineEndings )
        {
            log.info( "Windows-style line-endings detected, converting to Unix-style line endings "
                      + "to avoid that the manifest merger messes things up..." );
            try
            {
                File manifestFileLF;
                if ( sourceFile )
                {
                    // Create temp file:
                    manifestFileLF = new File( targetDirectory, "AndroidManifest_LF_EOL.xml" );
                    FileUtils.copyFile( mainManifestFile, manifestFileLF );
                    tempFile = manifestFileLF;
                }
                else
                {
                    manifestFileLF = mainManifestFile; // use main file if it is not the source manifest
                }
                // Convert line endings:
                EOLUtils.convertToUnixEOL( manifestFileLF );
                // Return manifest with LF line endings:
                return manifestFileLF;
            }
            catch ( IOException ioe )
            {
                log.warn( "Failed to convert manifest file line endings from CRLF to LF", ioe );
            }
        }
        return mainManifestFile; // use unchanged manifest
    }
    
    /**
     * Converts line-ending in given manifest file back to Windows-style (CRLF), if needed.
     * Also deletes the temp file, if one was created in {@link #getUnixEOLManifestFile()}.
     * 
     * @param destinationManifestFile
     */
    public void restoreEOL( File destinationManifestFile )
    {
        if ( hasWindowsLineEndings )
        {
            log.info( "Converting back to Windows-style line-endings..." );
            try
            {
                EOLUtils.convertToWindowsEOL(  destinationManifestFile );
            }
            catch ( IOException ioe )
            {
                log.warn( "Failed to convert manifest file line endings from LF to CRLF", ioe );
            }
            // Delete temp file if there is one and it is not the destination file:
            if ( ! destinationManifestFile.equals( tempFile ) )
            {
                FileUtils.deleteQuietly( tempFile );
            }
        }
    }
        
}
