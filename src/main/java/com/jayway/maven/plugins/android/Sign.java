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
 * Configuration for signing. Only receives config parameter values, and there is no logic in here. Logic is in
 * {@link AndroidSigner}.
 *
 * @author hugo.josefson@jayway.com
 */
public class Sign {

    /**
     * Whether to sign with the debug keystore. Valid values are:
     * <ul>
     * <li><code>true</code> = sign with the debug keystore.
     * <li><code>false</code> = don't sign with the debug keystore.
     * <li><code>auto</code> (default) = sign with debug keystore, unless another keystore is defined. (Signing with
     * other keystores is not yet implemented. See
     * <a href="http://code.google.com/p/maven-android-plugin/issues/detail?id=2">Issue 2</a>.)
     * </ul>
     *
     * @parameter expression="${android.sign.debug}" default-value="auto"
     */
    private String debug;

    public String getDebug() {
        return debug;
    }
}