
package com.simpligility.maven.plugins.android.configuration;

/**
 * Embedded configuration of {@link com.simpligility.maven.plugins.android.phase09package.ApkMojo}.
 * 
 * @author Pappy STĂNESCU - pappy.stanescu@gmail.com
 */
@SuppressWarnings( "unused" )
public class Apk
{

    /**
     * Mirror of {@link com.simpligility.maven.plugins.android.phase09package.ApkMojo#apkMetaIncludes}.
     */
    @Deprecated
    private String[] metaIncludes;

    /**
     * Mirror of {@link com.simpligility.maven.plugins.android.phase09package.ApkMojo#apkMetaInf}.
     */
    private MetaInf  metaInf;

    /**
     * Mirror of {@link com.simpligility.maven.plugins.android.phase09package.ApkMojo#apkDebug}.
     */
    private Boolean  debug;

    /**
     * Mirror of {@link com.simpligility.maven.plugins.android.phase09package.ApkMojo#apkNativeToolchain}.
     */
    private String   nativeToolchain;
}
