package p;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

// Creates the small images used for the characters, lives, and collectibles.
final class GameAssets {
    final BufferedImage heart;
    final BufferedImage ghost;
    final BufferedImage strawberry;
    final BufferedImage up;
    final BufferedImage down;
    final BufferedImage left;
    final BufferedImage right;

    // Build the four Pac-Man images so his mouth points in each direction.
    GameAssets() {
        down = createDirectionalImage(Color.YELLOW, Color.ORANGE, 90);
        up = createDirectionalImage(Color.YELLOW, Color.ORANGE, -90);
        left = createDirectionalImage(Color.YELLOW, Color.ORANGE, 180);
        right = createDirectionalImage(Color.YELLOW, Color.ORANGE, 0);
        ghost = createGhostImage();
        heart = createHeartImage();
        strawberry = createStrawberryImage();
    }

    // Draw one Pac-Man image, then rotate it to face the chosen direction.
    private BufferedImage createDirectionalImage(Color body, Color mouth, int rotation) {
        int width = 20;
        int height = 20;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.rotate(Math.toRadians(rotation), width / 2.0, height / 2.0);
        graphics.setColor(body);
        graphics.fillArc(0, 0, width, height, 30, 300);
        graphics.setColor(mouth);
        graphics.fillArc(0, 0, width, height, 30, 90);
        graphics.dispose();
        return image;
    }

    // Draw a simple ghost image using basic shapes.
    private BufferedImage createGhostImage() {
        BufferedImage image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(255, 100, 100));
        graphics.fillRoundRect(2, 2, 16, 14, 8, 8);
        graphics.fillRect(2, 10, 16, 6);
        graphics.setColor(Color.WHITE);
        graphics.fillOval(5, 5, 4, 4);
        graphics.fillOval(11, 5, 4, 4);
        graphics.dispose();
        return image;
    }

    // Draw the heart used to represent one remaining life.
    private BufferedImage createHeartImage() {
        BufferedImage image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.RED);
        graphics.fillOval(2, 2, 8, 8);
        graphics.fillOval(10, 2, 8, 8);
        int[] xPoints = {2, 10, 18, 10, 2};
        int[] yPoints = {6, 2, 6, 18, 18};
        graphics.fillPolygon(xPoints, yPoints, xPoints.length);
        graphics.dispose();
        return image;
    }

    // Draw the strawberry image used for a maze collectible.
    private BufferedImage createStrawberryImage() {
        BufferedImage image = new BufferedImage(18, 18, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(220, 25, 45));
        int[] bodyX = {4, 13, 16, 15, 12, 9, 6, 3, 2};
        int[] bodyY = {4, 4, 7, 11, 14, 17, 14, 11, 7};
        graphics.fillPolygon(bodyX, bodyY, bodyX.length);
        graphics.setColor(new Color(40, 170, 55));
        int[] leafX = {8, 3, 6, 5, 9, 11, 13, 14, 10};
        int[] leafY = {5, 2, 2, 0, 3, 0, 2, 2, 5};
        graphics.fillPolygon(leafX, leafY, leafX.length);
        graphics.setColor(new Color(255, 230, 120));
        graphics.fillRect(6, 7, 2, 2);
        graphics.fillRect(11, 8, 2, 2);
        graphics.fillRect(8, 11, 2, 2);
        graphics.fillRect(5, 10, 2, 2);
        graphics.dispose();
        return image;
    }
}
