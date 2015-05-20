package com.simpligility.maven.plugins.android;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.google.common.base.Predicate;
import org.apache.maven.artifact.Artifact;

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.collect.FluentIterable.from;

public class InclusionExclusionResolver
{

    private InclusionExclusionResolver()
    {
    }

    /**
     * @param skipDependencies     Skip all dependencies, but respect {@code includeArtifactTypes}
     * @param includeArtifactTypes Artifact types to be always included even if {@code skipDependencies} is
     *                             {@code true}
     * @param excludeArtifactTypes Artifact types to be always excluded even if {@code skipDependencies} is
     *                             {@code false}
     */
    public static Set< Artifact > filterArtifacts( @NonNull Iterable< Artifact > artifacts,
            final boolean skipDependencies, @Nullable Collection< String > includeArtifactTypes,
            @Nullable Collection< String > excludeArtifactTypes )
    {
        final Collection< String > incArtifactTypes = firstNonNull( includeArtifactTypes,
                Collections.< String > emptyList() );
        final Collection< String > excArtifactTypes = firstNonNull( excludeArtifactTypes,
                Collections.< String > emptyList() );
        return from( artifacts )
            .filter( new Predicate < Artifact >() {
                @Override
                public boolean apply( Artifact artifact )
                {
                    final boolean inIncArtifactTypes = incArtifactTypes.contains( artifact.getType() );
                    final boolean inExcArtifactTypes = excArtifactTypes.contains( artifact.getType() );
                    return inIncArtifactTypes
                            || !skipDependencies && !inExcArtifactTypes;
                }
            } )
            .toSet();
    }

}
