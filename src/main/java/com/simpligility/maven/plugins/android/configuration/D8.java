package com.simpligility.maven.plugins.android.configuration;

import com.simpligility.maven.plugins.android.phase08preparepackage.DexMechanism;

/**
 * Configuration for the D8 execution. This class is only the definition of the parameters that are
 * shadowed in
 * {@link com.simpligility.maven.plugins.android.phase08preparepackage.D8Mojo} and used there.
 *
 * @author William Ferguson - william.ferguson@xandar.com.aui
 */
public class D8
{
    /**
     * Mirror of {@link com.simpligility.maven.plugins.android.phase08preparepackage.D8Mojo#dexJvmArguments}
     */
    private String[] jvmArguments;
    /**
     * Mirror of {@link com.simpligility.maven.plugins.android.phase08preparepackage.D8Mojo#dexIntermediate}
     */
    private Boolean intermediate;
    /**
     * Mirror of {@link com.simpligility.maven.plugins.android.phase08preparepackage.D8Mojo#dexMainDexList}
     */
    private String mainDexList;

    private String dexArguments;

    /**
     * Mirror of {@link com.simpligility.maven.plugins.android.phase08preparepackage.D8Mojo#dexRelease}
     */
    private Boolean release;

    /**
     * Mirror of {@link com.simpligility.maven.plugins.android.phase08preparepackage.D8Mojo#dexMinApi}
     */
    private Integer minApi;

    private DexMechanism dexMechanism = DexMechanism.Dex;

    public String[] getJvmArguments()
    {
        return jvmArguments;
    }

    public Boolean isIntermediate()
    {
        return intermediate;
    }

    public String getMainDexList()
    {
        return mainDexList;
    }

    public String getDexArguments()
    {
        return dexArguments;
    }

    public DexMechanism getDexMechanism()
    {
        return dexMechanism;
    }

    public Boolean isRelease()
    {
        return release;
    }

    public Integer getMinApi()
    {
        return minApi;
    }
}
