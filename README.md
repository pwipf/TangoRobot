# TangoRobot

This is a [Google Project Tango](https://www.google.com/atap/project-tango/) app to let the Tango control a small robot using it's magical ADF localization and motion tracking capabilities.

This repository is an Android Studio 2.0 project.

The scenario is that the Tango tablet is attached to a robot and communicates with the robot sending commands over a USB to Serial connection (such as that provided by most Arduino boards, or many usb to serial dongles).  The robot is programmed to respond to simple character commands, such as a character followed by a number, specifying which wheel to move in which direction, at a what speed.

The goal is to manually drive the robot around (with a remote control) while the Tango learns the area, and also record various waypoints along it's path.  Then after saving the area, the robot can be told to go to a waypoint, and it will localize itself in the area, find the closest waypoint, and then follow the path to the destination or target.  The project was created for a robotics course assignment, and has been expanded with extras and the detailed user interface for fun.

The remote control is a separate android studio project: [TangoRobotRemote](https://github.com/pwipf/TangoRobotRemote).

Features:
- Manually drive robot with simple buttons.
- All operations and status available on a remote control device on the IP network.
- Record, save, and use last saved ADF file.
- Record a path of waypoints to autonomously follow to a given waypoint.
- Accept voice commands for waypoints and destinations.
- Use Tango Depth Perception to recognize an obstacle in the way, mention it, and wait for it to be moved.
- Go around and obstacle if told to.
- "Engage" target (simple action to simulate doing some operation upon reaching the target position)

This project really requires a Google Tango tablet, but with a slight modification can be tried on any android tablet, it only requires changing the TangoReal object in mainActivity to a TangoFake object, and then it will function in a sort of test mode on any tablet.  It works well to run this project on a tablet and run the TangoRobotRemote app on the android studio emulator.

Here is a screenshot of the remote, the screen on the Tango tablet is very similar, but has a first-person view, where the robot always stays in the middle facing forward, and the view rotates and moves about the robot.

![screen](/RemoteScreen.png?raw=true "screenshot")
