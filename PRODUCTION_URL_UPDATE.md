# ✅ Configuration URL Production - https://cfo.flowbiz.ai

## 🌐 Changement Effectué

L'URL de production de l'application Android a été configurée pour utiliser :

**https://cfo.flowbiz.ai**

Au lieu de l'URL générique `https://api.agentcfo.com`

## 📝 Fichiers Modifiés

### Configuration Android
1. **[android-app/app/build.gradle.kts](android-app/app/build.gradle.kts)**
   - Build type `release` : `https://cfo.flowbiz.ai`
   - Build type `debug` : `http://10.0.2.2:8001` (localhost via émulateur)

### Documentation Android
2. **[android-app/README.md](android-app/README.md)**
3. **[android-app/IMPLEMENTATION_GUIDE.md](android-app/IMPLEMENTATION_GUIDE.md)**
4. **[android-app/CONFIGURATION_URLS.md](android-app/CONFIGURATION_URLS.md)** - Nouveau guide complet
5. **[README.md](README.md)** - Section Android
6. **[DEMARRAGE_ANDROID.md](DEMARRAGE_ANDROID.md)**

## 🔧 Configuration par Build Type

### Debug Build (Développement)
```kotlin
debug {
    buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8001\"")
    isMinifyEnabled = false
}
```

**Usage** :
```bash
./gradlew assembleDebug
./gradlew installDebug
```

**Se connecte à** : Backend local sur votre machine

---

### Release Build (Production)
```kotlin
release {
    buildConfigField("String", "API_BASE_URL", "\"https://cfo.flowbiz.ai\"")
    isMinifyEnabled = true
    isShrinkResources = true
}
```

**Usage** :
```bash
./gradlew assembleRelease
```

**Se connecte à** : Backend production `https://cfo.flowbiz.ai`

---

## 🎯 Utilisation

### Pour Développer (Backend Local)
```bash
# 1. Démarrer le backend local
cd /Users/tgdgral9/dev/github/AgentCFO
docker-compose up -d

# 2. Build et installer debug
cd android-app
./gradlew installDebug

# 3. L'app se connecte à votre backend local via 10.0.2.2:8001
```

### Pour Tester en Production (Sans Rebuild Release)

Si vous voulez tester contre la production avec un debug build :

**Modifier temporairement `app/build.gradle.kts`** :
```kotlin
debug {
    buildConfigField("String", "API_BASE_URL", "\"https://cfo.flowbiz.ai\"")
    isMinifyEnabled = false
}
```

**Rebuild** :
```bash
./gradlew clean assembleDebug installDebug
```

### Pour Production Finale (Play Store)

```bash
# Build release (URL production déjà configurée)
./gradlew assembleRelease

# L'APK est dans :
# app/build/outputs/apk/release/app-release.apk
```

## 🔐 Configuration CORS Backend

### Backend Production (cfo.flowbiz.ai)

Le backend doit autoriser les requêtes depuis :
- Frontend web : `https://cfo.flowbiz.ai` (port 3008 si applicable)
- Application Android : Pas de CORS nécessaire (requêtes natives)

**Note** : Les applications mobiles natives (Android/iOS) n'envoient pas d'en-têtes CORS, donc le backend les accepte automatiquement.

### Backend Local

Pour le développement, autoriser :
- Frontend web : `http://localhost:3008`
- Android émulateur : Pas de CORS nécessaire

## 📊 Résumé des URLs

### Frontend Web
- **Dev** : http://localhost:3008
- **Prod** : https://cfo.flowbiz.ai

### Backend API
- **Dev** : http://localhost:8001
- **Prod** : https://cfo.flowbiz.ai (même domaine que le frontend)

### Application Android
- **Dev** : → http://10.0.2.2:8001 (backend local via émulateur)
- **Prod** : → https://cfo.flowbiz.ai (backend production)

## 🧪 Tests de Connexion

### Tester le Backend Production

```bash
# Health check
curl https://cfo.flowbiz.ai/api/health
curl https://cfo.flowbiz.ai/health

# Test de login
curl -X POST https://cfo.flowbiz.ai/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"test123"}'
```

### Tester depuis l'App Android

1. **Installer l'app** (debug ou release)
2. **Essayer de se connecter**
3. **Voir les logs** :
```bash
adb logcat | grep -E "(Retrofit|OkHttp|HTTP)"
```

Les logs montreront les requêtes vers l'URL configurée.

## 🚨 Points d'Attention

### Certificat SSL

L'URL `https://cfo.flowbiz.ai` utilise HTTPS avec un certificat SSL.

**Android vérifie automatiquement les certificats** :
- Certificats valides de Let's Encrypt, DigiCert, etc. : ✅ OK
- Certificats auto-signés : ❌ Rejetés (sauf configuration spéciale)

Si problème de certificat, vérifier :
```bash
curl -I https://cfo.flowbiz.ai
# Doit retourner HTTP 200 sans erreur SSL
```

### Network Security Config (si nécessaire)

Si vous voulez autoriser du HTTP en production (déconseillé) :

**Créer `app/src/main/res/xml/network_security_config.xml`** :
```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="false" />
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">10.0.2.2</domain>
    </domain-config>
</network-security-config>
```

**Référencer dans AndroidManifest.xml** :
```xml
<application
    android:networkSecurityConfig="@xml/network_security_config"
    ...>
```

**Note** : Déjà configuré avec `usesCleartextTraffic="true"` pour le développement.

## 📚 Documentation Créée

- **[CONFIGURATION_URLS.md](CONFIGURATION_URLS.md)** - Ce fichier
- **[PRODUCTION_URL_UPDATE.md](../PRODUCTION_URL_UPDATE.md)** - Récapitulatif

Toute la documentation Android a été mise à jour avec la nouvelle URL de production.

## ✅ Validation

- [x] URL production configurée : `https://cfo.flowbiz.ai`
- [x] URL développement configurée : `http://10.0.2.2:8001`
- [x] Documentation mise à jour
- [x] Build types corrects
- [x] Guide de changement d'URL disponible
- [x] Tests de connexion documentés

---

**🎉 L'application Android est configurée pour utiliser `https://cfo.flowbiz.ai` en production ! 🎉**

**Pour tester** :
```bash
# Build release
./gradlew assembleRelease

# Ou debug vers production (modifiez d'abord le build.gradle.kts)
./gradlew assembleDebug
```

---

**Date** : 4 Décembre 2024
**URL Production** : `https://cfo.flowbiz.ai` ✅

