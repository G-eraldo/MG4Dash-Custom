# Essai sur MG4 SWI69 R45 depuis un Mac

Ce guide concerne l'APK voiture signé `MG4Dash-1.7.2-beta34-tesla.1.apk`. Il suit le mécanisme du [projet officiel](https://github.com/Skittle6938/MG4Dash#install-on-the-car) et attend la validation des étapes sur la voiture. Le succès des tests sur ordinateur n'est pas une validation de SWI69 R45.

## Préparation

- Mac, câble USB capable de transférer des données et accès au port USB de l'unité multimédia qui permet une connexion ADB. Le port à utiliser et l'activation d'ADB doivent être vérifiés sur ce véhicule : ne pas supposer que le débogage est actif.
- [Android SDK Platform-Tools pour macOS](https://developer.android.com/tools/releases/platform-tools), qui fournit `adb`. Dans Terminal : `adb version` doit répondre. Pour vérifier localement l'APK, Android build-tools fournit `apksigner`.
- APK téléchargé depuis la Release du fork ou reçu comme livrable, puis empreinte SHA-256 comparée à celle publiée. Ne jamais utiliser la variante `app.mg4dash.emu` sur la voiture.
- Voiture à l'arrêt dans un endroit sûr, alimentation suffisante et écran multimédia disponible pendant les vérifications.

## 1. Vérifications sans écriture

Nous lancerons **une commande à la fois** dans Terminal et attendrons le résultat avant la suivante :

```sh
adb devices -l
adb shell getprop ro.build.mt2712.version
adb shell getprop ro.build.version.release
adb shell getprop ro.build.version.sdk
adb shell pm list packages app.mg4dash
adb shell pm path com.saicmotor.launcher
```

`adb devices -l` doit afficher un appareil autorisé. S'il n'y a aucun appareil, si le statut est `unauthorized`, ou si ADB est désactivé, la procédure s'arrête. Les dépôts publics ne donnent pas de confirmation du chemin d'activation pour **SWI69-29958-1100R45** ; nous vérifierons les options réellement présentes et ne contournerons aucune protection.

Les propriétés doivent confirmer la famille SWI69 et l'API Android compatible (`minSdk 28`). Ensuite, il faut comparer le certificat des applications système de cette voiture à celui de notre APK. `pm path` fournit le chemin de l'APK d'origine ; `adb pull <chemin_affiché> ./saic-launcher.apk` en fait une copie en lecture seule. Sur le Mac :

```sh
apksigner verify --print-certs saic-launcher.apk
apksigner verify --print-certs MG4Dash-1.7.2-beta34-tesla.1.apk
shasum -a 256 MG4Dash-1.7.2-beta34-tesla.1.apk
```

Le certificat attendu par le projet officiel est la clé de test plateforme AOSP, SHA-256 `C8:A2:E9:BC:CF:59:7C:2F:B6:DC:66:BE:E2:93:FC:13:F2:FC:47:EC:77:BC:6B:2B:0D:52:C1:1F:51:19:2A:B8`. Si la signature ou le firmware diffèrent, ou si une application `app.mg4dash` déjà installée a une autre signature, nous arrêtons l'installation pour diagnostiquer. La comparaison du certificat du launcher ne prouve pas à elle seule que tous les services seront accessibles.

## 2. Installation après accord explicite

Le projet officiel indique que `adb install` simple est refusé par l'unité. Son mécanisme est une copie dans `/data/local/tmp`, puis l'installation comme mise à jour système. Ces commandes écrivent sur l'unité ; nous ne les utiliserons qu'après les vérifications précédentes et votre accord :

```sh
adb push MG4Dash-1.7.2-beta34-tesla.1.apk /data/local/tmp/mg4dash-custom.apk
adb shell pm install -r -f /data/local/tmp/mg4dash-custom.apk
adb shell rm /data/local/tmp/mg4dash-custom.apk
```

Si `pm install` refuse la signature, la version ou les permissions, il faut s'arrêter et lire l'erreur, sans forcer un contournement.

## 3. Premier lancement sans remplacer l'accueil MG

Après installation, lancement direct :

```sh
adb shell am start -n app.mg4dash/.MainActivity
```

Nous vérifierons ensemble la vue personnalisée, l'alternance arrêt/roulage **seulement lors d'un essai sûr**, les données réellement reçues, la carte et les médias, puis les pages Énergie, Véhicule, Climatisation, Téléphone, Applications, Paramètres et les raccourcis. Le rapport reste « — » par conception. Le guidage d'itinéraire requiert une source compatible DiAuto ; aucune information de navigation n'est simulée sur la voiture.

## 4. Accueil principal, seulement après un second accord

Si les essais sont satisfaisants et si le mécanisme officiel est compatible, ouvrir **Paramètres MG4Dash › Système › Remplacer l'accueil SAIC** et confirmer. Après redémarrage, vérifier l'ouverture, le bouton Accueil et MG4Dash Rescue. Cette opération n'est pas nécessaire pour tester l'application.

## Retour à l'accueil MG et désinstallation

Dans MG4Dash : **Paramètres › Système › Mode maintenance** remet l'accueil SAIC avant toute désinstallation. Vérifier que le bouton Accueil revient à l'interface MG. Si l'application doit ensuite être retirée, le projet officiel documente `adb uninstall app.mg4dash` ; cette étape n'intervient qu'après le retour vérifié à l'accueil SAIC.

Crash Guard (trois plantages en trois minutes), MG4Dash Rescue et le mode maintenance sont inchangés dans ce fork. Leur présence dans le code et l'APK est vérifiable sur ordinateur ; leur action au démarrage ne peut être confirmée que sur la voiture.
