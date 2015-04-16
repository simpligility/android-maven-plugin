/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package com.simpligility.maven.plugins.android.common;

import com.android.SdkConstants;
import com.google.common.io.PatternFilenameFilter;
import com.simpligility.maven.plugins.android.phase09package.AarMojo;
import com.simpligility.maven.plugins.android.phase09package.ApklibMojo;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;

import java.io.File;
import java.io.IOException;

import static com.simpligility.maven.plugins.android.common.AndroidExtension.AAR;
import static com.simpligility.maven.plugins.android.common.AndroidExtension.APK;

/**
 * Provides convenience methods for unpacking Android libraries so that their contents can be used in the build.
 */
public final class UnpackedLibHelper
{
    private final ArtifactResolverHelper artifactResolverHelper;
    private final Logger log;

    // ${project.build.directory}/unpacked-libs
    private final File unpackedLibsDirectory;

    public UnpackedLibHelper( ArtifactResolverHelper artifactResolverHelper, MavenProject project, Logger log,
                              File unpackedLibsFolder )
    {
        this.artifactResolverHelper = artifactResolverHelper;
        if ( unpackedLibsFolder != null )
        {
            // if absolute then use it.
            // if relative then make it relative to the basedir of the project.
            this.unpackedLibsDirectory = unpackedLibsFolder.isAbsolute()
                    ? unpackedLibsFolder
                    : new File( project.getBasedir(), unpackedLibsFolder.getPath() );
        }
        else
        {
            // If not specified then default to target/unpacked-libs
            final File targetFolder = new File( project.getBuild().getDirectory() );
            this.unpackedLibsDirectory = new File( targetFolder, "unpacked-libs" );
        }
        this.log = log;
    }

    public void extractApklib( Artifact apklibArtifact ) throws MojoExecutionException
    {
        final File apkLibFile = artifactResolverHelper.resolveArtifactToFile( apklibArtifact );
        if ( apkLibFile.isDirectory() )
        {
            log.warn(
                    "The apklib artifact points to '" + apkLibFile + "' which is a directory; skipping unpacking it." );
            return;
        }

        final UnArchiver unArchiver = new ZipUnArchiver( apkLibFile )
        {
            @Override
            protected Logger getLogger()
            {
                return new ConsoleLogger( log.getThreshold(), "dependencies-unarchiver" );
            }
        };

        final File apklibDirectory = getUnpackedLibFolder( apklibArtifact );
        apklibDirectory.mkdirs();
        unArchiver.setDestDirectory( apklibDirectory );
        log.debug( "Extracting APKLIB to " + apklibDirectory );
        try
        {
            unArchiver.extract();
        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException( "ArchiverException while extracting " + apklibDirectory
                    + ". Message: " + e.getLocalizedMessage(), e );
        }
    }

    public void extractAarLib( Artifact aarArtifact ) throws MojoExecutionException
    {
        final File aarFile = artifactResolverHelper.resolveArtifactToFile( aarArtifact );
        if ( aarFile.isDirectory() )
        {
            log.warn(
                    "The aar artifact points to '" + aarFile + "' which is a directory; skipping unpacking it." );
            return;
        }

        final UnArchiver unArchiver = new ZipUnArchiver( aarFile )
        {
            @Override
            protected Logger getLogger()
            {
                return new ConsoleLogger( log.getThreshold(), "dependencies-unarchiver" );
            }
        };

        final File aarDirectory = getUnpackedLibFolder( aarArtifact );
        aarDirectory.mkdirs();
        unArchiver.setDestDirectory( aarDirectory );
        log.debug( "Extracting AAR to " + aarDirectory );
        try
        {
            unArchiver.extract();
        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException( "ArchiverException while extracting " + aarDirectory.getAbsolutePath()
                    + ". Message: " + e.getLocalizedMessage(), e );
        }

        // Move native libraries from libs to jni folder for legacy AARs.
        // This ensures backward compatibility with older AARs where libs are in "libs" folder.
        final File jniFolder = new File( aarDirectory, AarMojo.NATIVE_LIBRARIES_FOLDER );
        final File libsFolder = new File( aarDirectory, ApklibMojo.NATIVE_LIBRARIES_FOLDER );
        if ( !jniFolder.exists() && libsFolder.isDirectory() && libsFolder.exists() )
        {
            String[] natives = libsFolder.list( new PatternFilenameFilter( "^.*(?<!(?i)\\.jar)$" ) );
            if ( natives.length > 0 )
            {
                log.debug( "Moving AAR native libraries from libs to jni folder" );
                for ( String nativeLibPath : natives )
                {
                    try
                    {
                        FileUtils.moveToDirectory( new File( libsFolder, nativeLibPath ), jniFolder, true );
                    }
                    catch ( IOException e )
                    {
                        throw new MojoExecutionException(
                                "Could not move native libraries from " + libsFolder, e );
                    }
                }
            }
        }
    }

    public File getArtifactToFile( Artifact artifact ) throws MojoExecutionException
    {
        final File artifactFile = artifactResolverHelper.resolveArtifactToFile( artifact );
        return artifactFile;
    }

    public File getUnpackedLibsFolder()
    {
        return unpackedLibsDirectory;
    }

    public File getUnpackedLibFolder( Artifact artifact )
    {
        return new File( unpackedLibsDirectory.getAbsolutePath(),
                getShortenedGroupId( artifact.getGroupId() )
                + "_"
                + artifact.getArtifactId()
                + "_"
                + artifact.getVersion()
        );
    }

    public File getUnpackedClassesJar( Artifact artifact )
    {
        return new File( getUnpackedLibFolder( artifact ), SdkConstants.FN_CLASSES_JAR );
    }

    public File getUnpackedApkLibSourceFolder( Artifact artifact )
    {
        return new File( getUnpackedLibFolder( artifact ), "src" );
    }

    public File getUnpackedLibResourceFolder( Artifact artifact )
    {
        return new File( getUnpackedLibFolder( artifact ), "res" );
    }

    public File getUnpackedLibAssetsFolder( Artifact artifact )
    {
        return new File( getUnpackedLibFolder( artifact ), "assets" );
    }

    /**
     * @param artifact  Android dependency that is being referenced.
     * @return Folder where the unpacked native libraries are located.
     * @see http://tools.android.com/tech-docs/new-build-system/aar-format
     */
    public File getUnpackedLibNativesFolder( Artifact artifact )
    {
        if ( AAR.equals( artifact.getType() ) )
        {
            return new File( getUnpackedLibFolder( artifact ), AarMojo.NATIVE_LIBRARIES_FOLDER );
        }
        else
        {
            return new File( getUnpackedLibFolder( artifact ), ApklibMojo.NATIVE_LIBRARIES_FOLDER );
        }
    }

    public File getJarFileForApk( Artifact artifact )
    {
        final String fileName = artifact.getFile().getName();
        final String modifiedFileName = fileName.substring( 0, fileName.lastIndexOf( "." ) ) + ".jar";
        return new File( artifact.getFile().getParentFile(), modifiedFileName );
    }

    /**
     * @param groupId   An a dot separated groupId (eg org.apache.maven)
     * @return A shortened (and potentially non-unique) version of the groupId, that consists of the first letter
     *      of each part of the groupId. Eg oam for org.apache.maven
     */
    private String getShortenedGroupId( String groupId )
    {
        final String[] parts = groupId.split( "\\." );
        final StringBuilder sb = new StringBuilder();
        for ( final String part : parts )
        {
            sb.append( part.charAt( 0 ) );
        }
        return sb.toString();
    }

    /**
     * @return True if this project constructs an APK as opposed to an AAR or APKLIB.
     */
    public boolean isAPKBuild( MavenProject project )
    {
        return APK.equals( project.getPackaging() );
    }
}
