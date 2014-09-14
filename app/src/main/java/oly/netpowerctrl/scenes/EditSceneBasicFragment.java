package oly.netpowerctrl.scenes;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.AppData;
import oly.netpowerctrl.data.LoadStoreIconData;
import oly.netpowerctrl.main.NfcTagWriterActivity;
import oly.netpowerctrl.utils.AndroidShortcuts;

/**
 * Created by david on 06.09.14.
 */
public class EditSceneBasicFragment extends Fragment implements LoadStoreIconData.IconSelected {
    // UI widgets
    Switch show_mainWindow;
    Switch enable_feedback;
    Scene scene;

    private onEditSceneBasicsChanged listener;
    private boolean isSceneNotShortcut;
    private boolean isLoaded;
    private Button btnSceneFav;
    private View btnSceneAddHomescreen;
    private View btnNFC;

    public EditSceneBasicFragment() {
    }


    public void setOnBasicsChangedListener(onEditSceneBasicsChanged listener) {
        this.listener = listener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_scene_basics, container, false);
        assert view != null;
        show_mainWindow = (Switch) view.findViewById(R.id.shortcut_show_mainwindow);
        enable_feedback = (Switch) view.findViewById(R.id.shortcut_enable_feedback);

        view.findViewById(R.id.btnName).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestName(scene);
            }
        });

        view.findViewById(R.id.btnIcon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LoadStoreIconData.show_select_icon_dialog(getActivity(), "scene_icons", EditSceneBasicFragment.this, null);
            }
        });

        btnNFC = view.findViewById(R.id.btnNFC);
        btnNFC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), NfcTagWriterActivity.class);
                intent.putExtra("uuid", scene.uuid.toString());
                intent.putExtra("name", scene.sceneName);
                startActivity(intent);
            }
        });

        btnSceneFav = (Button) view.findViewById(R.id.btnSceneFavourite);
        btnSceneFav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AppData.getInstance().sceneCollection.setFavourite(scene, !scene.isFavourite());
                updateFavButton();
            }
        });

        btnSceneAddHomescreen = view.findViewById(R.id.btnSceneAddHomescreen);
        btnSceneAddHomescreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //noinspection ConstantConditions
                AndroidShortcuts.createHomeIcon(getActivity().getApplicationContext(), scene);
                Toast.makeText(getActivity(), R.string.scene_add_homescreen_success, Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void updateFavButton() {
        boolean isFav = scene.isFavourite();
        btnSceneFav.setText(isFav ? R.string.scene_remove_favourite : R.string.scene_make_favourite);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        applyScene();
    }

    private void requestName(final Scene scene) {
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

        alert.setTitle(this.getString(R.string.scene_rename));

        final EditText input = new EditText(alert.getContext());
        input.setText(scene.sceneName);
        alert.setView(input);

        alert.setPositiveButton(this.getString(android.R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                @SuppressWarnings("ConstantConditions")
                String name = input.getText().toString();
                if (name.trim().isEmpty())
                    return;
                scene.sceneName = name;
                listener.onNameChanged();
            }
        });

        alert.setNegativeButton(this.getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });

        alert.show();
    }

    public void setScene(Scene scene, boolean isSceneNotShortcut, boolean isLoaded) {
        this.scene = scene;
        this.isSceneNotShortcut = isSceneNotShortcut;
        this.isLoaded = isLoaded;
        applyScene();
    }

    private void applyScene() {
        if (show_mainWindow == null || scene == null)
            return;

        if (!isSceneNotShortcut) {
            show_mainWindow.setVisibility(View.VISIBLE);
            enable_feedback.setVisibility(View.VISIBLE);
            isLoaded = false;
        }

        btnSceneAddHomescreen.setVisibility(isLoaded ? View.VISIBLE : View.GONE);
        btnSceneFav.setVisibility(isLoaded ? View.VISIBLE : View.GONE);
        btnNFC.setVisibility(isLoaded ? View.VISIBLE : View.GONE);
        updateFavButton();
    }

    @Override
    public void setIcon(Object context_object, Bitmap o) {
        listener.onIconChanged(o);
    }
}
