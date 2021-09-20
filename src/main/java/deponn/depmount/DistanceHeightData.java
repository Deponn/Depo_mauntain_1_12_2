package deponn.depmount;

/**
 * ある点Pとの距離の二乗と高さの関係
 */
public class DistanceHeightData {
    /**
     * xz面上での距離
     */
    public final double xzDistance;
    /**
     * yの高さ
     */
    public final double yHeight;

    /**
     * ある点Pとの距離の二乗と高さの関係
     *
     * @param xzDistance 点Pとのxz面上での距離
     * @param yHeight    yの高さ
     */
    public DistanceHeightData(double xzDistance, double yHeight) {
        this.xzDistance = xzDistance;
        this.yHeight = yHeight;
    }
}
