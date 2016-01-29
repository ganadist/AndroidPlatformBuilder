package org.dbgsprw.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Copyright 2016 dbgsprw / dbgsprw@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class Builder {
    private ProcessBuilder mProcessBuilder;
    private ArrayList<String> mLunchMenuList;
    private String mProjectPath;
    private String mTargetProduct;
    private String mTargetBuildVariant;
    private String mOutDir;
    private ArrayList<MakeDoneListener> mMakeDoneListeners;


    private int numberOfProcess;

    private String mJobNumber;


    public Builder(String projectPath) {
        mProcessBuilder = new ProcessBuilder();
        mLunchMenuList = new ArrayList<>();
        mProjectPath = projectPath;
        //     mProcessBuilder.directory(new File(mProjectPath));
        mProcessBuilder.directory(new File("/home/myoo/WORKING_DIRECTORY/"));
        mMakeDoneListeners = new ArrayList<>();


        updateLunchMenu();
        updateNumberOfProcess();
    }

    public void executeMake() {
        final ArrayList<String> makeCommandLine;
        makeCommandLine = new ArrayList<>();
        makeCommandLine.add("make");
        if (mJobNumber != null) {
            makeCommandLine.add("-j" + mJobNumber);
        }
        if (mTargetProduct != null) {
            makeCommandLine.add("TARGET_PRODUCT=" + mTargetProduct);
        }
        if (mTargetBuildVariant != null) {
            makeCommandLine.add("TARGET_BUILD_VARIANT=" + mTargetBuildVariant);
        }
        if (mOutDir != null) {
            makeCommandLine.add("OUT_DIR=" + mOutDir);
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                executeShellCommand(makeCommandLine);
                Builder.this.notifyMakeDone();
            }
        }).start();
    }

    public int getNumberOfProcess() {
        return numberOfProcess;
    }

    public ArrayList<String> getLunchMenuList() {
        return mLunchMenuList;
    }

    public void setMakeOptions(String jobNumber, String outDir, String targetProduct, String targetBuildVariant) {
        mJobNumber = jobNumber;
        mOutDir = outDir;
        mTargetProduct = targetProduct;
        mTargetBuildVariant = targetBuildVariant;
    }

    public void addMakeDoneListener(MakeDoneListener makeDoneListener) {
        mMakeDoneListeners.add(makeDoneListener);

    }

    private void notifyMakeDone() {
        for (MakeDoneListener makeDoneListener : mMakeDoneListeners) {
            makeDoneListener.makeDone();
        }
    }

    private void updateNumberOfProcess() {
        ArrayList<String> getConfCommand = new ArrayList<String>();
        getConfCommand.add("getconf");
        getConfCommand.add("_NPROCESSORS_ONLN");
        ArrayList<String> outList = executeShellCommand(getConfCommand).getOutList();
        numberOfProcess = Integer.parseInt(outList.get(0));
    }

    private void updateLunchMenu() {
        ArrayList<String> lunchCommand = new ArrayList<String>();
        lunchCommand.add("bash");
        lunchCommand.add("-c");
        lunchCommand.add("source build/envsetup.sh > /dev/null ;" + "echo ${LUNCH_MENU_CHOICES[*]}");
        ArrayList<String> outList = executeShellCommand(lunchCommand).getOutList();
        String line = outList.get(0);

        String[] lunchMenus = line.split(" ");
        for (String lunchMenu : lunchMenus) mLunchMenuList.add(lunchMenu);
    }

    private ShellCommandResult executeShellCommand(ArrayList<String> command) {
        ArrayList<String> outList = new ArrayList<>();
        ArrayList<String> errList = new ArrayList<>();

        mProcessBuilder.command(command);

        try {
            Process process = mProcessBuilder.start();

            BufferedReader bufferedInputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            BufferedReader bufferedErrorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String inputLine, errorLine;

            while (true) {
                if ((inputLine = bufferedInputReader.readLine()) != null) {
                    outList.add(inputLine);
                    System.out.println(inputLine);
                } else if ((errorLine = bufferedErrorReader.readLine()) != null) {
                    errList.add(errorLine);
                    System.out.println(errorLine);
                } else {
                    System.out.println("nothing to out");
                    break;
                }
            }
            // maybe need thread
            process.waitFor();


        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return new ShellCommandResult(outList, errList);
    }

    public interface MakeDoneListener {
        void makeDone();
    }
}
