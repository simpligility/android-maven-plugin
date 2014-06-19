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
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.phase08preparepackage.DexMojo#dexIncremental}
     */
    private Boolean incremental;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.phase08preparepackage.DexMojo#forceJumbo}
     */
    private Boolean forceJumbo;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.phase08preparepackage.DexMojo#multiDex}
     */
    private Boolean multiDex;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.phase08preparepackage.DexMojo#mainDexList}
     */
    private String mainDexList;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.phase08preparepackage.DexMojo#minimalMainDex}
     */
    private Boolean minimalMainDex;

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

    public Boolean isIncremental()
    {
        return incremental;
    }

    public Boolean isForceJumbo()
    {
        return forceJumbo;
    }

    public Boolean isMultiDex()
    {
        return multiDex;
    }

    public String getMainDexList()
    {
        return mainDexList;
    }

    public Boolean isMinimalMainDex()
    {
        return minimalMainDex;
    }

}
