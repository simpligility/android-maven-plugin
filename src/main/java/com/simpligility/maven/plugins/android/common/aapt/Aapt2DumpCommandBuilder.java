package com.simpligility.maven.plugins.android.common.aapt;

import com.simpligility.maven.plugins.android.AndroidSdk;
import org.apache.maven.plugin.logging.Log;

/**
 * Responsible for building aapt commands for dumping xmltree information from apk file.
 */
public final class Aapt2DumpCommandBuilder extends AaptCommandBuilder implements AaptDumpCommandBuilder
{
    public Aapt2DumpCommandBuilder( AndroidSdk androidSdk, Log log )
    {
        super( androidSdk, log );
        commands.add( "dump" );
    }

    /**
     * Print the compiled xmls in the given assets.
     *
     * @return current instance of {@link Aapt2DumpCommandBuilder}
     */
    public Aapt2DumpCommandBuilder xmlTree()
    {
        commands.add( "xmltree" );
        return this;
    }

    /**
     * Print the packageName for the APK.
     *
     * @return current instance of {@link AaptDumpCommandBuilder}
     */
    public AaptDumpCommandBuilder packageName()
    {
        commands.add( "packagename" );
        return this;
    }

    /**
     * Set path to Apk file where to dump info from.
     *
     * @param pathToApk path to apk file for dumping
     * @return current instance of {@link Aapt2DumpCommandBuilder}
     */
    public Aapt2DumpCommandBuilder setPathToApk( String pathToApk )
    {
        commands.add( pathToApk );
        return this;
    }

    /**
     * @param assetFile name of the asset file
     * @return current instance of {@link Aapt2DumpCommandBuilder}
     */
    public Aapt2DumpCommandBuilder addAssetFile( String assetFile )
    {
        commands.add( "--file" );
        commands.add( assetFile );
        return this;
    }
}
