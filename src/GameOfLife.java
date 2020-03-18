import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferStrategy;
import java.io.*;

public class GameOfLife extends JFrame implements Runnable, MouseListener, MouseMotionListener {

    //member variables
    private BufferStrategy strategy;
    private Graphics offscreenBuffer;
    private boolean gameState[][][] = new boolean[40][40][2];
    private boolean playing = false;
    private boolean isInitialised = false;
    private int percentageAlive = 25;
    private int startX, startY, startW, startH;
    private int randomX, randomY, randomW, randomH;
    private int saveX, saveY, saveW, saveH;
    private int loadX, loadY, loadW, loadH;
    private int buffer = 0;
    private int prevX =-1, prevY =-1;

    //constructor
    public GameOfLife() {
        //display the window, centered on the screen
        Dimension ss = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        int x = ss.width/2 - 400;
        int y = ss.height/2 - 370;
        setBounds(x, y, 800, 800);
        setVisible(true);
        this.setTitle("Conway's Game of Life");

        //initialize double-buffering
        createBufferStrategy(2);
        strategy = getBufferStrategy();
        offscreenBuffer = strategy.getDrawGraphics();

        //register the JFrame to receive mouse events
        addMouseListener(this);
        addMouseMotionListener(this);

        //initialize game state
        for (x = 0; x < 40; x++) {
            for (y = 0; y < 40; y++) {
                gameState[x][y][0] = false;
                gameState[x][y][1] = false;
            }
        }

        //create and start animation thread
        Thread t = new Thread(this);
        t.start();

        isInitialised = true;
    }

    public void mousePressed(MouseEvent e) {
        //determine which cell was clicked on
        int x = e.getX()/20;
        int y = e.getY()/20;

        //actual x/y values for detecting buttons
        int xx = e.getX();
        int yy = e.getY();

        //if Random button is clicked
        if (xx > randomX && xx < randomX + randomW && yy > randomY && yy < randomY + randomH) {
            for (x = 0; x < 40; x++) {
                for (y = 0; y < 40; y++) {
                    if (Math.random()*100 < percentageAlive) gameState[x][y][buffer] = true;
                    else gameState[x][y][buffer] = false;
                }
            }
        }

        //if start button is clicked
        else if (xx > startX && xx < startX + startW && yy > startY && yy < startY + startH) {
            playing = true;
        }

        //if save button is clicked
        else if (xx > saveX && xx < saveX + saveW && yy > saveY && yy < saveY + saveH) {
            saveGame();
        }

        //if load button is clicked
        else if (xx > loadX && xx < loadX + loadW && yy > loadY && yy < loadY + loadH) {
            loadGame();
        }

        else {
            //toggle state of the cell
            gameState[x][y][buffer] = !gameState[x][y][buffer];
            //request extra repaint for immediate feedback
            this.repaint();
            //store current mouse position
            prevX=x;
            prevY=y;
        }
    }

    public void mouseReleased(MouseEvent e) {}

    public void mouseEntered(MouseEvent e) {}

    public void mouseExited(MouseEvent e) {}

    public void mouseClicked(MouseEvent e) {}

    public void mouseMoved(MouseEvent e) {}

    public void mouseDragged(MouseEvent e) {
        // determine which cell of the gameState array was clicked on
        // and make sure it has changed since the last mouseDragged event
        int x = e.getX()/20;
        int y = e.getY()/20;
        if (x != prevX || y != prevY) {
            // toggle state of the cell
            gameState[x][y][buffer] = !gameState[x][y][buffer];
            //repaint for immediate feedback
            this.repaint();
        }
        //store current mouse position
        prevX =x;
        prevY =y;
    }

    public void run() {
        while(true) {
            //1: sleep for 1/5 second
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {}

            //2: animate game objects
            if (playing) {
                for (int x=0; x<40; x++) {
                    for (int y=0; y<40; y++) {
                        int counter = 0;

                        // count the live neighbours of cell [x][y][0]
                        for (int xx=-1; xx<=1; xx++) {
                            for (int yy=-1; yy<=1; yy++) {
                                if (xx!=0 || yy!=0) {
                                    int i = ((x + xx) % 40), j = ((y + yy) % 40);
                                    if (i == -1) i = 39;
                                    if (j == -1) j = 39;
                                    if (gameState[i][j][buffer]) {
                                        counter++;
                                    }
                                }
                            }
                        }

                        //determine if current cell should be killed or not
                        if (gameState[x][y][buffer] && (counter < 2 || counter > 3)) {
                            gameState[x][y][(buffer+1)%2] = false;
                        }
                        else if (!gameState[x][y][buffer] && counter == 3) {
                            gameState[x][y][(buffer+1)%2] = true;
                        }
                        else {
                            gameState[x][y][(buffer+1)%2] = gameState[x][y][buffer];
                        }
                    }
                }
                buffer = ++buffer%2;
            }

            //3: force application repaint
            this.repaint();
        }
    }

    public void paint (Graphics g) {
        if (!isInitialised) return;
        g = offscreenBuffer; //draw to offscreen buffer

        //clear the canvas with a black rectangle
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 800, 800);


        //redraw all game objects
        g.setColor(Color.WHITE);
        for (int x = 0; x < 40; x++) {
            for (int y = 0; y < 40; y++) {
                if (gameState[x][y][buffer]) {
                    g.fillRect(x*20, y*20, 20, 20);
                }
            }
        }

        //draw buttons
        drawButton(g, 40, 60, 20, "Start");
        drawButton(g, 120, 60, 20, "Random");
        drawButton(g, 200, 60, 20, "Save");
        drawButton(g, 265, 60,20,"Load");

        //flip the buffers
        strategy.show();
    }

    private void saveGame() {
        //save gameState to string
        String savedState="";
        for (int x=0;x<40;x++) {
            for (int y=0;y<40;y++) {
                if (gameState[x][y][buffer])
                    savedState+="1";
                else
                    savedState+="0";
            }
        }

        try {
            String workingDirectory = System.getProperty("user.dir");
            String filename = workingDirectory+"\\save.txt";
            BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
            writer.write(savedState);
            writer.close();
        }
        catch (IOException e) { }
    }

    private void loadGame() {
        String workingDirectory = System.getProperty("user.dir");
        String filename = workingDirectory + "\\save.txt";
        String savedState = null;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filename));
            savedState = reader.readLine();
            reader.close();
        } catch (IOException e) {
        }
        if (savedState != null) {
            for (int x = 0; x < 40; x++) {
                for (int y = 0; y < 40; y++) {
                    gameState[x][y][buffer] = (savedState.charAt(x * 40 + y) == '1');
                }
            }
        }
    }

    private void drawButton(Graphics g, int x, int y, int fontSize, String message) {
        Font f = new Font("Times", Font.PLAIN, fontSize);
        g.setFont(f);
        FontMetrics fm = getFontMetrics(f);
        int width = fm.stringWidth(message);

        Rectangle2D r = fm.getStringBounds(message, g);
        int bWidth = (int)r.getWidth();
        int bHeight = (int)r.getHeight();

        g.setColor(Color.GREEN);
        g.fillRect((x-width/2)-5, y-20, bWidth+11, bHeight);
        g.setColor(Color.BLACK);
        g.drawString(message,x-width/2, y);

        if(message.equals("Random")) {
            randomX = (x-width/2) - 5;
            randomY = y-20;
            randomW = bWidth+11;
            randomH = bHeight;
        }
        if(message.equals("Start")) {
            startX = (x-width/2) - 5;
            startY = y-20;
            startW = bWidth+11;
            startH = bHeight;
        }
        if(message.equals("Save")) {
            saveX = (x-width/2) - 5;
            saveY = y-20;
            saveW = bWidth+11;
            saveH = bHeight;
        }
        if(message.equals("Load")) {
            loadX = (x-width/2) - 5;
            loadY = y-20;
            loadW = bWidth+11;
            loadH = bHeight;
        }
    }

    public static void main(String[] args) {
        GameOfLife game = new GameOfLife();
    }
}

