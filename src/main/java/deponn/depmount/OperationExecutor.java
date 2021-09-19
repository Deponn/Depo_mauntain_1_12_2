package deponn.depmount;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.RunContext;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class OperationExecutor {
    private static class OperationTask {
        public Operation operation;
        public final Consumer<List<String>> message;
        public final Consumer<WorldEditException> fail;
        public final Supplier<Boolean> success;

        public OperationTask(Operation operation, Consumer<List<String>> message, Consumer<WorldEditException> fail, Supplier<Boolean> success) {
            this.operation = operation;
            this.message = message;
            this.fail = fail;
            this.success = success;
        }
    }

    private BukkitTask task;
    private Deque<OperationTask> queue = new ConcurrentLinkedDeque<>();

    public void run(Operation operation, Consumer<List<String>> message, Consumer<WorldEditException> fail, Supplier<Boolean> success) {
        queue.offer(new OperationTask(operation, message, fail, success));
    }

    public void start() {
        task = new BukkitRunnable() {
            private OperationTask current;
            private final List<String> messages = new ArrayList<>();

            @Override
            public void run() {
                if (current == null) {
                    current = queue.poll();
                }

                if (current == null) {
                    return;
                }

                long startTime = System.currentTimeMillis();
                try {
                    current.operation = current.operation.resume(new RunContext() {
                        @Override
                        public boolean shouldContinue() {
                            long now = System.currentTimeMillis();
                            return now - startTime < 1000;
                        }
                    });
                    if (current.operation != null) {
                        messages.clear();
                        current.operation.addStatusMessages(messages);
                        current.message.accept(messages);
                    } else {
                        if (!current.success.get()) {
                            queue.clear();
                            stop();
                        }
                        current = null;
                    }
                } catch (WorldEditException e) {
                    current.fail.accept(e);
                    queue.clear();
                    current = null;
                    stop();
                }
            }
        }.runTaskTimer(JavaPlugin.getPlugin(DepMount.class), 0, 1);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
    }
}