import java.util.List;

public class Map3 {
    // Default ball count for this map
    public static int defaultBallCount() { return 10; }
    // Map3: single pocket (reduced to 1 as requested)
    public static void createHoles(List<Hole> holes) {
        int holeRadius = Config.HOLE_RADIUS;
        // place the single hole near the center (you can adjust position if desired)
        holes.add(new Hole(Config.FRAME_WIDTH / 2, Config.FRAME_HEIGHT / 2, holeRadius));
    }
}
