# Ktab — Layout V7

## Positionnement automatique

La syntaxe historique reste valide :

```yaml
lines:
  - "&7Joueur: &f%player_name%"
  - "&7Métier: &e%kjob_display_job_name%"
```

Les lignes sont placées dans la prochaine ligne libre.

## Positionnement exact avec `row`

Les lignes du TAB sont numérotées de **1 à 20** :

```yaml
lines:
  - row: 2
    text: "&7Joueur: &f%player_name%"
    skin: "viewer"

  - row: 6
    text: "&7Quêtes: &a%kjob_claimable_quests%"
```

Cela permet de laisser les lignes 3, 4 et 5 vides sans écrire trois chaînes `""`.

## Position du titre

Par défaut un titre est sur la ligne 1.

```yaml
jobs:
  title: "&e&lMÉTIERS"
  title_row: 1
```

Il peut être déplacé :

```yaml
jobs:
  title: "&e&lMÉTIERS"
  title_row: 3
```

## Désactiver une cellule sans la supprimer

```yaml
- enabled: false
  row: 8
  text: "&cMaintenance"
  skin: "server"
```

## Mélange auto + fixe

```yaml
lines:
  - row: 5
    text: "&6Important"

  - "&7Cette ligne prend la première place libre"
  - "&7Puis la suivante"
```

Ktab réserve d'abord les positions explicites, puis place les lignes automatiques
dans les emplacements encore disponibles.

## Validation

```text
/ktab validate
```

Détecte notamment :

- deux cellules configurées sur la même `row` ;
- une `row` hors de 1..20 / hors du nombre de lignes du layout ;
- une skin inconnue ;
- plus de colonnes actives que `columns_count` ;
- un `max_entries` qui tronque la grille ;
- trop de cellules automatiques pour la place disponible.

## Dump du rendu réel

```text
/ktab dump
/ktab dump Krunsh_
/ktab dump Krunsh_ 2
```

Le dump affiche pour chaque entrée :

```text
#00 c1/r1 infos skin=none
   VOLKARIA

#01 c1/r2 infos skin=viewer
   Joueur: Krunsh_
```

Les pages contiennent 15 cellules.
