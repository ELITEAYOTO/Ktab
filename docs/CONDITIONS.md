# Ktab — Conditions dynamiques (V8)

Les conditions peuvent être utilisées sur une **colonne entière** ou sur une
**ligne individuelle**.

Ktab reste indépendant des autres plugins : les comparaisons utilisent les
placeholders résolus par PlaceholderAPI.

## Syntaxe

```yaml
when:
  mode: all
  conditions:
    - type: permission
      value: "volkaria.vip"

    - type: equals
      input: "%kjob_has_display_job%"
      value: "true"
```

`mode` accepte :

- `all` : toutes les conditions doivent être vraies ;
- `any` : au moins une condition doit être vraie.

`case_sensitive` est optionnel et vaut `false` par défaut.

## Types disponibles

### Permission

```yaml
- type: permission
  value: "volkaria.staff"
```

```yaml
- type: not_permission
  value: "volkaria.staff"
```

### Égalité

```yaml
- type: equals
  input: "%kjob_active_mineur%"
  value: "true"
```

```yaml
- type: not_equals
  input: "%some_placeholder%"
  value: "disabled"
```

### Texte

```yaml
- type: contains
  input: "%vault_rank%"
  value: "VIP"
```

Types disponibles :

- `contains`
- `not_contains`
- `starts_with`
- `ends_with`

### Vide / non vide

```yaml
- type: not_empty
  input: "%kjob_display_job_name%"
```

### Nombre de joueurs en ligne

```yaml
- type: online_min
  value: "10"
```

```yaml
- type: online_max
  value: "100"
```

## Condition sur une ligne fixe

```yaml
- row: 8
  text: "&aRécompense disponible !"
  skin: "viewer"

  when:
    mode: all
    conditions:
      - type: equals
        input: "%kjob_claimable_quests%"
        value: "1"
```

Si la condition devient fausse, **la row 8 reste réservée et vide**. Les autres
éléments ne bougent donc pas.

## Condition sur une ligne automatique

```yaml
- text: "&dBonus VIP actif"
  when:
    type: permission
    value: "volkaria.vip"
```

Si la condition est fausse, la ligne ne prend aucune place. Les autres lignes
automatiques peuvent remonter.

## Condition sur une colonne

```yaml
staff:
  enabled: true
  title: "&c&lSTAFF"

  when:
    type: permission
    value: "volkaria.staff"

  lines:
    - "&7Mode modération"
```

Une colonne conditionnelle masquée reste physiquement présente dans la grille,
mais toutes ses cellules deviennent vides. Les autres colonnes ne se décalent
donc jamais.

## Diagnostic

```text
/ktab validate
/ktab dump
```

`/ktab validate` détecte les types inconnus et les champs obligatoires manquants.

`/ktab dump` affiche les décisions runtime :

```text
✔ column.infos.lines[2] - OK equals('true','true')
✘ column.staff - KO permission(volkaria.staff)
```
