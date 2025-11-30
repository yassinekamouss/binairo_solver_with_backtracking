public class BinairoPosition extends Position {
    
    // Constantes pour lisibilité
    public static final int EMPTY = -1; // cellule vide car zéro est une valeur valide
    public static final int ZERO = 0;
    public static final int ONE = 1;

    public int n;          // Taille de la grille (ex: 6, 8, 10)
    public int[][] board;  // La matrice de jeu

    // Table des domaines pour FC et AC-3
    // Clé: String "row,col" -> Valeur: SET {0, 1}
    public ST<String, SET<Integer>> domains; 

    public BinairoPosition(int n) {
        this.n = n;
        this.board = new int[n][n];
        this.domains = new ST<String, SET<Integer>>();

        // Initialisation : Tout est vide (-1) et Domaines complets {0, 1}
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                board[i][j] = EMPTY;
                
                // Initialiser le domaine de la case (i,j)
                SET<Integer> domain = new SET<Integer>();
                domain.add(ZERO);
                domain.add(ONE);
                domains.put(getKey(i, j), domain);
            }
        }
    }

    // Essentiel pour la récursion : on doit dupliquer l'état sans lier les références
    public BinairoPosition(BinairoPosition other) {
        this.n = other.n;
        this.board = new int[n][n];
        this.domains = new ST<String, SET<Integer>>();

        // 1. Copier la grille
        for (int i = 0; i < n; i++) {
            System.arraycopy(other.board[i], 0, this.board[i], 0, n);
        }

        // 2. Copier les doBinairoPositionmaines (Attention : il faut recréer les SETs)
        for (String key : other.domains) {
            SET<Integer> originalSet = other.domains.get(key);
            SET<Integer> newSet = new SET<Integer>();
            // On copie le contenu du set
            for (Integer val : originalSet) {
                newSet.add(val);
            }
            this.domains.put(key, newSet);
        }
    }

    // Utilitaire pour générer la clé unique d'une case dans la ST
    public String getKey(int r, int c) {
        return r + "," + c;
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