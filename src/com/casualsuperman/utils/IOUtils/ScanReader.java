package com.casualsuperman.utils.IOUtils;

import java.io.IOException;
import java.io.Reader;

/**
 * Created by Robert on 8/22/2015.
 */
public class ScanReader {
    private final static int NOT_FOUND = -1;
    private final static int MAYBE_OVERLAP = -2;

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

    public String collectUntil(String needle) throws IOException {
        return collectUntil(needle, false);
    }

    public String collectUntil(String needle, boolean inclusive) throws IOException {
        collector = new StringBuilder();
        boolean found = scanUntil(needle);

        if (!found) {
            return null;
        }

        if (inclusive) {
            drain(needle.length());
        }

        String result = collector.toString();
        collector = null;
        return result;
    }

    public boolean scanUntil(String needle) throws IOException {
        int pos = findInStream(needle);
        if (pos > 0) {
            drain(pos);
        }
        return pos >= 0;
    }

    public boolean scanPast(String needle) throws IOException {
        boolean found = scanUntil(needle);
        if (found) {
            drain(needle.length());
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
            if (read > 0) {
                lastValidByte += read;
            } else if (lastValidByte == 0) {
                lastValidByte = -1;
            }
            return read;
        }
        return 0;
    }

    private int findInStream(String needle) throws IOException {
        final int nLen = needle.length();
        if (buf.length < nLen) {
            int newLen = buf.length;
            while (newLen < nLen) {
                newLen *= 2;
            }
            char[] newBuf = new char[newLen];
            System.arraycopy(buf, 0, newBuf, 0, lastValidByte);
            buf = newBuf;
        }

        refill();

        int found = indexOf(needle);
        boolean done = lastValidByte == -1;

        //Log.v(TAG + "::findInStream", "done? " + done);
        //Log.v(TAG + "::findInStream", "found?" + found);

        while ((found == NOT_FOUND || found == MAYBE_OVERLAP) && !done) {
            if (found == MAYBE_OVERLAP) {
                // Drain everything except the end of the buffer equal to the needle length.
                drain(Math.max(lastValidByte - nLen, 1));
            } else {
                // Drain everything.
                drain(lastValidByte);
            }
            // Refill the gap.
            done = refill() == -1;
            found = indexOf(needle);
            //Log.v(TAG + "::findInStream", "done? " + done);
            //Log.v(TAG + "::findInStream", "found?" + found);
        }

        //Log.v(TAG + "::findInStream", "returning");

        return found;
    }

    private int indexOf(String str) {
        //Log.v(TAG + "::indexOf", "str? " + str);
        final int sLen = str.length();
        final int nextIndex = getRepeatLength(str);
        //Log.v(TAG + "::indexOf", "rLen? " + nextIndex);
        for (int i = 0; i < lastValidByte; i++) {
            if (buf[i] == str.charAt(0)) {
                //Log.v(TAG + "::indexOf", "matched(0): " + buf[i]);
                int j = 1;
                while (j < sLen && j + i < lastValidByte && buf[i + j] == str.charAt(j)) {
                    //Log.v(TAG + "::indexOf", "matched(" + j + "): " + buf[i]);
                    j++;
                }
                if (j == sLen) {
                    //Log.v(TAG + "::indexOf", "full match");
                    //Log.v(TAG + "::indexOf", new String(buf, i, j) + " = " + str);
                    return i;
                }
                if (j + i == lastValidByte) {
                    //Log.v(TAG + "::indexOf", "hit end of buffer");
                    return MAYBE_OVERLAP;
                }
                // If j doesn't match, then don't count it as possible to skip.
                j -=1;
                //Log.v(TAG + "::indexOf", "match failed");
                if (j < nextIndex) {
                    i += j;
                } else {
                    i += nextIndex;
                }
            }
        }

        //Log.v(TAG + "::indexOf", "not found");
        return NOT_FOUND;
    }

    private static int getRepeatLength(String str) {
        final int len = str.length();
        final char first = str.charAt(0);
        int i = 1;
        while (i < len && str.charAt(i) != first) {
            i++;
        }
        return i-1;
    }
}
