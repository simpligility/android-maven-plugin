package com.jayway.maven.plugins.android.common;

public final class AndroidExtension
{
    /**
     * Android application.
     */
    public static final String APK = "apk";

    /**
     * Android library project.
     */
    public static final String APKLIB = "apklib";

    /**
     * @deprecated Use {@link APKLIB} instead.
     */
    public static String APKSOURCES = "apksources";


    //No instances
    private AndroidExtension()
    {
    }


    /**
     * Determine whether or not a {@link MavenProject}'s packaging is an
     * Android project.
     *
     * @param packaging Project packaging.
     * @return True if an Android project.
     */
    public static boolean isAndroidPackaging( String packaging )
    {
        return APK.equals( packaging ) || APKLIB.equals( packaging ) || APKSOURCES.equals( packaging );
    }
}
