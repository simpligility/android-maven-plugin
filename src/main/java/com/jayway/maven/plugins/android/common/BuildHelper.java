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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
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
    private final File combinedAssets;
    private final MavenProjectHelper projectHelper;
    private final File unpackedLibsDirectory;
    private final File unpackedLibClassesDirectory;
    private final Logger log;

    public BuildHelper( RepositorySystem repoSystem, RepositorySystemSession repoSession,
                        List<RemoteRepository> projectRepos, File combinedAssets, MavenProjectHelper projectHelper,
                        File unpackedLibsDirectory, File unpackedLibClassesDirectory,
                        Logger log )
    {
        this.repoSystem = repoSystem;
        this.repoSession = repoSession;
        this.projectRepos = projectRepos;
        this.combinedAssets = combinedAssets;
        this.projectHelper = projectHelper;
        this.unpackedLibsDirectory = unpackedLibsDirectory;
        this.unpackedLibClassesDirectory = unpackedLibClassesDirectory;
        this.log = log;
    }

    public void extractLibraryDependencies( MavenProject project ) throws MojoExecutionException
    {
        for ( Artifact artifact : getAllRelevantDependencyArtifacts( project ) )
        {
            String type = artifact.getType();
            if ( type.equals( AndroidExtension.APKLIB ) )
            {
                log.info( "Extracting apklib " + artifact.getArtifactId() + "..." );
                extractApklib( project, artifact );
            }
            else if ( type.equals( AndroidExtension.AAR ) )
            {
                log.info( "Extracting aar " + artifact.getArtifactId() + "..." );
                extractAarLib( project, artifact );
            }
            else
            {
                log.info( "Not extracting " + artifact.getArtifactId() + "..." );
            }
        }
    }

    private void extractApklib( MavenProject project, Artifact apklibArtifact ) throws MojoExecutionException
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
                return new ConsoleLogger( Logger.LEVEL_DEBUG, "dependencies-unarchiver" );
            }
        };

        final File apklibDirectory = getUnpackedLibFolder( apklibArtifact );
        apklibDirectory.mkdirs();
        unArchiver.setDestDirectory( apklibDirectory );
        try
        {
            unArchiver.extract();
        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException( "ArchiverException while extracting " + apklibDirectory
                    + ". Message: " + e.getLocalizedMessage(), e );
        }



        // Copy the assets to the the combinedAssets folder.
        // Add the apklib source and resource to the compile.
        // NB apklib sources are added to compileSourceRoot because we may need to compile against them.
        //    This means the apklib classes will be compiled into target/classes and packaged with this build.
        copyFolder( getUnpackedLibAssetsFolder( apklibArtifact ), combinedAssets );

        final File apklibSourceFolder = getUnpackedLibSourceFolder( apklibArtifact );
        final List<String> resourceExclusions = Arrays.asList( "**/*.java", "**/*.aidl" );
        projectHelper.addResource( project, apklibSourceFolder.getAbsolutePath(), null, resourceExclusions );
        project.addCompileSourceRoot( apklibSourceFolder.getAbsolutePath() );
    }

    private void extractAarLib( MavenProject project, Artifact aarArtifact ) throws MojoExecutionException
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
                return new ConsoleLogger( Logger.LEVEL_DEBUG, "dependencies-unarchiver" );
            }
        };
        final File aarDirectory = getUnpackedLibFolder( aarArtifact );
        aarDirectory.mkdirs();
        unArchiver.setDestDirectory( aarDirectory );
        try
        {
            unArchiver.extract();
        }
        catch ( ArchiverException e )
        {
            throw new MojoExecutionException( "ArchiverException while extracting " + aarDirectory.getAbsolutePath()
                    + ". Message: " + e.getLocalizedMessage(), e );
        }

        // Copy the assets to the the combinedAssets folder, but only if an APK build.
        // Ie we only want to package assets that we own.
        // Assets should only live within their owners or the final APK.
        if ( isAPKBuild( project ) )
        {
            copyFolder( getUnpackedLibAssetsFolder( aarArtifact ), combinedAssets );
        }

        // Aar lib resources should only be included if we are building an apk.
        // Aar lib will not contain any classes in the src folder as classes will already be compiled.
        // We will added the classes later when we extract the lib classes.jar
        if ( isAPKBuild( project ) )
        {
            final File libSourceFolder = getUnpackedLibSourceFolder( aarArtifact );
            projectHelper.addResource( project, libSourceFolder.getAbsolutePath(), null, Arrays.asList( "**/*.aidl" ) );
        }
    }

    // @return a {@code List} of all project dependencies. Never {@code null}. This excludes artifacts of the {@code
    //     EXCLUDED_DEPENDENCY_SCOPES} scopes. And this should maintain dependency order to comply with library
    //     project resource precedence.
    public Set<Artifact> getAllRelevantDependencyArtifacts( MavenProject project )
    {
        final Set<Artifact> allArtifacts = project.getArtifacts();
        log.info( "projectArtifacts=" + allArtifacts );
        return filterOutIrrelevantArtifacts( allArtifacts );
    }

    private Set<Artifact> filterOutIrrelevantArtifacts( Iterable<Artifact> allArtifacts )
    {
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

            results.add( artifact );
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
        return new File( unpackedLibsDirectory.getAbsolutePath(), artifact.getArtifactId() );
    }
    public File getUnpackedLibSourceFolder( Artifact artifact )
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

    public File getUnpackedLibClassesFolder( Artifact artifact )
    {
        return new File( unpackedLibClassesDirectory.getAbsolutePath(), artifact.getArtifactId() );
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
