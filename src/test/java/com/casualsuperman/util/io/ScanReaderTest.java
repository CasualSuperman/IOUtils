/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.casualsuperman.util.io;

import com.casualsuperman.util.io.ScanReader.Pattern;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Reader;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author Robert Wertman <robert.wertman@gmail.com>
 */
public class ScanReaderTest {
	private final Pattern helloPattern;
    private final Pattern dataPattern;
    private final Pattern fivePattern;
    private final Pattern onePattern;
    private final Pattern ninePattern;
    private final Pattern needle1;
    private final Pattern needle2;
    private final Pattern needle3;

    public ScanReaderTest() {
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
		new ScanReader(new StringReader("Hello world!")).scanPast(helloPattern));
        assertTrue("Finds info in the middle of input",
                new ScanReader(new StringReader("<test>data</test>")).scanPast(dataPattern));
    }

    @Test
    public void collectsStrings() throws IOException {
        assertEquals("Collecting works",
                "1234",
                new ScanReader(new StringReader("12345")).collectUntil(fivePattern));
        assertEquals("Collecting works",
                "12345",
                new ScanReader(new StringReader("12345")).collectPast(fivePattern));
    }

    @Test
    public void skipWorks() throws IOException {
        ScanReader reader = new ScanReader(new StringReader("Hello world!"));
        reader.skip(1);
        assertFalse("Skipped data can't match",
                reader.scanUntil(helloPattern));
    }

    @Test
    public void collectFailsWithNull() throws IOException {
        ScanReader reader = new ScanReader(new StringReader("Hello world!"));
        assertNull("Collecting until a string that never occurs should return null",
                new ScanReader(new StringReader("Hello world!")).collectUntil(new Pattern("forest")));
    }

    @Test
    public void multipleScansSucceed() throws IOException {
        ScanReader reader = new ScanReader(new StringReader("12345"));
        reader.scanUntil(onePattern);
        reader.scanUntil(onePattern);
        assertEquals("Collecting works after scanning",
                "1234", reader.collectUntil(fivePattern));
    }

    @Test
    public void buffersResizeProperly() throws IOException {
        ScanReader reader = new ScanReader(new StringReader("1234567890"), 1);
        reader.scanPast(new Pattern("456"));
        assertEquals("78", reader.collectUntil(ninePattern));
    }

    @Test
    public void scanStopsAtFirstMatch() throws IOException {
        ScanReader reader = new ScanReader(new StringReader("1234567891234567891234567891234567890"), 1);
        reader.scanUntil(new Pattern("2"));
        assertEquals("2345", reader.collectPast(fivePattern));
        reader.scanPast(fivePattern);
        assertEquals("67891234567891234567890", reader.collectPast(new Pattern("0")));
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
        assertEquals(null, new ScanReader(new StringReader("1234")).collectPast(fivePattern));
        assertEquals(null, new ScanReader(new StringReader("1234")).collectPast(new Pattern("12345")));
    }

    @Test
    public void partialFailingMatch() throws IOException {
        assertTrue(new ScanReader(new StringReader("12312345")).scanUntil(new Pattern("1234")));
        assertTrue(new ScanReader(new StringReader("1231212345")).scanUntil(new Pattern("12123")));
        assertTrue(new ScanReader(new StringReader("12312123123")).scanUntil(new Pattern("123123")));
        assertTrue(new ScanReader(new StringReader("12112123123")).scanUntil(new Pattern("12123")));
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
        assertFalse(new ScanReader(new StringReader("12345")).scanPast(new Pattern("6")));
    }
    @Test
    public void scansAfterEndFail() throws IOException {
        ScanReader reader = new ScanReader(new StringReader("12345"));
        reader.skip(5);
        assertFalse(reader.scanPast(new Pattern("3")));
    }

    @Test
    public void matchSpansBufferBoundary() throws IOException {
        ScanReader reader = new ScanReader(new StringReader("123456"), 3);
        assertTrue(reader.scanUntil(new Pattern("234")));
        reader.skip(2);
        assertEquals("456", reader.collect(3));
        reader.close();
        reader = new ScanReader(new StringReader("123456"), 3);
        assertTrue(reader.scanUntil(new Pattern("345")));
        reader.skip(1);
        assertEquals("456", reader.collect(3));
    }

    @Test
    public void testFindInFile() throws IOException {
        ScanReader reader = new ScanReader(new InputStreamReader(getClass().getResourceAsStream("/elem.txt")), 20);
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
		ScanReader reader = new ScanReader(new StringReader(haystack1));

		assertTrue(reader.scanUntil(needle1));
		assertEquals("I have", reader.collect(6));
		assertTrue(reader.scanUntil(needle1));
		assertEquals("I have", reader.collect(6));
		assertEquals(" lost my sight, smell, hearing, taste, and touch: ", reader.collect(50));
	}

	final String haystack2 = "Dry bones can harm no one. "
			+ "Only a cock stood on the rooftree "
			+ "Co co rico co co rico ";
	@Test
	public void repetitiveStringTest() throws IOException {
		ScanReader reader = new ScanReader(new StringReader(haystack2));

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
		ScanReader reader = new ScanReader(new StringReader(haystack3));

		assertTrue(reader.scanUntil(needle3));
		reader.skip(1);
		assertTrue(reader.scanUntil(needle3));
		reader.skip(1);
		assertFalse(reader.scanUntil(needle3));
	}

	@Test
	public void testSlowFindInFile() throws IOException, InterruptedException {
		PipedInputStream pis = new PipedInputStream();
		final PipedOutputStream bos = new PipedOutputStream(pis);
		SlowReader r = new SlowReader(bos, "elem.txt");

		Thread t = new Thread(r);
		t.start();
		ScanReader reader = new ScanReader(new BufferedReader(new InputStreamReader(pis)), 1000);
		assertTrue(reader.scanUntil(new Pattern("function(tagname)")));
		assertTrue(reader.scanUntil(new Pattern("window.Elem = elem")));

		t.join();

		assertTrue(r.worked);
	}
	
	@Test
	public void testRepeatFindInFile() throws IOException, InterruptedException {
		PipedInputStream pis = new PipedInputStream();
		final PipedOutputStream bos = new PipedOutputStream(pis);
		LoopReader r = new LoopReader(bos, "elem.txt");

		Thread t = new Thread(r);
		t.start();
		ScanReader reader = new ScanReader(new BufferedReader(new InputStreamReader(pis)), 1000);
		Pattern decl = new Pattern("function(tagname)");
		Pattern global = new Pattern("window.Elem = elem");
		
		for (int i = 0; i < 10; i++) {
			assertTrue(reader.scanUntil(decl));
			assertTrue(reader.scanUntil(global));
		}

		t.join();

		assertTrue(r.worked);
	}
	
	@Test
	public void testGeneratedStepFailures() throws IOException, InterruptedException {
		final String initial = "1234567890";
		final Pattern search = new Pattern(initial);
		
		for (int i = 0, l = initial.length(); i < l - 1; i++) {
			String toRead = "";
			for (int j = 0; j < i; j++) {
				toRead += initial.charAt(j);
			}
			toRead += "*";
			for (int j = i + 1; j < l; j++) {
				toRead += initial.charAt(j);
			}
			
			toRead += initial;
			
			ScanReader s = new ScanReader(new StringReader(toRead));
			assertTrue(s.scanPast(search));
			boolean readAll = false;
			try
			{
				s.collect(1);
			} catch (StringIndexOutOfBoundsException ex)
			{
				readAll = true;
			}
			assertTrue(readAll);
			s.close();
		}
	}
	
	@Test
	public void testGeneratedStepFailures2() throws IOException, InterruptedException {
		final String initial = "112123123412345";
		final Pattern search = new Pattern(initial);
		
		for (int i = 0, l = initial.length(); i < l - 1; i++) {
			String toRead = "";
			for (int j = 0; j < i; j++) {
				toRead += initial.charAt(j);
			}
			toRead += "*";
			for (int j = i + 1; j < l; j++) {
				toRead += initial.charAt(j);
			}
			
			toRead += initial;
			
			ScanReader s = new ScanReader(new StringReader(toRead));
			assertTrue(s.scanPast(search));
			boolean readAll = false;
			try
			{
				s.collect(1);
			} catch (StringIndexOutOfBoundsException ex)
			{
				readAll = true;
			}
			assertTrue(readAll);
			s.close();
		}
	}
	
	@Test
	public void testGeneratedStepFailures3() throws IOException, InterruptedException {
		final String initial = "543214321321211";
		final Pattern search = new Pattern(initial);
		
		for (int i = 0, l = initial.length(); i < l - 1; i++) {
			String toRead = "";
			for (int j = 0; j < i; j++) {
				toRead += initial.charAt(j);
			}
			toRead += "*";
			for (int j = i + 1; j < l; j++) {
				toRead += initial.charAt(j);
			}
			
			toRead += initial;
			
			ScanReader s = new ScanReader(new StringReader(toRead));
			assertTrue(s.scanPast(search));
			boolean readAll = false;
			try
			{
				s.collect(1);
			} catch (StringIndexOutOfBoundsException ex)
			{
				readAll = true;
			}
			assertTrue(readAll);
			s.close();
		}
	}
	
	@Test
	public void testGeneratedStepFailures4() throws IOException, InterruptedException {
		final String initial = "111112";
		final Pattern search = new Pattern(initial);
		
		for (int i = 0, l = initial.length(); i < l - 1; i++) {
			String toRead = "";
			for (int j = 0; j < i; j++) {
				toRead += initial.charAt(j);
			}
			toRead += "*";
			for (int j = i + 1; j < l; j++) {
				toRead += initial.charAt(j);
			}
			
			toRead += initial;
			
			ScanReader s = new ScanReader(new StringReader(toRead));
			assertTrue(s.scanPast(search));
			boolean readAll = false;
			try
			{
				s.collect(1);
			} catch (StringIndexOutOfBoundsException ex)
			{
				readAll = true;
			}
			assertTrue(readAll);
			s.close();
		}
	}

	private static class SlowReader implements Runnable {
		private final PipedOutputStream pos;
		private final String fileName;
		public boolean worked;

		public SlowReader(PipedOutputStream bos, String fileName) {
			this.pos = bos;
			worked = true;
			this.fileName = fileName;
		}

		@Override
		public void run() {
			try (Reader r = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/"+fileName)))) {
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
	
	private static class LoopReader implements Runnable {
		private final PipedOutputStream pos;
		private final String fileName;
		public boolean worked;

		public LoopReader(PipedOutputStream bos, String fileName) {
			this.pos = bos;
			worked = true;
			this.fileName = fileName;
		}

		@Override
		public void run() {
			try {
				for (int j = 0; j < 10; j++) {
					try (Reader r = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/"+fileName)))) {
						int read;
						while ((read = r.read()) != -1) {
							pos.write(read);
						}
					}
				}
			} catch (Exception e) {
				worked = false;
			}
		}
	}
}
