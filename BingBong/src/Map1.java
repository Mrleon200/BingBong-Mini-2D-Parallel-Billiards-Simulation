import java.util.List;

public class Map1 {
    // Default ball count for this map
    public static int defaultBallCount() { return 10; }

    // Populate holes for Map1 (narrow / default)
    public static void createHoles(List<Hole> holes) {
        int margin = Config.NARROW_MARGIN;
        int holeRadius = Config.NARROW_HOLE_RADIUS;

        holes.add(new Hole(margin, margin, holeRadius));  // top-left
        holes.add(new Hole(Config.FRAME_WIDTH - margin, margin, holeRadius));  // top-right
        holes.add(new Hole(margin, Config.FRAME_HEIGHT - margin, holeRadius));  // bottom-left
        holes.add(new Hole(Config.FRAME_WIDTH - margin, Config.FRAME_HEIGHT - margin, holeRadius));  // bottom-right

        holes.add(new Hole(Config.FRAME_WIDTH / 2, margin, holeRadius));  // top-center
        holes.add(new Hole(Config.FRAME_WIDTH / 2, Config.FRAME_HEIGHT - margin, holeRadius));  // bottom-center
    }
}
