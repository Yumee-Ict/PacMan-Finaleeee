package p;

// Holds game progress and status separately from movement and drawing code.
final class GameState {
    // These flags describe whether play is active, ending, or won.
    boolean inGame;
    boolean dying;
    boolean gameWon;
    long gameStartedAtNanos;
    long gameCompletedAtNanos;

    // Current progress shown in the HUD and level banner.
    int lives;
    int score;
    int level = 1;
    int levelBannerTimer;
    boolean showLevelBanner;

    final String[] levelDifficulties = {
        "Easy", "Normal", "Hard", "Extra Hard", "Ultra Hard"
    };

    // Each number represents one maze cell, including walls and remaining items.
    final short[] screenData = new short[GameConstants.N_BLOCKS * GameConstants.N_BLOCKS];
}
