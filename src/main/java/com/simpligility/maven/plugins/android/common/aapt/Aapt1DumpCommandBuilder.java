package com.simpligility.maven.plugins.android.common.aapt;

import com.simpligility.maven.plugins.android.AndroidSdk;
import org.apache.maven.plugin.logging.Log;

/**
 * Class that responsible for building aapt commands for dumping information from apk file.
 */
public final class Aapt1DumpCommandBuilder extends AaptCommandBuilder implements AaptDumpCommandBuilder
{
    public Aapt1DumpCommandBuilder( AndroidSdk androidSdk, Log log )
    {
        super( androidSdk, log );
        commands.add( "dump" );
    }

    /**
     * Print the compiled xmls in the given assets.
     *
     * @return current instance of {@link Aapt1DumpCommandBuilder}
     */
    public Aapt1DumpCommandBuilder xmlTree()
    {
        commands.add( "xmltree" );
        return this;
    }

    /**
     * Print the compiled xmls in the given assets.
     *
     * @return current instance of {@link Aapt1DumpCommandBuilder}
     */
    public Aapt1DumpCommandBuilder packageName()
    {
        commands.add( "xmltree" );
        return this;
    }

    /**
     * Set path to Apk file where to dump info from.
     *
     * @param pathToApk path to apk file for dumping
     * @return current instance of {@link Aapt1DumpCommandBuilder}
     */
    public Aapt1DumpCommandBuilder setPathToApk( String pathToApk )
    {
        commands.add( pathToApk );
        return this;
    }

    /**
     * @param assetFile name of the asset file
     * @return current instance of {@link Aapt1DumpCommandBuilder}
     */
    public Aapt1DumpCommandBuilder addAssetFile( String assetFile )
    {
        commands.add( assetFile );
        return this;
    }
}
