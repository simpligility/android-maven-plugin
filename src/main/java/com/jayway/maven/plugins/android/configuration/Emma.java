package com.jayway.maven.plugins.android.configuration;


/**
 * Configuration for the emma test execution. This class is only the definition of the parameters that are
 * shadowed in
 * {@link com.jayway.maven.plugins.android.phase08preparepackage.EmmaMojo} and used there.
 *
 * @author Mariusz Saramak <mariusz@saramak.eu>
 */
public class Emma
{

    /**
     * Mirror of {@link com.jayway.maven.plugins.android.phase08preparepackage.EmmaMojo#emmaEnable}
     */
    private Boolean enable;

    /**
     * Mirror of {@link com.jayway.maven.plugins.android.phase08preparepackage.EmmaMojo#emmaClassFolders}
     */
    private String classFolders;

    /**
     * Mirror of {@link com.jayway.maven.plugins.android.phase08preparepackage.EmmaMojo#emmaOutputMetaFile}
     */
    private String outputMetaFile;

    /**
     * Mirror of {@link com.jayway.maven.plugins.android.phase08preparepackage.EmmaMojo#emmaFilters}
     */
    private String filters;

    public String getFilters()
    {
        return filters;
    }

    public Boolean isEnable()
    {
        return enable;
    }

    public String getClassFolders()
    {
        return classFolders;
    }

    public String getOutputMetaFile()
    {
        return outputMetaFile;
    }
}
