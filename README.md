# Résolution et Génération de Binairo (Takuzu) par CSP

![Java](https://img.shields.io/badge/Language-Java-orange)
![AI](https://img.shields.io/badge/Artificial%20Intelligence-CSP-blue)
![Algorithm](https://img.shields.io/badge/Algorithm-Backtracking%20%7C%20AC3-green)

Ce projet implémente un solveur et un générateur haute performance pour le jeu de logique **Binairo** (aussi appelé Takuzu). Il utilise les principes de la **Satisfaction de Contraintes (CSP)** pour résoudre des grilles complexes instantanément.

## 📋 Fonctionnalités

- **Résolution Automatique** : Capable de résoudre des grilles de taille 6x6 à 20x20+ en quelques millisecondes.
- **Génération de Grilles** : Création de puzzles valides avec solution unique (basée sur une approche aléatoire optimisée).
- **Interface Console (CLI)** : Menu interactif pour jouer, générer, résoudre ou comparer les algorithmes.
- **Comparatif d'Algorithmes** : Benchmarking intégré pour comparer les performances (Temps d'exécution, Nombre de nœuds explorés).

## 🧠 Algorithmes et IA

Le cœur du système repose sur un algorithme de **Backtracking** enrichi par des heuristiques et de la propagation de contraintes :

### Heuristiques de Choix de Variable
* **MRV (Minimum Remaining Values)** : Choisit la case avec le moins de possibilités (0 ou 1).
* **Degree Heuristic** : Utilise le nombre de contraintes actives sur les voisins pour départager.

### Heuristiques de Choix de Valeur
* **LCV (Least Constraining Value)** : Tente la valeur qui restreint le moins les voisins (pour la résolution).
* **Randomized Shuffle** : Mélange aléatoire des valeurs (pour la génération de grilles variées).

### Propagation de Contraintes (Inférence)
* **Forward Checking (FC)** : Anticipe les coups impossibles chez les voisins directs.
* **AC-3 (Arc Consistency)** : Algorithme puissant assurant la cohérence globale des arcs avant de tenter une affectation.

## 🚀 Optimisations Techniques (High Performance)

Pour passer de la résolution de petites grilles à des grilles 20x20 instantanées, plusieurs optimisations critiques ont été implémentées :

1.  **Bitmasking des Domaines** : Remplacement des structures lourdes (`HashSet<Integer>`) par des entiers primitifs (`short`).
    * *Gain* : Réduction drastique de l'allocation mémoire et accélération des opérations logiques.
2.  **Vérification en O(1)** : Utilisation de compteurs incrémentaux (`rowZeroCount`, `colOneCount`) pour vérifier les règles de parité instantanément, au lieu de parcourir les lignes à chaque itération.
3.  **Génération "Empty-Start"** : Abandon de la méthode de "diagonale aléatoire" (qui créait des conflits) au profit d'une résolution sur grille vide avec sélection de valeur aléatoire.
    * *Résultat* : Génération robuste sans retours en arrière massifs.

## 🛠️ Installation et Exécution

### Prérequis
* Java JDK 8 ou supérieur.

### Compilation
```bash
javac *.java
```

### Lancement
```bash
java BinairoGame
```

### Structure du Projet
- `BinairoGame.java` : Point d'entrée, gestion de l'UI et règles spécifiques du jeu.

- `CSPSolver.java` : Moteur abstrait de résolution CSP (Backtracking, MRV, AC-3 génériques).

- `BinairoPosition.java` : Représentation de l'état du jeu (Grille, Bitmasks, Compteurs).

- `BinairoMove.java` : Représentation d'un coup (Ligne, Colonne, Valeur).

---