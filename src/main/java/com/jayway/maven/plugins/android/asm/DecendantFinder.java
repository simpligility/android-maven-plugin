/*
 * Copyright (C) 2009 Jayway AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jayway.maven.plugins.android.asm;

import org.apache.commons.lang.StringUtils;
import org.objectweb.asm.*;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Finds decendants of any class from specific parent packages.
 * Will remember if any match was found, and returns that fact in {@link #isDecendantFound()}.
 *
 * @author hugo.josefson@jayway.com
 */
public class DecendantFinder implements ClassVisitor {

    /**
     * Constructs this finder.
     * @param parentPackages Packages to find decendants of. Must be formatted with <code>/</code> (slash) instead of
     * <code>.</code> (dot) for example: <code>junit/framework/</code>
     */
    public DecendantFinder(String... parentPackages) {
        this.parentPackages = parentPackages;
    }

    private final String[]      parentPackages;
    private final AtomicBoolean isDecendantFound = new AtomicBoolean(false);

    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        for (String testPackage : parentPackages) {
            if (StringUtils.startsWith(superName, testPackage)) {
                flagAsFound();
//                System.out.println(name + " extends " + superName);
            }
        }
    }

    private void flagAsFound() {
        isDecendantFound.set(true);
    }

    /**
     * Returns whether a match was found.
     * @return <code>true</code> is a match was found, <code>false</code> otherwise.
     */
    public boolean isDecendantFound() {
        return isDecendantFound.get();
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