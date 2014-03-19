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
package com.jayway.maven.plugins.android.common;

import com.android.SdkConstants;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;

import java.io.File;

import static com.jayway.maven.plugins.android.common.AndroidExtension.APK;

/**
 * Provides convenience methods for unpacking Android libraries so that their contents can be used in the build.
 */
public final class UnpackedLibHelper
{
    private final ArtifactResolverHelper artifactResolverHelper;
    private final Logger log;

    // ${project.build.directory}/unpacked-libs
    private final File unpackedLibsDirectory;

    public UnpackedLibHelper( ArtifactResolverHelper artifactResolverHelper, MavenProject project, Logger log )
    {
        this.artifactResolverHelper = artifactResolverHelper;
        final File targetFolder = new File( project.getBasedir(), "target" );
        this.unpackedLibsDirectory = new File( targetFolder, "unpacked-libs" );
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
     */
    public File getUnpackedLibNativesFolder( Artifact artifact )
    {
        return new File( getUnpackedLibFolder( artifact ), "libs" );
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
