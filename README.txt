====================================================================
MAVEN ANDROID PLUGIN v1
====================================================================

--------------------------------------------------------------------
About Maven-Android-Plugin version 1
--------------------------------------------------------------------
Version 1 of Maven-Android-Plugin is a direct clone of Masa's trunk.
It is meant as an easy way for any current user of Masa 1.0.0 to get
access to the latest bug fixes in Masa's trunk.

--------------------------------------------------------------------
How to use
--------------------------------------------------------------------
Use maven-android-plugin 1.0.1 just like Masa 1.0.0. Everything is
the same, except for a few bugs which have been fixed:

http://code.google.com/p/masa/issues/detail?id=15
http://code.google.com/p/masa/issues/detail?id=16
http://code.google.com/p/masa/issues/detail?id=17
http://code.google.com/p/masa/issues/detail?id=18
http://code.google.com/p/masa/issues/detail?id=19

Please see README.original.txt for configuration instructions,
which was the original README.txt in Masa. The Masa project website
may also be useful: http://masa.googlecode.com

You will need to update your pom.xml or ~/.m2/settings.xml to use
the Maven repository where maven-android-plugin is deployed:

  http://maven-android-plugin-m2repo.googlecode.com/svn/trunk/releases

You must also update your pom.xml to use the new groupId:

  <plugin>
    <groupId>com.jayway.maven.plugins.android.generation1.plugins</groupId>
    <artifactId>maven-dx-plugin</artifactId>
    <extensions>true</extensions>
  </plugin>

You should also remove any <version> tag, because leaving it out
will use the latest bugfix release of version 1.
