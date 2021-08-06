package com.simpligility.maven.plugins.android.common.aapt;

import com.simpligility.maven.plugins.android.AndroidSdk;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.util.List;

/**
 * Responsible for appt commands for compiling resources.
 *
 * NB aapt doesn't compile resources.
 * This while class is a NOP.
 */
public final class Aapt1CompileCommandBuilder extends AaptCommandBuilder implements AaptCompileCommandBuilder
{
    public Aapt1CompileCommandBuilder( AndroidSdk androidSdk, Log log )
    {
        super( androidSdk, log );
    }

    /**
     * Does nothing.
     */
    public Aapt1CompileCommandBuilder makeResourcesNonConstant()
    {
        return this;
    }

    /**
     * Does nothing.
     */
    public Aapt1CompileCommandBuilder makeResourcesNonConstant( boolean make )
    {
        return this;
    }

    /**
     * Does nothing.
     */
    public Aapt1CompileCommandBuilder makePackageDirectories()
    {
        return this;
    }

    /**
     * Does nothing.
     */
    public Aapt1CompileCommandBuilder setResourceConstantsFolder( File path )
    {
        return this;
    }

    /**
     * Does nothing.
     */
    public Aapt1CompileCommandBuilder generateRIntoPackage( String packageName )
    {
        return this;
    }

    /**
     * Does nothing.
     */
    public Aapt1CompileCommandBuilder setPathToAndroidManifest( File path )
    {
        return this;
    }

    /**
     * Does nothing.
     */
    public Aapt1CompileCommandBuilder addResourceDirectoryIfExists( File resourceDirectory )
    {
        return this;
    }

    /**
     * Does nothing.
     */
    public Aapt1CompileCommandBuilder addResourceDirectoriesIfExists( List<File> resourceDirectories )
    {
        return this;
    }

    /**
     * Does nothing.
     */
    public Aapt1CompileCommandBuilder addResourceDirectoriesIfExists( File[] resourceDirectories )
    {
        return this;
    }

    /**
     * Does nothing.
     */
    public Aapt1CompileCommandBuilder autoAddOverlay()
    {
        return this;
    }

    /**
     * Does nothing.
     */
    public Aapt1CompileCommandBuilder addRawAssetsDirectoryIfExists( File assetsFolder )
    {
        return this;
    }

    /**
     * Does nothing.
     */
    public Aapt1CompileCommandBuilder addExistingPackageToBaseIncludeSet( File path )
    {
        return this;
    }

    /**
     * Does nothing.
     */
    public Aapt1CompileCommandBuilder addConfigurations( String configurations )
    {
        return this;
    }

    /**
     * Does nothing.
     */
    public Aapt1CompileCommandBuilder addExtraArguments( String[] extraArguments )
    {
        return this;
    }

    /**
     * Does nothing.
     */
    public Aapt1CompileCommandBuilder setVerbose( boolean isVerbose )
    {
        return this;
    }

    /**
     * Does nothing.
     */
    public Aapt1CompileCommandBuilder generateRTextFile( File folderForR )
    {
        return this;
    }

    /**
     * Does nothing.
     */
    public Aapt1CompileCommandBuilder forceOverwriteExistingFiles()
    {
        return this;
    }

    /**
     * Does nothing.
     */
    public Aapt1CompileCommandBuilder disablePngCrunching()
    {
        return this;
    }

    /**
     * Does nothing.
     */
    public Aapt1CompileCommandBuilder setProguardOptionsOutputFile( File outputFile )
    {
        return this;
    }

    /**
     * Does nothing.
     */
    public Aapt1CompileCommandBuilder renameManifestPackage( String manifestPackage )
    {
        return this;
    }
}
