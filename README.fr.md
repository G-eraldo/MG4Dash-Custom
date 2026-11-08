# MG4 Custom Dashboard

Fork de [MG4Dash officiel](https://github.com/Skittle6938/MG4Dash), basé sur le commit `536fc5ba52169e1f500226b370cdb0559fb2925e` du 8 novembre 2026. Version Android : `1.7.2-beta34-tesla.1` (`versionCode 1000267`). Le code original et ses services restent présents ; le changement principal est un accueil noir et rouge inspiré des deux maquettes fournies. Le projet est sous licence [GPL-3.0-or-later](COPYING), avec les notices tierces conservées dans `licenses/`.

## Ce qui change

- Nouvel accueil **Personnalisé**, sélectionné au premier lancement et disponible dans **Paramètres › Accueil › Disposition**. Les dispositions officielles Complet, Épuré et Carte restent accessibles.
- Deux compositions : à l'arrêt, MG4 décorative à gauche, carte et lecteur à droite, climatisation en bas ; en roulant, vitesse et véhicule à gauche, carte centrale, raccourcis à droite et navigation rapide en bas.
- La vitesse provient exclusivement de `CarBridge.state.speedKmh`, donnée du service général SWI69. Passage en conduite après 1,5 s à au moins 5 km/h ; retour à l'arrêt après 3 s à au plus 1 km/h. Une vitesse inconnue, incohérente ou intermédiaire conserve la disposition actuelle.
- Carte OpenStreetMap existante forcée en mode sombre pour cet accueil ; navigation plein écran, recentrage et guidage DiAuto existants réutilisés. Le panneau de raccourcis réutilise `Shortcuts.render` et ses actions Home Assistant.
- Palette noire et accent rouge par défaut, avec les autres pages et réglages officiels toujours disponibles. La vérification automatique des mises à jour officielles est désactivée au premier lancement du fork : une mise à jour officielle remplacerait le thème. Le contrôle manuel reste dans les réglages.

Le visuel blanc de la MG4 (`app/src/main/res/drawable-nodpi/mg4_tesla_white.png`) est une illustration décorative générée à partir de la maquette fournie. Il ne représente aucun capteur ni aucun environnement routier. Aucune classe `car/`, aucun mécanisme d'assistance, de sécurité ou d'installation n'a été réécrit.

## Fonctionnalités conservées

Le fork conserve les composants et pages d'origine : données batterie, autonomie, énergie, puissance et consommation ; climatisation ; modes de conduite, régénération et One Pedal selon disponibilité ; assistance à la conduite déjà exposée ; carte, radio, multimédia, téléphone, CarPlay et Android Auto ; applications et raccourcis Home Assistant ; réglages, compatibilité, diagnostic, mise à jour manuelle ; HomeSwitcher, Maintenance Mode, Crash Guard, Rescue et MG4Dash Web. Les fonctionnalités absentes du firmware restent masquées ou indisponibles comme dans le projet officiel.

L'accueil personnalisé n'affiche qu'une partie de ces commandes. Les pages officielles restent accessibles depuis la barre latérale, les tuiles ou la barre de navigation. Les réglages des cartes de l'accueil Complet/Carte restent réservés à ces deux dispositions.

## Écarts explicites avec les maquettes

| Élément des images | Comportement dans le fork |
| --- | --- |
| Rapport engagé | `—` : MG4Dash ne lit pas cette donnée. |
| Limitation de vitesse en panneau | Omise : aucune donnée fiable de limite courante n'est fournie à cet accueil. |
| Itinéraire et prochaine instruction | Affichés uniquement si `NavRepo` reçoit les informations de DiAuto / Headunit Revived. Android Auto d'origine ne les fournit pas. |
| Deux températures de climatisation | Une seule consigne affichée, celle que MG4Dash sait lire et régler. |
| Vue de voies, obstacles ou voitures voisines | Omise : aucune donnée de perception n'est inventée. |
| Carte et médias | Les contenus réels viennent des composants officiels ; les captures de ce dépôt utilisent leurs données simulées. |

## Compatibilité ciblée

Véhicule visé : **MG4 Electric Standard 2024**, MPU `SWI69-29958-1100R45`, MCU `69.5.1.0`, famille SWI69 R45. `Firmware.kt` reconnaît le préfixe `SWI69` et dirige vers la famille A9 et son service `caradapter`. Le [README officiel, section Compatibility](https://github.com/Skittle6938/MG4Dash#compatibility-beta), indique SWI69 testé par l'auteur, mais ne documente pas spécifiquement cette révision R45. Le MCU n'est pas utilisé comme critère de routage. La compilation, les tests sur données simulées et la signature ne vérifient pas l'accès ADB, les services de ce véhicule, sa clé système ni le fonctionnement de l'installation. Ces points sont à relever sur la voiture avant toute écriture.

Sur MG4 Standard LFP, la limite de charge peut être indisponible selon le firmware et la batterie ; le fork conserve la logique officielle qui masque les commandes non exposées.

## Compilation et signature sur macOS

Ce paragraphe sert à reproduire le binaire et à maintenir le fork. L'utilisateur n'a pas besoin de compiler pour installer l'APK livré.

Prérequis : JDK 17, Android SDK API 34 et build-tools, connexion Maven au premier build. Le script `sign_platform.sh` utilise `JAVA_HOME` et `ANDROID_HOME` et produit `dist/MG4Dash-<version>.apk`. La clé de test AOSP indiquée par le projet officiel est absente du dépôt et ignorée par Git. À récupérer localement si l'on reconstruit :

```sh
curl -fsSL 'https://android.googlesource.com/platform/build/+/refs/tags/android-9.0.0_r61/target/product/security/platform.pk8?format=TEXT' | base64 -D > keys/platform.pk8
chmod 600 keys/platform.pk8
./sign_platform.sh
```

Le certificat public fourni est `keys/platform.x509.pem` ; son SHA-256 est `C8:A2:E9:BC:CF:59:7C:2F:B6:DC:66:BE:E2:93:FC:13:F2:FC:47:EC:77:BC:6B:2B:0D:52:C1:1F:51:19:2A:B8`. Il faut comparer la signature des applications système de la voiture avant l'installation. La clé privée de test n'est jamais incluse dans le dépôt ou la Release.

Tests sur ordinateur :

```sh
bash gradlew :app:testEmulatorDebugUnitTest :app:assembleEmulatorDebug :app:assembleCarRelease
```

Les captures de référence du fork sont [à l'arrêt](docs/screenshots/tesla_parked.png) et [en roulant](docs/screenshots/tesla_driving.png). Elles sont rendues par Robolectric à 1920 × 720 avec des données simulées. La variante `emulator` utilise `app.mg4dash.emu` et ne requiert pas l'UID système ; la variante `car` garde `app.mg4dash` et `android.uid.system`.

## Installation et retour à l'origine

Voir le [guide français](docs/INSTALLATION_FR.md). La première étape sur la MG4 sera uniquement en lecture. L'APK sera d'abord lancé comme application ; le remplacement de l'accueil MG et la désinstallation éventuelle demandent deux décisions distinctes après les essais.
