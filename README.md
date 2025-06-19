# PMR - Assistant d'Atelier Automobile (Android)

**Application Android Kotlin pour la gestion et l'assistance des opérations de maintenance en garage automobile, avec support ARCore et mode démo 2D.**

---

## 🚀 Fonctionnalités principales
- Suivi des étapes de réparation (To-Do list interactive)
- Scan de pièces et outils via QR code (ou simulation en mode démo)
- Ajout de notes vocales sur les pièces
- Consultation des fiches techniques (couple de serrage, outils, position, etc.)
- Partage d'avancement (simulation)
- Mode AR (si appareil compatible) ou **mode démo 2D** (émulateur ou appareil non compatible)

---

## ⚡️ Installation rapide

### 1. **Prérequis**
- **JDK 17** (installé et configuré dans Android Studio)
- **Android SDK** (API 34 recommandé)

### 2. **Cloner le projet**
```bash
git clone https://github.com/<votre-repo>/PMR.git
cd PMR/PMR_Project
```

### 3. **Ouvrir dans Android Studio**
- `File` → `Open` → Sélectionnez le dossier `PMR_Project`
- Attendez la synchronisation Gradle (2-3 min)

### 4. **Configurer le SDK Android**
- Android Studio détecte normalement le SDK automatiquement.
- Sinon, éditez le fichier `local.properties` à la racine du projet :
  ```
  sdk.dir=/chemin/vers/votre/Android/Sdk
  ```
  - **Windows** : `C:\Users\<user>\AppData\Local\Android\Sdk`
  - **Mac** : `/Users/<user>/Library/Android/sdk`
  - **Linux** : `/home/<user>/Android/Sdk`

### 5. **Lancer l'application**
- **Sur un émulateur** :
  - `Tools` → `Device Manager` → Créez un appareil virtuel (Pixel, API 34 recommandé)
  - Cliquez sur `Run` dans Android Studio
  - **Le mode démo 2D s'affichera automatiquement** (aucun besoin d'ARCore)
- **Sur un appareil réel** :
  - Activez le mode développeur et le débogage USB
  - Branchez l'appareil et sélectionnez-le dans Android Studio
  - Si l'appareil est compatible ARCore, le mode AR sera activé, sinon le mode démo 2D

---

## 🖥️ **Mode démo 2D (pour la soutenance, les tests, l'émulateur)**
- **Aucune dépendance à ARCore**
- Interface 2D avec :
  - To-Do list interactive
  - Scan QR code simulé
  - Fiche technique détaillée
  - Commande vocale simulée
  - Simulation de partage d'avancement
- **Idéal pour les captures d'écran et la présentation**

---

## 🛠️ **Dépannage courant**
- **Pop-up ARCore** : Si vous voyez un message "Google Play Services for AR", vérifiez que le mode démo est bien activé (voir DemoModeManager) et que le manifeste contient bien :
  ```xml
  <meta-data android:name="com.google.ar.core" android:value="optional" />
  ```
- **Erreur de SDK/JDK** : Vérifiez le chemin dans `local.properties` et la version du JDK dans Android Studio (`File > Project Structure > SDK Location`)
- **Problème d'émulateur** :
  - Vérifiez que l'émulateur est bien démarré dans Device Manager
  - Activez la virtualisation dans le BIOS si besoin

---

## **Contribuer**
- Forkez le repo, créez une branche, proposez vos améliorations via Pull Request !
- Merci de documenter vos changements.

---

## **Licence**
Projet open-source, libre de réutilisation et d'adaptation.

---

# PMR