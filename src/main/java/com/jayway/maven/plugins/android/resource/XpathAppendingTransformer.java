package com.jayway.maven.plugins.android.resource;

import org.apache.maven.plugins.shade.relocation.Relocator;
import org.apache.maven.plugins.shade.resource.ResourceTransformer;
import org.jdom.Attribute;
import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static java.lang.String.format;

/**
 * Combines multiple occurrences of some XML file
 * by appending contents of specified elements.
 */
public class XpathAppendingTransformer implements ResourceTransformer
{
    public static final String XSI_NS = "http://www.w3.org/2001/XMLSchema-instance";

    boolean ignoreDtd = true;

    String resource;

    /** XPATH expression selecting elements */
    String[] elements;

    Document doc;

    public boolean canTransformResource( String r )
    {
        if ( resource != null && resource.equalsIgnoreCase( r ) )
        {
            return true;
        }

        return false;
    }

    public void processResource( String resource, InputStream is, List<Relocator> relocators )
            throws IOException
    {
        Document r;
        try
        {
            SAXBuilder builder = new SAXBuilder( false );
            builder.setExpandEntities( false );
            if ( ignoreDtd )
            {
                builder.setEntityResolver( new EntityResolver()
                {
                    public InputSource resolveEntity( String publicId, String systemId )
                            throws SAXException, IOException
                    {
                        return new InputSource( new StringReader( "" ) );
                    }
                } );
            }
            r = builder.build( is );
        }
        catch ( JDOMException e )
        {
            throw new RuntimeException( "Error processing resource " + resource + ": " + e.getMessage(), e );
        }

        if ( doc == null )
        {
            doc = r;
        }
        else if ( elements == null || elements.length == 0 )
        {
            appendElement( r.getRootElement(), doc.getRootElement() );
        }
        else
        {
            for ( String xpath : elements )
            {
                try
                {
                    XPath path = XPath.newInstance( xpath );
                    Object source = path.selectSingleNode( r.getRootElement() );
                    if ( !( source instanceof Element ) )
                    {
                        throw new IOException( format( "xpath result must be element. %s returned %s",
                                xpath, source ) );
                    }
                    Object target = path.selectSingleNode( doc.getRootElement() );
                    if ( !( target instanceof Element ) )
                    {
                        throw new IOException( format( "xpath result must be element. %s returned %s",
                                xpath, target ) );
                    }
                    appendElement( (Element) source, (Element) target );
                }
                catch ( JDOMException e )
                {
                    throw new IOException( e );
                }
            }
        }
    }

    @SuppressWarnings( "unchecked" )
    private void appendElement( Element source, Element target )
    {
        for ( Iterator<Attribute> itr = source.getAttributes().iterator(); itr.hasNext(); )
        {
            Attribute a = itr.next();
            itr.remove();

            Attribute mergedAtt = target.getAttribute( a.getName(), a.getNamespace() );
            if ( mergedAtt == null )
            {
                target.setAttribute( a );
            }
        }

        for ( Iterator<Content> itr = source.getChildren().iterator(); itr.hasNext(); )
        {
            Content n = itr.next();
            itr.remove();

            target.addContent( n );
        }
    }

    public boolean hasTransformedResource()
    {
        return doc != null;
    }

    public void modifyOutputStream( JarOutputStream jos )
            throws IOException
    {
        jos.putNextEntry( new JarEntry( resource ) );

        new XMLOutputter( Format.getPrettyFormat() ).output( doc, jos );

        doc = null;
    }
}