package com.simpligility.maven.plugins.android.configuration;

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
     * Mirror of {@link com.simpligility.maven.plugins.android.phase08preparepackage.D8Mojo#d8JvmArguments}
     */
    private String[] jvmArguments;
    /**
     * Mirror of {@link com.simpligility.maven.plugins.android.phase08preparepackage.D8Mojo#d8Intermediate}
     */
    private Boolean intermediate;
    /**
     * Mirror of {@link com.simpligility.maven.plugins.android.phase08preparepackage.D8Mojo#d8MainDexList}
     */
    private String mainDexList;

    /**
     * Mirror of {@link com.simpligility.maven.plugins.android.phase08preparepackage.D8Mojo#d8Arguments}
     */
    private String[] arguments;

    /**
     * Mirror of {@link com.simpligility.maven.plugins.android.phase08preparepackage.D8Mojo#d8Release}
     */
    private Boolean release;

    /**
     * Mirror of {@link com.simpligility.maven.plugins.android.phase08preparepackage.D8Mojo#d8MinApi}
     */
    private Integer minApi;

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

    public String[] getArguments()
    {
        return arguments;
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
