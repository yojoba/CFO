# AgentCFO - Résumé du projet

## 📋 Vue d'ensemble

AgentCFO est une application complète de gestion financière intelligente pour les ménages suisses, utilisant des agents LLM spécialisés pour l'analyse comptable et juridique.

## ✅ État d'implémentation

### Backend (100% complété)
- ✅ FastAPI avec configuration complète
- ✅ Base de données PostgreSQL + pgvector
- ✅ Modèles SQLAlchemy (User, Document, DocumentChunk, Transaction, Conversation)
- ✅ API REST complète (auth, documents, chat, dashboard)
- ✅ Service d'extraction de documents (PDF + OCR)
- ✅ Service d'embeddings OpenAI
- ✅ Service RAG pour recherche sémantique
- ✅ Agent comptable avec LangChain
- ✅ Agent juridique spécialisé en droit suisse
- ✅ Dockerfile et configuration Docker

### Frontend (100% complété)
- ✅ Next.js 14 avec TypeScript
- ✅ Tailwind CSS pour le design
- ✅ Pages d'authentification (login/signup)
- ✅ Dashboard avec graphiques
- ✅ Page de gestion des documents
- ✅ Upload drag & drop avec react-dropzone
- ✅ Interface chat pour agent comptable
- ✅ Interface chat pour agent juridique
- ✅ React Query pour la gestion des données
- ✅ Zustand pour l'état global
- ✅ Composants réutilisables
- ✅ Dockerfile multi-stage

### Infrastructure (100% complétée)
- ✅ Docker Compose avec 3 services
- ✅ PostgreSQL avec pgvector configuré
- ✅ Volumes pour persistance des données
- ✅ Network Docker pour communication inter-services
- ✅ Variables d'environnement configurables
- ✅ Configuration adaptée au VPS Infomaniak

## 📁 Structure du projet

```
AgentCFO/
├── backend/                      # API FastAPI
│   ├── app/
│   │   ├── agents/              # Agents LLM
│   │   │   ├── accountant_agent.py
│   │   │   └── legal_agent.py
│   │   ├── api/                 # Endpoints REST
│   │   │   ├── auth.py
│   │   │   ├── documents.py
│   │   │   ├── chat.py
│   │   │   └── dashboard.py
│   │   ├── models/              # Modèles SQLAlchemy
│   │   │   ├── user.py
│   │   │   ├── document.py
│   │   │   ├── transaction.py
│   │   │   └── conversation.py
│   │   ├── schemas/             # Schémas Pydantic
│   │   ├── services/            # Logique métier
│   │   │   ├── document_service.py
│   │   │   ├── embedding_service.py
│   │   │   └── rag_service.py
│   │   ├── config.py
│   │   └── main.py
│   ├── Dockerfile
│   └── requirements.txt
│
├── frontend/                     # Application Next.js
│   ├── src/
│   │   ├── app/                 # Pages (App Router)
│   │   │   ├── page.tsx         # Login/Signup
│   │   │   ├── dashboard/
│   │   │   ├── documents/
│   │   │   └── chat/
│   │   │       ├── accountant/
│   │   │       └── legal/
│   │   ├── components/          # Composants React
│   │   │   ├── Layout.tsx
│   │   │   ├── ChatInterface.tsx
│   │   │   └── DocumentUploader.tsx
│   │   ├── lib/                 # Utilitaires
│   │   │   ├── api.ts
│   │   │   └── utils.ts
│   │   └── stores/              # État global
│   │       └── authStore.ts
│   ├── Dockerfile
│   ├── package.json
│   └── tailwind.config.ts
│
├── docker-compose.yml            # Configuration Docker
├── .env.example                  # Variables d'environnement
├── init-db.sql                   # Init PostgreSQL
├── README.md                     # Documentation principale
├── GETTING_STARTED.md            # Guide de démarrage
└── DEPLOYMENT.md                 # Guide de déploiement

```

## 🚀 Technologies utilisées

### Backend
- **Framework**: FastAPI 0.104+
- **Base de données**: PostgreSQL 15 + pgvector
- **ORM**: SQLAlchemy 2.0
- **LLM**: OpenAI GPT-4/GPT-4-turbo
- **Embeddings**: OpenAI text-embedding-3-small
- **OCR**: Tesseract (français, allemand, anglais)
- **PDF**: PyPDF2
- **Auth**: JWT (python-jose)
- **Logs**: Loguru

### Frontend
- **Framework**: Next.js 14 (App Router)
- **Language**: TypeScript
- **Styling**: Tailwind CSS
- **State Management**: Zustand
- **Data Fetching**: React Query (TanStack Query)
- **HTTP Client**: Axios
- **File Upload**: react-dropzone
- **Charts**: Recharts
- **Icons**: Lucide React

### DevOps
- **Containerization**: Docker & Docker Compose
- **Database**: PostgreSQL avec extension pgvector
- **Reverse Proxy**: Compatible avec Nginx/Apache existant

## 🎯 Fonctionnalités principales

### 1. Gestion de documents
- Upload de PDF et images
- Extraction automatique de texte
- OCR pour documents scannés
- Stockage sécurisé avec métadonnées
- Traitement asynchrone en arrière-plan

### 2. Système RAG
- Génération d'embeddings vectoriels
- Recherche sémantique dans les documents
- Indexation avec pgvector
- Contexte pertinent pour les agents

### 3. Agent Comptable
- Analyse des factures et transactions
- Catégorisation automatique des dépenses
- Conseils budgétaires personnalisés
- Statistiques et visualisations
- Réponses basées sur vos documents réels

### 4. Agent Juridique
- Expertise en droit suisse (CO, LPD)
- Analyse de contrats
- Conseils sur obligations légales
- Aide avec courriers administratifs
- Informations sur impôts et assurances

### 5. Dashboard
- Vue d'ensemble financière
- Graphiques des dépenses par catégorie
- Statistiques en temps réel
- Actions rapides

## 🔒 Sécurité

- ✅ Authentification JWT
- ✅ Hash des mots de passe (bcrypt)
- ✅ Validation des inputs (Pydantic)
- ✅ CORS configuré
- ✅ Limite de taille des uploads (10MB)
- ✅ Variables sensibles dans .env
- ✅ SQL injection protection (SQLAlchemy ORM)

## 📊 Base de données

### Tables principales
1. **users** - Comptes utilisateurs
2. **documents** - Métadonnées des documents
3. **document_chunks** - Morceaux de texte avec embeddings
4. **transactions** - Données financières extraites
5. **conversations** - Historique des chats
6. **messages** - Messages individuels

### Extensions
- **pgvector** - Recherche de similarité vectorielle

## 🔌 API Endpoints

### Authentication
- `POST /api/auth/register` - Inscription
- `POST /api/auth/login` - Connexion
- `GET /api/auth/me` - Profil utilisateur

### Documents
- `POST /api/documents/upload` - Upload document
- `GET /api/documents/` - Liste des documents
- `GET /api/documents/{id}` - Détails document
- `DELETE /api/documents/{id}` - Supprimer document

### Chat
- `POST /api/chat/accountant` - Chat avec agent comptable
- `POST /api/chat/legal` - Chat avec agent juridique

### Dashboard
- `GET /api/dashboard/` - Statistiques financières

## 📝 Configuration requise

### Développement
- Docker Desktop ou Docker Engine + Docker Compose
- 4GB RAM minimum
- Clé API OpenAI
- Ports disponibles: 3008, 8001, 5433

### Production (VPS Infomaniak)
- VPS avec Docker installé
- Reverse proxy configuré (Nginx/Apache)
- Certificat SSL (Let's Encrypt)
- 4GB RAM minimum recommandé
- Backup automatique configuré

## 🚦 Démarrage rapide

```bash
# 1. Cloner et configurer
git clone <repo>
cd AgentCFO
cp .env.example .env
# Éditer .env avec votre clé OpenAI

# 2. Démarrer
docker-compose up -d

# 3. Accéder
# Frontend: http://localhost:3008
# API: http://localhost:8001/docs
```

## 📖 Documentation

- **README.md** - Vue d'ensemble et installation
- **GETTING_STARTED.md** - Guide de démarrage détaillé
- **DEPLOYMENT.md** - Guide de déploiement production
- **API Documentation** - http://localhost:8001/docs (Swagger UI)

## 🎨 Design & UX

- Design moderne et épuré
- Responsive mobile-first
- Interface en français
- Feedback utilisateur en temps réel
- Loading states et error handling
- Dark mode ready (structure préparée)

## 🔄 Processus de traitement des documents

```
1. Upload → 2. Extraction texte → 3. Chunking → 4. Embeddings → 5. Stockage
   ↓             ↓                    ↓              ↓               ↓
  API      PDF/OCR              ~500 tokens      OpenAI         pgvector
```

## 🌐 Architecture de déploiement (Production)

```
Internet
    ↓
[Reverse Proxy Nginx/Apache]
    ↓
    ├─→ Frontend (port 3008) → Next.js
    └─→ Backend (port 8001) → FastAPI → PostgreSQL (port 5433)
```

## 📈 Évolutions futures possibles

- [ ] Application mobile (React Native)
- [ ] Support multi-devises
- [ ] Export de rapports PDF
- [ ] Intégration bancaire (API)
- [ ] Notifications push
- [ ] Partage de documents
- [ ] Mode hors ligne
- [ ] Support d'autres LLM (Claude, Llama)
- [ ] Analyse prédictive
- [ ] Objectifs d'épargne

## 🐛 Tests

À implémenter :
- Tests unitaires backend (pytest)
- Tests d'intégration API
- Tests E2E frontend (Playwright)
- Tests de charge

## 📞 Support & Contribution

- Consultez les logs: `docker-compose logs -f`
- Documentation API: http://localhost:8001/docs
- Issues GitHub: <repo>/issues

## 📄 License

Propriétaire - Tous droits réservés

---

**Projet créé en**: Décembre 2024
**Statut**: MVP Complet ✅
**Version**: 0.1.0

