import java.util.List;

public class Map2 {
    // Default ball count for this map
    public static int defaultBallCount() { return 5; }

    // Map2: single center hole
    public static void createHoles(List<Hole> holes) {
        int holeRadius = Config.HOLE_RADIUS;
        holes.add(new Hole(Config.FRAME_WIDTH / 2, Config.FRAME_HEIGHT / 2, holeRadius));
    }
}
