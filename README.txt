===============================================================================
MAVEN ANDROID PLUGIN
===============================================================================

-------------------------------------------------------------------------------
Setup your development environment
-------------------------------------------------------------------------------
1) Install Sun JDK 1.5+

2) Install Android SDK 1.1r1:
     http://developer.android.com/sdk/1.1_r1/
     NOTE: Android SDK 1.5 is not yet supported, but will be soon.
	 
3) Set environment variable ANDROID_SDK to the path of your installed Android
   SDK 1.1r1.

4) Run the install script install.sh or install.bat, supplied with this
   project.


-------------------------------------------------------------------------------
Test your development environment
-------------------------------------------------------------------------------
Try building the mavenized ApiDemos sample:

1) cd samples/ApiDemos

2) Connect an Android device via USB, or start an emulator.

3) mvn install

That should build an apk for the "apidemos" project and then an apk for the
"apidemos-platformtest" project. As part of the "apidemos-platformtest"
project, it will deploy both apk files to the connected device or emulator and
run the platform tests.


-------------------------------------------------------------------------------
Create your own Android application (apk)
-------------------------------------------------------------------------------
1) Create a project using the activitycreator tool:
     $ANDROID_SDK/docs/guide/developing/tools/othertools.html#activitycreator

     Example:
     activitycreator -o yourproject com.yourcompany.yourproject.YourActivityName

2) Create a pom.xml in your project, using the samples/ApiDemos/apidemos/pom.xml
   as template.
     Remove the entire <parent> tag.
     Change <groupId>, <artifactId>, <version> and <name> to your own.

3) Move the source directory to where Maven expects it:
     mv src java
     mkdir -p src/main
     mv java src/main/

4) You won't need these files/directories with maven-android-plugin, so remove
   them:
     rm -r bin build.xml default.properties libs

5) To build your apk, simply:
     mvn install

6) To deploy your apk to the connected device:
     mvn com.jayway.maven.plugins.android.generation2:maven-android-plugin:deploy
