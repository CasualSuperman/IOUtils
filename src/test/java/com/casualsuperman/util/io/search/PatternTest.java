/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.casualsuperman.util.io.search;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Robert Wertman <robert.wertman@gmail.com>
 */
public class PatternTest
{
	@Test
	public void testSearch()
	{
		final String toSearch1 = "bar is full of barbarians";
		final String pattern1  = "barbarian";
		Pattern p = new Pattern(pattern1);
		byte[] arr = toSearch1.getBytes(ByteBufferScanner.CHARSET);
		
		assertEquals(toSearch1.indexOf(pattern1), p.search(arr, 0, arr.length));
		assertEquals(toSearch1.substring(0, toSearch1.length() - 1).indexOf(pattern1), p.search(arr, 0, arr.length - 1));
		assertEquals(toSearch1.substring(0, toSearch1.length() - 2).indexOf(pattern1), p.search(arr, 0, arr.length - 2));
	}	
}
