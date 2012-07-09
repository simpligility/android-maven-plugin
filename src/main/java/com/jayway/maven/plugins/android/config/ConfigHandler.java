package com.jayway.maven.plugins.android.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;

/**
 * ConfigHandler is able to parse the configuration of a Mojo based on the Maven injected parameters as well as
 * a config pojo and annontations for default values on properties named parsed*. See the ProguardMojo for a working
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

    public ConfigHandler( Object mojo )
    {
        this.mojo = mojo;
        initConfigPojo();
    }

    private Collection<Field> findPropertiesByAnnotation( Class<? extends Annotation> annotation )
    {
        Collection<Field> result = new ArrayList<Field>();
        for ( Field field : mojo.getClass().getDeclaredFields() )
        {
            if ( field.isAnnotationPresent( annotation ) )
            {
                field.setAccessible( true );
                result.add( field );
            }
        }
        return result;
    }

    public void parseConfiguration()
    {
        Collection<Field> parsedFields = findPropertiesByAnnotation( PullParameter.class );

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
            if ( propertyValue == null ||
                    ( propertyValue instanceof Object[] && ( ( Object[] ) propertyValue ).length == 0 ) )
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
            } catch ( Exception e )
            {
                e.printStackTrace();
            }
        }
    }

    private Object getValueFromAnnotation( Field field )
    {
        PullParameter annotation = field.getAnnotation( PullParameter.class );
        String defaultValue = annotation.defaultValue();
        boolean required = annotation.required();
        String currentParameterName = "android." + configPojoName + "." + getFieldNameWithoutParsedPrefix( field );

        if ( ! defaultValue.isEmpty() )
        { // TODO find a better way to define an empty default value
            Class<?> fieldType = field.getType();
            if ( fieldType.isAssignableFrom( String.class ) )
            {
                return defaultValue;
            }
            else if ( fieldType.isAssignableFrom( Boolean.class ) )
            {
                return Boolean.valueOf( defaultValue );
            }

            // TODO add more handler types as required, for example integer, long, ... we will do that when we encounter
            // them in other mojos..
            throw new RuntimeException(
                    "No handler for type " + fieldType + " on " + currentParameterName + " found." );
        }
        else if ( ! required )
        {
            try
            {
                Method method = mojo.getClass().getDeclaredMethod( annotation.defaultValueGetterMethod() );
                // even access it if the method is private
                method.setAccessible( true );
                return method.invoke( mojo );
            } catch ( Exception e )
            {
                throw new RuntimeException(
                        "Problem encountered accessing default value for " + currentParameterName + " parameter", e );
            }
        }
        else
        {
            throw new RuntimeException(
                    "Required parameter " + currentParameterName + " has no value. " + "Please supply with -D" +
                            currentParameterName +
                            "=value on the command line or as property or plugin configuration in your pom or settings file." );
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
        try
        {
            Field pojoField = findFieldByName( object, fieldBaseName );
            if ( pojoField != null )
            {
                return pojoField.get( object );
            }
        } catch ( Exception e )
        {
        }
        return null;
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
        } catch ( Exception e )
        {
            // ignore, we can live without a config pojo
        }
    }
}
