package pacman;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import p.Model2;

// Builds the main window and places the game, settings, and leaderboard controls in it.
public class App extends JFrame {
    private static final String GAME_CARD = "game";
    private static final String SETTINGS_CARD = "settings";

    private final Model2 game = new Model2();
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);

    // Create the game screen and the buttons that open the other features.
    public App() {
        setTitle("Pacman");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        JButton leaderboardButton = new JButton("Leaderboard");
        leaderboardButton.setFocusable(false);
        leaderboardButton.addActionListener(event -> game.showLeaderboard());

        JButton settingsButton = new JButton("Settings");
        settingsButton.setFocusable(false);
        settingsButton.addActionListener(event -> showSettings());

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        controls.add(settingsButton);
        controls.add(leaderboardButton);

        content.add(game, GAME_CARD);
        content.add(new p.SettingsMenuController(this::showGame), SETTINGS_CARD);

        add(controls, BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);
        pack();
        setExtendedState(Frame.MAXIMIZED_BOTH);
    }

    // Switch from the game screen to the settings screen.
    private void showSettings() {
        cards.show(content, SETTINGS_CARD);
    }

    // Return to the game screen and give it keyboard focus.
    private void showGame() {
        cards.show(content, GAME_CARD);
        game.requestFocusInWindow();
    }

    @SuppressWarnings("java:S1172")
    public static void main(String[] args) {
        // Swing windows should be created on Swing's event thread.
        SwingUtilities.invokeLater(() -> {
            App pac = new App();
            pac.setVisible(true);
            pac.game.requestFocusInWindow();
        });
    }
}
