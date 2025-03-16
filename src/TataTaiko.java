import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;

public class TataTaiko extends JPanel implements MouseListener, KeyListener {
    int screen = 1;
    Image TitleImage;
    public TataTaiko() {
        addMouseListener(this);
        addKeyListener(this);
        setFocusable(true);
        requestFocusInWindow();
        try{
            TitleImage = ImageIO.read(new File("Assets\\1_Title\\Background.png"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }
    public void paint(Graphics g) {
        if(screen == 1) drawTitleScreen(g);
    }
    public void drawTitleScreen(Graphics g) {
        g.drawImage(TitleImage, 0, 0, 1920, 1080, this);
        g.setColor(Color.BLACK);
        g.drawString("Tata Taiko", 960, 800);
    }
}
