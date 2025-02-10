import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Random;

public class Game extends Frame implements MouseListener {
    private static final int SIZE = 4, TILE_SIZE = 120, SWIPE_THRESHOLD = 30;
    private static final Color BG_COLOR = new Color(0xBBADA0);
    private static final Font SCORE_FONT = new Font("Arial", Font.BOLD, 18);
    private static final Font TILE_FONT = new Font("Arial", Font.BOLD, 32);

    private int[][] board = new int[SIZE][SIZE];
    private int score = 0, highScore = 0;
    private boolean gameOver = false;
    private int startX, startY;
    private static final String HIGH_SCORE_FILE = "highscore.txt";

    enum Direction { LEFT, UP, RIGHT, DOWN }

    public Game() {
        super("2048 Game");
        setSize(SIZE * TILE_SIZE, SIZE * TILE_SIZE + 80);
        setResizable(false);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        addMouseListener(this);
        highScore = loadHighScore();
        initializeGame();
    }

    private void initializeGame() {
        board = new int[SIZE][SIZE];
        score = 0;
        gameOver = false;
        addRandomTile();
        addRandomTile();
    }

    private void addRandomTile() {
        ArrayList<Point> emptyCells = new ArrayList<>();
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (board[i][j] == 0) {
                    emptyCells.add(new Point(j, i));
                }
            }
        }
        if (!emptyCells.isEmpty()) {
            Point p = emptyCells.get(new Random().nextInt(emptyCells.size()));
            board[p.y][p.x] = (Math.random() < 0.9) ? 2 : 4;
        }
    }

    private boolean move(Direction direction) {
        int rotations = switch (direction) {
            case LEFT -> 0;
            case UP -> 3;
            case RIGHT -> 2;
            case DOWN -> 1;
        };

        rotateBoard(rotations);
        boolean moved = moveLeft();
        rotateBoard((4 - rotations) % 4);

        if (moved) {
            addRandomTile();
            checkGameOver();
            repaint();
        }
        return moved;
    }

    private void rotateBoard(int times) {
        while (times-- > 0) {
            int[][] newBoard = new int[SIZE][SIZE];
            for (int i = 0; i < SIZE; i++) {
                for (int j = 0; j < SIZE; j++) {
                    newBoard[j][SIZE - 1 - i] = board[i][j];
                }
            }
            board = newBoard;
        }
    }

    private boolean moveLeft() {
        boolean moved = false;
        for (int i = 0; i < SIZE; i++) {
            int[] row = board[i];
            int[] newRow = new int[SIZE];
            int pos = 0;
            boolean merged = false;
            for (int j = 0; j < SIZE; j++) {
                if (row[j] == 0)
                    continue;
                if (pos > 0 && newRow[pos - 1] == row[j] && !merged) {
                    newRow[pos - 1] *= 2;
                    score += newRow[pos - 1];
                    merged = true;
                    moved = true;
                } else {
                    newRow[pos++] = row[j];
                    merged = false;
                }
            }
            if (!moved) {
                moved = !java.util.Arrays.equals(board[i], newRow);
            }
            board[i] = newRow;
        }
        return moved;
    }

    private void checkGameOver() {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (board[i][j] == 0 ||
                    (j < SIZE - 1 && board[i][j] == board[i][j + 1]) ||
                    (i < SIZE - 1 && board[i][j] == board[i + 1][j])) {
                    return;
                }
            }
        }
        gameOver = true;
        updateHighScore();
    }

    private void updateHighScore() {
        if (score > highScore) {
            highScore = score;
            saveHighScore(highScore);
        }
    }

    private void saveHighScore(int highScore) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(HIGH_SCORE_FILE))) {
            writer.write(String.valueOf(highScore));
        } catch (IOException e) {
            System.err.println("Error saving high score: " + e.getMessage());
        }
    }

    private int loadHighScore() {
        File file = new File(HIGH_SCORE_FILE);
        if (!file.exists())
            return 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            return Integer.parseInt(reader.readLine());
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error loading high score: " + e.getMessage());
            return 0;
        }
    }

    private Color getTileColor(int value) {
        return switch (value) {
            case 2 -> new Color(0xEEE4DA);
            case 4 -> new Color(0xEDE0C8);
            case 8 -> new Color(0xF2B179);
            case 16 -> new Color(0xF59563);
            case 32 -> new Color(0xF67C5F);
            case 64 -> new Color(0xF65E3B);
            case 128 -> new Color(0xEDCF72);
            case 256 -> new Color(0xEDCC61);
            case 512 -> new Color(0xEDC850);
            case 1024 -> new Color(0xEDC53F);
            case 2048 -> new Color(0xEDC22E);
            default -> new Color(0xCDC1B4);
        };
    }

    public void paint(Graphics g) {
        // Draw background
        g.setColor(BG_COLOR);
        g.fillRect(0, 0, getWidth(), getHeight());

        // Draw score and high score with proper spacing
        g.setFont(SCORE_FONT);
        g.setColor(Color.WHITE);
        String scoreText = "Score: " + score;
        String highScoreText = "High Score: " + highScore;
        int padding = 20;
        int scoreX = padding;
        int highScoreX = getWidth() - g.getFontMetrics().stringWidth(highScoreText) - padding;
        int textY = 60; // Vertical position for the texts

        g.drawString(scoreText, scoreX, textY);
        g.drawString(highScoreText, highScoreX, textY);

        // Draw the game board tiles
        int offsetY = 70;
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                int value = board[i][j];
                Color tileColor = getTileColor(value);
                int x = j * TILE_SIZE + 10;
                int y = i * TILE_SIZE + offsetY + 10;
                g.setColor(tileColor);
                g.fillRoundRect(x, y, TILE_SIZE - 20, TILE_SIZE - 20, 15, 15);

                if (value != 0) {
                    g.setColor(value < 8 ? new Color(0x776E65) : Color.WHITE);
                    g.setFont(TILE_FONT);
                    FontMetrics fm = g.getFontMetrics();
                    String text = String.valueOf(value);
                    g.drawString(text, x + (TILE_SIZE - 20 - fm.stringWidth(text)) / 2,
                        y + (TILE_SIZE - 20 + fm.getAscent()) / 2);
                }
            }
        }

        // Draw game over overlay if the game is over
        if (gameOver) {
            g.setColor(new Color(255, 255, 255, 150));
            g.fillRect(0, offsetY, getWidth(), getHeight() - offsetY);
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 48));
            String msg = "Game Over!";
            g.drawString(msg, (getWidth() - g.getFontMetrics().stringWidth(msg)) / 2, getHeight() / 2);
        }
    }

    public void mousePressed(MouseEvent e) {
        startX = e.getX();
        startY = e.getY();
    }

    public void mouseReleased(MouseEvent e) {
        int deltaX = e.getX() - startX;
        int deltaY = e.getY() - startY;
        if (Math.abs(deltaX) < SWIPE_THRESHOLD && Math.abs(deltaY) < SWIPE_THRESHOLD)
            return;
        Direction direction = Math.abs(deltaX) > Math.abs(deltaY)
                ? (deltaX > 0 ? Direction.RIGHT : Direction.LEFT)
                : (deltaY > 0 ? Direction.DOWN : Direction.UP);
        if (!gameOver)
            move(direction);
    }

    public void mouseClicked(MouseEvent e) { }
    public void mouseEntered(MouseEvent e) { }
    public void mouseExited(MouseEvent e) { }

    public static void main(String[] args) {
        new Game().setVisible(true);
    }
}