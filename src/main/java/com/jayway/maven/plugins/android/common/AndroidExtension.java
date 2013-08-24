package com.jayway.maven.plugins.android.common;

/**
 * The file system extension for the Android artifact also used for the packaging type of an Android Maven Project.
 */
public final class AndroidExtension
{
    /**
     * Android application.
     */
    public static final String APK = "apk";

    /**
     * Android library project as created by Android Maven Plugin.
     */
    public static final String APKLIB = "apklib";

    /**
     * Android archive as introduced by the Gradle Android build system (modelled after apklib with extensions and some
     * differences(.
     */
    public static final String AAR = "aar";

    
    /**
     * @deprecated Use {@link APKLIB} instead.
     */
    public static final String APKSOURCES = "apksources";


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
        return APK.equals( packaging ) || APKLIB.equals( packaging ) || APKSOURCES.equals( packaging ) 
                || AAR.equalsIgnoreCase( packaging );
    }
}
