package Algorithms;
import classes.point;

import java.util.ArrayList;

public class KnightsTour {
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


        // print the board
        System.out.println("\nBoard:");
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                System.out.printf("%3d", board[row][col]);
            }
            System.out.println();
        }

        kt.printPath(board, size);

    }

    public static point[] knightsTourPath(int startRow, int startCol, int size) {
        int[][] board = knightsTourWarnsdroff(startRow, startCol, size);

        return extractPath(board, size);
    }

    public static int[][] knightsTourBacktracking(int rowStart, int colStart, int size) {
        int[][] board = new int[size][size];
        board[rowStart][colStart] = 1;

        knightsTourBacktrackingRecursion(board, rowStart, colStart, 2, size);


        return board;
    }

    private static boolean knightsTourBacktrackingRecursion(int[][] board, int row, int col, int move, int size) {
        if (move == size * size + 1) return true;

        for (int i = 0; i < 8; i++) {
            int nextRow = row + rowMoves[i];
            int nextCol = col + colMoves[i];

            if (isValid(nextRow, nextCol, board, size)) {
                board[nextRow][nextCol] = move;

                if (knightsTourBacktrackingRecursion(board, nextRow, nextCol, move + 1, size)) {
                    return true;
                }

                board[nextRow][nextCol] = 0;
            }
        }

        return false;
    }


    public static int[][] knightsTourWarnsdroff(int rowStart, int colStart, int size) {
        int[][] board = new int[size][size];


        board[rowStart][colStart] = 1;

        //initilaize board with degrees of entry for each square
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                // skip start
                if (!(row == rowStart && col == colStart)) {

                    int counter = 0;

                    for (int i = 0; i < 8; i++) {
                        int nextRow = row + rowMoves[i];
                        int nextCol = col + colMoves[i];
                        if (isValid(nextRow, nextCol, board, size)) {
                            counter++;
                        }
                    }
                    //minus counter, degrees of entry are in negetive so they are not confused with moves
                    board[row][col] = -counter;
                }
            }
        }

        knightsTourWarnsdorffRecursion(board, rowStart, colStart, 2, size);


        return board;
    }

    private static boolean knightsTourWarnsdorffRecursion(int[][] board, int row, int col, int move, int size) {
        if (move == size * size + 1) return true;

        int[] sortedMoves = getSortedMoves(board, row, col, size);
        for (int i = 0; i < 8; i++) {
            // no valid moves backtrack
            if (sortedMoves[i] == -1) return false;

            int nextRow = row + rowMoves[sortedMoves[i]];
            int nextCol = col + colMoves[sortedMoves[i]];

            board[nextRow][nextCol] = move;
            updateNeighbors(board, nextRow, nextCol, size, 1);

            if (knightsTourWarnsdorffRecursion(board, nextRow, nextCol, move + 1, size)) {
                return true;
            }
            // backtrack
            updateNeighbors(board, nextRow, nextCol, size, -1);
            board[nextRow][nextCol] = -countOnwardMoves(board, nextRow, nextCol, size);

        }
        return false;
    }

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

    //O(1)
    private static int[] getSortedMoves(int[][] board, int row, int col, int size) {
        int[] sortedMoves = new int[8];
        int[] moveCounts = new int[8];
        int validCount = 0;

        // collect all valid moves and their counts
        for (int i = 0; i < 8; i++) {
            int nextRow = row + rowMoves[i];
            int nextCol = col + colMoves[i];
            if (isValid(nextRow, nextCol, board, size)) {
                sortedMoves[validCount] = i;
                moveCounts[validCount] = countOnwardMoves(board, nextRow, nextCol, size);
                validCount++;
            }
        }

        // insertion sort by move count
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

        // fill remaining with -1
        for (int i = validCount; i < 8; i++) {
            sortedMoves[i] = -1;
        }

        return sortedMoves;
    }

    private static void updateNeighbors(int[][] board ,int row ,int col, int size, int amount){
        for  (int i = 0; i < 8; i++) {
            int nextRow = row + rowMoves[i];
            int nextCol = col + colMoves[i];
            if(isValid(nextRow, nextCol, board, size)){
                board[nextRow][nextCol] += amount;
            }
        }
    }


    private static boolean isValid(int row, int col, int[][] board, int size) {
        return row >= 0 && row < size
                && col >= 0 && col < size
                && board[row][col] <= 0;
    }


    public static point[] extractPath(int[][] board, int size) {
        point[] path = new point[size * size];
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                path[board[row][col] - 1] = new point(row, col);
            }
        }
        return path;
    }

    private static void printPath(int[][] board, int size){
        point[] path = extractPath(board, size);
        for (int i = 0; i < path.length; i++) {
            System.out.println(path[i].col + " " + path[i].row);
        }
    }

}
