# 🌐 Configuration Google Cloud Vision API

## Étapes Exactes pour Activer

### 1. Aller sur Google Cloud Console

Ouvrir : https://console.cloud.google.com/iam-admin/serviceaccounts

### 2. Créer un Service Account

1. Cliquer **"+ CREATE SERVICE ACCOUNT"** (en haut)
2. **Service account name** : `agentcfo-vision`
3. **Description** : `Service account for AgentCFO OCR`
4. Cliquer **"CREATE AND CONTINUE"**

### 3. Donner les Permissions

1. **Select a role** → Chercher "Cloud Vision"
2. Sélectionner : **"Cloud Vision AI Service Agent"**
3. Cliquer **"CONTINUE"**
4. Cliquer **"DONE"**

### 4. Créer la Clé JSON

1. Dans la liste des service accounts, cliquer sur **`agentcfo-vision@...`**
2. Onglet **"KEYS"**
3. **"ADD KEY"** → **"Create new key"**
4. Type : **JSON**
5. Cliquer **"CREATE"**
6. **Le fichier JSON se télécharge automatiquement**

### 5. Placer le Fichier

```bash
# Le fichier téléchargé s'appelle probablement :
# your-project-name-xxxxx.json

# Le copier dans le projet
cp ~/Downloads/your-project-name-xxxxx.json /Users/tgdgral9/dev/github/AgentCFO/backend/google-cloud-credentials.json
```

### 6. Vérifier que le Fichier Existe

```bash
ls -la /Users/tgdgral9/dev/github/AgentCFO/backend/google-cloud-credentials.json
```

Vous devriez voir le fichier.

### 7. Modifier le .env

Ouvrir `/Users/tgdgral9/dev/github/AgentCFO/.env` et ajouter :

```bash
# Remplacer la ligne existante
GOOGLE_CLOUD_VISION_CREDENTIALS=/app/google-cloud-credentials.json
```

**OU** si la ligne n'existe pas, l'ajouter à la fin du fichier.

### 8. Modifier docker-compose.yml

Ajouter le volume pour monter le fichier credentials.

Ouvrir `docker-compose.yml` et dans la section `backend` → `volumes`, ajouter :

```yaml
volumes:
  - ./backend:/app
  - upload_data:/app/uploads
  - ./backend/google-cloud-credentials.json:/app/google-cloud-credentials.json:ro
```

### 9. Recréer le Backend

```bash
cd /Users/tgdgral9/dev/github/AgentCFO
docker-compose up -d backend
```

### 10. Tester

Uploader un document et vérifier les logs :

```bash
docker-compose logs -f backend | grep -i "vision\|ocr"
```

**Vous devriez voir** :
```
✅ Google Cloud Vision API initialized with service account
✅ Successfully extracted text using Google Vision API
✅ Extracted X characters using google_vision (confidence: 0.90+)
```

---

## ✅ Checklist

- [ ] Service account créé sur Google Cloud
- [ ] Clé JSON téléchargée
- [ ] Fichier copié dans `backend/google-cloud-credentials.json`
- [ ] Variable ajoutée au .env
- [ ] Volume ajouté au docker-compose.yml
- [ ] Backend recréé
- [ ] Document test uploadé
- [ ] Logs vérifiés → "google_vision" utilisé

---

## 🐛 Dépannage

### "Permission denied"
→ Le service account n'a pas les bonnes permissions
→ Ajouter role "Cloud Vision AI Service Agent"

### "File not found"
→ Vérifier le chemin du fichier
→ Vérifier que le volume est bien monté

### "Invalid JSON"
→ Le fichier est corrompu
→ Télécharger à nouveau la clé

---

**Suivez ces étapes et Google Cloud Vision sera activé !**

