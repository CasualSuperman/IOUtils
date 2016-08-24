/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.casualsuperman.util.io.search;

import java.util.Arrays;

/**
 *
 * @author Robert Wertman <robert.wertman@gmail.com>
 */
public class Pattern
{
	public static final int NOT_FOUND     = -1;
	
	public final byte[] original;
	public final int[]  skipArray; // Boyer-Moore
	public final byte   minChar;
	public final byte   maxChar;

	public Pattern(String pat)
	{
		this.original    = pat.getBytes(ByteBufferScanner.CHARSET);
		byte min = this.original[0];
		byte max = this.original[0];
		
		int a = 0;
		
		for (int i = 1; i < this.original.length; i++)
		{
			final byte c = this.original[i];
			max = (byte) Math.max(max, c);
			min = (byte) Math.min(min, c);
		}
		
		this.minChar = min;
		this.maxChar = max;
		this.skipArray = new int[max - min + 1];
		Arrays.fill(this.skipArray, this.original.length);
		
		for (int i = 0; i < this.original.length; i++)
		{
			this.skipArray[this.original[i] - min] = this.original.length - i - 1;
		}
	}
	
	private int comps = 0;
	
	public int getComps()
	{
		int temp = comps;
		this.comps = 0;
		return temp;
	}
	
	public int search(byte[] buf, int start, int end)
	{
		final int lenM = this.original.length - 1;
		int i = start;
		
		final int endM = end - lenM;
		
		while (i < endM)
		{
			int jmpAmt;
			do
			{
				comps++;
				jmpAmt = getJump(buf[i + lenM]);
				i += jmpAmt;
			} while (jmpAmt > 0 && i < endM);
			
			if (jmpAmt == 0)
			{
				for (int j = lenM - 1; jmpAmt == 0 && j >= 0; j--)
				{
					comps++;
					if (this.original[j] != buf[i + j])
					{
						jmpAmt = getJump(buf[i + j]);
					}
				}
				if (jmpAmt == 0)
				{
					return i;
				}
				i += jmpAmt;
			}
		}
		return NOT_FOUND;
	}
	
	private int getJump(byte b)
	{
		if (b < minChar || b > maxChar)
		{
			return this.original.length;
		} else
		{
			return this.skipArray[b - minChar];
		}
	}
}
