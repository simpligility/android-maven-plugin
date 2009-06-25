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
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Undeploys an application from a connected device, based on <code>package</code>.<br/>
 * @goal undeploy-package
 * @requiresProject false
 * @author hugo.josefson@jayway.com
 */
public class UndeployPackageMojo extends AbstractAndroidMojo {

    /**
     * The package to undeploy from a connected emulator or usb device. If not defined, will use package from
     * <code>AndroidManifest.xml</code>.
     * @parameter property="package" expression="${android.package}"
     */
    private String packageName;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (packageName == null){
            extractPackageNameFromAndroidManifest(androidManifestFile);
        }
        undeployApk(packageName);
    }

}
