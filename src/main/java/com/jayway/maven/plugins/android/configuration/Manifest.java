package com.jayway.maven.plugins.android.configuration;

import java.util.List;

import com.jayway.maven.plugins.android.standalonemojos.CompatibleScreen;
import com.jayway.maven.plugins.android.standalonemojos.SupportsScreens;

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
	 private Boolean versionCodeAutoIncrement;

    /**
	 * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.ManifestUpdateMojo#manifestVersionCodeUpdateFromVersion}.
	 */
	protected Boolean versionCodeUpdateFromVersion;

    /**
	 * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.ManifestUpdateMojo#manifestSharedUserId}.
	 */
	protected String sharedUserId;

    /**
	 * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.ManifestUpdateMojo#manifestDebuggable}.
	 */
	protected Boolean debuggable;

    protected SupportsScreens supportsScreens;

    protected List<CompatibleScreen> compatibleScreens;

    public String getVersionName() {
        return versionName;
    }

    public Integer getVersionCode() {
        return versionCode;
    }

    public Boolean getVersionCodeAutoIncrement() {
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

    public SupportsScreens getSupportsScreens() {
        return supportsScreens;
    }

    public List<CompatibleScreen> getCompatibleScreens() {
        return compatibleScreens;
    }
}
