package dev.havoc.havoctab.shared.features.types;

import dev.havoc.havoctab.shared.cpu.ThreadExecutor;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for features that manage everything in their own thread.
 */
public interface CustomThreaded {

    /**
     * Returns the feature's custom thread to execute all tasks in.
     *
     * @return  feature's custom thread to execute all tasks in
     */
    @NotNull
    ThreadExecutor getCustomThread();
}
