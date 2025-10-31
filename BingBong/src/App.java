import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.IntConsumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Bing Bong Game");
            DrawPanel panel = new DrawPanel();

            frame.setLayout(new BorderLayout());
            frame.add(panel, BorderLayout.CENTER);

            // Top: map selection + per-map settings
            JPanel mapPanel = new JPanel();
            mapPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

            SpinnerNumberModel spinnerModel = new SpinnerNumberModel(panel.getBallCount(), 1, 1000, 1);
            JSpinner ballCountSpinner = new JSpinner(spinnerModel);
            JLabel ballsLabel = new JLabel("Balls:");
            JButton applyBallsBtn = new JButton("Apply");
            applyBallsBtn.addActionListener(e -> {
                int v = (Integer) ballCountSpinner.getValue();
                panel.setBallCount(v);
                panel.reset();
            });

            SpinnerNumberModel obsModel = new SpinnerNumberModel(panel.getObstacleCount(), 0, 500, 1);
            JSpinner obsSpinner = new JSpinner(obsModel);
            JLabel obsLabel = new JLabel("Obstacles:");
            JButton applyObsBtn = new JButton("Apply");
            applyObsBtn.addActionListener(e -> {
                int v = (Integer) obsSpinner.getValue();
                panel.setObstacleCount(v);
                panel.reset();
            });

            JButton map1Btn = new JButton("Map 1");
            JButton map2Btn = new JButton("Map 2");
            JButton map3Btn = new JButton("Map 3");

            // local helper to change map and update controls
            IntConsumer switchToMap = (mapId) -> {
                panel.setMap(mapId);
                ballCountSpinner.setValue(panel.getBallCount());
                obsSpinner.setValue(panel.getObstacleCount());
                boolean enableControls = (mapId != 1);
                ballCountSpinner.setEnabled(enableControls);
                applyBallsBtn.setEnabled(enableControls);
                obsSpinner.setEnabled(enableControls);
                applyObsBtn.setEnabled(enableControls);
            };

            map1Btn.addActionListener(_a -> switchToMap.accept(1));
            map2Btn.addActionListener(_a -> switchToMap.accept(2));
            map3Btn.addActionListener(_a -> switchToMap.accept(3));

            mapPanel.add(map1Btn);
            mapPanel.add(map2Btn);
            mapPanel.add(map3Btn);
            mapPanel.add(ballsLabel);
            mapPanel.add(ballCountSpinner);
            mapPanel.add(applyBallsBtn);
            mapPanel.add(obsLabel);
            mapPanel.add(obsSpinner);
            mapPanel.add(applyObsBtn);

            boolean addBallsAllowed = panel.getCurrentMap() != 1;
            ballCountSpinner.setEnabled(addBallsAllowed);
            applyBallsBtn.setEnabled(addBallsAllowed);
            boolean obsAllowed = panel.getCurrentMap() != 1;
            obsSpinner.setEnabled(obsAllowed);
            applyObsBtn.setEnabled(obsAllowed);

            frame.add(mapPanel, BorderLayout.NORTH);

            // Bottom: controls
            JPanel controlPanel = new JPanel();
            controlPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            JButton startBtn = new JButton("Start");
            JButton stopBtn = new JButton("Stop");
            JButton resetBtn = new JButton("Reset");

            startBtn.addActionListener(_ -> panel.start());
            stopBtn.addActionListener(_ -> panel.stop());
            resetBtn.addActionListener(_ -> panel.reset());

            controlPanel.add(startBtn);
            controlPanel.add(stopBtn);
            controlPanel.add(resetBtn);

            frame.add(controlPanel, BorderLayout.SOUTH);

            frame.pack();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // stop timer on close
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    panel.stop();
                }
            });
        });
    }
}
