# Guide de démarrage rapide - AgentCFO

## Prérequis

- Docker et Docker Compose installés
- Clé API OpenAI (https://platform.openai.com/api-keys)
- Minimum 4GB RAM disponible

## Installation en 5 minutes

### 1. Configuration

Créez un fichier `.env` à la racine :

```bash
cp .env.example .env
```

Éditez `.env` et ajoutez votre clé OpenAI :

```bash
OPENAI_API_KEY=sk-votre-clé-ici
```

**Important** : Générez aussi un secret JWT sécurisé :

```bash
# Sur Linux/Mac
openssl rand -hex 32

# Copiez le résultat dans JWT_SECRET dans .env
```

### 2. Démarrage

```bash
# Construire et démarrer tous les services
docker-compose up -d

# Attendre que tout soit prêt (environ 1-2 minutes)
docker-compose logs -f
```

### 3. Accès à l'application

Ouvrez votre navigateur :

- **Frontend** : http://localhost:3001
- **API Documentation** : http://localhost:8001/docs

### 4. Première utilisation

1. **Créez un compte** : 
   - Allez sur http://localhost:3001
   - Cliquez sur "Inscription"
   - Remplissez le formulaire

2. **Importez un document** :
   - Allez dans "Documents"
   - Glissez-déposez une facture PDF
   - Attendez le traitement (quelques secondes)

3. **Testez les agents** :
   - **Agent Comptable** : "Analyse mes dépenses du mois dernier"
   - **Agent Juridique** : "Quelles sont mes obligations fiscales en Suisse?"

## Architecture des services

```
┌─────────────────────────────────────────────────────┐
│                    localhost:3001                    │
│                   Frontend Next.js                   │
└──────────────────────┬──────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────┐
│                    localhost:8001                    │
│                  Backend FastAPI                     │
│  ┌──────────────────────────────────────────────┐  │
│  │  • API REST                                   │  │
│  │  • Agents LLM (Comptable, Juridique)        │  │
│  │  • Service RAG                               │  │
│  │  • OCR & Extraction de texte                 │  │
│  └──────────────────────────────────────────────┘  │
└──────────────────────┬──────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────┐
│                    localhost:5433                    │
│           PostgreSQL 15 + pgvector                   │
│  ┌──────────────────────────────────────────────┐  │
│  │  • Documents & Métadonnées                    │  │
│  │  • Embeddings vectoriels                      │  │
│  │  • Transactions                               │  │
│  │  • Conversations                              │  │
│  └──────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

## Fonctionnalités disponibles

### ✅ Gestion de documents
- Upload de PDF et images (factures, contrats, courriers)
- Extraction automatique de texte (OCR pour images)
- Recherche sémantique dans vos documents

### ✅ Agent Comptable
- Analyse de vos finances
- Catégorisation automatique des dépenses
- Conseils budgétaires personnalisés
- Réponses basées sur vos documents

### ✅ Agent Juridique
- Expertise en droit suisse
- Analyse de contrats
- Conseils sur obligations légales
- Aide avec courriers administratifs

### ✅ Dashboard
- Vue d'ensemble de vos finances
- Graphiques des dépenses par catégorie
- Statistiques en temps réel

## Commandes utiles

### Gestion des services

```bash
# Voir les logs en temps réel
docker-compose logs -f

# Logs d'un service spécifique
docker-compose logs -f backend
docker-compose logs -f frontend

# Redémarrer un service
docker-compose restart backend

# Arrêter tous les services
docker-compose down

# Arrêter et supprimer les données
docker-compose down -v
```

### Développement

```bash
# Reconstruire après modification du code
docker-compose up -d --build

# Accéder au shell du backend
docker-compose exec backend bash

# Accéder à la base de données
docker-compose exec postgres psql -U agentcfo agentcfo
```

## Exemples d'utilisation

### Agent Comptable

Questions que vous pouvez poser :

- "Analyse mes factures du mois dernier"
- "Combien j'ai dépensé en alimentation ce mois-ci?"
- "Donne-moi des conseils pour réduire mes dépenses"
- "Catégorise mes dernières transactions"
- "Quelle est ma dépense moyenne par mois?"

### Agent Juridique

Questions que vous pouvez poser :

- "Quelles sont mes obligations fiscales en Suisse?"
- "Analyse ce contrat de location" (après avoir uploadé le contrat)
- "Que dois-je vérifier dans une assurance maladie?"
- "Explique-moi mes droits en tant que locataire"
- "Quel est le délai pour contester une facture?"

## Types de documents supportés

| Type | Extensions | Traitement |
|------|-----------|------------|
| PDF | `.pdf` | Extraction de texte native |
| Images | `.png`, `.jpg`, `.jpeg` | OCR avec Tesseract |
| Images TIFF | `.tiff`, `.bmp` | OCR avec Tesseract |

**Taille maximale** : 10MB par fichier

## Résolution de problèmes

### "Cannot connect to backend"

Vérifiez que tous les services sont démarrés :

```bash
docker-compose ps
```

Tous les services doivent être "Up".

### "Database connection error"

Attendez quelques secondes que PostgreSQL soit prêt :

```bash
docker-compose logs postgres
```

### "OpenAI API error"

Vérifiez votre clé API dans `.env` :

```bash
cat .env | grep OPENAI_API_KEY
```

### Le traitement des documents est lent

C'est normal pour les premières utilisations. L'extraction de texte et la génération d'embeddings prennent quelques secondes.

### Port déjà utilisé

Si les ports 3001, 8001 ou 5433 sont déjà utilisés sur votre système, modifiez-les dans `.env` :

```bash
FRONTEND_PORT=3002
BACKEND_PORT=8002
POSTGRES_PORT=5434
```

Puis redémarrez :

```bash
docker-compose down
docker-compose up -d
```

## Prochaines étapes

1. **Importez vos documents** : Commencez par vos factures récentes
2. **Explorez les agents** : Posez des questions sur vos finances
3. **Consultez le dashboard** : Suivez vos statistiques
4. **Lisez DEPLOYMENT.md** : Pour déployer en production

## Support

Pour toute question ou problème :

1. Consultez les logs : `docker-compose logs -f`
2. Vérifiez la documentation API : http://localhost:8001/docs
3. Regardez les issues GitHub du projet

## Développement

Pour contribuer au projet, consultez le fichier `CONTRIBUTING.md` (à venir).

Structure du code :
- `backend/` : API FastAPI avec agents LLM
- `frontend/` : Application Next.js avec React
- `docker-compose.yml` : Configuration des services

Bon usage d'AgentCFO! 🚀
