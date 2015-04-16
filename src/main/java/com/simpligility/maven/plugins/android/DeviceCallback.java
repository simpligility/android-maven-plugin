/*
 * Copyright (C) 2011 Jayway AB
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
package com.simpligility.maven.plugins.android;

import com.android.ddmlib.IDevice;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Used for declare an action to perform on one or several {@link IDevice}s.
 *
 * @author hugo.josefson@jayway.com
 */
public interface DeviceCallback
{
    /**
     * What to do with an {@link IDevice}.
     *
     * @param device the device
     * @throws MojoExecutionException in case there is a problem, you may throw this
     * @throws org.apache.maven.plugin.MojoFailureException
     *                                in case there is a problem, you may throw this
     */
    void doWithDevice( IDevice device ) throws MojoExecutionException, MojoFailureException;
}
