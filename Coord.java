public class Coord {
    int r, c;
    Coord(int r, int c) { this.r = r; this.c = c; }
    // Pour comparaison
    public boolean equals(Coord o) { return this.r == o.r && this.c == o.c; }
}