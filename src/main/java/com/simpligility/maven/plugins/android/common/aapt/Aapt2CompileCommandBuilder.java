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
     * Make the resources ID non constant.
     * <p>
     * This is required to make an R java class
     * that does not contain the final value but is used to make reusable compiled
     * libraries that need to access resources.
     *
     * @param make if true make resources ID non constant, otherwise ignore
     * @return current instance of {@link Aapt2CompileCommandBuilder}
     */
    public Aapt2CompileCommandBuilder makeResourcesNonConstant( boolean make )
    {
        return this;
    }

    /**
     * Make package directories under location specified by {@link #setResourceConstantsFolder}.
     *
     * @return current instance of {@link AaptCommandBuilder}
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
     * Generates R java into a different package.
     *
     * @param packageName package name which generate R.java into
     * @return current instance of {@link AaptCommandBuilder}
     */
    public Aapt2CompileCommandBuilder generateRIntoPackage( String packageName )
    {
        return this;
    }

    /**
     * Specify full path to AndroidManifest.xml to include in zip.
     *
     * @param path Path to AndroidManifest.xml
     * @return current instance of {@link AaptCommandBuilder}
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
     * Directories in which to find resources.
     * <p>
     * Multiple directories will be scanned and the first match found (left to right) will take precedence.
     *
     * @param resourceDirectories {@link List} of resource directories {@link File}
     * @return current instance of {@link AaptCommandBuilder}
     */
    public Aapt2CompileCommandBuilder addResourceDirectoriesIfExists( List<File> resourceDirectories )
    {
        return this;
    }

    /**
     * Directories in which to find resources.
     * <p>
     * Multiple directories will be scanned and the first match found (left to right) will take precedence.
     *
     * @param resourceDirectories array of resource directories {@link File}
     * @return current instance of {@link AaptCommandBuilder}
     */
    public Aapt2CompileCommandBuilder addResourceDirectoriesIfExists( File[] resourceDirectories )
    {
        return this;
    }

    /**
     * Automatically add resources that are only in overlays.
     *
     * @return current instance of {@link AaptCommandBuilder}
     */
    public Aapt2CompileCommandBuilder autoAddOverlay()
    {
        return this;
    }

    /**
     * Additional directory in which to find raw asset files.
     *
     * @param assetsFolder Folder containing the combined raw assets to add.
     * @return current instance of {@link AaptCommandBuilder}
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
     * Specify which configurations to include.
     * <p>
     * The default is all configurations. The value of the parameter should be a comma
     * separated list of configuration values.  Locales should be specified
     * as either a language or language-region pair.
     *
     * <p>Some examples:<ul>
     * <li>en</li>
     * <li>port,en</li>
     * <li>port,land,en_US</li></ul>
     *
     * <p>If you put the special locale, zz_ZZ on the list, it will perform
     * pseudolocalization on the default locale, modifying all of the
     * strings so you can look for strings that missed the
     * internationalization process.
     * <p>For example:<ul>
     * <li>port,land,zz_ZZ </li></ul>
     *
     * @param configurations configuration to include in form of {@link String}
     * @return current instance of {@link AaptCommandBuilder}
     */
    public Aapt2CompileCommandBuilder addConfigurations( String configurations )
    {
        return this;
    }

    /**
     * Adds some additional aapt arguments that are not represented as separate parameters
     * android-maven-plugin configuration.
     *
     * @param extraArguments Array of extra arguments to pass to Aapt.
     * @return current instance of {@link AaptCommandBuilder}
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
     * Generates a text file containing the resource symbols in the 'R-TextSymbols.txt' file in the specified folder.
     *
     * @param folderForR folder in which text file will be generated
     * @return current instance of {@link AaptCommandBuilder}
     */
    public Aapt2CompileCommandBuilder generateRTextFile( File folderForR )
    {
        commands.add( "--output-text-symbols" );
        commands.add( new File( folderForR , "R-TextSymbols.txt" ).getAbsolutePath() );
        return this;
    }

    /**
     * Force overwrite of existing files.
     *
     * @return current instance of {@link AaptCommandBuilder}
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
     * Output Proguard options to a File.
     *
     * @return current instance of {@link AaptCommandBuilder}
     */
    public Aapt2CompileCommandBuilder setProguardOptionsOutputFile( File outputFile )
    {
        return this;
    }

    /**
     * Rewrite the manifest so that its package name is the package name given here. <br>
     * Relative class names (for example .Foo) will be changed to absolute names with the old package
     * so that the code does not need to change.
     *
     * @param manifestPackage new manifest package to apply
     * @return current instance of {@link Aapt2CompileCommandBuilder}
     */
    public Aapt2CompileCommandBuilder renameManifestPackage( String manifestPackage )
    {
        return this;
    }
}
