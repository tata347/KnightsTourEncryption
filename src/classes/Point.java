package classes;

import Encryption.Hash;

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

    @Override
    public int hashCode() {
        return Hash.hash(col, row);
    }

}
