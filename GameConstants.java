package p;

import java.awt.Color;

// Shared sizes, limits, and colors used to draw and position the game.
final class GameConstants {
    static final int BLOCK_SIZE = 24;
    static final int N_BLOCKS = 15;
    static final int SCREEN_SIZE = N_BLOCKS * BLOCK_SIZE;
    static final int MAZE_TOP_OFFSET = BLOCK_SIZE;
    static final int MAX_GHOSTS = 5;

    static final Color LIGHT_PURPLE = new Color(200, 162, 255);
    static final Color BACKGROUND_COLOR = new Color(230, 214, 255);

    // Prevent creating this utility class; its values are all static.
    private GameConstants() {
    }
}
