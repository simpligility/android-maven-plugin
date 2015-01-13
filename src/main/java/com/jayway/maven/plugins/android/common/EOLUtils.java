package com.jayway.maven.plugins.android.common;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 * Helper class to deal with end-of-line markers in text files.
 * 
 * Loosely based on these examples:
 *  - http://stackoverflow.com/a/9456947/1084488 (cc by-sa 3.0)
 *  - http://svn.apache.org/repos/asf/tomcat/trunk/java/org/apache/tomcat/buildutil/CheckEol.java (Apache License v2.0)
 * 
 * This file is posted here to meet the "ShareAlike" requirement of cc by-sa 3.0:
 *    http://stackoverflow.com/a/27930311/1084488
 * 
 * @author Matthias Stevens <matthias.stevens@gmail.com>
 */
public class EOLUtils
{

    /**
     * Unix-style end-of-line marker (LF)
     */
    private static final String EOL_UNIX = "\n";

    /**
     * Windows-style end-of-line marker (CRLF)
     */
    private static final String EOL_WINDOWS = "\r\n";

    /**
     * "Old Mac"-style end-of-line marker (CR)
     */
    private static final String EOL_OLD_MAC = "\r";

    /**
     * Default end-of-line marker on current system
     */
    private static final String EOL_SYSTEM_DEFAULT = System.getProperty( "line.separator" );

    /**
     * The support end-of-line marker modes
     */
    public static enum Mode
    {
        /**
         * Unix-style end-of-line marker ("\n")
         */
        LF,

        /**
         * Windows-style end-of-line marker ("\r\n") 
         */
        CRLF,

        /**
         * "Old Mac"-style end-of-line marker ("\r")
         */
        CR
    }

    /**
     * The default end-of-line marker mode for the current system
     */
    public static final Mode SYSTEM_DEFAULT = ( EOL_SYSTEM_DEFAULT.equals( EOL_UNIX ) ? Mode.LF : ( EOL_SYSTEM_DEFAULT
        .equals( EOL_WINDOWS ) ? Mode.CRLF : ( EOL_SYSTEM_DEFAULT.equals( EOL_OLD_MAC ) ? Mode.CR : null ) ) );
    static
    {
        // Just in case...
        if ( SYSTEM_DEFAULT == null )
        {
            throw new IllegalStateException( "Could not determine system default end-of-line marker" );
        }
    }

    /**
     * Determines the end-of-line {@link Mode} of a text file.
     * 
     * @param textFile the file to investigate
     * @return the end-of-line {@link Mode} of the given file, or {@code null} if it could not be determined
     * @throws Exception
     */
    public static Mode determineEOL( File textFile )
        throws Exception
    {
        if ( !textFile.exists() )
        {
            throw new IOException( "Could not find file to open: " + textFile.getAbsolutePath() );
        }

        FileInputStream fileIn = new FileInputStream( textFile );
        BufferedInputStream bufferIn = new BufferedInputStream( fileIn );
        try
        {
            int prev = -1;
            int ch;
            while ( ( ch = bufferIn.read() ) != -1 )
            {
                if ( ch == '\n' )
                {
                    if ( prev == '\r' )
                    {
                        return Mode.CRLF;
                    }
                    else
                    {
                        return Mode.LF;
                    }
                }
                else if ( prev == '\r' )
                {
                    return Mode.CR;
                }
                prev = ch;
            }
            throw new Exception( "Could not determine end-of-line marker mode" );
        }
        catch ( IOException ioe )
        {
            throw new Exception( "Could not determine end-of-line marker mode", ioe );
        }
        finally
        {
            // Clean up:
            IOUtils.closeQuietly( bufferIn );
        }
    }

    /**
     * Checks whether the given text file has Windows-style (CRLF) line endings.
     * 
     * @param textFile the file to investigate
     * @return
     * @throws Exception
     */
    public static boolean hasWindowsEOL( File textFile )
        throws Exception
    {
        return Mode.CRLF.equals( determineEOL( textFile ) );
    }

    /**
     * Checks whether the given text file has Unix-style (LF) line endings.
     * 
     * @param textFile the file to investigate
     * @return
     * @throws Exception
     */
    public static boolean hasUnixEOL( File textFile )
        throws Exception
    {
        return Mode.LF.equals( determineEOL( textFile ) );
    }

    /**
     * Checks whether the given text file has "Old Mac"-style (CR) line endings.
     * 
     * @param textFile the file to investigate
     * @return
     * @throws Exception
     */
    public static boolean hasOldMacEOL( File textFile )
        throws Exception
    {
        return Mode.CR.equals( determineEOL( textFile ) );
    }

    /**
     * Checks whether the given text file has line endings that conform to the system default mode (e.g. LF on Unix).
     * 
     * @param textFile the file to investigate
     * @return
     * @throws Exception
     */
    public static boolean hasSystemDefaultEOL( File textFile )
        throws Exception
    {
        return SYSTEM_DEFAULT.equals( determineEOL( textFile ) );
    }

    /**
     * Convert the line endings in the given file to Unix-style (LF).
     * 
     * @param textFile the file to process
     * @throws IOException
     */
    public static void convertToUnixEOL( File textFile )
        throws IOException
    {
        convertLineEndings( textFile, EOL_UNIX );
    }

    /**
     * Convert the line endings in the given file to Windows-style (CRLF).
     * 
     * @param textFile the file to process
     * @throws IOException
     */
    public static void convertToWindowsEOL( File textFile )
        throws IOException
    {
        convertLineEndings( textFile, EOL_WINDOWS );
    }

    /**
     * Convert the line endings in the given file to "Old Mac"-style (CR).
     * 
     * @param textFile the file to process
     * @throws IOException
     */
    public static void convertToOldMacEOL( File textFile )
        throws IOException
    {
        convertLineEndings( textFile, EOL_OLD_MAC );
    }

    /**
     * Convert the line endings in the given file to the system default mode.
     * 
     * @param textFile the file to process
     * @throws IOException
     */
    public static void convertToSystemEOL( File textFile )
        throws IOException
    {
        convertLineEndings( textFile, EOL_SYSTEM_DEFAULT );
    }

    /**
     * Line endings conversion method.
     * 
     * @param textFile the file to process
     * @param eol the end-of-line marker to use (as a {@link String})
     * @throws IOException 
     */
    private static void convertLineEndings( File textFile, String eol )
        throws IOException
    {
        File temp = null;
        BufferedReader bufferIn = null;
        BufferedWriter bufferOut = null;

        try
        {
            if ( textFile.exists() )
            {
                // Create a new temp file to write to
                temp = new File( textFile.getAbsolutePath() + ".normalized" );
                temp.createNewFile();

                // Get a stream to read from the file un-normalized file
                FileInputStream fileIn = new FileInputStream( textFile );
                DataInputStream dataIn = new DataInputStream( fileIn );
                bufferIn = new BufferedReader( new InputStreamReader( dataIn ) );

                // Get a stream to write to the normalized file
                FileOutputStream fileOut = new FileOutputStream( temp );
                DataOutputStream dataOut = new DataOutputStream( fileOut );
                bufferOut = new BufferedWriter( new OutputStreamWriter( dataOut ) );

                // For each line in the un-normalized file
                String line;
                while ( ( line = bufferIn.readLine() ) != null )
                {
                    // Write the original line plus the operating-system dependent newline
                    bufferOut.write( line );
                    bufferOut.write( eol ); // write EOL marker
                }

                // Close buffered reader & writer:
                bufferIn.close();
                bufferOut.close();

                // Remove the original file
                textFile.delete();

                // And rename the original file to the new one
                temp.renameTo( textFile );
            }
            else
            {
                // If the file doesn't exist...
                throw new IOException( "Could not find file to open: " + textFile.getAbsolutePath() );
            }
        }
        finally
        {
            // Clean up, temp should never exist
            FileUtils.deleteQuietly( temp );
            IOUtils.closeQuietly( bufferIn );
            IOUtils.closeQuietly( bufferOut );
        }
    }

}
