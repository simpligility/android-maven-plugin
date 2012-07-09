package com.jayway.maven.plugins.android.common;

/**
 * FileNameHelper can make a valid filename.
 *
 * @author alexv
 */
public class FileNameHelper
{
    //    { '/', '\n', '\r', '\t', '\0', '\f', '`', '?', '*', '\\', '<', '>', '|', '\"', ':' };
    private static final String ILLEGAL_CHARACTERS_REGEX = "[/\\n\\r\\t\\\0\\f`\\?\\*\\\\<>\\|\":]";
    private static final String SEPERATOR = "_";

    public static String fixFileName(String fileName)
    {
        return fileName.replaceAll( ILLEGAL_CHARACTERS_REGEX, SEPERATOR );
    }

}
