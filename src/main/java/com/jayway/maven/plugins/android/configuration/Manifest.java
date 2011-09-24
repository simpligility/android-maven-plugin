package com.jayway.maven.plugins.android.configuration;

/**
 * Created by IntelliJ IDEA.
 * User: manfred
 * Date: 23/09/11
 * Time: 8:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class Manifest {
    /**
	 * Update the <code>android:versionName</code> with the specified parameter.
	 *
	 * @parameter expression="${android.manifest.versionName}" default-value="${project.version}"
	 */
	protected String          versionName;

	/**
	 * Update the <code>android:versionCode</code> attribute with the specified parameter.
	 *
	 * @parameter expression="${android.manifest.versionCode}"
	 */
	protected Integer         versionCode;

	/**
	  * Auto increment the <code>android:versionCode</code> attribute with each build.
	  *
	  * @parameter expression="${android.manifest.versionCodeAutoIncrement}" default-value="false"
	  */
	 private boolean             versionCodeAutoIncrement = false;

	/**
	 * Update the <code>android:versionCode</code> attribute automatically from the project version
	 * e.g 3.0.1 will become version code 301. As described in this blog post
	 * http://www.simpligility.com/2010/11/release-version-management-for-your-android-application/
	 * but done without using resource filtering.
	 *
	 * @parameter expression="${android.manifest.versionCodeUpdateFromVersion} default-value="false"
	 */
	protected Boolean         versionCodeUpdateFromVersion = false;

	/**
	 * Update the <code>android:sharedUserId</code> attribute with the specified parameter.
	 *
	 * @parameter expression="${android.manifest.sharedUserId}"
	 */
	protected String          sharedUserId;

	/**
	 * Update the <code>android:debuggable</code> attribute with the specified parameter.
	 *
	 * @parameter expression="${android.manifest.debuggable}"
	 */
	protected Boolean         debuggable;

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
