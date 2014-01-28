/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    SAP AG - inject nested class path elements into maven model (TYCHO-483)
 *******************************************************************************/
package com.jayway.maven.plugins.android.phase_prebuild;

/*
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.artifacts.DependencyArtifacts;
import org.eclipse.tycho.core.osgitools.BundleReader;
*/

/**
 * Injects classpath into project somehow.
 * Copied from Tycho
 */
public final class MavenDependencyInjector
{

    /* see RepositoryLayoutHelper#getP2Gav */
    private static final String P2_GROUPID_PREFIX = "p2.";

    /**
     * Injects the dependencies of a project (as determined by the p2 dependency resolver) back into
     * the Maven model.
     * 
     * @param project
     *            A project
     * @param dependencies
     *            The p2-resolved dependencies of the project.
     */
/*
    public static void injectMavenDependencies(MavenProject project, DependencyArtifacts dependencies,
            BundleReader bundleReader, Logger logger) {
        MavenDependencyInjector generator = new MavenDependencyInjector(project, bundleReader, logger);
        for (ArtifactDescriptor artifact : dependencies.getArtifacts()) {
            generator.addDependency(artifact);
        }
    }

    private static final List<Dependency> NO_DEPENDENCIES = Collections.emptyList();

    private final BundleReader bundleReader;
    private final Logger logger;

    private final MavenProject project;

    MavenDependencyInjector(MavenProject project, BundleReader bundleReader, Logger logger) {
        this.project = project;
        this.bundleReader = bundleReader;
        this.logger = logger;
    }

    void addDependency(ArtifactDescriptor artifact) {
        List<Dependency> dependencyList = new ArrayList<Dependency>();
        if (artifact.getMavenProject() != null) {
            dependencyList.addAll(newProjectDependencies(artifact));
        } else {
            dependencyList.addAll(newExternalDependencies(artifact));
        }
        Model model = project.getModel();
        for (Dependency dependency : dependencyList) {
            model.addDependency(dependency);
        }
    }

    private List<Dependency> newExternalDependencies(ArtifactDescriptor artifact) {
        File location = artifact.getLocation();
        if (!location.isFile() || !location.canRead()) {
            logger.debug("Dependency at location " + location
                    + " can not be represented in Maven model and will not be visible to non-OSGi aware Maven plugins");
            return NO_DEPENDENCIES;
        }
        List<Dependency> result = new ArrayList<Dependency>();
        if (ArtifactKey.TYPE_ECLIPSE_PLUGIN.equals(artifact.getKey().getType())) {
            for (String classpathElement : getClasspathElements(location)) {
                if (".".equals(classpathElement)) {
                    result.add(createSystemScopeDependency(artifact.getKey(), location));
                } else {
                    File nestedJarOrDir = bundleReader.getEntry(location, classpathElement);
                    if (nestedJarOrDir != null) {
                        if (nestedJarOrDir.isFile()) {
                            Dependency nestedJarDependency = createSystemScopeDependency(artifact.getKey(),
                                    nestedJarOrDir);
                            nestedJarDependency.setClassifier(classpathElement);
                            result.add(nestedJarDependency);
                        } else if (nestedJarOrDir.isDirectory()) {
                            // system-scoped dependencies on directories are not supported
                            logger.debug("Dependency from "
                                    + project.getBasedir()
                                    + " to nested directory classpath entry "
                                    + nestedJarOrDir
                                    + " can not be represented in Maven model"
                                    + " and will not be visible to non-OSGi aware Maven plugins");
                        }
                    }
                }
            }
        } else {
            result.add(createSystemScopeDependency(artifact.getKey(), location));
        }
        return result;
    }

    private String[] getClasspathElements(File bundleLocation) {
        return bundleReader.loadManifest(bundleLocation).getBundleClasspath();
    }

    private Dependency createSystemScopeDependency(ArtifactKey artifactKey, File location) {
        return createSystemScopeDependency(artifactKey, P2_GROUPID_PREFIX + artifactKey.getType(), location);
    }

    private Dependency createSystemScopeDependency(ArtifactKey artifactKey, String groupId, File location) {
        Dependency dependency = new Dependency();
        dependency.setGroupId(groupId);
        dependency.setArtifactId(artifactKey.getId());
        dependency.setVersion(artifactKey.getVersion());
        dependency.setScope(Artifact.SCOPE_SYSTEM);
        dependency.setSystemPath(location.getAbsolutePath());
        return dependency;
    }

    private List<Dependency> newProjectDependencies(ArtifactDescriptor artifact) {
        ReactorProject dependentMavenProjectProxy = artifact.getMavenProject();
        List<Dependency> result = new ArrayList<Dependency>();
        if (!artifact.getMavenProject().sameProject(project)) {
            result.add(createProvidedScopeDependency(dependentMavenProjectProxy));
        }
        if (ArtifactKey.TYPE_ECLIPSE_PLUGIN.equals(dependentMavenProjectProxy.getPackaging())) {
            for (String classpathElement : getClasspathElements(dependentMavenProjectProxy.getBasedir())) {
                if (".".equals(classpathElement)) {
                    // covered by provided-scope dependency above
                    continue;
                } else // nested classpath entry
                {
                    File jar = new File(dependentMavenProjectProxy.getBasedir(), classpathElement);
                    // we can only add a system scope dependency for an existing (checked-in) jar file
                    // otherwise maven will throw a DependencyResolutionException
                    if (jar.isFile()) {
                        Dependency systemScopeDependency = createSystemScopeDependency(artifact.getKey(), artifact
                                .getMavenProject().getGroupId(), jar);
                        systemScopeDependency.setClassifier(classpathElement);
                        result.add(systemScopeDependency);
                    } else {
                        logger.debug("Dependency from "
                                + project.getBasedir()
                                + " to nested classpath entry "
                                + jar.getAbsolutePath()
                                + " can not be represented in Maven model"
                                + " and will not be visible to non-OSGi aware Maven plugins");
                    }
                }
            }
        }
        return result;
    }

    private Dependency createProvidedScopeDependency(ReactorProject dependentReactorProject) {
        Dependency dependency = new Dependency();
        dependency.setArtifactId(dependentReactorProject.getArtifactId());
        dependency.setGroupId(dependentReactorProject.getGroupId());
        dependency.setVersion(dependentReactorProject.getVersion());
        dependency.setType(dependentReactorProject.getPackaging());
        dependency.setScope(Artifact.SCOPE_PROVIDED);
        return dependency;
    }
        */
}
