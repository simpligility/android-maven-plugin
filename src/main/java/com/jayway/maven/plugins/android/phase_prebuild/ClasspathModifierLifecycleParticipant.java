/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package com.jayway.maven.plugins.android.phase_prebuild;

import com.jayway.maven.plugins.android.common.AndroidExtension;
import com.jayway.maven.plugins.android.common.ArtifactResolverHelper;
import com.jayway.maven.plugins.android.common.UnpackedLibHelper;
import com.jayway.maven.plugins.android.common.DependencyResolver;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Adds classes from AAR and APK dependencies to the project compile classpath.
 */
@Component( role = AbstractMavenLifecycleParticipant.class, hint = "default" )
public final class ClasspathModifierLifecycleParticipant extends AbstractMavenLifecycleParticipant
{
    @SuppressWarnings( "unused" )
    @Requirement
    private ArtifactResolver artifactResolver;

    @SuppressWarnings( "unused" )
    @Requirement( hint = "default" )
    private DependencyGraphBuilder dependencyGraphBuilder;

    @SuppressWarnings( "unused" )
    @Requirement
    private Logger log;

    @Override
    public void afterProjectsRead( MavenSession session ) throws MavenExecutionException
    {
        log.debug( "" );
        log.debug( "AMLP afterProjectsRead" );
        log.debug( "" );

        log.debug( "CurrentProject=" + session.getCurrentProject() );
        final List<MavenProject> projects = session.getProjects();
        final DependencyResolver dependencyResolver = new DependencyResolver( log, dependencyGraphBuilder );
        final ArtifactResolverHelper artifactResolverHelper = new ArtifactResolverHelper( artifactResolver, log );

        for ( MavenProject project : projects )
        {
            log.debug( "" );
            log.debug( "project=" + project.getArtifact() );

            final UnpackedLibHelper helper = new UnpackedLibHelper( artifactResolverHelper, project, log );

            final Set<Artifact> artifacts;
            try
            {
                artifacts = dependencyResolver.getProjectDependenciesFor( project );
            }
            catch ( MojoExecutionException e )
            {
                throw new MavenExecutionException( "Could not resolve dependencies for project : " + project, e );
            }

            log.debug( "projects deps: : " + artifacts );
            for ( Artifact artifact : artifacts )
            {
                final String type = artifact.getType();
                if ( type.equals( AndroidExtension.AAR ) )
                {
                    // An AAR lib contains a classes jar that needs to be added to the classpath.
                    // Create a placeholder classes.jar and add it to the compile classpath.
                    // It will replaced with the real classes.jar by GenerateSourcesMojo.
                    addClassesToClasspath( helper, project, artifact );
                }
                else if ( type.equals( AndroidExtension.APK ) )
                {
                    // The only time that an APK will likely be a dependency is when this an an APK test project.
                    // So add a placeholder (we cannot resolve the actual dep pre build) to the compile classpath.
                    // The placeholder will be replaced with the real APK jar later.
                    addClassesToClasspath( helper, project, artifact );
                }
            }
        }
    }

    /**
     * Add the dependent library classes to the project classpath.
     */
    private void addClassesToClasspath( UnpackedLibHelper helper, MavenProject project, Artifact artifact )
        throws MavenExecutionException
    {
        // Work out where the dep will be extracted and calculate the file path to the classes jar.
        // This is location where the GenerateSourcesMojo will extract the classes.
        final File classesJar = helper.getUnpackedClassesJar( artifact );
        log.debug( "Adding to classpath : " + classesJar );

        // In order to satisfy the LifecycleDependencyResolver on execution up to a phase that
        // has a Mojo requiring dependency resolution I need to create a dummy classesJar here.
        classesJar.getParentFile().mkdirs();
        try
        {
            classesJar.createNewFile();
            log.debug( "Created dummy " + classesJar.getName() + " exist=" + classesJar.exists() );
        }
        catch ( IOException e )
        {
            throw new MavenExecutionException( "Could not add " + classesJar.getName() + " as dependency", e );
        }

        // Add the classes to the classpath
        final Dependency dependency = createSystemScopeDependency( artifact, classesJar );
        project.getModel().addDependency( dependency );
    }

    private Dependency createSystemScopeDependency( Artifact artifact, File location )
    {
        final Dependency dependency = new Dependency();
        dependency.setGroupId( artifact.getGroupId() );
        dependency.setArtifactId( artifact.getArtifactId() );
        dependency.setVersion( artifact.getVersion() );
        dependency.setScope( Artifact.SCOPE_SYSTEM );
        dependency.setSystemPath( location.getAbsolutePath() );
        return dependency;
    }
}
  
