# Règles du projet AgentCFO

## Architecture et Stack Technique

### Backend (Python/FastAPI)
- Python 3.11+ requis
- Framework: FastAPI avec Uvicorn
- Base de données: PostgreSQL 15 + pgvector
- ORM: SQLAlchemy 2.0 avec Alembic pour migrations
- LLM: OpenAI GPT-4/GPT-4-turbo via API
- Embeddings: OpenAI text-embedding-3-small

### Frontend (TypeScript/Next.js)
- Next.js 14 avec App Router
- TypeScript strict mode
- Tailwind CSS pour le styling
- React Query pour data fetching
- Zustand pour state management
- Axios pour les requêtes HTTP

### Infrastructure
- Docker Compose pour orchestration
- 3 services: backend (port 8001), frontend (port 3001), postgres (port 5433)
- Variables d'environnement dans .env (jamais committer)

## ⚠️ IMPORTANT - Workflow de Développement Docker

### Après CHAQUE modification de code

**Backend Python** :
```bash
# Simple redémarrage (changements Python uniquement)
docker-compose restart backend

# Rebuild complet (si changement requirements.txt)
docker-compose build backend
docker-compose up -d backend
```

**Frontend TypeScript/React** :
```bash
# Rebuild REQUIS après changements composants/types
docker-compose build frontend
docker-compose up -d frontend

# OU pour rebuild et démarrer en une commande
docker-compose up -d --build frontend
```

**Base de données** (migrations) :
```bash
# Appliquer une migration SQL
docker-compose exec postgres psql -U agentcfo -d agentcfo < backend/migrations/xxx.sql

# OU via Alembic
docker-compose exec backend alembic upgrade head
```

### Vérifier les changements
```bash
# Voir les logs en temps réel
docker-compose logs -f backend
docker-compose logs -f frontend

# Tester l'application
# Frontend: http://localhost:3001
# Backend API: http://localhost:8001/docs
```

### 🔴 RÈGLE ABSOLUE
**TOUJOURS redémarrer/rebuild le service après modification du code**
**TOUJOURS vérifier les logs après redémarrage**
**TOUJOURS tester l'application dans le navigateur**

Sans rebuild/restart, les changements ne seront PAS visibles !

### Quand utiliser RESTART vs REBUILD ?

**RESTART** (rapide ~5 sec) :
- ✅ Changement fichier Python (.py)
- ✅ Changement configuration (.env, config.py)
- ✅ Changement modèles/schémas/endpoints
- ❌ PAS pour changement requirements.txt

**REBUILD** (plus lent ~30-60 sec) :
- ✅ Changement requirements.txt (backend)
- ✅ Changement package.json (frontend)
- ✅ Changement Dockerfile
- ✅ Changement composants React/TypeScript (frontend)
- ✅ Changement types TypeScript (frontend)
- ✅ En cas de doute ou comportement bizarre

**Commande combinée** (recommandée pour frontend) :
```bash
docker-compose up -d --build frontend
# Rebuild ET redémarre en une commande
```

## Conventions de code

### Backend Python
```python
# Nommage
- Classes: UpperCamelCase (ex: UserService, DocumentAgent)
- Fonctions/variables: snake_case (ex: get_user_by_id, analyze_document)
- Constantes: UPPER_SNAKE_CASE (ex: MAX_UPLOAD_SIZE)

# Type hints obligatoires
def process_document(doc_id: int, user_id: int) -> Document:
    pass

# Docstrings pour fonctions publiques
def create_embedding(text: str) -> List[float]:
    """Create embedding vector for text.
    
    Args:
        text: Text to embed
        
    Returns:
        Embedding vector of 1536 dimensions
    """

# Async/await pour opérations I/O
async def analyze_document(file_path: str) -> Dict[str, Any]:
    """Analyze document with OCR and AI."""
    pass
```

### Frontend TypeScript
```typescript
// Nommage
- Composants: PascalCase (ex: DocumentUploader)
- Fonctions/variables: camelCase (ex: getUserData)
- Types/Interfaces: PascalCase (ex: UserResponse)
- Fichiers: kebab-case ou PascalCase selon type

// Props typing obligatoire
interface ChatInterfaceProps {
  agentType: 'accountant' | 'legal'
}

// Hooks à extraire si réutilisables
const useDocuments = () => {
  // Custom hook logic
}
```

## Structure des fichiers

### Backend
```
backend/app/
├── api/          # Endpoints REST (auth, documents, chat, dashboard)
├── agents/       # Agents LLM spécialisés
│   ├── accountant_agent.py   # Agent comptable
│   ├── legal_agent.py        # Agent juridique
│   └── document_agent.py     # Agent analyse documentaire (NOUVEAU)
├── models/       # Modèles SQLAlchemy
├── schemas/      # Schémas Pydantic
├── services/     # Logique métier
│   ├── ocr_service.py                # OCR cloud/local (NOUVEAU)
│   ├── document_analysis_service.py  # Pipeline analyse (NOUVEAU)
│   ├── document_service.py           # Service documents
│   ├── embedding_service.py          # Service embeddings
│   └── rag_service.py                # Service RAG
├── config.py     # Configuration centralisée
└── main.py       # Point d'entrée FastAPI
```

### Frontend
```
frontend/src/
├── app/          # Pages Next.js (App Router)
├── components/   # Composants React réutilisables
├── lib/          # Utilitaires (api, utils)
├── stores/       # State management (Zustand)
└── types/        # Types TypeScript partagés
```

## Règles de développement

### Sécurité
1. **Jamais de secrets en dur** - toujours utiliser .env
2. **Validation stricte** - Pydantic backend, validation frontend
3. **JWT pour auth** - `Authorization: Bearer <token>`
4. **SQL injection** - toujours utiliser SQLAlchemy ORM
5. **CORS** - configuré pour domaines autorisés uniquement
6. **Upload files** - valider type et taille (max 10MB)

### Base de données
1. **Migrations** - toujours créer migration Alembic pour changements schema
2. **Transactions** - utiliser `db.commit()` et `db.rollback()` correctement
3. **Indexes** - ajouter index sur colonnes fréquemment recherchées
4. **pgvector** - utiliser opérateur `<=>` pour similarité cosine

### API REST
1. **Endpoints** - suivre conventions REST (GET, POST, PUT, DELETE)
2. **Status codes** - utiliser codes HTTP appropriés
3. **Pagination** - implémenter skip/limit pour grandes listes
4. **Errors** - retourner messages d'erreur clairs et localisés (français)

### Agents LLM
1. **Prompts système** - en français, spécifiques à chaque agent
2. **Température** - 0.7 pour comptable, 0.5 pour juridique, 0.3 pour documentaire (plus précis)
3. **Context** - toujours inclure contexte RAG pertinent
4. **Tokens** - limiter réponses (max 1000-1200 tokens, 800 pour extraction documentaire)
5. **Fallback** - gérer erreurs OpenAI gracieusement
6. **Structured output** - DocumentAgent retourne JSON structuré pour parsing

### Intelligence Documentaire
1. **OCR** - Google Cloud Vision en priorité, fallback Tesseract automatique
2. **Métadonnées** - Extraire : type, dates, deadline, montant, devise, mots-clés
3. **Score importance** - Base 50 + deadline (0-30) + urgence (0-15) + montant (0-15) + action (0-10)
4. **Validation** - Toujours valider et normaliser les données extraites
5. **Async processing** - Traitement en arrière-plan pour ne pas bloquer l'upload
6. **Confidence tracking** - Tracker le niveau de confiance OCR et IA

### Frontend
1. **Client vs Server** - 'use client' pour composants interactifs
2. **Loading states** - toujours afficher état de chargement
3. **Error handling** - messages d'erreur utilisateur friendly
4. **Responsive** - mobile-first design avec Tailwind
5. **Accessibility** - labels, alt text, keyboard navigation

## Workflows courants

### Ajouter un nouveau endpoint API
1. Créer schéma Pydantic dans `backend/app/schemas/`
2. Créer endpoint dans `backend/app/api/`
3. **⚠️ RESTART backend** : `docker-compose restart backend`
4. **✅ TESTER** : http://localhost:8001/docs
5. Créer fonction API côté frontend dans composant/page
6. **⚠️ REBUILD frontend** : `docker-compose build frontend && docker-compose up -d frontend`
7. **✅ TESTER** : http://localhost:3001
8. Gérer loading et error states

### Ajouter un nouveau modèle DB
1. Créer modèle dans `backend/app/models/`
2. **⚠️ RESTART backend** : `docker-compose restart backend`
3. Créer migration: `docker-compose exec backend alembic revision --autogenerate -m "description"`
4. Appliquer migration: `docker-compose exec backend alembic upgrade head`
5. Créer schémas Pydantic correspondants
6. **⚠️ RESTART backend** : `docker-compose restart backend`
7. **✅ TESTER** : Vérifier via /docs ou psql
8. Mettre à jour services si nécessaire

### Modifier un agent LLM
1. Modifier prompt système dans `backend/app/agents/`
2. Tester comportement avec diverses questions
3. Ajuster température et max_tokens si nécessaire
4. Documenter changements dans commentaires

### Traiter un document (workflow complet)
1. Upload → `POST /api/documents/upload`
2. Sauvegarde fichier → `DocumentService.save_file()`
3. OCR → `OCRService.extract_text_from_image()` ou `extract_text_from_pdf()`
4. Analyse IA → `DocumentAgent.analyze_document()`
5. Métadonnées → `DocumentAnalysisService.prepare_database_fields()`
6. Embeddings → `EmbeddingService.create_embeddings()`
7. Finalisation → Document.status = COMPLETED

### Ajuster le scoring d'importance
1. Modifier seuils dans `backend/app/config.py`
2. Ajuster logique dans `DocumentAgent._calculate_importance_score()`
3. Tester avec divers documents
4. Vérifier cohérence via `/api/documents/statistics`

## Tests (à implémenter)

### Backend
```python
# Tests unitaires avec pytest
# Fichier: tests/test_services.py
def test_document_extraction():
    result = document_service.extract_text("test.pdf")
    assert len(result) > 0
```

### Frontend
```typescript
// Tests composants avec Jest/React Testing Library
// Fichier: __tests__/DocumentUploader.test.tsx
describe('DocumentUploader', () => {
  it('should upload file successfully', () => {
    // Test logic
  })
})
```

## Performance

### Backend
- Utiliser `async/await` pour opérations I/O
- Cacher résultats coûteux (embeddings)
- Lazy loading pour relations SQLAlchemy
- Connection pooling PostgreSQL

### Frontend
- React Query cache automatique
- Code splitting avec dynamic imports
- Image optimization avec Next.js
- Memo composants lourds avec React.memo

## Déploiement

### Développement
```bash
docker-compose up -d        # Démarrer
docker-compose logs -f      # Voir logs
docker-compose restart      # Redémarrer
docker-compose down         # Arrêter
```

### Production VPS
- Variables .env adaptées (ENVIRONMENT=production)
- Reverse proxy configuré
- SSL/TLS activé
- Backups automatiques PostgreSQL
- Monitoring des logs

## Commandes Docker utiles

```bash
# Rebuild après changement code
docker-compose up -d --build

# Accéder au shell backend
docker-compose exec backend bash

# Accéder à PostgreSQL
docker-compose exec postgres psql -U agentcfo agentcfo

# Créer migration DB
docker-compose exec backend alembic revision --autogenerate -m "description"

# Appliquer migrations
docker-compose exec backend alembic upgrade head

# Appliquer migration SQL manuelle (intelligence documentaire)
docker-compose exec postgres psql -U agentcfo -d agentcfo < backend/migrations/001_add_document_metadata_fields.sql

# Tester l'agent documentaire
docker-compose exec backend pytest tests/test_document_agent.py -v

# Vérifier Tesseract OCR
docker-compose exec backend tesseract --version
```

## Dépannage

### Backend ne démarre pas
- Vérifier logs: `docker-compose logs backend`
- Vérifier .env: `cat .env | grep OPENAI`
- Vérifier DB: `docker-compose exec postgres pg_isready`

### Frontend erreurs de compilation
- Nettoyer cache: `docker-compose exec frontend rm -rf .next`
- Réinstaller deps: dans frontend local `npm install`
- Rebuild: `docker-compose build frontend`

### Problèmes d'authentification
- Vérifier JWT_SECRET dans .env
- Vérifier token dans localStorage (DevTools)
- Vérifier logs backend pour erreurs JWT

### OCR ne fonctionne pas
- Vérifier Tesseract: `docker-compose exec backend tesseract --version`
- Vérifier Google Cloud credentials si configuré
- Consulter logs: `docker-compose logs backend | grep OCR`

### Documents non classifiés
- Vérifier OpenAI API Key dans .env
- Vérifier qualité OCR (field classification_confidence)
- Consulter logs: `docker-compose logs backend | grep "Document.*processed"`

### Score d'importance incorrect
- Vérifier seuils dans `backend/app/config.py`
- Vérifier métadonnées extraites via API
- Ajuster logique dans `DocumentAgent._calculate_importance_score()`

## Notes spécifiques au projet

### Système RAG
- Chunks de ~500 tokens avec overlap de 50
- Embeddings dimension 1536 (OpenAI)
- Recherche par similarité cosine (1 - distance)
- Seuil de similarité: 0.7 par défaut

### Agents
- **Comptable**: Analyse financière, budgets, catégorisation
- **Juridique**: Droit suisse (CO, LPD), contrats, obligations
- **Documentaire**: Classification, extraction métadonnées, scoring importance

### Documents supportés
- PDF: extraction native avec PyPDF2
- Images (PNG, JPG, TIFF): OCR avec Google Cloud Vision ou Tesseract (FR, DE, EN)
- Taille max: 10MB
- Types détectés: invoice, letter, contract, receipt, other

### Intelligence Documentaire
- **OCR**: Google Cloud Vision (optionnel) + fallback Tesseract
- **Classification**: 5 types détectés automatiquement
- **Métadonnées extraites**:
  - document_date: Date du document
  - deadline: Date d'échéance
  - extracted_amount: Montant principal
  - currency: Devise (CHF par défaut)
  - keywords: Mots-clés importants (JSON)
  - classification_confidence: Confiance 0.0-1.0
- **Score importance**: 0-100 calculé automatiquement
  - Base: 50 points
  - Deadline proche: +0 à +30 points
  - Mots urgents: +15 points
  - Montant élevé: +0 à +15 points
  - Action requise: +10 points
  - Ajusté par confiance: × (0.7 + 0.3 × confidence)
- **Endpoints spécialisés**:
  - `/api/documents/by-importance`: Tri par score
  - `/api/documents/by-deadline`: Tri par échéance
  - `/api/documents/urgent`: Deadline < 7j ou score > 80
  - `/api/documents/statistics`: Vue d'ensemble

### Droit suisse
L'agent juridique est spécialisé en:
- Code des Obligations (CO)
- Loi sur la Protection des Données (LPD)
- Droit des contrats
- Obligations fiscales et assurances

## Ressources

### Documentation Générale
- FastAPI docs: https://fastapi.tiangolo.com
- Next.js docs: https://nextjs.org/docs
- OpenAI API: https://platform.openai.com/docs
- pgvector: https://github.com/pgvector/pgvector
- SQLAlchemy: https://docs.sqlalchemy.org

### Documentation Intelligence Documentaire
- [START_HERE_DOCUMENT_INTELLIGENCE.md](../../START_HERE_DOCUMENT_INTELLIGENCE.md) - Point de départ
- [DOCUMENT_INTELLIGENCE.md](../../DOCUMENT_INTELLIGENCE.md) - Documentation complète
- [DOCUMENT_INTELLIGENCE_QUICKSTART.md](../../DOCUMENT_INTELLIGENCE_QUICKSTART.md) - Guide rapide
- [FILES_CHANGED.md](../../FILES_CHANGED.md) - Liste des modifications

### APIs Externes
- Google Cloud Vision: https://cloud.google.com/vision/docs
- Tesseract OCR: https://github.com/tesseract-ocr/tesseract

