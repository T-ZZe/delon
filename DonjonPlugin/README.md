# DonjonPlugin

Plugin Paper 26.3 (Java 25) : la commande `/donjon` ouvre un menu avec 3 donjons.

| Donjon | Monde | Plateforme |
|---|---|---|
| Donjon Castel | `donjon_castel` | Pierre |
| Donjon Cieux | `donjon_cieux` | Glowstone |
| Donjon Abysse | `donjon_abysse` | Netherrack |

Les 3 mondes sont des mondes vides (ni overworld, ni nether, ni end), créés automatiquement au démarrage.

## Commandes
- `/donjon` : ouvre le menu
- `/donjon retour` : retour au monde principal

Permission : `donjon.use` (par défaut pour tout le monde).

## Compiler
GitHub Actions compile à chaque push : le jar est dans l'onglet **Actions** > dernier run > **Artifacts**.
En local : `mvn clean package` (JDK 25).
