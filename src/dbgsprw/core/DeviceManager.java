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
    private Process mFlashProcess;
    private Process mAdbSyncProcess;
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
            final File platformToolHome = new File(androidHome, "platform-tools");
            mAdbPath = new File(platformToolHome, "adb").getCanonicalPath();
            mFastBootPath = new File(platformToolHome, "fastboot").getCanonicalPath();
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
        ArrayList<String> command = buildAdbCommand(device, "shell");
        command.add("id");
        command.add("-u");
        final boolean[] isRootMode = new boolean[1];
        mShellCommandExecutor.executeShellCommand(command, new ShellCommandExecutor.ResultReceiver() {
            @Override
            public void newOut(String line) {
                isRootMode[0] = "0".equals(line);
            }

            @Override
            public void newError(String line) {}

            @Override
            public void onExit(int code) {}
        });
        return isRootMode[0];
    }

    private ArrayList<String> buildAdbCommand(IDevice device, String cmd) {
        ArrayList<String> command = new ArrayList<>();
        command.add(mAdbPath);
        command.add("-s");
        command.add(device.getSerialNumber());
        command.add(cmd);
        return command;
    }

    private Process runAdbCommamand(IDevice device, String cmd) {
        return mShellCommandExecutor.executeShellCommand(buildAdbCommand(device, cmd));
    }

    public void adbRoot(IDevice device) {
        if (isRootMode(device) || !IDevice.DeviceState.ONLINE.equals(device.getState())) {
            return;
        }
        runAdbCommamand(device, "root");
    }

    public void adbRemount(IDevice device) {
        runAdbCommamand(device, "remount");
    }

    public interface SyncListener {
        void onCompleted(boolean success);
    }

    private static class SyncResultReceiver implements ShellCommandExecutor.ResultReceiver {
        private SyncListener mListener;
        SyncResultReceiver(SyncListener listener) {
            mListener = listener;
        }
        @Override
        public void newOut(String line) {}

        @Override
        public void newError(String line) {}

        @Override
        public void onExit(int code) {
            mListener.onCompleted(code == 0);
        }
    }

    public void adbSync(IDevice device, String argument, SyncListener listener) {
        ArrayList<String> command = buildAdbCommand(device, "sync");
        if (argument != null) {
            command.add(argument);
        }
        mAdbSyncProcess = mShellCommandExecutor.executeShellCommand(command,
                new SyncResultReceiver(listener));
    }


    public void rebootDevice(String deviceSerialNumber) {
        ArrayList<String> command = new ArrayList<>();
        command.add(mFastBootPath);
        command.add("-s");
        command.add(deviceSerialNumber);
        command.add("reboot");
        mShellCommandExecutor.executeShellCommand(command);
    }

    /*
    @param deviceSerialNumber device serial number
    @param argument update, flashall, vendor, system, boot, etc..
    @param wipe -w option
    */
    public void flash(String deviceSerialNumber, boolean wipe, String[] arguments, SyncListener listener) {
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
        mFlashProcess = mShellCommandExecutor.executeShellCommand(command, new SyncResultReceiver(listener));
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
        if (mFlashProcess != null) {
            mFlashProcess.destroy();
        }
    }

    public void stopAdbSync() {
        if (mAdbSyncProcess != null) {
            mAdbSyncProcess.destroy();
        }
    }

    public interface FastBootStateChangeListener {
        void stateChanged(FastBootState fastBootState);

    }

    class FastBootState {
        public static final int DONE = 0;
    }
}