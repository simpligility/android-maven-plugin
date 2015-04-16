
package com.simpligility.maven.plugins.android.config;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;

/**
 * ConfigHandler is able to parse the configuration of a Mojo based on the Maven injected parameters as well as a config
 * pojo and annontations for default values on properties named parsed*. See the ProguardMojo for a working
 * implementation.
 * 
 * @author Adrian Stabiszewski https://github.com/grundid/
 * @author Manfred Moser <manfred@simpligility.com>
 * @see ConfigPojo
 * @see PullParameter
 */
public class ConfigHandler
{

    private Object mojo;
    private Object configPojoInstance;
    private String configPojoName;
    private String configPojoPrefix;
    private PluginParameterExpressionEvaluator evaluator;

    public ConfigHandler( Object mojo, MavenSession session, MojoExecution execution )
    {
        this.mojo = mojo;

        if ( session == null )
        {
            throw new IllegalArgumentException( "The argument session is required" );
        }
        if ( execution == null )
        {
            throw new IllegalArgumentException( "The argument execution is required" );
        }

        this.evaluator = new PluginParameterExpressionEvaluator( session, execution );

        initConfigPojo();
    }

    private Collection< Field > findPropertiesByAnnotation( Class< ? extends Annotation > annotation )
    {
        Collection< Field > result = new ArrayList< Field >();
        for ( Class< ? extends Object > cls = mojo.getClass(); cls != Object.class; cls = cls.getSuperclass() )
        {
            for ( Field field : cls.getDeclaredFields() )
            {
                if ( field.isAnnotationPresent( annotation ) )
                {
                    field.setAccessible( true );
                    result.add( field );
                }
            }
        }

        return result;
    }

    public void parseConfiguration()
    {
        Collection< Field > parsedFields = findPropertiesByAnnotation( PullParameter.class );

        for ( Field field : parsedFields )
        {
            Object value = null;
            String fieldBaseName = getFieldNameWithoutParsedPrefix( field );
            // first take the setting from the config pojo (e.g. nested config in plugin configuration)
            if ( configPojoInstance != null )
            {
                value = getValueFromPojo( fieldBaseName );
            }
            // then override with value from properties supplied in pom, settings or command line
            // unless it is null or an empty array
            Object propertyValue = getValueFromMojo( fieldBaseName );
            if ( propertyValue == null || propertyValue instanceof Object[]//
                && ( (Object[]) propertyValue ).length == 0 )
            {
                // no useful value
            }
            else
            {
                value = propertyValue;
            }
            // and only if we still have no value, get the default as declared in the annotation
            if ( value == null )
            {
                value = getValueFromAnnotation( field );
            }

            try
            {
                field.set( mojo, value );
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
        }
    }

    private Object getValueFromAnnotation( Field field )
    {
        PullParameter annotation = field.getAnnotation( PullParameter.class );
        String[] defaultValue = annotation.defaultValue();
        boolean required = annotation.required();
        String currentParameterName = "android." + configPojoName + "." + getFieldNameWithoutParsedPrefix( field );

        if ( defaultValue.length > 0 )
        {
            if ( defaultValue.length > 1 )
            {
                throw new RuntimeException( String.format( "Too many default values for field %s", field.getName() ) );
            }

            final Class< ? > fieldType = field.getType();

            try
            {
                final Object defValue = evaluator.evaluate( defaultValue[0], fieldType );

                if ( defValue == null || fieldType.isInstance( defValue ) )
                {
                    return defValue;
                }

                return convertTo( fieldType, defValue );
            }
            catch ( RuntimeException e )
            {
                throw e;
            }
            catch ( Exception e )
            {
                throw new RuntimeException( String.format(
                    "Problem encountered converting default value for %s parameter to %s",
                    currentParameterName, fieldType ), e );
            }
        }
        else
        {
            if ( !required )
            {
                // if no default value method, simply return null
                if ( annotation.defaultValueGetterMethod().isEmpty() )
                {
                    return null;
                }

                try
                {
                    Method method = mojo.getClass().getDeclaredMethod( annotation.defaultValueGetterMethod() );
                    // even access it if the method is private
                    method.setAccessible( true );
                    return method.invoke( mojo );
                }
                catch ( Exception e )
                {
                    throw new RuntimeException( String.format(
                        "Problem encountered accessing default value for %s parameter",
                        currentParameterName ), e );
                }
            }
            else
            {
                throw new RuntimeException( String.format(
                    "Required parameter %1$ has no value. Please "
                        + "supply with -D%1$=value on the command line or as property or "
                        + "plugin configuration in your pom or settings file.",
                    currentParameterName ) );
            }
        }
    }

    private Object convertTo( Class< ? > javaType, Object defValue )
    throws Exception
    {
        // try valueOf
        try
        {
            return javaType.getMethod( "valueOf", String.class ).invoke( null, defValue );
        }
        catch ( NoSuchMethodException e )
        {
            return javaType.getConstructor( String.class ).newInstance( defValue );
        }
    }

    private Object getValueFromMojo( String fieldBaseName )
    {
        return getValueFromObject( mojo, configPojoName + toFirstLetterUppercase( fieldBaseName ) );
    }

    private Object getValueFromPojo( String fieldBaseName )
    {
        return getValueFromObject( configPojoInstance, fieldBaseName );
    }

    private Object getValueFromObject( Object object, String fieldBaseName )
    {
        Object value = null;
        try
        {
            Field pojoField = findFieldByName( object, fieldBaseName );
            if ( pojoField != null )
            {
                value = pojoField.get( object );
            }
        }
        catch ( Exception e )
        {
            // swallow
        }
        return value;
    }

    private Field findFieldByName( Object object, String name )
    {
        for ( Field field : object.getClass().getDeclaredFields() )
        {
            if ( field.getName().equals( name ) )
            {
                field.setAccessible( true );
                return field;
            }
        }
        return null;
    }

    private String getFieldNameWithoutPrefix( Field field, String prefix )
    {
        if ( field.getName().startsWith( prefix ) )
        {
            String fieldName = field.getName().substring( prefix.length() );
            return fieldName.substring( 0, 1 ).toLowerCase() + fieldName.substring( 1 );
        }
        else
        {
            return field.getName();
        }
    }

    private String toFirstLetterUppercase( String s )
    {
        return s.substring( 0, 1 ).toUpperCase() + s.substring( 1 );
    }

    private String getFieldNameWithoutParsedPrefix( Field field )
    {
        return getFieldNameWithoutPrefix( field, configPojoPrefix );
    }

    private void initConfigPojo()
    {
        try
        {
            Field configPojo = findPropertiesByAnnotation( ConfigPojo.class ).iterator().next();
            configPojoName = configPojo.getName();
            configPojoInstance = configPojo.get( mojo );
            configPojoPrefix = configPojo.getAnnotation( ConfigPojo.class ).prefix();
        }
        catch ( Exception e )
        {
            // ignore, we can live without a config pojo
        }
    }
}
