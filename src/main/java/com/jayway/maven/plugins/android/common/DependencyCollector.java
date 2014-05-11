package com.jayway.maven.plugins.android.common;

import edu.emory.mathcs.backport.java.util.Collections;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.codehaus.plexus.logging.Logger;

import java.util.HashSet;
import java.util.Set;

/**
 * Filters a graph of Artifacts and transforms it into a Set.
 */
final class DependencyCollector
{
    private final Logger log;
    private final Set<Artifact> dependencies = new HashSet<Artifact>();
    private final Artifact target;

    /**
     * @param logger    Logger on which to output and messages.
     * @param target    Artifact from which we will start collecting.
     */
    DependencyCollector( Logger logger, Artifact target )
    {
        this.log = logger;
        this.target = target;
    }

    /**
     * Visits all nodes from the given node and collects dependencies.
     *
     * @param node          DependencyNode from which to search.
     * @param collecting    Whether we are currently collecting artifacts.
     */
    public void visit( DependencyNode node, boolean collecting )
    {
        if ( collecting )
        {
            dependencies.add( node.getArtifact() );
        }

        if ( matchesTarget( node.getArtifact() ) )
        {
            collecting = true;
            log.debug( "Found target. Collecting dependencies after " + node.getArtifact() );
        }

        for ( final DependencyNode child : node.getChildren() )
        {
            visit( child, collecting );
        }
    }

    public Set<Artifact> getDependencies()
    {
        return Collections.unmodifiableSet( dependencies );
    }

    private boolean matchesTarget( Artifact found )
    {
        return found.getGroupId().equals( target.getGroupId() )
                && found.getArtifactId().equals( target.getArtifactId() )
                && found.getVersion().equals( target.getVersion() )
                && found.getType().equals( target.getType() )
                && classifierMatch( found.getClassifier(), target.getClassifier() )
                ;
    }

    private boolean classifierMatch( String classifierA, String classifierB )
    {
        final boolean hasClassifierA = !isNullOrEmpty( classifierA );
        final boolean hasClassifierB = !isNullOrEmpty( classifierB );
        if ( !hasClassifierA && !hasClassifierB )
        {
            return true;
        }
        else if ( hasClassifierA && hasClassifierB )
        {
            return classifierA.equals( classifierB );
        }
        return false;
    }

    private boolean isNullOrEmpty( String string )
    {
        return ( string == null ) || string.isEmpty();
    }
}
