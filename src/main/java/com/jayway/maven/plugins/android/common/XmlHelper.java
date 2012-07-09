package com.jayway.maven.plugins.android.common;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XmlHelper
{

    public static void removeDirectChildren(Node parent)
    {
        NodeList childNodes = parent.getChildNodes();
        while ( childNodes.getLength() > 0 )
        {
            parent.removeChild( childNodes.item( 0 ) );
        }
    }

    public static Element getOrCreateElement(Document doc, Element manifestElement, String elementName)
    {
        NodeList nodeList = manifestElement.getElementsByTagName( elementName );
        Element element = null;
        if ( nodeList.getLength() == 0 )
        {
            element = doc.createElement( elementName );
            manifestElement.appendChild( element );
        } else
        {
            element = (Element) nodeList.item( 0 );
        }
        return element;
    }
}
