package oly.netpowerctrl.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class Streams {
    private static final int BUF_SIZE = 0x1000; // 4K

    public static long copy(InputStream from, OutputStream to)
            throws IOException {
        byte[] buf = new byte[BUF_SIZE];
        long total = 0;
        while (true) {
            int r = from.read(buf);
            if (r == -1) {
                break;
            }
            to.write(buf, 0, r);
            total += r;
        }
        return total;
    }

    public static void splitNonRegex(List<String> result, String input, String delim) {
        int offset = 0;

        while (true) {
            int index = input.indexOf(delim, offset);
            if (index == -1) {
                result.add(input.substring(offset));
                return;
            } else {
                result.add(input.substring(offset, index));
                offset = (index + delim.length());
            }
        }
    }
}
