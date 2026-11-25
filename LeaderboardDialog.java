package p;

import java.util.List;
import java.util.prefs.BackingStoreException;
import javax.swing.JOptionPane;

// Handles the pop-up windows for saving and viewing leaderboard times.
final class LeaderboardDialog {
    private static final String ERROR_TITLE = "Leaderboard Error";
    private final Model2 game;

    // Keep the game reference so the pop-ups can appear over the game window.
    LeaderboardDialog(Model2 game) {
        this.game = game;
    }

    // Ask for a name only when the finished game time qualifies for the top five.
    void promptForEntry() {
        try {
            long completionTime = Math.max(0,
                    (game.state.gameCompletedAtNanos - game.state.gameStartedAtNanos) / 1_000_000L);
            if (!Leaderboard.qualifies(Leaderboard.getEntries(), completionTime)) {
                return;
            }

            // Let the player cancel, or save their entered name with the time.
            String input = JOptionPane.showInputDialog(game,
                    "Your time: " + Leaderboard.formatTime(completionTime) + "\nEnter your name:",
                    "New Top 5 Record", JOptionPane.QUESTION_MESSAGE);
            if (input == null) {
                return;
            }

            String name = input.trim();
            if (name.isEmpty()) {
                name = "Player";
            }
            Leaderboard.addEntry(name, completionTime);
            showLeaderboard();
        } catch (BackingStoreException | IllegalStateException | SecurityException ex) {
            // Show a clear message if the saved leaderboard cannot be updated.
            JOptionPane.showMessageDialog(game,
                    "Could not save the leaderboard: " + ex.getMessage(),
                    ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
        }
    }

    // Load the saved records and show them from fastest to slowest.
    void showLeaderboard() {
        try {
            List<Leaderboard.Entry> entries = Leaderboard.getEntries();
            StringBuilder text = new StringBuilder();
            if (entries.isEmpty()) {
                text.append("No records yet. Win the game to set the first time!");
            } else {
                // Add each record's place, player name, and formatted time.
                for (int entryIndex = 0; entryIndex < entries.size(); entryIndex++) {
                    Leaderboard.Entry entry = entries.get(entryIndex);
                    text.append(entryIndex + 1)
                            .append(". ")
                            .append(entry.name)
                            .append(" - ")
                            .append(Leaderboard.formatTime(entry.timeMillis))
                            .append('\n');
                }
            }
            JOptionPane.showMessageDialog(game, text.toString(),
                    "Leaderboard - Top 5", JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalStateException | SecurityException ex) {
            // Tell the player if the saved records could not be read.
            JOptionPane.showMessageDialog(game,
                    "Could not load the leaderboard: " + ex.getMessage(),
                    ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
        }
    }
}
