# Commandes personnalisées AgentCFO

## 🐳 Docker & Services

### Démarrage et arrêt
```bash
# Démarrer tous les services
docker-compose up -d

# Démarrer avec rebuild
docker-compose up -d --build

# Arrêter tous les services
docker-compose down

# Arrêter et supprimer volumes (⚠️ perte de données)
docker-compose down -v

# Redémarrer un service spécifique
docker-compose restart backend
docker-compose restart frontend
docker-compose restart postgres
```

### Logs et monitoring
```bash
# Voir tous les logs en temps réel
docker-compose logs -f

# Logs d'un service spécifique
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs -f postgres

# Dernières 50 lignes
docker-compose logs --tail=50 backend

# État des conteneurs
docker-compose ps

# Ressources utilisées
docker stats
```

### Rebuild
```bash
# Rebuild backend après changement code Python
docker-compose build backend
docker-compose up -d backend

# Rebuild frontend après changement package.json
docker-compose build frontend
docker-compose up -d frontend

# Rebuild tout
docker-compose build
docker-compose up -d
```

## 🗄️ Base de données

### Accès PostgreSQL
```bash
# Accéder au shell PostgreSQL
docker-compose exec postgres psql -U agentcfo agentcfo

# Lister les tables
docker-compose exec postgres psql -U agentcfo agentcfo -c '\dt'

# Voir structure d'une table
docker-compose exec postgres psql -U agentcfo agentcfo -c '\d users'

# Requête SQL
docker-compose exec postgres psql -U agentcfo agentcfo -c 'SELECT * FROM users;'

# Vérifier l'état
docker-compose exec postgres pg_isready -U agentcfo
```

### Migrations Alembic
```bash
# Créer une nouvelle migration
docker-compose exec backend alembic revision --autogenerate -m "Description du changement"

# Appliquer les migrations
docker-compose exec backend alembic upgrade head

# Voir l'historique des migrations
docker-compose exec backend alembic history

# Revenir à la migration précédente
docker-compose exec backend alembic downgrade -1

# Voir l'état actuel
docker-compose exec backend alembic current
```

### Backup et restauration
```bash
# Créer un backup
docker-compose exec postgres pg_dump -U agentcfo agentcfo > backup_$(date +%Y%m%d_%H%M%S).sql

# Restaurer depuis un backup
docker-compose exec -T postgres psql -U agentcfo agentcfo < backup.sql

# Backup avec compression
docker-compose exec postgres pg_dump -U agentcfo agentcfo | gzip > backup.sql.gz

# Restaurer depuis backup compressé
gunzip -c backup.sql.gz | docker-compose exec -T postgres psql -U agentcfo agentcfo
```

## 🐍 Backend Python

### Shell et Python
```bash
# Accéder au shell du conteneur backend
docker-compose exec backend bash

# Python interactif avec contexte app
docker-compose exec backend python3

# Exécuter un script Python
docker-compose exec backend python3 -c "from app.config import settings; print(settings.OPENAI_API_KEY[:10])"

# Installer une nouvelle dépendance (développement)
docker-compose exec backend pip install nouvelle-lib
# Puis ajouter à requirements.txt et rebuild
```

### Tests backend
```bash
# Lancer pytest (à configurer)
docker-compose exec backend pytest

# Tests avec coverage
docker-compose exec backend pytest --cov=app

# Tests spécifiques
docker-compose exec backend pytest tests/test_auth.py
```

## ⚛️ Frontend Next.js

### Shell et npm
```bash
# Accéder au shell du conteneur frontend
docker-compose exec frontend sh

# Installer une dépendance
docker-compose exec frontend npm install nouvelle-lib

# Mise à jour des dépendances
docker-compose exec frontend npm update

# Nettoyer le cache Next.js
docker-compose exec frontend rm -rf .next
docker-compose restart frontend

# Vérifier les dépendances obsolètes
docker-compose exec frontend npm outdated
```

### Build et optimisation
```bash
# Build de production
docker-compose exec frontend npm run build

# Analyser le bundle
docker-compose exec frontend npm run build -- --analyze

# Linter
docker-compose exec frontend npm run lint
```

## 🔧 Développement

### Développement local (sans Docker)
```bash
# Backend
cd backend
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000

# Frontend
cd frontend
npm install
npm run dev
```

### Variables d'environnement
```bash
# Voir les variables chargées (backend)
docker-compose exec backend python3 -c "from app.config import settings; print(vars(settings))"

# Modifier .env et recharger
nano .env
docker-compose restart backend frontend
```

### Debug
```bash
# Activer logs debug backend
# Dans .env: ENVIRONMENT=development

# Voir requêtes SQL (dans config.py ajouter)
# echo=True dans create_engine()

# Frontend debug
# Dans browser DevTools: localStorage, Network tab
```

## 🧪 Tests API

### Avec curl
```bash
# Health check
curl http://localhost:8001/health

# Register
curl -X POST http://localhost:8001/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"test123","full_name":"Test User"}'

# Login
curl -X POST http://localhost:8001/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"test123"}'

# Dashboard (avec token)
TOKEN="votre-token-ici"
curl -H "Authorization: Bearer $TOKEN" http://localhost:8001/api/dashboard/

# Upload document
curl -X POST http://localhost:8001/api/documents/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@/path/to/document.pdf" \
  -F "document_type=invoice"
```

### Documentation API interactive
```bash
# Ouvrir Swagger UI
open http://localhost:8001/docs

# Ouvrir ReDoc
open http://localhost:8001/redoc

# Récupérer OpenAPI JSON
curl http://localhost:8001/openapi.json > openapi.json
```

## 🔍 Dépannage

### Problèmes courants
```bash
# Port déjà utilisé
lsof -ti:8001 | xargs kill -9  # Backend
lsof -ti:3008 | xargs kill -9  # Frontend
lsof -ti:5433 | xargs kill -9  # PostgreSQL

# Nettoyer tout Docker
docker-compose down -v
docker system prune -a --volumes  # ⚠️ Supprime tout!

# Recréer depuis zéro
docker-compose down -v
docker-compose build --no-cache
docker-compose up -d

# Vérifier espace disque Docker
docker system df
```

### Erreurs backend
```bash
# Erreur d'import Python
docker-compose exec backend pip list | grep package-name

# Erreur de base de données
docker-compose logs postgres | tail -50
docker-compose exec postgres psql -U agentcfo agentcfo -c '\conninfo'

# Erreur OpenAI
docker-compose exec backend python3 -c "import openai; print(openai.__version__)"
cat .env | grep OPENAI_API_KEY
```

### Erreurs frontend
```bash
# Erreur de compilation
docker-compose logs frontend | grep "Error"
docker-compose exec frontend npm run build

# Problèmes de cache
docker-compose exec frontend rm -rf .next node_modules
docker-compose exec frontend npm install
docker-compose restart frontend
```

## 📦 Production

### Préparation déploiement
```bash
# Vérifier la configuration
cat .env | grep -v "^#" | grep -v "^$"

# Build de production
ENVIRONMENT=production docker-compose build

# Test de production localement
ENVIRONMENT=production docker-compose up -d
```

### Monitoring production
```bash
# Logs en continu
docker-compose logs -f --tail=100

# Espace disque
df -h
docker system df

# Processus
docker-compose top

# Stats en temps réel
docker stats --no-stream
```

### Backup automatique (cron)
```bash
# Créer script de backup
cat > /opt/agentcfo/backup.sh << 'EOF'
#!/bin/bash
BACKUP_DIR="/opt/backups/agentcfo"
DATE=$(date +%Y%m%d_%H%M%S)
mkdir -p $BACKUP_DIR
docker-compose exec -T postgres pg_dump -U agentcfo agentcfo | gzip > $BACKUP_DIR/agentcfo_$DATE.sql.gz
find $BACKUP_DIR -name "agentcfo_*.sql.gz" -mtime +7 -delete
EOF

chmod +x /opt/agentcfo/backup.sh

# Ajouter au crontab
crontab -e
# Ajouter: 0 2 * * * /opt/agentcfo/backup.sh
```

## 📱 Android App

### Build et installation
```bash
# Naviguer vers le projet Android
cd android-app/

# Vérifier la version de Gradle
./gradlew --version

# Nettoyer le build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Build release APK (production)
./gradlew assembleRelease

# Installer sur appareil/émulateur connecté
./gradlew installDebug

# Build et installer en une commande
./gradlew installDebug --rerun-tasks
```

### Tests Android
```bash
# Lancer les tests unitaires
./gradlew test

# Tests instrumentés (nécessite appareil/émulateur)
./gradlew connectedAndroidTest

# Tests avec rapport détaillé
./gradlew test --info

# Voir les résultats des tests
open app/build/reports/tests/testDebugUnitTest/index.html
```

### Debug et logs
```bash
# Voir les logs Android en temps réel
adb logcat | grep AgentCFO

# Voir tous les logs de l'app
adb logcat -s AgentCFO:*

# Effacer les logs
adb logcat -c

# Voir les appareils connectés
adb devices

# Informations sur l'appareil
adb shell getprop ro.build.version.release  # Version Android
adb shell getprop ro.product.model          # Modèle appareil
```

### APK Management
```bash
# Installer un APK
adb install app/build/outputs/apk/debug/app-debug.apk

# Réinstaller (écrase l'ancienne version)
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Désinstaller l'app
adb uninstall com.agentcfo

# Vérifier si l'app est installée
adb shell pm list packages | grep agentcfo

# Lancer l'app
adb shell am start -n com.agentcfo/.MainActivity

# Forcer arrêt de l'app
adb shell am force-stop com.agentcfo
```

### Gradle maintenance
```bash
# Arrêter tous les daemons Gradle
./gradlew --stop

# Rafraîchir les dépendances
./gradlew build --refresh-dependencies

# Build avec logs détaillés
./gradlew build --stacktrace --info

# Voir toutes les tâches disponibles
./gradlew tasks --all

# Nettoyer complètement
./gradlew clean
rm -rf .gradle app/build

# Vérifier la configuration du projet
./gradlew projects
./gradlew dependencies
```

### Configuration backend pour Android
```bash
# L'émulateur Android utilise 10.0.2.2 pour accéder au localhost du host

# Vérifier que le backend est accessible
curl http://10.0.2.2:8001/health

# Depuis l'appareil physique, utiliser l'IP locale
# Trouver votre IP locale
ifconfig | grep "inet " | grep -v 127.0.0.1

# Exemple: 192.168.1.100:8001
```

### Permissions et cache
```bash
# Effacer les données de l'app
adb shell pm clear com.agentcfo

# Révoquer les permissions
adb shell pm revoke com.agentcfo android.permission.CAMERA
adb shell pm revoke com.agentcfo android.permission.READ_EXTERNAL_STORAGE

# Accorder les permissions (pour tests)
adb shell pm grant com.agentcfo android.permission.CAMERA
adb shell pm grant com.agentcfo android.permission.READ_MEDIA_IMAGES

# Voir le stockage utilisé
adb shell du -h /data/data/com.agentcfo
```

### Android Studio alternative (CLI)
```bash
# Si Android Studio n'est pas disponible

# 1. S'assurer que JAVA_HOME est configuré
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
export PATH="$JAVA_HOME/bin:$PATH"

# 2. Configurer ANDROID_HOME
export ANDROID_HOME=~/Library/Android/sdk
export PATH="$ANDROID_HOME/platform-tools:$PATH"

# 3. Vérifier
java -version
adb version

# 4. Build
cd android-app
./gradlew assembleDebug

# 5. Installer
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 🚀 Quick Start

### Premier lancement (Web)
```bash
# 1. Configuration
cp .env.example .env
nano .env  # Ajouter OPENAI_API_KEY

# 2. Build et start
docker-compose up -d --build

# 3. Vérifier
docker-compose ps
docker-compose logs -f

# 4. Ouvrir l'app web
open http://localhost:3008
```

### Premier lancement (Android)
```bash
# 1. S'assurer que le backend est démarré
docker-compose up -d

# 2. Configurer Java (si nécessaire)
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
export PATH="$JAVA_HOME/bin:$PATH"

# 3. Build l'app Android
cd android-app
./gradlew assembleDebug

# 4. Installer sur émulateur/appareil
./gradlew installDebug

# 5. Lancer l'app manuellement ou via :
adb shell am start -n com.agentcfo/.MainActivity
```

### Redémarrage quotidien
```bash
# Voir état
docker-compose ps

# Redémarrer si nécessaire
docker-compose restart

# Voir les logs récents
docker-compose logs --tail=50 backend frontend
```

## 🛠️ Maintenance

### Mise à jour dépendances
```bash
# Backend
cd backend
pip list --outdated
# Mettre à jour requirements.txt
docker-compose build backend

# Frontend
cd frontend
npm outdated
npm update
docker-compose build frontend
```

### Nettoyage
```bash
# Nettoyer images inutilisées
docker image prune -a

# Nettoyer volumes inutilisés
docker volume prune

# Nettoyer conteneurs arrêtés
docker container prune

# Tout nettoyer
docker system prune -a --volumes
```

## 📝 Notes

- Toujours tester en local avant de déployer
- Faire des backups réguliers de la base de données
- Monitorer les logs pour détecter problèmes tôt
- Garder les dépendances à jour (sécurité)
- Documenter les changements importants

## 🔗 Liens utiles

### Web
- Frontend: http://localhost:3008
- Backend API: http://localhost:8001
- API Docs: http://localhost:8001/docs
- PostgreSQL: localhost:5433

### Android
- Projet: `android-app/`
- APK Debug: `android-app/app/build/outputs/apk/debug/app-debug.apk`
- APK Release: `android-app/app/build/outputs/apk/release/app-release.apk`
- Documentation: `android-app/README.md`

### Documentation
- README principal: [README.md](../../README.md)
- Android: [android-app/IMPLEMENTATION_GUIDE.md](../../android-app/IMPLEMENTATION_GUIDE.md)
- Intelligence Doc: [START_HERE_DOCUMENT_INTELLIGENCE.md](../../START_HERE_DOCUMENT_INTELLIGENCE.md)

