netpowerctrl is an Android UI for controlling an
ANEL NET-PwrCtrl (www.anel-elektronik.de)

To compile,you need the Android SDK
(developer.android.com/sdk)

This is a beta release, it "works for me" with my
particular NET-PwrCtrl. Your mileage may vary.

What works:
- devices are detected on the network if they are
  configured for UDP communication, send
  port 1077 / receive port 1075. These ports
  are currently hardcoded.
  UNfortunately, We can not use the default ports
  of 77/75 on non-rooted android devices, so you have
  to re-configure.
   
- there is currently no user interface to configure
  the individual outlets of a device. You must set
  the username/password of a discovered device
  (and copy it to configured devices) to be able
  to switch the outlets on/off (which is what
  this software is all about ;-)

Copyright GPLv2
oly@nittka.com
-- o
