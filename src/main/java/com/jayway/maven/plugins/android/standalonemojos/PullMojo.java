/*
 * Copyright (C) 2007-2008 JVending Masa
 * Copyright (C) 2011 Jayway AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jayway.maven.plugins.android.standalonemojos;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.FileListingService;
import com.android.ddmlib.FileListingService.FileEntry;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.SyncException;
import com.android.ddmlib.SyncService;
import com.android.ddmlib.TimeoutException;
import com.jayway.maven.plugins.android.AbstractAndroidMojo;
import com.jayway.maven.plugins.android.DeviceCallback;
import com.jayway.maven.plugins.android.common.DeviceHelper;
import com.jayway.maven.plugins.android.common.LogSyncProgressMonitor;
import com.jayway.maven.plugins.android.config.ConfigHandler;
import com.jayway.maven.plugins.android.config.ConfigPojo;
import com.jayway.maven.plugins.android.config.PullParameter;
import com.jayway.maven.plugins.android.configuration.Pull;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.io.IOException;

/**
 * Copy file or directory from all the attached (or specified)
 * devices/emulators.
 *
 * @author Manfred Moser <manfred@simpligility.com>
 * @goal pull
 * @requiresProject false
 */
public class PullMojo extends AbstractAndroidMojo
{

    /**
     * <p>The configuration for the pull goal can be set up in the plugin configuration in the pom file as:</p>
     * <pre>
     * &lt;pull&gt;
     *     &lt;source&gt;path&lt;/source&gt;
     *     &lt;destination&gt;path&lt;/destination&gt;
     * &lt;/pull&gt;
     * </pre>
     * <p>The parameters can also be configured as property in the pom or settings file
     * <pre>
     * &lt;properties&gt;
     *     &lt;android.pull.source&gt;pathondevice&lt;/android.pull.source&gt;
     *     &lt;android.pull.destination&gt;path&lt;/android.pull.destination&gt;
     * &lt;/properties&gt;
     * </pre>
     * or from command-line with parameter <code>-Dandroid.pull.source=path</code>
     * and <code>-Dandroid.pull.destination=path</code>.</p>
     *
     * @parameter
     */
    @ConfigPojo
    private Pull pull;

    /**
     * The path of the source file or directory on the emulator/device.
     *
     * @parameter property="android.pull.source"
     */
    private String pullSource;

    @PullParameter( required = true )
    private String parsedSource;

    /**
     * The path of the destination to copy the file to.
     * <p/>
     * If destination ends with {@link File#separator}, it is supposed to be a
     * directory. Therefore the source - whether it refers to a file or
     * directory - will be copied into the destination directory.
     * <p/>
     * If destination does not end with {@link File#separator}, the last path
     * segment will be assumed as the new file or directory name (depending on
     * the type of source).
     * <p/>
     * Any missing directories will be created.
     *
     * @parameter property="android.pull.destination"
     */
    private String pullDestination;

    @PullParameter( required = true )
    private String parsedDestination;

    public void execute() throws MojoExecutionException, MojoFailureException
    {

        ConfigHandler configHandler = new ConfigHandler( this, this.session, this.execution );
        configHandler.parseConfiguration();

        doWithDevices( new DeviceCallback()
        {
            public void doWithDevice( final IDevice device ) throws MojoExecutionException
            {
                String deviceLogLinePrefix = DeviceHelper.getDeviceLogLinePrefix( device );

                // message will be set later according to the processed files
                String message = "";
                try
                {
                    SyncService syncService = device.getSyncService();
                    FileListingService fileListingService = device.getFileListingService();

                    FileEntry sourceFileEntry = getFileEntry( parsedSource, fileListingService );

                    if ( sourceFileEntry.isDirectory() )
                    {
                        // pulling directory
                        File destinationDir = new File( parsedDestination );
                        if ( ! destinationDir.exists() )
                        {
                            getLog().info( "Creating destination directory " + destinationDir );
                            destinationDir.mkdirs();
                            destinationDir.mkdir();
                        }
                        String destinationDirPath = destinationDir.getAbsolutePath();

                        FileEntry[] fileEntries;
                        if ( parsedDestination.endsWith( File.separator ) )
                        {
                            // pull source directory directly
                            fileEntries = new FileEntry[]{ sourceFileEntry };
                        }
                        else
                        {
                            // pull the children of source directory only
                            fileEntries = fileListingService.getChildren( sourceFileEntry, true, null );
                        }

                        message = deviceLogLinePrefix + "Pull of " + parsedSource + " to " + destinationDirPath 
                                + " from ";

                        syncService.pull( fileEntries, destinationDirPath, new LogSyncProgressMonitor( getLog() ) );
                    }
                    else
                    {
                        // pulling file
                        File parentDir = new File( FilenameUtils.getFullPath( parsedDestination ) );
                        if ( ! parentDir.exists() )
                        {
                            getLog().info( deviceLogLinePrefix + "Creating destination directory " + parentDir );
                            parentDir.mkdirs();
                        }

                        String destinationFileName;
                        if ( parsedDestination.endsWith( File.separator ) )
                        {
                            // keep original filename
                            destinationFileName = FilenameUtils.getName( parsedSource );
                        }
                        else
                        {
                            // rename filename
                            destinationFileName = FilenameUtils.getName( parsedDestination );
                        }

                        File destinationFile = new File( parentDir, destinationFileName );
                        String destinationFilePath = destinationFile.getAbsolutePath();
                        message = deviceLogLinePrefix + "Pull of " + parsedSource + " to " + destinationFilePath 
                                + " from " + DeviceHelper.getDescriptiveName( device );

                        syncService.pullFile( sourceFileEntry, destinationFilePath,
                                new LogSyncProgressMonitor( getLog() ) );
                    }

                    getLog().info( message + " successful." );
                }
                catch ( SyncException e )
                {
                    throw new MojoExecutionException( message + " failed.", e );
                }
                catch ( IOException e )
                {
                    throw new MojoExecutionException( message + " failed.", e );
                }
                catch ( TimeoutException e )
                {
                    throw new MojoExecutionException( message + " failed.", e );
                }
                catch ( AdbCommandRejectedException e )
                {
                    throw new MojoExecutionException( message + " failed.", e );
                }
            }
        } );
    }

    /**
     * Retrieves the corresponding {@link FileEntry} on the emulator/device for
     * a given file path.
     * <p/>
     * If the file path starts with the symlink "sdcard", it will be resolved
     * statically to "/mnt/sdcard".
     *
     * @param filePath           path to file or directory on device or emulator
     * @param fileListingService {@link FileListingService} for retrieving the
     *                           {@link FileEntry}
     * @return a {@link FileEntry} object for the given file path
     * @throws MojoExecutionException if the file path could not be found on the device
     */
    private FileEntry getFileEntry( String filePath, FileListingService fileListingService )
            throws MojoExecutionException
    {
        // static resolution of symlink
        if ( filePath.startsWith( "/sdcard" ) )
        {
            filePath = "/mnt" + filePath;
        }

        String[] destinationPathSegments = StringUtils.split( filePath, "/" );

        FileEntry fileEntry = fileListingService.getRoot();
        for ( String destinationPathSegment : destinationPathSegments )
        {
            // build up file listing cache
            fileListingService.getChildren( fileEntry, true, null );

            fileEntry = fileEntry.findChild( destinationPathSegment );
            if ( fileEntry == null )
            {
                throw new MojoExecutionException( "Cannot execute goal: " + filePath + " does not exist on device." );
            }
        }
        return fileEntry;
    }
}
