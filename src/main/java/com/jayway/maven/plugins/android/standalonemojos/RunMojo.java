/*
 * Copyright (C) 2011 Lorenzo Villani
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

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.NullOutputReceiver;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.TimeoutException;
import com.jayway.maven.plugins.android.AbstractAndroidMojo;
import com.jayway.maven.plugins.android.DeviceCallback;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Executes the first activity with the <code>android.intent.action.MAIN</code> Intent Filter on all connected devices.
 * 
 * @author Lorenzo Villani <lorenzo@villani.me>
 * @goal run
 * @requiresProject false
 */
public class RunMojo
    extends AbstractAndroidMojo
{
    // ----------------------------------------------------------------------
    // Mojo types
    // ----------------------------------------------------------------------

    /**
     * Thrown when no launcher activities could be found inside <code>AndroidManifest.xml</code>
     * 
     * @author Lorenzo Villani
     */
    private static class ActivityNotFoundException
        extends Exception
    {
        private static final long serialVersionUID = 1L;

        /**
         * Constructor.
         */
        public ActivityNotFoundException()
        {
            super( "Unable to determine launcher Activity" );
        }
    }

    /**
     * Holds information about the "Launcher" activity.
     * 
     * @author Lorenzo Villani
     */
    private static class LauncherInfo
    {
        public String packageName;

        public String activity;
    }

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        try
        {
            LauncherInfo launcherInfo;

            launcherInfo = getLauncherActivity();

            launch( launcherInfo );
        }
        catch ( Exception ex )
        {
            throw new MojoFailureException( "Unable to run launcher Activity", ex );
        }
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    /**
     * Gets the first "Launcher" Activity.
     * 
     * @return A {@link LauncherInfo}
     * @throws MojoExecutionException
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     * @throws XPathExpressionException
     * @throws ActivityNotFoundException
     */
    private LauncherInfo getLauncherActivity()
        throws ParserConfigurationException, SAXException, IOException, XPathExpressionException,
        ActivityNotFoundException
    {
        Document document;
        DocumentBuilder documentBuilder;
        DocumentBuilderFactory documentBuilderFactory;
        Object result;
        XPath xPath;
        XPathExpression xPathExpression;
        XPathFactory xPathFactory;

        //
        // Setup JAXP stuff
        //
        documentBuilderFactory = DocumentBuilderFactory.newInstance();

        documentBuilder = documentBuilderFactory.newDocumentBuilder();

        document = documentBuilder.parse( androidManifestFile );

        xPathFactory = XPathFactory.newInstance();

        xPath = xPathFactory.newXPath();

        xPathExpression =
            xPath.compile( "//manifest/application/activity/intent-filter[action[@name=\"android.intent.action.MAIN\"] "
                + "and category[@name=\"android.intent.category.LAUNCHER\"]]/.." );

        //
        // Run XPath query
        //
        result = xPathExpression.evaluate( document, XPathConstants.NODESET );

        if ( result instanceof NodeList )
        {
            NodeList activities;

            activities = (NodeList) result;

            if ( activities.getLength() > 0 )
            {
                // Grab the first declared Activity
                LauncherInfo launcherInfo;

                launcherInfo = new LauncherInfo();
                launcherInfo.activity =
                    activities.item( 0 ).getAttributes().getNamedItem( "android:name" ).getNodeValue();
                launcherInfo.packageName = document.getDocumentElement().getAttribute( "package" ).toString();

                return launcherInfo;
            }
            else
            {
                // If we get here, we couldn't find a launcher activity.
                throw new ActivityNotFoundException();
            }
        }
        else
        {
            // If we get here we couldn't find any Activity
            throw new ActivityNotFoundException();
        }
    }

    /**
     * Executes the "Launcher" activity on configured devices.
     * 
     * @param info
     * @throws MojoFailureException
     * @throws MojoExecutionException
     */
    private void launch( LauncherInfo info )
        throws MojoExecutionException, MojoFailureException
    {
        final String command;

        command = String.format( "am start -n %s/%s", info.packageName, info.activity );

        doWithDevices( new DeviceCallback()
        {
            @Override
            public void doWithDevice( IDevice device )
                throws MojoExecutionException, MojoFailureException
            {
                try
                {
                    device.executeShellCommand( command, new NullOutputReceiver() );
                }
                catch ( IOException ex )
                {
                    throw new MojoFailureException( "Input/Output error", ex );
                }
                catch ( TimeoutException ex )
                {
                    throw new MojoFailureException( "Command timeout", ex );
                }
                catch ( AdbCommandRejectedException ex )
                {
                    throw new MojoFailureException( "ADB rejected the command", ex );
                }
                catch ( ShellCommandUnresponsiveException ex )
                {
                    throw new MojoFailureException( "Unresponsive command", ex );
                }
            }
        } );
    }
}
