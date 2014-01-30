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
package com.jayway.maven.plugins.android.phase05compile;

import com.android.SdkConstants;
import com.jayway.maven.plugins.android.AbstractAndroidMojo;
import com.jayway.maven.plugins.android.CommandExecutor;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;

import java.io.File;

import static com.jayway.maven.plugins.android.common.AndroidExtension.AAR;
import static com.jayway.maven.plugins.android.common.AndroidExtension.APK;

/**
 * ConsumeAarMojo is responsible for consuming an Android Archive (aar) and 
 * its transitive dependencies into the current project. It only proceeds with 
 * this for Android Applications (apk), since it only makes sense for these to 
 * consume an aar. For projects with other packaging the execution is skipped.
 * 
 * @author Benoit Billington
 * @author Manfred Moser
 *
 * @goal consume-aar
 * @phase compile
 * @requiresProject true
 * @requiresDependencyResolution compile
 */
public class ConsumeAarMojo extends AbstractAndroidMojo
{

    public void execute() throws MojoExecutionException, MojoFailureException
    {
        // Skip execution if the current project is not packaging apk.
        // This helps work with multi-module projects.
        String projectPackaging = project.getPackaging();
        if ( ! APK.equals( projectPackaging ) )
        {
            getLog().info( "Project packaging (" + projectPackaging + ") is not APK - skipping AAR consumption" );
            return;
        }

        final CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger( this.getLog() );
        getLog().info( "Extracting classes for all aar dependencies" );

        for ( final Artifact artifact : getAllRelevantDependencyArtifacts() )
        {
            if ( artifact.getType().equals( AAR ) )
            {
                final String aarJar = getLibraryUnpackDirectory( artifact ) + "/" + SdkConstants.FN_CLASSES_JAR;

                final File aarFileJar = new File( aarJar );

                final UnArchiver unArchiver = new ZipUnArchiver( aarFileJar )
                {
                    @Override
                    protected Logger getLogger()
                    {
                        return new ConsoleLogger( Logger.LEVEL_DEBUG, "classes-unarchiver" );
                    }
                };
                final File classes = new File( project.getBuild().getOutputDirectory() );

                getLog().info( "Extract aar classes for " + artifact );

                classes.mkdir();

                unArchiver.setDestDirectory( classes );
                try
                {
                    unArchiver.extract();
                }
                catch ( ArchiverException e )
                {
                    throw new MojoExecutionException( "ArchiverException while extracting "
                            + aarFileJar.getAbsolutePath()
                            + ". Message: " + e.getLocalizedMessage(), e );
                }

                getLog().info( "Extracted " + aarJar );
            }
        }

    }
}
