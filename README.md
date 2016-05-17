# TangoRobot

This is a Tango tablet project to let the tango control a small robot using it's magical ADF tracking capabilities.

TCP Remote control for [TangoRobot](https://github.com/pwipf/TangoRobot).
This android studio project runs on an android tablet or emulator (not yet tried on a phone).  
It automatically connects to the android tablet runnning the TangoRobot app (assuming a network connection that allows the configured port number).  

For testing, it works well to run this app on an android studio emulator, and run the TangoRobot app on an android tablet.  The TangoRobot app can be adjusted to use a FakeTango class instead of the RealTango class in case you don't have a Google Tango tablet handy.

Here is a screenshot showing the Remote screen, very similar to the view on the Tango tablet attached to the robot:
![screen](/RemoteScreen.png?raw=true "screenshot")
There is a status area on the left, with a log area below, the main view shows the robot and various details of it's position and the environment.  The view is third-person view, and implements touch controls to zoom, pan, and rotate the view.  
The scattered buttons control the robot and it's various modes.
