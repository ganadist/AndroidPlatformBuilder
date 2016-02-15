package dbgsprw.core;

import com.android.ddmlib.*;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkTypeId;
import dbgsprw.exception.AndroidHomeNotFoundException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Copyright 2016 dbgsprw / dbgsprw@gmail.com
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class DeviceManager {

    ArrayList<FastBootStateChangeListener> mFastBootStateChangeListeners;

    AndroidDebugBridge mAndroidDebugBridge;
    ShellCommandExecutor mShellCommandExecutor;
    private Thread mFlashThread;
    private Thread mAdbSyncThread;
    private String mAdbPath;
    private String mFastBootPath;

    public DeviceManager() {
        mFastBootStateChangeListeners = new ArrayList<>();
        mShellCommandExecutor = new ShellCommandExecutor();
    }

    public void adbInit() throws AndroidHomeNotFoundException {
        adbInit(findAndroidHome());
    }

    public void adbInit(String androidHome) throws AndroidHomeNotFoundException {

        if (androidHome == null) {
            throw new AndroidHomeNotFoundException();
        }
        AndroidDebugBridge.initIfNeeded(false);
        try {
            mAdbPath = new File(androidHome, "platform-tools" + File.separator + "adb").getCanonicalPath();
            mFastBootPath = new File(androidHome, "platform-tools" + File.separator + "fastboot").getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mAndroidDebugBridge = AndroidDebugBridge.createBridge(mAdbPath, true);
    }


    public void fastBootMonitorInit() throws AndroidHomeNotFoundException {
        FastBootMonitor.init(mFastBootPath);
    }

    public void addDeviceChangeListener(FastBootMonitor.DeviceChangeListener listener) {
        AndroidDebugBridge.addDeviceChangeListener(listener);
        FastBootMonitor.addDeviceChangeListener(listener);
        // initial notify already connected device
        for (IDevice iDevice :getDevices()) {
            listener.deviceConnected(iDevice);
        }
        for (String serialNumber : getFastBootDevices()) {
            listener.fastBootDeviceConnected(serialNumber);
        }

    }

    public void FastBootMonitorInit(String androidHome) throws AndroidHomeNotFoundException {
        if (androidHome == null) {
            throw new AndroidHomeNotFoundException();
        }
        try {
            mFastBootPath = new File(androidHome, "platform-tools" + File.separator + "fastboot").getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public ArrayList<String> getFastBootDevices() {
        return FastBootMonitor.getDeviceSerialNumbers();
    }

    public IDevice[] getDevices() {
        return mAndroidDebugBridge.getDevices();
    }

    public void rebootDeviceBootloader(IDevice iDevice) {
        try {
            iDevice.reboot("bootloader");
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (AdbCommandRejectedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isRootMode(IDevice device) {
        ArrayList<String> command = new ArrayList<>();
        command.add(mAdbPath);
        command.add("-s");
        command.add(device.getSerialNumber());
        command.add("shell");
        command.add("id");
        command.add("-u");
        final boolean[] isRootMode = new boolean[1];
        mShellCommandExecutor.executeShellCommand(command, new ShellCommandExecutor.ResultReceiver() {
            @Override
            public void newOut(String line) {
                if("0".equals(line)) {
                    isRootMode[0] = true;
                } else {
                    isRootMode[0] = false;
                }
            }

            @Override
            public void newError(String line) {

            }
        });
        return isRootMode[0];
    }


    public void adbRoot(IDevice device, ShellCommandExecutor.ResultReceiver resultReceiver) {
        if (isRootMode(device) || !IDevice.DeviceState.ONLINE.equals(device.getState())) {
            return;
        }
        ArrayList<String> command = new ArrayList<>();
        command.add(mAdbPath);
        command.add("-s");
        command.add(device.getSerialNumber());
        command.add("root");
        mShellCommandExecutor.executeShellCommand(command, resultReceiver);
    }

    public void adbRemount(IDevice device, ShellCommandExecutor.ResultReceiver resultReceiver) {
        ArrayList<String> command = new ArrayList<>();
        command.add(mAdbPath);
        command.add("-s");
        command.add(device.getSerialNumber());
        command.add("remount");
        mShellCommandExecutor.executeShellCommand(command, resultReceiver);
    }

    public void adbSync(IDevice device, String argument, ShellCommandExecutor.ThreadResultReceiver threadResultReceiver) {
        ArrayList<String> command = new ArrayList<>();
        command.add(mAdbPath);
        command.add("-s");
        command.add(device.getSerialNumber());
        command.add("sync");
        if (argument != null) {
            command.add(argument);
        }
        mAdbSyncThread = mShellCommandExecutor.executeShellCommandInThread(command, threadResultReceiver);
    }


    public void rebootDevice(String deviceSerialNumber, ShellCommandExecutor.ResultReceiver resultReceiver) {
        ArrayList<String> command = new ArrayList<>();
        command.add(mFastBootPath);
        command.add("-s");
        command.add(deviceSerialNumber);
        command.add("reboot");
        mShellCommandExecutor.executeShellCommand(command, resultReceiver);
    }

    /*
    @param deviceSerialNumber device serial number
    @param argument update, flashall, vendor, system, boot, etc..
    @param wipe -w option
    */
    public void flash(String deviceSerialNumber, boolean wipe, String[] arguments,
                      ShellCommandExecutor.ThreadResultReceiver threadResultReceiver) {
        ArrayList<String> command = new ArrayList<>();
        command.add(mFastBootPath);
        command.add("-s");
        command.add(deviceSerialNumber);
        if (wipe == true) {
            command.add("-w");
        }
        // like flashall
        for (String argument :arguments) {
            command.add(argument);
        }
        mFlashThread = mShellCommandExecutor.executeShellCommandInThread(command, threadResultReceiver);
    }

    public void setTargetProductPath(File directory) {
        mShellCommandExecutor.environment().put("ANDROID_PRODUCT_OUT", directory.getAbsolutePath());
    }

    private String findAndroidHome() {
        for (Sdk sdk : ProjectJdkTable.getInstance().getAllJdks()) {
            SdkTypeId sdkTypeId = sdk.getSdkType();
            if ("Android SDK".equals(sdkTypeId.getName())) {
                return sdk.getHomePath();
            }
        }
        return System.getenv("ANDROID_HOME");
    }


    public void addMakeDoneListener(FastBootStateChangeListener fastBootStateChangeListener) {
        mFastBootStateChangeListeners.add(fastBootStateChangeListener);

    }

    private void notifyStateChange(FastBootState fastBootState) {
        for (FastBootStateChangeListener fastBootStateChangeListener : mFastBootStateChangeListeners) {
            fastBootStateChangeListener.stateChanged(fastBootState);
        }
    }

    public void stopFlash() {
        if (mFlashThread != null) {
            mFlashThread.interrupt();
        }
    }

    public void stopAdbSync() {
        if (mAdbSyncThread != null) {
            mAdbSyncThread.interrupt();
        }
    }

    public interface FastBootStateChangeListener {
        void stateChanged(FastBootState fastBootState);

    }

    class FastBootState {
        public static final int DONE = 0;
    }


}
