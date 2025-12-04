# 🚀 AgentCFO - Référence Rapide

## 📊 URLs d'Accès

| Service | Environnement | URL | Port |
|---------|---------------|-----|------|
| **Frontend Web** | Dev | http://localhost:3008 | 3008 |
| **Frontend Web** | Prod | https://cfo.flowbiz.ai | 443 |
| **Backend API** | Dev | http://localhost:8001 | 8001 |
| **Backend API** | Prod | https://cfo.flowbiz.ai | 443 |
| **API Docs** | Dev | http://localhost:8001/docs | 8001 |
| **PostgreSQL** | Dev | localhost:5433 | 5433 |
| **Android (Émulateur)** | Dev | http://10.0.2.2:8001 | - |
| **Android (Release)** | Prod | https://cfo.flowbiz.ai | - |

---

## ⚡ Commandes Essentielles

### Backend + Frontend Web
```bash
# Démarrer tout
docker-compose up -d

# Arrêter tout
docker-compose down

# Voir les logs
docker-compose logs -f

# Redémarrer après changement
docker-compose restart backend
docker-compose restart frontend

# Rebuild complet
docker-compose down
docker-compose build
docker-compose up -d
```

### Application Android
```bash
# Naviguer au projet
cd android-app

# Build debug
./gradlew assembleDebug

# Installer sur appareil
./gradlew installDebug

# Build release (production)
./gradlew assembleRelease

# Voir les logs
adb logcat | grep AgentCFO

# Nettoyer
./gradlew clean
```

### Base de Données
```bash
# Accès PostgreSQL
docker-compose exec postgres psql -U agentcfo agentcfo

# Migrations
docker-compose exec backend alembic upgrade head

# Backup
docker-compose exec postgres pg_dump -U agentcfo agentcfo > backup.sql
```

---

## 📱 Application Android - Configuration

### URLs Backend

**Debug Build** (Développement) :
- Émulateur : `http://10.0.2.2:8001`
- Appareil physique : `http://192.168.1.X:8001` (votre IP locale)

**Release Build** (Production) :
- URL : `https://cfo.flowbiz.ai`
- Configuré automatiquement dans `build.gradle.kts`

### Changer l'URL

Éditez `android-app/app/build.gradle.kts` :
```kotlin
debug {
    // Pour dev local
    buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8001\"")
    
    // OU pour tester contre production
    // buildConfigField("String", "API_BASE_URL", "\"https://cfo.flowbiz.ai\"")
}
```

Puis rebuild :
```bash
./gradlew clean assembleDebug installDebug
```

---

## 📚 Documentation Complète

### Démarrage
- **[README.md](README.md)** - Vue d'ensemble
- **[GETTING_STARTED.md](GETTING_STARTED.md)** - Guide de démarrage Web
- **[DEMARRAGE_ANDROID.md](DEMARRAGE_ANDROID.md)** - Guide Android (5 étapes)

### Android
- **[android-app/README.md](android-app/README.md)** - Installation Android
- **[android-app/IMPLEMENTATION_GUIDE.md](android-app/IMPLEMENTATION_GUIDE.md)** - Guide technique
- **[android-app/CONFIGURATION_URLS.md](android-app/CONFIGURATION_URLS.md)** - Configuration URLs

### Cursor
- **[.cursor/rules/my-project-rules.md](.cursor/rules/my-project-rules.md)** - Règles de code
- **[.cursor/commands/my-custom-commands.md](.cursor/commands/my-custom-commands.md)** - Commandes

### Configuration
- **[PORT_CHANGE_COMPLETE.md](PORT_CHANGE_COMPLETE.md)** - Changement port 3008
- **[PRODUCTION_URL_UPDATE.md](PRODUCTION_URL_UPDATE.md)** - URL cfo.flowbiz.ai
- **[FINAL_CONFIGURATION_SUMMARY.md](FINAL_CONFIGURATION_SUMMARY.md)** - Résumé complet

---

## 🛠️ Dépannage Express

### Backend ne répond pas
```bash
docker-compose ps
docker-compose logs backend
docker-compose restart backend
```

### Frontend erreur
```bash
docker-compose logs frontend
docker-compose restart frontend
```

### Android ne build pas
```bash
cd android-app
./gradlew clean
./gradlew --stop
./gradlew build
```

### Android ne se connecte pas
```bash
# Vérifier l'URL dans les logs
adb logcat | grep "API_BASE_URL"

# Tester le backend
curl http://localhost:8001/health         # Local
curl https://cfo.flowbiz.ai/api/health   # Production
```

### Permission caméra refusée
Aller dans : Paramètres → Apps → AgentCFO → Permissions → Activer Caméra

---

## 📞 Support

### Logs
```bash
# Backend
docker-compose logs -f backend

# Frontend
docker-compose logs -f frontend

# Android
adb logcat | grep AgentCFO

# Tous ensemble
docker-compose logs -f & adb logcat | grep AgentCFO
```

### État des Services
```bash
# Docker
docker-compose ps

# Android
adb devices
```

### API Health Check
```bash
# Local
curl http://localhost:8001/health

# Production
curl https://cfo.flowbiz.ai/api/health
```

---

## ✅ Checklist Démarrage

### Première Fois
- [ ] Backend : `docker-compose up -d`
- [ ] Vérifier : http://localhost:8008
- [ ] Vérifier API : http://localhost:8001/docs
- [ ] Android : `cd android-app && ./gradlew installDebug`
- [ ] Tester l'app Android

### Quotidien
- [ ] `docker-compose up -d`
- [ ] Ouvrir http://localhost:3008
- [ ] Développer et tester

---

**🎯 Tout est documenté et prêt à l'emploi ! 🎯**

Consultez cette référence rapide pour retrouver instantanément les commandes et URLs essentielles.

