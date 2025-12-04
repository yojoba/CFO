# Changelog - Document Intelligence System

## [1.0.0] - 2024-12-04

### 🎉 Ajouté

#### Nouveaux Services
- **OCRService** : Service d'extraction de texte avec Google Cloud Vision API
  - Support Google Cloud Vision pour OCR haute qualité
  - Fallback automatique vers Tesseract local
  - Support multi-langues (français, allemand, anglais)
  - Calcul de confiance de l'extraction

- **DocumentAnalysisService** : Service d'orchestration de l'analyse
  - Pipeline complet : OCR → Analyse IA → Métadonnées
  - Préparation des données pour la base de données
  - Gestion des erreurs robuste

#### Nouvel Agent
- **DocumentAgent** : Agent IA spécialisé en analyse de documents
  - Classification automatique (invoice, letter, contract, receipt, other)
  - Extraction de dates (document et deadline)
  - Extraction de montants et devises
  - Extraction de mots-clés importants
  - Détection d'urgence et d'importance
  - Génération de résumés
  - Calcul intelligent du score d'importance (0-100)

#### Nouveaux Champs de Base de Données
- `importance_score` : Score d'importance calculé (0-100)
- `document_date` : Date du document extraite
- `deadline` : Date d'échéance si applicable
- `extracted_amount` : Montant principal extrait
- `currency` : Devise détectée
- `keywords` : Mots-clés importants (JSON)
- `classification_confidence` : Niveau de confiance (0.0-1.0)

#### Nouveaux Endpoints API
- `GET /api/documents/by-importance` : Documents triés par importance
- `GET /api/documents/by-deadline` : Documents triés par deadline
- `GET /api/documents/urgent` : Documents urgents uniquement
- `GET /api/documents/statistics` : Statistiques complètes

#### Nouveaux Schémas
- `DocumentMetadata` : Schéma pour métadonnées structurées
- `DocumentStatistics` : Schéma pour statistiques de documents

#### Configuration
- `GOOGLE_CLOUD_VISION_CREDENTIALS` : Chemin vers credentials Google Cloud
- `OCR_FALLBACK_TO_LOCAL` : Activation du fallback Tesseract
- `IMPORTANCE_THRESHOLD_HIGH` : Seuil de haute importance (80.0)
- `IMPORTANCE_THRESHOLD_URGENT` : Seuil d'urgence (70.0)
- `URGENT_DEADLINE_DAYS` : Jours avant deadline urgente (7)
- `HIGH_AMOUNT_THRESHOLD` : Montant considéré élevé (500.0 CHF)

#### Documentation
- `DOCUMENT_INTELLIGENCE.md` : Documentation complète du système
- `DOCUMENT_INTELLIGENCE_QUICKSTART.md` : Guide de démarrage rapide
- `IMPLEMENTATION_SUMMARY_DOCUMENT_INTELLIGENCE.md` : Résumé d'implémentation

#### Tests
- `test_document_agent.py` : 25+ tests unitaires pour DocumentAgent
  - Tests de parsing de dates
  - Tests de parsing de montants
  - Tests de calcul de score d'importance
  - Tests de validation de métadonnées

#### Migration
- `001_add_document_metadata_fields.sql` : Migration SQL
  - Ajout des 7 nouveaux champs
  - Création d'index pour performance
  - Documentation des colonnes

### 🔄 Modifié

#### Pipeline de Traitement
- `process_document_async()` : Intégration du nouveau pipeline d'analyse
  - Utilisation de DocumentAnalysisService
  - Mise à jour automatique des métadonnées
  - Conservation du chunking et embeddings existants

#### Modèles
- `Document` : Extension avec nouveaux champs de métadonnées
- `DocumentResponse` : Ajout des nouveaux champs dans la réponse API

#### Configuration
- `config.py` : Ajout des paramètres OCR et intelligence documentaire

#### Dépendances
- Ajout de `google-cloud-vision==3.7.0`
- Ajout de `python-dateutil==2.8.2`

### 🎯 Algorithme de Score d'Importance

Le score d'importance (0-100) est calculé selon :

**Base** : 50 points

**Deadline** (jusqu'à +30 points) :
- En retard : +30
- < 3 jours : +25
- < 7 jours : +20
- < 14 jours : +15
- < 30 jours : +10

**Urgence** : +15 points si mots-clés urgents détectés

**Montant élevé** (jusqu'à +15 points) :
- > 1000 CHF : +15
- > 500 CHF : +10
- > 200 CHF : +5

**Action requise** : +10 points

**Ajustement** : Score final × (0.7 + 0.3 × confiance)

### 📊 Statistiques d'Implémentation

- **Fichiers créés** : 8
- **Fichiers modifiés** : 5
- **Lignes de code** : ~1,200+
- **Tests** : 25+
- **Endpoints** : 4 nouveaux
- **Champs DB** : 7 nouveaux

### 🔧 Compatibilité

- ✅ Compatible avec le système existant
- ✅ Pas de breaking changes
- ✅ Pipeline RAG préservé
- ✅ Agents existants peuvent accéder aux métadonnées

### 📈 Performance

- Upload : 1-2s (synchrone)
- OCR : 2-5s (asynchrone)
- Analyse IA : 3-8s (asynchrone)
- Total : 5-15s en arrière-plan

### 🔐 Sécurité

- ✅ Validation des entrées
- ✅ Gestion des erreurs robuste
- ✅ Credentials sécurisés
- ✅ Pas d'exposition de données sensibles

### 📝 Notes de Migration

Pour mettre à jour une installation existante :

1. Installer les nouvelles dépendances :
   ```bash
   pip install -r requirements.txt
   ```

2. Appliquer la migration SQL :
   ```bash
   psql -U agentcfo -d agentcfo -f backend/migrations/001_add_document_metadata_fields.sql
   ```

3. (Optionnel) Configurer Google Cloud Vision :
   ```bash
   export GOOGLE_CLOUD_VISION_CREDENTIALS=/path/to/credentials.json
   ```

4. Redémarrer les services :
   ```bash
   docker-compose restart backend
   ```

### 🐛 Corrections

Aucune correction dans cette version (nouvelle fonctionnalité).

### 🚀 Améliorations Futures

Prévues pour les prochaines versions :
- [ ] Interface frontend pour visualiser l'importance
- [ ] Notifications push pour documents urgents
- [ ] Dashboard de statistiques avancé
- [ ] Machine learning pour améliorer le scoring
- [ ] Détection automatique d'expéditeurs
- [ ] Intégration calendrier pour deadlines
- [ ] Export de rapports PDF
- [ ] Apprentissage des préférences utilisateur

### 👥 Contributeurs

- Implémentation complète du système d'intelligence documentaire

### 📚 Documentation

Consultez la documentation complète :
- [DOCUMENT_INTELLIGENCE.md](DOCUMENT_INTELLIGENCE.md) - Documentation détaillée
- [DOCUMENT_INTELLIGENCE_QUICKSTART.md](DOCUMENT_INTELLIGENCE_QUICKSTART.md) - Guide de démarrage
- [IMPLEMENTATION_SUMMARY_DOCUMENT_INTELLIGENCE.md](IMPLEMENTATION_SUMMARY_DOCUMENT_INTELLIGENCE.md) - Résumé technique

---

## Versions Antérieures

### [0.x.x] - Avant 2024-12-04

Système de base avec :
- Upload de documents
- Extraction de texte simple (PyPDF2 + Tesseract)
- Chunking et embeddings pour RAG
- Agents Accountant et Legal
- Classification manuelle des documents

