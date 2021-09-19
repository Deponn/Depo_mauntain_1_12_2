package deponn.depmount;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.function.mask.AbstractExtentMask;
import com.sk89q.worldedit.function.mask.FuzzyBlockMask;
import com.sk89q.worldedit.function.mask.Mask2D;
import com.sk89q.worldedit.function.mask.Masks;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.patterns.Pattern;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

public class MountOperation {
    public static Operation collectSurfacePoints(World wWorld, CuboidRegion region, boolean bCollectBorder, int[][] heightmapArray, ArrayList<ControlPointData> heightControlPoints) {
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

    public static Operation interpolateSurface(int maxi, CuboidRegion region, int[][] heightmapArray, ArrayList<ControlPointData> heightControlPoints) {
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
                trainingFixedList.sort(Comparator.comparingDouble(a -> a.xzDistance));

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

    public static Operation applySurface(EditSession editSession, World wWorld, boolean bReplaceAll, CuboidRegion region, int[][] heightmapArray) {
        BaseBlock lapis = new BaseBlock(BlockID.LAPIS_LAZULI_BLOCK);
        BaseBlock air = new BaseBlock(BlockID.AIR);
        BaseBlock grass = new BaseBlock(BlockID.GRASS);
        BaseBlock dirt = new BaseBlock(BlockID.DIRT);
        BaseBlock stone = new BaseBlock(BlockID.STONE);
        int y1 = region.getMinimumPoint().getBlockY();
        int y2 = region.getMaximumPoint().getBlockY();
        return new RegionOperation(region, heightmapArray, (xPoint, zPoint, top, context) -> {
            Pattern pattern = new Pattern() {
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
            };
            // 縦長の範囲を置き換えする
            // ラピスラズリブロックを消去したうえで、標高の地点まで土を盛っていく
            editSession.replaceBlocks(
                    new CuboidRegion(
                            wWorld,
                            new BlockVector(xPoint, y1, zPoint),
                            new BlockVector(xPoint, bReplaceAll ? y2 : top, zPoint)
                    ),
                    // bReplaceAllがtrueのとき全て書き換え、falseのとき空気のみ書き換える。
                    new AbstractExtentMask(editSession) {
                        @Override
                        public boolean test(Vector vector) {
                            BaseBlock lazyBlock = getExtent().getLazyBlock(vector);
                            BaseBlock compare = new BaseBlock(lazyBlock.getType(), lazyBlock.getData());
                            // 変更がなければ更新しない (差分が増えることでメモリを圧迫するため)
                            if (compare.equalsFuzzy(pattern.next(vector))) {
                                return false;
                            }
                            // ラピスラズリブロックは置き換え
                            if (compare.equalsFuzzy(lapis)) {
                                return true;
                            }
                            // bReplaceAllがfalseの場合は空気しか置き換えない
                            if (!bReplaceAll && !compare.equalsFuzzy(air)) {
                                return false;
                            }
                            return true;
                        }

                        @Nullable
                        @Override
                        public Mask2D toMask2D() {
                            return null;
                        }
                    }, pattern);
            return top;
        });
    }
}
