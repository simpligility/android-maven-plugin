package com.jayway.maven.plugins.android.asm;

import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.util.DirectoryWalkListener;
import org.codehaus.plexus.util.DirectoryWalker;
import org.objectweb.asm.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class AndroidTestFinder implements ClassVisitor {

    public static void main(String[] args) throws IOException {
        final List<File> classFiles = new LinkedList<File>();

        DirectoryWalker walker = new DirectoryWalker();
        walker.setBaseDir(new File("/home/hugo/code/way/maven-android-plugin-samples/apidemos-15/apidemos-15-platformtests/target/android-classes"));
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



        AndroidTestFinder cp = new AndroidTestFinder();

        for (File classFile : classFiles) {
            ClassReader cr = new ClassReader(new FileInputStream(classFile));
            cr.accept(cp, 0);
        }

    }

    private static final String[] TEST_PACKAGES = {"junit/framework/", "android/test/"};
    private final AtomicBoolean isTestFound = new AtomicBoolean(false);

    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        for (String testPackage : TEST_PACKAGES) {
            if (StringUtils.startsWith(superName, testPackage)){
                flagAsTest();
                System.out.println(name + " is a test because it extends " + superName);
            }
        }
    }

    private void flagAsTest() {
        isTestFound.set(true);
    }

    public boolean isTestFound(){
        return isTestFound.get();
    }

    public void visitSource(String source, String debug) {
    }

    public void visitOuterClass(String owner, String name, String desc) {
    }

    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return null;
    }

    public void visitAttribute(Attribute attr) {
    }

    public void visitInnerClass(String name, String outerName, String innerName, int access) {
    }

    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        return null;
    }

    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        return null;
    }

    public void visitEnd() {
    }
}
