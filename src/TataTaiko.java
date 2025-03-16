import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class TataTaiko extends JPanel implements MouseListener, KeyListener {
    int screen = 1;
    Image TitleImage;
    Image CursorMenu;
    int cursorPosition = 1;
    String songPath = "Songs/";
    File song = new File(songPath);
    Image SongSelectBackground;
    ArrayList<String> binFilesList = new ArrayList<>();
    boolean binFilesLoaded = false; // Ensures we load bin files only once

    public TataTaiko() {
        addMouseListener(this);
        addKeyListener(this);
        setFocusable(true);

        try {
            TitleImage = ImageIO.read(new File("Assets/1_Title/Background.png"));
            CursorMenu = ImageIO.read(new File("Assets/1_Title/Cursor_Right.png"));
            SongSelectBackground = ImageIO.read(new File("Assets/2_SongSelect/Background.png"));
        } catch (IOException e) {
            throw new RuntimeException("Error loading images: " + e.getMessage());
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (screen == 1) {
            if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                cursorPosition = 2;
            } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                cursorPosition = 1;
            }

            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                if (cursorPosition == 2) System.exit(0);
                if (cursorPosition == 1) {
                    screen = 2;
                    if (!binFilesLoaded) {  // Only load once when entering Taiko mode
                        loadBinFiles();
                        binFilesLoaded = true;
                    }
                    repaint();
                }
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (screen == 1) drawTitleScreen(g);
        if (screen == 2) drawSongSelect(g);
    }

    public void drawTitleScreen(Graphics g) {
        g.drawImage(TitleImage, 0, 0, 1920, 1080, this);
        drawCursorMenu(g);
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 40));
        g.drawString("Taiko Mode", 900, 900);
        g.drawString("Exit", 900, 950);
    }

    public void drawCursorMenu(Graphics g) {
        if (screen == 1) {
            int yPosition = (cursorPosition == 1) ? 870 : 920;
            g.drawImage(CursorMenu, 800, yPosition, this);
        }
    }

    public void drawSongSelect(Graphics g) {
        g.drawImage(SongSelectBackground, 0, 0, 1920, 1080, this);

        g.setColor(Color.BLACK);
        g.setFont(new Font("Meiryo", Font.BOLD, 40));
        g.drawString("曲の選択 (Song Selection)", 50, 50);

        int yOffset = 100;
        for (String fileName : binFilesList) {
            g.drawString(fileName, 50, yOffset);
            yOffset += 40;
        }

        if (binFilesList.isEmpty()) {
            g.drawString("No .bin files found.", 50, yOffset);
        }
    }

    private void loadBinFiles() {
        binFilesList.clear();

        if (song.exists() && song.isDirectory()) {
            File[] subFolders = song.listFiles(File::isDirectory);

            if (subFolders != null) {
                for (File subFolder : subFolders) {
                    File[] binFiles = subFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".bin"));
                    if (binFiles != null) {
                        for (File binFile : binFiles) {
                            binFilesList.add(binFile.getName());
                        }
                    }
                }
            }
        }
        System.out.println("Loaded " + binFilesList.size() + " .bin files.");
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
}