import javax.swing.*;

/**
 * Created by Vladimir on 21.04.2016.
 */
public class GameEngine {

    private volatile boolean running = false;

    private long start;

    public void update(float tpf) {
    }

    public void run(final JPanel canvas) {
        running = true;
        final int fps = 50;
        start = System.currentTimeMillis();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (running) {
                    try {
                        update(1f / fps);
                        canvas.repaint();
                        Thread.sleep(1000 / fps);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

}
