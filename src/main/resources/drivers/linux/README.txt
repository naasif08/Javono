===========================================
CH340 USB-to-Serial Driver for Linux
===========================================

The CH340 USB-to-Serial driver is usually included in most modern Linux distributions.

-------------------------
How to Check If It’s Working
-------------------------

1. Plug in your CH340 device (e.g., Arduino clone, ESP32, etc.).
2. Open a terminal and run:

   dmesg | grep ch341

   You should see messages indicating that the CH340 device was detected, such as:

   ch341 1-1.2:1.0: ch341-uart converter detected
   usb 1-1.2: ch341-uart converter now attached to ttyUSB0

3. Check for serial devices:

   ls /dev/ttyUSB*

   You should see entries like `/dev/ttyUSB0`.

-------------------------
If CH340 Driver Is Not Working
-------------------------

1. Try loading the kernel module manually:

   sudo modprobe ch341

2. To make sure the device is accessible without root permissions, add a udev rule:

   Create a file `/etc/udev/rules.d/99-ch340.rules` with the following content:

   SUBSYSTEM=="tty", ATTRS{idVendor}=="1a86", ATTRS{idProduct}=="7523", MODE="0666", GROUP="dialout", SYMLINK+="ch340"

3. Reload udev rules:

   sudo udevadm control --reload-rules
   sudo udevadm trigger

4. Unplug and replug your device.

-------------------------
Updating the Kernel or System
-------------------------

If the above does not work, your kernel might be missing support or outdated. Consider updating your system:

   sudo apt update
   sudo apt upgrade
   sudo reboot

-------------------------
Additional Help and Resources
-------------------------

- Arch Wiki: https://wiki.archlinux.org/title/CH340
- CH340 Linux GitHub: https://github.com/juliagoda/ch340-udev

-------------------------
Notes
-------------------------

- No driver installation is usually needed on Linux.
- Use the included README for troubleshooting and manual setup.
- For any questions, refer to your Linux distro documentation.

-------------------------
Thank you for using Javono!
-------------------------
