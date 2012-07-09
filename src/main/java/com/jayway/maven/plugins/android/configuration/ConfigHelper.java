package com.jayway.maven.plugins.android.configuration;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import java.lang.reflect.Array;
import java.lang.reflect.Field;

/**
 * Helper for parsing the embedded configuration of a mojo.
 *
 * @author <a href="mailto:pa314159&#64;gmail.com">Pappy Răzvan STĂNESCU &lt;pa314159&#64;gmail.com&gt;</a>
 */
public final class ConfigHelper
{

    static public void copyValues(AbstractMojo mojo, String confFieldName) throws MojoExecutionException
    {
        try
        {
            final Class<? extends AbstractMojo> mojoClass = mojo.getClass();
            final Field confField = mojoClass.getDeclaredField( confFieldName );

            confField.setAccessible( true );

            final Object conf = confField.get( mojo );

            if ( conf == null )
            {
                return;
            }

            for ( final Field field : conf.getClass().getDeclaredFields() )
            {
                field.setAccessible( true );

                final Object value = field.get( conf );

                if ( value == null )
                {
                    continue;
                }

                final Class<?> cls = value.getClass();

                if ( ( cls == String.class ) && ( ( (String) value ).length() == 0 ) )
                {
                    continue;
                }
                if ( cls.isArray() && ( Array.getLength( value ) == 0 ) )
                {
                    continue;
                }

                {
                    String mojoFieldName = field.getName();

                    mojoFieldName = Character.toUpperCase( mojoFieldName.charAt( 0 ) ) + mojoFieldName.substring( 1 );
                    mojoFieldName = confFieldName + mojoFieldName;

                    try
                    {
                        final Field mojoField = mojoClass.getDeclaredField( mojoFieldName );

                        mojoField.setAccessible( true );
                        mojoField.set( mojo, value );
                    } catch ( final NoSuchFieldException e )
                    {
                        ;
                    }
                }

                //  handle deprecated parameters
                {
                    try
                    {
                        final Field mojoField = mojoClass.getDeclaredField( field.getName() );

                        mojoField.setAccessible( true );
                        mojoField.set( mojo, value );
                    } catch ( final NoSuchFieldException e )
                    {
                        ;
                    } catch ( final IllegalArgumentException e )
                    {
                        // probably not a deprecated parameter, see Proguard configuration;
                    }
                }
            }
        } catch ( final Exception e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }
}
