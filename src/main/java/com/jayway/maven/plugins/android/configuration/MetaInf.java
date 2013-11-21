
package com.jayway.maven.plugins.android.configuration;

import org.codehaus.plexus.util.SelectorUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * POJO to specify META-INF include and exclude patterns.
 * 
 * @author Pappy STĂNESCU <a href="mailto:pappy.stanescu&#64;gmail.com">&lt;pappy.stanescu&#64;gmail.com&gt;</a>
 */
public class MetaInf
{

    private List<String> includes;

    private List<String> excludes;

    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }

        final MetaInf that = (MetaInf) obj;

        if ( this.includes == null )
        {
            if ( that.includes != null )
            {
                return false;
            }
        }
        else if ( !this.includes.equals( that.includes ) )
        {
            return false;
        }

        if ( this.excludes == null )
        {
            if ( that.excludes != null )
            {
                return false;
            }
        }
        else if ( !this.excludes.equals( that.excludes ) )
        {
            return false;
        }

        return true;
    }

<<<<<<< HEAD

=======
>>>>>>> issue-174-ext
    public MetaInf exclude( String... excludes )
    {
        getExcludes().addAll( Arrays.asList( excludes ) );

        return this;
    }

    public List<String> getExcludes()
    {
        if ( this.excludes == null )
        {
            this.excludes = new ArrayList<String>();
        }

        return this.excludes;
    }

    public List<String> getIncludes()
    {
        if ( this.includes == null )
        {
            this.includes = new ArrayList<String>();
        }

        return this.includes;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = ( prime * result ) + ( ( this.excludes == null ) ? 0 : this.excludes.hashCode() );
        result = ( prime * result ) + ( ( this.includes == null ) ? 0 : this.includes.hashCode() );
        return result;
    }

    public MetaInf include( String... includes )
    {
        getIncludes().addAll( Arrays.asList( includes ) );

        return this;
    }

    public boolean isIncluded( String name )
    {
        boolean included = this.includes == null;

        if ( this.includes != null )
        {
            for ( final String x : this.includes )
            {
                if ( SelectorUtils.matchPath( "META-INF/" + x, name ) )
                {
                    included = true;

                    break;
                }
            }
        }

        if ( included && ( this.excludes != null ) )
        {
            for ( final String x : this.excludes )
            {
                if ( SelectorUtils.matchPath( "META-INF/" + x, name ) )
                {
                    included = false;

                    break;
                }
            }
        }

        return included;
    }
}
