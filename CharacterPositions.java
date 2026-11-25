package p;

// Stores Pac-Man and ghost positions, directions, and movement speeds.
final class CharacterPositions {
    // Pac-Man's current movement and the direction requested by the player.
    int pacmanSpeed;
    int pacmanX;
    int pacmanY;
    int pacmanDirectionX;
    int pacmanDirectionY;
    int requestedDirectionX;
    int requestedDirectionY;

    // Ghost movement data is stored in matching array positions by ghost index.
    int ghostCount = 6;
    final int[] possibleDirectionX = new int[4];
    final int[] possibleDirectionY = new int[4];
    final int[] ghostX = new int[GameConstants.MAX_GHOSTS];
    final int[] ghostY = new int[GameConstants.MAX_GHOSTS];
    final int[] ghostDirectionX = new int[GameConstants.MAX_GHOSTS];
    final int[] ghostDirectionY = new int[GameConstants.MAX_GHOSTS];
    final int[] ghostSpeed = new int[GameConstants.MAX_GHOSTS];
    final double[] ghostSpeedFraction = new double[GameConstants.MAX_GHOSTS];
    final double[] ghostMoveAccumulator = new double[GameConstants.MAX_GHOSTS];
}
