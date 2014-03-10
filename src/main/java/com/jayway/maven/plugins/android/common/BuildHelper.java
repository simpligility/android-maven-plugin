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
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.jayway.maven.plugins.android.common.AndroidExtension.APK;

/**
 * Injects into life cycle
 */
public final class BuildHelper
{
    /**
     * Which dependency scopes should not be included when unpacking dependencies into the apk.
     */
    protected static final List<String> EXCLUDED_DEPENDENCY_SCOPES = Arrays.asList(
            Artifact.SCOPE_PROVIDED, Artifact.SCOPE_SYSTEM, Artifact.SCOPE_IMPORT
    );

    private final ArtifactResolver artifactResolver;
    private final Logger log;

    // ${project.build.directory}/unpacked-libs
    private final File unpackedLibsDirectory;


    public BuildHelper( ArtifactResolver artifactResolver, // @Component
                        MavenProject project,
                        Logger log
    )
    {
        this.artifactResolver = artifactResolver;
        final File targetFolder = new File( project.getBasedir(), "target" );
        this.unpackedLibsDirectory = new File( targetFolder, "unpacked-libs" );
        this.log = log;
    }

    public void extractApklib( Artifact apklibArtifact ) throws MojoExecutionException
    {
        final File apkLibFile = resolveArtifactToFile( apklibArtifact );
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
        final File aarFile = resolveArtifactToFile( aarArtifact );
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

    /**
     * Filters provided artifacts and selects only defined types based on {@code types} argument
     * or all types if {@code types} argument is empty
     *
     * @param allArtifacts artifacts to be filtered
     * @param types artifact types to be selected
     * @return a {@code List} of all project dependencies. Never {@code null}.
     *      This excludes artifacts of the {@code EXCLUDED_DEPENDENCY_SCOPES} scopes.
     *      And this should maintain dependency order to comply with library project resource precedence.
     */
    public Set<Artifact> getFilteredArtifacts( Iterable<Artifact> allArtifacts, String... types )
    {
        final List<String> acceptTypeList = Arrays.asList( types );
        boolean acceptAllArtifacts = acceptTypeList.isEmpty();
        final Set<Artifact> results = new LinkedHashSet<Artifact>();
        for ( Artifact artifact : allArtifacts )
        {
            if ( artifact == null )
            {
                continue;
            }

            if ( EXCLUDED_DEPENDENCY_SCOPES.contains( artifact.getScope() ) )
            {
                continue;
            }

            if ( acceptAllArtifacts || acceptTypeList.contains( artifact.getType() ) )
            {
                results.add( artifact );
            }
        }
        return results;
    }

    /**
     * Attempts to resolve an {@link org.apache.maven.artifact.Artifact} to a {@link java.io.File}.
     *
     * @param artifact to resolve
     * @return a {@link java.io.File} to the resolved artifact, never <code>null</code>.
     * @throws org.apache.maven.plugin.MojoExecutionException if the artifact could not be resolved.
     */
    public File resolveArtifactToFile( Artifact artifact ) throws MojoExecutionException
    {
        final Artifact resolvedArtifact = resolveArtifact( artifact );
        final File jar = resolvedArtifact.getFile();
        if ( jar == null )
        {
            throw new MojoExecutionException( "Could not resolve artifact " + artifact.getId()
                    + ". Please install it with \"mvn install:install-file ...\" or deploy it to a repository "
                    + "with \"mvn deploy:deploy-file ...\"" );
        }
        return jar;
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

    public Set<Artifact> resolveArtifacts( Collection<Artifact> artifacts ) throws MojoExecutionException
    {
        final Set<Artifact> resolvedArtifacts = new HashSet<Artifact>();
        for ( final Artifact artifact : artifacts )
        {
            resolvedArtifacts.add( resolveArtifact( artifact ) );
        }
        return resolvedArtifacts;
    }

    /**
     * Resolves an artifact to a particular repository.
     *
     * @param artifact  Artifact to resolve
     * @return fully resolved artifact.
     */
    private Artifact resolveArtifact( Artifact artifact ) throws MojoExecutionException
    {
        final ArtifactResolutionRequest resolutionRequest = new ArtifactResolutionRequest().setArtifact( artifact );
        final ArtifactResolutionResult resolutionResult = this.artifactResolver.resolve( resolutionRequest );

        log.debug( "Resolving : " + artifact );
        if ( resolutionResult.getArtifacts().size() == 0 )
        {
            throw new MojoExecutionException( "Could not resolve artifact " + artifact
                    + ". Please install it with \"mvn install:install-file ...\" or deploy it to a repository "
                    + "with \"mvn deploy:deploy-file ...\"" );
        }
        if ( resolutionResult.getArtifacts().size() > 1 )
        {
            log.debug( "Resolved artifacts : " + resolutionResult.getArtifacts() );
            throw new MojoExecutionException( "Could not resolve artifact " + artifact
                    + " to single target. Found the following possible options : " + resolutionResult.getArtifacts() );
        }

        final Artifact resolvedArtifact = resolutionResult.getArtifacts().iterator().next();
        log.debug( "Resolved :" + artifact );
        return resolvedArtifact;
    }
}
