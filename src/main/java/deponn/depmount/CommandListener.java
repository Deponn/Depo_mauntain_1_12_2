package deponn.depmount;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.extension.platform.AbstractPlayerActor;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * コマンド処理クラス
 *
 * @note WorldEditがない環境でWorldEditのクラスを読み込まないよう、別クラスに移動
 */
public class CommandListener implements CommandExecutor, TabCompleter {
    // コマンドを実際に処理
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        // プレイヤーがコマンドを投入した際の処理...
        if (cmd.getName().equalsIgnoreCase("/mountain")) {
            // プレイヤーチェック
            if (!(sender instanceof Player)) {
                // コマブロやコンソールからの実行の場合
                sender.sendMessage(ChatColor.RED + "このコマンドはプレイヤーのみが使えます。");
                return true;
            }
            Player player = (Player) sender;

            // WorldEditを取得
            WorldEditPlugin worldEdit = JavaPlugin.getPlugin(WorldEditPlugin.class);
            AbstractPlayerActor wPlayer = worldEdit.wrapPlayer(player);
            World wWorld = wPlayer.getWorld();

            // プレイヤーセッション
            LocalSession session = WorldEdit.getInstance()
                    .getSessionManager()
                    .get(wPlayer);

            if (!session.isSelectionDefined(wWorld)) {
                // 範囲が選択されていない場合
                sender.sendMessage(ChatColor.RED + "WorldEditの範囲が選択されていません。");
                return true;
            }

            Region region;
            try {
                region = session.getSelection(wWorld);
            } catch (WorldEditException e) {
                // 範囲が不完全です
                sender.sendMessage(ChatColor.RED + "WorldEditの範囲が不完全です。");
                return true;
            }

            //コマンド引数を処理
            CommandParser parser = CommandParser.parseCommand(sender, args);
            if (!parser.isSuccess) {
                // パース失敗
                return true;
            }

            // 範囲を設定
            CuboidRegion bound = new CuboidRegion(region.getWorld(), region.getMinimumPoint(), region.getMaximumPoint());
            //bound.expand(
            //        new Vector(0, (bound.getWorld().getMaxY() + 1), 0),
            //        new Vector(0, -(bound.getWorld().getMaxY() + 1), 0));

            // 範囲中のラピスラズリブロックの位置を座標指定型で記録
            int[][] heightmapArray = new int[bound.getWidth()][bound.getLength()];
            // 範囲中のラピスラズリブロックの位置をリストとして記録
            ArrayList<ControlPointData> heightControlPoints = new ArrayList<ControlPointData>();

            // 複数ティックに分けて操作をするための準備
            OperationExecutor executor = new OperationExecutor();
            executor.start();

            // ラピスラズリブロックを目印として、範囲中のデータを取得
            executor.run(
                    MountOperation.collectSurfacePoints(wWorld, bound, parser.bCollectBorder, heightmapArray, heightControlPoints),
                    s -> s.forEach(str -> sender.sendMessage(ChatColor.GREEN + "ラピスラズリの位置を取得中... " + str)),
                    e -> sender.sendMessage(ChatColor.RED + "ラピスブロック位置の取得中にエラーが発生しました。"),
                    () -> {
                        // 距離が近い順にk個取り出す。ただし、numInterpolationPointsArg=0の時は全部
                        int size = heightControlPoints.size();
                        int maxi;
                        if (parser.numInterpolationPoints == 0) {
                            if (size == 0) {
                                sender.sendMessage(ChatColor.RED + "最低一つはラピスラズリブロックをおいてください。");
                                return false;
                            }
                            maxi = size;
                        } else {
                            if (size < parser.numInterpolationPoints) {
                                sender.sendMessage(ChatColor.RED + "kより多いラピスラズリブロックをおいてください。");
                                return false;
                            }
                            maxi = parser.numInterpolationPoints;
                        }

                        // 地形の補間計算
                        executor.run(
                                MountOperation.interpolateSurface(maxi, bound, heightmapArray, heightControlPoints),
                                s -> s.forEach(str -> sender.sendMessage(ChatColor.GREEN + "地形補間を計算中... " + str)),
                                e -> sender.sendMessage(ChatColor.RED + "地形補間の計算中にエラーが発生しました。"),
                                () -> {
                                    // ブロック変更開始 (WorldEditのUndoに登録される)
                                    EditSession editSession = worldEdit.createEditSession(player);
                                    // 範囲中の地形を実際に改変
                                    executor.run(
                                            MountOperation.applySurface(editSession, wWorld, parser.bReplaceAll, bound, heightmapArray),
                                            s -> s.forEach(str -> sender.sendMessage(ChatColor.GREEN + "ブロックを設置中... " + str)),
                                            e -> sender.sendMessage(ChatColor.RED + e.getMessage()),
                                            () -> {
                                                sender.sendMessage(ChatColor.GREEN + "ブロックを反映中...");
                                                editSession.flushQueue();
                                                session.remember(editSession);
                                                sender.sendMessage(ChatColor.GREEN + "設置完了");
                                                return false;
                                            }
                                    );

                                    return true;
                                }
                        );

                        return true;
                    }
            );

            return true;
        }

        // コマンドが実行されなかった場合は、falseを返して当メソッドを抜ける。
        return false;

        // done undo がシングルのみ対応、done あと、向きが分かりにくい.done k実装、done 空気のみに作用させるか done ラピスラズリブロックなかったとき done境界条件
    }

    // コマンドのTAB補完の実装
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return null;
    }
}
