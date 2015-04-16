package com.simpligility.maven.plugins.android.config;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * PullParameter is an annotation identifying a property as a configuration property for ConfigHandler.
 *
 * @author Adrian Stabiszewski https://github.com/grundid/
 * @author Manfred Moser <manfred@simpligility.com>
 * @see ConfigHandler
 */
@Target( { ElementType.FIELD } )
@Retention( RetentionPolicy.RUNTIME )
public @interface PullParameter
{

    String[] defaultValue() default { };

    String defaultValueGetterMethod() default "";

    boolean required() default false;
}
