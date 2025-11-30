import java.util.*;

public class BinairoGame extends CSPSolver {

    public static Scanner scanner = new Scanner(System.in);

    // =========================================================================
    // I. IMPLÉMENTATION DES MÉTHODES ABSTRAITES DE CSPSOLVER
    // =========================================================================

    @Override
    public boolean isComplete(Position p) {
        BinairoPosition pos = (BinairoPosition) p;
        // Vérification rapide : plus de cases vides
        for (int i = 0; i < pos.n; i++) {
            for (int j = 0; j < pos.n; j++) {
                if (pos.board[i][j] == BinairoPosition.EMPTY) return false;
            }
        }
        // Vérification lourde finale (Unicité des lignes/colonnes)
        return checkUniqueRowsCols(pos);
    }

    @Override
    public List<Move> getUnassignedVariables(Position p) {
        BinairoPosition pos = (BinairoPosition) p;
        List<Move> moves = new ArrayList<>();
        for (int i = 0; i < pos.n; i++) {
            for (int j = 0; j < pos.n; j++) {
                if (pos.board[i][j] == BinairoPosition.EMPTY) {
                    moves.add(new BinairoMove(i, j, -1));
                }
            }
        }
        return moves;
    }

    @Override
    public boolean isValid(Position p, Move move) {
        BinairoPosition pos = (BinairoPosition) p;
        BinairoMove m = (BinairoMove) move;
        // Vérification locale immédiate (Triple et Parité)
        return checkMoveRules(pos, m.row, m.col, m.value);
    }

    @Override
    public Position applyMove(Position p, Move move) {
        BinairoPosition original = (BinairoPosition) p;
        BinairoMove m = (BinairoMove) move;
        
        // Copie Profonde
        BinairoPosition copy = new BinairoPosition(original);
        
        // Application du coup
        copy.board[m.row][m.col] = m.value;
        // Mise à jour des compteurs
        if (m.value == 0) { copy.rowZeroCount[m.row]++; copy.colZeroCount[m.col]++; }
        else { copy.rowOneCount[m.row]++; copy.colOneCount[m.col]++; }
        
        // Mise à jour du domaine : la variable assignée n'a plus qu'une seule valeur possible
        copy.setDomainSingle(m.row, m.col, m.value);

        return copy;
    }

    // =========================================================================
    // II. HEURISTIQUES AVANCÉES (IMPLÉMENTATIONS RÉELLES)
    // =========================================================================

    @Override
    public int getDomainSize(Position p, Move var) {
        BinairoPosition pos = (BinairoPosition) p;
        BinairoMove m = (BinairoMove) var;
        return pos.getDomainSize(m.row, m.col);
    }

    @Override
    public int getDegree(Position p, Move var) {
        BinairoPosition pos = (BinairoPosition) p;
        BinairoMove m = (BinairoMove) var;
        int degree = 0;
        // Degré = nombre de voisins non assignés dans la ligne et la colonne
        // Ce sont les variables "connectées" par une contrainte de ligne/colonne
        for (int k = 0; k < pos.n; k++) {
            if (k != m.col && pos.board[m.row][k] == BinairoPosition.EMPTY) degree++;
            if (k != m.row && pos.board[k][m.col] == BinairoPosition.EMPTY) degree++;
        }
        return degree;
    }

    @Override
    public List<Integer> getDomainValues(Position p, Move var) {
        BinairoPosition pos = (BinairoPosition) p;
        BinairoMove m = (BinairoMove) var;
        List<Integer> list = new ArrayList<>();
        if (pos.domainAllows(m.row, m.col, BinairoPosition.ZERO)) list.add(0);
        if (pos.domainAllows(m.row, m.col, BinairoPosition.ONE)) list.add(1);
        return list;
    }

    /**
     * VRAIE IMPLÉMENTATION LCV
     * Compte combien de valeurs sont supprimées des domaines des voisins
     * si on affecte 'val' à 'var'.
     */
    @Override
    public int countConstraints(Position p, Move var, int val) {
        BinairoPosition pos = (BinairoPosition) p;
        BinairoMove m = (BinairoMove) var;
        int constraintsCost = 0;

        // On simule temporairement l'affectation dans la grille SANS copier tout l'objet (pour perf)
        // On pose la valeur
        int oldVal = pos.board[m.row][m.col];
        pos.board[m.row][m.col] = val;

        // On parcourt tous les voisins (Ligne et Colonne) qui sont vides
        // Voisins Ligne
        for (int c = 0; c < pos.n; c++) {
            if (c == m.col || pos.board[m.row][c] != BinairoPosition.EMPTY) continue;
            
            // Pour ce voisin, combien de ses valeurs possibles deviennent invalides ?
            if (pos.domainAllows(m.row, c, 0)) {
                // Si le voisin prenait nVal, est-ce que ce serait compatible avec notre val ?
                // On utilise checkMoveRules sur le voisin
                if (!checkMoveRules(pos, m.row, c, 0)) constraintsCost++;
            }
            if (pos.domainAllows(m.row, c, 1)) {
                if (!checkMoveRules(pos, m.row, c, 1)) constraintsCost++;
            }
        }

        // Voisins Colonne
        for (int r = 0; r < pos.n; r++) {
            if (r == m.row || pos.board[r][m.col] != BinairoPosition.EMPTY) continue;
            
            if (pos.domainAllows(r, m.col, 0)) {
                if (!checkMoveRules(pos, r, m.col, 0)) constraintsCost++;
            }
            if (pos.domainAllows(r, m.col, 1)) {
                if (!checkMoveRules(pos, r, m.col, 1)) constraintsCost++;
            }
        }

        // On remet la grille dans l'état initial (Backtrack local)
        pos.board[m.row][m.col] = oldVal;

        return constraintsCost;
    }

    @Override
    public boolean forwardChecking(Position p, Move lastMove) {
        BinairoPosition pos = (BinairoPosition) p;
        BinairoMove m = (BinairoMove) lastMove;
        
        // Mise à jour des domaines pour tous les voisins de ligne et colonne
        // Si une révision vide un domaine, on retourne false immédiatement
        
        // Ligne
        for (int c = 0; c < pos.n; c++) {
            if (pos.board[m.row][c] == BinairoPosition.EMPTY) {
                if (!revise(pos, m.row, c)) return false;
            }
        }
        // Colonne
        for (int r = 0; r < pos.n; r++) {
            if (pos.board[r][m.col] == BinairoPosition.EMPTY) {
                if (!revise(pos, r, m.col)) return false;
            }
        }
        return true;
    }

    /**
     * VRAIE IMPLÉMENTATION AC-3 AVEC FILE (QUEUE)
     */
    @Override
    public boolean ac3(Position p) {
        BinairoPosition pos = (BinairoPosition) p;
        
        // 1. Initialisation de la File avec tous les arcs
        // Un arc = une dépendance entre deux variables non affectées (Xi, Xj)
        // Dans Binairo, tout élément d'une ligne dépend des autres de la ligne (et idem colonne)
        Queue<Arc> queue = new LinkedList<>();

        // On remplit la queue avec toutes les paires de contraintes
        for (int i = 0; i < pos.n; i++) {
            for (int j = 0; j < pos.n; j++) {
                if (pos.board[i][j] == BinairoPosition.EMPTY) {
                    addArcsForVariable(pos, i, j, queue);
                }
            }
        }

        // 2. Boucle principale AC-3
        while (!queue.isEmpty()) {
            Arc arc = queue.poll(); // Extraire (Xi, Xj)
            
            // Si on supprime une valeur de Xi parce qu'elle n'est pas compatible avec Xj
            if (reviseArc(pos, arc.xi, arc.xj)) {
                
                // Vérifier si le domaine est devenu vide (Échec)
                if (pos.getDomainSize(arc.xi.r, arc.xi.c) == 0) {
                    return false;
                }
                
                // Si changement, on doit revérifier tous les voisins de Xi (sauf Xj)
                // On ajoute les arcs (Xk, Xi) dans la file
                addNeighborArcs(pos, arc.xi, arc.xj, queue);
            }
        }
        return true;
    }

    // --- CLASSES UTILITAIRES POUR AC-3 ---
    
    private class Coord {
        int r, c;
        Coord(int r, int c) { this.r = r; this.c = c; }
        // Pour comparaison
        public boolean equals(Coord o) { return this.r == o.r && this.c == o.c; }
    }

    private class Arc {
        Coord xi, xj;
        Arc(Coord xi, Coord xj) { this.xi = xi; this.xj = xj; }
    }

    // Ajoute tous les arcs (Xi, Xk) où Xk est un voisin de Xi
    private void addArcsForVariable(BinairoPosition pos, int r, int c, Queue<Arc> queue) {
        Coord xi = new Coord(r, c);
        // Voisins Ligne
        for (int k = 0; k < pos.n; k++) {
            if (k != c && pos.board[r][k] == BinairoPosition.EMPTY) {
                queue.add(new Arc(xi, new Coord(r, k)));
            }
        }
        // Voisins Colonne
        for (int k = 0; k < pos.n; k++) {
            if (k != r && pos.board[k][c] == BinairoPosition.EMPTY) {
                queue.add(new Arc(xi, new Coord(k, c)));
            }
        }
    }

    // Ajoute les arcs entrants vers xi (sauf xj)
    private void addNeighborArcs(BinairoPosition pos, Coord xi, Coord xj, Queue<Arc> queue) {
        // Ligne
        for (int k = 0; k < pos.n; k++) {
            if (k != xi.c && pos.board[xi.r][k] == BinairoPosition.EMPTY) {
                Coord xk = new Coord(xi.r, k);
                if (!xk.equals(xj)) queue.add(new Arc(xk, xi));
            }
        }
        // Colonne
        for (int k = 0; k < pos.n; k++) {
            if (k != xi.r && pos.board[k][xi.c] == BinairoPosition.EMPTY) {
                Coord xk = new Coord(k, xi.c);
                if (!xk.equals(xj)) queue.add(new Arc(xk, xi));
            }
        }
    }

    // La fonction REVISE spécifique à AC-3 (Arc consistency check)
    // Vérifie si pour tout x dans D(xi), il existe un y dans D(xj) qui satisfait les contraintes
    private boolean reviseArc(BinairoPosition pos, Coord xi, Coord xj) {
        boolean revised = false;
        List<Integer> toRemove = new ArrayList<>();

        for (int x = 0; x <= 1; x++) {
            if (!pos.domainAllows(xi.r, xi.c, x)) continue;
            boolean supported = false;
            
            // On cherche un support y dans le domaine de Xj
            for (int y = 0; y <= 1; y++) {
                if (!pos.domainAllows(xj.r, xj.c, y)) continue;
                if (isConsistentPair(pos, xi, x, xj, y)) {
                    supported = true;
                    break;
                }
            }
            
            if (!supported) {
                toRemove.add(x);
                revised = true;
            }
        }

        for (int val : toRemove) {
            // retirer val du masque
            short m = pos.domainMask[xi.r][xi.c];
            if (val == 0) m = (short)(m & ~0b01); else m = (short)(m & ~0b10);
            pos.domainMask[xi.r][xi.c] = m;
        }
        return revised;
    }

    // Vérifie si l'affectation (Xi=valX, Xj=valY) est légale
    private boolean isConsistentPair(BinairoPosition pos, Coord xi, int valX, Coord xj, int valY) {
        // Pose virtuelle SANS modifier les compteurs (checkMoveRules ajoute +1 logique)
        int oldXi = pos.board[xi.r][xi.c];
        int oldXj = pos.board[xj.r][xj.c];

        pos.board[xi.r][xi.c] = valX;
        pos.board[xj.r][xj.c] = valY;

        boolean valid = checkMoveRules(pos, xi.r, xi.c, valX) &&
                        checkMoveRules(pos, xj.r, xj.c, valY);

        // revert
        pos.board[xi.r][xi.c] = oldXi;
        pos.board[xj.r][xj.c] = oldXj;
        return valid;
    }


    // Fonction REVISE simple pour Forward Checking (sans file)
    private boolean revise(BinairoPosition pos, int r, int c) {
        List<Integer> toRemove = new ArrayList<>();

        for (int val = 0; val <= 1; val++) {
            if (!pos.domainAllows(r, c, val)) continue;
            if (!checkMoveRules(pos, r, c, val)) {
                toRemove.add(val);
            }
        }
        for (int val : toRemove) {
            short m = pos.domainMask[r][c];
            if (val == 0) m = (short)(m & ~0b01); else m = (short)(m & ~0b10);
            pos.domainMask[r][c] = m;
        }
        return pos.getDomainSize(r, c) > 0;
    }

    // =========================================================================
    // III. RÈGLES DU JEU (LOGIQUE PURE)
    // =========================================================================

    private boolean checkMoveRules(BinairoPosition pos, int r, int c, int val) {
        int[][] b = pos.board;
        int n = pos.n;

        // 1. Règle du Triple (Pas de 000 ou 111)
        // Vérification avec conditions aux limites pour ne pas sortir du tableau
        if (c >= 2 && b[r][c-1] == val && b[r][c-2] == val) return false;
        if (c < n-2 && b[r][c+1] == val && b[r][c+2] == val) return false;
        if (c > 0 && c < n-1 && b[r][c-1] == val && b[r][c+1] == val) return false;

        if (r >= 2 && b[r-1][c] == val && b[r-2][c] == val) return false;
        if (r < n-2 && b[r+1][c] == val && b[r+2][c] == val) return false;
        if (r > 0 && r < n-1 && b[r-1][c] == val && b[r+1][c] == val) return false;

        // 2. Règle de Parité (Max N/2 occurrences)
        if (val == 0) {
            if (pos.rowZeroCount[r] + 1 > n / 2) return false;
            if (pos.colZeroCount[c] + 1 > n / 2) return false;
        } else {
            if (pos.rowOneCount[r] + 1 > n / 2) return false;
            if (pos.colOneCount[c] + 1 > n / 2) return false;
        }

        return true;
    }

    private boolean checkUniqueRowsCols(BinairoPosition pos) {
        // Vérifie unicité des lignes
        HashSet<String> lines = new HashSet<>();
        for (int i = 0; i < pos.n; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < pos.n; j++) sb.append(pos.board[i][j]);
            String s = sb.toString();
            // On ne vérifie l'unicité que si la ligne est complète (ne contient pas -1)
            if (!s.contains("-1")) {
                if (lines.contains(s)) return false;
                lines.add(s);
            }
        }
        
        // Vérifie unicité des colonnes
        HashSet<String> cols = new HashSet<>();
        for (int j = 0; j < pos.n; j++) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < pos.n; i++) sb.append(pos.board[i][j]);
            String s = sb.toString();
            if (!s.contains("-1")) {
                if (cols.contains(s)) return false;
                cols.add(s);
            }
        }
        return true;
    }

    // =========================================================================
    // IV. MAIN & INTERFACE UTILISATEUR
    // =========================================================================

    public static void main(String[] args) {
        BinairoGame game = new BinairoGame();
        BinairoPosition currentPos = null;

        while (true) {
            System.out.println("\n=== JEU BINAIRO (TAKUZU) ===");
            System.out.println("1. Générer une nouvelle grille");
            System.out.println("2. Jouer Manuellement");
            System.out.println("3. Résoudre Automatiquement (Choix Algos)");
            System.out.println("4. Comparer les Algorithmes");
            System.out.println("5. Quitter");
            System.out.print("Choix: ");
            
            int choice = -1;
            try { choice = Integer.parseInt(scanner.nextLine()); } catch(Exception e) {}

            switch (choice) {
                case 1:
                    currentPos = game.generateGridUI();
//                    currentPos = getGrid20x20(); // Pour test rapide
                    break;
                case 2:
                    if (currentPos != null) game.playManual(currentPos);
                    else System.out.println("Générez d'abord une grille !");
                    break;
                case 3:
                    if (currentPos != null) game.solveAutoUI(currentPos);
                    else System.out.println("Générez d'abord une grille !");
                    break;
                case 4:
                    if (currentPos != null) game.compareAlgorithms(currentPos);
                    else System.out.println("Générez d'abord une grille !");
                    break;
                case 5:
                    System.out.println("Au revoir.");
                    System.exit(0);
                    break;
                default:
                    System.out.println("Invalide.");
            }
        }
    }


    /**
     *
     * @fonction de test
     */
    public static BinairoPosition getGrid20x20() {
        int n = 14;
        BinairoPosition p = new BinairoPosition(n);

        // On remplit les diagonales pour briser les symétries et forcer des contraintes
        // Cela rend la grille valide mais difficile.
        for (int i = 0; i < n; i++) {
            p.board[i][i] = (i % 2); // Diagonale 1: 0, 1, 0, 1...
            p.board[i][n - 1 - i] = ((i + 1) % 2); // Diagonale 2: 1, 0, 1, 0...
        }

        // On ajoute quelques "pièges" (clusters) pour tester le Forward Checking
        // Un carré de valeurs en haut à gauche
        p.board[0][1] = 0; p.board[1][0] = 0;

        // Un carré au milieu
        p.board[10][11] = 1; p.board[11][10] = 1;

        // Mettre à jour compteurs et domaines pour les cases fixées
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (p.board[i][j] == 0) { p.rowZeroCount[i]++; p.colZeroCount[j]++; p.setDomainSingle(i,j,0); }
                else if (p.board[i][j] == 1) { p.rowOneCount[i]++; p.colOneCount[j]++; p.setDomainSingle(i,j,1); }
            }
        }

        return p;
    }
    /**
     * OPTIMISATION CRITIQUE : Surcharge de la méthode de sélection de valeur.
     * En mélangeant l'ordre des valeurs (0, 1) ou (1, 0), on introduit de l'aléatoire
     * directement au cœur du solveur. Cela permet de générer des grilles variées
     * à partir d'une grille vide, sans avoir besoin de "seed" la diagonale.
     */
    @Override
    protected List<Integer> orderDomainValues(Position p, Move var) {
        // 1. Récupérer les valeurs standard via la méthode du parent (CSPSolver)
        List<Integer> values = super.orderDomainValues(p, var);

        // 2. Si on n'utilise pas LCV (qui trie intelligemment), on mélange !
        // Cela est essentiel pour la génération aléatoire.
        if (!useLCV) {
            Collections.shuffle(values);
        }

        return values;
    }

    // --- GENERATEUR ROBUSTE ---
    public BinairoPosition generateGridUI() {
        System.out.print("Taille (pair, ex: 6, 8, 14, 20): ");
        int n = -1;
        try {
            n = Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Nombre invalide.");
            return null;
        }

        if (n % 2 != 0) {
            System.out.println("La taille doit être paire.");
            return null;
        }

        System.out.println("Génération en cours (avec MRV+Degree+FC+AC3, restart si lent)...");

        // 1. On part d'une grille TOTALEMENT vide
        // Plus de diagonale aléatoire qui bloque le solveur !
        BinairoPosition empty = new BinairoPosition(n);

        // 2. Configuration pour pruning fort pendant la génération
        this.useMRV = true;
        this.useDegree = true;
        this.useFC = true;
        this.useLCV = false; // garder l'aléatoire via shuffle
        this.useAC3 = false; // AC-3 coûteux à l'init sur grandes tailles

        // 3. Résolution avec limite de temps et redémarrage si nécessaire
        Position fullSol = null;
        int attempts = 0;
        while (attempts < 10 && fullSol == null) {
            attempts++;
            this.timeLimitMs = 5000L; // 5s par tentative pour éviter les branches pathologiques
            fullSol = solve(new BinairoPosition(empty));
        }
        this.timeLimitMs = null; // désactiver la limite pour les autres opérations

        if (fullSol == null) {
            System.out.println("Échec de génération après plusieurs tentatives. Réessayez.");
            return new BinairoPosition(n);
        }

        // 4. Création des trous (Difficulté)
        BinairoPosition puzzle = new BinairoPosition((BinairoPosition) fullSol);
        Random rand = new Random();

        // On retire environ 60% des cases pour faire le puzzle
        int cellsToRemove = (n * n) * 60 / 100;
        int removedCount = 0;

        while (removedCount < cellsToRemove) {
            int r = rand.nextInt(n);
            int c = rand.nextInt(n);

            // Si la case n'est pas déjà vide, on la vide
            if (puzzle.board[r][c] != BinairoPosition.EMPTY) {
                // décrémenter compteurs
                if (puzzle.board[r][c] == 0) { puzzle.rowZeroCount[r]--; puzzle.colZeroCount[c]--; }
                else if (puzzle.board[r][c] == 1) { puzzle.rowOneCount[r]--; puzzle.colOneCount[c]--; }

                puzzle.board[r][c] = BinairoPosition.EMPTY;
                puzzle.resetDomainBoth(r, c);
                removedCount++;
            }
        }

        System.out.println("Grille générée avec succès :\n" + puzzle);
        return puzzle;
    }

    // --- COMPARATEUR ---
    public void compareAlgorithms(BinairoPosition startPos) {
        System.out.println("\n--- COMPARATIF ---");
        System.out.println(String.format("%-20s | %-8s | %-8s", "Algo", "Temps(s)", "Nœuds"));
        System.out.println("--------------------------------------------");

        // Attention : AC-3 est très lourd sur les petites grilles car il initialise trop d'arcs
        // Il brille sur les problèmes très contraints complexes.
        
        runTest("BT Simple",    startPos, false, false, false, false, false);
        runTest("MRV",          startPos, true,  false, false, false, false);
        runTest("MRV+FC",       startPos, true,  false, false, true,  false);
        runTest("MRV+LCV",      startPos, true,  false, true,  false, false);
        runTest("MRV+AC3",      startPos, true,  false, false, false, true);
    }

    private void runTest(String name, BinairoPosition p, boolean mrv, boolean deg, boolean lcv, boolean fc, boolean ac3) {
        this.useMRV = mrv; this.useDegree = deg; this.useLCV = lcv; 
        this.useFC = fc; this.useAC3 = ac3;
        solve(new BinairoPosition(p));
        System.out.println(String.format("%-20s | %-8.4f | %-8d", name, executionTime, nodeCount));
    }

    // --- RESOLUTION UI ---
    public void solveAutoUI(BinairoPosition pos) {
        System.out.print("MRV? (o/n): "); boolean mrv = scanner.nextLine().equalsIgnoreCase("o");
        System.out.print("Degree? (o/n): "); boolean deg = scanner.nextLine().equalsIgnoreCase("o");
        System.out.print("LCV? (o/n): "); boolean lcv = scanner.nextLine().equalsIgnoreCase("o");
        System.out.print("Forward Check? (o/n): "); boolean fc = scanner.nextLine().equalsIgnoreCase("o");
        System.out.print("AC-3? (o/n): "); boolean ac3 = scanner.nextLine().equalsIgnoreCase("o");

        this.useMRV = mrv; this.useDegree = deg; this.useLCV = lcv; this.useFC = fc; this.useAC3 = ac3;
        
        Position res = solve(new BinairoPosition(pos));
        if(res != null) System.out.println(res);
        else System.out.println("Pas de solution.");
    }
    
    // --- MANUEL ---
    public void playManual(BinairoPosition pos) {
        BinairoPosition cur = new BinairoPosition(pos);
        while(!isComplete(cur)) {
            System.out.println(cur);
            System.out.print("Coup (lig col val) ou HINT: ");
            String s = scanner.nextLine().toUpperCase();
            if(s.equals("EXIT")) return;
            if(s.equals("HINT")) {
                this.useMRV=true; this.useFC=true; this.useAC3=false;
                Position sol = solve(new BinairoPosition(cur));
                if(sol!=null) {
                    BinairoPosition solved = (BinairoPosition)sol;
                    outer: for(int i=0;i<cur.n;i++) for(int j=0;j<cur.n;j++) 
                        if(cur.board[i][j]==-1) { System.out.println("Indice: "+solved.board[i][j]+" en "+i+","+j); break outer;}
                } else System.out.println("Impossible!");
                continue;
            }
            try {
                String[] p = s.split(" ");
                int r=Integer.parseInt(p[0]), c=Integer.parseInt(p[1]), v=Integer.parseInt(p[2]);
                if(checkMoveRules(cur,r,c,v)) {
                    // mettre à jour compteurs
                    if (cur.board[r][c] == 0) { cur.rowZeroCount[r]--; cur.colZeroCount[c]--; }
                    else if (cur.board[r][c] == 1) { cur.rowOneCount[r]--; cur.colOneCount[c]--; }
                    cur.board[r][c]=v;
                    if (v==0) { cur.rowZeroCount[r]++; cur.colZeroCount[c]++; }
                    else { cur.rowOneCount[r]++; cur.colOneCount[c]++; }
                }
                else System.out.println("Invalide!");
            } catch(Exception e) { System.out.println("Erreur format."); }
        }
        System.out.println("Bravo!");
    }
}