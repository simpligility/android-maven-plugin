package com.simpligility.maven.plugins.android.common;

import org.codehaus.plexus.util.DirectoryScanner;

import java.io.File;

/**
 * Retrieves the names of layout resource files.
 */
public final class FileRetriever
{
    private final String[] includes;

    /**
     * @param includes      Ant-style include statements, for example <code>"** /*.aidl"</code> (but without the space
     *                      in the middle).
     */
    public FileRetriever( String... includes )
    {
        this.includes = includes;
    }

    /**
     * Finds the files in the supplied folder that match the configured includes.
     *
     * @param baseDirectory Directory to find files in.
     * @return <code>String[]</code> of the files' paths and names, relative to <code>baseDirectory</code>. Empty
     *         <code>String[]</code> if <code>baseDirectory</code> does not exist.
     */
    public String[] getFileNames( File baseDirectory )
    {
        if ( !baseDirectory.exists() )
        {
            return new String[ 0 ];
        }

        final DirectoryScanner directoryScanner = new DirectoryScanner();
        directoryScanner.setBasedir( baseDirectory );
        directoryScanner.setIncludes( includes );
        directoryScanner.addDefaultExcludes();
        directoryScanner.scan();

        return directoryScanner.getIncludedFiles();
    }
}
