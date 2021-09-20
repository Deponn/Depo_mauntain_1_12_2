package deponn.depmount;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.RunContext;
import com.sk89q.worldedit.regions.CuboidRegion;

import java.util.List;

/**
 * x,z座標でループを行うクラス
 * ティック分散に必要な、作業を中断、及び途中からの作業の再開をサポートする
 */
public class RegionOperation implements Operation {
    // 範囲
    private final CuboidRegion region;
    // ハイトマップ
    private final int[][] heightmapArray;
    // 処理関数
    private final RegionLoop func;
    // キャンセルフラグ
    private boolean canceled;
    // 現在の進捗状況
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

    /**
     * RunContextのshouldContinueがtrueになるまで処理を行う。
     *
     * @param run コンテキスト、shouldContinue用
     * @return 操作オブジェクト
     * @throws WorldEditException エラー
     */
    @Override
    public Operation resume(RunContext run) throws WorldEditException {
        // 範囲
        int x1 = region.getMinimumPoint().getBlockX();
        int z1 = region.getMinimumPoint().getBlockZ();
        int xw = region.getWidth();
        int zw = region.getLength();
        // 進捗状況を保存しやすくするため、二重ループではなく単一ループにしている
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
        // キャンセルフラグをON
        canceled = true;
    }

    @Override
    public void addStatusMessages(List<String> messages) {
        // 進捗メッセージを出力
        int xw = region.getWidth();
        int zw = region.getLength();
        messages.add(String.format("(%d/%d)", current, xw * zw));
    }
}
