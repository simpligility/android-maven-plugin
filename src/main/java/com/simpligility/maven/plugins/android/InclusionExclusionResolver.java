package com.simpligility.maven.plugins.android;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import org.apache.maven.artifact.Artifact;

import static com.google.common.collect.FluentIterable.from;

public class InclusionExclusionResolver
{

    private InclusionExclusionResolver()
    {
    }

    /**
     * @param skipDependencies          Skip all dependencies, but respect {@code includeArtifactTypes}
     * @param includeArtifactTypes      Artifact types to be always included even if {@code skipDependencies} is
     *                                  {@code true}
     * @param excludeArtifactTypes      Artifact types to be always excluded even if {@code skipDependencies} is
     *                                  {@code false}
     * @param includeArtifactQualifiers Artifact qualifiers to be always included even if {@code skipDependencies} is
     *                                  {@code false}
     * @param excludeArtifactQualifiers Artifact qualifiers to be always excluded even if {@code skipDependencies} is
     *                                  {@code true}
     */
    public static Set< Artifact > filterArtifacts( @NonNull Iterable< Artifact > artifacts,
            final boolean skipDependencies, @Nullable final Collection< String > includeArtifactTypes,
            @Nullable final Collection< String > excludeArtifactTypes,
            @Nullable final Collection< String > includeArtifactQualifiers,
            @Nullable final Collection< String > excludeArtifactQualifiers )
    {
        final boolean hasIncludeTypes = includeArtifactTypes != null;
        final boolean hasExcludeTypes = excludeArtifactTypes != null;
        final boolean hasIncludeQualifier = includeArtifactQualifiers != null;
        final boolean hasExcludeQualifier = excludeArtifactQualifiers != null;
        return from( artifacts )
            .filter( new Predicate<Artifact>() {
                @Override
                public boolean apply( Artifact artifact )
                {
                    final boolean includedByType = hasIncludeTypes
                            && includeArtifactTypes.contains( artifact.getType() );
                    final boolean includedByQualifier = hasIncludeQualifier
                            && match( artifact, includeArtifactQualifiers );
                    final boolean excludedByType = hasExcludeTypes
                            && excludeArtifactTypes.contains( artifact.getType() );
                    final boolean excludedByQualifier = hasExcludeQualifier
                            && match( artifact, excludeArtifactQualifiers );
                    if ( !skipDependencies )
                    {
                        return !excludedByType && !excludedByQualifier
                                || includedByQualifier
                                || includedByType && !excludedByQualifier;
                    }
                    else
                    {
                        return includedByQualifier
                                || includedByType && hasExcludeQualifier && !excludedByQualifier
                                || includedByType;
                    }
                }
            } )
            .toSet();
    }

    private static boolean match( final Artifact artifact, Iterable< String > artifactQualifiers )
    {
        return from( artifactQualifiers )
            .filter( MUST_NOT_BE_BLANK )
            .anyMatch( new Predicate< String >() {
                @Override
                public boolean apply( String artifactQualifier )
                {
                    return match( artifact, artifactQualifier );
                }
            } );
    }

    private static boolean match( Artifact artifact, String artifactQualifier )
    {
        final List< String > split = from( COLON_SPLITTER.split( artifactQualifier ) ).transform( TRIMMER ).toList();
        final int count = split.size();
        if ( split.isEmpty() || count > 3 )
        {
            throw new IllegalArgumentException( "Invalid artifact qualifier: " + artifactQualifier );
        }
        // check groupId
        final String groupId = split.get( 0 );
        if ( !groupId.equals( artifact.getGroupId() ) )
        {
            return false;
        }
        if ( count == 1 )
        {
            return true;
        }
        // check artifactId
        final String artifactId = split.get( 1 );
        if ( !artifactId.equals( artifact.getArtifactId() ) )
        {
            return false;
        }
        if ( count == 2 )
        {
            return true;
        }
        // check version
        final String version = split.get( 2 );
        return version.equals( artifact.getVersion() );
    }

    private static final Splitter COLON_SPLITTER = Splitter.on( ':' );

    private static final Function< String, String > TRIMMER = new Function< String, String >()
    {
        @Override
        public String apply( String value )
        {
            return value.trim();
        }
    };

    private static final Predicate< String > MUST_NOT_BE_BLANK = new Predicate< String >()
    {
        @Override
        public boolean apply( String value )
        {
            return !value.trim().isEmpty();
        }
    };

}
