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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.jayway.maven.plugins.android.common.AndroidExtension.AAR;
import static com.jayway.maven.plugins.android.common.AndroidExtension.APK;
import static com.jayway.maven.plugins.android.common.AndroidExtension.APKLIB;
import static com.jayway.maven.plugins.android.common.AndroidExtension.APKSOURCES;

/**
 *
 * @author Benoit Billington
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

        // If the current POM isn't an Android-related POM, then don't do
        // anything.  This helps work with multi-module projects.
        if ( ! isCurrentProjectAndroid() )
        {
            return;
        }

        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger( this.getLog() );

        for ( Artifact artifact : getAllRelevantDependencyArtifacts() )
        {
            if ( artifact.getType().equals( AAR ) )
            {
                String aarJar = getLibraryUnpackDirectory( artifact ) + "/" + SdkConstants.FN_CLASSES_JAR;

                File aarFileJar = new File( aarJar );

                final UnArchiver unArchiver = new ZipUnArchiver( aarFileJar )
                {
                    @Override
                    protected Logger getLogger()
                    {
                        return new ConsoleLogger( Logger.LEVEL_DEBUG, "classes-unarchiver" );
                    }
                };
                File classes = new File( project.getBuild().getOutputDirectory() );

                getLog().info( "output " + classes );

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

    /**
     * @return true if the pom type is APK, AAR, APKLIB, or APKSOURCES
     */
    private boolean isCurrentProjectAndroid()
    {
        Set<String> androidArtifacts = new HashSet<String>()
        {
            {
                addAll( Arrays.asList( APK, APKLIB, APKSOURCES, AAR ) );
            }
        };
        return androidArtifacts.contains( project.getArtifact().getType() );
    }

}
