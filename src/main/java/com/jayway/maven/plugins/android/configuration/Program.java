package com.jayway.maven.plugins.android.configuration;

/**
 * Configuration element inside MonkeyRunner configuration. It represents both filename and options for a given
 * monkeyrunner program execution.
 * 
 * @author Stéphane Nicolas <snicolas@octo.com>
 */
public class Program
{
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.MonkeyRunner#filename}
     */
    private String filename;
    /**
     * Mirror of {@link com.jayway.maven.plugins.android.standalonemojos.MonkeyRunner#options}
     */
    private String options;

    public String getFilename()
    {
        return filename;
    }

    public String getOptions()
    {
        return options;
    }
}
