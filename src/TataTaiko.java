import javax.imageio.ImageIO;
import javax.sound.sampled.*;
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
    Image SongSelectBackground;
    Image InGameBackground;
    Image TaikoDrum;
    Image DonDrum;
    Image KaDrum;
    Image Lane;
    int cursorPosition = 0; // Cursor for both menu and song selection
    String songPath = "Songs/";
    File song = new File(songPath);
    String songName = song.getName();
    ArrayList<String> binFilesList = new ArrayList<>();
    boolean binFilesLoaded = false; // Ensures we load bin files only once
    boolean drawDonActive = false;
    boolean drawKaActive = false;

    public TataTaiko() {
        addMouseListener(this);
        addKeyListener(this);
        setFocusable(true);

        try {
            TitleImage = ImageIO.read(new File("Assets/1_Title/Background.png"));
            CursorMenu = ImageIO.read(new File("Assets/1_Title/Cursor_Right.png"));
            SongSelectBackground = ImageIO.read(new File("Assets/2_SongSelect/Background.png"));
            InGameBackground = ImageIO.read(new File("Assets/3_InGame/Background.png"));
            TaikoDrum = ImageIO.read(new File("Assets/3_InGame/Taiko/Base.png"));
            DonDrum = ImageIO.read(new File("Assets/3_InGame/Taiko/Don.png"));
            KaDrum = ImageIO.read(new File("Assets/3_InGame/Taiko/Ka.png"));
            Lane = ImageIO.read(new File("Assets/3_InGame/Lane.png"));
        } catch (IOException e) {
            throw new RuntimeException("Error loading images: " + e.getMessage());
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (screen == 1) {
            handleMenuNavigation(e);
        } else if (screen == 2) {
            handleSongSelection(e);
        } else if (screen == 3) {
            HandleGameBoard(e);
        }
        repaint();
    }


    private void handleMenuNavigation(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            cursorPosition = Math.min(cursorPosition + 1, 1); // Limit cursor to [0, 1]
        } else if (e.getKeyCode() == KeyEvent.VK_UP) {
            cursorPosition = Math.max(cursorPosition - 1, 0);
        }

        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            if (cursorPosition == 1) System.exit(0);
            if (cursorPosition == 0) {
                screen = 2;
                cursorPosition = 0; // Reset for song selection
                if (!binFilesLoaded) {
                    loadBinFiles();
                    binFilesLoaded = true;
                }
            }
        }
    }

    private void handleSongSelection(KeyEvent e) {
        if (!binFilesList.isEmpty()) {
            if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                cursorPosition = Math.min(cursorPosition + 1, binFilesList.size()); // Includes Exit option
            } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                cursorPosition = Math.max(cursorPosition - 1, 0);
            } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                if (cursorPosition == binFilesList.size()) {
                    screen = 1; // If "Exit" is selected, go back to Title Screen
                    cursorPosition = 0; // Reset cursor for menu
                } else {
                    System.out.println("Selected song: " + binFilesList.get(cursorPosition));
                    loadSong(binFilesList.get(cursorPosition));
                }
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (screen == 1) drawTitleScreen(g);
        if (screen == 2) drawSongSelect(g);
        if (screen == 3) drawGameboard(g);
    }

    public void drawTitleScreen(Graphics g) {
        g.drawImage(TitleImage, 0, 0, 1920, 1080, this);
        drawCursorMenu(g, new int[]{870, 920}); // Cursor for menu options
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 40));
        g.drawString("Taiko Mode", 900, 900);
        g.drawString("Exit", 900, 950);
    }

    public void drawCursorMenu(Graphics g, int[] yPositions) {
        if (cursorPosition < 0 || cursorPosition >= yPositions.length) {
            System.out.println("Cursor out of bounds: " + cursorPosition);
            cursorPosition = Math.max(0, Math.min(cursorPosition, yPositions.length - 1));
        }
        if (screen == 1) {
            g.drawImage(CursorMenu, 800, yPositions[cursorPosition], this);
        } else if (screen == 2) {
            g.drawImage(CursorMenu, 60, yPositions[cursorPosition] - 25, this);
        }
    }

    public void drawSongSelect(Graphics g) {
        g.drawImage(SongSelectBackground, 0, 0, 1920, 1080, this);
        g.setColor(Color.BLACK);
        g.setFont(new Font("Meiryo", Font.BOLD, 40));
        g.drawString("曲の選択 (Song Selection)", 50, 50);

        int yOffset = 100;
        int cursorOffset = 95;
        int[] yPositions = new int[binFilesList.size() + 1]; // Extra space for "Exit"

        for (int i = 0; i < binFilesList.size(); i++) {
            g.drawString(binFilesList.get(i).replace(".bin", ""), 100, yOffset);
            yPositions[i] = cursorOffset;
            yOffset += 50;
            cursorOffset += 50;
        }

        // Draw "Exit" option at the bottom of the song list
        g.drawString("Exit", 100, yOffset);
        yPositions[binFilesList.size()] = cursorOffset;

        drawCursorMenu(g, yPositions); // Draw cursor
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

    public void loadSong(String song) {
        this.songName = new File(song).getName();
        System.out.println("Loading song: " + songName);
        screen = 3;
        repaint();
    }
    public void drawGameboard(Graphics g) {
        g.drawImage(InGameBackground, 0, 0, 1920, 1080, this);
        g.drawImage(Lane, 40, 120, 1600, 165, this);
        g.drawImage(TaikoDrum, 40, 120, this);

        // Draw song name
        g.setColor(Color.WHITE);
        g.setFont(new Font("Meiryo", Font.BOLD, 40));
        g.drawString(songName.substring(0, songName.length() - 4), 100, 50);

        // Draw Don hit effect (S/K)
        if (drawDonActive) {
            g.drawImage(DonDrum, 40, 120, this);
        }

        // Draw Ka hit effect (A/L)
        if (drawKaActive) {
            g.drawImage(KaDrum, 40, 120, this);
        }
    }

    public void HandleGameBoard(KeyEvent e) {
        int keyCode = e.getKeyCode();

        // DonDrum on S or K
        if (keyCode == KeyEvent.VK_S || keyCode == KeyEvent.VK_K) {
            drawDonActive = true;
            repaint();
            new Timer(100, evt -> {
                drawDonActive = false;
                repaint();
                ((Timer) evt.getSource()).stop();
            }).start();
        }

        // KaDrum on A or L
        if (keyCode == KeyEvent.VK_A || keyCode == KeyEvent.VK_L) {
            drawKaActive = true;
            repaint();
            new Timer(100, evt -> {
                drawKaActive = false;
                repaint();
                ((Timer) evt.getSource()).stop();
            }).start();
        }
    }

    public void drawDon(Graphics g) {
        g.drawImage(DonDrum,40,120,this);
}
    public static void playSound(String filePath) {
        try {
            File soundFile = new File(filePath);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundFile);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            clip.start();

            // Keep the program running while sound plays
            Thread.sleep(clip.getMicrosecondLength() / 1000);
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException | InterruptedException e) {
            e.printStackTrace();
        }}
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mousePressed(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}
    @Override public void keyReleased(KeyEvent e) {}
}
