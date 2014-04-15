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
package com.jayway.maven.plugins.android.common;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.logging.Logger;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Provides convenient functions for resolving artifacts.
 */
public final class ArtifactResolverHelper
{
    /**
     * Which dependency scopes should not be included when unpacking dependencies into the apk.
     */
    protected static final List<String> EXCLUDED_DEPENDENCY_SCOPES = Arrays.asList(
            Artifact.SCOPE_PROVIDED, Artifact.SCOPE_SYSTEM, Artifact.SCOPE_IMPORT
    );

    private final ArtifactResolver artifactResolver;
    private final Logger log;


    public ArtifactResolverHelper( ArtifactResolver artifactResolver, Logger log )
    {
        this.artifactResolver = artifactResolver;
        this.log = log;
    }

    /**
     * Filters provided artifacts and selects only defined types based on {@code types} argument
     * or all types if {@code types} argument is empty.
     *
     * @param allArtifacts artifacts to be filtered
     * @param types artifact types to be selected
     * @return a {@code List} of all project dependencies. Never {@code null}.
     *      This excludes artifacts of the {@code EXCLUDED_DEPENDENCY_SCOPES} scopes.
     *      And this should maintain dependency order to comply with library project resource precedence.
     */
    public Set<Artifact> getFilteredArtifacts( Iterable<Artifact> allArtifacts, String... types )
    {
        final List<String> acceptTypeList = Arrays.asList( types );
        boolean acceptAllArtifacts = acceptTypeList.isEmpty();
        final Set<Artifact> results = new LinkedHashSet<Artifact>();
        for ( Artifact artifact : allArtifacts )
        {
            if ( artifact == null )
            {
                continue;
            }

            if ( EXCLUDED_DEPENDENCY_SCOPES.contains( artifact.getScope() ) )
            {
                continue;
            }

            if ( acceptAllArtifacts || acceptTypeList.contains( artifact.getType() ) )
            {
                results.add( artifact );
            }
        }
        return results;
    }

    /**
     * Attempts to resolve an {@link org.apache.maven.artifact.Artifact} to a {@link java.io.File}.
     *
     * @param artifact to resolve
     * @return a {@link java.io.File} to the resolved artifact, never <code>null</code>.
     * @throws org.apache.maven.plugin.MojoExecutionException if the artifact could not be resolved.
     */
    public File resolveArtifactToFile( Artifact artifact ) throws MojoExecutionException
    {
        final Artifact resolvedArtifact = resolveArtifact( artifact );
        final File jar = resolvedArtifact.getFile();
        if ( jar == null )
        {
            throw new MojoExecutionException( "Could not resolve artifact " + artifact.getId()
                    + ". Please install it with \"mvn install:install-file ...\" or deploy it to a repository "
                    + "with \"mvn deploy:deploy-file ...\"" );
        }
        return jar;
    }

    public Set<Artifact> resolveArtifacts( Collection<Artifact> artifacts ) throws MojoExecutionException
    {
        final Set<Artifact> resolvedArtifacts = new LinkedHashSet<Artifact>();
        for ( final Artifact artifact : artifacts )
        {
            resolvedArtifacts.add( resolveArtifact( artifact ) );
        }
        return resolvedArtifacts;
    }

    /**
     * Resolves an artifact to a particular repository.
     *
     * @param artifact  Artifact to resolve
     * @return fully resolved artifact.
     */
    private Artifact resolveArtifact( Artifact artifact ) throws MojoExecutionException
    {
        final ArtifactResolutionRequest resolutionRequest = new ArtifactResolutionRequest().setArtifact( artifact );
        final ArtifactResolutionResult resolutionResult = this.artifactResolver.resolve( resolutionRequest );

        log.debug( "Resolving : " + artifact );
        if ( resolutionResult.getArtifacts().size() == 0 )
        {
            throw new MojoExecutionException( "Could not resolve artifact " + artifact
                    + ". Please install it with \"mvn install:install-file ...\" or deploy it to a repository "
                    + "with \"mvn deploy:deploy-file ...\"" );
        }
        if ( resolutionResult.getArtifacts().size() > 1 )
        {
            log.debug( "Resolved artifacts : " + resolutionResult.getArtifacts() );
            throw new MojoExecutionException( "Could not resolve artifact " + artifact
                    + " to single target. Found the following possible options : " + resolutionResult.getArtifacts() );
        }

        final Artifact resolvedArtifact = resolutionResult.getArtifacts().iterator().next();
        log.debug( "Resolved :" + artifact );
        return resolvedArtifact;
    }
}
