package org.dbgsprw.core;

import com.android.ddmlib.IDevice;
import com.android.ddmlib.IShellOutputReceiver;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map;

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

public class ShellCommandExecutor {
    private ProcessBuilder mProcessBuilder;

    public ShellCommandExecutor() {
        mProcessBuilder = new ProcessBuilder();
    }

    public ProcessBuilder directory(File directory) {
        return mProcessBuilder.directory(directory);
    }

    public Map<String, String> environment() {
        return mProcessBuilder.environment();
    }

    public void executeShellCommand(ArrayList<String> command, IShellOutputReceiver iShellOutputReceiver) {
    //    mProcessBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
    //    mProcessBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
        mProcessBuilder.command(command);
        Process process = null;

        try {
            process = mProcessBuilder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            BufferedReader bufferedInputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            mProcessBuilder.redirectError(mProcessBuilder.redirectInput());
            String inputLine, errorLine;

            boolean hasAnythingOut = true;
            while (hasAnythingOut) {
                hasAnythingOut = false;
                if ((inputLine = bufferedInputReader.readLine()) != null) {
                    iShellOutputReceiver.addOutput(inputLine.getBytes(), 0 , inputLine.length());
                    iShellOutputReceiver.flush();
                    hasAnythingOut = true;
                }

            }
            process.waitFor();
        } catch (InterruptedException e) {
            process.destroy();
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Thread executeShellCommandInThread(final ArrayList<String> command,
                                              final ShellThreadDoneListener shellThreadDoneListener) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
            //    executeShellCommand(command);
                shellThreadDoneListener.shellThreadDone();
            }
        });
        thread.start();
        return thread;

    }

    public ShellCommandResult executeShellCommandResult(ArrayList<String> command) {
        ArrayList<String> outList = new ArrayList<>();
        ArrayList<String> errList = new ArrayList<>();
        mProcessBuilder.command(command);

        try {
            Process process = mProcessBuilder.start();

            BufferedReader bufferedInputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            BufferedReader bufferedErrorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String inputLine, errorLine;

            while (true) {
                if ((errorLine = bufferedErrorReader.readLine()) != null) {
                    errList.add(errorLine);
                } else if ((inputLine = bufferedInputReader.readLine()) != null) {
                    outList.add(inputLine);
                } else {
                    break;
                }
            }
            process.waitFor();


        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return new ShellCommandResult(outList, errList);


    }

    public interface ShellThreadDoneListener {
        void shellThreadDone();
    }
}
