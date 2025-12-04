# 🚀 Démarrage ultra-rapide - AgentCFO

## En 3 commandes

```bash
# 1. Créer le fichier .env avec votre clé OpenAI
echo "OPENAI_API_KEY=sk-votre-clé-ici" > .env
cat .env.example >> .env

# 2. Démarrer tout
docker-compose up -d

# 3. Ouvrir l'application
open http://localhost:3001
```

## ✅ Vérification

```bash
# Tous les services doivent être "Up"
docker-compose ps

# Les logs doivent être sans erreur majeure
docker-compose logs -f
```

## 🎯 Première utilisation

1. **Créez un compte** sur http://localhost:3001
2. **Importez une facture** dans "Documents"
3. **Chattez avec l'agent** dans "Agent Comptable"

## 📚 Plus de détails

- Installation complète : voir `GETTING_STARTED.md`
- Déploiement production : voir `DEPLOYMENT.md`
- Résumé du projet : voir `PROJECT_SUMMARY.md`

## ⚠️ Problèmes courants

### "Cannot connect to backend"
```bash
# Attendre 30 secondes que tout démarre
docker-compose logs backend
```

### "Port already in use"
Modifiez les ports dans `.env`:
```bash
FRONTEND_PORT=3002
BACKEND_PORT=8002
POSTGRES_PORT=5434
```

## 🆘 Support

```bash
# Tout redémarrer
docker-compose restart

# Tout réinitialiser (⚠️ perte de données)
docker-compose down -v
docker-compose up -d
```

---

**Astuce** : Consultez la documentation API interactive sur http://localhost:8001/docs

