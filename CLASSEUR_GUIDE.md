# 🗄️ Guide du Classeur Virtuel 3 Niveaux

## Vue d'ensemble

Le Classeur Virtuel organise automatiquement vos documents selon une hiérarchie à 3 niveaux :

```
📅 Année (2025, 2024...)
  └─ 📂 Catégorie (Impots, Assurance, Banque...)
      └─ 📄 Type (Factures, Courrier, Contrats...)
          └─ 📑 Documents individuels
```

## 🎯 Catégories Automatiques

L'IA classifie automatiquement vos documents dans ces catégories :

| Catégorie | Icône | Exemples de Documents |
|-----------|-------|----------------------|
| **Impots** | 📋 | Déclarations fiscales, impôts cantonaux/fédéraux |
| **Poursuites** | ⚖️ | Commandements de payer, poursuites, contentieux |
| **Assurance** | 🛡️ | Assurance maladie, véhicule, habitation |
| **Banque** | 🏦 | Relevés bancaires, cartes de crédit |
| **Energie** | ⚡ | Électricité, gaz, eau |
| **Telecom** | 📱 | Téléphone, internet, TV |
| **Sante** | 🏥 | Médecin, hôpital, pharmacie, dentiste |
| **Immobilier** | 🏠 | Loyer, charges, entretien |
| **Emploi** | 💼 | Salaire, contrat de travail |
| **Non classé** | 📁 | Documents sans catégorie définie |

## 🔍 Navigation

### Étape 1 : Sélectionner l'année

Cliquez sur une carte d'année (ex: **2025**) pour voir ses catégories.

```
📅 2025 (10 documents)
  ↓ CLIC
  └─ Affiche les catégories
```

### Étape 2 : Sélectionner la catégorie

Cliquez sur une catégorie (ex: **Impots**) pour voir les types de documents.

```
📋 Impots (4 documents)
  ↓ CLIC
  └─ Factures (3)
  └─ Courrier (1)
```

### Étape 3 : Sélectionner le type

Cliquez sur un type (ex: **Factures**) pour afficher les documents.

```
📄 Factures (3 documents)
  ↓ CLIC
  └─ Liste des 3 factures
```

## 📂 Gestion des Documents "Non classé"

### Identification

Les documents non catégorisés apparaissent dans :
- **Catégorie "Non classé"** avec icône 📁
- **Fond jaune** pour attirer l'attention
- **Icône d'alerte** ⚠️ sur la catégorie

### Reclassification

1. Naviguez jusqu'à un document "Non classé"
2. Cliquez sur **"Choisir une catégorie"** (bouton jaune)
3. Sélectionnez une catégorie dans le dropdown
4. Cliquez **"Valider"** ✓
5. Le document est automatiquement déplacé

## 🔎 Recherche

### Mode Local (Par défaut)

Recherche uniquement dans le dossier actuel sélectionné.

**Exemple :** Si vous êtes dans `2025 > Impots > Factures`, la recherche ne trouvera que les factures d'impôts de 2025.

```
🔍 Recherche : "électricité"
Mode : Dans la sélection
└─ Cherche uniquement dans 2025/Impots/Factures
```

### Mode Global

Recherche dans tous vos documents, tous dossiers confondus.

**Activation :** Cliquez sur le toggle **"Recherche globale"**

```
🔍 Recherche : "électricité"
Mode : Recherche globale
└─ Cherche partout
    ├─ 2025/Energie/Factures
    ├─ 2024/Energie/Factures
    └─ Autres correspondances
```

## 🎛️ Filtres Avancés

Cliquez sur **"Filtres"** pour affiner votre recherche :

| Filtre | Description | Exemple |
|--------|-------------|---------|
| **Montant min** | Montant minimum en CHF | `> 500` |
| **Montant max** | Montant maximum en CHF | `< 1000` |
| **Importance min** | Score d'importance (0-100) | `> 80` (urgent) |
| **Date de** | Documents après cette date | `2024-01-01` |
| **Date à** | Documents avant cette date | `2024-12-31` |

### Exemples de Filtres

**Trouver toutes les factures > 1000 CHF :**
```
Montant min: 1000
```

**Documents urgents uniquement :**
```
Importance min: 80
```

**Factures du dernier trimestre :**
```
Date de: 2024-10-01
Date à: 2024-12-31
```

## 📊 Statistiques

Cliquez sur **"Statistiques"** pour voir :

- **Graphiques** : Distribution par année et catégorie
- **Totaux** : Nombre de documents par type
- **Tendances** : Évolution dans le temps

## ✅ Sélection Multiple

### Sélectionner des documents

1. Cochez les cases sur les documents souhaités
2. Cliquez **"Tout sélectionner"** pour tous les documents visibles
3. Le compteur affiche le nombre sélectionné

### Actions groupées

- **Télécharger tout** : Télécharge tous les documents sélectionnés (PDF OCR)
- **Désélectionner** : Annule la sélection

## 🎨 Codes Couleurs

### Importance

- 🔴 **Rouge** (80-100) : Urgent, action immédiate requise
- 🟡 **Jaune** (60-79) : Important, à traiter bientôt
- 🟢 **Vert** (0-59) : Normal, pas d'urgence

### Status

- **Fond jaune** : Document "Non classé"
- **Bordure bleue** : Dossier/document sélectionné
- **Fond bleu clair** : Élément survolé

## 🚀 Workflow Recommandé

### 1. Upload d'un nouveau document

```
1. Allez dans "Documents"
2. Glissez-déposez votre fichier
3. L'IA analyse et classifie automatiquement
4. Le document apparaît dans le classeur
```

### 2. Vérification mensuelle

```
1. Ouvrez le Classeur Virtuel
2. Cliquez sur "Non classé"
3. Reclassifiez les documents non catégorisés
4. Vérifiez les documents urgents (importance > 80)
```

### 3. Recherche d'un document

```
Option A - Je connais l'emplacement :
  └─ Naviguer : Année > Catégorie > Type

Option B - Je ne sais pas où il est :
  └─ Recherche globale avec mots-clés
```

### 4. Préparation déclaration d'impôts

```
1. Allez dans 2024
2. Cliquez sur "Impots"
3. Sélectionnez tous les documents
4. Téléchargez en masse
```

## 💡 Astuces

### Trouver rapidement

- Utilisez la **recherche globale** pour des mots-clés
- Filtrez par **montant** pour les grosses dépenses
- Triez par **importance** pour l'urgent

### Organisation

- Reclassifiez régulièrement les **"Non classé"**
- Vérifiez les **catégories automatiques** (l'IA peut se tromper)
- Utilisez les **statistiques** pour avoir une vue d'ensemble

### Performance

- La **recherche locale** est plus rapide
- Les **filtres** s'appliquent en temps réel
- Le **cache** accélère la navigation répétée

## ❓ FAQ

### Comment l'IA choisit la catégorie ?

L'IA analyse le contenu du document (texte OCR) et identifie :
- Les mots-clés (ex: "impôt", "assurance", "électricité")
- L'émetteur (ex: Office des impôts, Romande Energie)
- Le contexte (montants, dates, références)

### Puis-je créer mes propres catégories ?

Actuellement, les 10 catégories sont prédéfinies. Les nouvelles catégories détectées par l'IA sont automatiquement créées.

### Que se passe-t-il si je reclassifie un document ?

Le document est physiquement déplacé dans le système de fichiers :
```
Avant : /uploads/2025/Non classé/other/document.pdf
Après : /uploads/2025/Energie/invoice/document.pdf
```

### Les anciens documents sont-ils migrés ?

Oui, utilisez le script de migration :
```bash
docker-compose exec backend python scripts/migrate_existing_documents.py
```

### La recherche trouve-t-elle le contenu OCR ?

Oui ! La recherche interroge :
- Le nom du document (display_name)
- Le nom du fichier original
- Le texte extrait par OCR
- Les mots-clés

## 🔗 Documentation Complémentaire

- [HIERARCHIE_3_NIVEAUX_IMPLEMENTATION.md](HIERARCHIE_3_NIVEAUX_IMPLEMENTATION.md) - Détails techniques
- [DOCUMENT_INTELLIGENCE.md](DOCUMENT_INTELLIGENCE.md) - Intelligence documentaire
- [README.md](README.md) - Guide complet du projet

---

**Astuce finale** : Le classeur virtuel est conçu pour être intuitif. N'hésitez pas à explorer et cliquer partout - tout est réversible ! 🎯

