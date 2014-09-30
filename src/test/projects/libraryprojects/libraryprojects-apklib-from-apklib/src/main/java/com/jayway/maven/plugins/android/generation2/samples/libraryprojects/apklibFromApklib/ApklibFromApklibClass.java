package com.jayway.maven.plugins.android.generation2.samples.libraryprojects.apklibFromApklib;

import com.jayway.maven.plugins.android.generation2.samples.libraryprojects.apklib1.Lib1Class;

/**
 * Extends lib1Class from apklib1 to prove that we have compiled using apklib1 as a dependency.
 */
public class ApklibFromApklibClass extends Lib1Class {

    public static String getApklibFromApklibString() {
        return "libraryprojects-apklib-from-apklib Java class";
    }
}
