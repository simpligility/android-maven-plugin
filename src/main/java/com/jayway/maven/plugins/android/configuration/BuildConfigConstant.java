package com.jayway.maven.plugins.android.configuration;

import org.apache.maven.plugins.annotations.Parameter;

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
     */
    @Parameter ( property = "android.buildConfigConstants[].name" , required = true )
    private String name;

    /**
     * Type of the value.
     * Eg.: String, int, com.mypackage.MyType, etc
     */
    @Parameter ( property = "android.buildConfigConstants[].type", required = true )
    private String type;

    /**
     * Value of the constant.
     * Eg.: MyString, 123, new com.mypackage.MyType(), etc
     */
    @Parameter ( property = "android.buildConfigConstants[].value" , required = true )
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
