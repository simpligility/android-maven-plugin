package com.jayway.maven.plugins.android.configuration;

/**
 * Configuration for a custom BuildConfig constant.
 *
 * @author jonasfa@gmail.com
 */
public class BuildConfigConstant
{
    /**
     * Name of the constant.
     * Eg.: SERVER_URL, etc
     *
     * @parameter expression="{android.buildConfigConstants[].name}"
     * @required
     */
    private String name;

    /**
     * Type of the value.
     * Eg.: String, int, com.mypackage.MyType, etc
     *
     * @parameter expression="{android.buildConfigConstants[].type}"
     * @required
     */
    private String type;

    /**
     * Value of the constant.
     * Eg.: MyString, 123, new com.mypackage.MyType(), etc
     *
     * @parameter expression="{android.buildConfigConstants[].value}"
     * @required
     */
    private String value;

    public String getType()
    {
        return type;
    }

    public String getValue()
    {
        return value;
    }

    public String getName()
    {
        return name;
    }
}
