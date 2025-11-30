public class BinairoMove extends Move {
    public int row;    // Ligne
    public int col;    // Colonne
    public int value;  // 0a ou 1

    public BinairoMove(int row, int col, int value) {
        this.row = row;
        this.col = col;
        this.value = value;
    }

    @Override
    public String toString() {
        return "Move: (" + row + ", " + col + ") -> " + value;
    }
}