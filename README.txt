netpowerctrl is an Android UI for controlling an
ANEL NET-PwrCtrl (www.anel-elektronik.de)

To compile,you need the Android SDK
(developer.android.com/sdk)

This is a beta release, it "works for me" with my
particular NET-PwrCtrl. Your mileage may vary.

What works:
- devices are detected on the network if they are
  configured for UDP communication.
  Default send port 1077 / receive port 1075
  (can be configured).
  Unfortunately, We can not use the ANEL default ports
  of 77/75 on non-rooted android devices, so you have
  to re-configure.
   
- Copy a discovered device or create a new one,
  set the username/password and maybe the ports
  (for the correct ports, see the web-interface
  of your ANEL device)
  
- Create a homescreen widget for a particular
  (previously configured) device.  

enjoy :-)

Copyright GPLv2
oly@nittka.com
-- o
