package deponn.depmount;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.function.operation.Operation;

/**
 * x,z座標でループする際、共通部分が多いため、クラスを使用して共通化する
 * ティック分散処理の処理も共通化できる
 */
public interface RegionLoop {
    /**
     * x,z座標ごとにyHeightを変更する
     *
     * @param xPoint    x座標
     * @param zPoint    z座標
     * @param yHeight   ハイトマップの高さ、未定義は-1
     * @param operation 操作オブジェクト
     * @return 更新後のyHeight
     */
    int loop(int xPoint, int zPoint, int yHeight, Operation operation) throws WorldEditException;
}
