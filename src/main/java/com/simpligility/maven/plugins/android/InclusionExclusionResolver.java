package com.simpligility.maven.plugins.android;

import java.util.Set;

import com.google.common.base.Predicate;
import org.apache.maven.artifact.Artifact;

import static com.google.common.collect.FluentIterable.from;

public class InclusionExclusionResolver
{

    private InclusionExclusionResolver()
    {
    }

    public static Set< Artifact > filterDependencies( Set< Artifact > artifacts, final boolean skipDependencies )
    {
        return from( artifacts )
            .filter( new Predicate<Artifact>() {
                @Override
                public boolean apply( Artifact artifact )
                {
                    return !skipDependencies;
                }
            } )
            .toSet();
    }

}
