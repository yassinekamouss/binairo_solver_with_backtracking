public class BinairoPosition extends Position {
    
    // Constantes pour lisibilité
    public static final int EMPTY = -1; // cellule vide car zéro est une valeur valide
    public static final int ZERO = 0;
    public static final int ONE = 1;

    public int n;          // Taille de la grille (ex: 6, 8, 10)
    public int[][] board;  // La matrice de jeu

    // Domaines représentés via un masque de bits par cellule:
    // 1 = valeur 0 autorisée, 2 = valeur 1 autorisée, 3 = {0,1}
    public short[][] domainMask;

    // Compteurs parité pour accélérer les vérifications
    public int[] rowZeroCount;
    public int[] rowOneCount;
    public int[] colZeroCount;
    public int[] colOneCount;

    public BinairoPosition(int n) {
        this.n = n;
        this.board = new int[n][n];
        this.domainMask = new short[n][n];
        this.rowZeroCount = new int[n];
        this.rowOneCount = new int[n];
        this.colZeroCount = new int[n];
        this.colOneCount = new int[n];

        // Initialisation : Tout est vide (-1) et Domaines complets {0, 1}
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                board[i][j] = EMPTY;
                domainMask[i][j] = 0b11; // {0,1}
            }
        }
    }

    // Essentiel pour la récursion : on doit dupliquer l'état sans lier les références
    public BinairoPosition(BinairoPosition other) {
        this.n = other.n;
        this.board = new int[n][n];
        this.domainMask = new short[n][n];
        this.rowZeroCount = new int[n];
        this.rowOneCount = new int[n];
        this.colZeroCount = new int[n];
        this.colOneCount = new int[n];

        // 1. Copier la grille
        for (int i = 0; i < n; i++) {
            System.arraycopy(other.board[i], 0, this.board[i], 0, n);
            System.arraycopy(other.domainMask[i], 0, this.domainMask[i], 0, n);
        }

        // 2. Copier les compteurs
        System.arraycopy(other.rowZeroCount, 0, this.rowZeroCount, 0, n);
        System.arraycopy(other.rowOneCount, 0, this.rowOneCount, 0, n);
        System.arraycopy(other.colZeroCount, 0, this.colZeroCount, 0, n);
        System.arraycopy(other.colOneCount, 0, this.colOneCount, 0, n);
    }

    // Utilitaires domaine
    public int getDomainSize(int r, int c) {
        short m = domainMask[r][c];
        if (m == 0) return 0;
        if (m == 3) return 2;
        return 1;
    }

    public void setDomainSingle(int r, int c, int val) {
        domainMask[r][c] = (short) (val == ZERO ? 0b01 : 0b10);
    }

    public void resetDomainBoth(int r, int c) { domainMask[r][c] = 0b11; }

    public boolean domainAllows(int r, int c, int val) {
        short m = domainMask[r][c];
        return val == ZERO ? (m & 0b01) != 0 : (m & 0b10) != 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("  ");
        for (int i = 0; i < n; i++) sb.append(i).append(" "); // Numéros colonnes
        sb.append("\n");

        for (int i = 0; i < n; i++) {
            sb.append(i).append(" "); // Numéro ligne
            for (int j = 0; j < n; j++) {
                if (board[i][j] == EMPTY) sb.append(". ");
                else sb.append(board[i][j]).append(" ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}