package com.simpligility.maven.plugins.android.configuration;

import java.io.File;


/**
 * @author kedzie
 */
@SuppressWarnings( { "unused" } )
public class DataBinding
{

   /**
    * Mirror of {@link AbstractDataBinderMojo#dataBindingSkip}
    */
   private Boolean skip;

   /**
    * Mirror of {@link AbstractDataBinderMojo#dataBindingPrintEncodedErrors}
    */
   private Boolean printEncodedErrors;

   /**
    * Mirror of {@link AbstractDataBinderMojo#dataBindingVerbose}
    */
   private Boolean verbose;

   /**
    * Mirror of {@link AbstractDataBinderMojo#dataBindingBindingInfoDirectory}
    */
   private File bindingInfoDirectory;

   /**
    * Mirror of {@link AbstractDataBinderMojo#dataBindingLayoutInfoDirectory}
    */
   private File layoutInfoDirectory;

   /**
    * Mirror of {@link AbstractDataBinderMojo#dataBindingResourceDirectory}
    */
   private File resourceDirectory;
}
