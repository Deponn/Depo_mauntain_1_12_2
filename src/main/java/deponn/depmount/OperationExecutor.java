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

/**
 * ティック分散処理を行うためのタスク管理システム
 * タスクをキューに入れて毎ティック処理を行う
 */
public class OperationExecutor {
    private static class OperationTask {
        /**
         * 操作オブジェクト
         */
        public Operation operation;
        /**
         * メッセージ出力を行う関数、進捗状況を出力する
         */
        public final Consumer<List<String>> message;
        /**
         * エラー時呼び出される関数、エラーを表示する
         */
        public final Consumer<WorldEditException> fail;
        /**
         * 操作完了時に呼び出される関数、後の処理につなげる。戻り値がfalseの場合一連の操作をすべて中止する
         */
        public final Supplier<Boolean> success;

        public OperationTask(Operation operation, Consumer<List<String>> message, Consumer<WorldEditException> fail, Supplier<Boolean> success) {
            this.operation = operation;
            this.message = message;
            this.fail = fail;
            this.success = success;
        }
    }

    private BukkitTask task;
    private final Deque<OperationTask> queue = new ConcurrentLinkedDeque<>();

    /**
     * 操作オブジェクトをキューに追加し、ティック分散処理を行う
     *
     * @param operation 操作オブジェクト
     * @param message   メッセージ出力を行う関数、進捗状況を出力する
     * @param fail      エラー時呼び出される関数、エラーを表示する
     * @param success   操作完了時に呼び出される関数、後の処理につなげる。戻り値がfalseの場合一連の操作をすべて中止する
     */
    public void run(Operation operation, Consumer<List<String>> message, Consumer<WorldEditException> fail, Supplier<Boolean> success) {
        // キューに操作を追加
        queue.offer(new OperationTask(operation, message, fail, success));
    }

    /**
     * タスク処理を開始する
     * run関数でキューに追加しても、startを呼び出さないと機能しない
     */
    public void start() {
        // タスクを開始
        task = new BukkitRunnable() {
            // 現在処理中のタスク
            private OperationTask current;
            // メッセージ (オブジェクトを使い回すため)
            private final List<String> messages = new ArrayList<>();

            @Override
            public void run() {
                // 処理中のタスクがないときはキューから取得する
                if (current == null) {
                    current = queue.poll();
                }

                // キューにタスクがないときは待つ
                if (current == null) {
                    return;
                }

                // 時間を取得する (ストップウォッチ開始)
                long startTime = System.currentTimeMillis();
                try {
                    // 操作を処理する
                    current.operation = current.operation.resume(new RunContext() {
                        // 操作を中断するべきか決定する
                        @Override
                        public boolean shouldContinue() {
                            // 1秒立っていたら休憩 (鯖のティックを1秒まで止めることを許容する)
                            long now = System.currentTimeMillis();
                            return now - startTime < 1000;
                        }
                    });
                    if (current.operation != null) {
                        // 操作が続く場合、メッセージを出力する
                        messages.clear();
                        current.operation.addStatusMessages(messages);
                        current.message.accept(messages);
                    } else {
                        // 操作が終わった場合、操作完了時に呼び出される関数を呼び出し、falseの場合一連の操作をすべて中止する
                        if (!current.success.get()) {
                            stop();
                        }
                        // 現在処理中のタスクはなし
                        current = null;
                    }
                } catch (WorldEditException e) {
                    // エラーが発生した場合一連の操作をすべて中止する
                    current.fail.accept(e);
                    current = null;
                    stop();
                }
            }
        }
                // 4ティックに1回処理を行う (これでもかなり負荷がかかる)
                .runTaskTimer(JavaPlugin.getPlugin(DepMount.class), 0, 4);
    }

    /**
     * 一連の操作をすべて中止する
     */
    public void stop() {
        queue.clear();
        if (task != null) {
            task.cancel();
        }
    }
}