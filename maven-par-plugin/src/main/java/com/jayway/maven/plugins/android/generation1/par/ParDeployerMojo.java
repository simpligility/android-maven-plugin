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
package com.jayway.maven.plugins.android.generation1.par;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;

import java.net.URL;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.io.*;

/**
 * @goal deploy
 * @requiresProject true
 * @description
 */
public class ParDeployerMojo extends AbstractMojo {

    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * @parameter expression = "${serverUri}" default-value="http://localhost:8080/provisioning/stockpar"
     * @required
     */
    private String serverUri;

    public void execute() throws MojoExecutionException, MojoFailureException {
        URL url;
        try {
            url = new URL(serverUri);
        } catch (MalformedURLException e) {
            throw new MojoExecutionException(e.getMessage());
        }
        File parFile = new File(project.getBuild().getDirectory(), project.getBuild().getFinalName() + ".par");
        OutputStream os;
        HttpURLConnection connection;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Encoding", "multipart/form-data");
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=parfile");
            os = connection.getOutputStream();
            os.write("--parfile\r\n".getBytes());
            os.write("Content-Disposition: form-data; name=\"parfile\"; filename=\"".getBytes());
            os.write(parFile.getAbsolutePath().getBytes());
            os.write("\"\r\n".getBytes());
            os.write("Content-Type: application/x-zip-compressed\r\n\r\n".getBytes());

        } catch (IOException e) {
            throw new MojoExecutionException("Failed to connect to server: " + e.getMessage());
        }
        FileInputStream fis;
        try {
            fis = new FileInputStream(parFile);
        } catch (FileNotFoundException e) {
            try {
                if (os != null) os.close();
            } catch (IOException e1) {

            }
            throw new MojoExecutionException("Could not find provisioning archive: " + e.getMessage());
        }

        try {
            IOUtil.copy(fis, os);
            os.write("\r\n--parfile--".getBytes());
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to upload provisioning archive" + e.getMessage());
        }
        finally {
            try {
                if (os != null) os.close();
                fis.close();
            } catch (IOException e) {

            }
        }

        try {
            getLog().info("HTTP Response: " + connection.getResponseCode());
        } catch (IOException e) {

        }
    }
}
