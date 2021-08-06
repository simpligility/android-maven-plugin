package com.simpligility.maven.plugins.android.common.aapt;

import com.simpligility.maven.plugins.android.AndroidSdk;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.util.List;

/**
 * Responsible for appt commands for compiling resources.
 */
public final class Aapt2CompileCommandBuilder extends AaptCommandBuilder implements AaptCompileCommandBuilder
{
    public Aapt2CompileCommandBuilder( AndroidSdk androidSdk, Log log )
    {
        super( androidSdk, log );
        commands.add( "compile" );
    }

    /**
     * Make the resources ID non constant.
     * <p>
     * This is required to make an R java class
     * that does not contain the final value but is used to make reusable compiled
     * libraries that need to access resources.
     *
     * @return current instance of {@link Aapt2CompileCommandBuilder}
     */
    public Aapt2CompileCommandBuilder makeResourcesNonConstant()
    {
        return makeResourcesNonConstant( true );
    }

    /**
     * Does nothing.
     */
    public Aapt2CompileCommandBuilder makeResourcesNonConstant( boolean make )
    {
        return this;
    }

    /**
     * Does nothing.
     */
    public Aapt2CompileCommandBuilder makePackageDirectories()
    {
        return this;
    }

    /**
     * Specify where the compiled flat resource files should be generated.
     *
     * @param path path to resource constants folder.
     * @return current instance of {@link AaptCommandBuilder}
     */
    public Aapt2CompileCommandBuilder setResourceConstantsFolder( File path )
    {
        commands.add( "-o" );
        commands.add( path.getAbsolutePath() );
        return this;
    }

    /**
     * Does nothing.
     */
    public Aapt2CompileCommandBuilder generateRIntoPackage( String packageName )
    {
        return this;
    }

    /**
     * Does nothing.
     */
    public Aapt2CompileCommandBuilder setPathToAndroidManifest( File path )
    {
        return this;
    }

    /**
     * Directory in which to find resources.
     * <p>
     * Multiple directories will be scanned and the first match found (left to right) will take precedence.
     *
     * @param resourceDirectory resource directory {@link File}
     * @return current instance of {@link AaptCommandBuilder}
     */
    public Aapt2CompileCommandBuilder addResourceDirectoryIfExists( File resourceDirectory )
    {
        if ( resourceDirectory != null && resourceDirectory.exists() )
        {
            commands.add( "--dir" );
            commands.add( resourceDirectory.getAbsolutePath() );
        }
        return this;
    }

    /**
     * Does nothing.
     */
    public Aapt2CompileCommandBuilder addResourceDirectoriesIfExists( List<File> resourceDirectories )
    {
        return this;
    }

    /**
     * Does nothing.
     */
    public Aapt2CompileCommandBuilder addResourceDirectoriesIfExists( File[] resourceDirectories )
    {
        return this;
    }

    /**
     * Does nothing.
     */
    public Aapt2CompileCommandBuilder autoAddOverlay()
    {
        return this;
    }

    /**
     * Does nothing.
     */
    public Aapt2CompileCommandBuilder addRawAssetsDirectoryIfExists( File assetsFolder )
    {
        return this;
    }

    /**
     * Add an existing package to base include set.
     *
     * @param path Path to existing package to add.
     * @return current instance of {@link AaptCommandBuilder}
     */
    public Aapt2CompileCommandBuilder addExistingPackageToBaseIncludeSet( File path )
    {
        return this;
    }

    /**
     * Does nothing.
     */
    public Aapt2CompileCommandBuilder addConfigurations( String configurations )
    {
        return this;
    }

    /**
     * Does nothing.
     */
    public Aapt2CompileCommandBuilder addExtraArguments( String[] extraArguments )
    {
        return this;
    }

    /**
     * Makes output verbose.
     *
     * @param isVerbose if true aapt will be verbose, otherwise - no
     * @return current instance of {@link AaptCommandBuilder}
     */
    public Aapt2CompileCommandBuilder setVerbose( boolean isVerbose )
    {
        if ( isVerbose )
        {
            commands.add( "-v" );
        }
        return this;
    }

    /**
     * Does nothing.
     */
    public Aapt2CompileCommandBuilder generateRTextFile( File folderForR )
    {
        return this;
    }

    /**
     * Does nothing.
     */
    public Aapt2CompileCommandBuilder forceOverwriteExistingFiles()
    {
        return this;
    }

    /**
     * Disable PNG crunching.
     *
     * @return current instance of {@link AaptCommandBuilder}
     */
    public Aapt2CompileCommandBuilder disablePngCrunching()
    {
        commands.add( "--no-crunch" );
        return this;
    }

    /**
     * Does nothing.
     */
    public Aapt2CompileCommandBuilder setProguardOptionsOutputFile( File outputFile )
    {
        return this;
    }

    /**
     * Does nothing.
     */
    public Aapt2CompileCommandBuilder renameManifestPackage( String manifestPackage )
    {
        return this;
    }
}
