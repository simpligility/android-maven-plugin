
package com.jayway.maven.plugins.android.configuration;

import org.codehaus.plexus.util.SelectorUtils;

/**
 * POJO to specify META-INF include and exclude patterns.
 * 
 * @author <a href="mailto:pa314159&#64;gmail.com">Pappy STĂNESCU &lt;pa314159&#64;gmail.com&gt;</a>
 */
public class MetaInf
{

	private String[]	includes;

	private String[]	excludes;

	public boolean isIncluded( String name )
	{
		boolean included = includes == null;

		if( includes != null ) {
			for( String x : includes ) {
				if( SelectorUtils.matchPath( "META-INF/" + x, name ) ) {
					included = true;

					break;
				}
			}
		}

		if( included && excludes != null ) {
			for( String x : excludes ) {
				if( SelectorUtils.matchPath( "META-INF/" + x, name ) ) {
					included = false;

					break;
				}
			}
		}

		return included;
	}
}
