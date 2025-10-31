import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import javax.swing.*;

public class DrawPanel extends JPanel implements MouseListener, MouseMotionListener {
    private final List<Ball> balls = new ArrayList<>();
    private final List<Hole> holes = new ArrayList<>();
    private final List<Obstacle> obstacles = new ArrayList<>();
    private final List<Notification> notifications = new ArrayList<>();
    private final Timer timer;
    private boolean started = false;
    private int currentMap = 1; // current selected map (1..3)
    // per-map ball counts (index 1..3). Initialized from MapX defaults
    private final int[] mapBallCounts = new int[] {0, Map1.defaultBallCount(), Map2.defaultBallCount(), Map3.defaultBallCount()};
    // per-map obstacle counts (index 1..3). Defaults: map1=0, map2=4, map3=8
    private final int[] mapObstacleCounts = new int[] {0, 0, 4, 8};
    
    private Ball selectedBall = null;
    private Point mousePoint = null;
    private Point dragStart = null;
    // prediction: id of the ball predicted to fall into a hole next (-1 = none)
    private int predictedBallId = -1;
    private final Random rng = new Random();

    public DrawPanel() {
        setPreferredSize(new Dimension(Config.FRAME_WIDTH, Config.FRAME_HEIGHT));

        // Add mouse listeners
        addMouseListener(this);
        addMouseMotionListener(this);

    createHoles();
        createInitialBalls();

    timer = new Timer(Config.TICK_MS, ev -> {
            // Kiểm tra va chạm giữa các bi
            for (int i = 0; i < balls.size(); i++) {
                for (int j = i + 1; j < balls.size(); j++) {
                    Ball b1 = balls.get(i);
                    Ball b2 = balls.get(j);
                    if (b1.active && b2.active && b1.isColliding(b2)) {
                        b1.resolveCollision(b2);
                    }
                }
            }
            
            // Cập nhật từng bi và kiểm tra lỗ
            for (Ball b : balls) {
                if (!b.active) continue;
                b.updatePosition();

                // Check collisions with obstacles and resolve by reflecting velocity
                for (Obstacle obs : obstacles) {
                    if (obs.collides(b)) {
                        // compute collision normal: nearest point on obstacle to ball center
                        double nx = 0, ny = 0;
                        if (obs.getShape() == Obstacle.Shape.CIRCLE) {
                            nx = b.x - obs.getCx();
                            ny = b.y - obs.getCy();
                        } else {
                            double closestX = Math.max(obs.getX(), Math.min(b.x, obs.getX() + obs.getWidth()));
                            double closestY = Math.max(obs.getY(), Math.min(b.y, obs.getY() + obs.getHeight()));
                            nx = b.x - closestX;
                            ny = b.y - closestY;
                        }
                        double nlen = Math.hypot(nx, ny);
                        if (nlen == 0) {
                            // fallback normal
                            nx = 0; ny = -1; nlen = 1;
                        }
                        nx /= nlen; ny /= nlen;

                        // reflect velocity
                        double rv = b.vx * nx + b.vy * ny;
                        if (rv < 0) {
                            double restitution = 0.85; // slight energy loss
                            b.vx = b.vx - 2 * rv * nx * restitution;
                            b.vy = b.vy - 2 * rv * ny * restitution;
                        }

                        // push ball out of obstacle slightly to avoid sticking
                        double push = (b.radius + (obs.getShape() == Obstacle.Shape.CIRCLE ? obs.getRadius() : 0)) - nlen;
                        if (push > 0) {
                            b.x += nx * (push + 1);
                            b.y += ny * (push + 1);
                        }
                    }
                }
                for (Hole hole : holes) {
                    if (hole.contains(b.x, b.y, b.radius)) {
                        // Mark ball inactive
                        b.active = false;

                        // If current map is interactive (map 1) use full white/black rules
                        if (currentMap == 1) {
                            // Count remaining active balls (excluding white ball)
                            int remainingColored = 0;
                            for (Ball other : balls) {
                                if (other.active && other.id != 1) {
                                    remainingColored++;
                                }
                            }

                            // Nếu bi trắng rơi xuống lỗ khi còn bi khác trên bàn -> thua
                            if (b.id == 1) {
                                if (remainingColored > 0) {
                                    addNotification("Bạn đã thua!");
                                    ((Timer)ev.getSource()).stop();
                                        started = false;
                                } else {
                                    // Bi trắng rơi xuống khi không còn bi nào - tính như bình thường
                                    addNotification(String.format("Bi sô %d đã rơi xuống", b.id));
                                }
                            } else if (b.id == 8) {  // black ball rules
                                if (remainingColored > 0) {
                                    // Bi đen rơi xuống lỗ quá sớm -> thua
                                    addNotification("Bạn đã thua!");
                                    ((Timer)ev.getSource()).stop();
                                        started = false;
                                } else {
                                    // Bi đen vào lỗ sau cùng -> thắng!
                                    addNotification("Bạn đã thắng!");
                                    ((Timer)ev.getSource()).stop();
                                        started = false;
                                }
                            } else {
                                // normal object ball pocketed
                                addNotification(String.format("Bi sô %d đã rơi xuống", b.id));
                            }
                        } else {
                            // For map 2: stop activity and show a dialog indicating which ball fell
                            if (currentMap == 2) {
                                addNotification(String.format("(Map %d) Bi sô %d đã rơi xuống", currentMap, b.id));
                                // stop timer and mark as not started to prevent interactions
                                ((Timer)ev.getSource()).stop();
                                started = false;
                                // Show a dialog on the EDT
                                SwingUtilities.invokeLater(() -> {
                                    JOptionPane.showMessageDialog(this,
                                        String.format("Bi số %d đã rơi xuống", b.id),"Thông báo",
                                        JOptionPane.INFORMATION_MESSAGE);
                                });
                            } else if (currentMap == 3) {
                                // Map 3: stop the simulation and compare with pre-generated prediction
                                addNotification(String.format("(Map %d) Bi sô %d đã rơi xuống", currentMap, b.id));
                                // stop timer and mark as not started to prevent interactions
                                ((Timer)ev.getSource()).stop();
                                started = false;
                                final boolean match = (predictedBallId > 0 && b.id == predictedBallId);
                                if (match) {
                                    addNotification("Bạn đoán đúng rồi!");
                                } else {
                                    addNotification("Bạn đoán sai rồi");
                                }
                                // Show a dialog on the EDT indicating result
                                SwingUtilities.invokeLater(() -> {
                                    JOptionPane.showMessageDialog(this,
                                        match ? "Bạn đoán đúng rồi" : "Bạn đoán sai rồi",
                                        "Kết quả dự đoán",
                                        JOptionPane.INFORMATION_MESSAGE);
                                });
                            } else {
                                // other non-interactive maps
                                addNotification(String.format("(Map %d) Bi sô %d đã rơi xuống", currentMap, b.id));
                            }
                        }

                        break;
                    }
                }
            }

            // Cập nhật các thông báo (giảm thời gian sống)
            Iterator<Notification> it = notifications.iterator();
            while (it.hasNext()) {
                Notification n = it.next();
                n.ticksRemaining--;
                if (n.ticksRemaining <= 0) it.remove();
            }

            // Keep the pre-generated prediction fixed until next reset; only clear when not on Map 3
            if (currentMap != 3) predictedBallId = -1;

            repaint();
        });
    }

    private void addNotification(String text) {
        int lifeMs = 2000; // hiển thị 2s
        int ticks = Math.max(1, lifeMs / Config.TICK_MS);
        notifications.add(new Notification(text, ticks));
    }
    
    // Generate a fixed random prediction (a ball id from the current set of balls).
    // This is called after the board is reset so the predicted value is visible before Start
    private void generatePredictionForCurrentSetup() {
        if (currentMap != 3) {
            predictedBallId = -1;
            return;
        }
        List<Ball> activeBalls = new ArrayList<>();
        for (Ball b : balls) {
            if (b.active) activeBalls.add(b);
        }
        if (activeBalls.isEmpty()) {
            predictedBallId = -1;
        } else {
            Ball pick = activeBalls.get(rng.nextInt(activeBalls.size()));
            predictedBallId = pick.id;
        }
    }
    private void createHoles() {
        // create holes for the currently selected map
        createHolesFor(currentMap);
    }

    // Public API to switch maps from outside (App)
    public void setMap(int mapId) {
        if (mapId < 1) mapId = 1;
        if (mapId > 3) mapId = 3;
        this.currentMap = mapId;
        // Reinitialize the board for the selected map so its features take effect
        addNotification("Map " + mapId + " selected");
        reset();
    }

    // Allow changing number of object balls (excluding cue ball)
    public void setBallCount(int count) {
        if (count < 1) count = 1;
        if (count > 1000) count = 1000; // arbitrary upper bound
        this.mapBallCounts[currentMap] = count;
        addNotification("Balls for map " + currentMap + " set to " + count);
    }

    // Allow changing number of obstacles per map
    public void setObstacleCount(int count) {
        if (count < 0) count = 0;
        if (count > 500) count = 500; // arbitrary upper bound
        this.mapObstacleCounts[currentMap] = count;
        addNotification("Obstacles for map " + currentMap + " set to " + count);
    }

    public int getObstacleCount() { return mapObstacleCounts[currentMap]; }

    // Get ball count for currently selected map
    public int getBallCount() {
        return mapBallCounts[currentMap];
    }

    // expose current map to UI
    public int getCurrentMap() { return currentMap; }

    // Create holes variation depending on chosen map
    private void createHolesFor(int mapId) {
        holes.clear();
        obstacles.clear();
        switch (mapId) {
            case 2:
                Map2.createHoles(holes);
                break;
            case 3:
                Map3.createHoles(holes);
                break;
            default:
                Map1.createHoles(holes);
                break;
        }
        createObstaclesFor(mapId);
    }

    private void createObstaclesFor(int mapId) {
        obstacles.clear();
        
        int desired = getObstacleCount();
        if (mapId == 2 || mapId == 3) {
            int attemptsLimit = Math.max(200, desired * 50);
            int created = 0;
            int tries = 0;
            while (created < desired && tries < attemptsLimit) {
                tries++;
                boolean makeRect = rng.nextBoolean();
                Obstacle candidate;
                if (makeRect) {
                    int w = (mapId == 2) ? 50 + rng.nextInt(120) : 30 + rng.nextInt(80);
                    int h = (mapId == 2) ? 30 + rng.nextInt(80) : 20 + rng.nextInt(60);
                    int x = Config.BORDER + rng.nextInt(Math.max(1, Config.FRAME_WIDTH - 2 * Config.BORDER - w));
                    int y = Config.BORDER + rng.nextInt(Math.max(1, Config.FRAME_HEIGHT - 2 * Config.BORDER - h));
                    candidate = new Obstacle(x, y, w, h, new Color(120,120,120));
                } else {
                    int rad = (mapId == 2) ? 20 + rng.nextInt(50) : 10 + rng.nextInt(30);
                    int cx = Config.BORDER + rad + rng.nextInt(Math.max(1, Config.FRAME_WIDTH - 2 * (Config.BORDER + rad)));
                    int cy = Config.BORDER + rad + rng.nextInt(Math.max(1, Config.FRAME_HEIGHT - 2 * (Config.BORDER + rad)));
                    candidate = new Obstacle(cx, cy, rad, new Color(140,140,140), true);
                }

                // avoid overlapping holes
                boolean bad = false;
                for (Hole h : holes) {
                    if (candidate.getShape() == Obstacle.Shape.CIRCLE) {
                        Ball tmp = new Ball(0, candidate.getCx(), candidate.getCy(), 0, 0, candidate.getRadius(), Color.GRAY);
                        if (h.contains(tmp.x, tmp.y, tmp.radius)) { bad = true; break; }
                    } else {
                        double cx = candidate.getX() + candidate.getWidth() / 2.0;
                        double cy = candidate.getY() + candidate.getHeight() / 2.0;
                        int probeR = (int) Math.max(1, Math.round(Math.max(candidate.getWidth(), candidate.getHeight())/2.0));
                        if (h.contains(cx, cy, probeR)) { bad = true; break; }
                    }
                }
                if (bad) continue;

                // avoid overlapping existing obstacles
                boolean overlaps = false;
                for (Obstacle o : obstacles) {
                    if (candidate.overlaps(o)) { overlaps = true; break; }
                }
                if (overlaps) continue;

                // accept candidate
                obstacles.add(candidate);
                created++;
            }
            if (desired > 0 && created < desired) {
                final int placed = created;
                addNotification(String.format("Chỉ tạo được %d/%d vật cản do giới hạn không gian", placed, desired));
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this,
                        String.format("Chỉ tạo được %d trên %d vật cản yêu cầu (không đủ chỗ)", placed, desired),
                        "Cảnh báo vật cản",
                        JOptionPane.WARNING_MESSAGE);
                });
            }
        }
    }

    private void createInitialBalls() {
        int baseR = Config.DEFAULT_RADIUS;
    // Special behavior for map 2 and map 3: spawn balls at random positions with random velocities
    if (currentMap == 2 || currentMap == 3) {
            int totalObjects = getBallCount();
            int rUsed = calcBallRadiusForCount(totalObjects, baseR);
            int attemptsLimit = 200;
            for (int id = 1; id <= totalObjects; id++) {
                // find non-overlapping random position inside table bounds
                int attempts = 0;
                double x = 0, y = 0;
                boolean placed = false;
                while (attempts++ < attemptsLimit && !placed) {
                    x = Config.BORDER + rUsed + rng.nextInt(Config.FRAME_WIDTH - 2 * (Config.BORDER + rUsed));
                    y = Config.BORDER + rUsed + rng.nextInt(Config.FRAME_HEIGHT - 2 * (Config.BORDER + rUsed));
                    // don't place inside any hole
                    boolean inHole = false;
                    for (Hole hole : holes) {
                        if (hole.contains(x, y, rUsed)) { inHole = true; break; }
                    }
                    if (inHole) continue;

                    // also avoid obstacles
                    boolean inObstacle = false;
                    for (Obstacle obs : obstacles) {
                        // use a temp ball to test collision/containment
                        Ball tmp = new Ball(0, x, y, 0, 0, rUsed, Color.GRAY);
                        if (obs.collides(tmp)) { inObstacle = true; break; }
                    }
                    if (inObstacle) continue;
                    if (inHole) continue;

                    // check overlap with existing balls
                    boolean overlap = false;
                    for (Ball other : balls) {
                        double dx = x - other.x;
                        double dy = y - other.y;
                        if (Math.hypot(dx, dy) <= (rUsed + other.radius + 2)) { overlap = true; break; }
                    }
                    if (!overlap) placed = true;
                }

                // random velocity
                double angle = rng.nextDouble() * Math.PI * 2;
                double speed = 1.0 + rng.nextDouble() * (Config.MAX_SHOOT_POWER); // at least 1.0
                double vx = Math.cos(angle) * speed;
                double vy = Math.sin(angle) * speed;

                Color c;
                if (id == 8) c = Color.BLACK; else c = Color.getHSBColor((float)((id-1)/(double)Math.max(1,totalObjects-1)), 0.85f, 0.9f);
                balls.add(new Ball(id, x, y, vx, vy, rUsed, c));
            }
            return;
        }

        // Bi số 1 (bi trắng)
        int cueX = Config.FRAME_WIDTH / 4; // bên trái
        int cueY = Config.FRAME_HEIGHT / 2;
        int totalObjects = getBallCount();
        int rUsed = baseR;
        if (currentMap == 3) {
            rUsed = calcBallRadiusForCount(totalObjects, baseR);
        }
        balls.add(new Ball(1, cueX, cueY, 0, 0, rUsed, Color.WHITE));

        // Tạo các bi từ số 2 ... (ballCount bi for current map) => cộng với bi trắng
        int startTriangleX = (Config.FRAME_WIDTH * 3) / 4; // rack position (right side)
        int centerY = Config.FRAME_HEIGHT / 2;

        int ballId = 2;
        int remaining = totalObjects;
        int row = 0;

        while (remaining > 0) {
            int toPlace = Math.min(row + 1, remaining);
            // horizontal spacing (closer packing)
            double x = startTriangleX + row * (rUsed * 1.9);
            for (int i = 0; i < toPlace; i++) {
                double y = centerY + (i - (toPlace - 1) / 2.0) * (rUsed * 2);

                Color c;
                if (ballId == 8) {
                    c = Color.BLACK; // bi số 8 màu đen
                } else {
                    // Phân bố màu sắc cho các bi còn lại bằng HSB
                    float hue = (float) ((ballId - 2) / (double) (totalObjects - 1));
                    c = Color.getHSBColor(hue, 0.85f, 0.9f);
                }

                balls.add(new Ball(ballId++, x, y, 0, 0, rUsed, c));
            }

            remaining -= toPlace;
            row++;
        }
    }

    private void resetGame() {
        boolean wasRunning = timer.isRunning();
        timer.stop();
        balls.clear();
        holes.clear();
        notifications.clear();
        selectedBall = null;
        mousePoint = null;
        dragStart = null;

        createHoles();
        createInitialBalls();
    // create a prediction for the current setup (visible before start)
    generatePredictionForCurrentSetup();

    // ensure not in stopped state
    started = false;

        repaint();

        if (wasRunning) timer.start();
    }

    // Public control methods used by external UI (App)
    public void start() { started = true; timer.start(); }
    public void stop() { started = false; timer.stop(); }
    public void reset() { resetGame(); }

    // sự kiện của chuột
    @Override
    public void mousePressed(MouseEvent e) {
        // néu không phải map tương tác thì bỏ qua
    if (currentMap != 1) return;
    if (!started) return;
        dragStart = e.getPoint();
        // Tìm bi được chọn
        for (Ball ball : balls) {
            if (ball.active && pointInBall(e.getPoint(), ball)) {
                selectedBall = ball;
                // dừng bi lại khi bắt đầu kéo
                selectedBall.vx = 0;
                selectedBall.vy = 0;
                break;
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // bỏ qua nếu không phải map tương tác
    if (currentMap != 1) return;
    if (!started) return;

        if (selectedBall != null && dragStart != null) {
            // tính toán khoảng cách kéo
            double dx = dragStart.x - e.getX();
            double dy = dragStart.y - e.getY();
            double distance = Math.hypot(dx, dy);

            // tính toán power
            double power = Math.min(distance * Config.POWER_SCALE, Config.MAX_SHOOT_POWER);
            if (distance > 0) {
                selectedBall.vx = (dx / distance) * power;
                selectedBall.vy = (dy / distance) * power;
            }
        }
        selectedBall = null;
        dragStart = null;
        mousePoint = null;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (currentMap != 1) return; // do nothing on non-interactive maps
        mousePoint = e.getPoint();
        repaint(); // to show aiming line
    }

    @Override
    public void mouseMoved(MouseEvent e) {}
    @Override
    public void mouseClicked(MouseEvent e) {}
    @Override
    public void mouseEntered(MouseEvent e) {}
    @Override
    public void mouseExited(MouseEvent e) {}

    private boolean pointInBall(Point p, Ball ball) {
        double dx = p.x - ball.x;
        double dy = p.y - ball.y;
        return Math.hypot(dx, dy) <= ball.radius;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        
        // Vẽ nền
        g2.setColor(Config.BACKGROUND_COLOR);
        g2.fillRect(0, 0, Config.FRAME_WIDTH, Config.FRAME_HEIGHT);
        
        // Vẽ viền ngoài
        g2.setColor(Config.BORDER_COLOR);
        g2.fillRect(0, 0, Config.FRAME_WIDTH, Config.BORDER); // Viền trên
        g2.fillRect(0, Config.FRAME_HEIGHT - Config.BORDER, Config.FRAME_WIDTH, Config.BORDER); // Viền dưới
        g2.fillRect(0, 0, Config.BORDER, Config.FRAME_HEIGHT); // Viền trái
        g2.fillRect(Config.FRAME_WIDTH - Config.BORDER, 0, Config.BORDER, Config.FRAME_HEIGHT); // Viền phải
        
        // Vẽ viền bàn
        g2.setColor(Config.TABLE_BORDER_COLOR);
        g2.setStroke(new BasicStroke(4));
        g2.drawRect(Config.BORDER, Config.BORDER, 
                   Config.FRAME_WIDTH - 2 * Config.BORDER, 
                   Config.FRAME_HEIGHT - 2 * Config.BORDER);

        // Vẽ các lỗ
        for (Hole hole : holes) {
            hole.draw(g2);
        }

        // Vẽ các vật cản
        for (Obstacle obs : obstacles) {
            obs.draw(g2);
        }

        // Vẽ các bi
            for (Ball b : balls) {
            if (!b.active) continue;
            g2.setColor(b.color);
            int drawX = (int) Math.round(b.x - b.radius);
            int drawY = (int) Math.round(b.y - b.radius);
            g2.fillOval(drawX, drawY, b.radius * 2, b.radius * 2);
            g2.setColor(Color.DARK_GRAY);
            g2.drawOval(drawX, drawY, b.radius * 2, b.radius * 2);
            // Vẽ số trên bi
            g2.setColor(Color.WHITE);
            FontMetrics fm = g2.getFontMetrics();
            String s = String.valueOf(b.id);
            int sw = fm.stringWidth(s);
            int sh = fm.getAscent();
            g2.drawString(s, (int)(b.x - sw/2.0), (int)(b.y + sh/2.0 - 2));
        }

        

        // vẽ đường chỉ hướng nếu có bi được chọn và đang kéo
        if (selectedBall != null && mousePoint != null && dragStart != null) {
            g2.setColor(Config.AIM_LINE_COLOR);
            g2.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{5}, 0));
            g2.drawLine((int)selectedBall.x, (int)selectedBall.y, mousePoint.x, mousePoint.y);
        }

        // vẽ các thông báo
        if (!notifications.isEmpty()) {
            int padding = 8;
            int x = Config.FRAME_WIDTH - Config.BORDER - 10; // start from inner border
            int y = Config.BORDER + 10;
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 14f));
            for (Notification n : notifications) {
                FontMetrics fm = g2.getFontMetrics();
                int tw = fm.stringWidth(n.text) + padding * 2;
                int th = fm.getHeight() + padding;
                int bx = x - tw;
                int by = y - fm.getAscent();
                // Background (semi-transparent)
                Composite old = g2.getComposite();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.85f));
                g2.setColor(new Color(0, 0, 0));
                g2.fillRoundRect(bx, by, tw, th, 8, 8);
                g2.setComposite(old);
                g2.setColor(Color.WHITE);
                g2.drawString(n.text, bx + padding, y);

                y += th + 6;
            }
        }

        // Draw prediction text (which ball is likely to fall next) only for Map 3
        if (currentMap == 3) {
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 14f));
            g2.setColor(Color.YELLOW);
            String predText = (predictedBallId > 0) ? String.format("Dự đoán: Bi số %d sẽ vào lỗ", predictedBallId) : "Dự đoán: -";
            g2.drawString(predText, Config.BORDER + 12, Config.BORDER + 20);
        }
    }
    private static class Notification {
        final String text;
        int ticksRemaining;
        Notification(String text, int ticks) { this.text = text; this.ticksRemaining = ticks; }
    }
    // Tăng sô lượng bi sẽ giảm kích thước bi để tránh chồng chéo quá mức.Kích thước tối thiểu là 6
    private int calcBallRadiusForCount(int count, int baseR) {
        if (count <= 10) return baseR;
        double scale = Math.sqrt(10.0 / (double) Math.max(1, count));
        if (scale > 1.0) scale = 1.0;
        int r = (int) Math.max(6, Math.round(baseR * scale));
        return r;
    }

}
