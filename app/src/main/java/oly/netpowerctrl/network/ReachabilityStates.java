package oly.netpowerctrl.network;

/**
 * Defines the three states an Executable/Device/IOConnection can have. Either it is reachable, not-reachable or
 * the state is not yet known.
 */
public enum ReachabilityStates {
    NotReachable, Reachable, MaybeReachable
}
