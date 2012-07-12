package com.jayway.maven.plugins.android.configuration;

/**
 * @author Johan Lindquist
 */
public class HeaderFilesDirective
{

    /**
     * Base directory from where to include/exclude files from.
     */
    private String directory;

    /**
     * A list of &lt;include> elements specifying the files (usually C/C++ header files) that should be included in the
     * header archive. When not specified, the default includes will be <code><br/>
     * &lt;includes><br/>
     * &nbsp;&lt;include>**&#47;*.h&lt;/include><br/>
     * &lt;/includes><br/>
     * </code>
     *
     * @parameter
     */
    private String[] includes;

    /**
     * A list of &lt;include> elements specifying the files (usually C/C++ header files) that should be excluded from
     * the header archive.
     *
     * @parameter
     */
    private String[] excludes;

    public String getDirectory()
    {
        return directory;
    }

    public void setDirectory( String directory )
    {
        this.directory = directory;
    }

    public String[] getExcludes()
    {
        return excludes;
    }

    public void setExcludes( String[] excludes )
    {
        this.excludes = excludes;
    }

    public String[] getIncludes()
    {
        return includes;
    }

    public void setIncludes( String[] includes )
    {
        this.includes = includes;
    }
}
