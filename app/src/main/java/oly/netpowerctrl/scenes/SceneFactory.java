package oly.netpowerctrl.scenes;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.device_base.data.JSONHelper;
import oly.netpowerctrl.executables.ExecutableAdapterItem;
import oly.netpowerctrl.utils.Streams;

/**
 * Created by david on 22.10.14.
 */
public class SceneFactory {
    /**
     * Create a list of scene items by all visible device ports.
     *
     * @return List of scene items.
     */
    public static List<SceneItem> sceneItemsFromList(SceneElementsAdapter adapter) {
        List<SceneItem> list_of_scene_items = new ArrayList<>();
        for (ExecutableAdapterItem info : adapter.mItems) {
            if (info.getExecutable() == null) // skip header items
                continue;
            list_of_scene_items.add(new SceneItem(info.getExecutableUid(), info.getCommand_value()));
        }
        return list_of_scene_items;
    }

    public static void createSceneFromActivityIntent(Context context, Intent data) {
        String[] tempBitmapFile = data.getStringArrayExtra(EditSceneActivity.RESULT_SCENE_BITMAP_FILES_TEMP);
        String[] realBitmapFile = data.getStringArrayExtra(EditSceneActivity.RESULT_SCENE_BITMAP_FILES_DEST);
        String scene_json = data.getStringExtra(EditSceneActivity.RESULT_SCENE_JSON);
        String scene_remove_uuid = data.getStringExtra(EditSceneActivity.RESULT_SCENE_REMOVE_UID);
        boolean isFavourite = data.getBooleanExtra(EditSceneActivity.RESULT_SCENE_IS_FAVOURITE, false);

        if (scene_remove_uuid != null) {
            AppData.getInstance().sceneCollection.removeScene(scene_remove_uuid);
            return;
        }

        if (scene_json == null) return;
        Scene scene;
        try {
            scene = Scene.loadFromJson(JSONHelper.getReader(scene_json));
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(context, R.string.error_scene_save_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        if (tempBitmapFile != null) {
            for (int i = 0; i < tempBitmapFile.length; ++i) {
                try {
                    File tempFile = new File(tempBitmapFile[i]);
                    Streams.copy(new FileInputStream(tempFile), new FileOutputStream(new File(realBitmapFile[i])));
                    //noinspection ResultOfMethodCallIgnored
                    tempFile.delete();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(context, R.string.error_scene_icon_save_failed, Toast.LENGTH_SHORT).show();
                }
            }
        }

        AppData appData = AppData.getInstance();
        appData.sceneCollection.add(scene, true);
        appData.favCollection.setFavourite(scene.getUid(), isFavourite);
    }
}
