package com.simpligility.maven.plugins.android.common.aapt;

import java.io.File;
import java.util.List;

/**
 * Responsible for building the appt command that generates R.Java amd R.txt.
 */
public interface AaptGenerateSourcesCommandBuilder
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
     * Make the resources ID non constant.
     * <p>
     * This is required to make an R java class
     * that does not contain the final value but is used to make reusable compiled
     * libraries that need to access resources.
     *
     * @return current instance of {@link AaptGenerateSourcesCommandBuilder}
     */
    AaptGenerateSourcesCommandBuilder makeResourcesNonConstant();

    /**
     * Make the resources ID non constant.
     * <p>
     * This is required to make an R java class
     * that does not contain the final value but is used to make reusable compiled
     * libraries that need to access resources.
     *
     * @param make if true make resources ID non constant, otherwise ignore
     * @return current instance of {@link AaptGenerateSourcesCommandBuilder}
     */
    AaptGenerateSourcesCommandBuilder makeResourcesNonConstant( boolean make );

    /**
     * Make package directories under location specified by {@link #setResourceConstantsFolder}.
     *
     * @return current instance of {@link AaptCommandBuilder}
     */
    AaptGenerateSourcesCommandBuilder makePackageDirectories();

    /**
     * Specify where the R java resource constant definitions should be generated or found.
     *
     * @param path path to resource constants folder.
     * @return current instance of {@link AaptCommandBuilder}
     */
    AaptGenerateSourcesCommandBuilder setResourceConstantsFolder( File path );

    /**
     * Generates R java into a different package.
     *
     * @param packageName package name which generate R.java into
     * @return current instance of {@link AaptCommandBuilder}
     */
    AaptGenerateSourcesCommandBuilder generateRIntoPackage( String packageName );

    /**
     * Specify full path to AndroidManifest.xml to include in zip.
     *
     * @param path Path to AndroidManifest.xml
     * @return current instance of {@link AaptCommandBuilder}
     */
    AaptGenerateSourcesCommandBuilder setPathToAndroidManifest( File path );

    /**
     * Directory in which to find resources.
     * <p>
     * Multiple directories will be scanned and the first match found (left to right) will take precedence.
     *
     * @param resourceDirectory resource directory {@link File}
     * @return current instance of {@link AaptCommandBuilder}
     */
    AaptGenerateSourcesCommandBuilder addResourceDirectoryIfExists( File resourceDirectory );

    /**
     * Directories in which to find resources.
     * <p>
     * Multiple directories will be scanned and the first match found (left to right) will take precedence.
     *
     * @param resourceDirectories {@link List} of resource directories {@link File}
     * @return current instance of {@link AaptCommandBuilder}
     */
    AaptGenerateSourcesCommandBuilder addResourceDirectoriesIfExists( List<File> resourceDirectories );

    /**
     * Directories in which to find resources.
     * <p>
     * Multiple directories will be scanned and the first match found (left to right) will take precedence.
     *
     * @param resourceDirectories array of resource directories {@link File}
     * @return current instance of {@link AaptCommandBuilder}
     */
    AaptGenerateSourcesCommandBuilder addResourceDirectoriesIfExists( File[] resourceDirectories );

    /**
     * Automatically add resources that are only in overlays.
     *
     * @return current instance of {@link AaptCommandBuilder}
     */
    AaptGenerateSourcesCommandBuilder autoAddOverlay();

    /**
     * Additional directory in which to find raw asset files.
     *
     * @param assetsFolder Folder containing the combined raw assets to add.
     * @return current instance of {@link AaptCommandBuilder}
     */
    AaptGenerateSourcesCommandBuilder addRawAssetsDirectoryIfExists( File assetsFolder );

    /**
     * Add an existing package to base include set.
     *
     * @param path Path to existing package to add.
     * @return current instance of {@link AaptCommandBuilder}
     */
    AaptGenerateSourcesCommandBuilder addExistingPackageToBaseIncludeSet( File path );

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
    AaptGenerateSourcesCommandBuilder addConfigurations( String configurations );

    /**
     * Adds some additional aapt arguments that are not represented as separate parameters
     * android-maven-plugin configuration.
     *
     * @param extraArguments Array of extra arguments to pass to Aapt.
     * @return current instance of {@link AaptCommandBuilder}
     */
    AaptGenerateSourcesCommandBuilder addExtraArguments( String[] extraArguments );

    /**
     * Makes output verbose.
     *
     * @param isVerbose if true aapt will be verbose, otherwise - no
     * @return current instance of {@link AaptCommandBuilder}
     */
    AaptGenerateSourcesCommandBuilder setVerbose( boolean isVerbose );

    /**
     * Generates a text file containing the resource symbols of the R class in the
     * specified folder.
     *
     * @param folderForR folder in which text file will be generated
     * @return current instance of {@link AaptCommandBuilder}
     */
    AaptGenerateSourcesCommandBuilder generateRTextFile( File folderForR );

    /**
     * Force overwrite of existing files.
     *
     * @return current instance of {@link AaptCommandBuilder}
     */
    AaptGenerateSourcesCommandBuilder forceOverwriteExistingFiles();

    /**
     * Specify the apk file to output.
     *
     * @return current instance of {@link AaptCommandBuilder}
     */
    AaptGenerateSourcesCommandBuilder setOutputApkFile( File outputFile );

    /**
     * Rewrite the manifest so that its package name is the package name given here. <br>
     * Relative class names (for example .Foo) will be changed to absolute names with the old package
     * so that the code does not need to change.
     *
     * @param manifestPackage new manifest package to apply
     * @return current instance of {@link AaptGenerateSourcesCommandBuilder}
     */
    AaptGenerateSourcesCommandBuilder renameManifestPackage( String manifestPackage );

    /**
     * Rewrite the manifest so that all of its instrumentation components target the given package. <br>
     * Useful when used in conjunction with --rename-manifest-package to fix tests against
     * a package that has been renamed.
     *
     * @param instrumentationPackage new instrumentation target package to apply
     * @return current instance of {@link AaptGenerateSourcesCommandBuilder}
     */
    AaptGenerateSourcesCommandBuilder renameInstrumentationTargetPackage( String instrumentationPackage );

    /**
     * Inserts android:debuggable="true" into the application node of the
     * manifest, making the application debuggable even on production devices.
     *
     * @return current instance of {@link AaptGenerateSourcesCommandBuilder}
     */
    AaptGenerateSourcesCommandBuilder setDebugMode( boolean isDebugMode );

    /**
     * Output Proguard options to a File.
     *
     * @return current instance of {@link AaptCommandBuilder}
     */
    AaptGenerateSourcesCommandBuilder setProguardOptionsOutputFile( File outputFile );
}
