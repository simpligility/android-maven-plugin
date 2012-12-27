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
 * Configuration for an Android NDK. Only receives config parameter values, and
 * there is no logic in here. Logic is in
 * {@link com.jayway.maven.plugins.android.AndroidNdk}.
 * 
 * @author Johan Lindquist <johanlindquist@gmail.com>
 */
public class Ndk
{

    /**
     * Directory of the installed Android NDK, for example
     * <code>/usr/local/android-ndk-r4</code>
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
    private Boolean maxJobs;

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

    public File getPath()
    {
        return path;
    }

    public void setPath( File path )
    {
        this.path = path;
    }

    public List<HeaderFilesDirective> getHeaderFilesDirectives()
    {
        return headerFilesDirectives;
    }

    public void setHeaderFilesDirectives(
            List<HeaderFilesDirective> headerFilesDirectives )
    {
        this.headerFilesDirectives = headerFilesDirectives;
    }

    public Boolean getUseHeaderArchives()
    {
        return useHeaderArchives;
    }

    public void setUseHeaderArchives( Boolean useHeaderArchives )
    {
        this.useHeaderArchives = useHeaderArchives;
    }

    public Boolean getAttachHeaderFiles()
    {
        return attachHeaderFiles;
    }

    public void setAttachHeaderFiles( Boolean attachHeaderFiles )
    {
        this.attachHeaderFiles = attachHeaderFiles;
    }

    public Boolean getUseLocalSrcIncludePaths()
    {
        return useLocalSrcIncludePaths;
    }

    public void setUseLocalSrcIncludePaths( Boolean useLocalSrcIncludePaths )
    {
        this.useLocalSrcIncludePaths = useLocalSrcIncludePaths;
    }

    public String getBuildExecutable()
    {
        return buildExecutable;
    }

    public void setBuildExecutable( String buildExecutable )
    {
        this.buildExecutable = buildExecutable;
    }

    public String getBuildAdditionalCommandline()
    {
        return buildAdditionalCommandline;
    }

    public void setBuildAdditionalCommandline( String buildAdditionalCommandline )
    {
        this.buildAdditionalCommandline = buildAdditionalCommandline;
    }

    public String getToolchain()
    {
        return toolchain;
    }

    public void setToolchain( String toolchain )
    {
        this.toolchain = toolchain;
    }

    public String getClassifier()
    {
        return classifier;
    }

    public void setClassifier( String classifier )
    {
        this.classifier = classifier;
    }

    public Boolean getClearNativeArtifacts()
    {
        return clearNativeArtifacts;
    }

    public void setClearNativeArtifacts( Boolean clearNativeArtifacts )
    {
        this.clearNativeArtifacts = clearNativeArtifacts;
    }

    public Boolean getAttachNativeArtifacts()
    {
        return attachNativeArtifacts;
    }

    public void setAttachNativeArtifacts( Boolean attachNativeArtifacts )
    {
        this.attachNativeArtifacts = attachNativeArtifacts;
    }

    public String getBuildDirectory()
    {
        return buildDirectory;
    }

    public void setBuildDirectory( String buildDirectory )
    {
        this.buildDirectory = buildDirectory;
    }

    public String getFinalLibraryName()
    {
        return finalLibraryName;
    }

    public void setFinalLibraryName( String finalLibraryName )
    {
        this.finalLibraryName = finalLibraryName;
    }

    public String getMakefile()
    {
        return makefile;
    }

    public void setMakefile( String makefile )
    {
        this.makefile = makefile;
    }

    public String getApplicationMakefile()
    {
        return applicationMakefile;
    }

    public void setApplicationMakefile( String applicationMakefile )
    {
        this.applicationMakefile = applicationMakefile;
    }

    public Boolean getMaxJobs()
    {
        return maxJobs;
    }

    public void setMaxJobs( Boolean maxJobs )
    {
        this.maxJobs = maxJobs;
    }

    public String getTarget()
    {
        return target;
    }

    public void setTarget( String target )
    {
        this.target = target;
    }

    public String getArchitecture()
    {
        return architecture;
    }

    public void setArchitecture( String architecture )
    {
        this.architecture = architecture;
    }

    public Boolean getSkipStripping()
    {
        return skipStripping;
    }

    public void setSkipStripping( Boolean skipStripping )
    {
        this.skipStripping = skipStripping;
    }

    public Map<String, String> getSystemProperties()
    {
        return systemProperties;
    }

    public void setSystemProperties( Map<String, String> systemProperties )
    {
        this.systemProperties = systemProperties;
    }

    public Boolean getIgnoreBuildWarnings()
    {
        return ignoreBuildWarnings;
    }

    public void setIgnoreBuildWarnings( Boolean ignoreBuildWarnings )
    {
        this.ignoreBuildWarnings = ignoreBuildWarnings;
    }

    public String getBuildWarningsRegularExpression()
    {
        return buildWarningsRegularExpression;
    }

    public void setBuildWarningsRegularExpression(
            String buildWarningsRegularExpression )
    {
        this.buildWarningsRegularExpression = buildWarningsRegularExpression;
    }

}