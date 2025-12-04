# 🔍 Détection de Duplicates - Documentation

## Vue d'ensemble

Le système AgentCFO détecte automatiquement les documents en double (duplicates) lors de l'upload, utilisant 3 stratégies complémentaires.

## 🎯 Stratégies de Détection

### 1. **Hash Exact du Fichier** (100% de similarité)
- Calcule le SHA256 du contenu du fichier
- Détecte les fichiers **exactement identiques**
- Même si le nom est différent
- **Le plus rapide et le plus fiable**

### 2. **Similarité du Contenu** (>85% de similarité)
- Utilise les embeddings vectoriels (pgvector)
- Détecte les documents au **contenu très similaire**
- Même si le fichier est légèrement différent (scan vs PDF original)
- Compare le texte extrait par OCR

### 3. **Métadonnées Similaires** (>85% de similarité)
- Compare : montant + type + date (±30 jours)
- Détecte les **mêmes factures/reçus**
- Exemple : même facture uploadée 2 fois
- Utile pour documents similaires mais pas identiques

## 🔄 Processus Automatique

Lors de l'upload d'un document :

```
1. Upload fichier
2. Calcul hash SHA256
3. Extraction OCR
4. Analyse IA
5. Génération embeddings
6. ⚠️ DÉTECTION DUPLICATES
   ├─ Check hash exact
   ├─ Check similarité contenu
   └─ Check métadonnées
7. Marquage si duplicate détecté
8. Sauvegarde en base
```

## 📊 Champs de Base de Données

| Champ | Type | Description |
|-------|------|-------------|
| `file_hash` | String | Hash SHA256 du contenu du fichier |
| `is_duplicate` | Boolean | True si document est un doublon |
| `duplicate_of_id` | Integer | ID du document original |
| `similarity_score` | Float | Score de similarité (0.0-1.0) |

## 🌐 Nouveaux Endpoints API

### GET `/api/documents/duplicates`
Retourne tous les documents marqués comme duplicates

**Réponse** :
```json
[
  {
    "id": 10,
    "display_name": "Facture Électricité Nov 2024",
    "is_duplicate": true,
    "duplicate_of_id": 5,
    "similarity_score": 0.98,
    "created_at": "2024-12-04T14:00:00Z"
  }
]
```

## 🎨 Affichage Frontend

### Badge Duplicate
Les documents duplicates sont affichés avec :
- 🟡 **Fond jaune clair** pour toute la ligne
- 📋 **Icône "Copy"** avec message
- **Pourcentage de similarité** : "Doublon détecté (98% similaire)"

### Exemple d'Affichage
```
📄 Facture Électricité Romande - Nov 2024
   📋 Doublon détecté (98% similaire)
   WhatsApp Image 2025-12-03 at 15.27.40.jpeg
```

## 🧪 Tests et Exemples

### Test 1 : Upload du Même Fichier
```bash
# Upload document.pdf
# Upload document.pdf (même fichier, même nom)
# Résultat : ⚠️ Duplicate détecté (100% - hash exact)
```

### Test 2 : Upload Scan et PDF Original
```bash
# Upload facture_scan.jpg (scan d'une facture)
# Upload facture.pdf (PDF original de la même facture)
# Résultat : ⚠️ Duplicate détecté (95% - similarité contenu)
```

### Test 3 : Même Facture, Noms Différents
```bash
# Upload "Invoice_Nov.pdf" (facture 245 CHF du 15/11)
# Upload "Facture_Novembre.pdf" (même facture, même montant)
# Résultat : ⚠️ Duplicate détecté (90% - métadonnées similaires)
```

## ⚙️ Configuration

### Seuils de Détection

Les seuils par défaut sont définis dans `DuplicateDetectionService` :

```python
exact_match_threshold = 0.99          # Hash exact
high_similarity_threshold = 0.95      # Très similaire
moderate_similarity_threshold = 0.85  # Probablement duplicate
metadata_match_window_days = 30       # ±30 jours pour dates
```

### Ajuster les Seuils

Modifiez dans `backend/app/services/duplicate_detection_service.py` :

```python
def __init__(self):
    self.moderate_similarity_threshold = 0.80  # Plus permissif
    self.metadata_match_window_days = 60       # Fenêtre plus large
```

## 🔧 Utilisation

### Automatique
La détection est **automatique** pour chaque upload. Aucune action requise.

### Manuel - Vérifier les Duplicates
```bash
# Via API
curl http://localhost:8001/api/documents/duplicates \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Supprimer les Duplicates
```bash
# 1. Identifier les duplicates
GET /api/documents/duplicates

# 2. Supprimer ceux qui ne sont pas nécessaires
DELETE /api/documents/{duplicate_id}
```

## 📈 Statistiques

Le endpoint `/api/documents/statistics` inclut maintenant :

```json
{
  "total_documents": 10,
  "duplicate_documents": 2,
  ...
}
```

## 🚨 Comportement

### Quand un Duplicate est Détecté

1. ✅ Le document est **quand même sauvegardé**
2. ⚠️ Il est **marqué comme duplicate**
3. 🔗 **Lien vers l'original** (duplicate_of_id)
4. 📊 **Score de similarité** enregistré
5. 🎨 **Affichage visuel** dans le frontend (fond jaune)

### L'Utilisateur Peut

- ✅ **Garder** le duplicate (si nécessaire)
- ✅ **Supprimer** le duplicate
- ✅ **Voir l'original** via le lien

## 💡 Cas d'Usage

### Cas 1 : Même Facture Uploadée 2 Fois
```
Upload 1 : "facture_nov.pdf" → OK
Upload 2 : "facture_nov.pdf" → ⚠️ Duplicate (hash 100%)
Action : Supprimer le duplicate
```

### Cas 2 : Scan et PDF Original
```
Upload 1 : "facture_scan.jpg" → OK
Upload 2 : "facture_original.pdf" → ⚠️ Duplicate (contenu 96%)
Action : Garder le PDF (meilleure qualité), supprimer le scan
```

### Cas 3 : Documents Similaires mais Différents
```
Upload 1 : "Facture Électricité Nov 2024" → OK
Upload 2 : "Facture Électricité Dec 2024" → OK (dates différentes)
Résultat : Pas de duplicate (dates hors fenêtre de 30j)
```

## 🔍 Debugging

### Voir les Logs de Détection
```bash
docker-compose logs backend | grep -i "duplicate"
```

Exemple de logs :
```
⚠️ Duplicate detected for document 10: original=5, similarity=0.98, method=exact_hash
✅ No duplicate found for document 11
```

### Vérifier en Base de Données
```sql
-- Voir tous les duplicates
SELECT 
    id, 
    display_name, 
    is_duplicate, 
    duplicate_of_id, 
    similarity_score 
FROM documents 
WHERE is_duplicate = true;

-- Voir les documents avec leurs duplicates
SELECT 
    d1.id as original_id,
    d1.display_name as original_name,
    d2.id as duplicate_id,
    d2.display_name as duplicate_name,
    d2.similarity_score
FROM documents d1
JOIN documents d2 ON d1.id = d2.duplicate_of_id
ORDER BY d1.id;
```

## 🎯 Avantages

- ✅ **Évite le stockage inutile** de fichiers identiques
- ✅ **Alerte visuelle** immédiate
- ✅ **Multiple stratégies** pour différents cas
- ✅ **Détection automatique** sans intervention
- ✅ **Flexible** : l'utilisateur décide de garder ou supprimer

## ⚙️ Performance

- **Hash calculation** : ~50-200ms
- **Content similarity** : ~100-500ms
- **Metadata check** : ~10-50ms
- **Total overhead** : ~160-750ms par document

Le coût est minimal et la détection se fait en arrière-plan.

## 🔄 Améliorations Futures

- [ ] Onglet "Duplicates" dans le frontend
- [ ] Action "Fusionner duplicates"
- [ ] Notification push lors de détection
- [ ] Prévention d'upload si duplicate exact
- [ ] Suggestion de suppression automatique
- [ ] ML pour améliorer la détection

## 📚 Ressources

- Service : `backend/app/services/duplicate_detection_service.py`
- Modèle : `backend/app/models/document.py` (champs duplicate)
- Migration : `backend/migrations/003_add_duplicate_detection.sql`
- Frontend : `frontend/src/components/DocumentList.tsx` (affichage)

---

**Date d'implémentation** : 4 décembre 2024  
**Version** : 1.0.0  
**Statut** : ✅ OPÉRATIONNEL

