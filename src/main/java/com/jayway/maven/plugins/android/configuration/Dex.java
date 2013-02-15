package com.jayway.maven.plugins.android.configuration;

/**
 * Configuration for the dex  test execution. This class is only the definition of the parameters that are
 * shadowed in
 * {@link com.jayway.maven.plugins.android.phase08preparepackage.DexMojo} and used there.
 *
 * @author Manfred Moser <manfred@simpligility.com>
 */
public class Dex
{
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.phase08preparepackage.DexMojo#dexJvmArguments}
     */
    private String[] jvmArguments;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.phase08preparepackage.DexMojo#dexCoreLibrary}
     */
    private Boolean coreLibrary;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.phase08preparepackage.DexMojo#dexNoLocals}
     */
    private Boolean noLocals;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.phase08preparepackage.DexMojo#dexOptimize}
     */
    private Boolean optimize;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.phase08preparepackage.DexMojo#dexPreDex}
     */
    private Boolean preDex;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.phase08preparepackage.DexMojo#dexPreDexLibLocation}
     */
    private String preDexLibLocation;

    public String[] getJvmArguments()
    {
        return jvmArguments;
    }

    public Boolean isCoreLibrary()
    {
        return coreLibrary;
    }

    public Boolean isNoLocals()
    {
        return noLocals;
    }

    public Boolean isOptimize()
    {
        return optimize;
    }

    public Boolean isPreDex()
    {
        return preDex;
    }

    public String getPreDexLibLocation()
    {
      return preDexLibLocation;
    }
}
