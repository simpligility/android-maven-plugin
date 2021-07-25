package com.simpligility.maven.plugins.android.common.aapt;

import com.simpligility.maven.plugins.android.AndroidSdk;
import org.apache.maven.plugin.logging.Log;

/**
 * Class that responsible for building aapt commands for dumping information from apk file
 */
public class AaptDumpCommandBuilder extends AaptCommandBuilder
{
    public AaptDumpCommandBuilder( AndroidSdk androidSdk, Log log )
    {
        super( androidSdk, log );
        commands.add( "dump" );
    }

    /**
     * Print the compiled xmls in the given assets.
     *
     * @return current instance of {@link com.simpligility.maven.plugins.android.common.aapt.AaptDumpCommandBuilder}
     */
    public com.simpligility.maven.plugins.android.common.aapt.AaptDumpCommandBuilder xmlTree()
    {
        commands.add( "xmltree" );
        return this;
    }

    /**
     * Set path to Apk file where to dump info from.
     *
     * @param pathToApk path to apk file for dumping
     * @return current instance of {@link com.simpligility.maven.plugins.android.common.aapt.AaptDumpCommandBuilder}
     */
    public com.simpligility.maven.plugins.android.common.aapt.AaptDumpCommandBuilder setPathToApk( String pathToApk )
    {
        commands.add( pathToApk );
        return this;
    }

    /**
     * @param assetFile name of the asset file
     * @return current instance of {@link com.simpligility.maven.plugins.android.common.aapt.AaptDumpCommandBuilder}
     */
    public com.simpligility.maven.plugins.android.common.aapt.AaptDumpCommandBuilder addAssetFile( String assetFile )
    {
        commands.add( assetFile );
        return this;
    }
}
