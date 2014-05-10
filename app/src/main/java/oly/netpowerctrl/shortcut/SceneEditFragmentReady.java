package oly.netpowerctrl.shortcut;

/**
 * Used for the communication of the SceneEditFragments and the SceneEditActivity.
 */
public interface SceneEditFragmentReady {
    public void sceneEditFragmentReady(SceneEditFragment fragment);

    public void entryDismiss(SceneEditFragment fragment, int position);
}
