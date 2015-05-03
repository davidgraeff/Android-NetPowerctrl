class {


/**
 * This search all executables by name and return the first one that fits the name. This can be used
 * by speech commands.
 * @param executable_title The name of an executable.
 * @param unprecise If true, also return an executable if its name is not a direct hit but contains
 *                  the given title.
 * @return
 */
@Nullable
public Executable findFirstExecutableByName(@NonNull String executable_title, boolean unprecise) {
        for (Credentials di : credentialsCollection.items) {
        di.lockDevicePorts();
        Iterator<Executable> it = di.getDevicePortIterator();
        while (it.hasNext()) {
        Executable port = it.next();
        if (!unprecise) {
        if (executable_title.contains(port.getTitle().toLowerCase())) {
        di.releaseDevicePorts();
        return port;
        }
        } else {
        String[] strings = port.getTitle().toLowerCase().split("\\W+");
        for (String string : strings) {
        if (executable_title.contains(string)) {
        di.releaseDevicePorts();
        return port;
        }
        }
        }
        }
        di.releaseDevicePorts();
        }

        for (Scene scene : sceneCollection.items)
        if (!unprecise) {
        if (executable_title.contains(scene.getTitle().toLowerCase()))
        return scene;
        } else {
        String[] strings = scene.getTitle().toLowerCase().split("\\W+");
        for (String string : strings) {
        if (executable_title.contains(string)) {
        return scene;
        }
        }
        }

        return null;
        }

@Nullable
public Executable findDevicePort(@Nullable String uuid) {
        if (uuid == null)
        return null;

        for (Credentials di : credentialsCollection.items) {
        di.lockDevicePorts();
        Iterator<Executable> it = di.getDevicePortIterator();
        while (it.hasNext()) {
        Executable port = it.next();
        if (port.getUid().equals(uuid)) {
        di.releaseDevicePorts();
        return port;
        }
        }
        di.releaseDevicePorts();
        }
        return null;
        }

@Nullable
public Credentials findDevice(@NonNull String uniqueID) {
        for (Credentials di : credentialsCollection.items) {
        if (di.getUniqueDeviceID().equals(uniqueID)) {
        return di;
        }
        }
        return null;
        }

@Nullable
public Credentials findDeviceUnconfigured(@NonNull String uniqueID) {
        for (Credentials di : IOConnectionsNotConfiguredCollection.items) {
        if (di.getUniqueDeviceID().equals(uniqueID)) {
        return di;
        }
        }
        return null;
        }

}