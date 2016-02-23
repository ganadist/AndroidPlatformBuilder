package dbgsprw.view;

import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.execution.ui.RunnerLayoutUi;
import com.intellij.execution.ui.layout.PlaceInGrid;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import dbgsprw.core.ShellCommandExecutor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * Created by ganadist on 16. 2. 23.
 */
public class AndroidBuilderConsole implements Disposable, ShellCommandExecutor.ResultReceiver {
    private static final String CONSOLE_ID = "Android Builder Console";
    private static final String TOOL_WINDOW_ID = "Android Build";
    private final Project mProject;

    private ConsoleViewImpl mConsoleView;
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

        mConsoleView = new ConsoleViewImpl(project, GlobalSearchScope.allScope(project), false, false);
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
        mConsoleView.print(line + "\n", ConsoleViewContentType.NORMAL_OUTPUT);
    }

    @Override
    public void newOut(String line) {
        print(line);
    }

    @Override
    public void newError(String line) {
        mConsoleView.print(line + "\n", ConsoleViewContentType.ERROR_OUTPUT);
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
            final String message = "build failed with exit code: " + code;
            showNotification(message, NotificationType.ERROR);
            mConsoleView.print(message + "\n", ConsoleViewContentType.SYSTEM_OUTPUT);
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
