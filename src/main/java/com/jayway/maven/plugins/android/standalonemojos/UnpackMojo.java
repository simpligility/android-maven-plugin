/*
 * Copyright (C) 2009 Jayway AB
 * Copyright (C) 2007-2008 JVending Masa
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

import com.jayway.maven.plugins.android.AbstractAndroidMojo;
import com.jayway.maven.plugins.android.CommandExecutor;
import com.jayway.maven.plugins.android.common.JarHelper;
import com.jayway.maven.plugins.android.config.ConfigPojo;
import com.jayway.maven.plugins.android.config.PullParameter;
import com.jayway.maven.plugins.android.configuration.MetaInf;
import com.jayway.maven.plugins.android.configuration.Unpack;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Unpack libraries code and dependencies into target.
 * <p/>
 * This can be useful for using the proguard maven plugin to provide the input jars. Although it is encouraged to use
 * the proguard mojo of the android maven plugin.
 *
 * @author hugo.josefson@jayway.com
 * @author Manfred Moser
 * @goal unpack
 * @requiresDependencyResolution compile
 */
public class UnpackMojo extends AbstractAndroidMojo
{
    /**
     * If true, the library will be unpacked only when outputDirectory doesn't
     * exist, i.e, a clean build for most cases.
     *
     * @parameter property="android.lazyLibraryUnpack" default-value="false"
     * @deprecated use ${android.unpack.lazy}
     */
    @Deprecated
    private boolean lazyLibraryUnpack;

    @PullParameter( defaultValueGetterMethod = "getDefaultMetaInf" )
    private MetaInf unpackMetaInf;

    /**
     * @parameter property="android.unpack.lazy"
     */
    @PullParameter( defaultValueGetterMethod = "getLazyLibraryUnpack" )
    private Boolean unpackLazy;

    /**
     * @parameter alias="metaInf"
     */
    private MetaInf pluginMetaInf;

    /**
     * @parameter
     */
    @ConfigPojo( prefix = "unpack" )
    private Unpack unpack;

    public void execute() throws MojoExecutionException, MojoFailureException
    {

        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger( this.getLog() );

        if ( generateApk )
        {
            // Unpack all dependent and main classes
            unpackClasses();
        }
    }

    private File unpackClasses() throws MojoExecutionException
    {
        File outputDirectory = new File( project.getBuild().getDirectory(), "android-classes" );
        if ( lazyLibraryUnpack && outputDirectory.exists() )
        {
            getLog().info( "skip library unpacking due to lazyLibraryUnpack policy" );
        }
        else
        {
            outputDirectory.mkdirs();

            for ( Artifact artifact : getRelevantCompileArtifacts() )
            {

                if ( artifact.getFile().isDirectory() )
                {
                    try
                    {
                        FileUtils.copyDirectory( artifact.getFile(), outputDirectory );
                    }
                    catch ( IOException e )
                    {
                        throw new MojoExecutionException( "IOException while copying "
                                + artifact.getFile().getAbsolutePath() + " into " + outputDirectory.getAbsolutePath()
                                , e );
                    }
                }
                else
                {
                    try
                    {
                        JarHelper.unjar( new JarFile( artifact.getFile() ), outputDirectory,
                                new JarHelper.UnjarListener()
                                {
                                    @Override
                                    public boolean include( JarEntry jarEntry )
                                    {
                                         return isIncluded( jarEntry );
                                    }
                                } );
                    }
                    catch ( IOException e )
                    {
                        throw new MojoExecutionException( "IOException while unjarring "
                                + artifact.getFile().getAbsolutePath() + " into " + outputDirectory.getAbsolutePath()
                                , e );
                    }
                }

            }
        }

        try
        {
            File sourceDirectory = new File( project.getBuild().getOutputDirectory() );
            FileUtils.copyDirectory( sourceDirectory, outputDirectory );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "IOException while copying " + sourceDirectory.getAbsolutePath()
                    + " into " + outputDirectory.getAbsolutePath(), e );
        }
        return outputDirectory;
    }

    boolean isIncluded( JarEntry jarEntry )
    {
        String entName = jarEntry.getName();

        if ( entName.endsWith( ".class" ) )
        {
            return true;
        }

        return this.unpackMetaInf != null && this.unpackMetaInf.isIncluded( entName );
    }
    
    MetaInf getDefaultMetaInf()
    {
        return this.pluginMetaInf;
    }

    boolean getLazyLibraryUnpack()
    {
        return this.lazyLibraryUnpack;
    }
}
