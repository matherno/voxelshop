package com.vitco.app.manager.async;

import com.vitco.app.manager.thread.LifeTimeThread;
import com.vitco.app.manager.thread.ThreadManagerInterface;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages Async Actions
 */
public class AsyncActionManager {

    private ThreadManagerInterface threadManager;
    // set the action handler
    @Autowired
    public final void setThreadManager(ThreadManagerInterface threadManager) {
        this.threadManager = threadManager;
    }

    // list of new action
    private final ArrayList<AsyncAction> newActions = new ArrayList<AsyncAction>();

    // list of actions
    private final ArrayList<String> stack = new ArrayList<String>();
    // retry to execute when the main stack is empty
    private final ArrayList<String> idleStack = new ArrayList<String>();

    // list of current action names
    private final HashMap<String, AsyncAction> actionNames = new HashMap<String, AsyncAction>();

    public final void removeAsyncAction(String actionName) {
        if (null != actionNames.remove(actionName)) {
            stack.remove(actionName);
            idleStack.remove(actionName);
        }
    }

    // Note: re-adding an action does not ensure that the action
    // is at the end of the queue!
    public final void addAsyncAction(AsyncAction action) {
        synchronized (newActions) {
            newActions.add(action);
        }
        synchronized (workerThread) {
            workerThread.notify();
        }
    }

    // needs to be one as those tasks can not be executed in parallel!
    // Note: ExecutorService is much faster than using a new thread to
    // execute each AsyncAction
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final LifeTimeThread workerThread = new LifeTimeThread() {

        @Override
        public void onAfterStop() {
            executor.shutdown();
            // Wait until all threads are finish
            //noinspection StatementWithEmptyBody
            while (!executor.isTerminated()) {}
        }

        @Override
        public void loop() throws InterruptedException {
            // add new tasks
            synchronized (newActions) {
                if (!newActions.isEmpty()) {
                    for (AsyncAction action : newActions) {
                        String actionName = action.name;
                        if (null == actionNames.put(actionName, action)) {
                            stack.add(actionName);
                        }
                    }
                    newActions.clear();
                }
            }
            // handle stack execution
            if (!stack.isEmpty()) {
                // fetch action
                String actionName = stack.remove(0);
                final AsyncAction action = actionNames.get(actionName);
                if (action.ready()) {
                    // remove first in case the action adds
                    // itself to the cue again (e.g. for refreshWorld())
                    actionNames.remove(actionName);
                    executor.execute(action);
                } else {
                    idleStack.add(actionName);
                }
            } else {
                // -- the main stack is empty
                // add <ready> idle stack back to main stack
                if (!idleStack.isEmpty()) {
                    for (String asyncAction : idleStack) {
                        if (actionNames.get(asyncAction).ready()) {
                            stack.add(asyncAction);
                        }
                    }
                    idleStack.removeAll(stack);
                }

                if (stack.isEmpty()) {
                    synchronized (workerThread) {
                        // sometimes notify is "too early"/fails(?), so we need a timeout here
                        if (idleStack.isEmpty()) {
                            // no actions waiting, we can be "lazy"
                            workerThread.wait(500);
                        } else {
                            // there are some actions not ready yet, so we should check often
                            workerThread.wait(50);
                        }
                    }
                }
            }
        }
    };

    @PostConstruct
    public void init() {
        threadManager.manage(workerThread);
    }
}
