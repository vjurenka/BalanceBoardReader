import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;

/**
 * Created by Vladimir on 21.04.2016.
 */
public class Main implements DataListener {

    final int BOARD_WIDTH_MM = 440, BOARD_HEIGHT_MM = 256, WINDOW_UPSCALE = 3, TICK_SIZE = 10;

    //masses on X an Y in %
    double massX;
    double massY;

    //masses on the platforms
    double topLeft;
    double topRight;
    double bottomRight;
    double bottomLeft;

    //engine to loop
    GameEngine engine;

    //handle file writing
    BufferedImage out;
    StringBuilder textOut;
    boolean recording = false;
    Point lastPos = null;

    private JPanel canvas = new JPanel() {
        @Override
        protected void paintComponent(Graphics g) {
            int width = getWidth();
            int height = getHeight();

            //draw white background
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);

            g.setColor(Color.GRAY);
            for (int i = -1; i <= BOARD_WIDTH_MM; i += TICK_SIZE) {
                g.drawLine(i * WINDOW_UPSCALE, 0, i * WINDOW_UPSCALE, height);
            }
            for (int i = -7; i <= BOARD_HEIGHT_MM; i += TICK_SIZE) {
                g.drawLine(0, i * WINDOW_UPSCALE, width, i * WINDOW_UPSCALE);
            }

            //draw x and y axis
            g.setColor(Color.RED);
            g.drawLine(width / 2, 0, width / 2, height);
            g.drawLine(0, height / 2, width, height / 2);


            //draw current position
            int cWidth = 40;
            int cX = (int) (width * massX);
            int cY = (int) (height * massY);
            g.setColor(Color.BLUE);
            g.fillOval(width / 2 + cX - cWidth / 2, height / 2 + cY - cWidth / 2, cWidth, cWidth);

            //show current weights on sensors
            int textSize = 30;
            g.setColor(Color.BLACK);
            g.drawString("" + Math.round(topLeft * 10) / 10d, 0, textSize);
            g.drawString("" + Math.round(topRight * 10) / 10d, width / 2, textSize);
            g.drawString("" + Math.round(bottomLeft * 10) / 10d, 0, height / 2 + textSize);
            g.drawString("" + Math.round(bottomRight * 10) / 10d, width / 2, height / 2 + textSize);

            //draw recording indicator
            g.setColor(Color.BLACK);
            g.drawString(recording ? "Recording.... press enter to write" : "Press enter to start recording", 10, 10);

            //write position to file
            if (recording) {
                cWidth = 5;
                g = out.getGraphics();
                width = out.getWidth();
                height = out.getHeight();
                cX = (int) (width * massX);
                cY = (int) (height * massY);

                //draw current position in PNG
                g.setColor(Color.BLACK);
                if (lastPos != null) {
                    g.drawLine(lastPos.x, lastPos.y, width / 2 + cX, height / 2 + cY);
                } else {
                    g.setColor(Color.GREEN);
                    cWidth = 10;
                }
                g.fillOval(width / 2 + cX - cWidth / 2, height / 2 + cY - cWidth / 2, cWidth, cWidth);

                //append to text file buffer
                textOut.append(massX * BOARD_WIDTH_MM).append(" ").append(massY * BOARD_HEIGHT_MM).append("\n");

                //remember last pos
                lastPos = new Point(width / 2 + cX, height / 2 + cY);
            }
        }
    };

    public static void main(String... args) throws Exception {
        new Main().run();
    }

    public void run() throws Exception {
        System.loadLibrary("wiibridge");
        engine = new GameEngine();

        //init UI
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        JFrame frame = new JFrame("Balancer");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(BOARD_WIDTH_MM * WINDOW_UPSCALE, BOARD_HEIGHT_MM * WINDOW_UPSCALE);
        canvas.setDoubleBuffered(true);
        frame.getContentPane().add(canvas);
        frame.setResizable(false);
        frame.setVisible(true);
        frame.setLocationRelativeTo(null);

        //record / stop recording on enter press
        frame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    lastPos = null;
                    if (recording) {
                        try {
                            //write coords to text file and to png
                            long ts = new Date().getTime();
                            ImageIO.write(out, "png", new File("output " + ts + ".png"));
                            new PrintStream(new File("output " + ts + ".txt")).print(textOut.toString());
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    } else {
                        out = new BufferedImage(BOARD_WIDTH_MM * WINDOW_UPSCALE, BOARD_HEIGHT_MM * WINDOW_UPSCALE, BufferedImage.TYPE_INT_RGB);
                        textOut = new StringBuilder();
                        Graphics g = out.getGraphics();
                        int width = out.getWidth();
                        int height = out.getHeight();

                        g.setColor(Color.WHITE);
                        g.fillRect(0, 0, out.getWidth(), out.getHeight());

                        g.setColor(Color.GRAY);
                        for (int i = 0; i <= BOARD_WIDTH_MM; i += TICK_SIZE) {
                            g.drawLine(i * WINDOW_UPSCALE, 0, i * WINDOW_UPSCALE, height);
                        }
                        for (int i = -2; i <= BOARD_HEIGHT_MM; i += TICK_SIZE) {
                            g.drawLine(0, i * WINDOW_UPSCALE, width, i * WINDOW_UPSCALE);
                        }

                        g.setColor(Color.RED);
                        g.drawLine(width / 2, 0, width / 2, height);
                        g.drawLine(0, height / 2, width, height / 2);


                    }
                    recording = !recording;
                }
            }
        });

        engine.run(canvas);
        //connect the wii board
        connect(this);
    }

    //connect to wii board
    public native void connect(DataListener listener);

    //callback to receive data from wii board
    @Override
    public void onDataReceived(double topLeft, double topRight, double bottomLeft, double bottomRight, double total) {

        if ((topLeft) < 0.5) {
            topLeft = 0;
        }
        if ((topRight) < 0.5) {
            topRight = 0;
        }
        if ((bottomLeft) < 0.5) {
            bottomLeft = 0;
        }
        if ((bottomRight) < 0.5) {
            bottomRight = 0;
        }

        this.topLeft = topLeft;
        this.topRight = topRight;
        this.bottomLeft = bottomLeft;
        this.bottomRight = bottomRight;

        double topMass = topLeft + topRight;
        double bottomMass = bottomLeft + bottomRight;
        double leftMass = topLeft + bottomLeft;
        double rightMass = topRight + bottomRight;

        double vertRange = topMass + bottomMass;
        double horizRange = rightMass + leftMass;

        if (horizRange != 0) {
            massX = (rightMass - leftMass) / horizRange;
        }
        else{
            massX = 0;
        }
        if (vertRange != 0) {
            massY = -(topMass - bottomMass) / vertRange;
        }
        else{
            massY = 0;
        }
    }
}
