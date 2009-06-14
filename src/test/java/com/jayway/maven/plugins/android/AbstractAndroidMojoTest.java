/*
 * Copyright (C) 2009 Jayway AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jayway.maven.plugins.android;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @author hugo.josefson@jayway.com
 */
public class AbstractAndroidMojoTest {
    protected MyAbstractAndroidMojo androidMojo;

    @Before
    public void setUp() throws Exception {
        androidMojo = new MyAbstractAndroidMojo();
    }

    @Test
    public void givenAndroidManifestThenTargetPackageIsFound() throws MalformedURLException, URISyntaxException, MojoExecutionException {
        final URL    url                = this.getClass().getResource("AndroidManifest.xml");
        final URI    uri                = url.toURI();
        final File   file               = new File(uri);
        final String foundTargetPackage = androidMojo.extractPackageNameFromAndroidManifest(file);
        Assert.assertEquals("com.example.android.apis.tests", foundTargetPackage);
    }

    @Test
    public void givenAndroidManifestThenTestRunnerIsFound() throws MalformedURLException, URISyntaxException, MojoExecutionException {
        final URL    url             = this.getClass().getResource("AndroidManifest.xml");
        final URI    uri             = url.toURI();
        final File   file            = new File(uri);
        final String foundTestRunner = androidMojo.extractTestRunnerFromAndroidManifest(file);
        Assert.assertEquals("android.test.InstrumentationTestRunner", foundTestRunner);
    }

    @Test
    public void givenValidAndroidManifestXmlTreeThenPackageIsFound() throws IOException {
        final URL         resource               = this.getClass().getResource("AndroidManifestXmlTree.txt");
        final InputStream inputStream            = resource.openStream();
        final String      androidManifestXmlTree = IOUtils.toString(inputStream);
        final String      foundPackage           = androidMojo.extractPackageNameFromAndroidManifestXmlTree(androidManifestXmlTree);
        Assert.assertEquals("com.example.android.apis", foundPackage);
    }

    @Test
    public void givenApidemosApkThenPackageIsFound() throws IOException, MojoExecutionException {
        final URL    resource     = this.getClass().getResource("apidemos-0.1.0-SNAPSHOT.apk");
        final String foundPackage = androidMojo.extractPackageNameFromApk(new File(resource.getFile()));
        Assert.assertEquals("com.example.android.apis", foundPackage);
    }

    @Test
    public void givenApidemosPlatformtestsApkThenPackageIsFound() throws IOException, MojoExecutionException {
        final URL    resource     = this.getClass().getResource("apidemos-platformtests-0.1.0-SNAPSHOT.apk");
        final String foundPackage = androidMojo.extractPackageNameFromApk(new File(resource.getFile()));
        Assert.assertEquals("com.example.android.apis.tests", foundPackage);
    }

    private static class MyAbstractAndroidMojo extends AbstractAndroidMojo {
        private MyAbstractAndroidMojo() {
            super.androidSdk = AndroidSdkTest.SDK_1_5_PLATFORM_1_5;
        }

        public void execute() throws MojoExecutionException, MojoFailureException {

        }
    }
}
