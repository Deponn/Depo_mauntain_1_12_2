package deponn.depmount;

import com.sk89q.worldedit.*;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.extension.platform.AbstractPlayerActor;
import com.sk89q.worldedit.function.mask.BlockMask;
import com.sk89q.worldedit.function.mask.FuzzyBlockMask;
import com.sk89q.worldedit.function.mask.Masks;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.patterns.Pattern;
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
                    collectSurfacePoints(wWorld, bound, bCollectBorder, heightmapArray, heightControlPoints),
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
                                interpolateSurface(maxi, bound, heightmapArray, heightControlPoints),
                                s -> s.forEach(str -> sender.sendMessage(ChatColor.GREEN + "地形補間を計算中... " + str)),
                                e -> sender.sendMessage(ChatColor.RED + "地形補間の計算中にエラーが発生しました。"),
                                () -> {
                                    // ブロック変更開始 (WorldEditのUndoに登録される)
                                    EditSession editSession = worldEdit.createEditSession(player);
                                    // 範囲中の地形を実際に改変
                                    executor.run(
                                            applySurface(editSession, wWorld, bReplaceAll, bound, heightmapArray),
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

    /**
     * ハイトマップx,z地点における高さyのデータ
     */
    private static class ControlPointData {
        public final int xPoint, zPoint;
        public final int yHeight;

        public ControlPointData(int xPoint, int zPoint, int yHeight) {
            this.xPoint = xPoint;
            this.zPoint = zPoint;
            this.yHeight = yHeight;
        }
    }

    /**
     * 距離の二乗と高さの関係
     */
    private static class DistanceHeightData {
        public final double xzDistance;
        public final double yHeight;

        public DistanceHeightData(double xzDistance, double yHeight) {
            this.xzDistance = xzDistance;
            this.yHeight = yHeight;
        }
    }

    private static Operation applySurface(EditSession editSession, World wWorld, boolean bReplaceAll, CuboidRegion region, int[][] heightmapArray) {
        BaseBlock lapis = new BaseBlock(BlockID.LAPIS_LAZULI_BLOCK);
        BaseBlock air = new BaseBlock(BlockID.AIR);
        BaseBlock grass = new BaseBlock(BlockID.GRASS);
        BaseBlock dirt = new BaseBlock(BlockID.DIRT);
        BaseBlock stone = new BaseBlock(BlockID.STONE);
        int y1 = region.getMinimumPoint().getBlockY();
        int y2 = region.getMaximumPoint().getBlockY();
        return new RegionOperation(region, heightmapArray, (xPoint, zPoint, top, context) -> {
            // 縦長の範囲を置き換えする
            // ラピスラズリブロックを消去したうえで、標高の地点まで土を盛っていく
            editSession.replaceBlocks(
                    new CuboidRegion(
                            wWorld,
                            new BlockVector(xPoint, y1, zPoint),
                            new BlockVector(xPoint, bReplaceAll ? y2 : top, zPoint)
                    ),
                    // bReplaceAllがtrueのとき全て書き換え、falseのとき空気のみ書き換える。
                    bReplaceAll ? Masks.alwaysTrue() : new FuzzyBlockMask(editSession, air, lapis),
                    new Pattern() {
                        @Override
                        public BaseBlock next(Vector position) {
                            int yPoint = position.getBlockY();
                            if (top - yPoint < 0) {
                                return air;
                            } else if (top - yPoint < 1) {
                                return grass;
                            } else if (top - yPoint < 5) {
                                return dirt;
                            } else {
                                return stone;
                            }
                        }

                        @Override
                        public BaseBlock next(int x, int y, int z) {
                            return next(new Vector(x, y, z));
                        }
                    });
            return top;
        });
    }

    private static Operation interpolateSurface(int maxi, CuboidRegion region, int[][] heightmapArray, ArrayList<ControlPointData> heightControlPoints) {
        return new RegionOperation(region, heightmapArray, (xPoint, zPoint, top, context) -> {
            // ラピスラズリブロックがなかった場合、k近傍法を参考にし、y=sum(yn/((x-xn)^2+(z-zn)^2))/sum(1/((x-xn)^2+(z-zn)^2))で標高計算。あった場合そのy座標が標高
            if (top == -1) {
                // 距離のリストに変換。
                ArrayList<DistanceHeightData> trainingFixedList = new ArrayList<DistanceHeightData>();
                for (ControlPointData line : heightControlPoints) {
                    trainingFixedList.add(new DistanceHeightData(
                            Math.pow(xPoint - line.xPoint, 2) + Math.pow(zPoint - line.zPoint, 2),
                            line.yHeight
                    ));
                }
                // 距離順にする
                sort2DListCol(trainingFixedList, heightControlPoints.size());

                // 計算
                double numerator = 0;
                double denominator = 0;
                for (int i = 0; i < maxi; i++) {
                    numerator += trainingFixedList.get(i).yHeight / trainingFixedList.get(i).xzDistance;
                    denominator += 1 / trainingFixedList.get(i).xzDistance;
                }
                // ハイトマップを補間
                top = (int) Math.floor(numerator / denominator);
            }
            return top;
        });
    }

    private static Operation collectSurfacePoints(World wWorld, CuboidRegion region, boolean bCollectBorder, int[][] heightmapArray, ArrayList<ControlPointData> heightControlPoints) {
        int x1 = region.getMinimumPoint().getBlockX();
        int y1 = region.getMinimumPoint().getBlockY();
        int z1 = region.getMinimumPoint().getBlockZ();
        int x2 = region.getMaximumPoint().getBlockX();
        int y2 = region.getMaximumPoint().getBlockY();
        int z2 = region.getMaximumPoint().getBlockZ();
        return new RegionOperation(region, heightmapArray, (xPoint, zPoint, top, context) -> {
            // (x,z)におけるラピスラズリブロックのうち最高を記録。なければ-1を代入
            top = -1;
            // y座標方向のループ
            for (int yPoint = y2; yPoint >= y1; yPoint--) {
                // ループで処理する座標のブロックを取得します。
                BaseBlock currentBlock = wWorld.getLazyBlock(new BlockVector(xPoint, yPoint, zPoint));
                if (currentBlock.getId() == BlockID.LAPIS_LAZULI_BLOCK) {
                    top = yPoint;
                    break;
                }
                // ボーダーモードがONかつ、座標が縁で空気以外のブロックがあれば、それをラピスブロックとして扱う
                else if (bCollectBorder
                        && (xPoint == x1 || xPoint == x2 || zPoint == z1 || zPoint == z2)
                        && (!currentBlock.isAir())) {
                    top = yPoint;
                    break;
                }
            }
            // ラピスラズリブロックがあった場合にリストに記録
            if (top != -1) {
                heightControlPoints.add(new ControlPointData(xPoint, zPoint, top));
            }
            return top;
        });
    }

    public static void sort2DListCol(ArrayList<DistanceHeightData> array, final int columnLength) {
        for (int i = 0; i < columnLength - 1; i++) {
            for (int j = columnLength - 1; j > i; j--) {
                if (array.get(j - 1).xzDistance > array.get(j).xzDistance) {
                    DistanceHeightData tmp = array.get(j - 1);
                    array.set(j - 1, array.get(j));
                    array.set(j, tmp);
                }
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return null;
    }
}
