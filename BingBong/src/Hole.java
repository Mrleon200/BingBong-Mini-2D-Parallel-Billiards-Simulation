import java.awt.*;

public class Hole {
    private final int x;
    private final int y;
    private final int radius;

    public Hole(int x, int y, int radius) {
        this.x = x;
        this.y = y;
        this.radius = radius;
    }

    public boolean contains(double ballX, double ballY, int ballRadius) {
        double dx = ballX - x;
        double dy = ballY - y;
        double distance = Math.hypot(dx, dy);
        return distance < (radius + ballRadius * 0.5); // Ball chỉ cần vào 1/2 là tính là vào lỗ
    }

    public void draw(Graphics2D g) {
        g.setColor(Color.BLACK);
        g.fillOval(x - radius, y - radius, radius * 2, radius * 2);
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getRadius() { return radius; }
}