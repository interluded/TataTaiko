import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;

public class TataTaiko extends JPanel implements MouseListener, KeyListener {
    // Screen and image variables
    int screen = 1;
    Image TitleImage;
    Image CursorMenu;
    Image SongSelectBackground;
    Image InGameBackground;
    Image TaikoDrum;
    Image DonDrum; // Drum images (for hit effects)
    Image KaDrum;
    Image Lane;
    Image BackgroundFinished;
    Image JudgeMeter;

    // Falling note images
    Image noteDon;
    Image noteKa;
    Image noteDonBig;
    Image noteKaBig;

    // Judgment feedback images
    Image imgGood;
    Image imgOK;
    Image imgBad;

    // Judgment display variables
    Image currentJudgmentImage = null;
    long judgmentDisplayTime = 0; // Time when judgment image was set (ms)
    final int judgmentDisplayDuration = 800; // Duration to show the judgment image (ms)
    int gameScore = 0;

    // Hit counters
    int countGood = 0;
    int countOK = 0;
    int countBad = 0;

    int cursorPosition = 0;
    String songPath = "Songs/";
    File song = new File(songPath);
    String songName = "";
    ArrayList<String> binFilesList = new ArrayList<>();
    boolean binFilesLoaded = false;
    boolean drawDonRight = false;
    boolean drawDonLeft = false;
    boolean drawKaRight = false;
    boolean drawKaLeft = false;

    // List to hold note objects and game timing variables
    ArrayList<Note> notes = new ArrayList<>();
    long songStartTime; // System time when song started (ms)
    Timer gameTimer;
    final int travelTime = 2000; // Time in ms for a note to travel across the lane
    final int laneLeftX = 40;
    final int laneRightX = 40 + 1600; // Assuming lane width is 1600

    // Timing windows (in ms) for judgment feedback
    final int GOOD_WINDOW = 25;
    final int OK_WINDOW = 70;
    final int MAX_WINDOW = 108; // Maximum acceptable error for a hit

    // Judging circle radius declaration
    final int judgeRadius = 50; // Radius for the judging circle

    // Hit target offset (the target X where notes should be hit)
    final int hitOffsetX = 150;

    // To track the time of the last note.
    long lastNoteTime = 0;

    // Inner class for a note
    class Note {
        long hitTime; // When the note should hit the drum (ms from start)
        int type;     // 1 for Don, 0 for Ka
        int x;        // Current x position

        public Note(long hitTime, int type) {
            this.hitTime = hitTime;
            this.type = type;
            this.x = laneRightX; // Start at right edge
        }
    }

    public TataTaiko() {
        addMouseListener(this);
        addKeyListener(this);
        setFocusable(true);
        // Request focus so key events are captured.
        this.requestFocusInWindow();

        try {
            TitleImage = ImageIO.read(new File("Assets/1_Title/Background.png"));
            CursorMenu = ImageIO.read(new File("Assets/1_Title/Cursor_Right.png"));
            SongSelectBackground = ImageIO.read(new File("Assets/2_SongSelect/Background.png"));
            InGameBackground = ImageIO.read(new File("Assets/3_InGame/Background.png"));
            TaikoDrum = ImageIO.read(new File("Assets/3_InGame/Taiko/Base.png"));
            DonDrum = ImageIO.read(new File("Assets/3_InGame/Taiko/Don.png"));
            KaDrum = ImageIO.read(new File("Assets/3_InGame/Taiko/Ka.png"));
            Lane = ImageIO.read(new File("Assets/3_InGame/Lane.png"));
            BackgroundFinished = ImageIO.read(new File("Assets/4_GameDone/Background.png"));
            JudgeMeter = ImageIO.read(new File("Assets/4_GameDone/JudgeMeter.png"));
            // Falling note images
            noteDon = ImageIO.read(new File("Assets/3_InGame/Don.png"));
            noteKa = ImageIO.read(new File("Assets/3_InGame/Ka.png"));
            noteDonBig = ImageIO.read(new File("Assets/3_InGame/DonBig.png"));
            noteKaBig = ImageIO.read(new File("Assets/3_InGame/KaBig.png"));
            // Judgment images
            imgGood = ImageIO.read(new File("Assets/3_InGame/Good.png"));
            imgOK = ImageIO.read(new File("Assets/3_InGame/OK.png"));
            imgBad = ImageIO.read(new File("Assets/3_InGame/Bad.png"));
        } catch (IOException e) {
            throw new RuntimeException("Error loading images: " + e.getMessage());
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // For menus (screens 1 and 2), only play sound on Enter key.
        if (screen == 1 || screen == 2) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                playDonSound(); // Use Don sound for menu selections (adjust if desired)
            }
        } else if (screen == 3) {
            // In game, play respective sounds for A, S, K, L.
            if (e.getKeyCode() == KeyEvent.VK_S || e.getKeyCode() == KeyEvent.VK_K) {
                playDonSound();
            } else if (e.getKeyCode() == KeyEvent.VK_A || e.getKeyCode() == KeyEvent.VK_L) {
                playKaSound();
            }
        } else if (screen == 4) {
            // End screen: Escape exits; Enter returns to main menu.
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                System.exit(0);
            } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                screen = 1;
            }
        }

        if (screen == 1) {
            handleMenuNavigation(e);
        } else if (screen == 2) {
            handleSongSelection(e);
        } else if (screen == 3) {
            checkHit(e);
            HandleGameBoard(e);
        }
        repaint();
    }

    // Helper method to play the Don sound
    private void playDonSound() {
        MP3Player temp = new MP3Player("Assets/Sounds/Don.mp3");
        temp.play();
    }

    // Helper method to play the Ka sound
    private void playKaSound() {
        MP3Player temp = new MP3Player("Assets/Sounds/Ka.mp3");
        temp.play();
    }

    private void handleMenuNavigation(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            cursorPosition = Math.min(cursorPosition + 1, 1);
        } else if (e.getKeyCode() == KeyEvent.VK_UP) {
            cursorPosition = Math.max(cursorPosition - 1, 0);
        }
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            if (cursorPosition == 1 && screen == 1)
                System.exit(0);
            if (cursorPosition == 0 && screen == 1) {
                screen = 2;
                cursorPosition = 0;
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
                cursorPosition = Math.min(cursorPosition + 1, binFilesList.size());
            } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                cursorPosition = Math.max(cursorPosition - 1, 0);
            } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                if (cursorPosition == binFilesList.size()) {
                    screen = 1;
                    cursorPosition = 0;
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
        if (screen == 1)
            drawTitleScreen(g);
        else if (screen == 2)
            drawSongSelect(g);
        else if (screen == 3)
            drawGameboard(g);
        else if (screen == 4)
            drawEndScreen(g);
    }

    public void drawTitleScreen(Graphics g) {
        g.drawImage(TitleImage, 0, 0, 1920, 1080, this);
        drawCursorMenu(g, new int[]{870, 920});
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 40));
        g.drawString("Taiko Mode", 900, 900);
        g.drawString("Exit", 900, 950);
    }

    // Updated drawCursorMenu: For screen 2, assume text is centered.
    // We'll draw the cursor 50 pixels to the left of the centered text.
    public void drawCursorMenu(Graphics g, int[] yPositions) {
        if (cursorPosition < 0 || cursorPosition >= yPositions.length) {
            System.out.println("Cursor out of bounds: " + cursorPosition);
            cursorPosition = Math.max(0, Math.min(cursorPosition, yPositions.length - 1));
        }
        if (screen == 2) {
            int cursorX = getWidth() / 2 - 200; // Adjust as needed
            g.drawImage(CursorMenu, cursorX, yPositions[cursorPosition] - 25, this);
        } else if (screen == 1) {
            g.drawImage(CursorMenu, 800, yPositions[cursorPosition], this);
        }
    }

    // Updated drawSongSelect: Center all text horizontally.
    public void drawSongSelect(Graphics g) {
        g.drawImage(SongSelectBackground, 0, 0, getWidth(), getHeight(), this);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Meiryo", Font.BOLD, 40));
        FontMetrics fm = g.getFontMetrics();

        // Draw title centered
        String title = "曲の選択 (Song Selection)";
        int titleWidth = fm.stringWidth(title);
        int titleX = (getWidth() - titleWidth) / 2;
        g.drawString(title, titleX, 100);

        // Draw the list of songs (and "Exit") centered.
        int yOffset = 200;
        int lineHeight = 50;
        int[] yPositions = new int[binFilesList.size() + 1];
        for (int i = 0; i < binFilesList.size(); i++) {
            String sName = binFilesList.get(i).replace(".bin", "");
            int textWidth = fm.stringWidth(sName);
            int x = (getWidth() - textWidth) / 2;
            g.drawString(sName, x, yOffset);
            yPositions[i] = yOffset;
            yOffset += lineHeight;
        }
        // Draw "Exit" option
        String exitText = "Exit";
        int exitWidth = fm.stringWidth(exitText);
        int exitX = (getWidth() - exitWidth) / 2;
        g.drawString(exitText, exitX, yOffset);
        yPositions[binFilesList.size()] = yOffset;

        // Draw the cursor next to the current selection
        drawCursorMenu(g, yPositions);
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

    MP3Player mp3Player;

    public void loadSong(String song) {
        this.songName = new File(song).getName();
        System.out.println("Loading song: " + songName);
        screen = 3;
        repaint();

        // Stop any currently playing song
        if (mp3Player != null) {
            mp3Player.stop();
        }

        // Start new MP3 playback
        String songFolder = songName.replace(".bin", "");
        String mp3FilePath = "Songs/" + songFolder + "/" + songFolder + ".mp3";
        mp3Player = new MP3Player(mp3FilePath);
        mp3Player.play();

        // Load the note file and record song start time
        loadNotes("Songs/" + songFolder + "/" + songName);
        songStartTime = System.currentTimeMillis();

        // Start a game loop timer to update note positions
        gameTimer = new Timer(16, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateNotes();
                repaint();
            }
        });
        gameTimer.start();
    }

    // Method to read and parse the note file.
    public void loadNotes(String filePath) {
        notes.clear();
        lastNoteTime = 0;
        try (Scanner scanner = new Scanner(new File(filePath))) {
            // Skip metadata until "Note Timing:" line is found.
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.startsWith("Note Timing:")) {
                    break;
                }
            }
            // Parse note lines
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty())
                    continue;
                String[] parts = line.split("\\s+");
                if (parts.length >= 2) {
                    long hitTime = parseTimeToMs(parts[0]);
                    int type = Integer.parseInt(parts[1]); // 1 for Don, 0 for Ka
                    notes.add(new Note(hitTime, type));
                    if (hitTime > lastNoteTime) {
                        lastNoteTime = hitTime;
                    }
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("Note file not found: " + filePath);
        }
    }

    // Utility method to convert "mm:ss:SSS" to milliseconds
    public long parseTimeToMs(String timeStr) {
        String[] parts = timeStr.split(":");
        int minutes = Integer.parseInt(parts[0]);
        int seconds = Integer.parseInt(parts[1]);
        int milliseconds = Integer.parseInt(parts[2]);
        return minutes * 60000 + seconds * 1000 + milliseconds;
    }

    // Update note positions based on elapsed time.
    public void updateNotes() {
        long currentTime = System.currentTimeMillis() - songStartTime;
        Iterator<Note> iter = notes.iterator();
        while (iter.hasNext()) {
            Note note = iter.next();
            int targetX = laneLeftX + hitOffsetX;
            // If current time exceeds the note hit time by MAX_WINDOW, count it as missed.
            if (currentTime >= note.hitTime + MAX_WINDOW) {
                countBad++;
                currentJudgmentImage = imgBad;
                judgmentDisplayTime = System.currentTimeMillis();
                iter.remove();
                continue;
            }
            double fraction = (double) (currentTime - (note.hitTime - travelTime)) / travelTime;
            note.x = (int) (laneRightX - fraction * (laneRightX - targetX));
        }
        // If all notes are processed and the song has played beyond the last note, switch screen.
        if (notes.isEmpty() && currentTime > lastNoteTime + MAX_WINDOW) {
            screen = 4;
            if (gameTimer != null) {
                gameTimer.stop();
            }
        }
    }

    // Check for a hit when a key is pressed (and show a judgment)
    private void checkHit(KeyEvent e) {
        int expectedType = -1; // 1 for Don, 0 for Ka
        if (e.getKeyCode() == KeyEvent.VK_S || e.getKeyCode() == KeyEvent.VK_K) {
            expectedType = 1;
        } else if (e.getKeyCode() == KeyEvent.VK_A || e.getKeyCode() == KeyEvent.VK_L) {
            expectedType = 0;
        } else {
            return;
        }
        long currentTime = System.currentTimeMillis() - songStartTime;
        Note target = null;
        long bestError = Long.MAX_VALUE;
        for (Note note : notes) {
            if (note.type != expectedType)
                continue;
            long error = Math.abs(note.hitTime - currentTime);
            if (error < bestError && error <= MAX_WINDOW) {
                bestError = error;
                target = note;
            }
        }
        if (target != null) {
            notes.remove(target);
            if (bestError <= GOOD_WINDOW) {
                currentJudgmentImage = imgGood;
                countGood++;
            } else if (bestError <= OK_WINDOW) {
                currentJudgmentImage = imgOK;
                countOK++;
            } else {
                currentJudgmentImage = imgBad;
                countBad++;
            }
            judgmentDisplayTime = System.currentTimeMillis();
        }
    }

    // Handle visual hit effects based on key events
    public void HandleGameBoard(KeyEvent e) {
        int keyCode = e.getKeyCode();
        int duration = 100; // Duration for hit effect
        if (keyCode == KeyEvent.VK_S) {
            drawDonLeft = true;
            repaint();
            new Timer(duration, evt -> {
                drawDonLeft = false;
                repaint();
                ((Timer) evt.getSource()).stop();
            }).start();
        } else if (keyCode == KeyEvent.VK_K) {
            drawDonRight = true;
            repaint();
            new Timer(duration, evt -> {
                drawDonRight = false;
                repaint();
                ((Timer) evt.getSource()).stop();
            }).start();
        } else if (keyCode == KeyEvent.VK_A) {
            drawKaLeft = true;
            repaint();
            new Timer(duration, evt -> {
                drawKaLeft = false;
                repaint();
                ((Timer) evt.getSource()).stop();
            }).start();
        } else if (keyCode == KeyEvent.VK_L) {
            drawKaRight = true;
            repaint();
            new Timer(duration, evt -> {
                drawKaRight = false;
                repaint();
                ((Timer) evt.getSource()).stop();
            }).start();
        }
    }

    public void drawGameboard(Graphics g) {
        g.drawImage(InGameBackground, 0, 0, 1920, 1080, this);
        g.drawImage(Lane, laneLeftX, 120, 2000, 165, this);
        g.drawImage(TaikoDrum, laneLeftX, 120, this);

        // Draw song name
        g.setColor(Color.WHITE);
        g.setFont(new Font("Meiryo", Font.BOLD, 40));
        if (songName.length() > 4)
            g.drawString(songName.substring(0, songName.length() - 4), 100, 50);

        int drumX = laneLeftX;
        int drumY = 120;
        int drumWidth = TaikoDrum.getWidth(null);
        int drumHeight = TaikoDrum.getHeight(null);

        // Draw hit effects for drum halves
        if (drawDonLeft) {
            g.drawImage(DonDrum, drumX, drumY, drumX + drumWidth / 2, drumY + drumHeight,
                    0, 0, drumWidth / 2, drumHeight, this);
        }
        if (drawDonRight) {
            g.drawImage(DonDrum, drumX + drumWidth / 2, drumY, drumX + drumWidth, drumY + drumHeight,
                    drumWidth / 2, 0, drumWidth, drumHeight, this);
        }
        if (drawKaLeft) {
            g.drawImage(KaDrum, drumX, drumY, drumX + drumWidth / 2, drumY + drumHeight,
                    0, 0, drumWidth / 2, drumHeight, this);
        }
        if (drawKaRight) {
            g.drawImage(KaDrum, drumX + drumWidth / 2, drumY, drumX + drumWidth, drumY + drumHeight,
                    drumWidth / 2, 0, drumWidth, drumHeight, this);
        }

        // Draw falling notes using note images
        for (Note note : notes) {
            int noteSize = 100; // Adjust as needed
            if (note.type == 1) {
                g.drawImage(noteDon, note.x, 150, note.x + noteSize, 150 + noteSize,
                        0, 0, noteDon.getWidth(null), noteDon.getHeight(null), this);
            } else {
                g.drawImage(noteKa, note.x, 150, note.x + noteSize, 150 + noteSize,
                        0, 0, noteKa.getWidth(null), noteKa.getHeight(null), this);
            }
        }

        // Compute judging circle position based on the target hit location.
        int targetX = laneLeftX + hitOffsetX;
        int noteSize = 100;
        int judgeX = targetX + noteSize / 2; // Center of note when it hits
        int judgeY = 150 + noteSize / 2;       // Vertical center of note

        // Draw the judging circle (for visual reference)
        g.setColor(Color.YELLOW);
        g.drawOval(judgeX - judgeRadius, judgeY - judgeRadius, judgeRadius * 2, judgeRadius * 2);
        g.setColor(new Color(255, 255, 0, 50));
        g.fillOval(judgeX - judgeRadius, judgeY - judgeRadius, judgeRadius * 2, judgeRadius * 2);

        // Draw judgment image if within display duration (centered in the judging circle, but shifted up 100 pixels)
        if (currentJudgmentImage != null) {
            long displayElapsed = System.currentTimeMillis() - judgmentDisplayTime;
            if (displayElapsed < judgmentDisplayDuration) {
                int imgWidth = 84, imgHeight = 80;
                g.drawImage(currentJudgmentImage, judgeX - imgWidth / 2, judgeY - imgHeight / 2 - 100, imgWidth, imgHeight, this);
            } else {
                currentJudgmentImage = null;
            }
        }

        g.setColor(Color.white);
        g.drawImage(JudgeMeter, 0, 400, this);
        g.setFont(new Font("Arial", Font.BOLD, 40));
        g.drawString(String.valueOf(countGood), 120, 570);
        g.drawString(String.valueOf(countOK), 120, 610);
        g.drawString(String.valueOf(countBad), 120, 650);
    }

    // Draw the end screen when the song is finished.
    public void drawEndScreen(Graphics g) {
        mp3Player.stop();
        gameScore = (countGood * 960) + (countOK * 420);
        g.drawImage(BackgroundFinished, 0, 0, 1920, 1080, this);
        g.drawImage(JudgeMeter, 400, 400, this);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 50));
        g.drawString("Song Finished!", getWidth() / 2 - 150, getHeight() / 2);
        g.setFont(new Font("Arial", Font.BOLD, 40));
        g.drawString(String.valueOf(countGood), 520, 570);
        g.drawString(String.valueOf(countOK), 520, 610);
        g.drawString(String.valueOf(countBad), 520, 650);
        g.drawString(String.valueOf(gameScore), 520, 700);
        g.drawString("Total Score: ", 290, 690);
        gameScore = 0;
    }

    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mousePressed(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}
    @Override public void keyReleased(KeyEvent e) {}
}
