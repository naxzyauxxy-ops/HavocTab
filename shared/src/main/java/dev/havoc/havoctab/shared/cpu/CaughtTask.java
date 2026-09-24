package dev.havoc.havoctab.shared.cpu;

import lombok.RequiredArgsConstructor;
import dev.havoc.havoctab.shared.HavocTab;

/**
 * Runnable wrapper that try/catches the task.
 */
@RequiredArgsConstructor
public class CaughtTask implements Runnable {

    /** Task to run */
    private final Runnable task;

    @Override
    public void run() {
        try {
            task.run();
        } catch (Exception | LinkageError | StackOverflowError e) {
            HavocTab.getInstance().getErrorManager().taskThrewError(e);
        }
    }
}
