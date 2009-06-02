/*
 * Copyright (C) 2009 Jayway AB
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
package com.jayway.maven.plugins.android;

/**
 * For integrationtest related Mojos.
 * 
 * @author hugo.josefson@jayway.com
 */
public abstract class AbstractIntegrationtestMojo extends AbstractAndroidMojo {
    /**
     * -Dmaven.test.skip is commonly used with Maven to skip tests. We honor it too.
     *
     * @parameter expression="${maven.test.skip}" default-value=false
     * @readonly
     */
    private boolean mavenTestSkip;

    /**
     * Enables integration test related goals. If <code>false</code>, they will be skipped.
     * @parameter expression="${android.enableIntegrationTest}" default-value=true
     */
    private boolean enableIntegrationTest;

    /**
     * Whether or not to execute integration test related goals. Reads from configuration parameter
     * <code>enableIntegrationTest</code>, but can be overridden with <code>-Dmaven.test.skip</code>.
     * @return <code>true</code> if integration test goals should be executed, <code>false</code> otherwise.
     */
    protected boolean isEnableIntegrationTest() {
        if (mavenTestSkip){
            return false;
        }

        return enableIntegrationTest;
    }
}
