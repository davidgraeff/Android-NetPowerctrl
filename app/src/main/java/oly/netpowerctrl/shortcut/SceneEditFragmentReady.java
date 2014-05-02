package oly.netpowerctrl.shortcut;

import oly.netpowerctrl.main.SceneEditFragment;

/**
 * Used for the communication of the SceneEditFragments and the SceneEditActivity.
 */
public interface SceneEditFragmentReady {
    void sceneEditFragmentReady(SceneEditFragment fragment);
}
