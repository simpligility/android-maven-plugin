
package com.jayway.maven.plugins.android.configuration;

/**
 * Embedded configuration of {@link com.jayway.maven.plugins.android.phase09package.ApkMojo}.
 * 
 * @author Pappy STĂNESCU <a href="mailto:pappy.stanescu&#64;gmail.com">&lt;pappy.stanescu&#64;gmail.com&gt;</a>
 */
@SuppressWarnings( "unused" )
public class Apk
{

    /**
     * Mirror of {@link com.jayway.maven.plugins.android.phase09package.ApkMojo#apkMetaIncludes}.
     */
    @Deprecated
    private String[] metaIncludes;

    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.ApkMojo#apkMetaInf}.
     */
    private MetaInf  metaInf;

    /**
     * Mirror of {@link com.jayway.maven.plugins.android.phase09package.ApkMojo#apkDebug}.
     */
    private Boolean  debug;

    /**
     * Mirror of {@link com.jayway.maven.plugins.android.phase09package.ApkMojo#apkNativeToolchain}.
     */
    private String   nativeToolchain;
}
