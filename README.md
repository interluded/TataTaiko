# TataTaiko

TataTaiko is a Java-based Taiko Drum Simulator built with Swing and AWT. It mimics the classic Taiko game by displaying falling notes that the player must hit in time with the music. The game supports both small and big notes with distinct scoring rules and is optimized to update at approximately 144 Hz.

## Features

- **Multiple Screens:** Title, Song Selection, Gameplay, and End Screen.
- **Note Timing & Scoring:** Judged as Good, OK, or Bad based on your timing.
- **Small and Big Notes:**  
  - **Small Notes:**  
    - **Type 0:** Lil Don  
    - **Type 1:** Lil Ka  
  - **Big Notes:**  
    - **Type 2:** Big Don  
    - **Type 3:** Big Ka  
- **High Frame Rate Support:** Uses a Swing Timer with a ~7 ms delay to target 144 frames per second.
- **MP3 Playback:** Plays theme music and in-game songs using an MP3Player based on jLayer.
- **Note File Support:** A separate parser converts .tja files to the .bin format used by this simulator.

## Requirements

- **Java Development Kit (JDK):** Java 8 or higher.
- **jLayer Library:** For MP3 playback (ensure that `javazoom.jl.player.advanced.AdvancedPlayer` is available).
- **Assets Directory Structure:**  
  - `Assets/1_Title/` – Title screen images (backgrounds, cursors, etc.)  
  - `Assets/2_SongSelect/` – Song selection backgrounds  
  - `Assets/3_InGame/` – In-game images, including:  
    - Drum images: `Taiko/Base.png`, `Taiko/Don.png`, `Taiko/Ka.png`  
    - Lane image: `Lane.png`  
    - Falling note images:  
      - Small notes: `Don.png` and `Ka.png`  
      - Big notes: `DonBig.png` and `KaBig.png`  
    - Background image: `Background.png`  
  - `Assets/4_GameDone/` – End screen images (background, JudgeMeter, etc.)  
  - `Assets/Sounds/` – Sound files (e.g., `Don.mp3`, `Ka.mp3`, `theme.mp3`)


## Usage

- **Title Screen:**  
  The theme song plays when you first reach the title screen. Use the arrow keys to navigate and press Enter to select.

- **Song Selection:**  
  Navigate with the arrow keys and press Enter to select a song or exit.

- **Gameplay:**  
  - **Small Notes:**  
    - **Type 0 (Lil Don):** Hit with **S** or **K**.  
    - **Type 1 (Lil Ka):** Hit with **A** or **L**.
  - **Big Notes:**  
    - **Type 2 (Big Don):** Hit with **S** (left) and **K** (right). Full score if both are hit within the “Good” window.  
    - **Type 3 (Big Ka):** Hit with **A** (left) and **L** (right).  
  The game judges your timing as Good, OK, or Bad and updates your score accordingly.

- **End Screen:**  
  After the last note, the game waits 3.5 seconds before displaying your final score. Press Enter to return to the title screen or Escape to exit.

## High Refresh Rate Support

The game is designed to update at roughly 144 Hz by using a Swing Timer with a 7 ms delay:
```java
gameTimer = new Timer(7, new ActionListener() {
    public void actionPerformed(ActionEvent e) {
        updateNotes();
        repaint();
    }
});
```
Keep in mind that the actual refresh rate depends on your monitor, OS scheduling, and Java’s rendering performance.

## Customization

- **Frame Rate:** Adjust the delay value (currently 7 ms) if you need a different update frequency.
- **Scoring & Timing:** Modify the `GOOD_WINDOW`, `OK_WINDOW`, and `MAX_WINDOW` values along with score increments in the code to fine-tune gameplay.
- **Assets:** Replace images or sound files by updating the corresponding files in their respective directories. If you change file names, update the code accordingly.
- **Note Parser:** A separate parser script converts .tja files to the .bin format used by this simulator. See the provided parser for instructions.

## Known Issues
- **MP3 Playback:** MP3 playback is handled by jLayer; ensure you have the correct version installed.

## Credits

Developed by Marcus Kongjika.

Assets and libraries are credited as appropriate.  
MP3 playback is implemented using the jLayer library by JavaZoom.

## License
See LICENCE.md
