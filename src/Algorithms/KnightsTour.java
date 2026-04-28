package Algorithms;

/**
 * Solves the Knight's Tour problem - find a sequence of knight moves
 * that visits every square on an NxN board exactly once.
 *
 * Provides two algorithms:
 *   - Backtracking: simple but slow on large boards (tries everything).
 *   - Warnsdorff's heuristic: at each step, prefer the move that leaves
 *     the fewest onward moves. Almost always finds a tour in linear time.
 */
public class KnightsTour {

    // the 8 possible knight moves as (row delta, col delta) pairs
    private static final int[] rowMoves = {-2, -2, -1, -1,  1,  1,  2,  2};
    private static final int[] colMoves = { -1,  1, -2,  2, -2,  2, -1,  1};

    public static void main(String[] args) {
        KnightsTour kt = new KnightsTour();
        int size = 8;
        int startRow = 0;
        int startCol = 0;

        System.out.println("Solving knight's tour on " + size + "x" + size + " board...");
        System.out.println("Starting at (" + startRow + ", " + startCol + ")");

        int[][] board = kt.knightsTourWarnsdroff(startRow, startCol, size);

        // print the board - each cell shows the move number when the knight visited it
        System.out.println("\nBoard:");
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                System.out.printf("%3d", board[row][col]);
            }
            System.out.println();
        }

        kt.printPath(board, size);
    }

    /** Returns the tour as an ordered array of points (path[0] is the start). */
    public static Point[] knightsTourPath(int startRow, int startCol, int size) {
        int[][] board = knightsTourWarnsdroff(startRow, startCol, size);
        return extractPath(board, size);
    }

    // backtracking - the slow but straightforward approach

    /** Solves the tour by pure backtracking. Works but is impractically slow for size > 6. */
    public static int[][] knightsTourBacktracking(int rowStart, int colStart, int size) {
        int[][] board = new int[size][size];
        board[rowStart][colStart] = 1;

        knightsTourBacktrackingRecursion(board, rowStart, colStart, 2, size);

        return board;
    }

    private static boolean knightsTourBacktrackingRecursion(int[][] board, int row, int col, int move, int size) {
        // base case - filled every square
        if (move == size * size + 1) return true;

        for (int i = 0; i < 8; i++) {
            int nextRow = row + rowMoves[i];
            int nextCol = col + colMoves[i];

            if (isValid(nextRow, nextCol, board, size)) {
                board[nextRow][nextCol] = move;

                if (knightsTourBacktrackingRecursion(board, nextRow, nextCol, move + 1, size)) {
                    return true;
                }

                // dead end - undo and try the next move
                board[nextRow][nextCol] = 0;
            }
        }

        return false;
    }

    // Warnsdorff's heuristic - much faster than plain backtracking

    /**
     * Solves the tour using Warnsdorff's rule: always move to the square that
     * has the fewest onward moves. This greedily avoids painting yourself into
     * a corner, and finds a tour almost instantly even on large boards.
     */
    public static int[][] knightsTourWarnsdroff(int rowStart, int colStart, int size) {
        int[][] board = new int[size][size];

        board[rowStart][colStart] = 1;

        // Pre-compute the degree (number of incoming knight moves) for every square.
        // Stored as negative so the values don't get confused with actual move numbers
        // once the algorithm starts placing them. The cells with the most negative
        // value are corners and edges - the hardest squares to enter.
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                if (!(row == rowStart && col == colStart)) {

                    int counter = 0;

                    for (int i = 0; i < 8; i++) {
                        int nextRow = row + rowMoves[i];
                        int nextCol = col + colMoves[i];
                        if (isValid(nextRow, nextCol, board, size)) {
                            counter++;
                        }
                    }
                    // store as negative to distinguish from move numbers
                    board[row][col] = -counter;
                }
            }
        }

        knightsTourWarnsdorffRecursion(board, rowStart, colStart, 2, size);

        return board;
    }

    private static boolean knightsTourWarnsdorffRecursion(int[][] board, int row, int col, int move, int size) {
        if (move == size * size + 1) return true;

        // sort the 8 possible moves by how few onward moves each one would leave -
        // this is Warnsdorff's heuristic in action
        int[] sortedMoves = getSortedMoves(board, row, col, size);
        for (int i = 0; i < 8; i++) {
            // -1 marks the end of valid moves - if we hit one, no path forward exists here
            if (sortedMoves[i] == -1) return false;

            int nextRow = row + rowMoves[sortedMoves[i]];
            int nextCol = col + colMoves[sortedMoves[i]];

            board[nextRow][nextCol] = move;
            // moving onto this square reduces every neighbor's onward-move count by 1
            updateNeighbors(board, nextRow, nextCol, size, 1);

            if (knightsTourWarnsdorffRecursion(board, nextRow, nextCol, move + 1, size)) {
                return true;
            }
            // backtrack - undo neighbor counts and restore the cell's original degree
            updateNeighbors(board, nextRow, nextCol, size, -1);
            board[nextRow][nextCol] = -countOnwardMoves(board, nextRow, nextCol, size);
        }
        return false;
    }

    /** Counts how many of the 8 knight moves from (row,col) land on an unvisited square. */
    //O(1)
    private static int countOnwardMoves(int[][] board, int row, int col, int size) {
        int count = 0;
        for (int i = 0; i < 8; i++) {
            int nextRow = row + rowMoves[i];
            int nextCol = col + colMoves[i];
            if (isValid(nextRow, nextCol, board, size)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns the 8 move indices sorted by onward-move count (lowest first).
     * Invalid moves are placed at the end, marked with -1.
     */
    //O(1)
    private static int[] getSortedMoves(int[][] board, int row, int col, int size) {
        int[] sortedMoves = new int[8];
        int[] moveCounts = new int[8];
        int validCount = 0;

        // collect all valid moves and their onward-move counts
        for (int i = 0; i < 8; i++) {
            int nextRow = row + rowMoves[i];
            int nextCol = col + colMoves[i];
            if (isValid(nextRow, nextCol, board, size)) {
                sortedMoves[validCount] = i;
                moveCounts[validCount] = countOnwardMoves(board, nextRow, nextCol, size);
                validCount++;
            }
        }

        // insertion sort by move count - small array (max 8) so insertion sort is fine
        // O(8)
        for (int i = 1; i < validCount; i++) {
            int keyMove = sortedMoves[i];
            int keyCount = moveCounts[i];
            int j = i - 1;
            while (j >= 0 && moveCounts[j] > keyCount) {
                sortedMoves[j + 1] = sortedMoves[j];
                moveCounts[j + 1] = moveCounts[j];
                j--;
            }
            sortedMoves[j + 1] = keyMove;
            moveCounts[j + 1] = keyCount;
        }

        // mark unused slots so the caller knows where the valid moves end
        for (int i = validCount; i < 8; i++) {
            sortedMoves[i] = -1;
        }

        return sortedMoves;
    }

    /** Adjusts the cached degree of every neighbor by `amount` (+1 or -1). */
    private static void updateNeighbors(int[][] board ,int row ,int col, int size, int amount){
        for  (int i = 0; i < 8; i++) {
            int nextRow = row + rowMoves[i];
            int nextCol = col + colMoves[i];
            if(isValid(nextRow, nextCol, board, size)){
                board[nextRow][nextCol] += amount;
            }
        }
    }

    /**
     * A square is valid to move into if it's inside the board and hasn't been
     * visited yet. board[row][col] <= 0 means either an untouched cell (0) or
     * a degree-cached cell (negative). Positive values mean already visited.
     */
    private static boolean isValid(int row, int col, int[][] board, int size) {
        return row >= 0 && row < size
                && col >= 0 && col < size
                && board[row][col] <= 0;
    }

    /** Converts the board (where each cell holds its move number) into an ordered path. */
    public static Point[] extractPath(int[][] board, int size) {
        Point[] path = new Point[size * size];
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                path[board[row][col] - 1] = new Point(row, col);
            }
        }
        return path;
    }

    private static void printPath(int[][] board, int size){
        Point[] path = extractPath(board, size);
        for (int i = 0; i < path.length; i++) {
            System.out.println(path[i].col + " " + path[i].row);
        }
    }
}