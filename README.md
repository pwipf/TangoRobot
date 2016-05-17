# TangoRobot

This is a Google Tango tablet project to let the Tango control a small robot using it's magical ADF tracking capabilities.

The project is a Android Studio 2.0 project.

The idea is that the Tango tablet is attached to a robot and communicates with the robot sending commands over a USB to Serial connection (such as that provided by most Arduino boards, or many usb to serial dongles).  The robot is programmed to respond to simple character commands, such as a character followed by a number, specifying which wheel to move in which direction, at a particular speed.

The goal is to manually drive the robot around (with a remote control) while the Tango learns the area, and also record various waypoints along it's path.  Then after saving the area, the robot can be told to go to a waypoint, and it will localize itself in the area, find the closest waypoint, and then follow the path to the destination or target.  The project was created for a robotics course assignment, and expanded with the detailed user interface for fun.

The remote control is a separate android studio project: [TangoRobotRemote](https://github.com/pwipf/TangoRobotRemote).

This project requires a Google Tango tablet, but with a slight modification can be tried on any android tablet, it only requires changing the TangoReal object in mainActivity to a TangoFake object, and then it will function in a sort of test mode on any tablet.  It works well to run this project on a tablet and run the TangoRobotRemote app on the android studio emulator.

Here is a screenshot of the remote, the screen on the Tango tablet is very similar, but has a first-person view, where the robot always stays in the middle facing forward, and the view rotates and moves about the robot.

![screen](/RemoteScreen.png?raw=true "screenshot")
