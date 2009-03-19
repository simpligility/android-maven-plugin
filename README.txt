Prerequisites:
1) JDK 1.5+
2) Android SDK: http://code.google.com/android/download.html

Steps to get working:
1) Set ANDROID_SDK variable to the path of the android sdk
2) Add ANDROID_SDK/tools to your path
3) Run the install script included in the maven-android project.
4) Build maven-android project: mvn install
5) Create an android project: http://code.google.com/android/reference/othertools.html#activitycreator
6) Create a pom.xml file for the project

The packaging type is android:apk

Sample POM
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
   <modelVersion>4.0.0</modelVersion>
   <groupId>org.jvending.masa</groupId>
   <artifactId>maven-test</artifactId>
   <version>1.0.1-sandbox</version>
   <packaging>android:apk</packaging>
   <name>maven-test</name>
   <description>Maven Plugin for Android DX</description>
   <dependencies>
      <dependency>
         <groupId>android</groupId>
         <artifactId>android</artifactId>
         <version>m5-rc15</version>
      </dependency>
   </dependencies>
   <build>
      <sourceDirectory>src</sourceDirectory>
      <plugins>
         <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
               <source>1.5</source>
               <target>1.5</target>
            </configuration>
         </plugin>
         <plugin>
            <groupId>org.jvending.masa.plugins</groupId>
            <artifactId>maven-android-plugin</artifactId>
            <extensions>true</extensions>
         </plugin>
      </plugins>
   </build>
</project>

7) Build project: mvn install

If you start your emulator prior to step 7, the install command will also deploy the apk file to the emulator
under the data/app directory.
