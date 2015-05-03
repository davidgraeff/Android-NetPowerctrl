package oly.netpowerctrl.utils;

import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

/**
 * Some helper methods to read and write json to and from strings.
 */
public class JSONHelper {
    private ByteArrayOutputStream output_stream;
    private JsonWriter writer;

    public static JsonReader getReader(byte[] bytes) {
        ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
        return new JsonReader(new InputStreamReader(stream));
    }

    public static JsonReader getReader(String text) {
        if (text == null)
            return null;

        // Get JsonReader from String
        byte[] bytes;
        try {
            bytes = text.getBytes("UTF-8");
        } catch (UnsupportedEncodingException ignored) {
            return null;
        }
        ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
        return new JsonReader(new InputStreamReader(stream));
    }

    public static JsonWriter createWriter(OutputStream outputStream) throws UnsupportedEncodingException {
        return new JsonWriter(new OutputStreamWriter(outputStream, "UTF-8"));
    }

    /**
     * Uses an internal output stream. Call getString() after this method. Example:
     * JSONHelper h = new JSONHelper();
     * toJSON(h.createWriter());
     * h.getString();
     *
     * @return Return a json writer
     */
    public JsonWriter createWriter() {
        output_stream = new ByteArrayOutputStream();
        try {
            writer = new JsonWriter(new OutputStreamWriter(output_stream, "UTF-8"));
            return writer;
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    public String getString() throws IOException {
        writer.close();
        return output_stream.toString();
    }
}
