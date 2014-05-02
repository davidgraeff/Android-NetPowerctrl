package oly.netpowerctrl.utils;

import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

/**
 * Some helper methods to read and write json to and from strings.
 */
public class JSONHelper {
    public static JsonReader getReader(String text) {
        // Get JsonReader from String
        byte[] bytes;
        try {
            bytes = text.getBytes("UTF-8");
        } catch (UnsupportedEncodingException ignored) {
            return null;
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        return new JsonReader(new InputStreamReader(bais));
    }

    private ByteArrayOutputStream baos;
    private JsonWriter writer;

    public JsonWriter createWriter() {
        baos = new ByteArrayOutputStream();
        try {
            writer = new JsonWriter(new OutputStreamWriter(baos, "UTF-8"));
            return writer;
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    public String getString() throws IOException {
        writer.close();
        return baos.toString();
    }
}
