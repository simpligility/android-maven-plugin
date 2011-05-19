/*
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

import java.io.File;

/**
 * Configuration for an Android NDK. Only receives config parameter values, and there is no logic in here. Logic is in
 * {@link com.jayway.maven.plugins.android.AndroidNdk}.
 *
 * @author Johan Lindquist <johanlindquist@gmail.com>
 */
public class Ndk {
    /**
     * Directory of the installed Android NDK, for example <code>/usr/local/android-ndk-r4</code>
     *
     * @parameter expression="${android.ndk.path}"
     * @required
     */
    private File path;

    public File getPath() {
        return path;
    }

}