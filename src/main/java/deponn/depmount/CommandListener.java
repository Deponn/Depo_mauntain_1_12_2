package deponn.depmount;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.extension.platform.AbstractPlayerActor;
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
            Block currentBlock;

            //コマンド引数を処理
            List<String> argsList = Arrays.asList(args);
            boolean bReplaceAll = false;
            boolean bPlaceBorder = false;
            int numInterpolationPoints = 0;
            if (argsList.contains("-a") || argsList.contains("--all")) {
                // 全置き換えモード、trueの場合空気ブロック以外も置き換える
                bReplaceAll = true;
            }
            if (argsList.contains("-b") || argsList.contains("--border")) {
                // 境界にラピスラズリブロック配置モード、trueの場合境界にラピスラズリブロックをおく
                bPlaceBorder = true;
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
            bound.expand(
                    new Vector(0, (bound.getWorld().getMaxY() + 1), 0),
                    new Vector(0, -(bound.getWorld().getMaxY() + 1), 0));
            int x1 = bound.getMinimumPoint().getBlockX();
            int y1 = bound.getMinimumPoint().getBlockY();
            int z1 = bound.getMinimumPoint().getBlockZ();
            int x2 = bound.getMaximumPoint().getBlockX();
            int y2 = bound.getMaximumPoint().getBlockY();
            int z2 = bound.getMaximumPoint().getBlockZ();

            // bPlaceBorderがtureのとき、境界にラピスラズリブロックをおく。境界条件。
            if (bPlaceBorder) {
                // x座標方向のループ
                for (int xPoint = x1; xPoint < x2; xPoint++) {
                    int zPoint;
                    zPoint = z1;
                    // y座標方向の逆向きループ
                    for (int yPoint = y2; yPoint > y1; yPoint--) {
                        loc.setX(xPoint);
                        loc.setZ(zPoint);
                        loc.setY(yPoint);
                        currentBlock = loc.getBlock();
                        if (currentBlock.getType() != Material.AIR) {
                            currentBlock.setType(Material.LAPIS_BLOCK);
                            break;
                        }
                    }
                    zPoint = z2 - 1;
                    for (int yPoint = y2; yPoint > y1; yPoint--) {
                        loc.setX(xPoint);
                        loc.setZ(zPoint);
                        loc.setY(yPoint);
                        currentBlock = loc.getBlock();
                        if (currentBlock.getType() != Material.AIR) {
                            currentBlock.setType(Material.LAPIS_BLOCK);
                            break;
                        }
                    }
                }
                // z座標方向のループ
                for (int zPoint = z1; zPoint < z2; zPoint++) {
                    int xPoint;
                    xPoint = x1;
                    // y座標方向の逆向きループ
                    for (int yPoint = y2; yPoint > y1; yPoint--) {
                        loc.setX(xPoint);
                        loc.setZ(zPoint);
                        loc.setY(yPoint);
                        currentBlock = loc.getBlock();
                        if (currentBlock.getType() != Material.AIR) {
                            currentBlock.setType(Material.LAPIS_BLOCK);
                            break;
                        }
                    }
                    xPoint = x2 - 1;
                    for (int yPoint = y2; yPoint > y1; yPoint--) {
                        loc.setX(xPoint);
                        loc.setZ(zPoint);
                        loc.setY(yPoint);
                        currentBlock = loc.getBlock();
                        if (currentBlock.getType() != Material.AIR) {
                            currentBlock.setType(Material.LAPIS_BLOCK);
                            break;
                        }
                    }
                }
            }

            // ラピスラズリブロックを目印として、範囲中のデータを取得
            int[][] heightmapArray = new int[bound.getWidth()][bound.getLength()]; //範囲中のラピスラズリブロックの位置を座標指定型で記録
            ArrayList<ControlPointData> heightControlPoints = new ArrayList<ControlPointData>(); //範囲中のラピスラズリブロックの位置をリストとして記録
            collectSurfacePoints(loc.getWorld(), x1, y1, z1, x2, y2, z2, heightmapArray, heightControlPoints);

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
            interpolateSurface(maxi, x1, z1, x2, z2, heightmapArray, heightControlPoints);

            // 範囲中の地形を実際に改変
            applySurface(loc.getWorld(), bReplaceAll, x1, y1, z1, x2, y2, z2, heightmapArray);

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

    private void applySurface(World world, boolean bReplaceAll, int x1, int y1, int z1, int x2, int y2, int z2, int[][] heightmapArray) {
        // x座標方向のループ
        for (int xPoint = x1; xPoint < x2; xPoint++) {
            // z座標方向のループ
            for (int zPoint = z1; zPoint < z2; zPoint++) {
                // y座標方向のループ
                for (int yPoint = y1; yPoint < y2; yPoint++) {
                    // ハイトマップから高さを取得
                    int top = heightmapArray[xPoint - x1][zPoint - z1];
                    // ループで処理する座標のブロックを取得します。
                    Block currentBlock = new Location(world, xPoint, yPoint, zPoint).getBlock();
                    // ラピスラズリブロックを消去したうえで、標高の地点まで土を盛っていく
                    if (currentBlock.getType() == Material.LAPIS_BLOCK) {
                        currentBlock.setType(Material.AIR);
                    }
                    // bReplaceAllがtrueのとき全て書き換え、falseのとき空気のみ書き換える。
                    if (bReplaceAll) {
                        if (top - yPoint < 0) {
                            currentBlock.setType(Material.AIR);
                        } else if (top - yPoint < 1) {
                            currentBlock.setType(Material.GRASS);
                        } else if (top - yPoint < 5) {
                            currentBlock.setType(Material.DIRT);
                        } else {
                            currentBlock.setType(Material.STONE);
                        }
                    } else {
                        if (top - yPoint < 0) {
                            if (currentBlock.getType() == Material.AIR) {
                                currentBlock.setType(Material.AIR);
                            }
                        } else if (top - yPoint < 1) {
                            if (currentBlock.getType() == Material.AIR) {
                                currentBlock.setType(Material.GRASS);
                            }
                        } else if (top - yPoint < 5) {
                            if (currentBlock.getType() == Material.AIR) {
                                currentBlock.setType(Material.DIRT);
                            }
                        } else {
                            if (currentBlock.getType() == Material.AIR) {
                                currentBlock.setType(Material.STONE);
                            }
                        }
                    }
                }
            }
        }
    }

    private void interpolateSurface(int maxi, int x1, int z1, int x2, int z2, int[][] heightmapArray, ArrayList<ControlPointData> heightControlPoints) {
        // x座標方向のループ
        for (int xPoint = x1; xPoint < x2; xPoint++) {
            // z座標方向のループ
            for (int zPoint = z1; zPoint < z2; zPoint++) {
                double top;
                // ラピスラズリブロックがなかった場合、k近傍法を参考にし、y=sum(yn/((x-xn)^2+(z-zn)^2))/sum(1/((x-xn)^2+(z-zn)^2))で標高計算。あった場合そのy座標が標高
                if (heightmapArray[xPoint - x1][zPoint - z1] == -1) {
                    // 距離のリストに変換。
                    ArrayList<ArrayList<Double>> trainingFixedList = new ArrayList<ArrayList<Double>>();
                    for (ControlPointData line : heightControlPoints) {
                        ArrayList<Double> oneData = new ArrayList<Double>();
                        oneData.add(Math.pow(xPoint - line.xPoint, 2) + Math.pow(zPoint - line.zPoint, 2));
                        oneData.add((double) line.yHeight);
                        trainingFixedList.add(oneData);
                    }
                    // 距離順にする
                    sort2DListCol(trainingFixedList, heightControlPoints.size());

                    // 計算
                    double numerator = 0;
                    for (int i = 0; i < maxi; i++) {
                        numerator += trainingFixedList.get(i).get(1) / trainingFixedList.get(i).get(0);
                    }
                    double denominator = 0;
                    for (int i = 0; i < maxi; i++) {
                        denominator += 1 / trainingFixedList.get(i).get(0);
                    }
                    top = numerator / denominator;

                    // ハイトマップを補間
                    heightmapArray[xPoint - x1][zPoint - z1] = (int) Math.floor(top);
                }
            }
        }
    }

    private static void collectSurfacePoints(World world, int x1, int y1, int z1, int x2, int y2, int z2, int[][] heightmapArray, ArrayList<ControlPointData> heightControlPoints) {
        Block currentBlock;
        // x座標方向のループ
        for (int xPoint = x1; xPoint < x2; xPoint++) {
            // z座標方向のループ
            for (int zPoint = z1; zPoint < z2; zPoint++) {
                // (x,z)におけるラピスラズリブロックのうち最高を記録。なければ-1を代入
                heightmapArray[xPoint - x1][zPoint - z1] = -1;
                // y座標方向のループ
                for (int yPoint = y1; yPoint < y2; yPoint++) {
                    // ループで処理する座標のブロックを取得します。
                    currentBlock = new Location(world, xPoint, yPoint, zPoint).getBlock();
                    if (currentBlock.getType() == Material.LAPIS_BLOCK) {
                        heightmapArray[xPoint - x1][zPoint - z1] = yPoint;
                    }
                }
                // ラピスラズリブロックがあった場合にリストに記録
                if (heightmapArray[xPoint - x1][zPoint - z1] != -1) {
                    heightControlPoints.add(new ControlPointData(xPoint, zPoint, heightmapArray[xPoint - x1][zPoint - z1]));
                }
            }
        }
    }

    public static void sort2DListCol(ArrayList<ArrayList<Double>> array, final int columnLength) {
        for (int i = 0; i < columnLength - 1; i++) {
            for (int j = columnLength - 1; j > i; j--) {
                if (array.get(j - 1).get(0) > array.get(j).get(0)) {
                    ArrayList<Double> tmp = array.get(j - 1);
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
