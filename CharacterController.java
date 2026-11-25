package p;

import java.awt.Graphics2D;
import java.security.SecureRandom;

// Handles movement, wall checks, pellet collection, and collisions.
final class CharacterController {
    private final Model2 game;
    private final CharacterPositions characters;
    private final GameState state;
    private final SecureRandom directionRandom = new SecureRandom();

    // Reuse the game's shared state so movement changes are visible everywhere.
    CharacterController(Model2 game) {
        this.game = game;
        characters = game.characters;
        state = game.state;
    }

    // Move and draw each ghost that is active on the current level.
    void moveGhosts(Graphics2D graphics) {
        for (int ghostIndex = 0; ghostIndex < characters.ghostCount; ghostIndex++) {
            moveGhost(graphics, ghostIndex);
        }
    }

    private void moveGhost(Graphics2D graphics, int ghostIndex) {
        // Pick a new direction only when the ghost reaches a maze intersection.
        if (characters.ghostX[ghostIndex] % GameConstants.BLOCK_SIZE == 0
                && characters.ghostY[ghostIndex] % GameConstants.BLOCK_SIZE == 0) {
            chooseGhostDirection(ghostIndex);
        }

        // Add fractional speed over time so non-integer speeds work smoothly.
        characters.ghostMoveAccumulator[ghostIndex] +=
                characters.ghostSpeedFraction[ghostIndex];
        int movementSpeed = characters.ghostSpeed[ghostIndex];
        if (characters.ghostMoveAccumulator[ghostIndex] >= 1) {
            movementSpeed++;
            characters.ghostMoveAccumulator[ghostIndex] -= 1;
        }

        int distanceToNextCell = distanceToNextCell(characters.ghostX[ghostIndex],
                characters.ghostDirectionX[ghostIndex]);
        if (distanceToNextCell == 0) {
            distanceToNextCell = distanceToNextCell(characters.ghostY[ghostIndex],
                    characters.ghostDirectionY[ghostIndex]);
        }
        if (distanceToNextCell > 0) {
            movementSpeed = Math.min(movementSpeed, distanceToNextCell);
        }

        int nextGhostX = characters.ghostX[ghostIndex]
                + characters.ghostDirectionX[ghostIndex] * movementSpeed;
        int nextGhostY = characters.ghostY[ghostIndex]
                + characters.ghostDirectionY[ghostIndex] * movementSpeed;

        if (nextGhostX < 0) {
            nextGhostX = 0;
            characters.ghostDirectionX[ghostIndex] = 0;
            characters.ghostDirectionY[ghostIndex] = 1;
        } else if (nextGhostX > GameConstants.SCREEN_SIZE - GameConstants.BLOCK_SIZE) {
            nextGhostX = GameConstants.SCREEN_SIZE - GameConstants.BLOCK_SIZE;
            characters.ghostDirectionX[ghostIndex] = 0;
            characters.ghostDirectionY[ghostIndex] = -1;
        }

        if (nextGhostY < 0) {
            nextGhostY = 0;
            characters.ghostDirectionY[ghostIndex] = 0;
            characters.ghostDirectionX[ghostIndex] = 1;
        } else if (nextGhostY > GameConstants.SCREEN_SIZE - GameConstants.BLOCK_SIZE) {
            nextGhostY = GameConstants.SCREEN_SIZE - GameConstants.BLOCK_SIZE;
            characters.ghostDirectionY[ghostIndex] = 0;
            characters.ghostDirectionX[ghostIndex] = -1;
        }

        characters.ghostX[ghostIndex] = nextGhostX;
        characters.ghostY[ghostIndex] = nextGhostY;
        game.renderer.drawGhost(graphics, nextGhostX + 1,
                nextGhostY + GameConstants.MAZE_TOP_OFFSET + 1);
        if (isGhostCollidingWithPacman(ghostIndex)) {
            state.dying = true;
        }
    }

    private int distanceToNextCell(int position, int direction) {
        int offset = position % GameConstants.BLOCK_SIZE;
        if (direction > 0) {
            return GameConstants.BLOCK_SIZE - offset;
        }
        if (direction < 0) {
            return offset == 0 ? GameConstants.BLOCK_SIZE : offset;
        }
        return 0;
    }

    private void chooseGhostDirection(int ghostIndex) {
        int mazeCellIndex = characters.ghostX[ghostIndex] / GameConstants.BLOCK_SIZE
                + GameConstants.N_BLOCKS
                * (characters.ghostY[ghostIndex] / GameConstants.BLOCK_SIZE);
        int openDirectionCount = 0;
        openDirectionCount = addGhostDirection(openDirectionCount, ghostIndex, mazeCellIndex, 1, -1, 0);
        openDirectionCount = addGhostDirection(openDirectionCount, ghostIndex, mazeCellIndex, 2, 0, -1);
        openDirectionCount = addGhostDirection(openDirectionCount, ghostIndex, mazeCellIndex, 4, 1, 0);
        openDirectionCount = addGhostDirection(openDirectionCount, ghostIndex, mazeCellIndex, 8, 0, 1);

        if (characters.ghostX[ghostIndex] > 360 - GameConstants.BLOCK_SIZE) {
            characters.ghostX[ghostIndex] = 0;
        }
        if (characters.ghostX[ghostIndex] < 0) {
            characters.ghostX[ghostIndex] = 360 - GameConstants.BLOCK_SIZE;
        }

        if (openDirectionCount == 0) {
            reverseOrStopGhost(ghostIndex, mazeCellIndex);
        } else {
            int selectedDirectionIndex = directionRandom.nextInt(openDirectionCount);
            characters.ghostDirectionX[ghostIndex] = characters.possibleDirectionX[selectedDirectionIndex];
            characters.ghostDirectionY[ghostIndex] = characters.possibleDirectionY[selectedDirectionIndex];
        }
    }

    private int addGhostDirection(int openDirectionCount, int ghostIndex, int mazeCellIndex,
                                  int wallMask, int directionX, int directionY) {
        boolean canContinue;
        if (directionX == 0) {
            canContinue = characters.ghostDirectionY[ghostIndex] != -directionY;
        } else {
            canContinue = characters.ghostDirectionX[ghostIndex] != -directionX;
        }
        if ((state.screenData[mazeCellIndex] & wallMask) == 0 && canContinue) {
            characters.possibleDirectionX[openDirectionCount] = directionX;
            characters.possibleDirectionY[openDirectionCount] = directionY;
            return openDirectionCount + 1;
        }
        return openDirectionCount;
    }

    private void reverseOrStopGhost(int ghostIndex, int mazeCellIndex) {
        if ((state.screenData[mazeCellIndex] & 15) == 15) {
            characters.ghostDirectionX[ghostIndex] = 0;
            characters.ghostDirectionY[ghostIndex] = 0;
        } else {
            characters.ghostDirectionX[ghostIndex] = -characters.ghostDirectionX[ghostIndex];
            characters.ghostDirectionY[ghostIndex] = -characters.ghostDirectionY[ghostIndex];
        }
    }

    private boolean isGhostCollidingWithPacman(int ghostIndex) {
        return characters.pacmanX > characters.ghostX[ghostIndex] - 12
                && characters.pacmanX < characters.ghostX[ghostIndex] + 12
                && characters.pacmanY > characters.ghostY[ghostIndex] - 12
                && characters.pacmanY < characters.ghostY[ghostIndex] + 12
                && state.inGame;
    }

    // Move Pac-Man and check for pellets or a wall when he reaches a cell.
    void movePacman() {
        if (characters.pacmanX % GameConstants.BLOCK_SIZE == 0
                && characters.pacmanY % GameConstants.BLOCK_SIZE == 0) {
            movePacmanAtIntersection();
        }
        characters.pacmanX += characters.pacmanSpeed * characters.pacmanDirectionX;
        characters.pacmanY += characters.pacmanSpeed * characters.pacmanDirectionY;
        keepPacmanInsideMaze();
    }

    private void movePacmanAtIntersection() {
        int mazeCellIndex = characters.pacmanX / GameConstants.BLOCK_SIZE
                + GameConstants.N_BLOCKS * (characters.pacmanY / GameConstants.BLOCK_SIZE);
        short cell = state.screenData[mazeCellIndex];

        // A pellet is marked with bit 16; clear it so it cannot score twice.
        if ((cell & 16) != 0) {
             state.screenData[mazeCellIndex] = (short) (cell & 15);
            state.score += state.level * characters.ghostCount;
        }

        if ((characters.requestedDirectionX != 0 || characters.requestedDirectionY != 0)
                && !isWallBlocking(cell, characters.requestedDirectionX, characters.requestedDirectionY)) {
            characters.pacmanDirectionX = characters.requestedDirectionX;
            characters.pacmanDirectionY = characters.requestedDirectionY;
        }

        if (isWallBlocking(cell, characters.pacmanDirectionX, characters.pacmanDirectionY)) {
            characters.pacmanDirectionX = 0;
            characters.pacmanDirectionY = 0;
        }
    }

    // The low four bits mark walls on the left, top, right, and bottom.
    private boolean isWallBlocking(short cell, int directionX, int directionY) {
        return (directionX == -1 && directionY == 0 && (cell & 1) != 0)
                || (directionX == 1 && directionY == 0 && (cell & 4) != 0)
                || (directionX == 0 && directionY == -1 && (cell & 2) != 0)
                || (directionX == 0 && directionY == 1 && (cell & 8) != 0);
    }

    // Allow travel through open edges, but stop Pac-Man at closed maze edges.
    private void keepPacmanInsideMaze() {
        keepPacmanOnHorizontalBounds();
        keepPacmanOnVerticalBounds();
    }

    private void keepPacmanOnHorizontalBounds() {
        int row = characters.pacmanY / GameConstants.BLOCK_SIZE;
        if (row < 0) {
            row = 0;
        } else if (row >= GameConstants.N_BLOCKS) {
            row = GameConstants.N_BLOCKS - 1;
        }
        int leftEdge = state.screenData[row * GameConstants.N_BLOCKS];
        int rightEdge = state.screenData[row * GameConstants.N_BLOCKS + GameConstants.N_BLOCKS - 1];
        boolean horizontalPassage = (leftEdge & 1) == 0 && (rightEdge & 4) == 0;

        if (characters.pacmanX >= GameConstants.SCREEN_SIZE) {
            if (horizontalPassage) {
                characters.pacmanX = 0;
            } else {
                characters.pacmanX = GameConstants.SCREEN_SIZE - GameConstants.BLOCK_SIZE;
                characters.pacmanDirectionX = 0;
                characters.requestedDirectionX = 0;
            }
        } else if (characters.pacmanX < 0) {
            if (horizontalPassage) {
                characters.pacmanX = GameConstants.SCREEN_SIZE - GameConstants.BLOCK_SIZE;
            } else {
                characters.pacmanX = 0;
                characters.pacmanDirectionX = 0;
                characters.requestedDirectionX = 0;
            }
        }
    }

    private void keepPacmanOnVerticalBounds() {
        int column = characters.pacmanX / GameConstants.BLOCK_SIZE;
        if (column < 0) {
            column = 0;
        } else if (column >= GameConstants.N_BLOCKS) {
            column = GameConstants.N_BLOCKS - 1;
        }
        int topEdge = state.screenData[column];
        int bottomEdge = state.screenData[(GameConstants.N_BLOCKS - 1) * GameConstants.N_BLOCKS + column];
        boolean verticalPassage = (topEdge & 2) == 0 && (bottomEdge & 8) == 0;

        if (characters.pacmanY >= GameConstants.SCREEN_SIZE) {
            if (verticalPassage) {
                characters.pacmanY = 0;
            } else {
                characters.pacmanY = GameConstants.SCREEN_SIZE - GameConstants.BLOCK_SIZE;
                characters.pacmanDirectionY = 0;
                characters.requestedDirectionY = 0;
            }
        } else if (characters.pacmanY < 0) {
            if (verticalPassage) {
                characters.pacmanY = GameConstants.SCREEN_SIZE - GameConstants.BLOCK_SIZE;
            } else {
                characters.pacmanY = 0;
                characters.pacmanDirectionY = 0;
                characters.requestedDirectionY = 0;
            }
        }
    }
}
