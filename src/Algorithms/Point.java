package Algorithms;

import Encryption.Hash;

/**
 * A simple (row, col) coordinate on a 2D grid.
 * Used by the Knight's Tour and the encryption layer to address cells.
 */
public class Point {
    public int row;
    public int col;

    public Point(int row, int col) {
        this.row = row;
        this.col = col;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Point)) return false;
        Point p = (Point) o;
        return this.col == p.col && this.row == p.row;
    }

    /** Uses the project's custom Hash for consistency with the rest of the codebase. */
    @Override
    public int hashCode() {
        return Hash.hash(col, row);
    }
}