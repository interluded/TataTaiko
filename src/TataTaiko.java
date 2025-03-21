import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;

// Make sure Note.java is in the same package or imported accordingly.
public class TataTaiko extends JPanel implements MouseListener, KeyListener {
    // Screen and image variables
    int screen = 1;
    Image TitleImage;
    Image CursorMenu;
    Image SongSelectBackground;
    Image InGameBackground;
    Image TaikoDrum;
    Image DonDrum; // For small Don hit effect
    Image KaDrum;  // For small Ka hit effect
    Image Lane;
    Image BackgroundFinished;
    Image JudgeMeter;

    // Falling note images (small notes)
    Image noteDon;
    Image noteKa;
    // Falling note images for big notes
    Image noteDonBig;
    Image noteKaBig;

    // Judgment feedback images
    Image imgGood;
    Image imgOK;
    Image imgBad;

    // Judgment display variables
    Image currentJudgmentImage = null;
    long judgmentDisplayTime = 0;
    final int judgmentDisplayDuration = 800;
    int gameScore = 0;

    // Hit counters (for display)
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
    final int END_DELAY = 3500; // 3.5 seconds delay after the last note

    // Note timing and movement variables
    ArrayList<Note> notes = new ArrayList<>();
    long songStartTime;
    Timer gameTimer;
    final int travelTime = 2000;
    final int laneLeftX = 40;
    final int laneRightX = 40 + 1600;

    // Timing windows (ms)
    final int GOOD_WINDOW = 25;
    final int OK_WINDOW = 70;
    final int MAX_WINDOW = 108;

    // Judging circle radius
    final int judgeRadius = 50;

    // Hit target offset (X position for note hit)
    final int hitOffsetX = 150;

    // To track last note time.
    long lastNoteTime = 0;
    MP3Player themeSong = new MP3Player("Assets/Sounds/theme.mp3");
    boolean themeSongPlayed = false;

    public TataTaiko() {
        addMouseListener(this);
        addKeyListener(this);
        setFocusable(true);
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
            // Load small note images.
            noteDon = ImageIO.read(new File("Assets/3_InGame/Don.png"));
            noteKa = ImageIO.read(new File("Assets/3_InGame/Ka.png"));
            // Load big note images.
            noteDonBig = ImageIO.read(new File("Assets/3_InGame/DonBig.png"));
            noteKaBig = ImageIO.read(new File("Assets/3_InGame/KaBig.png"));
            // Judgment images.
            imgGood = ImageIO.read(new File("Assets/3_InGame/Good.png"));
            imgOK = ImageIO.read(new File("Assets/3_InGame/OK.png"));
            imgBad = ImageIO.read(new File("Assets/3_InGame/Bad.png"));
        } catch (IOException e) {
            throw new RuntimeException("Error loading images: " + e.getMessage());
        }
        System.out.println("noteDonBig: " + noteDonBig.getWidth(null) + "x" + noteDonBig.getHeight(null));
        System.out.println("noteKaBig: " + noteKaBig.getWidth(null) + "x" + noteKaBig.getHeight(null));
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // In menus (screens 1 & 2): play sound on Enter.
        if (screen == 1 || screen == 2) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                playDonSound();
            }
        } else if (screen == 3) {
            // In game: S/K for Don, A/L for Ka.
            if (e.getKeyCode() == KeyEvent.VK_S || e.getKeyCode() == KeyEvent.VK_K) {
                playDonSound();
            } else if (e.getKeyCode() == KeyEvent.VK_A || e.getKeyCode() == KeyEvent.VK_L) {
                playKaSound();
            }
        } else if (screen == 4) {
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

    private void playDonSound() {
        MP3Player temp = new MP3Player("Assets/Sounds/Don.mp3");
        temp.play();
    }

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
        if (!themeSongPlayed) {
            themeSong.play();
            themeSongPlayed = true;
        }
        g.drawImage(TitleImage, 0, 0, 1920, 1080, this);
        drawCursorMenu(g, new int[]{870, 920});
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
        if (screen == 2) {
            int cursorX = getWidth() / 2 - 200;
            g.drawImage(CursorMenu, cursorX, yPositions[cursorPosition] - 25, this);
        } else if (screen == 1) {
            g.drawImage(CursorMenu, 800, yPositions[cursorPosition], this);
        }
    }

    public void drawSongSelect(Graphics g) {
        g.drawImage(SongSelectBackground, 0, 0, getWidth(), getHeight(), this);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Meiryo", Font.BOLD, 40));
        FontMetrics fm = g.getFontMetrics();
        String title = "曲の選択 (Song Selection)";
        int titleWidth = fm.stringWidth(title);
        int titleX = (getWidth() - titleWidth) / 2;
        g.drawString(title, titleX, 100);
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
        String exitText = "Exit";
        int exitWidth = fm.stringWidth(exitText);
        int exitX = (getWidth() - exitWidth) / 2;
        g.drawString(exitText, exitX, yOffset);
        yPositions[binFilesList.size()] = yOffset;
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
        if (mp3Player != null) {
            mp3Player.stop();
        }
        String songFolder = songName.replace(".bin", "");
        String mp3FilePath = "Songs/" + songFolder + "/" + songFolder + ".mp3";
        mp3Player = new MP3Player(mp3FilePath);
        mp3Player.play();
        loadNotes("Songs/" + songFolder + "/" + songName);
        songStartTime = System.currentTimeMillis();
        gameTimer = new Timer(7, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateNotes();
                repaint();
            }
        });
        gameTimer.start();
    }

    public void loadNotes(String filePath) {
        themeSong.stop();
        themeSongPlayed = false;
        notes.clear();
        lastNoteTime = 0;
        try (Scanner scanner = new Scanner(new File(filePath))) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.startsWith("Note Timing:")) {
                    break;
                }
            }
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty())
                    continue;
                String[] parts = line.split("\\s+");
                if (parts.length >= 2) {
                    long hitTime = parseTimeToMs(parts[0]);
                    int type = Integer.parseInt(parts[1]); // 0: small Don, 1: small Ka, 2: big Don, 3: big Ka
                    // Create note with starting x position = laneRightX
                    notes.add(new Note(hitTime, type, laneRightX));
                    if (hitTime > lastNoteTime) {
                        lastNoteTime = hitTime;
                    }
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("Note file not found: " + filePath);
        }
    }

    public long parseTimeToMs(String timeStr) {
        String[] parts = timeStr.split(":");
        int minutes = Integer.parseInt(parts[0]);
        int seconds = Integer.parseInt(parts[1]);
        int milliseconds = Integer.parseInt(parts[2]);
        return minutes * 60000 + seconds * 1000 + milliseconds;
    }

    public void updateNotes() {
        long currentTime = System.currentTimeMillis() - songStartTime;
        Iterator<Note> iter = notes.iterator();
        while (iter.hasNext()) {
            Note note = iter.next();
            int targetX = laneLeftX + hitOffsetX;
            if (currentTime >= note.hitTime + MAX_WINDOW) {
                if (note.type == 2 || note.type == 3) {
                    if (note.leftHit || note.rightHit) {
                        long error = (note.leftHit && !note.rightHit) ? note.leftError :
                                (!note.leftHit && note.rightHit) ? note.rightError : Math.min(note.leftError, note.rightError);
                        if (error <= GOOD_WINDOW) {
                            gameScore += 960;
                        } else {
                            gameScore += 420;
                        }
                    }
                }
                currentJudgmentImage = imgBad;
                judgmentDisplayTime = System.currentTimeMillis();
                countBad++;
                iter.remove();
                continue;
            }
            double fraction = (double)(currentTime - (note.hitTime - travelTime)) / travelTime;
            note.x = (int)(laneRightX - fraction * (laneRightX - targetX));
        }
        if (notes.isEmpty() && currentTime > lastNoteTime + MAX_WINDOW + END_DELAY) {
            screen = 4;
            if (gameTimer != null) {
                gameTimer.stop();
            }
        }
    }

    private void checkHit(KeyEvent e) {
        long currentTime = System.currentTimeMillis() - songStartTime;
        int key = e.getKeyCode();
        if (key != KeyEvent.VK_S && key != KeyEvent.VK_K && key != KeyEvent.VK_A && key != KeyEvent.VK_L) {
            return;
        }
        for (Note note : new ArrayList<>(notes)) {
            // Small notes: types 0 = small Don, 1 = small Ka.
            if (note.type == 0 || note.type == 1) {
                int expectedSmallType = (key == KeyEvent.VK_S || key == KeyEvent.VK_K) ? 0 :
                        (key == KeyEvent.VK_A || key == KeyEvent.VK_L) ? 1 : -1;
                if (expectedSmallType == -1) continue;
                if (note.type == expectedSmallType) {
                    long error = Math.abs(note.hitTime - currentTime);
                    if (error <= MAX_WINDOW) {
                        notes.remove(note);
                        if (error <= GOOD_WINDOW) {
                            currentJudgmentImage = imgGood;
                            gameScore += 960;
                            countGood++;
                        } else if (error <= OK_WINDOW) {
                            currentJudgmentImage = imgOK;
                            gameScore += 420;
                            countOK++;
                        } else {
                            currentJudgmentImage = imgBad;
                            countBad++;
                        }
                        judgmentDisplayTime = System.currentTimeMillis();
                        return;
                    }
                }
            }
            // Big notes: types 2 = big Don, 3 = big Ka.
            else if (note.type == 2 || note.type == 3) {
                if (Math.abs(note.hitTime - currentTime) > MAX_WINDOW) {
                    continue;
                }
                if (note.type == 2) { // big Don: expect S (left) and K (right)
                    if (key == KeyEvent.VK_S && !note.leftHit) {
                        note.leftHit = true;
                        note.leftError = Math.abs(note.hitTime - currentTime);
                    } else if (key == KeyEvent.VK_K && !note.rightHit) {
                        note.rightHit = true;
                        note.rightError = Math.abs(note.hitTime - currentTime);
                    } else {
                        continue;
                    }
                } else if (note.type == 3) { // big Ka: expect A (left) and L (right)
                    if (key == KeyEvent.VK_A && !note.leftHit) {
                        note.leftHit = true;
                        note.leftError = Math.abs(note.hitTime - currentTime);
                    } else if (key == KeyEvent.VK_L && !note.rightHit) {
                        note.rightHit = true;
                        note.rightError = Math.abs(note.hitTime - currentTime);
                    } else {
                        continue;
                    }
                }
                if (note.leftHit && note.rightHit) {
                    if (note.leftError <= GOOD_WINDOW && note.rightError <= GOOD_WINDOW) {
                        currentJudgmentImage = imgGood;
                        gameScore += 1820;
                        countGood++;
                    } else {
                        currentJudgmentImage = imgOK;
                        gameScore += 420;
                        countOK++;
                    }
                    judgmentDisplayTime = System.currentTimeMillis();
                    notes.remove(note);
                    return;
                }
            }
        }
    }

    public void HandleGameBoard(KeyEvent e) {
        int keyCode = e.getKeyCode();
        int duration = 100;
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

        g.setColor(Color.WHITE);
        g.setFont(new Font("Meiryo", Font.BOLD, 40));
        if (songName.length() > 4)
            g.drawString(songName.substring(0, songName.length() - 4), 100, 50);

        int drumX = laneLeftX;
        int drumY = 120;
        int drumWidth = TaikoDrum.getWidth(null);
        int drumHeight = TaikoDrum.getHeight(null);

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

        int noteSize = 100;
        for (Note note : notes) {
            if (note.type == 1) { // small ka
                g.drawImage(noteKa, note.x, 150, note.x + noteSize, 150 + noteSize,
                        0, 0, noteKa.getWidth(null), noteKa.getHeight(null), this);
            } else if (note.type == 0) { // small don
                g.drawImage(noteDon, note.x, 150, note.x + noteSize, 150 + noteSize,
                        0, 0, noteDon.getWidth(null), noteDon.getHeight(null), this);
            } else if (note.type == 2) { // big don
                int newSize = (int) (noteSize * 1.25);
                g.drawImage(noteDonBig, note.x, 150, note.x + newSize, 150 + newSize,
                        0, 0, noteDonBig.getWidth(null), noteDonBig.getHeight(null), this);
            } else if (note.type == 3) { // big ka
                int newSize = (int) (noteSize * 1.25);
                g.drawImage(noteKaBig, note.x, 150, note.x + newSize, 150 + newSize,
                        0, 0, noteKaBig.getWidth(null), noteKaBig.getHeight(null), this);
            }
        }

        int targetX = laneLeftX + hitOffsetX;
        int judgeX = targetX + noteSize / 2;
        int judgeY = 150 + noteSize / 2;

        g.setColor(Color.YELLOW);
        g.drawOval(judgeX - judgeRadius, judgeY - judgeRadius, judgeRadius * 2, judgeRadius * 2);
        g.setColor(new Color(255, 255, 0, 50));
        g.fillOval(judgeX - judgeRadius, judgeY - judgeRadius, judgeRadius * 2, judgeRadius * 2);

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
