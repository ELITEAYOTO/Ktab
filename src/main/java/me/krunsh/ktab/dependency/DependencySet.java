package me.krunsh.ktab.dependency;

/**
 * Dépendances nécessaires au rendu d'une cellule/condition.
 *
 * Un cache de rendu peut rester valide tant que :
 * - son TTL n'est pas expiré ;
 * - la revision globale n'a pas changé pour les dépendances globales.
 */
public final class DependencySet {

    private boolean global;
    private boolean permission;
    private boolean dynamic;
    private long ttlTicks = Long.MAX_VALUE;

    public void markGlobal() {
        global = true;
    }

    public void markPermission() {
        permission = true;
    }

    public void markDynamic() {
        dynamic = true;
    }

    public void includeTtl(
            long ttlTicks) {

        long safe =
            Math.max(
                0L,
                ttlTicks
            );

        this.ttlTicks =
            Math.min(
                this.ttlTicks,
                safe
            );
    }

    public void merge(
            DependencySet other) {

        if (other == null) {
            return;
        }

        global =
            global || other.global;

        permission =
            permission || other.permission;

        dynamic =
            dynamic || other.dynamic;

        ttlTicks =
            Math.min(
                ttlTicks,
                other.ttlTicks
            );
    }

    public boolean isGlobal() {
        return global;
    }

    public boolean isPermission() {
        return permission;
    }

    public boolean isDynamic() {
        return dynamic;
    }

    public long getTtlTicks(
            long fallback) {

        if (ttlTicks == Long.MAX_VALUE) {
            return Math.max(
                0L,
                fallback
            );
        }

        return ttlTicks;
    }
}
