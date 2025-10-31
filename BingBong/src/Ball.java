

import java.awt.*;

public class Ball {
    public final int id;
    public double x, y;
    public double vx, vy;
    public final int radius;
    public final Color color;
    public boolean active = true;
    public static final double COLLISION_DAMPING = 0.95; // Hệ số giảm chấn khi va chạm

    public Ball(int id, double x, double y, double vx, double vy, int radius, Color color) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.radius = radius;
        this.color = color;
    }

    /**
     * Update position by one tick and handle wall bounce inside the rectangle defined by borders in Config.
     */
    public void updatePosition() {
        // Apply friction
        if (vx != 0 || vy != 0) {
            // Calculate current speed
            double speed = Math.hypot(vx, vy);
            
            // Apply friction
            vx *= Config.FRICTION;
            vy *= Config.FRICTION;
            
            // If speed is below minimum, stop the ball
            if (speed < Config.MINIMUM_SPEED) {
                vx = 0;
                vy = 0;
            }
        }

        // Update position
        x += vx;
        y += vy;

        double left = Config.BORDER + radius;
        double right = Config.FRAME_WIDTH - Config.BORDER - radius;
        double top = Config.BORDER + radius;
        double bottom = Config.FRAME_HEIGHT - Config.BORDER - radius;

        if (x < left) {
            x = left;
            vx = -vx * Config.WALL_RESTITUTION;
        } else if (x > right) {
            x = right;
            vx = -vx * Config.WALL_RESTITUTION;
        }

        if (y < top) {
            y = top;
            vy = -vy * Config.WALL_RESTITUTION;
        } else if (y > bottom) {
            y = bottom;
            vy = -vy * Config.WALL_RESTITUTION;
        }
    }

    /**
     * Kiểm tra xem hai viên bi có va chạm không
     */
    public boolean isColliding(Ball other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        double distance = Math.hypot(dx, dy);
        return distance <= (this.radius + other.radius);
    }

    /**
     * Xử lý va chạm giữa hai viên bi
     */
    public void resolveCollision(Ball other) {
        double dx = other.x - this.x;
        double dy = other.y - this.y;
        double distance = Math.hypot(dx, dy);
        
        // Normalize collision vector
        double nx = dx / distance;
        double ny = dy / distance;

        // Relative velocity
        double rvx = other.vx - this.vx;
        double rvy = other.vy - this.vy;

        // Relative velocity along normal
        double velAlongNormal = rvx * nx + rvy * ny;

        // Don't resolve if objects are moving apart
        if (velAlongNormal > 0) return;

        // Calculate impulse
        double impulse = -(1 + COLLISION_DAMPING) * velAlongNormal;
        
        // Update velocities
        this.vx -= nx * impulse;
        this.vy -= ny * impulse;
        other.vx += nx * impulse;
        other.vy += ny * impulse;

        // Prevent overlapping by moving balls apart
        double overlap = (this.radius + other.radius) - distance;
        if (overlap > 0) {
            this.x -= (nx * overlap / 2);
            this.y -= (ny * overlap / 2);
            other.x += (nx * overlap / 2);
            other.y += (ny * overlap / 2);
        }
    }
}
