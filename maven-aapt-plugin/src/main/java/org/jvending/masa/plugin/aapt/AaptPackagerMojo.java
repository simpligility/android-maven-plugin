/*
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
package org.jvending.masa.plugin.aapt;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.jvending.masa.CommandExecutor;
import org.jvending.masa.ExecutionException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @goal package
 * @phase package
 * @description
 */
public class AaptPackagerMojo extends AbstractAaptMojo {


    public void execute() throws MojoExecutionException, MojoFailureException {

        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger(this.getLog());

        if (androidManifestFile == null) {
            androidManifestFile = new File(resourceDirectory.getParent(), "AndroidManifest.xml");
        }

        File androidJar = resolveAndroidJar();
        File outputFile = new File(project.getBuild().getDirectory(),  project.getBuild().getFinalName() + ".ap_");

        List<String> commands = new ArrayList<String>();
        commands.add("package");
        commands.add("-f");
        commands.add("-M");
        commands.add(androidManifestFile.getAbsolutePath());
        if (resourceDirectory.exists()) {
            commands.add("-S");
            commands.add(resourceDirectory.getAbsolutePath());
        }
        commands.add("-I");
        commands.add(androidJar.getAbsolutePath());
        commands.add("-F");
        commands.add(outputFile.getAbsolutePath());
        getLog().info("aapt " + commands.toString());
        try {
            executor.executeCommand("aapt", commands, project.getBasedir(), false);
        } catch (ExecutionException e) {
            throw new MojoExecutionException("", e);
        }
        /*
        File dexClassesFile = new File(project.getBuild().getDirectory(), project.getBuild().getFinalName() + ".classes-dex");

        ZipOutputStream os = null;
        InputStream is = null;

        try {
            ZipFile zipFile = new ZipFile(tmpOutputFile);
            os = new ZipOutputStream(new FileOutputStream(outputFile));

            for (ZipEntry entry : (List<ZipEntry>) Collections.list(zipFile.entries())) {
                os.putNextEntry(new ZipEntry(entry.getName()));
                is = zipFile.getInputStream(entry);
                byte[] buffer = new byte[1024];
                int i;
                while ((i = is.read(buffer)) > 0) {
                    os.write(buffer, 0, i);
                }
                is.close();
            }
            os.putNextEntry(new ZipEntry("classes.dex"));
            is = new FileInputStream(dexClassesFile);
            byte[] buffer = new byte[1024];
            int i;
            while ((i = is.read(buffer)) > 0) {
                os.write(buffer, 0, i);
            }
            is.close();
            os.close();
        } catch (IOException e) {
            throw new MojoExecutionException("", e);
        }
        finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {

                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }
        */
       // project.getArtifact().setFile(outputFile);
    }
}
