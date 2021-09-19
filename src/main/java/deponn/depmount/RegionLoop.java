package deponn.depmount;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.function.operation.Operation;

public interface RegionLoop {
    /**
     * x,z座標ごとにyHeightを変更する
     *
     * @param xPoint  x座標
     * @param zPoint  z座標
     * @param yHeight ハイトマップの高さ、未定義は-1
     * @return 更新後のyHeight
     */
    int loop(int xPoint, int zPoint, int yHeight, Operation operation) throws WorldEditException;
}
