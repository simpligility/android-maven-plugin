package com.jayway.maven.plugins.android.common;

/**
 * User: alexv
 * Date: 09/10/11
 * Time: 17:21
 */
public class FileNameHelper {
    //    { '/', '\n', '\r', '\t', '\0', '\f', '`', '?', '*', '\\', '<', '>', '|', '\"', ':' };
    private static final String ILLEGAL_CHARACTERS_REGEX = "[/\\n\\r\\t\\\0\\f`\\?\\*\\\\<>\\|\":]";
    private static final String SEPERATOR = "_";

    public static String fixFileName(String fileName) {
        return fileName.replaceAll(ILLEGAL_CHARACTERS_REGEX, SEPERATOR);
    }

}
