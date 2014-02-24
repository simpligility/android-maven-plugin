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
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
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
    protected static final List<String> EXCLUDED_DEPENDENCY_SCOPES = Arrays.asList( "provided", "system", "import" );

    private final RepositorySystem repoSystem;
    private final RepositorySystemSession repoSession;
    private final List<RemoteRepository> projectRepos;
    private final Logger log;

    // ${project.build.directory}/unpacked-libs
    private final File unpackedLibsDirectory;

    public BuildHelper( RepositorySystem repoSystem,
                        RepositorySystemSession repoSession,
                        MavenProject project,
                        Logger log
    )
    {
        this.repoSystem = repoSystem;
        this.repoSession = repoSession;
        this.projectRepos = project.getRemoteProjectRepositories();
        final File targetFolder = new File( project.getBasedir(), "target" );
        this.unpackedLibsDirectory = new File( targetFolder, "unpacked-libs" );
        this.log = log;
    }

    public void extractApklib( Artifact apklibArtifact ) throws MojoExecutionException
    {

        final Artifact resolvedArtifact = AetherHelper
                .resolveArtifact( apklibArtifact, repoSystem, repoSession, projectRepos );

        File apkLibFile = resolvedArtifact.getFile();

        // When the artifact is not installed in local repository, but rather part of the current reactor,
        // resolve from within the reactor. (i.e. ../someothermodule/target/*)
        if ( ! apkLibFile.exists() )
        {
            apkLibFile = resolveArtifactToFile( apklibArtifact );
        }

        //When using maven under eclipse the artifact will by default point to a directory, which isn't correct.
        //To work around this we'll first try to get the archive from the local repo, and only if it isn't found there
        // we'll do a normal resolve.
        if ( apkLibFile.isDirectory() )
        {
            apkLibFile = resolveArtifactToFile( apklibArtifact );
        }

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

        final Artifact resolvedArtifact = AetherHelper
                .resolveArtifact( aarArtifact, repoSystem, repoSession, projectRepos );

        File aarFile = resolvedArtifact.getFile();

        // When the artifact is not installed in local repository, but rather part of the current reactor,
        // resolve from within the reactor. (i.e. ../someothermodule/target/*)
        if ( ! aarFile.exists() )
        {
            aarFile = resolveArtifactToFile( aarArtifact );
        }

        //When using maven under eclipse the artifact will by default point to a directory, which isn't correct.
        //To work around this we'll first try to get the archive from the local repo, and only if it isn't found there
        // we'll do a normal resolve.
        if ( aarFile.isDirectory() )
        {
            aarFile = resolveArtifactToFile( aarArtifact );
        }

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

            if ( AndroidExtension.APK.equalsIgnoreCase( artifact.getType() ) )
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
        Artifact resolvedArtifact = AetherHelper.resolveArtifact( artifact, repoSystem, repoSession, projectRepos );
        final File jar = resolvedArtifact.getFile();
        if ( jar == null )
        {
            throw new MojoExecutionException( "Could not resolve artifact " + artifact.getId()
                    + ". Please install it with \"mvn install:install-file ...\" or deploy it to a repository "
                    + "with \"mvn deploy:deploy-file ...\"" );
        }
        return jar;
    }

    public File getUnpackedLibFolder( Artifact artifact )
    {
        return new File( unpackedLibsDirectory.getAbsolutePath(),
                artifact.getGroupId() + "_" + artifact.getArtifactId() );
    }

    public File getUnpackedAarClassesJar( Artifact artifact )
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

    /**
     * Copies the files contained within the source folder to the target folder.
     * <p>
     * The the target folder doesn't exist it will be created.
     * </p>
     *
     * @param sourceFolder      Folder from which to copy the resources.
     * @param targetFolder      Folder to which to copy the files.
     * @throws org.apache.maven.plugin.MojoExecutionException if the files cannot be copied.
     */
    protected void copyFolder( File sourceFolder, File targetFolder ) throws MojoExecutionException
    {
        copyFolder( sourceFolder, targetFolder, TrueFileFilter.TRUE );
    }

    private void copyFolder( File sourceFolder, File targetFolder, FileFilter filter ) throws MojoExecutionException
    {
        if ( !sourceFolder.exists() )
        {
            return;
        }

        try
        {
            log.debug( "Copying " + sourceFolder + " to " + targetFolder );
            if ( ! targetFolder.exists() )
            {
                if ( ! targetFolder.mkdirs() )
                {
                    throw new MojoExecutionException( "Could not create target directory " + targetFolder );
                }
            }
            FileUtils.copyDirectory( sourceFolder, targetFolder, filter );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Could not copy source folder to target folder", e );
        }
    }

    /**
     * @return True if this project constructs an APK as opposed to an AAR or APKLIB.
     */
    public boolean isAPKBuild( MavenProject project )
    {
        return APK.equals( project.getPackaging() );
    }
}
