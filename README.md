# AgentCFO

Application de gestion financière intelligente pour les ménages suisses, avec agents LLM spécialisés en comptabilité et droit suisse.

## Architecture

- **Backend**: Python 3.11 + FastAPI + LangChain
- **Frontend**: React + Next.js 14 + TypeScript
- **Database**: PostgreSQL 15 + pgvector pour le RAG
- **Déploiement**: Docker + Docker Compose

## Fonctionnalités principales

- Import et analyse de documents (factures, courriers administratifs)
- Agent comptable intelligent pour analyse financière
- Agent juridique spécialisé en droit suisse
- Système RAG pour recherche sémantique dans les documents
- Dashboard de suivi budgétaire

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

### Démarrage

```bash
docker-compose up -d
```

L'application sera accessible sur:
- Frontend: http://localhost:3001
- Backend API: http://localhost:8001
- Documentation API: http://localhost:8001/docs

### Développement

Pour voir les logs:
```bash
docker-compose logs -f
```

Pour arrêter:
```bash
docker-compose down
```

Pour tout réinitialiser (⚠️ supprime les données):
```bash
docker-compose down -v
```

## Structure du projet

```
AgentCFO/
├── backend/           # API FastAPI
│   ├── app/
│   │   ├── agents/   # Agents LLM
│   │   ├── api/      # Endpoints REST
│   │   ├── models/   # Modèles SQLAlchemy
│   │   └── services/ # Logique métier
│   └── Dockerfile
├── frontend/          # Application Next.js
│   ├── src/
│   │   ├── app/      # Pages et routes
│   │   └── components/ # Composants React
│   └── Dockerfile
└── docker-compose.yml
```

## Sécurité

- Ne jamais committer le fichier `.env`
- Changer les mots de passe par défaut en production
- Utiliser des secrets forts pour JWT
- Limiter la taille des uploads

## License

Propriétaire
