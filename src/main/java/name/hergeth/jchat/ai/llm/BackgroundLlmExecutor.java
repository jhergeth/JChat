package name.hergeth.jchat.ai.llm;

import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Serializes background LLM calls (extraction, search-extract) so they do not
 * stampede the same Bifrost backend / GPU with parallel requests.
 */
@Singleton
public class BackgroundLlmExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(BackgroundLlmExecutor.class);

    private final AtomicInteger pending = new AtomicInteger();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "jchat-background-llm");
        thread.setDaemon(true);
        return thread;
    });

    public void run(String label, Runnable task) {
        pending.incrementAndGet();
        executor.execute(() -> {
            try {
                task.run();
            } catch (Exception e) {
                LOG.warn("Background LLM task '{}' failed", label, e);
            } finally {
                pending.decrementAndGet();
            }
        });
    }

    public int pendingCount() {
        return pending.get();
    }

    public void awaitIdle(Duration timeout) throws InterruptedException {
        long deadlineNanos = System.nanoTime() + timeout.toNanos();
        while (pending.get() > 0 && System.nanoTime() < deadlineNanos) {
            Thread.sleep(100);
        }
    }
}
