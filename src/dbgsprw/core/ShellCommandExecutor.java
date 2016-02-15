package dbgsprw.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
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

    public void executeShellCommand(ArrayList<String> command, final ResultReceiver resultReceiver) {
        mProcessBuilder.command(command);
        Process process = null;

        try {
            process = mProcessBuilder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        final Process finalProcess = process;
        new Thread(new Runnable() {
            @Override
            public void run() {
                BufferedReader bufferedErrorReader = new BufferedReader(new InputStreamReader(finalProcess.getErrorStream()));
                String errorLine;
                try {
                    while ((errorLine = bufferedErrorReader.readLine()) != null) {
                        resultReceiver.newError(errorLine);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                BufferedReader bufferedInputReader = new BufferedReader(new InputStreamReader(finalProcess.getInputStream()));
                String inputLine;
                try {
                    while ((inputLine = bufferedInputReader.readLine()) != null) {
                        resultReceiver.newOut(inputLine);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }).start();
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            process.destroy();
            e.printStackTrace();
        }
    }

    public Thread executeShellCommandInThread(final ArrayList<String> command,
                                              final ThreadResultReceiver threadResultReceiver) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                executeShellCommand(command, threadResultReceiver);
                threadResultReceiver.shellThreadDone();
            }
        });
        thread.start();
        return thread;

    }

    public interface ResultReceiver {
        void newOut(String line);

        void newError(String line);
    }

    public interface ThreadResultReceiver extends ResultReceiver {

        void shellThreadDone();
    }

}
