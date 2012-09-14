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
package com.jayway.maven.plugins.android.configuration;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Configuration for an Android NDK. Only receives config parameter values, and there is no logic in here. Logic is in
 * {@link com.jayway.maven.plugins.android.AndroidNdk}.
 *
 * @author Johan Lindquist <johanlindquist@gmail.com>
 */
public class Ndk
{

    /**
     * Directory of the installed Android NDK, for example <code>/usr/local/android-ndk-r4</code>
     *
     * @see com.jayway.maven.plugins.android.phase05compile.NdkBuildMojo#ndkPath
     */
    private File path;

    /**
     *
     */
    private List<HeaderFilesDirective> headerFilesDirectives;
    /**
     *
     */
    private Boolean useHeaderArchives;
    /**
     *
     */
    private Boolean attachHeaderFiles;
    /**
     *
     */
    private Boolean useLocalSrcIncludePaths;

    /**
     *
     */
    private String buildExecutable;
    /**
     *
     */
    private String buildAdditionalCommandline;
    /**
     *
     */
    private String toolchain;

    /**
     *
     */
    private String classifier;

    /**
     *
     */
    private Boolean clearNativeArtifacts;
    /**
     *
     */
    private Boolean attachNativeArtifacts;

    /**
     *
     */
    // private File outputDirectory;
    /**
     *
     */
    // private File nativeLibrariesOutputDirectory;
    /**
     *
     */
    private String buildDirectory;

    /**
     *
     */
    private String finalLibraryName;

    /**
     *
     */
    private String makefile;

    /**
     *
     */
    private String applicationMakefile;

    /**
     *
     */
    private String target;
    /**
     *
     */
    private String architecture;

    /**
     *
     */
    private Boolean skipStripping = false;

    /**
     *
     */
    private Map<String, String> systemProperties;

    /**
     *
     */
    private Boolean ignoreBuildWarnings;
    /**
     *
     */
    private String buildWarningsRegularExpression;

}