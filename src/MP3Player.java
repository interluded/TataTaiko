import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.advanced.AdvancedPlayer;
import java.io.FileInputStream;
import java.io.IOException;

public class MP3Player implements Runnable {
    private String filePath;
    private AdvancedPlayer player;
    private Thread playThread;

    public MP3Player(String filePath) {
        this.filePath = filePath;
    }

    public void play() {
        playThread = new Thread(this);
        playThread.start();
    }

    public void stop() {
        if (player != null) {
            player.close();
            player = null;
        }
        if (playThread != null && playThread.isAlive()) {
            playThread.interrupt();
            playThread = null;
        }
    }


    @Override
    public void run() {
        try {
            FileInputStream fileStream = new FileInputStream(filePath);
            player = new AdvancedPlayer(fileStream);
            player.play();
        } catch (JavaLayerException | IOException e) {
            e.printStackTrace();
        }
    }
}
