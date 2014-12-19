package com.jayway.maven.plugins.android.configuration;

/**
 * Configuration for the manifest update. This class is only the definition of the parameters that are shadowed in
 * {@link com.jayway.maven.plugins.android.standalonemojos.ManifestMergerMojo} and used there.
 *
 * @author Benoit Billington
 */
public class ManifestMerger
{

    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.ManifestMergerMojo#manifestVersionName}.
     */
    protected String versionName;

    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.ManifestMergerMojo#manifestVersionCode}.
     */
    protected Integer versionCode;

    /**
     * Mirror of
     * {@link com.jayway.maven.plugins.android.standalonemojos.ManifestMergerMojo#manifestUsesSdk}
     */
    protected UsesSdk usesSdk;

    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.ManifestMergerMojo
     * #manifestVersionCodeUpdateFromVersion}.
     */
    protected Boolean versionCodeUpdateFromVersion;

    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.ManifestMergerMojo
     * #manifestMergeLibraries}.
     */
    protected Boolean mergeLibraries;

    public String getVersionName()
    {
        return versionName;
    }

    public Integer getVersionCode()
    {
        return versionCode;
    }

    public UsesSdk getUsesSdk()
    {
        return usesSdk;
    }

    public Boolean getVersionCodeUpdateFromVersion()
    {
        return versionCodeUpdateFromVersion;
    }

    public Boolean getMergeLibraries()
    {
        return mergeLibraries;
    }
}
