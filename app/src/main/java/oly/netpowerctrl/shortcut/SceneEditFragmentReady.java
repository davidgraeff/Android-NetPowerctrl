package oly.netpowerctrl.shortcut;

/**
 * Used for the communication of the SceneEditFragments and the SceneEditActivity.
 */
public interface SceneEditFragmentReady {
    void sceneEditFragmentReady(SceneEditFragment fragment);

    void entryDismiss(SceneEditFragment fragment, int position);
}
