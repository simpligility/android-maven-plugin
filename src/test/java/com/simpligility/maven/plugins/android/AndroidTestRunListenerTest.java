package com.simpligility.maven.plugins.android;

import com.android.ddmlib.IDevice;
import com.android.ddmlib.testrunner.ITestRunListener;
import com.android.ddmlib.testrunner.TestIdentifier;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.util.Collections;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(EasyMockRunner.class)
public class AndroidTestRunListenerTest
{

    @Mock
    private IDevice device;

    private final String suffix = RandomStringUtils.randomAlphabetic( 10 );
    private final String serial = RandomStringUtils.randomAlphabetic( 10 );
    private final String avd = RandomStringUtils.randomAlphabetic( 10 );
    private final String manufacturer = RandomStringUtils.randomAlphabetic( 10 );
    private final String model = RandomStringUtils.randomAlphabetic( 10 );
    private final String runName = RandomStringUtils.randomAlphabetic( 10 );
    private final String key = RandomStringUtils.randomAlphabetic( 10 );
    private final String value = RandomStringUtils.randomAlphabetic( 10 );
    private final int count = RandomUtils.nextInt( 1, 10 );
    private final int elapsed = RandomUtils.nextInt( 10, 1000 );

    @Rule
    public TemporaryFolder target = new TemporaryFolder();

    @Before
    public void setUp() throws Exception
    {
        expect( device.getSerialNumber() ).andReturn( serial ).anyTimes();
        expect( device.getAvdName() ).andReturn( avd ).anyTimes();
        expect( device.getProperty( "ro.product.manufacturer" ) ).andReturn( manufacturer ).anyTimes();
        expect( device.getProperty( "ro.product.model" ) ).andReturn( model ).anyTimes();
        expect( device.getProperties() ).andReturn( Collections.singletonMap( key, value ) ).anyTimes();
    }

    private String randomTrace() {
        return RandomStringUtils.randomAlphabetic( 20 ) + ":" + RandomStringUtils.randomAlphabetic( 20 );
    }

    @Test
    public void validReport()
    {
        replay( device );

        final ITestRunListener listener = new AndroidTestRunListener( device, new SystemStreamLog(), true, false, null, suffix, target.getRoot() );
        listener.testRunStarted( runName, count );

        final int tests = RandomUtils.nextInt( 5, 10 );
        for (int i = 0; i < tests; i++)
        {
            final TestIdentifier id = new TestIdentifier( RandomStringUtils.randomAlphabetic( 20 ), RandomStringUtils.randomAlphabetic( 10 ) );
            listener.testStarted( id );
            switch (RandomUtils.nextInt( 0, 4 ))
            {
                case 0:
                    listener.testFailed( id,  randomTrace());
                    break;
                case 1:
                    listener.testAssumptionFailure( id, randomTrace() );
                    break;
                case 2:
                    listener.testIgnored( id );
            }
            listener.testEnded( id, Collections.<String, String>emptyMap() );
        }

        if (RandomUtils.nextInt( 0, 1 ) == 1)
        {
            listener.testRunFailed( RandomStringUtils.randomAlphabetic( 20 ) );
        }
        listener.testRunEnded( elapsed, Collections.<String, String>emptyMap() );

        verify( device );

        assertEquals( 1, target.getRoot().listFiles().length );
        assertEquals( 1, target.getRoot().listFiles()[0].listFiles().length );
        for (File file : target.getRoot().listFiles()[0].listFiles())
        {
            assertTrue( validateXMLSchema( "surefire/surefire-test-report.xsd", file ) );
        }
    }

    public boolean validateXMLSchema( String xsdResource, File xmlFile )
    {
        try
        {
            final SchemaFactory factory = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
            final Schema schema = factory.newSchema( new StreamSource( getClass().getClassLoader().getResourceAsStream( xsdResource ) ) );
            final Validator validator = schema.newValidator();
            validator.validate( new StreamSource( xmlFile ) );
        } catch (IOException | SAXException e)
        {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}