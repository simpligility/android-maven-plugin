package com.jayway.maven.plugins.android.common;

import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Oleg Green
 */
public class AaptCommandBuilder
{
    protected List<String> commands = new ArrayList<String>();

    /**
     * Package the android resources.
     *
     * @return current instance of {@link AaptCommandBuilder}
     */
    public AaptCommandBuilder packageResources()
    {
        commands.add( "package" );
        return this;
    }

    /**
     * Make the resources ID non constant. This is required to make an R java class
     * that does not contain the final value but is used to make reusable compiled
     * libraries that need to access resources.
     *
     * @return current instance of {@link AaptCommandBuilder}
     */
    public AaptCommandBuilder makeResourcesNonConstant()
    {
        commands.add( "--non-constant-id" );
        return this;
    }

    /**
     * make package directories under location specified by {@link #setWhereToOutputResourceConstants}
     *
     * @return current instance of {@link AaptCommandBuilder}
     */
    public AaptCommandBuilder makePackageDirectories()
    {
        commands.add( "-m" );
        return this;
    }

    /**
     * Specify where to output R.java resource constant definitions
     *
     * @param path path where to output R.java
     * @return current instance of {@link AaptCommandBuilder}
     */
    public AaptCommandBuilder setWhereToOutputResourceConstants( String path )
    {
        commands.add( "-J" );
        commands.add( path );
        return this;
    }

    /**
     * Generates R.java into a different package
     *
     * @param packageName package name which generate R.java into
     * @return current instance of {@link AaptCommandBuilder}
     */
    public AaptCommandBuilder generateRIntoPackage( String packageName )
    {
        commands.add( "--custom-package" );
        commands.add( packageName );
        return this;
    }

    /**
     * Specify full path to AndroidManifest.xml to include in zip
     *
     * @param path full path to AndroidManifest.xml
     * @return current instance of {@link AaptCommandBuilder}
     */
    public AaptCommandBuilder setPathToAndroidManifest( String path )
    {
        commands.add( "-M" );
        commands.add( path );
        return this;
    }

    /**
     * Directory in which to find resources. <br>
     * Multiple directories will be scanned and the first match found (left to right) will take precedence.
     *
     * @param resourceDirectory resource directory {@link File}
     * @return current instance of {@link AaptCommandBuilder}
     */
    public AaptCommandBuilder addResourceDirectoryIfExists( File resourceDirectory )
    {
        if ( resourceDirectory != null && resourceDirectory.exists() )
        {
            commands.add( "-S" );
            commands.add( resourceDirectory.getAbsolutePath() );
        }
        return this;
    }

    /**
     * Directories in which to find resources. <br>
     * Multiple directories will be scanned and the first match found (left to right) will take precedence.
     *
     * @param resourceDirectories {@link List} of resource directories {@link File}
     * @return current instance of {@link AaptCommandBuilder}
     */
    public AaptCommandBuilder addResourceDirectoriesIfExists( List<File> resourceDirectories )
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
     * Directories in which to find resources. <br>
     * Multiple directories will be scanned and the first match found (left to right) will take precedence.
     *
     * @param resourceDirectories array of resource directories {@link File}
     * @return current instance of {@link AaptCommandBuilder}
     */
    public AaptCommandBuilder addResourceDirectoriesIfExists( File[] resourceDirectories )
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
     * Automatically add resources that are only in overlays
     *
     * @return current instance of {@link AaptCommandBuilder}
     */
    public AaptCommandBuilder autoAddOverlay()
    {
        commands.add( "--auto-add-overlay" );
        return this;
    }

    /**
     * Additional directory in which to find raw asset files
     *
     * @param apklibCombAssets
     * @return current instance of {@link AaptCommandBuilder}
     */
    public AaptCommandBuilder addRawAssetsDirectoryIfExists( File apklibCombAssets )
    {
        if ( apklibCombAssets != null && apklibCombAssets.exists() )
        {
            commands.add( "-A" );
            commands.add( apklibCombAssets.getAbsolutePath() );
        }
        return this;
    }

    /**
     * Add an existing package to base include set
     *
     * @param path
     * @return current instance of {@link AaptCommandBuilder}
     */
    public AaptCommandBuilder addExistingPackageToBaseIncludeSet( String path )
    {
        commands.add( "-I" );
        commands.add( path );
        return this;
    }

    /**
     * Specify which configurations to include.
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
    public AaptCommandBuilder addConfigurations( String configurations )
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
     * android-maven-plugin configuration
     *
     * @param extraArguments
     * @return current instance of {@link AaptCommandBuilder}
     */
    public AaptCommandBuilder addExtraArguments( String[] extraArguments )
    {
        if ( extraArguments != null )
        {
            for ( String extraArgument : extraArguments )
            {
                commands.add( extraArgument );
            }
        }
        return this;
    }

    /**
     * Makes output verbose
     *
     * @param isVerbose if true aapt will be verbose, otherwise - no
     * @return current instance of {@link AaptCommandBuilder}
     */
    public AaptCommandBuilder setVerbose( boolean isVerbose )
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
    public AaptCommandBuilder generateRTextFile( String folderForR )
    {
        commands.add( "--output-text-symbols" );
        commands.add( folderForR );
        return this;
    }

    /**
     * Force overwrite of existing files
     *
     * @return current instance of {@link AaptCommandBuilder}
     */
    public AaptCommandBuilder forceOverwriteExistingFiles()
    {
        commands.add( "-f" );
        return this;
    }

    /**
     * Disable PNG crunching
     *
     * @return current instance of {@link AaptCommandBuilder}
     */
    public AaptCommandBuilder disablePngCrunching()
    {
        commands.add( "--no-crunch" );
        return this;
    }

    /**
     * Specify the apk file to output
     *
     * @return current instance of {@link AaptCommandBuilder}
     */
    public AaptCommandBuilder setOutputApkFile( String outputApkFile )
    {
        commands.add( "-F" );
        commands.add( outputApkFile );
        return this;
    }

    @Override
    public String toString()
    {
        return commands.toString();
    }

    /**
     * Provides unmodifiable list of a aapt commands
     *
     * @return unmodifiable {@link List} of {@link String} commands
     */
    public List<String> build()
    {
        return Collections.unmodifiableList( commands );
    }
}
