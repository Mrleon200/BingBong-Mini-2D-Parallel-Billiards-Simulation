
import java.awt.Color;

public class Config {
    // Kích thước cửa sổ
    public static final int FRAME_WIDTH = 800;
    public static final int FRAME_HEIGHT = 600;
    public static final int BORDER = 30;
    
    // Màu sắc giao diện
    public static final Color BACKGROUND_COLOR = new Color(0, 100, 0); // Màu xanh lá đậm (màu bàn bida)
    public static final Color BORDER_COLOR = new Color(139, 69, 19); // Màu nâu gỗ
    public static final Color TABLE_BORDER_COLOR = new Color(153, 101, 21); // Màu viền bàn
    public static final int TICK_MS = 20; // update rate
    public static final double WALL_RESTITUTION = 0.95; // sau va chạm tường giữ 95% vx/vy
    public static final int DEFAULT_RADIUS = 18;
    public static final int HOLE_RADIUS = 30;
    public static final double FRICTION = 0.98; // Hệ số ma sát (giảm 2% vận tốc mỗi tick)
    public static final double MINIMUM_SPEED = 0.1; // Vận tốc tối thiểu trước khi dừng
    public static final double MAX_SHOOT_POWER = 8.0; // Tốc độ tối đa khi bắn
    public static final double POWER_SCALE = 0.05; // Hệ số chuyển đổi từ khoảng cách kéo sang lực bắn
    public static final Color AIM_LINE_COLOR = Color.RED; // Màu của đường ngắm

    // Map settings
    public static final int NARROW_HOLE_RADIUS = 22; // Smaller holes for narrow map
    public static final int NARROW_MARGIN = BORDER + 8; // Holes slightly inset
}
