# Configuration des URLs Backend - Application Android

## 🌐 URLs Configurées

### Environnements

L'application Android supporte deux environnements :

#### 🔧 Development (Debug Build)
- **URL** : `http://10.0.2.2:8001`
- **Usage** : Émulateur Android connecté au backend local
- **CORS** : Backend doit autoriser cette origine
- **Build** : `./gradlew assembleDebug`

#### 🚀 Production (Release Build)
- **URL** : `https://cfo.flowbiz.ai`
- **Usage** : Application finale pour utilisateurs
- **CORS** : Backend production doit autoriser cette origine
- **Build** : `./gradlew assembleRelease`

## 📝 Configuration Actuelle

### Dans `app/build.gradle.kts`

```kotlin
defaultConfig {
    // URL par défaut (utilisée si pas de build type spécifique)
    buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8001\"")
}

buildTypes {
    release {
        // Production
        buildConfigField("String", "API_BASE_URL", "\"https://cfo.flowbiz.ai\"")
        isMinifyEnabled = true
        isShrinkResources = true
    }
    
    debug {
        // Développement local
        buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8001\"")
        isMinifyEnabled = false
    }
}
```

## 🔄 Changer l'URL

### Option 1 : Utiliser la Production en Debug

Si vous voulez tester contre la production sans faire un build release :

**Modifier `app/build.gradle.kts`** :
```kotlin
debug {
    buildConfigField("String", "API_BASE_URL", "\"https://cfo.flowbiz.ai\"")
    isMinifyEnabled = false
}
```

**Rebuild** :
```bash
./gradlew clean assembleDebug
./gradlew installDebug
```

### Option 2 : Utiliser un Appareil Physique avec Backend Local

Si vous testez sur un appareil physique connecté au même réseau :

1. **Trouver votre IP locale** :
```bash
ifconfig | grep "inet " | grep -v 127.0.0.1
# Exemple : inet 192.168.1.100
```

2. **Modifier `app/build.gradle.kts`** :
```kotlin
debug {
    buildConfigField("String", "API_BASE_URL", "\"http://192.168.1.100:8001\"")
    isMinifyEnabled = false
}
```

3. **Rebuild** :
```bash
./gradlew clean assembleDebug
./gradlew installDebug
```

### Option 3 : Build Types Personnalisés

Vous pouvez créer un build type supplémentaire pour la staging :

**Ajouter dans `app/build.gradle.kts`** :
```kotlin
buildTypes {
    release {
        buildConfigField("String", "API_BASE_URL", "\"https://cfo.flowbiz.ai\"")
        // ... autres configs
    }
    
    debug {
        buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8001\"")
        // ... autres configs
    }
    
    // Nouveau build type pour staging
    create("staging") {
        initWith(getByName("debug"))
        buildConfigField("String", "API_BASE_URL", "\"https://staging.cfo.flowbiz.ai\"")
        applicationIdSuffix = ".staging"
        versionNameSuffix = "-staging"
    }
}
```

**Build staging** :
```bash
./gradlew assembleStaging
./gradlew installStaging
```

## 🔐 Configuration CORS Backend

Pour que l'application Android fonctionne, le backend doit autoriser les origines suivantes :

### Backend Local (`backend/app/main.py` ou `config.py`)

```python
ALLOWED_ORIGINS = [
    "http://localhost:3008",          # Frontend web local
    "https://cfo.flowbiz.ai",         # Frontend web production
    "http://10.0.2.2:8001",          # Android emulator
    # Ajouter autres IPs si nécessaire
]
```

### Backend Production

Le backend en production doit autoriser :
- `https://cfo.flowbiz.ai` (frontend web)
- Toutes les requêtes mobiles (pas de CORS pour API calls depuis apps natives)

**Note** : Les applications Android natives n'envoient pas d'en-têtes CORS Origin, donc le backend devrait les accepter automatiquement.

## 🧪 Tester les URLs

### Tester depuis le Terminal

**Production** :
```bash
curl -I https://cfo.flowbiz.ai/api/health
# Devrait retourner HTTP 200 ou HTTP 404 si /health n'existe pas
```

**Local depuis l'émulateur** :
```bash
# D'abord sur votre machine
curl http://localhost:8001/health

# Depuis l'émulateur (via adb shell)
adb shell curl http://10.0.2.2:8001/health
```

### Tester depuis l'App

1. **Installer l'app**
2. **Ouvrir et essayer de se connecter**
3. **Voir les logs** :
```bash
adb logcat | grep -E "(AgentCFO|Retrofit|OkHttp)"
```

Les logs montreront les requêtes HTTP avec les URLs utilisées.

## 📱 Builds Recommandés

### Pour le Développement
```bash
# Debug build avec backend local
./gradlew installDebug
```

**Avantage** :
- Logs détaillés
- Fast rebuild
- Backend sur votre machine

### Pour les Tests Utilisateurs
```bash
# Debug build avec backend production
# (après avoir modifié l'URL dans build.gradle.kts)
./gradlew installDebug
```

**Avantage** :
- Teste contre les vraies données
- Pas besoin de backend local

### Pour la Production
```bash
# Release build (automatiquement vers production)
./gradlew assembleRelease
```

**Avantage** :
- Optimisé et minifié
- URL production déjà configurée
- Prêt pour Play Store

## 🔍 Debug des Problèmes de Connexion

### L'app ne se connecte pas ?

1. **Vérifier l'URL utilisée** :
```bash
# Voir les logs de l'app
adb logcat | grep "API_BASE_URL"
```

2. **Vérifier que le backend répond** :
```bash
# Production
curl https://cfo.flowbiz.ai/api/health

# Local
curl http://localhost:8001/health
```

3. **Vérifier les logs Retrofit** :
```bash
# Logs complets des requêtes HTTP
adb logcat | grep OkHttp
```

4. **Vérifier les permissions Internet** :
```bash
# AndroidManifest.xml doit avoir :
# <uses-permission android:name="android.permission.INTERNET" />
```

5. **Vérifier les logs backend** :
```bash
# Si local
docker-compose logs -f backend

# Chercher les erreurs CORS ou 401/403
```

## 📋 Checklist de Configuration

### Pour Développement Local
- [x] Backend démarré : `docker-compose up -d`
- [x] Backend accessible : `curl http://localhost:8001/health`
- [x] URL debug : `http://10.0.2.2:8001`
- [x] Build : `./gradlew installDebug`

### Pour Tests Production
- [x] URL production : `https://cfo.flowbiz.ai`
- [x] Backend accessible : `curl https://cfo.flowbiz.ai`
- [x] Modifier debug URL vers production (si nécessaire)
- [x] Build : `./gradlew installDebug`

### Pour Release Production
- [x] URL production : `https://cfo.flowbiz.ai` (déjà configurée)
- [x] Build : `./gradlew assembleRelease`
- [x] Signer l'APK (Play Store)
- [x] Tester avant publication

## 💡 Astuces

### Switcher rapidement entre Local et Production

**Créer un fichier `local.properties`** (ignoré par git) :
```properties
# Pour debug local
backend.url=http://10.0.2.2:8001

# Pour debug production
# backend.url=https://cfo.flowbiz.ai
```

**Puis dans `app/build.gradle.kts`** :
```kotlin
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

val backendUrl = localProperties.getProperty("backend.url", "http://10.0.2.2:8001")

debug {
    buildConfigField("String", "API_BASE_URL", "\"$backendUrl\"")
}
```

**Avantage** : Changer l'URL sans modifier le code !

## ✅ Validation

Le changement d'URL vers `https://cfo.flowbiz.ai` est maintenant configuré et documenté !

**Build release** utilisera automatiquement : **https://cfo.flowbiz.ai** ✅

---

**Date** : 4 Décembre 2024
**Production URL** : `https://cfo.flowbiz.ai`
**Dev URL** : `http://10.0.2.2:8001`

