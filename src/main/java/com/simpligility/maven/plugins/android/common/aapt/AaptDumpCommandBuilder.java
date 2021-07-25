package com.simpligility.maven.plugins.android.common.aapt;

import java.util.List;

/**
 * Class that responsible for building aapt commands for dumping information from apk file
 */
public interface AaptDumpCommandBuilder
{
    /**
     * @return Path to the version of AAPT to use. This will be AAPT2 if it is available otherwise AAPT.
     */
    String getApplicationPath();
    /**
     * Provides unmodifiable list of a aapt commands
     *
     * @return unmodifiable {@link List} of {@link String} commands
     */
    List<String> build();

    /**
     * @return true if the aapt2 executable exists.
     */
    boolean aapt2Exists();

    /**
     * Print the compiled xmls in the given assets.
     *
     * @return current instance of {@link AaptDumpCommandBuilder}
     */
    AaptDumpCommandBuilder xmlTree();

    /**
     * Print the packageName for the APK.
     *
     * @return current instance of {@link AaptDumpCommandBuilder}
     */
    AaptDumpCommandBuilder packageName();

    /**
     * Set path to Apk file where to dump info from.
     *
     * @param pathToApk path to apk file for dumping
     * @return current instance of {@link AaptDumpCommandBuilder}
     */
    AaptDumpCommandBuilder setPathToApk( String pathToApk );

    /**
     * @param assetFile name of the asset file
     * @return current instance of {@link AaptDumpCommandBuilder}
     */
    AaptDumpCommandBuilder addAssetFile( String assetFile );
}
