package com.simpligility.maven.plugins.android.common.aapt;

import com.simpligility.maven.plugins.android.AndroidSdk;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Responsible for appt commands for linking resources.
 */
public final class Aapt2LinkCommandBuilder extends AaptCommandBuilder implements AaptLinkCommandBuilder
{
    public Aapt2LinkCommandBuilder( AndroidSdk androidSdk, Log log )
    {
        super( androidSdk, log );
        commands.add( "link" );
    }

    /**
     * Make the resources ID non constant.
     * <p>
     * This is required to make an R java class
     * that does not contain the final value but is used to make reusable compiled
     * libraries that need to access resources.
     *
     * @return current instance of {@link Aapt2LinkCommandBuilder}
     */
    public Aapt2LinkCommandBuilder makeResourcesNonConstant()
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
     * @return current instance of {@link Aapt2LinkCommandBuilder}
     */
    public Aapt2LinkCommandBuilder makeResourcesNonConstant( boolean make )
    {
        if ( make )
        {
            log.debug( "Adding non-final-ids" );
            commands.add( "--non-final-ids" );
        }
        return this;
    }

    /**
     * Make package directories under location specified by {@link #setResourceConstantsFolder}.
     *
     * @return current instance of {@link AaptCommandBuilder}
     */
    public Aapt2LinkCommandBuilder makePackageDirectories()
    {
        return this;
    }

    /**
     * Specify where the R java resource constant definitions should be generated or found.
     *
     * @param path path to resource constants folder.
     * @return current instance of {@link AaptCommandBuilder}
     */
    public Aapt2LinkCommandBuilder setResourceConstantsFolder( File path )
    {
        commands.add( "--java" );
        commands.add( path.getAbsolutePath() );
        return this;
    }

    /**
     * Specify full path to AndroidManifest.xml to include in zip.
     *
     * @param path Path to AndroidManifest.xml
     * @return current instance of {@link AaptCommandBuilder}
     */
    public Aapt2LinkCommandBuilder setPathToAndroidManifest( File path )
    {
        commands.add( "--manifest" );
        commands.add( path.getAbsolutePath() );
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
    public Aapt2LinkCommandBuilder addResourceDirectoryIfExists( File resourceDirectory )
    {
        if ( resourceDirectory != null && resourceDirectory.exists() )
        {
            commands.add( "-S" );
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
    public Aapt2LinkCommandBuilder addResourceDirectoriesIfExists( List<File> resourceDirectories )
    {
        if ( resourceDirectories != null )
        {
            for ( File resourceDirectory : resourceDirectories )
            {
                addResourceDirectoryIfExists( resourceDirectory );
            }
        }
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
    public Aapt2LinkCommandBuilder addResourceDirectoriesIfExists( File[] resourceDirectories )
    {
        if ( resourceDirectories != null )
        {
            for ( File resourceDirectory : resourceDirectories )
            {
                addResourceDirectoryIfExists( resourceDirectory );
            }
        }
        return this;
    }

    /**
     * Automatically add resources that are only in overlays.
     *
     * @return current instance of {@link AaptCommandBuilder}
     */
    public Aapt2LinkCommandBuilder autoAddOverlay()
    {
        commands.add( "--auto-add-overlay" );
        return this;
    }

    /**
     * Additional directory in which to find raw asset files.
     *
     * @param assetsFolder Folder containing the combined raw assets to add.
     * @return current instance of {@link AaptCommandBuilder}
     */
    public Aapt2LinkCommandBuilder addRawAssetsDirectoryIfExists( File assetsFolder )
    {
        if ( assetsFolder != null && assetsFolder.exists() )
        {
            log.debug( "Adding assets folder : " + assetsFolder );
            commands.add( "-A" );
            commands.add( assetsFolder.getAbsolutePath() );
        }
        return this;
    }

    /**
     * Add an existing package to base include set.
     *
     * @param path Path to existing package to add.
     * @return current instance of {@link AaptCommandBuilder}
     */
    public Aapt2LinkCommandBuilder addExistingPackageToBaseIncludeSet( File path )
    {
        commands.add( "-I" );
        commands.add( path.getAbsolutePath() );
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
    public Aapt2LinkCommandBuilder addConfigurations( String configurations )
    {
        if ( StringUtils.isNotBlank( configurations ) )
        {
            commands.add( "-c" );
            commands.add( configurations );
        }
        return this;
    }

    /**
     * Adds some additional aapt arguments that are not represented as separate parameters
     * android-maven-plugin configuration.
     *
     * @param extraArguments Array of extra arguments to pass to Aapt.
     * @return current instance of {@link AaptCommandBuilder}
     */
    public Aapt2LinkCommandBuilder addExtraArguments( String[] extraArguments )
    {
        if ( extraArguments != null )
        {
            commands.addAll( Arrays.asList( extraArguments ) );
        }
        return this;
    }

    /**
     * Makes output verbose.
     *
     * @param isVerbose if true aapt will be verbose, otherwise - no
     * @return current instance of {@link AaptCommandBuilder}
     */
    public Aapt2LinkCommandBuilder setVerbose( boolean isVerbose )
    {
        if ( isVerbose )
        {
            commands.add( "-v" );
        }
        return this;
    }

    /**
     * Generates a text file containing the resource symbols of the R class in the
     * specified folder.
     *
     * @param folderForR folder in which text file will be generated
     * @return current instance of {@link AaptCommandBuilder}
     */
    public Aapt2LinkCommandBuilder generateRTextFile( File folderForR )
    {
        commands.add( "--output-text-symbols" );
        commands.add( new File( folderForR , "R.txt" ).getAbsolutePath() );
        return this;
    }

    /**
     * Force overwrite of existing files.
     *
     * @return current instance of {@link AaptCommandBuilder}
     */
    public Aapt2LinkCommandBuilder forceOverwriteExistingFiles()
    {
        return this;
    }

    /**
     * Specify the apk file to output.
     *
     * @return current instance of {@link AaptCommandBuilder}
     */
    public Aapt2LinkCommandBuilder setOutputApkFile( File outputFile )
    {
        commands.add( "-o" );
        commands.add( outputFile.getAbsolutePath() );
        return this;
    }

    /**
     * Rewrite the manifest so that its package name is the package name given here. <br>
     * Relative class names (for example .Foo) will be changed to absolute names with the old package
     * so that the code does not need to change.
     *
     * @param manifestPackage new manifest package to apply
     * @return current instance of {@link Aapt2LinkCommandBuilder}
     */
    public Aapt2LinkCommandBuilder renameManifestPackage( String manifestPackage )
    {
        if ( StringUtils.isNotBlank( manifestPackage ) )
        {
            commands.add( "--rename-manifest-package" );
            commands.add( manifestPackage );
        }
        return this;
    }

    /**
     * Rewrite the manifest so that all of its instrumentation components target the given package. <br>
     * Useful when used in conjunction with --rename-manifest-package to fix tests against
     * a package that has been renamed.
     *
     * @param instrumentationPackage new instrumentation target package to apply
     * @return current instance of {@link Aapt2LinkCommandBuilder}
     */
    public Aapt2LinkCommandBuilder renameInstrumentationTargetPackage( String instrumentationPackage )
    {
        if ( StringUtils.isNotBlank( instrumentationPackage ) )
        {
            commands.add( "--rename-instrumentation-target-package" );
            commands.add( instrumentationPackage );
        }
        return this;
    }

    /**
     * Inserts android:debuggable="true" into the application node of the
     * manifest, making the application debuggable even on production devices.
     *
     * @return current instance of {@link Aapt2LinkCommandBuilder}
     */
    public Aapt2LinkCommandBuilder setDebugMode( boolean isDebugMode )
    {
        if ( isDebugMode )
        {
            log.info( "Generating debug apk." );
            commands.add( "--debug-mode" );
        }
        else
        {
            log.info( "Generating release apk." );
        }
        return this;
    }
}
