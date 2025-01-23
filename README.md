# Pihla & Aire application for Temi 3

~/Library/Android/sdk/platform-tools/adb connect 192.168.1.171  
~/Library/Android/sdk/platform-tools/adb connect 10.224.3.12  
~/Library/Android/sdk/platform-tools/adb devices | grep emulator | cut -f1 | while read line; do adb -s $line emu kill; done  