package me.krunsh.ktab.performance;

/**
 * Raison d'une invalidation de viewer.
 *
 * V9.1 n'exploite pas encore les raisons pour faire un rendu partiel :
 * elles sont déjà conservées afin que V9.2 puisse invalider uniquement
 * les dépendances nécessaires.
 */
public enum DirtyReason {

    JOIN,
    GLOBAL,
    MANUAL,
    SKIN,
    CONFIG,
    FALLBACK
}
