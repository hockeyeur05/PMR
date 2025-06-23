# PMR - Assistant d'Atelier Automobile (Android)

**Application Android Kotlin pour la gestion d'atelier automobile avec AR, QR code, reconnaissance vocale locale (sans API), et gestion de tâches.**

---

## 🚀 Installation pour collègues (prise en main rapide)

### 1. Prérequis
- **Android Studio** (Hedgehog ou plus récent recommandé)
- **JDK 17**
- **Android SDK** (API 34 recommandé)
- **Tablette ou smartphone Android 7.0+** (mode AR complet sur tablette Samsung, mode démo sur émulateur)

### 2. Cloner le projet
```bash
git clone https://github.com/<votre-repo>/PMR.git
cd PMR/PMR_Project
```

### 3. Ouvrir dans Android Studio
- `File` → `Open` → Sélectionnez le dossier `PMR_Project`
- Attendez la synchronisation Gradle (2-3 min)

### 4. Configurer le SDK Android
- Android Studio détecte normalement le SDK automatiquement
- Sinon, éditez le fichier `local.properties` à la racine du projet :
  ```
  sdk.dir=/chemin/vers/votre/Android/Sdk
  ```

### ⚠️ Si Android Studio ne détecte pas le JDK 17 automatiquement
- Va dans `File` → `Project Structure` → `SDK Location`
- Renseigne le chemin du JDK 17 (exemple : `C:\Program Files\Java\jdk-17.0.10`)
- Ou ajoute dans `local.properties` à la racine du projet :
  ```
  org.gradle.java.home=C:\Program Files\Java\jdk-17.0.10
  ```

### 5. Lancer l'application
- **Sur votre appareil** :
  - Activez le mode développeur et le débogage USB
  - Branchez l'appareil et sélectionnez-le dans Android Studio
  - Cliquez sur ▶️ pour lancer l'app
- **Sur un émulateur** :
  - Le mode démo 2D s'affichera automatiquement

### 6. Aucune clé API requise
- **La reconnaissance vocale fonctionne SANS clé API** (tout est local, aucune configuration Gemini ou Google Cloud à faire)
- **Aucune modification de code n'est nécessaire** pour la voix, le QR ou l'AR

### 7. Points de vigilance
- **Permissions** :
  - L'application demandera l'accès à la caméra et au micro au premier lancement
  - Acceptez toutes les permissions pour profiter du scan QR et de la voix
- **Si vous souhaitez modifier le nom du package ou d'autres paramètres** :
  - Faites-le dans `AndroidManifest.xml` et dans le dossier `java/com/example/pmr_project/`
- **Si vous rencontrez une erreur de SDK** :
  - Vérifiez la version du SDK dans `local.properties` et dans les paramètres du projet

---

## 🎤 Commandes vocales reconnues

En mode Mécanicien AR, vous pouvez contrôler la liste des tâches à la voix :

- **Pour démarrer une tâche** :
  - `je commence (nomDeLaTache)`
  - Exemple : « je commence Remplacer les plaquettes de frein »
- **Pour terminer une tâche** :
  - `j'ai fini (nomDeLaTache)`
  - Exemple : « j'ai fini Changer le filtre à huile »
- **Pour ajouter une tâche** :
  - `ajoute (nomDeLaTache)`
  - Exemple : « ajoute Vidange »

> **Astuce** : Le nom de la tâche doit correspondre au titre affiché dans la liste (insensible à la casse).

---

## 📱 Fonctionnalités principales

- Tableau de bord avec statistiques
- Scanner QR Code (ML Kit)
- Vue AR complète (Sceneform, mode démo 2D sur émulateur)
- Reconnaissance vocale avancée (commandes locales, sans API)
- Gestion des tâches (ajout, démarrage, validation par la voix)
- Fiches techniques détaillées (affichage contextuel via QR)
- Compatibilité lunettes AR (Bluetooth)

---

## 🛠️ Dépannage courant

- **Permissions caméra/micro** : Vérifiez dans les paramètres Android si l'app n'a pas accès
- **Scan QR ne fonctionne pas** : Testez avec les QR codes de démonstration
- **Problème ARCore** : L'app bascule automatiquement en mode démo si ARCore n'est pas dispo
- **Erreur de build** : Vérifiez le SDK, la version de Gradle, et synchronisez le projet

---

## 🔧 Personnalisation rapide

- **Changer le nom de l'app** : Modifiez `android:label` dans `AndroidManifest.xml`
- **Modifier les tâches ou pièces de démo** : Éditez le fichier `DemoData.kt` dans `app/src/main/java/com/example/pmr_project/demo/`
- **Adapter les couleurs/thèmes** : Voir `ui/theme/`

---

## 👨‍💻 Contribuer
- Forkez le repo, créez une branche, proposez vos améliorations via Pull Request !
- Merci de documenter vos changements.

---

## 📝 Licence
Projet open-source, libre de réutilisation et d'adaptation.

---

# PMR - Assistant d'Atelier Automobile