package oly.netpowerctrl.executables;

import android.util.JsonReader;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import oly.netpowerctrl.scenes.Scene;
import oly.netpowerctrl.utils.FactoryInterface;

/**
 * Return an executable by providing a json reader or FileInputStream.
 */
public class ExecutableFabric implements FactoryInterface<Executable> {

    public static String fromStream(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder out = new StringBuilder();
        String newLine = System.getProperty("line.separator");
        String line;
        while ((line = reader.readLine()) != null) {
            out.append(line);
            out.append(newLine);
        }
        return out.toString();
    }

    @Override
    public Executable newInstance(FileInputStream fileInputStream) throws IOException, ClassNotFoundException {
        return newInstance(new JsonReader(new InputStreamReader(fileInputStream)));
    }

    public Executable newInstance(JsonReader reader) throws IOException, ClassNotFoundException {
        Log.w("Fabric", "newInstance");
        if (!reader.hasNext()) {
            return null;
        }

        reader.beginObject();

        String name = reader.nextName();
        if (!name.equals("ObjectType")) {
            Log.e("DeviceConnection", "Expected connection_type first! " + name);
            reader.endObject();
            return null;
        }

        name = reader.nextString();

        Executable executable;

        if (name.equals(Scene.class.getName())) {
            executable = new Scene();
        } else {
            executable = new Executable();
        }

        executable.load(reader, true);

        return executable;
    }
}
