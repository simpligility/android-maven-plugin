package com.jayway.maven.plugins.android.common;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Collates commands used to invoke Aapt.
 *
 * @author Oleg Green
 * @author William Ferguson
 * @author Manfred Moser
 */
public class AaptCommandBuilder
{
    protected final List<String> commands;
    protected final Log log;

    protected AaptCommandBuilder( Log log )
    {
        this.log = log;
        this.commands = new ArrayList<String>();
    }

    /**
     * Package the android resources.
     *
     * @return instance of {@link AaptPackageCommandBuilder}
     */
    public static AaptPackageCommandBuilder packageResources( Log log )
    {
        return new AaptPackageCommandBuilder( log );
    }

    /**
     * Dump label, icon, permissions, compiled xmls etc.
     *
     * @return instance of {@link AaptDumpCommandBuilder}
     */
    public static final AaptDumpCommandBuilder dump( Log log )
    {
        return new AaptDumpCommandBuilder( log );
    }

    /**
     * Class that responsible for building appt commands for packaging resources
     */
    public static final class AaptPackageCommandBuilder extends AaptCommandBuilder
    {
        public AaptPackageCommandBuilder( Log log )
        {
            super( log );
            commands.add( "package" );
        }

        /**
         * Make the resources ID non constant.
         *
         * This is required to make an R java class
         * that does not contain the final value but is used to make reusable compiled
         * libraries that need to access resources.
         *
         * @return current instance of {@link AaptPackageCommandBuilder}
         */
        public AaptPackageCommandBuilder makeResourcesNonConstant()
        {
            return makeResourcesNonConstant( true );
        }

        /**
         * Make the resources ID non constant.
         *
         * This is required to make an R java class
         * that does not contain the final value but is used to make reusable compiled
         * libraries that need to access resources.
         *
         * @param make if true make resources ID non constant, otherwise ignore
         * @return current instance of {@link AaptPackageCommandBuilder}
         */
        public AaptPackageCommandBuilder makeResourcesNonConstant( boolean make )
        {
            if ( make )
            {
                log.debug( "Adding non-constant-id" );
                commands.add( "--non-constant-id" );
            }
            return this;
        }

        /**
         * Make package directories under location specified by {@link #setWhereToOutputResourceConstants}.
         *
         * @return current instance of {@link AaptCommandBuilder}
         */
        public AaptPackageCommandBuilder makePackageDirectories()
        {
            commands.add( "-m" );
            return this;
        }

        /**
         * Specify where to output R java resource constant definitions.
         *
         * @param path path where to output R.java
         * @return current instance of {@link AaptCommandBuilder}
         */
        public AaptPackageCommandBuilder setWhereToOutputResourceConstants( File path )
        {
            commands.add( "-J" );
            commands.add( path.getAbsolutePath() );
            return this;
        }

        /**
         * Generates R java into a different package.
         *
         * @param packageName package name which generate R.java into
         * @return current instance of {@link AaptCommandBuilder}
         */
        public AaptPackageCommandBuilder generateRIntoPackage( String packageName )
        {
            if ( StringUtils.isNotBlank( packageName ) )
            {
                commands.add( "--custom-package" );
                commands.add( packageName );
            }
            return this;
        }

        /**
         * Specify full path to AndroidManifest.xml to include in zip.
         *
         * @param path  Path to AndroidManifest.xml
         * @return current instance of {@link AaptCommandBuilder}
         */
        public AaptPackageCommandBuilder setPathToAndroidManifest( File path )
        {
            commands.add( "-M" );
            commands.add( path.getAbsolutePath() );
            return this;
        }

        /**
         * Directory in which to find resources.
         *
         * Multiple directories will be scanned and the first match found (left to right) will take precedence.
         *
         * @param resourceDirectory resource directory {@link File}
         * @return current instance of {@link AaptCommandBuilder}
         */
        public AaptPackageCommandBuilder addResourceDirectoryIfExists( File resourceDirectory )
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
         *
         * Multiple directories will be scanned and the first match found (left to right) will take precedence.
         *
         * @param resourceDirectories {@link List} of resource directories {@link File}
         * @return current instance of {@link AaptCommandBuilder}
         */
        public AaptPackageCommandBuilder addResourceDirectoriesIfExists( List<File> resourceDirectories )
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
        *
        * Multiple directories will be scanned and the first match found (left to right) will take precedence.
        *
        * @param resourceDirectories array of resource directories {@link File}
        * @return current instance of {@link AaptCommandBuilder}
        */
        public AaptPackageCommandBuilder addResourceDirectoriesIfExists( File[] resourceDirectories )
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
        public AaptPackageCommandBuilder autoAddOverlay()
        {
            commands.add( "--auto-add-overlay" );
            return this;
        }

        /**
         * Additional directory in which to find raw asset files.
         *
         * @param assetsFolder  Folder containing the combined raw assets to add.
         * @return current instance of {@link AaptCommandBuilder}
         */
        public AaptPackageCommandBuilder addRawAssetsDirectoryIfExists( File assetsFolder )
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
         * @param path  Path to existing package to add.
         * @return current instance of {@link AaptCommandBuilder}
         */
        public AaptPackageCommandBuilder addExistingPackageToBaseIncludeSet( File path )
        {
            commands.add( "-I" );
            commands.add( path.getAbsolutePath() );
            return this;
        }

        /**
         * Specify which configurations to include.
         *
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
        public AaptPackageCommandBuilder addConfigurations( String configurations )
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
         * @param extraArguments    Array of extra arguments to pass to Aapt.
         * @return current instance of {@link AaptCommandBuilder}
         */
        public AaptPackageCommandBuilder addExtraArguments( String[] extraArguments )
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
        public AaptPackageCommandBuilder setVerbose( boolean isVerbose )
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
        public AaptPackageCommandBuilder generateRTextFile( File folderForR )
        {
            commands.add( "--output-text-symbols" );
            commands.add( folderForR.getAbsolutePath() );
            return this;
        }

        /**
         * Force overwrite of existing files.
         *
         * @return current instance of {@link AaptCommandBuilder}
         */
        public AaptPackageCommandBuilder forceOverwriteExistingFiles()
        {
            commands.add( "-f" );
            return this;
        }

        /**
         * Disable PNG crunching.
         *
         * @return current instance of {@link AaptCommandBuilder}
         */
        public AaptPackageCommandBuilder disablePngCrunching()
        {
            commands.add( "--no-crunch" );
            return this;
        }

        /**
         * Specify the apk file to output.
         *
         * @return current instance of {@link AaptCommandBuilder}
         */
        public AaptPackageCommandBuilder setOutputApkFile( File outputFile )
        {
            commands.add( "-F" );
            commands.add( outputFile.getAbsolutePath() );
            return this;
        }

        /**
         * Output Proguard options to a File.
         *
         * @return current instance of {@link AaptCommandBuilder}
         */
        public AaptPackageCommandBuilder setProguardOptionsOutputFile( File outputFile )
        {
            if ( outputFile != null )
            {
                final File parentFolder = outputFile.getParentFile();
                if ( parentFolder != null )
                {
                    parentFolder.mkdirs();
                }
                log.debug( "Adding proguard file : " + outputFile );
                commands.add( "-F" );
                commands.add( outputFile.getAbsolutePath() );
            }
            return this;
        }

        /**
         * Rewrite the manifest so that its package name is the package name given here. <br>
         * Relative class names (for example .Foo) will be changed to absolute names with the old package
         * so that the code does not need to change.
         *
         * @param manifestPackage new manifest package to apply
         * @return current instance of {@link AaptPackageCommandBuilder}
         */
        public AaptPackageCommandBuilder renameManifestPackage( String manifestPackage )
        {
            commands.add( "--rename-manifest-package" );
            commands.add( manifestPackage );
            return this;
        }

        /**
         * Rewrite the manifest so that all of its instrumentation components target the given package. <br>
         * Useful when used in conjunction with --rename-manifest-package to fix tests against
         * a package that has been renamed.
         *
         * @param instrumentationPackage new instrumentation target package to apply
         * @return current instance of {@link AaptPackageCommandBuilder}
         */
        public AaptPackageCommandBuilder renameInstrumentationTargetPackage( String instrumentationPackage )
        {
            commands.add( "--rename-instrumentation-target-package" );
            commands.add( instrumentationPackage );
            return this;
        }

        /**
         * Inserts android:debuggable="true" into the application node of the
         * manifest, making the application debuggable even on production devices.
         *
         * @return current instance of {@link AaptPackageCommandBuilder}
         */
        public AaptPackageCommandBuilder setDebugMode( boolean isDebugMode )
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

    /**
     * Class that responsible for building aapt commands for dumping information from apk file
     */
    public static final class AaptDumpCommandBuilder extends AaptCommandBuilder
    {
        public AaptDumpCommandBuilder( Log log )
        {
            super( log );
            commands.add( "dump" );
        }
        /**
         * Print the compiled xmls in the given assets.
         *
         * @return current instance of {@link AaptDumpCommandBuilder}
         */
        public AaptDumpCommandBuilder xmlTree()
        {
            commands.add( "xmltree" );
            return this;
        }

        /**
         * Set path to Apk file where to dump info from.
         *
         * @param pathToApk path to apk file for dumping
         * @return current instance of {@link AaptDumpCommandBuilder}
         */
        public AaptDumpCommandBuilder setPathToApk( String pathToApk )
        {
            commands.add( pathToApk );
            return this;
        }

        /**
         *
         * @param assetFile name of the asset file
         * @return current instance of {@link AaptDumpCommandBuilder}
         */
        public AaptDumpCommandBuilder addAssetFile( String assetFile )
        {
            commands.add( assetFile );
            return this;
        }
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
