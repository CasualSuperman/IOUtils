package com.casualsuperman.utils.IOUtils;

import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;

import static org.junit.Assert.*;

/**
 * Created by Robert on 8/22/2015.
 */
public class ScanReaderTest {
    @Test
    public void findsString() throws IOException {
        assertTrue("Finds info at the beginning of input",
                new ScanReader(new StringReader("Hello world!")).scanPast("Hello"));
        assertTrue("Finds info in the middle of input",
                new ScanReader(new StringReader("<test>data</test>")).scanPast("data"));
    }

    @Test
    public void collectsStrings() throws IOException {
        assertEquals("Collecting works",
                "1234",
                new ScanReader(new StringReader("12345")).collectUntil("5"));
        assertEquals("Collecting works",
                "12345",
                new ScanReader(new StringReader("12345")).collectUntil("5", true));
    }

    @Test
    public void skipWorks() throws IOException {
        ScanReader reader = new ScanReader(new StringReader("Hello world!"));
        reader.skip(1);
        assertFalse("Skipped data can't match",
                reader.scanUntil("Hello"));
    }

    @Test
    public void collectFailsWithNull() throws IOException {
        ScanReader reader = new ScanReader(new StringReader("Hello world!"));
        assertNull("Collecting until a string that never occurs should return null",
                new ScanReader(new StringReader("Hello world!")).collectUntil("forest"));
    }

    @Test
    public void multipleScansSucceed() throws IOException {
        ScanReader reader = new ScanReader(new StringReader("12345"));
        reader.scanUntil("1");
        reader.scanUntil("1");
        assertEquals("Collecting works after scanning",
                "1234", reader.collectUntil("5"));
    }

    @Test
    public void buffersResizeProperly() throws IOException {
        ScanReader reader = new ScanReader(new StringReader("1234567890"), 1);
        reader.scanPast("456");
        assertEquals("78", reader.collectUntil("9"));
    }

    @Test
    public void scanStopsAtFirstMatch() throws IOException {
        ScanReader reader = new ScanReader(new StringReader("1234567891234567891234567891234567890"), 1);
        reader.scanUntil("2");
        assertEquals("2345", reader.collectUntil("5", true));
        reader.scanPast("5");
        assertEquals("67891234567891234567890", reader.collectUntil("0", true));
    }

    @Test(expected=IOException.class)
    public void closeClosesUnderlyingReader() throws IOException {
        StringReader reader1 = new StringReader("test");
        ScanReader reader2 = new ScanReader(reader1);
        reader2.close();
        reader1.read();
    }

    @Test
    public void testCollect() throws IOException {
        assertEquals("12345", new ScanReader(new StringReader("12345")).collect(5));
    }

    @Test
    public void notEnoughDataWorks() throws IOException {
        assertEquals(null, new ScanReader(new StringReader("1234")).collectUntil("5"));
        assertEquals(null, new ScanReader(new StringReader("1234")).collectUntil("12345"));
    }

    @Test
    public void partialFailingMatch() throws IOException {
        assertTrue(new ScanReader(new StringReader("12312345")).scanUntil("1234"));
        assertTrue(new ScanReader(new StringReader("1231212345")).scanUntil("12123"));
    }

    @Test
    public void skipSkips() throws IOException {
        ScanReader reader = new ScanReader(new StringReader("12345"), 1);
        reader.skip(3);
        assertEquals("4", reader.collect(1));
    }

    @Test
    public void skipRemainderSkips() throws IOException {
        ScanReader reader = new ScanReader(new StringReader("12345"), 4);
        reader.collect(1);
        reader.skip(4);
        assertEquals("", reader.collect(0));
    }

    @Test
    public void failingScanPast() throws IOException {
        assertFalse(new ScanReader(new StringReader("12345")).scanPast("6"));
    }
    @Test
    public void scansAfterEndFail() throws IOException {
        ScanReader reader = new ScanReader(new StringReader("12345"));
        reader.skip(5);
        assertFalse(reader.scanPast("3"));
    }

    @Test
    public void matchSpansBufferBoundary() throws IOException {
        ScanReader reader = new ScanReader(new StringReader("123456"), 3);
        assertTrue(reader.scanUntil("234"));
        reader.skip(2);
        assertEquals("456", reader.collect(3));
        reader.close();
        reader = new ScanReader(new StringReader("123456"), 3);
        assertTrue(reader.scanUntil("345"));
        reader.skip(1);
        assertEquals("456", reader.collect(3));
    }
}