package deponn.depmount;

import com.sk89q.worldedit.*;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.extension.platform.AbstractPlayerActor;
import com.sk89q.worldedit.function.mask.BlockMask;
import com.sk89q.worldedit.function.mask.Masks;
import com.sk89q.worldedit.patterns.Pattern;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
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
            com.sk89q.worldedit.world.World wWorld = wPlayer.getWorld();

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

            Location loc = player.getLocation();

            //コマンド引数を処理
            List<String> argsList = Arrays.asList(args);
            boolean bReplaceAll = false;
            boolean bCollectBorder = false;
            int numInterpolationPoints = 0;
            if (argsList.contains("-a") || argsList.contains("--all")) {
                // 全置き換えモード、trueの場合空気ブロック以外も置き換える
                bReplaceAll = true;
            }
            if (argsList.contains("-b") || argsList.contains("--border")) {
                // 境界にラピスラズリブロック配置モード、trueの場合境界にラピスラズリブロックをおく
                bCollectBorder = true;
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
                    numInterpolationPoints = Integer.parseInt(argsList.get(index + 1));
                    if (numInterpolationPoints < 0) {
                        sender.sendMessage(ChatColor.RED + "数値は正の数である必要があります。 -n <数字>");
                        return false;
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "数値が不正です。 -n <数字>");
                    return false;
                }
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

            // ラピスラズリブロックを目印として、範囲中のデータを取得
            collectSurfacePoints(loc.getWorld(), bound, bCollectBorder, heightmapArray, heightControlPoints);

            // 距離が近い順にk個取り出す。ただし、numInterpolationPoints=0の時は全部
            int size = heightControlPoints.size();
            int maxi;
            if (numInterpolationPoints == 0) {
                if (size == 0) {
                    sender.sendMessage("最低一つはラピスラズリブロックをおいてください。");
                    return true;
                }
                maxi = size;
            } else {
                if (size < numInterpolationPoints) {
                    sender.sendMessage("kより多いラピスラズリブロックをおいてください。");
                    return true;
                }
                maxi = numInterpolationPoints;
            }

            // 地形の補間計算
            interpolateSurface(maxi, bound, heightmapArray, heightControlPoints);

            // ブロック変更開始 (WorldEditのUndoに登録される)
            EditSession editSession = worldEdit.createEditSession(player);
            try {
                // 範囲中の地形を実際に改変
                applySurface(editSession, wWorld, bReplaceAll, bound, heightmapArray);
            } catch (RuntimeException e) {
                // 最大設置数制限超過
                sender.sendMessage(e.getMessage());
                return true;
            } finally {
                editSession.flushQueue();
                session.remember(editSession);
            }

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

    private interface RegionLoop {
        /**
         * x,z座標ごとにyHeightを変更する
         *
         * @param xPoint  x座標
         * @param zPoint  z座標
         * @param yHeight ハイトマップの高さ、未定義は-1
         * @return 更新後のyHeight
         */
        int loop(int xPoint, int zPoint, int yHeight);

        /**
         * x,z座標ごとにループを行う
         *
         * @param region         範囲
         * @param heightmapArray ハイトマップデータ
         * @param func           処理関数
         */
        static void forEach(CuboidRegion region, int[][] heightmapArray, RegionLoop func) {
            int x1 = region.getMinimumPoint().getBlockX();
            int z1 = region.getMinimumPoint().getBlockZ();
            int x2 = region.getMaximumPoint().getBlockX();
            int z2 = region.getMaximumPoint().getBlockZ();
            // x座標方向のループ
            for (int xPoint = x1; xPoint <= x2; xPoint++) {
                // z座標方向のループ
                for (int zPoint = z1; zPoint <= z2; zPoint++) {
                    int top = heightmapArray[xPoint - x1][zPoint - z1];
                    top = func.loop(xPoint, zPoint, top);
                    heightmapArray[xPoint - x1][zPoint - z1] = top;
                }
            }
        }
    }

    private void applySurface(EditSession editSession, com.sk89q.worldedit.world.World wWorld, boolean bReplaceAll, CuboidRegion region, int[][] heightmapArray) {
        BaseBlock lapis = new BaseBlock(BlockID.LAPIS_LAZULI_BLOCK);
        BaseBlock air = new BaseBlock(BlockID.AIR);
        BaseBlock grass = new BaseBlock(BlockID.GRASS);
        BaseBlock dirt = new BaseBlock(BlockID.DIRT);
        BaseBlock stone = new BaseBlock(BlockID.STONE);
        int y1 = region.getMinimumPoint().getBlockY();
        int y2 = region.getMaximumPoint().getBlockY();
        RegionLoop.forEach(region, heightmapArray, (xPoint, zPoint, top) -> {
            try {
                // 縦長の範囲を置き換えする
                // ラピスラズリブロックを消去したうえで、標高の地点まで土を盛っていく
                editSession.replaceBlocks(
                        new CuboidRegion(wWorld, new BlockVector(xPoint, y1, zPoint), new BlockVector(xPoint, y2, zPoint)),
                        // bReplaceAllがtrueのとき全て書き換え、falseのとき空気のみ書き換える。
                        bReplaceAll ? Masks.alwaysTrue() : new BlockMask(editSession, air, lapis),
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
            } catch (MaxChangedBlocksException e) {
                throw new RuntimeException("最大ブロック数制限を超えました", e);
            }
            return top;
        });
    }

    private void interpolateSurface(int maxi, CuboidRegion region, int[][] heightmapArray, ArrayList<ControlPointData> heightControlPoints) {
        RegionLoop.forEach(region, heightmapArray, (xPoint, zPoint, top) -> {
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

    private static void collectSurfacePoints(World world, CuboidRegion region, boolean bCollectBorder, int[][] heightmapArray, ArrayList<ControlPointData> heightControlPoints) {
        int x1 = region.getMinimumPoint().getBlockX();
        int y1 = region.getMinimumPoint().getBlockY();
        int z1 = region.getMinimumPoint().getBlockZ();
        int x2 = region.getMaximumPoint().getBlockX();
        int y2 = region.getMaximumPoint().getBlockY();
        int z2 = region.getMaximumPoint().getBlockZ();
        RegionLoop.forEach(region, heightmapArray, (xPoint, zPoint, top) -> {
            // (x,z)におけるラピスラズリブロックのうち最高を記録。なければ-1を代入
            top = -1;
            // y座標方向のループ
            for (int yPoint = y2; yPoint >= y1; yPoint--) {
                // ループで処理する座標のブロックを取得します。
                Block currentBlock = new Location(world, xPoint, yPoint, zPoint).getBlock();
                if (currentBlock.getType() == Material.LAPIS_BLOCK) {
                    top = yPoint;
                    break;
                }
                // ボーダーモードがONかつ、座標が縁で空気以外のブロックがあれば、それをラピスブロックとして扱う
                else if (bCollectBorder
                        && (xPoint == x1 || xPoint == x2 || zPoint == z1 || zPoint == z2)
                        && (currentBlock.getType() != Material.AIR)) {
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
