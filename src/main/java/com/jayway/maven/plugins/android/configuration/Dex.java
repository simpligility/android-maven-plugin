package com.jayway.maven.plugins.android.configuration;

/**
 * Configuration for the dex  test execution. This class is only the definition of the parameters that are
 * shadowed in
 * {@link com.jayway.maven.plugins.android.phase08preparepackage.DexMojo} and used there.
 *
 * @author Manfred Moser <manfred@simpligility.com>
 */
public class Dex {
    /**
      * Mirror of {@link com.jayway.maven.plugins.android.phase08preparepackage.DexMojo#dexJvmArguments}
      */
    private String[] jvmArguments;
    /**
      * Mirror of {@link com.jayway.maven.plugins.android.phase08preparepackage.DexMojo#dexCoreLibrary}
      */
    private boolean coreLibrary = false;
    /**
      * Mirror of {@link com.jayway.maven.plugins.android.phase08preparepackage.DexMojo#dexNoLocals}
      */
    private boolean noLocals = false;
    /**
      * Mirror of {@link com.jayway.maven.plugins.android.phase08preparepackage.DexMojo#dexOptimize}
      */
    private boolean optimize = true;

    public String[] getJvmArguments() {
        return jvmArguments;
    }

    public boolean isCoreLibrary() {
        return coreLibrary;
    }

    public boolean isNoLocals() {
        return noLocals;
    }

    public boolean isOptimize() {
        return optimize;
    }
}
