package p;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.security.SecureRandom;
import java.util.List;
import java.util.prefs.BackingStoreException;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

// Main game panel: stores game data, runs the timer, handles input, and coordinates play.
public class Model2 extends JPanel implements ActionListener {

    private static final String LEADERBOARD_ERROR_TITLE = "Leaderboard Error";
    private static final String LEADERBOARD_SAVE_ERROR = "Could not save the leaderboard: ";

    public Dimension panelSize;
    public final Font smallFont = new Font(Font.MONOSPACED, Font.BOLD, 14);

    // These flags decide whether the game is playing, ending, or finished.
    private boolean inGame = false;
    private boolean dying = false;
    private boolean gameWon;
    private long gameStartedAtNanos;
    private long gameCompletedAtNanos;

    // The maze is a 15-by-15 grid; each cell is 24 pixels wide and tall.
    private static final int BLOCK_SIZE = 24;
    private static final int N_BLOCKS = 15;
    private static final int SCREEN_SIZE = N_BLOCKS * BLOCK_SIZE;
    private static final int MAZE_TOP_OFFSET = BLOCK_SIZE;
    private static final int MAX_GHOSTS = 5;
    private static final Color LIGHT_PURPLE = new Color(200, 162, 255);
    private static final Color WALL_PIXEL_DARK = new Color(163, 126, 216);
    private static final Color WALL_PIXEL_HIGHLIGHT = new Color(235, 218, 255);
    private static final int WALL_PIXEL_SIZE = 6;
    private static final Color BACKGROUND_COLOR = new Color(230, 214, 255);
    // Character movement and score values are updated as the game runs.
    private int pacmanSpeed;

    private int ghostCount = 6;
    private int lives;
    private int score;
    private int[] possibleDirectionX;
    private int[] possibleDirectionY;
    private int[] ghostX;
    private int[] ghostY;
    private int[] ghostDirectionX;
    private int[] ghostDirectionY;
    private int[] ghostSpeed;

    // Images are created when the game starts and used while drawing.
    private transient Image heart;
    private transient Image ghost;
    private transient Image strawberry;
    private transient Image up;
    private transient Image down;
    private transient Image left;
    private transient Image right;

    // Positions use pixels; directions use -1, 0, or 1 for each axis.
    private int pacmanX;
    private int pacmanY;
    private int pacmanDirectionX;
    private int pacmanDirectionY;
    private int requestedDirectionX;
    private int requestedDirectionY;

    // Level and banner values control the maze and its start-of-level message.
    private int level = 1;
    private int levelBannerTimer;
    private boolean showLevelBanner;
    private final String[] levelDifficulties = {
            "Easy", "Normal", "Hard", "Extra Hard", "Ultra Hard"
    };

    // Maze cells store wall and pellet flags as bits inside each short value.
    private final int[] validSpeeds = {2, 2, 3, 3, 3};
    private final SecureRandom ghostDirectionRandom = new SecureRandom();
    private short[] screenData;
    private Timer timer;
    private final transient SoundManager soundManager = new SoundManager();
    public transient GameState state;
    public transient CharacterPositions characters;
    public transient GameAssets assets;
    public transient GameRenderer renderer;

    // Set up game data, drawing assets, keyboard controls, and the game timer.
    public Model2() {
        state = new GameState();
        characters = new CharacterPositions();
        assets = new GameAssets();
        loadImages();
        initVariables();
        renderer = new GameRenderer(this);
        setBackground(BACKGROUND_COLOR);
        addKeyListener(new TAdapter());
        setFocusable(true);
        initGame();
    }

    // Create all images used by the game characters and HUD.
    private void loadImages() {
        down = createDirectionalImage(Color.YELLOW, Color.ORANGE, 20, 20, 90);
        up = createDirectionalImage(Color.YELLOW, Color.ORANGE, 20, 20, -90);
        left = createDirectionalImage(Color.YELLOW, Color.ORANGE, 20, 20, 180);
        right = createDirectionalImage(Color.YELLOW, Color.ORANGE, 20, 20, 0);
        ghost = createGhostImage();
        heart = createHeartImage();
        strawberry = createStrawberryImage();
    }

    // Draw Pac-Man facing right, then rotate the image for the other directions.
    private BufferedImage createDirectionalImage(Color body, Color mouth, int width, int height, int rotation) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D imageGraphics = image.createGraphics();
        imageGraphics.rotate(Math.toRadians(rotation), width / 2.0, height / 2.0);
        imageGraphics.setColor(body);
        imageGraphics.fillArc(0, 0, width, height, 30, 300);
        imageGraphics.setColor(mouth);
        imageGraphics.fillArc(0, 0, width, height, 30, 90);
        imageGraphics.dispose();
        return image;
    }

    // Build the ghost image from simple colored shapes.
    private BufferedImage createGhostImage() {
        BufferedImage image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        Graphics2D imageGraphics = image.createGraphics();
        imageGraphics.setColor(new Color(255, 100, 100));
        imageGraphics.fillRoundRect(2, 2, 16, 14, 8, 8);
        imageGraphics.fillRect(2, 10, 16, 6);
        imageGraphics.setColor(Color.WHITE);
        imageGraphics.fillOval(5, 5, 4, 4);
        imageGraphics.fillOval(11, 5, 4, 4);
        imageGraphics.dispose();
        return image;
    }

    // Build the heart icon used to show remaining lives.
    private BufferedImage createHeartImage() {
        BufferedImage image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        Graphics2D imageGraphics = image.createGraphics();
        imageGraphics.setColor(Color.RED);
        imageGraphics.fillOval(2, 2, 8, 8);
        imageGraphics.fillOval(10, 2, 8, 8);
        int[] xPoints = {2, 10, 18, 10, 2};
        int[] yPoints = {6, 2, 6, 18, 18};
        imageGraphics.fillPolygon(xPoints, yPoints, 5);
        imageGraphics.dispose();
        return image;
    }

    // Build the strawberry collectible image from basic shapes.
    private BufferedImage createStrawberryImage() {
        BufferedImage image = new BufferedImage(18, 18, BufferedImage.TYPE_INT_ARGB);
        Graphics2D imageGraphics = image.createGraphics();

        imageGraphics.setColor(new Color(220, 25, 45));
        int[] bodyX = {4, 13, 16, 15, 12, 9, 6, 3, 2};
        int[] bodyY = {4, 4, 7, 11, 14, 17, 14, 11, 7};
        imageGraphics.fillPolygon(bodyX, bodyY, bodyX.length);
        imageGraphics.setColor(new Color(40, 170, 55));
        int[] leafX = {8, 3, 6, 5, 9, 11, 13, 14, 10};
        int[] leafY = {5, 2, 2, 0, 3, 0, 2, 2, 5};
        imageGraphics.fillPolygon(leafX, leafY, leafX.length);

        imageGraphics.setColor(new Color(255, 230, 120));
        imageGraphics.fillRect(6, 7, 2, 2);
        imageGraphics.fillRect(11, 8, 2, 2);
        imageGraphics.fillRect(8, 11, 2, 2);
        imageGraphics.fillRect(5, 10, 2, 2);

        imageGraphics.dispose();
        return image;
    }

    // Create the maze data and start a timer that refreshes the screen.
    private void initVariables() {
        screenData = new short[N_BLOCKS * N_BLOCKS];
        panelSize = new Dimension(400, SCREEN_SIZE + 2 * BLOCK_SIZE);
        setPreferredSize(panelSize);
        ghostX = new int[MAX_GHOSTS];
        ghostDirectionX = new int[MAX_GHOSTS];
        ghostY = new int[MAX_GHOSTS];
        ghostDirectionY = new int[MAX_GHOSTS];
        ghostSpeed = new int[MAX_GHOSTS];
        possibleDirectionX = new int[4];
        possibleDirectionY = new int[4];

        timer = new Timer(40, this);
        timer.start();
    }

    // Run one game update: move characters, draw them, and check level progress.
    public void playGame(Graphics2D graphics) {
        if (dying) {
            death();

        } else {
            movePacman();
            drawPacman(graphics);
            moveGhosts(graphics);
            checkMaze();
        }

    }

    private void showIntroScreen(Graphics2D graphics) {
        String startMessage = "Press SPACE to Start The Game";
        graphics.setColor(Color.black);
        graphics.drawString(startMessage, (SCREEN_SIZE)/6, MAZE_TOP_OFFSET + 150);
    }

    private void drawScore(Graphics2D graphics) {
        int bottomHudY = MAZE_TOP_OFFSET + SCREEN_SIZE;
        graphics.setColor(BACKGROUND_COLOR);
        graphics.fillRect(0, 0, SCREEN_SIZE, BLOCK_SIZE);
        graphics.fillRect(0, bottomHudY, SCREEN_SIZE, BLOCK_SIZE);
        graphics.setColor(LIGHT_PURPLE);
        graphics.drawRect(2, 1, SCREEN_SIZE - 5, BLOCK_SIZE - 3);
        graphics.drawRect(2, bottomHudY + 1, SCREEN_SIZE - 5, BLOCK_SIZE - 3);

        graphics.setFont(smallFont);

        graphics.setColor(Color.black);
        String levelText =("level: " + level);
        graphics.drawString(levelText, 10, 17);

        String scoreText = "Score: " + score;
        int scoreX = SCREEN_SIZE - 10 - graphics.getFontMetrics().stringWidth(scoreText);
        graphics.drawString(scoreText, scoreX, 17);

        for (int lifeIndex = 0; lifeIndex < lives; lifeIndex++) {
            graphics.drawImage(heart, lifeIndex * 28 + 8, bottomHudY + 2, this);
        }

        graphics.setColor(Color.black);
        graphics.drawString("Time: " + getElapsedTime(), 245, bottomHudY + 18);
    }

    // Show elapsed time while playing, or the final time after the game ends.
    public String getElapsedTime() {
        if (gameStartedAtNanos == 0) {
            return "00:00";
        }

        long endTime;
        if (gameCompletedAtNanos != 0) {
            endTime = gameCompletedAtNanos;
        } else if (inGame) {
            endTime = System.nanoTime();
        } else {
            endTime = gameStartedAtNanos;
        }
        long elapsedSeconds = (endTime - gameStartedAtNanos) / 1_000_000_000L;
        long minutes = elapsedSeconds / 60;
        long seconds = elapsedSeconds % 60;
        String minuteText = "" + minutes;
        String secondText = "" + seconds;

        if (minutes < 10) {
            minuteText = "0" + minutes;
        }
        if (seconds < 10) {
            secondText = "0" + seconds;
        }

        return minuteText + ":" + secondText;
    }

    // Convert the finished game's elapsed time from nanoseconds to milliseconds.
    private long getCompletedTimeMillis() {
        long completedTime = (gameCompletedAtNanos - gameStartedAtNanos) / 1_000_000L;
        if (completedTime < 0) {
            completedTime = 0;
        }
        return completedTime;
    }

    // Ask for a player name if the completed time qualifies for the top five.
    private void promptForLeaderboardEntry() {
        try {
            long completionTime = getCompletedTimeMillis();
            if (!Leaderboard.qualifies(Leaderboard.getEntries(), completionTime)) {
                return;
            }

            String input = JOptionPane.showInputDialog(this,
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
            JOptionPane.showMessageDialog(this,
                    LEADERBOARD_SAVE_ERROR + ex.getMessage(),
                    LEADERBOARD_ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
        }
    }

    // Read saved records and display them in a pop-up window.
    public void showLeaderboard() {
        try {
            List<Leaderboard.Entry> entries = Leaderboard.getEntries();
            StringBuilder text = new StringBuilder();
            if (entries.isEmpty()) {
                text.append("No records yet. Win the game to set the first time!");
            } else {
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
            JOptionPane.showMessageDialog(this, text.toString(),
                    "Leaderboard - Top 5", JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalStateException | SecurityException ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not load the leaderboard: " + ex.getMessage(),
                    LEADERBOARD_ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
        }
    }

    private void startLevelBanner() {
        showLevelBanner = true;
        levelBannerTimer = 1000;
    }

    private void drawLevelBanner(Graphics2D graphics) {
        graphics.setFont(new Font(Font.MONOSPACED, Font.BOLD, 24));
        graphics.setColor(new Color(255, 255, 255, 220));
        FontMetrics metrics = graphics.getFontMetrics();
        String levelText = "LEVEL " + level;
        String difficultyText = levelDifficulties[level - 1];
        difficultyText = difficultyText.toUpperCase();
        int levelX = (SCREEN_SIZE - metrics.stringWidth(levelText)) / 2;
        int difficultyX = (SCREEN_SIZE - metrics.stringWidth(difficultyText)) / 2;
        int levelY = MAZE_TOP_OFFSET + SCREEN_SIZE / 2 - metrics.getHeight() / 2;
        graphics.drawString(levelText, levelX, levelY);
        graphics.drawString(difficultyText, difficultyX, levelY + metrics.getHeight());
    }

    // Advance to the next level when all pellets are gone, or finish after level five.
    private void checkMaze() {
        int mazeCellIndex = 0;
        boolean finished = true;

        while (mazeCellIndex < N_BLOCKS * N_BLOCKS && finished) {
            if ((screenData[mazeCellIndex] & 48) != 0) {
                finished = false;
            }
            mazeCellIndex++;
        }
        if (finished) {
            if (level == levelDifficulties.length) {
                gameWon = true;
                inGame = false;
                gameCompletedAtNanos = System.nanoTime();
                showLevelBanner = false;
                soundManager.playCongratulations();
                SwingUtilities.invokeLater(this::promptForLeaderboardEntry);
                return;
            }

            level++;

            startLevelBanner();

            if (ghostCount < MAX_GHOSTS) {
                ghostCount++;
            }

            initLevel(level);
        }
    }

    // Remove one life and either reset the current level or end the game.
    private void death() {
        lives--;
        if (lives == 0) {
            inGame = false;
            dying = false;
            gameCompletedAtNanos = System.nanoTime();
            soundManager.playFailed();
        } else {
            continueLevel();
        }
    }

    // Update and draw each ghost that belongs to the current level.
    private void moveGhosts(Graphics2D graphics) {
        for (int ghostIndex = 0; ghostIndex < ghostCount; ghostIndex++) {
            moveGhost(graphics, ghostIndex);
        }
    }

    private void moveGhost(Graphics2D graphics, int ghostIndex) {
        if (ghostX[ghostIndex] % BLOCK_SIZE == 0 && ghostY[ghostIndex] % BLOCK_SIZE == 0) {
            chooseGhostDirection(ghostIndex);
        }

        int nextGhostX = ghostX[ghostIndex] + ghostDirectionX[ghostIndex] * ghostSpeed[ghostIndex];
        int nextGhostY = ghostY[ghostIndex] + ghostDirectionY[ghostIndex] * ghostSpeed[ghostIndex];

        if (nextGhostX < 0) {
            nextGhostX = 0;
            ghostDirectionX[ghostIndex] = 0;
            ghostDirectionY[ghostIndex] = 1;
        } else if (nextGhostX > SCREEN_SIZE - BLOCK_SIZE) {
            nextGhostX = SCREEN_SIZE - BLOCK_SIZE;
            ghostDirectionX[ghostIndex] = 0;
            ghostDirectionY[ghostIndex] = -1;
        }

        if (nextGhostY < 0) {
            nextGhostY = 0;
            ghostDirectionY[ghostIndex] = 0;
            ghostDirectionX[ghostIndex] = 1;
        } else if (nextGhostY > SCREEN_SIZE - BLOCK_SIZE) {
            nextGhostY = SCREEN_SIZE - BLOCK_SIZE;
            ghostDirectionY[ghostIndex] = 0;
            ghostDirectionX[ghostIndex] = -1;
        }

        ghostX[ghostIndex] = nextGhostX;
        ghostY[ghostIndex] = nextGhostY;
        drawGhost(graphics, ghostX[ghostIndex] + 1,
                ghostY[ghostIndex] + MAZE_TOP_OFFSET + 1);

        if (isGhostCollidingWithPacman(ghostIndex)) {
            dying = true;
        }
    }

    // Choose a random path that is open and does not immediately reverse direction.
    private void chooseGhostDirection(int ghostIndex) {
        int mazeCellIndex = ghostX[ghostIndex] / BLOCK_SIZE
            + N_BLOCKS * (ghostY[ghostIndex] / BLOCK_SIZE);
        int openDirectionCount = 0;
        openDirectionCount = addGhostDirection(openDirectionCount, ghostIndex, mazeCellIndex, 1, -1, 0);
        openDirectionCount = addGhostDirection(openDirectionCount, ghostIndex, mazeCellIndex, 2, 0, -1);
        openDirectionCount = addGhostDirection(openDirectionCount, ghostIndex, mazeCellIndex, 4, 1, 0);
        openDirectionCount = addGhostDirection(openDirectionCount, ghostIndex, mazeCellIndex, 8, 0, 1);

        if (ghostX[ghostIndex] > 360 - BLOCK_SIZE) {
            ghostX[ghostIndex] = 0;
        }
        if (ghostX[ghostIndex] < 0) {
            ghostX[ghostIndex] = 360 - BLOCK_SIZE;
        }

        if (openDirectionCount == 0) {
            reverseOrStopGhost(ghostIndex, mazeCellIndex);
        } else {
            int selectedDirectionIndex = ghostDirectionRandom.nextInt(openDirectionCount);
            ghostDirectionX[ghostIndex] = possibleDirectionX[selectedDirectionIndex];
            ghostDirectionY[ghostIndex] = possibleDirectionY[selectedDirectionIndex];
        }
    }

    // Add a possible direction only when that side of the current cell has no wall.
    private int addGhostDirection(int openDirectionCount, int ghostIndex, int mazeCellIndex, int wallMask,
                                  int directionX, int directionY) {
        boolean canContinue;
        if (directionX == 0) {
            canContinue = ghostDirectionY[ghostIndex] != -directionY;
        } else {
            canContinue = ghostDirectionX[ghostIndex] != -directionX;
        }
        if ((screenData[mazeCellIndex] & wallMask) == 0 && canContinue) {
            possibleDirectionX[openDirectionCount] = directionX;
            possibleDirectionY[openDirectionCount] = directionY;
            return openDirectionCount + 1;
        }
        return openDirectionCount;
    }

    private void reverseOrStopGhost(int ghostIndex, int mazeCellIndex) {
        if ((screenData[mazeCellIndex] & 15) == 15) {
            ghostDirectionX[ghostIndex] = 0;
            ghostDirectionY[ghostIndex] = 0;
        } else {
            ghostDirectionX[ghostIndex] = -ghostDirectionX[ghostIndex];
            ghostDirectionY[ghostIndex] = -ghostDirectionY[ghostIndex];
        }
    }

    // Check whether a ghost is close enough to Pac-Man to cause a collision.
    private boolean isGhostCollidingWithPacman(int ghostIndex) {
        return pacmanX > ghostX[ghostIndex] - 12 && pacmanX < ghostX[ghostIndex] + 12
                && pacmanY > ghostY[ghostIndex] - 12 && pacmanY < ghostY[ghostIndex] + 12
                && inGame;
    }

    private void drawGhost(Graphics2D graphics, int ghostXPosition, int ghostYPosition) {
        graphics.drawImage(ghost, ghostXPosition, ghostYPosition, this);
    }

    // Move Pac-Man and check for a new direction whenever he reaches a cell.
    private void movePacman() {
        if (pacmanX % BLOCK_SIZE == 0 && pacmanY % BLOCK_SIZE == 0) {
            movePacmanAtIntersection();
        }
        pacmanX = pacmanX + pacmanSpeed * pacmanDirectionX;
        pacmanY = pacmanY + pacmanSpeed * pacmanDirectionY;
        keepPacmanInsideMaze();
    }

    // Eat a pellet, apply a queued turn, and stop if a wall blocks the path.
    private void movePacmanAtIntersection() {
        int mazeCellIndex = pacmanX / BLOCK_SIZE + N_BLOCKS * (pacmanY / BLOCK_SIZE);
        short cell = screenData[mazeCellIndex];

        if ((cell & 16) != 0) {
            screenData[mazeCellIndex] = (short) (cell & 15);
            score += level * ghostCount;
        }

        if ((requestedDirectionX != 0 || requestedDirectionY != 0) && !isWallBlocking(cell, requestedDirectionX, requestedDirectionY)) {
            pacmanDirectionX = requestedDirectionX;
            pacmanDirectionY = requestedDirectionY;
        }

        if (isWallBlocking(cell, pacmanDirectionX, pacmanDirectionY)) {
            pacmanDirectionX = 0;
            pacmanDirectionY = 0;
        }
    }

    // Check the wall bit for the side Pac-Man is trying to enter.
    private boolean isWallBlocking(short cell, int directionX, int directionY) {
        return (directionX == -1 && directionY == 0 && (cell & 1) != 0)
                || (directionX == 1 && directionY == 0 && (cell & 4) != 0)
                || (directionX == 0 && directionY == -1 && (cell & 2) != 0)
                || (directionX == 0 && directionY == 1 && (cell & 8) != 0);
    }

    // Keep Pac-Man inside the maze, except where an open edge connects across.
    private void keepPacmanInsideMaze() {
        keepPacmanOnHorizontalBounds();
        keepPacmanOnVerticalBounds();
    }

    private void keepPacmanOnHorizontalBounds() {
        int row = pacmanY / BLOCK_SIZE;
        if (row < 0) {
            row = 0;
        } else if (row >= N_BLOCKS) {
            row = N_BLOCKS - 1;
        }
        int leftEdge = screenData[row * N_BLOCKS];
        int rightEdge = screenData[row * N_BLOCKS + N_BLOCKS - 1];
        boolean horizontalPassage = (leftEdge & 1) == 0 && (rightEdge & 4) == 0;

        if (pacmanX >= SCREEN_SIZE) {
            if (horizontalPassage) {
                pacmanX = 0;
            } else {
                pacmanX = SCREEN_SIZE - BLOCK_SIZE;
                pacmanDirectionX = 0;
                requestedDirectionX = 0;
            }
        } else if (pacmanX < 0) {
            if (horizontalPassage) {
                pacmanX = SCREEN_SIZE - BLOCK_SIZE;
            } else {
                pacmanX = 0;
                pacmanDirectionX = 0;
                requestedDirectionX = 0;
            }
        }
    }

    private void keepPacmanOnVerticalBounds() {
        int column = pacmanX / BLOCK_SIZE;
        if (column < 0) {
            column = 0;
        } else if (column >= N_BLOCKS) {
            column = N_BLOCKS - 1;
        }
        int topEdge = screenData[column];
        int bottomEdge = screenData[(N_BLOCKS - 1) * N_BLOCKS + column];
        boolean verticalPassage = (topEdge & 2) == 0 && (bottomEdge & 8) == 0;

        if (pacmanY >= SCREEN_SIZE) {
            if (verticalPassage) {
                pacmanY = 0;
            } else {
                pacmanY = SCREEN_SIZE - BLOCK_SIZE;
                pacmanDirectionY = 0;
                requestedDirectionY = 0;
            }
        } else if (pacmanY < 0) {
            if (verticalPassage) {
                pacmanY = SCREEN_SIZE - BLOCK_SIZE;
            } else {
                pacmanY = 0;
                pacmanDirectionY = 0;
                requestedDirectionY = 0;
            }
        }
    }

    // Draw Pac-Man using the image that matches his current direction.
    private void drawPacman(Graphics2D graphics) {
        int directionX = pacmanDirectionX;
        int directionY = pacmanDirectionY;

        if (directionX == 0) {
            directionX = requestedDirectionX;
        }
        if (directionY == 0) {
            directionY = requestedDirectionY;
        }

        if (directionX == -1) {
            graphics.drawImage(left, pacmanX + 1, pacmanY + MAZE_TOP_OFFSET + 1, this);
        } else if (directionX == 1) {
            graphics.drawImage(right, pacmanX + 1, pacmanY + MAZE_TOP_OFFSET + 1, this);
        } else if (directionY == -1) {
            graphics.drawImage(up, pacmanX + 1, pacmanY + MAZE_TOP_OFFSET + 1, this);
        } else {
            graphics.drawImage(down, pacmanX + 1, pacmanY + MAZE_TOP_OFFSET + 1, this);
        }
    }

    // Draw each cell in the maze grid from left to right and top to bottom.
    private void drawMaze(Graphics2D graphics, short[] mazeLayout) {
        int mazeCellIndex = 0;
        for (int mazeY = 0; mazeY < SCREEN_SIZE; mazeY += BLOCK_SIZE) {
            for (int mazeX = 0; mazeX < SCREEN_SIZE; mazeX += BLOCK_SIZE) {
                drawMazeTile(graphics, mazeLayout, mazeCellIndex, mazeX, mazeY + MAZE_TOP_OFFSET);
                mazeCellIndex++;
            }
        }
    }

    private void drawMazeTile(Graphics2D graphics, short[] mazeData, int mazeCellIndex,
                              int mazeX, int mazeY) {
        graphics.setColor(LIGHT_PURPLE);
        if (mazeData[mazeCellIndex] == 0) {
            graphics.fillRect(mazeX, mazeY, BLOCK_SIZE, BLOCK_SIZE);
        }

        drawMazeWalls(graphics, screenData[mazeCellIndex], mazeX, mazeY);
        // Bit 16 means a pellet is still here; remove it and add its points once.
        if ((screenData[mazeCellIndex] & 16) != 0) {
            graphics.drawImage(strawberry, mazeX + 3, mazeY + 3, this);
        }
    }

    // The cell's first four bits tell which walls should be drawn.
    private void drawMazeWalls(Graphics2D graphics, short cell, int mazeX, int mazeY) {
        if ((cell & 1) != 0) {
            drawPixelWall(graphics, mazeX, mazeY, false);
        }
        if ((cell & 2) != 0) {
            drawPixelWall(graphics, mazeX, mazeY, true);
        }
        if ((cell & 4) != 0) {
            drawPixelWall(graphics, mazeX + BLOCK_SIZE - WALL_PIXEL_SIZE, mazeY, false);
        }
        if ((cell & 8) != 0) {
            drawPixelWall(graphics, mazeX, mazeY + BLOCK_SIZE - WALL_PIXEL_SIZE, true);
        }
    }

    // Draw a wall as small alternating blocks to give it a pixel style.
    private void drawPixelWall(Graphics2D graphics, int x, int y, boolean horizontal) {
        for (int offset = 0; offset < BLOCK_SIZE; offset += WALL_PIXEL_SIZE) {
            graphics.setColor((offset / WALL_PIXEL_SIZE) % 2 == 0
                    ? LIGHT_PURPLE : WALL_PIXEL_DARK);
            if (horizontal) {
                graphics.fillRect(x + offset, y, WALL_PIXEL_SIZE, WALL_PIXEL_SIZE);
            } else {
                graphics.fillRect(x, y + offset, WALL_PIXEL_SIZE, WALL_PIXEL_SIZE);
            }
            graphics.setColor(WALL_PIXEL_HIGHLIGHT);
            if (horizontal) {
                graphics.fillRect(x + offset, y, 2, 2);
            } else {
                graphics.fillRect(x, y + offset, 2, 2);
            }
        }
    }

    // Reset score, lives, level, and maze data for a new game.
    private void initGame() {
        lives = 8;
        score = 0;
        ghostCount = 1;
        level = 1;
        gameWon = false;
        gameStartedAtNanos = 0;
        gameCompletedAtNanos = 0;
        initLevel();
    }

    public void resetGame(Graphics2D graphics)
    {
        String gameOver = "GAME OVER";
        String finalScore = "Score = " + score;
        String playAgain = "press SPACE to play again";

        graphics.setColor(Color.red);
        graphics.setFont(smallFont);
        drawCenteredText(graphics, gameOver, MAZE_TOP_OFFSET + SCREEN_SIZE / 2 - 18);
        drawCenteredText(graphics, finalScore, MAZE_TOP_OFFSET + SCREEN_SIZE / 2 + 4);
        drawCenteredText(graphics, playAgain, MAZE_TOP_OFFSET + SCREEN_SIZE / 2 + 26);
    }

    private void drawWinScreen(Graphics2D graphics) {
        graphics.setColor(new Color(0, 0, 0, 210));
        graphics.fillRect(0, MAZE_TOP_OFFSET, SCREEN_SIZE, SCREEN_SIZE);

        graphics.setColor(Color.YELLOW);
        graphics.setFont(new Font(Font.MONOSPACED, Font.BOLD, 25));
        drawCenteredText(graphics, "CONGRATULATIONS!", MAZE_TOP_OFFSET + 135);

        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font(Font.MONOSPACED, Font.BOLD, 20));
        drawCenteredText(graphics, "YOU WON!", MAZE_TOP_OFFSET + 170);
        drawCenteredText(graphics, "Score: " + score, MAZE_TOP_OFFSET + 210);

        drawCenteredText(graphics, "Time: " + getElapsedTime(), MAZE_TOP_OFFSET + 240);

        graphics.setFont(smallFont);
        drawCenteredText(graphics, "Press SPACE to play again", MAZE_TOP_OFFSET + 280);
    }

    private void drawCenteredText(Graphics2D graphics, String text, int textBaselineY) {
        FontMetrics metrics = graphics.getFontMetrics();
        int textX = (SCREEN_SIZE - metrics.stringWidth(text)) / 2;
        graphics.drawString(text, textX, textBaselineY);
    }

    // Load the first maze and reduce its pellet count before resetting positions.
    private void initLevel() {
        System.arraycopy(MazeLayouts.getLayout(1), 0, screenData, 0, N_BLOCKS * N_BLOCKS);
        keepEveryNthPellet(4);
        continueLevel();
    }
    // Select the maze layout that matches the requested level.
    private void initLevel(int level) {
        ghostCount = level;
        if(level == 1)
            initLevel();
        if(level == 2)
            initLevel2();
        if(level == 3)
            initLevel3();
        if(level == 4)
            initLevel4();
        if(level == 5)
            initLevel5();

        continueLevel();
    }

    private void initLevel2()
    {
        level = 2;
        System.arraycopy(MazeLayouts.getLayout(2), 0, screenData, 0, N_BLOCKS * N_BLOCKS);
        keepEveryNthPellet(3);
        continueLevel();
    }

    // Remove some pellets while keeping one out of every selected number.
    private void keepEveryNthPellet(int interval) {
        int pelletIndex = 0;
        for (int mazeCellIndex = 0; mazeCellIndex < screenData.length; mazeCellIndex++) {
            if ((screenData[mazeCellIndex] & 16) != 0) {
                if (pelletIndex % interval != 0) {
                    screenData[mazeCellIndex] = (short) (screenData[mazeCellIndex] & 15);
                }
                pelletIndex++;
            }
        }
    }

    private void initLevel3()
    {
        level = 3;
        System.arraycopy(MazeLayouts.getLayout(3), 0, screenData, 0, N_BLOCKS * N_BLOCKS);
        keepEveryNthPellet(3);
        continueLevel();
    }
    private void initLevel4()
    {
        level = 4;
        System.arraycopy(MazeLayouts.getLayout(4), 0, screenData, 0, N_BLOCKS * N_BLOCKS);
        keepEveryNthPellet(2);
        continueLevel();
    }
    private void initLevel5()
    {
        level = 5;
        System.arraycopy(MazeLayouts.getLayout(5), 0, screenData, 0, N_BLOCKS * N_BLOCKS);
        keepEveryNthPellet(2);
        continueLevel();
    }

    // Set character speeds and place everyone at their starting positions.
    private void continueLevel() {
        int ghostDirection = 1;
        int speedIndex = level - 1;
        if (speedIndex >= validSpeeds.length) {
            speedIndex = validSpeeds.length - 1;
        }
        int levelSpeed = validSpeeds[speedIndex];
        pacmanSpeed = levelSpeed + 1;
        while (BLOCK_SIZE % pacmanSpeed != 0) {
            pacmanSpeed++;
        }

        for (int ghostIndex = 0; ghostIndex < ghostCount; ghostIndex++) {
            ghostY[ghostIndex] = 4 * BLOCK_SIZE;
            ghostX[ghostIndex] = 4 * BLOCK_SIZE;
            ghostDirectionY[ghostIndex] = 0;
            ghostDirectionX[ghostIndex] = ghostDirection;
            ghostDirection = -ghostDirection;
            ghostSpeed[ghostIndex] = levelSpeed;
        }
        pacmanX = 7 * BLOCK_SIZE;
        pacmanY = 11 * BLOCK_SIZE;
        pacmanDirectionX = 0;
        pacmanDirectionY = 0;
        requestedDirectionX = 0;
        requestedDirectionY = 0;
        dying = false;
    }

    public void subtractGhost()
    {
        if(ghostCount > 0)
            ghostCount--;
    }
    public void addGhost()
    {
        if(ghostCount < MAX_GHOSTS)
            ghostCount++;

        continueLevel();
    }

    @Override
    // Swing calls this to redraw the panel whenever it needs a fresh frame.
    public void paintComponent(Graphics componentGraphics) {
        super.paintComponent(componentGraphics);

        Graphics2D graphics = (Graphics2D) componentGraphics.create();
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        double widthScale = (double) getWidth() / panelSize.width;
        double heightScale = (double) getHeight() / panelSize.height;
        double scale = widthScale;
        if (heightScale < scale) {
            scale = heightScale;
        }
        int scaledWidth = (int) (panelSize.width * scale);
        int scaledHeight = (int) (panelSize.height * scale);
        graphics.translate((getWidth() - scaledWidth) / 2, (getHeight() - scaledHeight) / 2);
        graphics.scale(scale, scale);

        graphics.setColor(BACKGROUND_COLOR);
        graphics.fillRect(0, 0, panelSize.width, panelSize.height);

        drawMaze(graphics, MazeLayouts.getLayout(level));

        if (showLevelBanner) {
            drawLevelBanner(graphics);
        }

        drawScore(graphics);
        if (inGame) {
            playGame(graphics);
        }
        else if (gameWon) {
            drawWinScreen(graphics);
        }
        else if(lives == 0)
            resetGame(graphics);

        else
        {
            showIntroScreen(graphics);
        }
        Toolkit.getDefaultToolkit().sync();
        graphics.dispose();

        Graphics2D frameGraphics = (Graphics2D) componentGraphics.create();
        frameGraphics.setColor(LIGHT_PURPLE);
        frameGraphics.setStroke(new BasicStroke(5));
        frameGraphics.drawRect(2, 2, getWidth() - 5, getHeight() - 5);
        frameGraphics.dispose();
    }

    // Handle game keys directly: arrows move, Escape pauses, and Space starts.
    class TAdapter extends KeyAdapter {
        private void nextLevel() {
            if (level >= levelDifficulties.length) {
                return;
            }
            level++;
            initLevel(level);
        }

        @Override
        public void keyPressed(KeyEvent e) {
            int keyCode = e.getKeyCode();

            if (keyCode == KeyEvent.VK_N)
            {
                nextLevel();
            }
            if (keyCode == KeyEvent.VK_S)
            {
                subtractGhost();
            }
            if (keyCode == KeyEvent.VK_A)
            {
                addGhost();
            }
            if (inGame) {
                if (keyCode == KeyEvent.VK_LEFT) {
                    requestedDirectionX = -1;
                    requestedDirectionY = 0;
                } else if (keyCode == KeyEvent.VK_RIGHT) {
                    requestedDirectionX = 1;
                    requestedDirectionY = 0;
                } else if (keyCode == KeyEvent.VK_UP) {
                    requestedDirectionX = 0;
                    requestedDirectionY = -1;
                } else if (keyCode == KeyEvent.VK_DOWN) {
                    requestedDirectionX = 0;
                    requestedDirectionY = 1;
                } else if (keyCode == KeyEvent.VK_ESCAPE && timer.isRunning()) {
                    inGame = false;
                }
            } else {
                if (keyCode == KeyEvent.VK_SPACE) {
                    inGame = true;
                    initGame();
                    gameStartedAtNanos = System.nanoTime();
                    startLevelBanner();
                    soundManager.stopEndSounds();
                    soundManager.startGameMusic();
                }
            }
        }
    }

    @Override
    // The timer calls this repeatedly to update banners and request a redraw.
    public void actionPerformed(ActionEvent e) {
        if (showLevelBanner) {
            levelBannerTimer -= 40;
            if (levelBannerTimer <= 0) {
                showLevelBanner = false;
                levelBannerTimer = 0;
            }
        }
        repaint();
    }

    public boolean isInGame() {
        return inGame;
    }

    // Save the next direction so Pac-Man can turn at the next open cell.
    public void requestDirection(int directionX, int directionY) {
        requestedDirectionX = directionX;
        requestedDirectionY = directionY;
    }

    public boolean isTimerRunning() {
        return timer.isRunning();
    }

    // Stop active play without resetting the current level.
    public void pauseGame() {
        inGame = false;
    }

    // Start a new game and begin its timer, level banner, and background music.
    public void startGame() {
        inGame = true;
        initGame();
        gameStartedAtNanos = System.nanoTime();
        startLevelBanner();
        soundManager.stopEndSounds();
        soundManager.startGameMusic();
    }
}
