package deponn.depmount;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.RunContext;
import com.sk89q.worldedit.regions.CuboidRegion;

import java.util.List;

public class RegionOperation implements Operation {
    private final CuboidRegion region;
    private final int[][] heightmapArray;
    private final RegionLoop func;
    private boolean canceled;
    private int current;

    /**
     * x,z座標ごとにループを行う
     *
     * @param region         範囲
     * @param heightmapArray ハイトマップデータ
     * @param func           処理関数
     */
    public RegionOperation(CuboidRegion region, int[][] heightmapArray, RegionLoop func) {
        this.region = region;
        this.heightmapArray = heightmapArray;
        this.func = func;
    }

    @Override
    public Operation resume(RunContext run) throws WorldEditException {
        int x1 = region.getMinimumPoint().getBlockX();
        int z1 = region.getMinimumPoint().getBlockZ();
        int xw = region.getWidth();
        int zw = region.getLength();
        for (int i = current; i < xw * zw; i++) {
            // x座標方向の相対座標
            int x = i % xw;
            // z座標方向の相対座標
            int z = i / xw;
            // x座標方向の座標
            int xPoint = x + x1;
            // z座標方向の座標
            int zPoint = z + z1;

            // 置き換え
            int top = heightmapArray[x][z];
            top = func.loop(xPoint, zPoint, top, this);
            heightmapArray[x][z] = top;

            // キャンセル時
            if (canceled) {
                return null;
            }

            // ある程度実行したら一旦休憩
            if (!run.shouldContinue()) {
                // 進捗を保存
                current = i + 1;
                return this;
            }
        }
        // ここまできたらタスク完了
        return null;
    }

    @Override
    public void cancel() {
        canceled = true;
    }

    @Override
    public void addStatusMessages(List<String> messages) {
        int xw = region.getWidth();
        int zw = region.getLength();
        messages.add(String.format("(%d/%d)", current, xw * zw));
    }
}
