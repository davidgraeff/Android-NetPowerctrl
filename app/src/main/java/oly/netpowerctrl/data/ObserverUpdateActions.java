package oly.netpowerctrl.data;

/**
 * Created by david on 01.09.14.
 */
public enum ObserverUpdateActions {
    AddAction, // Elements have been added
    UpdateAction, // Amount of entries stay the same
    RemoveAction, // Elements have been removed
    RemoveAllAction, // Elements have been removed
    ClearAndNewAction // Complex changes
}
