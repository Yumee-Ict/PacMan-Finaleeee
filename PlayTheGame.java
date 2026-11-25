package p;

import javax.swing.SwingUtilities;
import pacman.App;

// Alternate entry point that opens the main Pac-Man application.
public class PlayTheGame {

    public static void main(String[] args) {
        // Create the Swing app on the event thread, then show its window.
        SwingUtilities.invokeLater(() -> {
            App game = new App();
            game.setVisible(true);
        });
    }
}
