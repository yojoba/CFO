# 🔧 Workflow de Développement AgentCFO

## ⚠️ RÈGLE D'OR

**APRÈS CHAQUE MODIFICATION DE CODE : RESTART OU REBUILD !**

Sans cela, vos changements ne seront **PAS visibles** dans l'application.

## 🚀 Commandes Rapides

### Backend (Python/FastAPI)

```bash
# Simple changement de code Python
docker-compose restart backend

# Changement requirements.txt
docker-compose build backend
docker-compose up -d backend

# Voir les logs
docker-compose logs -f backend
```

### Frontend (TypeScript/React/Next.js)

```bash
# Après TOUT changement (composants, types, styles)
docker-compose build frontend
docker-compose up -d frontend

# OU en une commande (recommandé)
docker-compose up -d --build frontend

# Voir les logs
docker-compose logs -f frontend
```

### Base de Données

```bash
# Appliquer migration SQL
docker-compose exec postgres psql -U agentcfo -d agentcfo < backend/migrations/xxx.sql

# Migration Alembic
docker-compose exec backend alembic upgrade head

# Accéder à PostgreSQL
docker-compose exec postgres psql -U agentcfo agentcfo
```

## 📋 Quand utiliser RESTART vs REBUILD ?

### RESTART (rapide ~5 sec)

**Utilisez pour** :
- ✅ Changement fichier Python (.py)
- ✅ Changement configuration (.env, config.py)
- ✅ Changement modèles/schémas/endpoints
- ✅ Changement agents/services

**Commande** :
```bash
docker-compose restart backend
```

### REBUILD (plus lent ~30-60 sec)

**Utilisez pour** :
- ✅ Changement requirements.txt (backend)
- ✅ Changement package.json (frontend)
- ✅ Changement Dockerfile
- ✅ **Tout changement frontend** (composants, types, styles)
- ✅ En cas de doute ou comportement bizarre
- ✅ Après avoir tiré du code depuis git

**Commandes** :
```bash
# Backend
docker-compose build backend && docker-compose up -d backend

# Frontend
docker-compose build frontend && docker-compose up -d frontend

# Tout rebuild
docker-compose build && docker-compose up -d
```

## 🔄 Workflow Complet par Type de Modification

### 1. Modifier un Endpoint API Backend

```bash
# 1. Modifier le code dans backend/app/api/
# 2. Restart
docker-compose restart backend

# 3. Vérifier les logs
docker-compose logs --tail=50 backend

# 4. Tester
curl http://localhost:8001/api/votre-endpoint
# OU
# Ouvrir http://localhost:8001/docs
```

### 2. Modifier un Composant Frontend

```bash
# 1. Modifier le code dans frontend/src/
# 2. Rebuild (OBLIGATOIRE pour frontend)
docker-compose build frontend
docker-compose up -d frontend

# 3. Vérifier les logs
docker-compose logs --tail=30 frontend

# 4. Tester dans le navigateur
# Ouvrir http://localhost:3008
# Faire un hard refresh (Cmd+Shift+R ou Ctrl+Shift+R)
```

### 3. Ajouter un Nouveau Modèle DB

```bash
# 1. Créer le modèle dans backend/app/models/
# 2. Restart
docker-compose restart backend

# 3. Créer la migration
docker-compose exec backend alembic revision --autogenerate -m "add_new_model"

# 4. Vérifier la migration générée
cat backend/alembic/versions/xxx_add_new_model.py

# 5. Appliquer la migration
docker-compose exec backend alembic upgrade head

# 6. Restart backend
docker-compose restart backend

# 7. Vérifier en DB
docker-compose exec postgres psql -U agentcfo -d agentcfo -c "\d+ nom_table"
```

### 4. Ajouter une Dépendance

**Backend** :
```bash
# 1. Ajouter dans requirements.txt
# 2. Rebuild OBLIGATOIRE
docker-compose build backend
docker-compose up -d backend

# 3. Vérifier l'installation
docker-compose exec backend pip list | grep nouvelle-lib
```

**Frontend** :
```bash
# 1. Ajouter dans package.json ou via npm
docker-compose exec frontend npm install nouvelle-lib

# 2. Rebuild
docker-compose build frontend
docker-compose up -d frontend
```

### 5. Modifier la Configuration

```bash
# 1. Modifier .env ou backend/app/config.py
# 2. Restart suffit
docker-compose restart backend
docker-compose restart frontend

# 3. Vérifier que la config est chargée
docker-compose logs backend | grep -i "config\|starting"
```

## 🐛 Debugging

### Les changements ne s'appliquent pas ?

```bash
# 1. Rebuild complet
docker-compose down
docker-compose build
docker-compose up -d

# 2. Vérifier les logs
docker-compose logs -f

# 3. Nettoyer les volumes si nécessaire (⚠️ perte de données)
docker-compose down -v
docker-compose up -d
```

### Erreur "Module not found" ?

```bash
# Backend
docker-compose build backend --no-cache
docker-compose up -d backend

# Frontend
docker-compose build frontend --no-cache
docker-compose up -d frontend
```

### Container crash au démarrage ?

```bash
# Voir les logs d'erreur
docker-compose logs backend
docker-compose logs frontend

# Redémarrer avec logs en direct
docker-compose up backend
# (sans -d pour voir les logs)
```

## ✅ Checklist Avant de Commit

- [ ] Code modifié et testé localement
- [ ] Services redémarrés/rebuild
- [ ] Logs vérifiés (pas d'erreurs)
- [ ] Application testée dans le navigateur
- [ ] Migration DB créée si nécessaire
- [ ] README/Documentation mis à jour si nécessaire
- [ ] `.env` PAS inclus dans le commit

## 🚨 Erreurs Courantes

### "column does not exist"
→ Migration DB manquante ou non appliquée
```bash
docker-compose exec backend alembic upgrade head
docker-compose restart backend
```

### "Module 'X' has no attribute 'Y'"
→ Oublié de restart/rebuild
```bash
docker-compose restart backend  # ou build si changement deps
```

### Frontend affiche l'ancienne version
→ Cache navigateur ou oublié de rebuild
```bash
docker-compose build frontend --no-cache
docker-compose up -d frontend
# Puis hard refresh navigateur (Cmd+Shift+R)
```

### "Cannot connect to database"
→ PostgreSQL pas démarré ou mauvaises credentials
```bash
docker-compose ps
docker-compose logs postgres
docker-compose restart postgres
```

## 📚 Ressources

- **Documentation complète** : Voir README.md
- **Règles Cursor** : `.cursor/rules/my-project-rules.md`
- **Commandes Docker** : `.cursor/commands/my-custom-commands.md`
- **Intelligence Documentaire** : DOCUMENT_INTELLIGENCE.md

## 🎯 Résumé Ultra-Rapide

```bash
# Modifié du Python ?
docker-compose restart backend

# Modifié du React/TypeScript ?
docker-compose build frontend && docker-compose up -d frontend

# En doute ?
docker-compose build && docker-compose up -d

# Toujours vérifier les logs après !
docker-compose logs -f
```

---

**Dernière mise à jour** : 4 décembre 2024  
**Important** : Ce workflow est ESSENTIEL pour le développement avec Docker !

