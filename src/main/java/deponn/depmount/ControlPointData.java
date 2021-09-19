package deponn.depmount;

/**
 * ハイトマップx,z地点における高さyのデータ
 */
public class ControlPointData {
    public final int xPoint, zPoint;
    public final int yHeight;

    public ControlPointData(int xPoint, int zPoint, int yHeight) {
        this.xPoint = xPoint;
        this.zPoint = zPoint;
        this.yHeight = yHeight;
    }
}
