import java.util.Collections;
import java.util.List;

public abstract class CSPSolver {

    public boolean useMRV = false;      // Minimum Remaining Values
    public boolean useDegree = false;   // Degree Heuristic
    public boolean useLCV = false;      // Least Constraining Value
    public boolean useFC = false;       // Forward Checking
    public boolean useAC3 = false;      // Arc Consistency

    // --- STATISTIQUES (POUR COMPARAISON) ---
    public long nodeCount = 0;          // Nombre de nœuds explorés
    public long startTime = 0;
    public double executionTime = 0;    // En secondes
    // Limite facultative de temps pour interrompre la recherche
    public Long timeLimitMs = null;

    /**
     * Point d'entrée principal pour lancer la résolution
     */
    public Position solve(Position startPos) {
        this.nodeCount = 0;
        this.startTime = System.currentTimeMillis();
        
        // Lancer la récursion
        Position result = backtracking(startPos);
        
        this.executionTime = (System.currentTimeMillis() - this.startTime) / 1000.0;
        System.out.println(this.executionTime);
        return result;
    }

    /**
     * L'ALGORITHME DE BACKTRACKING (Cœur du moteur)
     */
    protected Position backtracking(Position p) {
        nodeCount++;

        // Interruption douce si limite de temps dépassée
        if (timeLimitMs != null) {
            long elapsed = System.currentTimeMillis() - this.startTime;
            if (elapsed > timeLimitMs) return null;
        }

        // 1. TEST D'ARRÊT : Si la grille est complète et valide
        if (isComplete(p)) {
            return p;
        }

        // 2. CHOIX DE LA VARIABLE (Case vide)
        // C'est ici que MRV et Degree interviennent
        Move var = selectVariable(p);
        
        // Si aucune variable n'est retournée alors qu'on n'est pas complet, c'est une impasse
        if (var == null) return null;

        // 3. CHOIX DES VALEURS (0 ou 1)
        // C'est ici que LCV intervient
        List<Integer> values = orderDomainValues(p, var);

        // 4. BOUCLE D'ESSAI
        for (int val : values) {
            
            // Créer le coup complet (ligne, col, valeur)
            // On cast pour accéder aux champs spécifiques si besoin, 
            // mais ici on suppose que 'var' contient déjà row/col
            BinairoMove move = new BinairoMove(((BinairoMove)var).row, ((BinairoMove)var).col, val);

            // A. VÉRIFICATION DE LA CONSISTANCE (Règles du jeu)
            if (isValid(p, move)) {
                
                // Appliquer le coup sur une COPIE de la position (pour pouvoir backtracker)
                // Note: applyMove doit utiliser le constructeur de copie de BinairoPosition
                Position nextP = applyMove(p, move);

                // B. PROPAGATION DE CONTRAINTES (FC ou AC-3)
                // Si activé, on réduit les domaines des futures variables.
                // Si un domaine devient vide, runInference renvoie false -> on coupe la branche.
                if (useFC || useAC3) {
                    if (!runInference(nextP, move)) {
                        continue; // "Elagage" : on abandonne cette branche
                    }
                }

                // C. APPEL RÉCURSIF
                Position result = backtracking(nextP);
                if (result != null) {
                    return result; // Solution trouvée !
                }
            }
            // Si on arrive ici, c'est que 'val' ne mène pas à une solution.
            // On boucle pour tester la valeur suivante (Backtrack implicite).
        }

        return null; // Échec : aucune valeur ne fonctionne pour cette variable
    }

    /**
     * Sélectionne la prochaine variable à assigner.
     * Implémente MRV et Degree Heuristic.
     */
    protected Move selectVariable(Position p) {
        List<Move> vars = getUnassignedVariables(p);

        if (vars.isEmpty()) return null;

        // Si aucune heuristique, retourner la première variable
        if (!useMRV && !useDegree) {
            return vars.get(0);
        }

        Move bestVar = null;
        int minDomainSize = Integer.MAX_VALUE;
        int maxDegree = -1;

        for (Move var : vars) {
            int currentDomainSize = useMRV ? getDomainSize(p, var) : 0;
            int currentDegree = 0;

            boolean shouldUpdate = false;

            if (useMRV && useDegree) {
                // CAS 1 : MRV + Degree (tie-breaker)
                if (currentDomainSize < minDomainSize) {
                    // Nouveau minimum MRV trouvé
                    minDomainSize = currentDomainSize;
                    currentDegree = getDegree(p, var);
                    maxDegree = currentDegree;
                    shouldUpdate = true;
                }
                else if (currentDomainSize == minDomainSize) {
                    // Égalité MRV : utiliser Degree comme tie-breaker
                    currentDegree = getDegree(p, var);
                    if (currentDegree > maxDegree) {
                        maxDegree = currentDegree;
                        shouldUpdate = true;
                    }
                }
            }
            else if (useMRV) {
                // CAS 2 : MRV seul
                if (currentDomainSize < minDomainSize) {
                    minDomainSize = currentDomainSize;
                    shouldUpdate = true;
                }
            }
            else if (useDegree) {
                // CAS 3 : Degree seul
                currentDegree = getDegree(p, var);
                if (currentDegree > maxDegree) {
                    maxDegree = currentDegree;
                    shouldUpdate = true;
                }
            }

            if (shouldUpdate) {
                bestVar = var;
            }
        }

        return bestVar;
    }
    /**
     * Ordonne les valeurs du domaine.
     * Implémente LCV (Least Constraining Value).
     */
    protected List<Integer> orderDomainValues(Position p, Move var) {
        // Récupérer les valeurs possibles depuis la Position (gérée par ST/SET dans BinairoPosition)
        // Pour Binairo simple c'est souvent juste {0, 1}
        List<Integer> values = getDomainValues(p, var);

        if (!useLCV) {
            return values; // Ordre par défaut (souvent 0 puis 1)
        }

        // Stratégie LCV : Trier les valeurs par "impact croissant" sur les voisins
        // On veut la valeur qui élimine le MOINS de choix pour les autres.
        Collections.sort(values, (v1, v2) -> {
            int constraint1 = countConstraints(p, var, v1);
            int constraint2 = countConstraints(p, var, v2);
            return constraint1 - constraint2; // Tri croissant
        });

        return values;
    }

    /**
     * Exécute Forward Checking ou AC-3.
     * Retourne false si un domaine devient vide (échec).
     */
    protected boolean runInference(Position p, Move lastMove) {
        if (useAC3) {
            return ac3(p); // Algorithme le plus puissant
        } else if (useFC) {
            return forwardChecking(p, lastMove); // Algorithme plus léger
        }
        return true;
    }

    // MÉTHODES ABSTRAITES (A implémenter dans BinairoGame)
    // Ces méthodes dépendent spécifiquement des règles du jeu Binairo

    // Est-ce que la grille est pleine ?
    public abstract boolean isComplete(Position p);

    // Retourne la liste des cases vides (sous forme de Move sans valeur définie)
    public abstract List<Move> getUnassignedVariables(Position p);

    // Vérifie si poser 'move' est légal (règles immédiates: triple, parité...)
    public abstract boolean isValid(Position p, Move move);

    // Applique le coup et retourne une NOUVELLE position (copie)
    public abstract Position applyMove(Position p, Move move);
    
    // Retourne la taille du domaine d'une variable (0, 1 ou 2 pour Binairo)
    public abstract int getDomainSize(Position p, Move var);
    
    // Retourne le degré (nombre de voisins non assignés ou contraintes actives)
    public abstract int getDegree(Position p, Move var);
    
    // Retourne les valeurs du domaine (ex: [0, 1])
    public abstract List<Integer> getDomainValues(Position p, Move var);
    
    // Simule l'impact d'une valeur (pour LCV) : combien de valeurs supprime-t-elle chez les voisins ?
    public abstract int countConstraints(Position p, Move var, int val);

    // -- Pour la propagation --
    
    // Applique Forward Checking : met à jour les domaines des voisins
    public abstract boolean forwardChecking(Position p, Move lastMove);
    
    // Applique AC-3 : propagation complète
    public abstract boolean ac3(Position p);
}