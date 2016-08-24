/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.casualsuperman.util.io.search;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 *
 * @author Robert Wertman <robert.wertman@gmail.com>
 */
public class ByteBufferScanner implements AutoCloseable
{
	public static final Charset CHARSET = StandardCharsets.UTF_8;
	
	private ByteBuffer wrapped;
	private byte[]     buf;
	private int        lastValidByte = 0;
	private int        currentOffset = 0;
	private byte[]     collector     = null;
	private int        collectorLen  = 0;
	
	public ByteBufferScanner(ByteBuffer b)
	{
		this(b, 4096);
	}
	
	public ByteBufferScanner(ByteBuffer b, int bufSize)
	{
		this.wrapped = b;
		this.buf     = new byte[bufSize];
	}
	
	public String collectPast(Pattern needle)
	{
		return collect(needle, true);
	}
	
    public String collectUntil(Pattern needle)
	{
		return collect(needle, false);
	}
	
	private String collect(Pattern needle, boolean inclusive)
	{
		refill(needle.original.length);
		this.collector = new byte[128];
		this.collectorLen = 0;
		
		int index = needle.search(buf, currentOffset, lastValidByte);
		while (index == Pattern.NOT_FOUND && wrapped.hasRemaining())
		{
			collect(lastValidByte - currentOffset - needle.original.length);
			index = needle.search(buf, currentOffset, lastValidByte);
		}
		
		if (index == Pattern.NOT_FOUND)
		{
			throw new ArrayIndexOutOfBoundsException("buffer empty");
		}
		collect(index - this.currentOffset + (inclusive ? needle.original.length : 0));
		
		return new String(this.collector, 0, this.collectorLen, CHARSET);
	}
	
	public void collect(int amt)
	{
		while (amt > 0)
		{
			int n = Math.min(amt, lastValidByte - currentOffset);
			if (this.collectorLen + n > this.collector.length)
			{
				int newLen = this.collector.length * 2;
				while (newLen < this.collectorLen + n)
				{
					newLen *= 2;
				}
				this.collector = Arrays.copyOf(collector, newLen);
			}
			System.arraycopy(buf, currentOffset, collector, collectorLen, n);
			skip(n);
			amt -= n;
		}
	}
	
	public void skip(int amt)
	{
		while (amt > 0)
		{
			int n = Math.min(amt, lastValidByte - currentOffset);
			System.arraycopy(buf, currentOffset, buf, 0, n);
			this.lastValidByte = n;
			this.currentOffset = 0;
			
			int amtToAdd = Math.min(buf.length - lastValidByte, this.wrapped.remaining());
			if (amtToAdd > 0)
			{
				this.wrapped.get(buf, lastValidByte, amtToAdd);
				this.lastValidByte += amtToAdd;
			}
			if (n == 0 && amtToAdd == 0)
			{
				throw new ArrayIndexOutOfBoundsException("out of buffer");
			}
			amt -= n;
		}
	}

    public boolean scanPast(Pattern needle)
	{
		if (scanUntil(needle))
		{
			skip(needle.original.length);
			return true;
		}
		return false;
	}

    public boolean scanUntil(Pattern needle)
	{
		refill(needle.original.length);
		int index = needle.search(buf, currentOffset, lastValidByte);
		while (index == Pattern.NOT_FOUND && wrapped.hasRemaining())
		{
			skip(lastValidByte - currentOffset - needle.original.length);
			index = needle.search(buf, currentOffset, lastValidByte);
		}
		
		if (index == Pattern.NOT_FOUND)
		{
			return false;
		}
		skip(index - this.currentOffset);
		return true;
	}

	private void refill(int min)
	{
		if (lastValidByte - currentOffset < min)
		{
			int amtToAdd = Math.min(buf.length - lastValidByte, this.wrapped.remaining());
			if (amtToAdd > min)
			{
				this.wrapped.get(buf, lastValidByte, amtToAdd);
				this.lastValidByte += amtToAdd;
			} else
			{
				throw new ArrayIndexOutOfBoundsException();
			}
		}
	}
	
	@Override
	public void close()
	{
		this.buf = null;
		this.wrapped = null;
	}
}
