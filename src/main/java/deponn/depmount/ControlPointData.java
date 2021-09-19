package deponn.depmount;

/**
 * ハイトマップx,z地点における高さyのデータ
 */
public class ControlPointData {
    /**
     * x, z座標 (検索用)
     */
    public final int xPoint, zPoint;
    /**
     * y座標 (取り出し用)
     */
    public final int yHeight;

    /**
     * ハイトマップx,z地点における高さyのデータ
     *
     * @param xPoint  x座標 (検索用)
     * @param zPoint  z座標 (検索用)
     * @param yHeight y座標 (取り出し用)
     */
    public ControlPointData(int xPoint, int zPoint, int yHeight) {
        this.xPoint = xPoint;
        this.zPoint = zPoint;
        this.yHeight = yHeight;
    }
}
