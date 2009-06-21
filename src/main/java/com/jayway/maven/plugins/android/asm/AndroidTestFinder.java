package com.jayway.maven.plugins.android.asm;

import org.codehaus.plexus.util.DirectoryWalkListener;
import org.codehaus.plexus.util.DirectoryWalker;
import org.objectweb.asm.ClassReader;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Finds Android platformtest classes in a directory of compiled Java classes.
 */
public class AndroidTestFinder {

    private static final String[] TEST_PACKAGES = {"junit/framework/", "android/test/"};

    public static boolean containsAndroidTests(File classesBaseDirectory) throws MojoExecutionException {

        if (classesBaseDirectory == null || !classesBaseDirectory.isDirectory()) {
            throw new IllegalArgumentException("classesBaseDirectory must be a valid directory!");
        }

        final List<File>      classFiles      = findEligebleClassFiles(classesBaseDirectory);
        final DecendantFinder decendantFinder = new DecendantFinder(TEST_PACKAGES);

        for (File classFile : classFiles) {
            ClassReader classReader;
            try {
                classReader = new ClassReader(new FileInputStream(classFile));
            } catch (IOException e) {
                throw new MojoExecutionException("Error reading " + classFile + ".\nCould not determine whether it contains tests. Please specify with plugin config parameter <containsPlatformTests>true|false</containsPlatformTests>.", e);
            }
            classReader.accept(decendantFinder, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES | ClassReader.SKIP_CODE);
        }

        return decendantFinder.isDecendantFound();
    }

    private static List<File> findEligebleClassFiles(File classesBaseDirectory) {
        final List<File>      classFiles = new LinkedList<File>();
        final DirectoryWalker walker     = new DirectoryWalker();
        walker.setBaseDir(classesBaseDirectory);
        walker.addSCMExcludes();
        walker.addInclude("**/*.class");
        walker.addDirectoryWalkListener(new DirectoryWalkListener() {
            public void directoryWalkStarting(File basedir) {
            }

            public void directoryWalkStep(int percentage, File file) {
                classFiles.add(file);
            }

            public void directoryWalkFinished() {
            }

            public void debug(String message) {
            }
        });
        walker.scan();
        return classFiles;
    }


}
