# netpowerctrl
	netpowerctrl is an Android App for controlling ANEL NET-PwrCtrl's (http://www.anel-elektronik.de)
	switchable network outlets.

You can find a ready to install apk at [Github Releases](https://github.com/davidgraeff/Android-NetPowerctrl/releases)
and on [Google Play](https://play.google.com/store/apps/details?id=oly.netpowerctrl).
For bugs and feature requests please use [Github Issues](https://github.com/davidgraeff/Android-NetPowerctrl/issues).

## Features
* Devices are detected automatically on the network.
* Username/password and individual ports can be configured for every device.
* Create a homescreen widget for a particular outlet with life visualisation of the current state.
* Use your own icons for on/off/unreachable widget states.
* Dark and light app theme.
* Overview of all outlets for all configured devices. Reorder and hide outlets.
* Tablets supported.
* Create switch-groups (scenes) to switch multiple outlets at once.
* Use Shortcuts for toggling, switching on/off individual outlets.

<table><tr valign="top"><td>
<img width="200px" src="doc/devices.png" />
</td><td>
<img width="200px" src="doc/outlets.png" />
</td></tr></table>

### Automatic device detection
Devices are detected automatically if they are configured for UDP communication.
Default send port 1077 / receive port 1075 (can be configured). Please be aware
that you cannot use port numbers < 1024 because of android restrictions!

### Support for automation apps like Tasker/Llama
Most of the automation apps support creating shortcuts for invoking other applications.
You may create shortcuts for single outlets to toggle the state or switch it on/off.
This way you can for instance switch on lights if your mobile gets in range of your wifi network.

### Building
To compile, you need the [Android SDK](http://developer.android.com/sdk) and [Android Studio](http://developer.android.com/sdk/installing/studio.html). This project is not for
the eclipse based SDK!

### Authors
* david.graeff(at)web_de
* Based on http://sourceforge.net/projects/netpowerctrl/ (oly(at)nittka_com)

### License
GPLv2
