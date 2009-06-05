====================================================================
MAVEN ANDROID PLUGIN VERSION 1
====================================================================

--------------------------------------------------------------------
About the Maven-Android-Plugin versions
--------------------------------------------------------------------
Version 1 of Maven-Android-Plugin is a direct clone of Masa's trunk.
It is meant as an easy way for any current user of Masa 1.0.0 to get
access to the latest bug fixes in Masa's trunk.

Version 2 of Maven-Android-Plugin has been improved in terms of
features, usability and bugfixing, compared to version 1. It is
recommended for all new users.

Please see http://maven-android-plugin.googlecode.com for version 2.


--------------------------------------------------------------------
How to use version 1
--------------------------------------------------------------------
Use maven-android-plugin 1.0.1+ just like Masa 1.0.0. Everything is
the same, except for a few bugs which have been fixed. Currently:

http://code.google.com/p/masa/issues/detail?id=15
http://code.google.com/p/masa/issues/detail?id=16
http://code.google.com/p/masa/issues/detail?id=17
http://code.google.com/p/masa/issues/detail?id=18
http://code.google.com/p/masa/issues/detail?id=19

Because Maven Android Plugin version 1 is primarily meant for
current users of Masa who just want the latest bugfixes, it is
assumed that you already have your project set up with Masa.
Otherwise please see README.original.txt for configuration
instructions, which was the original README.txt in Masa. The Masa
project website may also be useful for setting it up:
http://masa.googlecode.com

You will need to update your pom.xml (or ~/.m2/settings.xml) to use
the Maven repository where maven-android-plugin is deployed:

  <pluginRepositories>
    <pluginRepository>
      <id>maven-android-plugin-m2repo.googlecode.com</id>
      <name>Maven Android Plugin Release Repository</name>
      <url>http://maven-android-plugin-m2repo.googlecode.com/svn/trunk/releases</url>
    </pluginRepository>
  </pluginRepositories>

You must also update your pom.xml to use the new groupId:

  <plugin>
    <groupId>com.jayway.maven.plugins.android.generation1.plugins</groupId>
    <artifactId>maven-dx-plugin</artifactId>
    <extensions>true</extensions>
  </plugin>

You should remove any <version> tag from the <plugin> tag. That way
Maven will use the latest bugfix release of maven-android-plugin
version 1.
