# This project is deprecated.

Please use [Android Studio for Platform](https://d.android.com/studio/platform) instead of this plugin.

# Android Platform Builder plugin for Intellij Idea
[![Build Status](https://travis-ci.org/ganadist/AndroidPlatformBuilder.svg?branch=develop)](https://travis-ci.org/ganadist/AndroidPlatformBuilder)

## Features
 * Show available build configuration on your android platform sources.
 * Support full image build and partial build.
 * Prints on Build console with link for source codes that have errors.
 * Java compiler can be selected via module Sdk to build platform.
 * Flash ROM image on your devices with fastboot or adb.

## Screenshot
 ![alt idea](https://i.imgur.com/jJ0aCiq.png "build log when it failed")

## Setup for usage
 1. You must get android platform sources and prepare to build. For details, see http://s.android.com/source/requirements.html
 1. Enter android source directory and run this command to generate Idea project file.
 ```
 $ cd ANDROID_PLATFORM_SOURCE_IS_HERE

 $ make idegen
 $ ./development/tools/idegen/idegen.sh
Read excludes: 27ms
Traversed tree: 327384ms
 $ ls android.i*
android.iml  android.ipr
 ```
 1. Install Android Platform Builder Plugin from IntelliJ plugin repository. (TODO)
 1. Open android.ipr with IntelliJ Idea or Android Studio.
 1. Set up project Sdk to Android Platform API XX
 1. Set up module Sdk to JDK that prefer that requires android platform.
  * needs Sun JDK SE 1.6 below Android 4.4
  * needs OpenJDK 1.7 above Android 5.0

## Requires
 1. It needs IntelliJ Idea 14.0 or above.
 1. Android Support Plugin for Idea must be installed and setup with Android Sdk
 1. Project file must contains module named "android"
  * If you generated project file with "idegen" command, project file have already have it.

## Usage
 1. This plugin provides "Android Builder" Tool Window. If you want to use this, select following menu.
  * Select View -> Tool Windows -> Android Builder
 1. Select Product to you want to build.
 1. Select Variant.
  * eng means for platform developer build.
  * userdebug means for end user and can be switched to debug mode.
  * user means for end user.
 1. Select Build mode in Make radio box
  * When you select "make" mode, you can select "Target" that provides build rule from android build system
  * When you select "mm" mode, you can build partially.
 1. Press "Make" button to start build.
 1. TODO (usage to flash rom image)
