import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setBounds(0, 0, 1920, 1080);
        frame.setTitle("TataTaiko");
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        TataTaiko game = new TataTaiko();
        frame.add(game);
        frame.setVisible(true);
    }
}