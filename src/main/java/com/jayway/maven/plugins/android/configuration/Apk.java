
package com.jayway.maven.plugins.android.configuration;

import java.io.File;

import com.jayway.maven.plugins.android.phase09package.ApkMojo;

/**
 * Embedded configuration of {@link com.jayway.maven.plugins.android.phase09package.ApkMojo}.
 * 
 * @author <a href="mailto:pa314159&#64;gmail.com">Pappy Răzvan STĂNESCU &lt;pa314159&#64;gmail.com&gt;</a>
 */
@SuppressWarnings( "unused" )
public class Apk
{

	/**
	 * Mirror of {@link com.jayway.maven.plugins.android.phase09package.ApkMojo#apkMetaIncludes}.
	 */
	private String[]	metaIncludes;

	/**
	 * Mirror of {@link com.jayway.maven.plugins.android.phase09package.ApkMojo#apkExtractDuplicates}.
	 */
	Boolean				extractDuplicates;

	/**
	 * Mirror of {@link com.jayway.maven.plugins.android.phase09package.ApkMojo#apkSourceDirectories}.
	 */
	private File[]		sourceDirectories;
}
