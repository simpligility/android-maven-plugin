/*
 * Copyright (C) 2009 Jayway AB
 * Copyright (C) 2007-2008 JVending Masa
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
package com.jayway.maven.plugins.android.standalonemojos;

import com.jayway.maven.plugins.android.AbstractIntegrationtestMojo;
import com.jayway.maven.plugins.android.CommandExecutor;
import com.jayway.maven.plugins.android.ExecutionException;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.util.ArrayList;
import java.util.List;

/**
 * @author hugo.josefson@jayway.com
 */
public abstract class AbstractInstrumentationMojo extends AbstractIntegrationtestMojo {
    /**
     * Package name of the apk we wish to instrument. If not specified, it is inferred from
     * <code>AndroidManifest.xml</code>.
     *
     * @optional
     * @parameter expression="${android.instrumentationPackage}
     */
    private String instrumentationPackage;

    /**
     * Class name of test runner. If not specified, it is inferred from <code>AndroidManifest.xml</code>.
     *
     * @optional
     * @parameter expression="${android.instrumentationRunner}"
     */
    private String instrumentationRunner;

    protected void instrument() throws MojoExecutionException, MojoFailureException {
        if (instrumentationPackage == null) {
            instrumentationPackage = extractPackageNameFromAndroidManifest(androidManifestFile);
        }

        if (instrumentationRunner == null) {
            instrumentationRunner = extractInstrumentationRunnerFromAndroidManifest(androidManifestFile);
        }

        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger(this.getLog());

        List<String> commands = new ArrayList<String>();

        addDeviceParameter(commands);

        commands.add("shell");
        commands.add("am");
        commands.add("instrument");
        commands.add("-w");
        
        // only run Tests in specific package
        String testPackages = buildTestPackagesString();
        // only run Tests in specific class
        String testClasses = buildTestClassesString();
        boolean tcExists = StringUtils.isNotBlank(testClasses);
        boolean tpExists = StringUtils.isNotBlank(testPackages);
        
        if(tcExists && tpExists) {
            // if both testPackages and testClasses are specified --> ERROR
        	throw new MojoFailureException("testPackages and testClasses are mutual exclusive. They cannot be specified at the same time. " +
				"Please specify either testPackages or testClasses! For details, see http://developer.android.com/guide/developing/testing/testing_otheride.html");
        }
        
        if(tpExists) {
            commands.add("-e");
            commands.add("package");
            commands.add(testPackages);
            
            getLog().info("Running tests for specified test packages: " + testPackages);
        }
        
        if(tcExists) {
        	commands.add("-e");
        	commands.add("class");
        	commands.add(testClasses);
        	
        	getLog().info("Running tests for specified test classes/methods: " + testClasses);
        }
        //---- stop mkessel extensions
        
        commands.add(instrumentationPackage + "/" + instrumentationRunner);

        getLog().info(getAndroidSdk().getAdbPath() + " " + commands.toString());
        try {
            executor.executeCommand(getAndroidSdk().getAdbPath(), commands, project.getBasedir(), true);
            final String standardOut = executor.getStandardOut();
            final String standardError = executor.getStandardError();
            getLog().debug(standardOut);
            getLog().debug(standardError);
            // Fail when tests on device fail. adb does not exit with errorcode!=0 or even print to stderr, so we have to parse stdout.
            if (standardOut == null || !standardOut.matches(".*?OK \\([0-9]+ tests?\\)\\s*")) {
                throw new MojoFailureException("Tests failed on device.");
            }
        } catch (ExecutionException e) {
            getLog().error(executor.getStandardOut());
            getLog().error(executor.getStandardError());
            throw new MojoFailureException("Tests failed on device.");
        }
    }
}
