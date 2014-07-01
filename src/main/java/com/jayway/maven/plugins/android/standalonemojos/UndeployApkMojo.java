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
import com.jayway.maven.plugins.android.config.ConfigHandler;
import com.jayway.maven.plugins.android.config.ConfigPojo;
import com.jayway.maven.plugins.android.config.PullParameter;
import com.jayway.maven.plugins.android.configuration.DeployApk;
import com.jayway.maven.plugins.android.configuration.ValidationResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

/**
 * Undeploys a specified Android application apk from attached devices and emulators. 
 * By default it will undeploy from all, but a subset or single one can be configured 
 * with the device and devices parameters. You can supply the package of the 
 * application and/or an apk file. This goal can be used in non-android projects and as 
 * standalone execution on the command line.<br/>
 *
 * @author Manfred Moser <manfred@simpligility.com>
 */
@Mojo( name = "undeploy-apk", requiresProject = false )
public class UndeployApkMojo extends AbstractAndroidMojo
{
    /**
     * Configuration for apk file undeployment within a pom file. See {@link #deployapkFilename}. 
     * <p/>
     * <pre>
     * &lt;deployapk&gt;
     *    &lt;filename&gt;yourapk.apk&lt;/filename&gt;
     *    &lt;packagename&gt;com.yourcompany.app&lt;/packagename&gt;
     * &lt;/deployapk&gt;
     * </pre>
     * 
     * @parameter
     */
    @ConfigPojo
    protected DeployApk deployapk;

    @Parameter( property = "android.deployapk.filename" )
    private File deployapkFilename;

    @PullParameter
    private File parsedFilename;

    @Parameter( property = "android.deployapk.packagename" )
    private String deployapkPackagename;

    @PullParameter
    private String parsedPackagename;

    /**
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        ConfigHandler configHandler = new ConfigHandler( this, this.session, this.execution );
        configHandler.parseConfiguration();

        if ( parsedFilename == null && parsedPackagename == null )
        {
            throw new MojoFailureException( "\n\n One of the parameters android.deployapk.packagename "
                    + "or android.deployapk.filename is required. \n" );
        }
        
        if ( StringUtils.isNotBlank( parsedPackagename ) )
        {
            getLog().debug( "Undeploying with packagename " + parsedPackagename );
            undeployApk( parsedPackagename );
        }

        ValidationResponse response = DeployApk.validFileParameter( parsedFilename );
        if ( response.isValid() ) 
        {
            getLog().debug( "Undeploying with file " + parsedFilename );
            undeployApk( parsedFilename );
        } 
        else 
        {
            getLog().info( "Ignoring invalid file parameter." );
            getLog().debug( response.getMessage() );
            
        }
    }
}
