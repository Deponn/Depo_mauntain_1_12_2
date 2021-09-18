package deponn.depmount;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;

public final class DepMount extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("Deponn's Plugin is now available");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("Deponn's Plugin is not available");
    }

    //undo処理用
    final int MINIMUM_Y = 1;
    final int MAXIMUM_Y = 255;
    final int MAXIMUM_X = 150;
    final int MAXIMUM_Z = 150;
    Material[][][] BlockRecord = new Material[MAXIMUM_X][MAXIMUM_Y][MAXIMUM_Z];
    int X1, Y1, Z1, X2, Y2, Z2;
    boolean undoflag = false;

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        // プレイヤーがコマンドを投入した際の処理...
        if (cmd.getName().equalsIgnoreCase("CreateMountain")) {

            Player player = (Player) sender;
            Location loc = player.getLocation();
            Block currentBlock;
            //コマンド引数を処理
            int lengthX;
            int lengthZ;
            boolean option1, option2;
            int k;
            try {
                lengthX = Integer.parseInt(args[0]);
                lengthZ = Integer.parseInt(args[1]);
                if (Integer.parseInt(args[2]) == 0) {
                    option1 = false;
                } else if (Integer.parseInt(args[2]) == 1) {
                    option1 = true;
                } else {
                    sender.sendMessage("args[2],0 or 1 only");
                    return false;
                }
                if (Integer.parseInt(args[3]) == 0) {
                    option2 = false;
                } else if (Integer.parseInt(args[3]) == 1) {
                    option2 = true;
                } else {
                    sender.sendMessage("args[2],0 or 1 only");
                    return false;
                }
                k = Integer.parseInt(args[4]);
            } catch (NumberFormatException e) {
                sender.sendMessage("integer only");
                return false;
            }
            if (lengthX < -MAXIMUM_X || lengthX > MAXIMUM_X) {
                sender.sendMessage("-150<=x<=150");
                return false;
            }
            if (lengthZ < -MAXIMUM_Z || lengthZ > MAXIMUM_Z) {
                sender.sendMessage("-150<=z<=150");
                return false;
            }
            if (k < 0) {
                sender.sendMessage("0<k");
                return false;
            }
            //範囲を設定
            int x1, y1, z1, x2, y2, z2;
            if (lengthX > 0) {
                x1 = loc.getBlockX();
                x2 = x1 + lengthX;
            } else {
                x2 = loc.getBlockX();
                x1 = x2 + lengthX;
            }
            if (lengthZ > 0) {
                z1 = loc.getBlockZ();
                z2 = z1 + lengthZ;
            } else {
                z2 = loc.getBlockZ();
                z1 = z2 + lengthZ;
            }
            y1 = MINIMUM_Y;
            y2 = MAXIMUM_Y;
            //undo処理用
            X1 = x1;
            X2 = x2;
            Y1 = y1;
            Y2 = y2;
            Z1 = z1;
            Z2 = z2;
            //もともとの地形を記録
            for (int xPoint = x1; xPoint < x2; xPoint++) {
                for (int yPoint = y1; yPoint < y2; yPoint++) {
                    for (int zPoint = z1; zPoint < z2; zPoint++) {
                        loc.setX(xPoint);
                        loc.setZ(zPoint);
                        loc.setY(yPoint);
                        currentBlock = loc.getBlock();
                        BlockRecord[xPoint - x1][yPoint - y1][zPoint - z1] = currentBlock.getType();
                    }
                }
            }
            //option2がtureのとき、境界にラピスラズリブロックをおく。境界条件。
            if (option2 == true) {
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

            //ラピスラズリブロックを目印として、範囲中のデータを取得
            int[][] TrainingArray = new int[MAXIMUM_X][MAXIMUM_Z]; //範囲中のラピスラズリブロックの位置を座標指定型で記録
            ArrayList<ArrayList<Integer>> TrainingList = new ArrayList<ArrayList<Integer>>(); //範囲中のラピスラズリブロックの位置をリストとして記録
            // x座標方向のループ
            for (int xPoint = x1; xPoint < x2; xPoint++) {
                // z座標方向のループ
                for (int zPoint = z1; zPoint < z2; zPoint++) {
                    //(x,z)におけるラピスラズリブロックのうち最高を記録。なければ-1を代入
                    TrainingArray[xPoint - x1][zPoint - z1] = -1;
                    // y座標方向のループ
                    for (int yPoint = y1; yPoint < y2; yPoint++) {
                        // ループで処理する座標のブロックを取得します。
                        loc.setX(xPoint);
                        loc.setZ(zPoint);
                        loc.setY(yPoint);
                        currentBlock = loc.getBlock();
                        if (currentBlock.getType() == Material.LAPIS_BLOCK) {
                            TrainingArray[xPoint - x1][zPoint - z1] = yPoint;
                        }
                    }
                    //ラピスラズリブロックがあった場合にリストに記録
                    if (TrainingArray[xPoint - x1][zPoint - z1] != -1) {
                        ArrayList<Integer> OneData = new ArrayList<Integer>();
                        OneData.add(xPoint);
                        OneData.add(zPoint);
                        OneData.add(TrainingArray[xPoint - x1][zPoint - z1]);
                        TrainingList.add(OneData);
                    }
                }
            }
            //範囲中の地形を実際に改変
            double top;
            double numerator, denominator;
            int size;
            // x座標方向のループ
            for (int xPoint = x1; xPoint < x2; xPoint++) {
                // z座標方向のループ
                for (int zPoint = z1; zPoint < z2; zPoint++) {
                    //ラピスラズリブロックがなかった場合、k近傍法を参考にし、y=sum(yn/((x-xn)^2+(z-zn)^2))/sum(1/((x-xn)^2+(z-zn)^2))で標高計算。あった場合そのy座標が標高
                    if (TrainingArray[xPoint - x1][zPoint - z1] == -1) {
                        try {
                            size = TrainingList.size();
                            //距離のリストに変換。
                            ArrayList<ArrayList<Double>> TrainingFixedList = new ArrayList<ArrayList<Double>>();
                            for (int i = 0; i < size; i++) {
                                ArrayList<Double> OneData = new ArrayList<Double>();
                                OneData.add(Math.pow(xPoint - TrainingList.get(i).get(0), 2) + Math.pow(zPoint - TrainingList.get(i).get(1), 2));
                                OneData.add(TrainingList.get(i).get(2).doubleValue());
                                TrainingFixedList.add(OneData);
                            }
                            //距離順にする
                            Sort2DListCol(TrainingFixedList, size);
                            //距離が近い順にk個取り出す。ただし、k=0の時は全部
                            int maxi;
                            if (k == 0) {
                                if (size == 0) {
                                    sender.sendMessage("最低一つはラピスラズリブロックをおいてください。");
                                    return false;
                                }
                                maxi = size;
                            } else {
                                if (size < k) {
                                    sender.sendMessage("kより多いラピスラズリブロックをおいてください。");
                                    return false;
                                }
                                maxi = k;
                            }
                            //計算
                            numerator = 0;
                            for (int i = 0; i < maxi; i++) {
                                numerator += TrainingFixedList.get(i).get(1) / TrainingFixedList.get(i).get(0);
                            }
                            denominator = 0;
                            for (int i = 0; i < maxi; i++) {
                                denominator += 1 / TrainingFixedList.get(i).get(0);
                            }
                            top = numerator / denominator;
                        } catch (Exception e) {
                            sender.sendMessage(e.getMessage());
                            return false;
                        }

                    } else {
                        top = TrainingArray[xPoint - x1][zPoint - z1];
                    }

                    // y座標方向のループ
                    for (int yPoint = y1; yPoint < y2; yPoint++) {
                        // ループで処理する座標のブロックを取得します。
                        loc.setX(xPoint);
                        loc.setZ(zPoint);
                        loc.setY(yPoint);
                        currentBlock = loc.getBlock();
                        //ラピスラズリブロックを消去したうえで、標高の地点まで土を盛っていく
                        if (currentBlock.getType() == Material.LAPIS_BLOCK) {
                            currentBlock.setType(Material.AIR);
                        }
                        //option1がfalseのとき全て書き換え、trueのとき空気のみ書き換える。
                        if (option1 == false) {
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
            //undoできるようにする。
            undoflag = true;

            return true;
        } else if (cmd.getName().equalsIgnoreCase("UndoMountain")) {
            //まだundoする内容がない時。
            if (undoflag == false) {
                sender.sendMessage("戻す内容がありません。");
                return false;
            }
            Player player = (Player) sender;
            Location loc = player.getLocation();
            Block currentBlock;
            //undoとして、ブロックを一個一個、記録したものに戻していく。
            try {
                for (int xPoint = X1; xPoint < X2; xPoint++) {
                    for (int yPoint = Y1; yPoint < Y2; yPoint++) {
                        for (int zPoint = Z1; zPoint < Z2; zPoint++) {
                            loc.setX(xPoint);
                            loc.setZ(zPoint);
                            loc.setY(yPoint);
                            currentBlock = loc.getBlock();
                            if (currentBlock.getType() == Material.AIR || currentBlock.getType() == Material.GRASS || currentBlock.getType() == Material.DIRT || currentBlock.getType() == Material.STONE) {
                                currentBlock.setType(BlockRecord[xPoint - X1][yPoint - Y1][zPoint - Z1]);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                sender.sendMessage(e.getMessage());
                return false;
            }

            undoflag = false;
            return true;
        }
        return false;
        // コマンドが実行されなかった場合は、falseを返して当メソッドを抜ける。


        //undo がシングルのみ対応、あと、向きが分かりにくい.done k実装、done 空気のみに作用させるか done ラピスラズリブロックなかったとき done境界条件
    }

    public static void Sort2DListCol(ArrayList<ArrayList<Double>> array, final int columnlength) {

        for (int i = 0; i < columnlength - 1; i++) {
            for (int j = columnlength - 1; j > i; j--) {
                if (array.get(j - 1).get(0) > array.get(j).get(0)) {
                    ArrayList<Double> tmp = array.get(j - 1);
                    array.set(j - 1, array.get(j));
                    array.set(j, tmp);
                }
            }
        }

    }
}
