package com.jayway.maven.plugins.android.configuration;

/**
 * Configuration for the manifest update. This class is only the definition of the parameters that are shadowed in
 * {@link com.jayway.maven.plugins.android.standalonemojos.ManifestUpdateMojo} and used there.
 *
 * @author Manfred Moser <manfred@simpligility.com>
 */
public class Manifest {
    /**
	 * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.ManifestUpdateMojo#manifestVersionName}.
	 */
	protected String versionName;

    /**
	 * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.ManifestUpdateMojo#manifestVersionCode}.
	 */
	protected Integer versionCode;

    /**
	 * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.ManifestUpdateMojo#manifestVersionCodeAutoIncrement}.
	 */
	 private boolean versionCodeAutoIncrement = false;

    /**
	 * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.ManifestUpdateMojo#manifestVersionCodeUpdateFromVersion}.
	 */
	protected Boolean versionCodeUpdateFromVersion = false;

    /**
	 * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.ManifestUpdateMojo#manifestSharedUserId}.
	 */
	protected String sharedUserId;

    /**
	 * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.ManifestUpdateMojo#manifestDebuggable}.
	 */
	protected Boolean debuggable;

    public String getVersionName() {
        return versionName;
    }

    public Integer getVersionCode() {
        return versionCode;
    }

    public boolean isVersionCodeAutoIncrement() {
        return versionCodeAutoIncrement;
    }

    public Boolean getVersionCodeUpdateFromVersion() {
        return versionCodeUpdateFromVersion;
    }

    public String getSharedUserId() {
        return sharedUserId;
    }

    public Boolean getDebuggable() {
        return debuggable;
    }
}
