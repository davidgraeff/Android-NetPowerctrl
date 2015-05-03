package oly.netpowerctrl.status_bar;

import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.data.storage_container.CollectionMapItems;
import oly.netpowerctrl.utils.ObserverUpdateActions;

/**
 * List of scenes
 */
public class FavCollection extends CollectionMapItems<FavCollection, FavItem> {
    public FavCollection(DataService dataService) {
        super(dataService, "favourites");
    }

    /**
     * Updates the favourite flag of this executable. A favourite is shown in the android
     * system bar (if enabled) and can be executed from there directly.
     *
     * @param favourite The new favourite status.
     */
    public void setFavourite(String executable_uid, boolean favourite) {
        FavItem favItem = items.get(executable_uid);
        if (favourite && favItem == null) {
            favItem = new FavItem(executable_uid);
            items.put(executable_uid, favItem);
            notifyObservers(favItem, ObserverUpdateActions.AddAction);
            storage.save(favItem);
        } else if (!favourite && favItem != null) {
            items.remove(executable_uid);
            notifyObservers(favItem, ObserverUpdateActions.RemoveAction);
            storage.remove(favItem);
        }
    }

    /**
     * Return true if this scene is a favourite. If you want to set the favourite
     * flag use DataService.getInstance().sceneCollection.setFavaourite(scene, boolean);
     *
     * @return Return true if this scene is a favourite.
     */
    @SuppressWarnings("unused")
    public boolean isFavourite(String executable_uid) {
        return items.containsKey(executable_uid);
    }
}
