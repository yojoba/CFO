# AgentCFO

Application de gestion financière intelligente pour les ménages suisses, avec agents LLM spécialisés en comptabilité et droit suisse.

## Architecture

- **Backend**: Python 3.11 + FastAPI + LangChain
- **Frontend Web**: React + Next.js 14 + TypeScript
- **Mobile Android**: Kotlin + Jetpack Compose (MVP natif)
- **Database**: PostgreSQL 15 + pgvector pour le RAG
- **Déploiement**: Docker + Docker Compose

## Fonctionnalités principales

### 🤖 Agents IA Spécialisés
- **Agent Comptable** : Analyse financière, budgets, catégorisation des dépenses
- **Agent Juridique** : Droit suisse (CO, LPD), contrats, obligations légales
- **Agent Documentaire** : Analyse et classification automatique des documents

### 📄 Intelligence Documentaire
- **OCR Automatique** : Tesseract local (précis et efficace) + Google Cloud Vision optionnel
- **Classification automatique** : Factures, lettres, contrats, reçus
- **Extraction de métadonnées** : Dates, deadlines, montants, devises
- **Score d'importance** : Calcul intelligent (0-100) basé sur urgence et impact
- **Priorisation automatique** : Identification des documents urgents
- **Détection de deadlines** : Alertes pour échéances proches

### 🗄️ Classeur Virtuel (Organisation 3 Niveaux)
- **Hiérarchie intelligente** : Documents classés automatiquement par Année > Catégorie > Type
  - **Année** : 2025, 2024, etc.
  - **Catégorie** : Impots 📋, Poursuites ⚖️, Assurance 🛡️, Banque 🏦, Energie ⚡, etc.
  - **Type** : Factures, Courrier, Contrats, Reçus
- **Catégorisation automatique par IA** : L'agent documentaire attribue automatiquement la bonne catégorie
- **Gestion "Non classé"** : Documents sans catégorie affichés séparément avec possibilité de reclassement
- **PDFs Searchable** : Tous les documents convertis en PDF avec couche OCR intégrée
- **Double archivage** : Fichier original + version OCR pour sécurité maximale
- **Navigation intuitive** : Interface type classeur physique avec arborescence à 3 niveaux
- **Recherche duale** : Recherche locale (dans un dossier) ou globale (tous les documents)
- **Filtres avancés** : Par montant, importance, dates
- **Impression facile** : Accès direct aux documents pour réimpression ou archivage
- **Structure organisée** : `/uploads/{année}/{catégorie}/{type}/{document}.pdf`
- **Actions en masse** : Sélection multiple et téléchargement groupé

### 📸 Recadrage Automatique (NOUVEAU ✨)
- **Détection intelligente** : Détecte automatiquement les contours du document dans l'image
- **Recadrage auto** : Supprime les bordures et arrière-plans pour ne garder que le document
- **Redressement (deskew)** : Corrige automatiquement les documents photographiés de travers
- **Amélioration d'image** : Optimise le contraste et réduit le bruit pour un meilleur OCR
- **Pipeline avant OCR** : Appliqué automatiquement avant la reconnaissance de texte
- **Qualité optimale** : PDFs finaux propres, bien cadrés et faciles à lire
- **Photos smartphone** : Parfait pour les documents photographiés avec un téléphone portable

### 💾 Système RAG
- Recherche sémantique avancée dans les documents
- Embeddings avec OpenAI text-embedding-3-small
- Base vectorielle PostgreSQL + pgvector

### 📊 Dashboard et Suivi
- Vue d'ensemble des finances
- Statistiques sur les documents
- Suivi des deadlines et urgences

## Installation

### Prérequis

- Docker et Docker Compose
- Clé API OpenAI

### Configuration

1. Copier le fichier d'environnement:
```bash
cp .env.example .env
```

2. Éditer `.env` et ajouter votre clé API OpenAI:
```
OPENAI_API_KEY=sk-votre-clé-ici
```

3. Générer un secret JWT sécurisé:
```bash
openssl rand -hex 32
```
Mettre ce secret dans `JWT_SECRET` dans `.env`.

4. **(Optionnel - Non Requis)** Configuration OCR avancée:
```
# Le système utilise Tesseract par défaut (déjà configuré et fonctionnel)
# Google Cloud Vision est optionnel et nécessite des credentials JSON
# GOOGLE_CLOUD_VISION_CREDENTIALS=/path/to/credentials.json
```
> **Note**: Tesseract fonctionne parfaitement (confiance 54-70%) et l'IA GPT-4 compense les petites imprécisions. Google Cloud Vision est optionnel et offre une confiance légèrement supérieure (85-95%) mais le résultat final est quasi identique.

### Démarrage

```bash
# Démarrer les services
docker-compose up -d

# Appliquer les migrations (première installation)
docker-compose exec postgres psql -U agentcfo -d agentcfo -f /app/backend/migrations/001_add_document_metadata_fields.sql
docker-compose exec postgres psql -U agentcfo -d agentcfo -f /app/backend/migrations/002_add_display_name.sql
docker-compose exec postgres psql -U agentcfo -d agentcfo -f /app/backend/migrations/003_add_duplicate_detection.sql
docker-compose exec postgres psql -U agentcfo -d agentcfo -f /app/backend/migrations/004_add_filing_cabinet_fields.sql
```

L'application sera accessible sur:
- Frontend Web: http://localhost:3008
- Backend API: http://localhost:8001
- Documentation API: http://localhost:8001/docs
- Application Android: Voir section [Application Mobile](#application-mobile-android-)

### Endpoints API

**Intelligence Documentaire** :
- `GET /api/documents/by-importance` - Documents triés par importance
- `GET /api/documents/by-deadline` - Documents triés par deadline
- `GET /api/documents/urgent` - Documents urgents uniquement (deadline < 7j ou score > 80)
- `GET /api/documents/statistics` - Statistiques complètes sur vos documents

**Classeur Virtuel Hiérarchique** :
- `GET /api/documents/filing-cabinet/hierarchical-overview` - Vue hiérarchique complète (Année > Catégorie > Type)
- `GET /api/documents/filing-cabinet/overview` - Vue d'ensemble classique (rétrocompatibilité)
- `GET /api/documents/filing-cabinet/years` - Liste des années avec documents
- `GET /api/documents/filing-cabinet/{year}` - Statistiques pour une année
- `GET /api/documents/filing-cabinet/{year}/categories` - Catégories disponibles pour une année
- `GET /api/documents/filing-cabinet/{year}/{category}/{type}` - Documents par année/catégorie/type
- `GET /api/documents/filing-cabinet/{year}/{type}` - Documents par année/type (ancien format, rétrocompatible)
- `GET /api/documents/categories` - Liste de toutes les catégories de l'utilisateur
- `GET /api/documents/search?q=query` - Recherche globale dans les documents
- `PATCH /api/documents/{id}` - Mise à jour document (incluant changement de catégorie)
- `GET /api/documents/{id}/download/original` - Télécharger le fichier original
- `GET /api/documents/{id}/download/ocr-pdf` - Télécharger le PDF searchable
- `GET /api/documents/{id}/preview` - Prévisualiser le document

### Migration des Documents Existants

Si vous avez déjà des documents uploadés avant la mise à jour du classeur virtuel, utilisez le script de migration :

```bash
# Dry-run pour voir ce qui sera fait (recommandé d'abord)
docker-compose exec backend python scripts/migrate_existing_documents.py --dry-run

# Migration réelle
docker-compose exec backend python scripts/migrate_existing_documents.py

# Limiter à 10 documents pour tester
docker-compose exec backend python scripts/migrate_existing_documents.py --limit 10

# Migrer uniquement les documents d'un utilisateur spécifique
docker-compose exec backend python scripts/migrate_existing_documents.py --user-id 1
```

Le script va :
1. Créer des PDFs searchable avec OCR pour tous les documents
2. Réorganiser les fichiers dans la structure `/uploads/{année}/{catégorie}/{type}/`
3. Mettre à jour la base de données avec les nouveaux chemins
4. Attribuer automatiquement les catégories via l'agent documentaire

### Configuration du Classeur Virtuel

Variables d'environnement optionnelles dans `.env` :

```bash
# Qualité des PDFs OCR (low, medium, high)
OCR_PDF_QUALITY=high

# Conserver les fichiers originaux (true/false)
KEEP_ORIGINAL_FILES=true

# Source de l'année pour le classement (document_date ou upload_date)
FILING_CABINET_YEAR_SOURCE=document_date
```

### Configuration du Recadrage Automatique

Variables d'environnement pour le prétraitement d'images dans `.env` :

```bash
# Activer le recadrage automatique (true/false)
ENABLE_AUTO_CROP=true

# Activer le redressement automatique (true/false)
ENABLE_DESKEW=true

# Activer l'amélioration du contraste (true/false)
ENABLE_CONTRAST_ENHANCEMENT=true

# Activer la réduction du bruit (true/false)
ENABLE_NOISE_REDUCTION=true

# Surface minimale du document (ratio de l'image, 0.1 = 10%)
MIN_DOCUMENT_AREA_RATIO=0.1

# Angle minimum pour déclencher le redressement (en degrés)
DESKEW_ANGLE_THRESHOLD=0.5
```

**Notes importantes** :
- Le prétraitement ajoute ~1-2 secondes par document
- Fonctionne mieux sur documents avec bordures claires (papier blanc sur fond foncé)
- Si la détection échoue, l'image originale est utilisée (pas de perte de données)
- Particulièrement utile pour photos prises au smartphone

### Développement

**Après chaque modification de code, TOUJOURS redémarrer/rebuild** :

```bash
# Backend (changement Python)
docker-compose restart backend

# Frontend (changement React/TypeScript)
docker-compose build frontend && docker-compose up -d frontend
# OU
docker-compose up -d --build frontend

# Voir les logs
docker-compose logs -f backend
docker-compose logs -f frontend
```

**Commandes utiles** :

```bash
# Voir les logs en temps réel
docker-compose logs -f

# Arrêter les services
docker-compose down

# Rebuild complet après changement dépendances
docker-compose build
docker-compose up -d

# Réinitialiser tout (⚠️ supprime les données)
docker-compose down -v
```

**⚠️ IMPORTANT** : Sans restart/rebuild, vos changements ne seront PAS visibles !

**📝 Note** : Le frontend utilise maintenant le port **3008** au lieu de 3001.

## Structure du projet

```
AgentCFO/
├── backend/              # API FastAPI
│   ├── app/
│   │   ├── agents/      # Agents LLM
│   │   │   ├── accountant_agent.py
│   │   │   ├── legal_agent.py
│   │   │   └── document_agent.py      # 🆕 Analyse documentaire
│   │   ├── api/         # Endpoints REST
│   │   ├── models/      # Modèles SQLAlchemy
│   │   └── services/    # Logique métier
│   │       ├── ocr_service.py         # 🆕 OCR cloud/local
│   │       ├── document_analysis_service.py  # 🆕 Pipeline analyse
│   │       └── ...
│   ├── migrations/      # 🆕 Migrations SQL
│   ├── tests/          # Tests unitaires
│   └── Dockerfile
├── frontend/            # Application Web Next.js
│   ├── src/
│   │   ├── app/        # Pages et routes
│   │   └── components/ # Composants React
│   └── Dockerfile
├── android-app/         # 📱 Application Android Native (NOUVEAU)
│   ├── app/
│   │   └── src/main/
│   │       ├── java/com/agentcfo/
│   │       │   ├── MainActivity.kt
│   │       │   ├── network/       # API Retrofit
│   │       │   ├── auth/          # JWT + Biométrie
│   │       │   ├── viewmodel/     # MVVM ViewModels
│   │       │   ├── ui/            # Jetpack Compose UI
│   │       │   └── utils/         # Utilitaires
│   │       └── res/               # Ressources Android
│   └── build.gradle.kts
└── docker-compose.yml
```

## Sécurité

- Ne jamais committer le fichier `.env`
- Changer les mots de passe par défaut en production
- Utiliser des secrets forts pour JWT
- Limiter la taille des uploads

## 📚 Documentation Intelligence Documentaire

Le système d'intelligence documentaire dispose d'une documentation complète :

| Document | Description |
|----------|-------------|
| [START_HERE_DOCUMENT_INTELLIGENCE.md](START_HERE_DOCUMENT_INTELLIGENCE.md) | 👈 **Commencez ici** - Point de départ |
| [DOCUMENT_INTELLIGENCE_QUICKSTART.md](DOCUMENT_INTELLIGENCE_QUICKSTART.md) | Guide de démarrage rapide |
| [DOCUMENT_INTELLIGENCE.md](DOCUMENT_INTELLIGENCE.md) | Documentation technique complète |
| [HIERARCHIE_3_NIVEAUX_IMPLEMENTATION.md](HIERARCHIE_3_NIVEAUX_IMPLEMENTATION.md) | 🆕 Classeur hiérarchique à 3 niveaux |
| [FILES_CHANGED.md](FILES_CHANGED.md) | Liste des fichiers créés/modifiés |

### Fonctionnement Rapide

Lorsque vous uploadez un document :

1. **OCR automatique** - Extraction du texte (Google Vision ou Tesseract)
2. **Analyse IA** - Classification et extraction de métadonnées
3. **Score d'importance** - Calcul automatique (0-100) basé sur :
   - Proximité de la deadline (+30 points max)
   - Mots-clés urgents (+15 points)
   - Montant élevé (+15 points)
   - Action requise (+10 points)
4. **Stockage enrichi** - Document avec toutes ses métadonnées

### Exemple de Résultat

```json
{
  "document_type": "invoice",
  "category": "Energie",
  "importance_score": 85.5,
  "deadline": "2024-12-10",
  "extracted_amount": 245.50,
  "currency": "CHF",
  "keywords": ["électricité", "paiement"],
  "storage_year": 2024
}
```

Le document sera automatiquement classé dans : **2024 / Energie / Factures**

## 📱 Application Mobile Android (NOUVEAU)

Une application Android native complète est disponible pour gérer vos documents en mobilité.

### Fonctionnalités

- ✅ **Authentification sécurisée** : JWT + Biométrie (empreinte/face)
- ✅ **Upload de documents** : Caméra ou galerie
- ✅ **Capture photo CameraX** : Prendre des photos de documents directement
- ✅ **Liste des documents** : Avec métadonnées enrichies (importance, deadline, montant)
- ✅ **Détails complets** : Visualisation et gestion des documents
- ✅ **Compression automatique** : Optimisation des images avant upload
- ✅ **Design Material 3** : Interface moderne et intuitive

### Technologies

- **Langage** : Kotlin
- **UI** : Jetpack Compose
- **Architecture** : MVVM (Model-View-ViewModel)
- **API** : Retrofit + OkHttp
- **Async** : Coroutines + Flow
- **Camera** : CameraX
- **Security** : Biometric API + DataStore

### Installation et Démarrage

#### Prérequis
- Android Studio Ladybug (2024.2.1+)
- JDK 11+
- Android SDK 35
- Backend démarré sur `localhost:8001`

#### Configuration

```bash
# 1. Ouvrir le projet dans Android Studio
cd android-app/
# Ouvrir avec Android Studio

# 2. Synchroniser les dépendances Gradle (automatique)

# 3. S'assurer que le backend est démarré
cd ../
docker-compose up -d

# 4. Configurer l'URL backend (si nécessaire)
# Éditer android-app/app/build.gradle.kts
# Dev: http://10.0.2.2:8001 (émulateur → localhost)
# Prod: https://cfo.flowbiz.ai
```

#### Build et Exécution

**Via Android Studio** :
1. Connecter un appareil ou lancer un émulateur (API 24+)
2. Cliquer sur Run ▶️

**Via ligne de commande** :
```bash
cd android-app

# Build debug APK
./gradlew assembleDebug

# Installer sur appareil connecté
./gradlew installDebug

# Build release APK (production)
./gradlew assembleRelease
```

Les APKs générés se trouvent dans :
- **Debug** : `app/build/outputs/apk/debug/app-debug.apk`
- **Release** : `app/build/outputs/apk/release/app-release.apk`

### Documentation Complète

- **[android-app/README.md](android-app/README.md)** - Guide d'installation et usage
- **[android-app/IMPLEMENTATION_GUIDE.md](android-app/IMPLEMENTATION_GUIDE.md)** - Documentation technique détaillée
- **[ANDROID_APP_COMPLETE.md](ANDROID_APP_COMPLETE.md)** - Résumé de l'implémentation

### Architecture Android

```
android-app/app/src/main/java/com/agentcfo/
├── MainActivity.kt                 # Point d'entrée + Navigation
├── network/                        # API Retrofit
│   ├── AgentCfoApiService.kt      # Interface API
│   ├── ApiModels.kt               # Modèles de données
│   └── RetrofitClient.kt          # Configuration HTTP
├── auth/                          # Authentification
│   ├── TokenManager.kt            # Gestion JWT (DataStore)
│   ├── BiometricAuthManager.kt    # Authentification biométrique
│   └── AuthenticationState.kt     # État d'authentification
├── data/                          # Repositories
│   ├── AuthRepository.kt
│   ├── DocumentRepository.kt
│   └── ChatRepository.kt
├── viewmodel/                     # ViewModels MVVM
│   ├── AuthViewModel.kt
│   ├── DocumentViewModel.kt
│   └── ChatViewModel.kt
├── ui/                            # Interface Jetpack Compose
│   ├── auth/                      # Écrans d'authentification
│   ├── documents/                 # Écrans de gestion documents
│   ├── camera/                    # Écran de capture photo
│   ├── theme/                     # Thème Material 3
│   └── BiometricLockScreen.kt    # Verrouillage biométrique
└── utils/                         # Utilitaires
    ├── FileUtils.kt               # Gestion fichiers
    └── PermissionHandler.kt       # Permissions Compose
```

### Flux de Travail Android

1. **Inscription/Connexion** → Authentification JWT
2. **Verrouillage biométrique** → Sécurité supplémentaire
3. **Capture photo** → CameraX pour documents
4. **Upload** → Compression + envoi au backend
5. **Liste** → Affichage avec métadonnées
6. **Détail** → Consultation complète du document

### Notes Importantes

- **Émulateur** : Utiliser `10.0.2.2` pour accéder à `localhost` du host
- **Appareil physique** : Utiliser l'IP locale de votre machine (ex: `192.168.1.X`)
- **Permissions** : Caméra et stockage demandées au runtime
- **Biométrie** : Optionnelle, l'app fonctionne sans si non disponible

## Configuration Cursor

Ce projet inclut des fichiers de configuration optimisés pour Cursor IDE:

### Fichiers disponibles

- **`.cursor/rules/my-project-rules.md`** - Règles et conventions du projet
  - Architecture et stack technique
  - Conventions de code Python et TypeScript
  - Règles de sécurité, base de données, API
  - Configuration des agents LLM
  - Workflows de développement

- **`.cursor/commands/my-custom-commands.md`** - Commandes personnalisées
  - 50+ commandes Docker courantes
  - Gestion PostgreSQL et migrations
  - Tests et debugging
  - Scripts de backup et monitoring
  - Quick start et maintenance

### Utilisation dans Cursor

1. Tapez `@` dans le chat pour référencer les fichiers de règles
2. L'AI comprendra automatiquement le contexte du projet
3. Utilisez les commandes comme référence rapide

### Exemples de prompts

```
"Crée un nouveau endpoint selon nos règles du projet"
"Debug l'authentification avec nos commandes"
"Ajoute un modèle DB en suivant nos conventions"
```

## License

Propriétaire
