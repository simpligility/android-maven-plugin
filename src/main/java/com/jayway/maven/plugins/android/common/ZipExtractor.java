package com.jayway.maven.plugins.android.common;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.components.io.fileselectors.FileInfo;
import org.codehaus.plexus.components.io.fileselectors.FileSelector;
import org.codehaus.plexus.logging.Logger;

import java.io.File;
import java.io.IOException;

/**
 * Extract an archive to a given location.
 */
public final class ZipExtractor
{
    private final Log log;

    public ZipExtractor( Log log )
    {
        this.log = log;
    }

    public void extract( File zipFile, File targetFolder, final String suffixToExclude ) throws MojoExecutionException
    {
        final UnArchiver unArchiver = new ZipUnArchiver( zipFile )
        {
            @Override
            protected Logger getLogger()
            {
                return new MavenToPlexusLogAdapter( log );
            }
        };

        targetFolder.mkdirs();

        final FileSelector exclusionFilter = new FileSelector()
        {
            @Override
            public boolean isSelected( FileInfo fileInfo ) throws IOException
            {
                return !fileInfo.getName().endsWith( suffixToExclude );
            }
        };

        unArchiver.setDestDirectory( targetFolder );
        unArchiver.setFileSelectors( new FileSelector[] { exclusionFilter } );

        log.debug( "Extracting archive to " + targetFolder );
        try
        {
            unArchiver.extract();
        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException( "ArchiverException while extracting " + zipFile.getAbsolutePath()
                    + ". Message: " + e.getLocalizedMessage(), e );
        }
    }
}
