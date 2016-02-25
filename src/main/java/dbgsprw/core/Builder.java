package dbgsprw.core;

import java.io.File;
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

public class Builder {

    private final LunchDoneListener mLunchDoneListener;
    ShellCommandExecutor mShellCommandExecutor;
    private ArrayList<String> mLunchMenuList;
    private String mProjectPath;
    private String mTargetProduct;
    private String mTargetBuildVariant;
    private String mOutDir;
    private String mTarget;
    private String mExtraArguments = "";
    private boolean mIsVerbose;
    private String mOneShotMakefile;
    private int mJobNumber = 1;
    private Process mMakeProcess;
    private Process mProductOutRetriever;

    public Builder(String projectPath, LunchDoneListener lunchDoneListener) {
        mLunchMenuList = new ArrayList<String>();
        mProjectPath = projectPath;
        mShellCommandExecutor = new ShellCommandExecutor();
        mShellCommandExecutor.directory(new File(mProjectPath));
        mLunchDoneListener = lunchDoneListener;

        updateLunchMenu();
    }

    public void setAndroidJavaHome(String directoryPath) {
        Map<String, String> env = mShellCommandExecutor.environment();
        env.put("ANDROID_JAVA_HOME", directoryPath);
        final String path = env.get("PATH");
        final String jdkBinPath = Utils.pathJoin(directoryPath, "bin");
        if (!path.startsWith(jdkBinPath)) {
            env.put("PATH", jdkBinPath + File.pathSeparator + path);
        }
    }

    public void changeProjectPath(String projectPath) {
        mProjectPath = projectPath;
        mShellCommandExecutor.directory(new File(mProjectPath));
        updateLunchMenu();
    }

    public ArrayList<String> buildMakeCommand() {
        final ArrayList<String> makeCommandLine;

        makeCommandLine = new ArrayList<>();
        makeCommandLine.add("make");

        if (mJobNumber > 1) {
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

        if (mIsVerbose) {
            makeCommandLine.add("showcommands");
        }
        if (!mExtraArguments.equals("")) {
            for (String arg : mExtraArguments.split(("\\s+"))) {
                makeCommandLine.add(arg);
            }
        }
        return makeCommandLine;
    }

    public void executeMake(ShellCommandExecutor.ResultReceiver receiver) {
        mMakeProcess = mShellCommandExecutor.executeShellCommand(buildMakeCommand(), receiver);
    }

    public void stopMake() {
        if (mMakeProcess != null) {
            mMakeProcess.destroy();
            mMakeProcess = null;

        }
    }

    public ArrayList<String> getLunchMenuList() {
        return mLunchMenuList;
    }

    public void setOneShotMakefile(String directory) {
        mOneShotMakefile = Utils.pathJoin(directory, Utils.ANDROID_MK);
        mTarget = "all_modules";
    }

    public void setTarget(String target) {
        mOneShotMakefile = null;
        mTarget = target;
    }

    public void setTargetBuildVariant(String targetBuildVariant) {
        mTargetBuildVariant = targetBuildVariant;
        updateOutDir();
    }

    public void setTargetProduct(String targetProduct) {
        mTargetProduct = targetProduct;
        updateOutDir();
    }

    public void setJobNumber(int jobNumber) {
        mJobNumber = jobNumber;
    }

    private void updateOutDir() {
        if (mTargetProduct == null || mTargetBuildVariant == null) {
            return;
        }
        mOutDir = Utils.join('-', "out", mTargetProduct, mTargetBuildVariant);
        mShellCommandExecutor.environment().put("OUT_DIR", mOutDir);
        if (mOutListener != null) {
            mOutListener.onOutDirChanged(mOutDir);
        }
        findProductOutPath();
    }

    public String getOutDir() {
        return mOutDir;
    }

    public void setExtraArguments(String args) {
        mExtraArguments = args.trim();
    }

    public void setVerbose(boolean isVerbose) {
        mIsVerbose = isVerbose;
    }

    private void updateLunchMenu() {
        mLunchMenuList.clear();
        ArrayList<String> lunchCommand = new ArrayList<>();
        lunchCommand.add("source build/envsetup.sh > /dev/null ;" +
                "printf '%s\\n' ${LUNCH_MENU_CHOICES[@]} | cut -f 1 -d - | sort -u");
        mShellCommandExecutor.executeInBash(lunchCommand, new ShellCommandExecutor.ResultReceiver() {
            @Override
            public void newOut(String line) {
                if (!"".equals(line)) {
                    String[] lunchMenus = line.split(" ");
                    for (String lunchMenu : lunchMenus) mLunchMenuList.add(lunchMenu);
                }
            }

            @Override
            public void newError(String line) {

            }

            @Override
            public void onExit(int code) {
                mLunchDoneListener.lunchDone(mLunchMenuList);
            }
        });;
    }

    private void findProductOutPath() {
        String selectedTarget = mTargetProduct + '-' + mTargetBuildVariant;
        ArrayList<String> command = new ArrayList<>();
        command.add("source build/envsetup.sh > /dev/null;" +
                " lunch " + selectedTarget + " > /dev/null; echo $ANDROID_PRODUCT_OUT");
        if (mProductOutRetriever != null) {
            mProductOutRetriever.destroy();
        }
        mProductOutRetriever = mShellCommandExecutor.executeInBash(command, new ShellCommandExecutor.ResultReceiver() {
            @Override
            public void newOut(String line) {
                if (mOutListener != null) {
                    mOutListener.onAndroidProductOutChanged(line);
                }
                mProductOutRetriever = null;
            }

            @Override
            public void newError(String line) {}

            @Override
            public void onExit(int code) {}
        });
    }

    public interface LunchDoneListener {
        public void lunchDone(ArrayList<String> mLunchMenuList);
    }

    public void setOutDirListener(OutPathChangedListener listener) {
        mOutListener = listener;
    }

    private OutPathChangedListener mOutListener;

    public interface OutPathChangedListener {
        void onOutDirChanged(String path);
        void onAndroidProductOutChanged(String path);
    }
}
