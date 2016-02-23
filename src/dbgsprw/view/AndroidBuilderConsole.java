package dbgsprw.view;

import com.intellij.execution.filters.HyperlinkInfo;
import com.intellij.execution.filters.OpenFileHyperlinkInfo;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.execution.ui.RunnerLayoutUi;
import com.intellij.execution.ui.layout.PlaceInGrid;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import dbgsprw.core.ShellCommandExecutor;
import dbgsprw.core.Utils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ganadist on 16. 2. 23.
 */
public class AndroidBuilderConsole implements Disposable, ShellCommandExecutor.ResultReceiver {
    private static final String CONSOLE_ID = "Android Builder Console";
    private static final String TOOL_WINDOW_ID = "Android Build";
    private final Project mProject;

    private ConsoleView mConsoleView;
    private final JPanel mPanel = new JPanel(new BorderLayout());
    private ToolWindow mWindow;

    AndroidBuilderConsole(Project project) {
        mProject = project;
        setupToolWindow(project);
    }

    private void setupToolWindow(@NotNull Project project) {
        mWindow = ToolWindowManager.getInstance(project).registerToolWindow(TOOL_WINDOW_ID, false,
                ToolWindowAnchor.BOTTOM, this, true);
        final RunnerLayoutUi.Factory factory = RunnerLayoutUi.Factory.getInstance(project);
        final RunnerLayoutUi layoutUi = factory.create("", "", "session", project);

        mConsoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole();
        Disposer.register(this, mConsoleView);
        final Content console = layoutUi.createContent(CONSOLE_ID, mConsoleView.getComponent(),
                "", null, null);
        layoutUi.addContent(console, 0, PlaceInGrid.right, false);

        final JComponent uiComponent = layoutUi.getComponent();
        mPanel.add(uiComponent, BorderLayout.CENTER);

        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(uiComponent, "", false);
        mWindow.getContentManager().addContent(content);
    }

    public void show(boolean focus) {
        mWindow.activate(null, focus);
    }

    public AndroidBuilderConsole run(ExitListener listener) {
        assert(mExitListener == null);
        mExitListener = listener;
        mConsoleView.clear();
        show(true);
        return this;
    }

    public void print(String line) {
        mConsoleView.print(line, ConsoleViewContentType.NORMAL_OUTPUT);
        mConsoleView.print("\n", ConsoleViewContentType.NORMAL_OUTPUT);
    }

    @Override
    public void newOut(String line) {
        print(line);
    }

    private static final String FILE_POSITION_REGEX = "(.+?):(\\d+):(?:(\\d+):)? (.*)$";
    private static final Pattern FILE_POSITION_PATTERN = Pattern.compile(FILE_POSITION_REGEX);
    private static final String SEPERATOR = ": ";

    @Override
    public void newError(String line) {
        final Matcher m = FILE_POSITION_PATTERN.matcher(line);;
        if (m.find()) {
            final String filename = m.group(1);
            final int lineNo = Integer.parseInt(m.group(2)) - 1;
            final int column = m.group(3) == null ? -1 : (Integer.parseInt(m.group(3)) - 1);
            final String message = m.group(4);
            final String location = m.group(0).substring(0, m.start(4) - SEPERATOR.length());

            final VirtualFile file = VfsUtil.findRelativeFile(filename, mProject.getBaseDir());
            if (file == null) {
                mConsoleView.print(line, ConsoleViewContentType.ERROR_OUTPUT);
            } else {
                final HyperlinkInfo linkInfo = new OpenFileHyperlinkInfo(mProject,
                        file, lineNo, column);

                mConsoleView.printHyperlink(location, linkInfo);
                mConsoleView.print(SEPERATOR, ConsoleViewContentType.ERROR_OUTPUT);
                mConsoleView.print(message, ConsoleViewContentType.ERROR_OUTPUT);
            }
        } else {
            mConsoleView.print(line, ConsoleViewContentType.ERROR_OUTPUT);
        }
        mConsoleView.print("\n", ConsoleViewContentType.ERROR_OUTPUT);
    }

    private void showNotification(String message, NotificationType type) {
        Notifications.Bus.notify(new Notification("Android Builder", "Android Builder", message, type));
    }

    @Override
    public void onExit(int code) {
        if (mExitListener != null) {
            mExitListener.onExit();
            mExitListener = null;
        }
        if (code == 0) {
            mWindow.hide(null);
        } else if (code < 128) {
            final String message = "execution is failed with exit code: " + code;
            showNotification(message, NotificationType.ERROR);
            mConsoleView.print(message, ConsoleViewContentType.SYSTEM_OUTPUT);
            mConsoleView.print("\n", ConsoleViewContentType.SYSTEM_OUTPUT);
        }
    }

    private ExitListener mExitListener;

    @Override
    public void dispose() {

    }

    public interface ExitListener {
        void onExit();
    }
}
