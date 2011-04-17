package com.jayway.maven.plugins.android.common;

import org.apache.maven.project.MavenProject;

public final class AndroidExtension {
	/** Android application. */
	public static final String APK = "apk";
	
	/** Android library project. */
	public static final String APKLIB = "apklib";
	
	/** @deprecated Use {@link APKLIB} instead. */
	public static String APKSOURCES = "apksources";
	
	
	//No instances
	private AndroidExtension() {}
	
	
	/**
	 * Determine whether or not a {@link MavenProject} is an Android project.
	 * 
	 * @param project Project instance.
	 * @return True if an Android project.
	 */
	public static boolean isAndroidProject(MavenProject project) {
		return APK.equals(project.getPackaging())
			|| APKLIB.equals(project.getPackaging())
			|| APKSOURCES.equals(project.getPackaging());
	}
}
