package org.dbgsprw.core;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;

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

public class FastBootMonitor {


    private static ArrayList<String> sDeviceSerialNumbers;
    private static ArrayList<DeviceChangeListener> sIDeviceChangeListeners;
    private static ShellCommandExecutor sShellCommandExecutor;
    private static Thread sThread;
    private static boolean sIsTerminate;


    public static void init() {
        sIsTerminate = false;
        sIDeviceChangeListeners = new ArrayList<>();
        sShellCommandExecutor = new ShellCommandExecutor();
        sDeviceSerialNumbers = new ArrayList<>();
        sThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(!sIsTerminate) {
                    ArrayList<String> command = new ArrayList<>();
                    command.add("fastboot");
                    command.add("devices");
                    ShellCommandResult shellCommandResult = sShellCommandExecutor.executeShellCommandResult(command);
                    ArrayList<String> outList = shellCommandResult.getOutList();
                    boolean[] isExist = new boolean[sDeviceSerialNumbers.size()];
                    int length = isExist.length;
                    for (String out : outList) {
                        String deviceSerialNumber = out.split("\t")[0];
                        if (!sDeviceSerialNumbers.contains(deviceSerialNumber)) {
                            sDeviceSerialNumbers.add(deviceSerialNumber);
                            deviceConnected(deviceSerialNumber);
                        } else {
                            isExist[sDeviceSerialNumbers.indexOf(deviceSerialNumber)] = true;
                        }
                    }
                    for (int i = 0, serialNumberIndex = 0; i < length; i++) {
                        if (!isExist[i]) {
                            deviceDisconnected(sDeviceSerialNumbers.get(serialNumberIndex));
                            sDeviceSerialNumbers.remove(serialNumberIndex);
                        } else {
                            serialNumberIndex++;
                        }
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

        });
        sThread.start();
    }

    public static ArrayList<String> getDeviceSerialNumbers() {
        return sDeviceSerialNumbers;
    }

    public static void terminate() {
        sIsTerminate = true;
    }

    public static void addDeviceChangeListener(DeviceChangeListener deviceChangeListener) {
        sIDeviceChangeListeners.add(deviceChangeListener);
    }

    private static void deviceConnected(String serialNumber) {
        for (DeviceChangeListener deviceChangeListener : sIDeviceChangeListeners) {
            deviceChangeListener.fastBootDeviceConnected(serialNumber);
        }
    }

    private static void deviceDisconnected(String serialNumber) {
        for (DeviceChangeListener deviceChangeListener : sIDeviceChangeListeners) {
            deviceChangeListener.fastBootDeviceDisconnected(serialNumber);
        }
    }

 /*   private static void deviceChanged(String serialNumber) {
        for (DeviceChangeListener deviceChangeListener : sIDeviceChangeListeners) {
            deviceChangeListener.fastBootDeviceChanged(serialNumber);
        }
    }*/

    public interface DeviceChangeListener extends AndroidDebugBridge.IDeviceChangeListener {
        @Override
        public void deviceConnected(IDevice device);

        @Override
        public void deviceDisconnected(IDevice device);

        @Override
        public void deviceChanged(IDevice device, int changeMask);

        public void fastBootDeviceConnected(String serialNumber);

        public void fastBootDeviceDisconnected(String serialNumber);

    //    public void fastBootDeviceChanged(String serialNumber);
    }

}
