package dbgsprw.core;

import java.io.File;
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

public class Builder {

    ShellCommandExecutor mShellCommandExecutor;
    private ArrayList<String> mLunchMenuList;
    private String mProjectPath;
    private String mTargetProduct;
    private String mTargetBuildVariant;
    private String mOutDir;
    private String mTarget;
    private boolean mIsVerbose;
    private String mOneShotMakefile;
    private int mNumberOfProcess;
    private String mJobNumber;
    private Thread mMakeThread;
    private boolean mIsAOSPPath;
    private String mProductOutPath;


    public Builder(String projectPath) {
        mLunchMenuList = new ArrayList<>();
        mProjectPath = projectPath;
        mShellCommandExecutor = new ShellCommandExecutor();
        mShellCommandExecutor.directory(new File(mProjectPath));


        updateLunchMenu();
        updateNumberOfProcess();
    }

    public void executeMake(ShellCommandExecutor.ThreadResultReceiver threadResultReceiver) {
        final ArrayList<String> makeCommandLine;

        makeCommandLine = new ArrayList<>();
        makeCommandLine.add("make");
        makeCommandLine.add("-C");
        makeCommandLine.add(mProjectPath);
        makeCommandLine.add("-f");
        makeCommandLine.add("build/core/main.mk");
        if (mJobNumber != null) {
            makeCommandLine.add("-j" + mJobNumber);
        }
        if (mTarget != null) {
            makeCommandLine.add(mTarget);
        }
        if (mOneShotMakefile != null) {
            makeCommandLine.add("ONE_SHOT_MAKEFILE=" + mOneShotMakefile);
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
        if (mIsVerbose) {
            makeCommandLine.add("showcommands");
        }
        mMakeThread = mShellCommandExecutor.executeShellCommandInThread(makeCommandLine, threadResultReceiver);

    }

    public void stopMake() {
        if (mMakeThread != null) {
            mMakeThread.interrupt();
        }
    }

    public int getNumberOfProcess() {
        return mNumberOfProcess;
    }

    public ArrayList<String> getLunchMenuList() {
        return mLunchMenuList;
    }

    public void setOneShotMakefile(String oneShotMakefile) {
        mOneShotMakefile = oneShotMakefile;
    }

    public void setTargetBuildVariant(String targetBuildVariant) {
        mTargetBuildVariant = targetBuildVariant;
    }

    public void setTargetProduct(String targetProduct) {
        mTargetProduct = targetProduct;
    }

    public void setJobNumber(String jobNumber) {
        mJobNumber = jobNumber;
    }

    public void setOutDir(String outDir) {
        mOutDir = outDir;
    }

    public void setTarget(String target) {
        mTarget = target;
    }

    public void setVerbose(boolean isVerbose) {
        mIsVerbose = isVerbose;
    }

    public void setMakeOptions(String jobNumber, String outDir, String targetProduct, String targetBuildVariant) {
        mJobNumber = jobNumber;
        mOutDir = outDir;
        mTargetProduct = targetProduct;
        mTargetBuildVariant = targetBuildVariant;
    }

    private void updateNumberOfProcess() {
        ArrayList<String> getConfCommand = new ArrayList<>();
        getConfCommand.add("getconf");
        getConfCommand.add("_NPROCESSORS_ONLN");
        mShellCommandExecutor.executeShellCommand(getConfCommand, new ShellCommandExecutor.ResultReceiver() {
            @Override
            public void newOut(String line) {
                mNumberOfProcess = Integer.parseInt(line);
            }

            @Override
            public void newError(String line) {

            }
        });
    }

    private void updateLunchMenu() {
        ArrayList<String> lunchCommand = new ArrayList<>();
        lunchCommand.add("bash");
        lunchCommand.add("-c");
        lunchCommand.add("source build/envsetup.sh > /dev/null ;" + "echo ${LUNCH_MENU_CHOICES[*]}");
        mShellCommandExecutor.executeShellCommand(lunchCommand, new ShellCommandExecutor.ResultReceiver() {
            @Override
            public void newOut(String line) {
                if ("".equals(line)) {
                    mIsAOSPPath = false;
                    return;
                }
                mIsAOSPPath = true;
                String[] lunchMenus = line.split(" ");
                for (String lunchMenu : lunchMenus) mLunchMenuList.add(lunchMenu);
            }

            @Override
            public void newError(String line) {

            }
        });
    }

    public boolean isAOSPPath() {
        return mIsAOSPPath;
    }

    public void findOriginalProductOutPath(String lunchMenu, ShellCommandExecutor.ThreadResultReceiver threadResultReceiver) {
        ArrayList<String> command = new ArrayList<>();
        command.add("bash");
        command.add("-c");
        command.add("source build/envsetup.sh > /dev/null;" + " lunch " + lunchMenu + " > /dev/null; echo $ANDROID_PRODUCT_OUT");
        mShellCommandExecutor.executeShellCommandInThread(command, threadResultReceiver);
    }
}
