package main;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Used to execute threads
 */
public class ThreadsExecutor {

    private ExecutorService executorService;
    private Thread guiThread;

    ThreadsExecutor() {
        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    public void execute(Runnable runnable) {
        executorService.execute(runnable);
    }

    public void setGuiThread(Thread thread) {
        guiThread = thread;
        executorService.execute(thread);
    }

    public Thread getGuiThread() {
        return guiThread;
    }

}
