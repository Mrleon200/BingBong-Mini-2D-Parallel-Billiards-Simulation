import java.awt.*;
public class Obstacle {
    public enum Shape { RECT, CIRCLE }

    private final Shape shape;
    // rectangle params (used if shape==RECT)
    private final int x, y, width, height;
    // circle params (used if shape==CIRCLE)
    private final int cx, cy, radius;
    private final Color color;

    // Rectangular obstacle
    public Obstacle(int x, int y, int width, int height, Color color) {
        this.shape = Shape.RECT;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.cx = 0; this.cy = 0; this.radius = 0;
        this.color = color == null ? Color.GRAY : color;
    }

    // Circular obstacle
    public Obstacle(int centerX, int centerY, int radius, Color color, boolean circle) {
        this.shape = Shape.CIRCLE;
        this.cx = centerX;
        this.cy = centerY;
        this.radius = radius;
        this.x = 0; this.y = 0; this.width = 0; this.height = 0;
        this.color = color == null ? Color.GRAY : color;
    }

    public Shape getShape() { return shape; }

    public void draw(Graphics2D g) {
        Color old = g.getColor();
        g.setColor(color);
        if (shape == Shape.RECT) {
            g.fillRect(x, y, width, height);
            g.setColor(Color.DARK_GRAY);
            g.drawRect(x, y, width, height);
        } else {
            g.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
            g.setColor(Color.DARK_GRAY);
            g.drawOval(cx - radius, cy - radius, radius * 2, radius * 2);
        }
        g.setColor(old);
    }

    /**
     * Check collision between this obstacle and a ball.
     */
    public boolean collides(Ball b) {
        if (b == null) return false;
        if (shape == Shape.CIRCLE) {
            double dx = b.x - cx;
            double dy = b.y - cy;
            double dist = Math.hypot(dx, dy);
            return dist < (radius + b.radius);
        } else {
            // rectangle-circle collision: find closest point on rect to circle center
            double closestX = clamp(b.x, x, x + width);
            double closestY = clamp(b.y, y, y + height);
            double dx = b.x - closestX;
            double dy = b.y - closestY;
            return Math.hypot(dx, dy) < b.radius;
        }
    }

    /**
     * Check whether this obstacle overlaps another obstacle (rect-rect, rect-circle, circle-circle).
     */
    public boolean overlaps(Obstacle other) {
        if (other == null) return false;
        if (this.shape == Shape.CIRCLE && other.shape == Shape.CIRCLE) {
            double dx = this.cx - other.cx;
            double dy = this.cy - other.cy;
            return Math.hypot(dx, dy) < (this.radius + other.radius + 4); // small padding
        }
        if (this.shape == Shape.RECT && other.shape == Shape.RECT) {
            return !(this.x + this.width + 4 < other.x || other.x + other.width + 4 < this.x ||
                     this.y + this.height + 4 < other.y || other.y + other.height + 4 < this.y);
        }
        // rect-circle cases: ensure circle doesn't intersect rect
        Obstacle rect = (this.shape == Shape.RECT) ? this : other;
        Obstacle circ = (this.shape == Shape.CIRCLE) ? this : other;
        double closestX = clamp(circ.cx, rect.x, rect.x + rect.width);
        double closestY = clamp(circ.cy, rect.y, rect.y + rect.height);
        double dx = circ.cx - closestX;
        double dy = circ.cy - closestY;
        return Math.hypot(dx, dy) < (circ.radius + 4);
    }

    private double clamp(double v, double a, double b) {
        return Math.max(a, Math.min(b, v));
    }

    // getters for potential use
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getCx() { return cx; }
    public int getCy() { return cy; }
    public int getRadius() { return radius; }
}
