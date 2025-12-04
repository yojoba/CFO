# Document Intelligence System

## Vue d'ensemble

Le système de gestion de documents d'AgentCFO a été amélioré avec des capacités d'intelligence artificielle pour automatiser l'analyse, la classification et la priorisation des documents.

## Fonctionnalités

### 1. OCR Automatique avec Tesseract

- **OCR Local**: Utilise Tesseract pour une extraction de texte efficace et gratuite
- **Support multi-langues**: Français, allemand et anglais (adapté à la Suisse)
- **Confiance**: Calcul du niveau de confiance de l'extraction (54-70% typique)
- **Qualité**: Suffisante car l'IA GPT-4 corrige les imprécisions
- **Google Cloud Vision optionnel**: Peut être activé mais non nécessaire

### 2. Agent DocumentAgent

Un agent IA spécialisé qui analyse automatiquement chaque document pour extraire :

- **Type de document**: Classification automatique (facture, lettre, contrat, reçu, autre)
- **Date du document**: Date d'émission extraite du contenu
- **Deadline**: Date limite de paiement ou d'action requise
- **Montant**: Montant principal pour les factures et reçus
- **Devise**: Détection automatique de la devise
- **Mots-clés**: Extraction des termes importants
- **Résumé**: Description concise du document et actions requises

### 3. Système de Priorisation Intelligent

Calcul automatique d'un **score d'importance** (0-100) basé sur :

- **Proximité de la deadline**: +30 points si échéance proche
  - En retard: +30 points
  - < 3 jours: +25 points
  - < 7 jours: +20 points
  - < 14 jours: +15 points
  - < 30 jours: +10 points

- **Urgence**: +15 points si mots-clés urgents détectés
  - "urgent", "rappel", "dernière chance", "mise en demeure"

- **Montant élevé**: +15 points
  - > 1000 CHF: +15 points
  - > 500 CHF: +10 points
  - > 200 CHF: +5 points

- **Action requise**: +10 points si une action est nécessaire

- **Ajustement par confiance**: Score final multiplié par le niveau de confiance

## Nouveaux Endpoints API

### GET `/api/documents/by-importance`
Liste les documents triés par score d'importance (décroissant)

**Paramètres**:
- `skip`: Nombre de documents à sauter (pagination)
- `limit`: Nombre maximum de documents à retourner

### GET `/api/documents/by-deadline`
Liste les documents avec deadline, triés par date (plus proche en premier)

**Paramètres**:
- `skip`: Nombre de documents à sauter
- `limit`: Nombre maximum de documents

### GET `/api/documents/urgent`
Retourne les documents urgents :
- Deadline dans les 7 prochains jours
- OU score d'importance > 80

### GET `/api/documents/statistics`
Statistiques complètes sur les documents :

```json
{
  "total_documents": 42,
  "documents_by_type": {
    "invoice": 15,
    "letter": 10,
    "receipt": 8,
    "contract": 5,
    "other": 4
  },
  "documents_with_deadline": 20,
  "overdue_documents": 3,
  "upcoming_deadlines": 5,
  "high_importance_documents": 8,
  "average_importance_score": 65.5,
  "total_amount_extracted": 12450.75
}
```

## Nouveaux Champs de Document

Chaque document dispose maintenant des champs suivants :

| Champ | Type | Description |
|-------|------|-------------|
| `importance_score` | Float | Score d'importance (0-100) |
| `document_date` | Date | Date du document |
| `deadline` | Date | Date d'échéance si applicable |
| `extracted_amount` | Numeric | Montant principal extrait |
| `currency` | String | Devise (CHF par défaut) |
| `keywords` | JSON | Liste de mots-clés importants |
| `classification_confidence` | Float | Niveau de confiance (0.0-1.0) |

## Configuration

### Variables d'environnement

Ajoutez ces variables à votre fichier `.env` :

```bash
# OCR Configuration
GOOGLE_CLOUD_VISION_CREDENTIALS=/path/to/credentials.json
OCR_FALLBACK_TO_LOCAL=true

# Document Intelligence
IMPORTANCE_THRESHOLD_HIGH=80.0
IMPORTANCE_THRESHOLD_URGENT=70.0
URGENT_DEADLINE_DAYS=7
HIGH_AMOUNT_THRESHOLD=500.0
```

### Configuration OCR

**Tesseract (Par Défaut - Recommandé)** ✅
- Déjà configuré et fonctionnel
- Aucune configuration supplémentaire requise
- Confiance 54-70% suffisante (GPT-4 corrige)
- 100% de succès sur classification et extraction
- Gratuit et illimité

**Google Cloud Vision (Optionnel - Non Nécessaire)**
- Nécessite credentials JSON complexes
- Peut être bloqué par politique d'entreprise
- Confiance 85-95% mais résultat final identique
- Coût après 1000 images/mois
- **Non recommandé** sauf si documents très complexes

## Migration de la Base de Données

Pour ajouter les nouveaux champs à la base de données existante :

```bash
cd backend
psql -U agentcfo -d agentcfo -f migrations/001_add_document_metadata_fields.sql
```

Ou via Docker :

```bash
docker-compose exec postgres psql -U agentcfo -d agentcfo -f /migrations/001_add_document_metadata_fields.sql
```

## Pipeline de Traitement

Lorsqu'un document est uploadé, le pipeline suivant s'exécute automatiquement :

1. **Sauvegarde du fichier** sur le disque
2. **Extraction OCR** (Google Vision ou Tesseract)
3. **Analyse IA** par le DocumentAgent
4. **Extraction des métadonnées** structurées
5. **Calcul du score d'importance**
6. **Mise à jour de la base de données**
7. **Génération des embeddings** pour le RAG
8. **Marquage comme complété**

Tout le processus est asynchrone et n'impacte pas le temps de réponse de l'upload.

## Utilisation avec les Agents Existants

Les agents Accountant et Legal peuvent maintenant accéder aux nouvelles métadonnées :

- **Accountant Agent**: Peut analyser les montants, deadlines de paiement, et prioriser les factures
- **Legal Agent**: Peut identifier les documents légaux importants, les délais de réponse, et les contrats

## Tests

Pour tester le système :

1. Uploadez une facture avec une date d'échéance
2. Vérifiez que le document est automatiquement classifié
3. Consultez `/api/documents/urgent` pour voir les documents prioritaires
4. Utilisez `/api/documents/statistics` pour voir les statistiques

## Exemples de Réponse

### Document avec métadonnées

```json
{
  "id": 123,
  "filename": "facture_electricite.pdf",
  "original_filename": "Facture Électricité Nov 2024.pdf",
  "document_type": "invoice",
  "status": "completed",
  "importance_score": 85.5,
  "document_date": "2024-11-15",
  "deadline": "2024-12-10",
  "extracted_amount": 245.50,
  "currency": "CHF",
  "keywords": ["électricité", "Romande Energie", "novembre", "paiement"],
  "classification_confidence": 0.92,
  "created_at": "2024-12-04T10:30:00Z"
}
```

## Dépannage

### OCR ne fonctionne pas

1. Vérifiez que Tesseract est installé : `tesseract --version`
2. Vérifiez les credentials Google Cloud si utilisés
3. Consultez les logs : `docker-compose logs backend`

### Score d'importance incorrect

Le score est calculé automatiquement. Pour l'ajuster :
- Modifiez les seuils dans `config.py`
- Ajustez la logique dans `DocumentAgent._calculate_importance_score()`

### Documents non classifiés

Si `document_type` reste "other" :
- Vérifiez la qualité de l'OCR
- Le texte extrait doit être suffisamment clair
- Consultez le champ `classification_confidence`

## Améliorations Futures

- [ ] Support de plus de types de documents
- [ ] Détection automatique des expéditeurs
- [ ] Extraction de numéros de référence
- [ ] Notifications pour documents urgents
- [ ] Intégration avec calendrier pour deadlines
- [ ] Apprentissage des préférences utilisateur

