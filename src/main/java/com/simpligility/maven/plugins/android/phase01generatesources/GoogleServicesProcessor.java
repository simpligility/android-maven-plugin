package com.simpligility.maven.plugins.android.phase01generatesources;

import com.google.common.base.Strings;
import com.google.common.io.Files;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import static com.google.common.base.Charsets.UTF_8;

/**
 * This code is based on the corresponding Gradle plugin. I have tried to keep the differences to a minimum (apart from
 * respecting the checkstyle rules).
 */
public class GoogleServicesProcessor 
{
    private static final String STATUS_DISABLED = "1";
    private static final String STATUS_ENABLED = "2";

    private static final String OAUTH_CLIENT_TYPE_WEB = "3";

    private File googleServicesJson;
    private File resDirectory;
    private String packageName;
    private Log log;

    public GoogleServicesProcessor( File googleServicesJson, File resDirectory, String packageName, Log log )
    {
        this.googleServicesJson = googleServicesJson;
        this.resDirectory = resDirectory;
        this.packageName = packageName;
        this.log = log;
    }

    public void process() throws IOException
    {
        if ( packageName == null )
        {
            throw new IllegalArgumentException( "Parameter packageName must be configured" );
        }

        if ( !googleServicesJson.isFile() )
        {
            throw new IOException( String.format( 
                    "File %s is missing. The Google Services Plugin cannot function without it",
                    googleServicesJson.getAbsolutePath() ) );
        }

        log.warn( "Parsing json file: " + googleServicesJson.getPath() );

        if ( !resDirectory.exists() && !resDirectory.mkdirs() )
        {
            throw new IOException( "Failed to create folder: " + resDirectory );
        }

        JsonElement root;
        try
        {
            root = new JsonParser().parse( Files.newReader( googleServicesJson, UTF_8 ) );
        }
        catch ( FileNotFoundException e )
        {
            throw new IOException( "Unable to find " + googleServicesJson.getAbsolutePath() );
        }

        if ( !root.isJsonObject() )
        {
            throw new IOException( "Malformed root json" );
        }

        JsonObject rootObject = root.getAsJsonObject();

        Map<String, String> resValues = new TreeMap<String, String>();
        Map<String, Map<String, String>> resAttributes = new TreeMap<String, Map<String, String>>();

        handleProjectNumberAndProjectId( rootObject, resValues );
        handleFirebaseUrl( rootObject, resValues );

        JsonObject clientObject = getClientForPackageName( rootObject );

        if ( clientObject != null )
        {
            handleAnalytics( clientObject, resValues );
            handleMapsService( clientObject, resValues );
            handleGoogleApiKey( clientObject, resValues );
            handleGoogleAppId( clientObject, resValues );
            handleWebClientId( clientObject, resValues );
        }
        else
        {
            throw new IOException( "No matching client found for package name '" + packageName + "'" );
        }

        // write the values file.
        File values = new File( resDirectory, "values" );
        if ( !values.exists() && !values.mkdirs() )
        {
            throw new IOException( "Failed to create folder: " + values );
        }

        File outputFile = new File( values, "google-services.xml" );
        try
        {
            log.info( "Writing " + outputFile.getAbsolutePath() );
            Files.write( getValuesContent( resValues, resAttributes ), outputFile, UTF_8 );
        }
        catch ( IOException e )
        {
            throw new IOException( "Unable to write output to " + outputFile.getAbsolutePath(), e );
        }
    }

    private void handleFirebaseUrl( JsonObject rootObject, Map<String, String> resValues )
            throws IOException
    {
        JsonObject projectInfo = rootObject.getAsJsonObject( "project_info" );
        if ( projectInfo == null )
        {
            throw new IOException( "Missing project_info object" );
        }

        JsonPrimitive firebaseUrl = projectInfo.getAsJsonPrimitive( "firebase_url" );
        if ( firebaseUrl != null )
        {
            resValues.put( "firebase_database_url", firebaseUrl.getAsString() );
        }
    }

    /**
     * Handle project_info/project_number for @string/gcm_defaultSenderId, and fill the res map with the read value.
     *
     * @param rootObject the root Json object.
     * @throws IOException
     */
    private void handleProjectNumberAndProjectId( JsonObject rootObject, Map<String, String> resValues )
            throws IOException
    {
        JsonObject projectInfo = rootObject.getAsJsonObject( "project_info" );
        if ( projectInfo == null )
        {
            throw new IOException( "Missing project_info object" );
        }

        JsonPrimitive projectNumber = projectInfo.getAsJsonPrimitive( "project_number" );
        if ( projectNumber == null )
        {
            throw new IOException( "Missing project_info/project_number object" );
        }

        resValues.put( "gcm_defaultSenderId", projectNumber.getAsString() );

        JsonPrimitive bucketName = projectInfo.getAsJsonPrimitive( "storage_bucket" );
        if ( bucketName != null )
        {
            resValues.put( "google_storage_bucket", bucketName.getAsString() );
        }
    }

    private void handleWebClientId( JsonObject clientObject, Map<String, String> resValues )
    {
        JsonArray array = clientObject.getAsJsonArray( "oauth_client" );
        if ( array != null )
        {
            final int count = array.size();
            for ( int i = 0; i < count; i++ )
            {
                JsonElement oauthClientElement = array.get( i );
                if ( oauthClientElement == null || !oauthClientElement.isJsonObject() )
                {
                    continue;
                }

                JsonObject oauthClientObject = oauthClientElement.getAsJsonObject();
                JsonPrimitive clientType = oauthClientObject.getAsJsonPrimitive( "client_type" );
                if ( clientType == null )
                {
                    continue;
                }

                String clientTypeStr = clientType.getAsString();
                if ( !OAUTH_CLIENT_TYPE_WEB.equals( clientTypeStr ) )
                {
                    continue;
                }

                JsonPrimitive clientId = oauthClientObject.getAsJsonPrimitive( "client_id" );
                if ( clientId == null )
                {
                    continue;
                }

                resValues.put( "default_web_client_id", clientId.getAsString() );
                return;
            }
        }
    }

    /**
     * Handle a client object for analytics ( @xml/global_tracker )
     *
     * @param clientObject the client Json object.
     * @throws IOException
     */
    private void handleAnalytics( JsonObject clientObject, Map<String, String> resValues )
            throws IOException
    {
        JsonObject analyticsService = getServiceByName( clientObject, "analytics_service" );
        if ( analyticsService == null )
        {
            return;
        }

        JsonObject analyticsProp = analyticsService.getAsJsonObject( "analytics_property" );
        if ( analyticsProp == null )
        {
            return;
        }

        JsonPrimitive trackingId = analyticsProp.getAsJsonPrimitive( "tracking_id" );
        if ( trackingId == null )
        {
            return;
        }

        resValues.put( "ga_trackingId", trackingId.getAsString() );

        File xml = new File( resDirectory, "xml" );
        if ( !xml.exists() && !xml.mkdirs() )
        {
            throw new IOException( "Failed to create folder: " + xml );
        }

        final File outputFile = new File( xml, "global_tracker.xml" );
        try
        {
            log.info( "Writing " + outputFile.getAbsolutePath() );
            Files.write( getGlobalTrackerContent( 
                    trackingId.getAsString() ),
                    outputFile,
                    UTF_8 );
        }
        catch ( IOException e )
        {
            throw new IOException( "Unable to write output to " + outputFile.getAbsolutePath(), e );
        }
    }

    /**
     * Handle a client object for maps ( @string/google_maps_key ).
     *
     * @param clientObject the client Json object.
     * @throws IOException
     */
    private void handleMapsService( JsonObject clientObject, Map<String, String> resValues )
            throws IOException
    {
        JsonObject mapsService = getServiceByName( clientObject, "maps_service" );
        if ( mapsService == null )
        {
            return;
        }

        String apiKey = getAndroidApiKey( clientObject );
        if ( apiKey != null )
        {
            resValues.put( "google_maps_key", apiKey );
            return;
        }
        throw new IOException( "Missing api_key/current_key object" );
    }

    private void handleGoogleApiKey( JsonObject clientObject, Map<String, String> resValues ) throws IOException
    {
        String apiKey = getAndroidApiKey( clientObject );
        if ( apiKey != null )
        {
            resValues.put( "google_api_key", apiKey );
            // TODO: remove this once SDK starts to use google_api_key.
            resValues.put( "google_crash_reporting_api_key", apiKey );
            return;
        }

        // if google_crash_reporting_api_key is missing.
        // throw new IOException( "Missing api_key/current_key object" );
        throw new IOException( "Missing api_key/current_key object" );
    }

    private String getAndroidApiKey( JsonObject clientObject )
    {
        JsonArray array = clientObject.getAsJsonArray( "api_key" );
        if ( array != null )
        {
            final int count = array.size();
            for ( int i = 0; i < count; i++ )
            {
                JsonElement apiKeyElement = array.get( i );
                if ( apiKeyElement == null || !apiKeyElement.isJsonObject() )
                {
                    continue;
                }

                JsonObject apiKeyObject = apiKeyElement.getAsJsonObject();
                JsonPrimitive currentKey = apiKeyObject.getAsJsonPrimitive( "current_key" );
                if ( currentKey == null )
                {
                    continue;
                }

                return currentKey.getAsString();
            }
        }
        return null;
    }

    /**
     * find an item in the "client" array that match the package name of the app
     *
     * @param jsonObject the root json object.
     * @return a JsonObject representing the client entry or null if no match is found.
     */
    private JsonObject getClientForPackageName( JsonObject jsonObject )
    {
        JsonArray array = jsonObject.getAsJsonArray( "client" );
        if ( array != null )
        {
            final int count = array.size();
            for ( int i = 0; i < count; i++ )
            {
                JsonElement clientElement = array.get( i );
                if ( clientElement == null || !clientElement.isJsonObject() )
                {
                    continue;
                }

                JsonObject clientObject = clientElement.getAsJsonObject();

                JsonObject clientInfo = clientObject.getAsJsonObject( "client_info" );
                if ( clientInfo == null )
                {
                    continue;
                }

                JsonObject androidClientInfo = clientInfo.getAsJsonObject( "android_client_info" );
                if ( androidClientInfo == null )
                {
                    continue;
                }

                JsonPrimitive clientPackageName = androidClientInfo.getAsJsonPrimitive( "package_name" );
                if ( clientPackageName == null )
                {
                    continue;
                }

                if ( packageName.equals( clientPackageName.getAsString() ) )
                {
                    return clientObject;
                }
            }
        }

        return null;
    }

    /**
     * Handle a client object for Google App Id.
     */
    private void handleGoogleAppId( JsonObject clientObject, Map<String, String> resValues )
            throws IOException
    {
        JsonObject clientInfo = clientObject.getAsJsonObject( "client_info" );
        if ( clientInfo == null )
        {
            // Should not happen
            throw new IOException( "Client does not have client info" );
        }

        JsonPrimitive googleAppId = clientInfo.getAsJsonPrimitive( "mobilesdk_app_id" );
        if ( googleAppId == null )
        {
            return;
        }

        String googleAppIdStr = googleAppId.getAsString();
        if ( Strings.isNullOrEmpty( googleAppIdStr ) )
        {
            return;
        }

        resValues.put( "google_app_id", googleAppIdStr );
    }

    /**
     * Finds a service by name in the client object. Returns null if the service is not found
     * or if the service is disabled.
     *
     * @param clientObject the json object that represents the client.
     * @param serviceName  the service name
     * @return the service if found.
     */
    private JsonObject getServiceByName( JsonObject clientObject, String serviceName )
    {
        JsonObject services = clientObject.getAsJsonObject( "services" );
        if ( services == null )
        {
            return null;
        }

        JsonObject service = services.getAsJsonObject( serviceName );
        if ( service == null )
        {
            return null;
        }

        JsonPrimitive status = service.getAsJsonPrimitive( "status" );
        if ( status == null )
        {
            return null;
        }

        String statusStr = status.getAsString();

        if ( STATUS_DISABLED.equals( statusStr ) )
        {
            return null;
        }

        if ( !STATUS_ENABLED.equals( statusStr ) )
        {
            log.warn( String.format( "Status with value '%1$s' for service '%2$s' is unknown",
                    statusStr,
                    serviceName ) );
            return null;
        }

        return service;
    }

    private static String getGlobalTrackerContent( String trackingId )
    {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<resources>\n"
                + "    <string name=\"ga_trackingId\" translatable=\"false\">" + trackingId + "</string>\n"
                + "</resources>\n";
    }

    private static String getValuesContent( Map<String, String> values,
                                           Map<String, Map<String, String>> attributes )
    {
        StringBuilder sb = new StringBuilder( 256 );

        sb.append( "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n" );

        for ( Map.Entry<String, String> entry : values.entrySet() )
        {
            String name = entry.getKey();
            sb.append( "    <string name=\"" ).append( name ).append( "\" translatable=\"false\"" );
            if ( attributes.containsKey( name ) )
            {
                for ( Map.Entry<String, String> attr : attributes.get( name ).entrySet() )
                {
                    sb.append( " " ).append( attr.getKey() ).append( "=\"" )
                            .append( attr.getValue() ).append( "\"" );
                }
            }
            sb.append( ">" ).append( entry.getValue() ).append( "</string>\n" );
        }

        sb.append( "</resources>\n" );

        return sb.toString();
    }
}
