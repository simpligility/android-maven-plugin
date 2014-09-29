package com.simpligility.android.morse;

import junit.framework.TestCase;
import org.junit.Assert;

/**
 * MorseCodeConverterTest is the unit test suite for {@link com.simpligility.android.morse.MorseCodeConverter}.
 *
* @author Manfred Moser <manfred@simpligility.com>
 */
public class MorseCodeConverterTest extends TestCase {

    /**
     * Test the timing parameters for signals.
     */
    public void testSetup()
    {
        Assert.assertEquals(MorseCodeConverter.GAP, 100);
        Assert.assertEquals(MorseCodeConverter.DASH, 300);
        Assert.assertEquals(MorseCodeConverter.DOT, 100);
    }

    /**
     * Test the string "SOS".
     */
    public void testSOS()
    {
        long[] sosArrayExpected = new long[] {0,100,100,100,100,100,300,300,100,300,100,300,300,100,100,100,100,100,0};
        long[] actual = MorseCodeConverter.pattern("SOS");
        Assert.assertArrayEquals(sosArrayExpected, actual);
    }

    public void testCaseSensitivity()
    {
        Assert.assertArrayEquals(MorseCodeConverter.pattern("sos"), MorseCodeConverter.pattern("SOS"));
        Assert.assertArrayEquals(MorseCodeConverter.pattern("sOs"), MorseCodeConverter.pattern("SOS"));
    }

    public void testSomeNumbers()
    {
        long[] expected = new long[]{0,100,100,300,100,300,100,300,100,300,300,100,100,100,100,300,100,300,100,300,300,
                100,100,100,100,100,100,300,100,300,0};
        long[] actual = MorseCodeConverter.pattern("123");
        Assert.assertArrayEquals(expected, actual);
    }

    public void testWhitespaceTreatment()
    {
        long[] expected = new long[]{0,100,100,100,100,100,100,100,700,100,100,300,100,300,0};
        long[] actual = MorseCodeConverter.pattern("H W");
        Assert.assertArrayEquals(expected, actual);
    }

    public void testChars()
    {
        Assert.assertArrayEquals(new long[] {100,100,300}, MorseCodeConverter.pattern('A'));

        Assert.assertArrayEquals(new long[] {300,100,300}, MorseCodeConverter.pattern('m'));

        Assert.assertArrayEquals(new long[] {300,100,300}, MorseCodeConverter.pattern('M'));

        Assert.assertArrayEquals(new long[] {100}, MorseCodeConverter.pattern(' '));

        Assert.assertArrayEquals(new long[] {100,100,100,100,100,100,100,100,300}, MorseCodeConverter.pattern('4'));

        Assert.assertArrayEquals(new long[] {100}, MorseCodeConverter.pattern('?'));
    }
}
