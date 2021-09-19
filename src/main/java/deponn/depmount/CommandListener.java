package deponn.depmount;

import com.sk89q.worldedit.*;
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

// @note WorldEditがない環境でWorldEditのクラスを読み込まないよう、別クラスに移動
public class CommandListener implements CommandExecutor, TabCompleter {
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
            List<String> argsList = Arrays.asList(args);
            boolean bReplaceAllArg = false;
            boolean bCollectBorderArg = false;
            int numInterpolationPointsArg = 0;
            if (argsList.contains("-a") || argsList.contains("--all")) {
                // 全置き換えモード、trueの場合空気ブロック以外も置き換える
                bReplaceAllArg = true;
            }
            if (argsList.contains("-b") || argsList.contains("--border")) {
                // 境界にラピスラズリブロック配置モード、trueの場合境界にラピスラズリブロックをおく
                bCollectBorderArg = true;
            }
            if (argsList.contains("-n") || argsList.contains("--num")) {
                // 引数が何番目か取得し、若い番号を採用する
                int index = Math.min(argsList.indexOf("-n"), argsList.indexOf("--num"));
                if (index + 1 >= argsList.size()) {
                    // 引数の次がなかった場合、エラー
                    sender.sendMessage(ChatColor.RED + "数値が必要です。 -n <数字>");
                    return true;
                }
                try {
                    // 補間する頂点(ラピスラズリブロック)の数
                    numInterpolationPointsArg = Integer.parseInt(argsList.get(index + 1));
                    if (numInterpolationPointsArg < 0) {
                        sender.sendMessage(ChatColor.RED + "数値は正の数である必要があります。 -n <数字>");
                        return false;
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "数値が不正です。 -n <数字>");
                    return false;
                }
            }
            boolean bReplaceAll = bReplaceAllArg;
            boolean bCollectBorder = bCollectBorderArg;
            int numInterpolationPoints = numInterpolationPointsArg;

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
                    MountOperation.collectSurfacePoints(wWorld, bound, bCollectBorder, heightmapArray, heightControlPoints),
                    s -> s.forEach(str -> sender.sendMessage(ChatColor.GREEN + "ラピスラズリの位置を取得中... " + str)),
                    e -> sender.sendMessage(ChatColor.RED + "ラピスブロック位置の取得中にエラーが発生しました。"),
                    () -> {
                        // 距離が近い順にk個取り出す。ただし、numInterpolationPointsArg=0の時は全部
                        int size = heightControlPoints.size();
                        int maxi;
                        if (numInterpolationPoints == 0) {
                            if (size == 0) {
                                sender.sendMessage(ChatColor.RED + "最低一つはラピスラズリブロックをおいてください。");
                                return false;
                            }
                            maxi = size;
                        } else {
                            if (size < numInterpolationPoints) {
                                sender.sendMessage(ChatColor.RED + "kより多いラピスラズリブロックをおいてください。");
                                return false;
                            }
                            maxi = numInterpolationPoints;
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
                                            MountOperation.applySurface(editSession, wWorld, bReplaceAll, bound, heightmapArray),
                                            s -> s.forEach(str -> sender.sendMessage(ChatColor.GREEN + "ブロックを設置中... " + str)),
                                            e -> sender.sendMessage(ChatColor.RED + e.getMessage()),
                                            () -> {
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return null;
    }
}
