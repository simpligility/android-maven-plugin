package com.jayway.maven.plugins.android.standalonemojos;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.jayway.maven.plugins.android.AbstractAndroidMojo;

/**
 * Writes the project version into the <code>AndroidManifest.xml</code> on projects that have
 * <code>&lt;packaging&gt;apk&lt;/packaging&gt;</code>
 * <p>
 * Note: This process might reformat the <code>AndroidManifest.xml</code> per JAXP {@link Transformer} defaults.
 * 
 * @author joakim@erdfelt.com
 * @goal version-update
 * @requiresProject true
 * @phase prepare-resources
 */
public class VersionUpdateMojo extends AbstractAndroidMojo {

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!"apk".equals(project.getPackaging())) {
            return; // skip, not an android project.
        }

        if (!androidManifestFile.exists()) {
            return; // skip, no AndroidManifest.xml file found.
        }

        String version = project.getVersion();
        try {
            updateManifest(androidManifestFile, version);
        } catch (IOException e) {
            throw new MojoFailureException("XML I/O error: " + androidManifestFile, e);
        } catch (ParserConfigurationException e) {
            throw new MojoFailureException("Unable to prepare XML parser", e);
        } catch (SAXException e) {
            throw new MojoFailureException("Unable to parse XML: " + androidManifestFile, e);
        } catch (TransformerException e) {
            throw new MojoFailureException("Unable write XML: " + androidManifestFile, e);
        }
    }

    /**
     * Read manifest using JAXP
     */
    private Document readManifest(File manifestFile) throws IOException, ParserConfigurationException, SAXException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(manifestFile);
        return doc;
    }

    /**
     * Write manifest using JAXP transformer
     */
    private void writeManifest(File manifestFile, Document doc) throws IOException, TransformerException {
        TransformerFactory xfactory = TransformerFactory.newInstance();
        Transformer xformer = xfactory.newTransformer();
        xformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        Source source = new DOMSource(doc);

        FileWriter writer = null;
        try {
            writer = new FileWriter(manifestFile, false);
            String xmldecl = String.format("<?xml version=\"%s\" encoding=\"%s\"?>%n", doc.getXmlVersion(),
                    doc.getXmlEncoding());
            writer.write(xmldecl);
            Result result = new StreamResult(writer);

            xformer.transform(source, result);
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }

    public void updateManifest(File manifestFile, String version) throws IOException, ParserConfigurationException,
            SAXException, TransformerException {
        Document doc = readManifest(manifestFile);

        Element root = doc.getDocumentElement();
        Attr versionName = root.getAttributeNode("android:versionName");

        if ((versionName == null) || (StringUtils.equals(version, versionName.getValue()) == false)) {
            root.setAttribute("android:versionName", version);
            writeManifest(manifestFile, doc);
        }
    }
}
