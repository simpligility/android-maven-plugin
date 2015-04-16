/*
 * Copyright 2014 Google Inc. All rights reserved.
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

package com.simpligility.maven.plugins.android.common;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.repackaged.com.google.common.base.Preconditions;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.api.services.androidpublisher.AndroidPublisher;
import com.google.api.services.androidpublisher.AndroidPublisherScopes;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

import javax.annotation.Nullable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Helper class to initialize the publisher APIs client library.
 * <p>
 * Before making any calls to the API through the client library you need to
 * call the {@link AndroidPublisherHelper#init(String)} method. This will run
 * all precondition checks for for client id and secret setup properly in
 * resources/client_secrets.json and authorize this client against the API.
 * </p>
 */
public class AndroidPublisherHelper
{

    private static final Log LOG = LogFactory.getLog( AndroidPublisherHelper.class );

    public static final String MIME_TYPE_APK = "application/vnd.android.package-archive";

    public static final String MIME_TYPE_IMAGE = "image/*";

    /** Path to the private key file (only used for Service Account auth). */
    private static final String SRC_RESOURCES_KEY_P12 = "resources/key.p12";

    /**
     * Path to the client secrets file (only used for Installed Application
     * auth).
     */
    private static final String RESOURCES_CLIENT_SECRETS_JSON = "/resources/client_secrets.json";

    /**
     * Directory to store user credentials (only for Installed Application
     * auth).
     */
    private static final String DATA_STORE_SYSTEM_PROPERTY = "user.home";
    private static final String DATA_STORE_FILE = ".store/android_publisher_api";
    private static final File DATA_STORE_DIR =
            new File( System.getProperty( DATA_STORE_SYSTEM_PROPERTY ), DATA_STORE_FILE );

    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    /** Global instance of the HTTP transport. */
    private static HttpTransport httpTransport;

    /** Installed application user ID. */
    private static final String INST_APP_USER_ID = "user";

    private static Credential authorizeWithServiceAccount( String serviceAccountEmail )
            throws GeneralSecurityException, IOException
    {
        LOG.info( String.format( "Authorizing using Service Account: %s", serviceAccountEmail ) );

        return authorizeWithServiceAccount( serviceAccountEmail, null );
    }

    private static Credential authorizeWithServiceAccount( String serviceAccountEmail, File pk12File )
            throws GeneralSecurityException, IOException
    {
        LOG.info( String.format( "Authorizing using Service Account: %s", serviceAccountEmail ) );

        // Build service account credential.
        GoogleCredential credential = new GoogleCredential.Builder()
                .setTransport( httpTransport )
                .setJsonFactory( JSON_FACTORY )
                .setServiceAccountId( serviceAccountEmail )
                .setServiceAccountScopes( Collections.singleton( AndroidPublisherScopes.ANDROIDPUBLISHER ) )
                .setServiceAccountPrivateKeyFromP12File( pk12File == null ? new File( SRC_RESOURCES_KEY_P12 )
                        : pk12File )
                .build();
        return credential;
    }

    /**
     * Ensure the client secrets file has been filled out.
     *
     * @param clientSecrets the GoogleClientSecrets containing data from the
     *            file
     */
    private static void checkClientSecretsFile( GoogleClientSecrets clientSecrets )
    {
        if ( clientSecrets.getDetails().getClientId().startsWith( "[[INSERT" )
                || clientSecrets.getDetails().getClientSecret().startsWith( "[[INSERT" ) )
        {
            LOG.error( "Enter Client ID and Secret from "
                    + "APIs console into resources/client_secrets.json." );
            System.exit( 1 );
        }
    }

    /**
     * Performs all necessary setup steps for running requests against the API
     * using the Installed Application auth method.
     *
     * @param applicationName the name of the application: com.example.app
     * @return the {@Link AndroidPublisher} service
     */
    public static AndroidPublisher init( String applicationName ) throws Exception
    {
        return init( applicationName, null );
    }

    /**
     * Performs all necessary setup steps for running requests against the API.
     *
     * @param applicationName the name of the application: com.example.app
     * @param serviceAccountEmail the Service Account Email (empty if using
     *            installed application)
     * @return the {@Link AndroidPublisher} service
     * @throws GeneralSecurityException
     * @throws IOException
     */
    public static AndroidPublisher init( String applicationName,
            @Nullable String serviceAccountEmail ) throws IOException, GeneralSecurityException
    {
        Preconditions.checkArgument( !Strings.isNullOrEmpty( applicationName ),
                "applicationName cannot be null or empty!" );

        // Authorization.
        newTrustedTransport();
        Credential credential;
        credential = authorizeWithServiceAccount( serviceAccountEmail );

        // Set up and return API client.
        return new AndroidPublisher.Builder(
                httpTransport, JSON_FACTORY, credential ).setApplicationName( applicationName )
                .build();
    }

    /**
     * Performs all necessary setup steps for running requests against the API.
     *
     * @param serviceAccountEmail the Service Account Email (empty if using
     *            installed application)
     * @return the {@Link AndroidPublisher} service
     * @throws GeneralSecurityException
     * @throws IOException
     */
    public static AndroidPublisher init( String applicationName, String serviceAccountEmail, File pk12File )
            throws IOException, GeneralSecurityException
    {

        // Authorization.
        newTrustedTransport();
        Credential credential = authorizeWithServiceAccount( serviceAccountEmail, pk12File );

        // Set up and return API client.
        return new AndroidPublisher.Builder( httpTransport, JSON_FACTORY, credential )
                .setApplicationName( applicationName )
                .build();
    }

    private static void newTrustedTransport() throws GeneralSecurityException,
            IOException
    {
        if ( null == httpTransport )
        {
            httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        }
    }

}