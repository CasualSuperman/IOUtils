package com.casualsuperman.util.io;

import java.io.IOException;
import java.io.Reader;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Robert Wertman
 */
public class ScanReader {
	private final static int NOT_FOUND = -1;
	private final static int MAYBE_OVERLAP = -2;
	
	//protected static AtomicInteger comps = new AtomicInteger();
	//protected static AtomicInteger bytesRead = new AtomicInteger();

	private Reader wrapped;
	private char[] buf;
	private int lastValidByte = 0;
	private StringBuilder collector = null;

	public ScanReader(Reader wrapped) {
		this(wrapped, 1024);
	}

	public ScanReader(Reader wrapped, int bufSize) {
		this.buf = new char[bufSize];
		this.wrapped = wrapped;
	}

	public void close() throws IOException {
		wrapped.close();
		wrapped = null;
		buf = null;
	}

	public void skip(int n) throws IOException {
		while (n > lastValidByte) {
			n -= lastValidByte;
			drain(lastValidByte);
			refill();
		}

		if (n > 0) {
			drain(n);
		}
	}

	public String collect(int n) throws IOException {
		collector = new StringBuilder();
		skip(n);
		String result = collector.toString();
		collector = null;
		return result;
	}

	public String collectUntil(Pattern needle) throws IOException {
		return collect(needle, false);
	}

	public String collectPast(Pattern needle) throws IOException {
		return collect(needle, true);
	}

	private String collect(Pattern needle, boolean inclusive) throws IOException {
		collector = new StringBuilder();
		boolean found = scanUntil(needle);

		if (!found) {
			return null;
		}

		if (inclusive) {
			drain(needle.matchOffset.length);
		}

		String result = collector.toString();
		collector = null;
		return result;
	}

	public boolean scanUntil(Pattern needle) throws IOException {
		int pos = findInStream(needle);
		if (pos > 0) {
			drain(pos);
		}
		return pos >= 0;
	}

	public boolean scanPast(Pattern needle) throws IOException {
		boolean found = scanUntil(needle);
		if (found) {
			drain(needle.matchOffset.length);
		}
		return found;
	}

	private void drain(int n) {
		//if ((n > lastValidByte)) throw new AssertionError();
		if (collector != null) {
			collector.append(new String(buf, 0, n));
		}
		System.arraycopy(buf, n, buf, 0, lastValidByte - n);
		lastValidByte -= n;
	}

	private int refill() throws IOException {
		if (lastValidByte < buf.length) {
			int read = wrapped.read(buf, lastValidByte, buf.length - lastValidByte);
			//Log.v(TAG + "::refill", new String(buf, lastValidByte, read));
			if (read >= 0) {
				//bytesRead.addAndGet(read);
				lastValidByte += read;
			} else if (lastValidByte == 0) {
				lastValidByte = -1;
			}
			return read;
		}
		return 0;
	}

	private int findInStream(Pattern needle) throws IOException {
		final int nLen = needle.matchOffset.length;
		if (buf.length < nLen) {
			int newLen = buf.length;
			while (newLen < nLen) {
				newLen *= 2;
			}
			char[] newBuf = new char[newLen];
			System.arraycopy(buf, 0, newBuf, 0, lastValidByte);
			buf = newBuf;
		}

		if (lastValidByte < nLen) {
			refill();
		}

		int found = indexOf(needle);
		boolean done = lastValidByte == -1;

		while ((found == NOT_FOUND || found == MAYBE_OVERLAP) && !done) {
			if (found == MAYBE_OVERLAP) {
				// Drain everything except the end of the buffer equal to the needle length.
				drain(Math.max(lastValidByte - nLen, 1));
			} else {
				// Drain everything.
				drain(lastValidByte);
			}
			// Refill the gap.
			if (lastValidByte < nLen) {
				done = refill() == -1;
			}

			found = indexOf(needle);
		}

		return found;
	}

	private int indexOf(Pattern str) {
		//Log.v(TAG + "::indexOf", "str? " + str);
		final int sLen = str.matchOffset.length;
		//Log.v(TAG + "::indexOf", "rLen? " + nextIndex);

		int i = 0;
		while (i < lastValidByte) {
			//comps.incrementAndGet();
			if (buf[i] == str.original[0]) {
				//Log.v(TAG + "::indexOf", "matched(0): " + buf[i]);
				int j = 1;
				boolean matching = true;
				while (matching && j < sLen && j + i < lastValidByte) {
					//comps.incrementAndGet();
					//Log.v(TAG + "::indexOf", "matched(" + j + "): " + buf[i]);
					if (buf[i + j] == str.original[j]) {
						j++;
					} else if (j > 1 && str.matchOffset[j] > 0) {
						i += str.matchOffset[j];
						j = str.matchOffset[j];
						//j = 1;
					} else {
						matching = false;
						if (j == 1) {
							i += str.skip0;
						} else {
							i += sLen - j + 2;
						}
					}
				}
				if (matching) {
					if (j == sLen) {
						//Log.v(TAG + "::indexOf", "full match");
						//Log.v(TAG + "::indexOf", new String(buf, i, j) + " = " + str);
						return i;
					}
					return MAYBE_OVERLAP;
				}
			} else {
				i++;
			}
		}

		return NOT_FOUND;
	}

	private static int getRepeatLength(String str, final char first) {
		final int len = str.length();
		int i = len - 1;
		while (i >= 0 && str.charAt(i) != first) {
			i--;
		}
		return len - i - 1;
	}

	public static class Pattern {
		protected final char[] original;
		protected final int[]  matchOffset;
		protected final int    skip0;

		public Pattern(String p) {
			this.original    = p.toCharArray();
			this.matchOffset = new int[original.length];
			this.skip0       = getRepeatLength(p, original[0]);

			int a = 0;

			this.matchOffset[0] = 0;

			for (int b = 1; b < matchOffset.length; b++) {
				final char c = original[b];
				
				if (c == original[a]) {
					a++;
					matchOffset[b] = a;
				} else if (a != 0) {
					a = matchOffset[a - 1];
					b--;
				} else {
					matchOffset[b] = a;
				}
			}
		}
	}
}
