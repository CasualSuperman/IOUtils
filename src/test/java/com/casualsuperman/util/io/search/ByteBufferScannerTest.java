/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.casualsuperman.util.io.search;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Rule;
import org.junit.rules.Timeout;

/**
 *
 * @author rober
 */
public class ByteBufferScannerTest
{
    private final Pattern helloPattern;
    private final Pattern dataPattern;
    private final Pattern fivePattern;
    private final Pattern onePattern;
    private final Pattern ninePattern;
    private final Pattern needle1;
    private final Pattern needle2;
    private final Pattern needle3;

	@Rule
	public Timeout globalTimeout = Timeout.seconds(2);
	
    public ByteBufferScannerTest()
	{
		helloPattern = new Pattern("Hello");
		dataPattern = new Pattern("data");
		fivePattern = new Pattern("5");
		onePattern = new Pattern("1");
		ninePattern = new Pattern("9");
		needle1 = new Pattern("I have");
		needle2 = new Pattern("co ");
		needle3 = new Pattern("12345");
    }
    
    
    
    @Test
    public void findsString() throws IOException {
        assertTrue("Finds info at the beginning of input",
		new ByteBufferScanner(ByteBuffer.wrap("Hello world!".getBytes(ByteBufferScanner.CHARSET))).scanPast(helloPattern));
        assertTrue("Finds info in the middle of input",
                new ByteBufferScanner(ByteBuffer.wrap("<test>data</test>".getBytes(ByteBufferScanner.CHARSET))).scanPast(dataPattern));
    }

    @Test
    public void collectsStrings() throws IOException {
        assertEquals("Collecting works",
                "1234",
                new ByteBufferScanner(ByteBuffer.wrap("12345".getBytes(ByteBufferScanner.CHARSET))).collectUntil(fivePattern));
        assertEquals("Collecting works",
                "12345",
                new ByteBufferScanner(ByteBuffer.wrap("12345".getBytes(ByteBufferScanner.CHARSET))).collectPast(fivePattern));
    }

    @Test
    public void skipWorks() throws IOException {
        ByteBufferScanner reader = new ByteBufferScanner(ByteBuffer.wrap("Hello world!".getBytes(ByteBufferScanner.CHARSET)));
        reader.skip(1);
        assertFalse("Skipped data can't match",
                reader.scanUntil(helloPattern));
    }

    @Test
    public void collectFailsWithNull() throws IOException {
        ByteBufferScanner reader = new ByteBufferScanner(ByteBuffer.wrap("Hello world!".getBytes(ByteBufferScanner.CHARSET)));
        assertNull("Collecting until a string that never occurs should return null",
                new ByteBufferScanner(ByteBuffer.wrap("Hello world!".getBytes(ByteBufferScanner.CHARSET))).collectUntil(new Pattern("forest")));
    }

    @Test
    public void multipleScansSucceed() throws IOException {
        ByteBufferScanner reader = new ByteBufferScanner(ByteBuffer.wrap("12345".getBytes(ByteBufferScanner.CHARSET)));
        reader.scanUntil(onePattern);
        reader.scanUntil(onePattern);
        assertEquals("Collecting works after scanning",
                "1234", reader.collectUntil(fivePattern));
    }

    @Test
    public void buffersResizeProperly() throws IOException {
        ByteBufferScanner reader = new ByteBufferScanner(ByteBuffer.wrap("1234567890".getBytes(ByteBufferScanner.CHARSET)), 1);
        reader.scanPast(new Pattern("456"));
        assertEquals("78", reader.collectUntil(ninePattern));
    }

    @Test
    public void scanStopsAtFirstMatch() throws IOException {
        ByteBufferScanner reader = new ByteBufferScanner(ByteBuffer.wrap("1234567891234567891234567891234567890".getBytes(ByteBufferScanner.CHARSET)), 1);
        reader.scanUntil(new Pattern("2"));
        assertEquals("2345", reader.collectPast(fivePattern));
        reader.scanPast(fivePattern);
        assertEquals("67891234567891234567890", reader.collectPast(new Pattern("0")));
    }

    @Test
    public void notEnoughDataWorks() throws IOException {
        assertEquals(null, new ByteBufferScanner(ByteBuffer.wrap("1234".getBytes(ByteBufferScanner.CHARSET))).collectPast(fivePattern));
        assertEquals(null, new ByteBufferScanner(ByteBuffer.wrap("1234".getBytes(ByteBufferScanner.CHARSET))).collectPast(new Pattern("12345")));
    }

    @Test
    public void partialFailingMatch() throws IOException {
        assertTrue(new ByteBufferScanner(ByteBuffer.wrap("12312345".getBytes(ByteBufferScanner.CHARSET))).scanUntil(new Pattern("1234")));
        assertTrue(new ByteBufferScanner(ByteBuffer.wrap("1231212345".getBytes(ByteBufferScanner.CHARSET))).scanUntil(new Pattern("12123")));
        assertTrue(new ByteBufferScanner(ByteBuffer.wrap("12312123123".getBytes(ByteBufferScanner.CHARSET))).scanUntil(new Pattern("123123")));
        assertTrue(new ByteBufferScanner(ByteBuffer.wrap("12112123123".getBytes(ByteBufferScanner.CHARSET))).scanUntil(new Pattern("12123")));
    }

    @Test
    public void skipSkips() throws IOException {
        ByteBufferScanner reader = new ByteBufferScanner(ByteBuffer.wrap("12345".getBytes(ByteBufferScanner.CHARSET)), 1);
        reader.skip(3);
        assertEquals("4", reader.collectPast(new Pattern("4")));
    }

    @Test
    public void skipRemainderSkips() throws IOException {
        ByteBufferScanner reader = new ByteBufferScanner(ByteBuffer.wrap("12345".getBytes(ByteBufferScanner.CHARSET)), 4);
        reader.collect(1);
        reader.skip(4);
    }

    @Test
    public void failingScanPast() throws IOException {
        assertFalse(new ByteBufferScanner(ByteBuffer.wrap("12345".getBytes(ByteBufferScanner.CHARSET))).scanPast(new Pattern("6")));
    }
    @Test
    public void scansAfterEndFail() throws IOException {
        ByteBufferScanner reader = new ByteBufferScanner(ByteBuffer.wrap("12345".getBytes(ByteBufferScanner.CHARSET)));
        reader.skip(5);
        assertFalse(reader.scanPast(new Pattern("3")));
    }

    @Test
    public void matchSpansBufferBoundary() throws IOException {
        ByteBufferScanner reader = new ByteBufferScanner(ByteBuffer.wrap("123456".getBytes(ByteBufferScanner.CHARSET)), 3);
        assertTrue(reader.scanUntil(new Pattern("234")));
        reader.skip(2);
        assertEquals("456", reader.collectPast(new Pattern("6")));
        reader.close();
        reader = new ByteBufferScanner(ByteBuffer.wrap("123456".getBytes(ByteBufferScanner.CHARSET)), 3);
        assertTrue(reader.scanUntil(new Pattern("345")));
        reader.skip(1);
        assertEquals("456", reader.collectPast(new Pattern("6")));
    }

	public ByteBuffer getBuffer(String fileName) throws IOException
	{
		RandomAccessFile f = new RandomAccessFile(fileName, "r");
		return f.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, f.length());
	}
	
    @Test
    public void testFindInFile() throws IOException {
        ByteBufferScanner reader = new ByteBufferScanner(getBuffer("resources/elem.txt"), 20);
        assertTrue(reader.scanUntil(new Pattern("function(tagname)")));
        assertTrue(reader.scanUntil(new Pattern("window.Elem = elem")));
    }

	final String haystack1 = "I that was near your heart was removed therefrom "
			+ "To lose beauty in terror, terror in inquisition. "
			+ "I have lost my passion: why should I need to keep it "
			+ "Since what is kept must be adulterated? "
			+ "I have lost my sight, smell, hearing, taste, and touch: "
			+ "How should I use them for your closer contact?";

	@Test
	public void longStringTest() throws IOException {
		ByteBufferScanner reader = new ByteBufferScanner(ByteBuffer.wrap(haystack1.getBytes(ByteBufferScanner.CHARSET)));

		assertTrue(reader.scanUntil(needle1));
		assertEquals("I have", reader.collectPast(new Pattern("e")));
		assertTrue(reader.scanUntil(needle1));
		assertEquals("I have", reader.collectPast(new Pattern("e")));
		assertEquals(" lost my sight, smell, hearing, taste, and touch: ", reader.collectPast(new Pattern(": ")));
	}

	final String haystack2 = "Dry bones can harm no one. "
			+ "Only a cock stood on the rooftree "
			+ "Co co rico co co rico ";
	@Test
	public void repetitiveStringTest() throws IOException {
		ByteBufferScanner reader = new ByteBufferScanner(ByteBuffer.wrap(haystack2.getBytes(ByteBufferScanner.CHARSET)));

		assertTrue(reader.scanUntil(needle2));
		reader.skip(1);
		assertTrue(reader.scanUntil(needle2));
		reader.skip(1);
		assertTrue(reader.scanUntil(needle2));
		reader.skip(1);
		assertTrue(reader.scanUntil(needle2));
		reader.skip(1);
		assertTrue(reader.scanUntil(needle2));
		reader.skip(1);
		assertFalse(reader.scanUntil(needle2));
	}

	final String haystack3 = "1212312341234543213434213432123432214234231423343234314423124231433443231423212345";
	@Test
	public void testOverlappingPartialMatches() throws IOException {
		ByteBufferScanner reader = new ByteBufferScanner(ByteBuffer.wrap(haystack3.getBytes(ByteBufferScanner.CHARSET)));

		assertTrue(reader.scanUntil(needle3));
		reader.skip(1);
		assertTrue(reader.scanUntil(needle3));
		reader.skip(1);
		assertFalse(reader.scanUntil(needle3));
	}

	@Test
	public void testRepeatingCharacters() throws IOException {
		ByteBufferScanner reader = new ByteBufferScanner(ByteBuffer.wrap("AAAAAAAB".getBytes(ByteBufferScanner.CHARSET)));
		Pattern needle = new Pattern("AAAAAAB");
		
		assertTrue(reader.scanPast(needle));
		try
		{
			reader.skip(1);
			assertTrue(false);
		} catch (Exception e)
		{ }
	}
	
	@Test
	public void testInternet() throws IOException {
		ByteBufferScanner reader = new ByteBufferScanner(getBuffer("resources/music.txt"));

		assertTrue(reader.scanPast(new Pattern("Gay Deer World Takeover")));
		assertTrue(reader.scanPast(new Pattern("The Missed Symphony")));
		assertTrue(reader.scanPast(new Pattern("Tourist History")));
		assertTrue(reader.scanPast(new Pattern("#STUPiDFACEDD")));
		assertTrue(reader.scanPast(new Pattern("Relativity 2")));
		assertTrue(reader.scanPast(new Pattern("Electric Light Orchestra")));
		assertTrue(reader.scanPast(new Pattern("02-It's Over.mp3")));
	}

	private static class SlowReader implements Runnable {
		private final PipedOutputStream pos;
		public boolean worked;

		public SlowReader(PipedOutputStream bos) {
			this.pos = bos;
			worked = true;
		}

		public void run() {
			try {
				FileReader r = new FileReader("resources/elem.txt");
				int read;
				int i = 0;
				while ((read = r.read()) != -1) {
					pos.write(read);
					i++;

					if (i == 50) {
						Thread.sleep(10);
						i = 0;
					}
				}
			} catch (Exception e) {
				worked = false;
			}
		}
	}
}
