package dbgsprw.core;

import java.io.*;
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

    public Process executeShellCommand(ArrayList<String> command) {
        return executeShellCommand(command, new NullResultReceiver());
    }

    public Process executeShellCommand(ArrayList<String> command, final ResultReceiver resultReceiver) {
        mProcessBuilder.command(command);
        Process process = null;

        try {
            process = mProcessBuilder.start();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        final Process finalProcess = process;
        readFromInputStream(finalProcess.getErrorStream(), new ShellOutputReader() {
            @Override
            public void onRead(String line) {
                resultReceiver.newError(line);
            }

            @Override
            public void onExit() {
            }
        });

        readFromInputStream(finalProcess.getInputStream(), new ShellOutputReader() {
            @Override
            public void onRead(String line) {
                resultReceiver.newOut(line);
            }

            @Override
            public void onExit() {
                final int exitCode;
                try {
                    exitCode = finalProcess.waitFor();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return;
                }
                resultReceiver.onExit(exitCode);
            }
        });

        return process;
    }

    private static void readFromInputStream(InputStream is, ShellOutputReader reader) {
        final BufferedReader br = new BufferedReader(new InputStreamReader(is));
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    final String line;
                    try {
                        line = br.readLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                    if (line == null) break;
                    Utils.runOnUi(new Runnable() {
                        @Override
                        public void run() {
                            reader.onRead(line);
                        }
                    });
                }
                Utils.runOnUi(new Runnable() {
                    @Override
                    public void run() {
                        reader.onExit();
                    }
                });
            }
        }).start();
    }

    private interface ShellOutputReader {
        void onRead(String line);
        void onExit();
    }

    public interface ResultReceiver {
        void newOut(String line);
        void newError(String line);
        void onExit(int code);
    }

    private static class NullResultReceiver implements ResultReceiver {

        @Override
        public void newOut(String line) {

        }

        @Override
        public void newError(String line) {

        }

        @Override
        public void onExit(int code) {

        }
    }
}
