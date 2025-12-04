# 📄 Modal Détail de Document - Documentation

## Vue d'ensemble

Chaque document dispose maintenant d'une **vue détaillée complète** accessible en un clic, permettant de consulter toutes les données OCR, métadonnées extraites, et d'interagir avec les agents IA.

## 🎯 Comment Accéder

### Méthode 1 : Clic sur la Ligne
- Cliquer n'importe où sur la ligne du document dans le tableau
- Le modal s'ouvre automatiquement

### Méthode 2 : Bouton "Œil"
- Cliquer sur l'icône 👁️ dans la colonne "Actions"
- Plus précis si vous voulez éviter les clics accidentels

## 🎨 Interface du Modal

### En-tête
```
┌──────────────────────────────────────────────────────────┐
│ [X] Sommation Impôt Cantonal Valais 2024 - 4737.70 CHF  │
│     🔴 Urgent (100)  |  Facture  |  ✓ Traité            │
└──────────────────────────────────────────────────────────┘
```

### 3 Onglets Disponibles

#### 📋 **Onglet "Vue d'ensemble"**

**Métadonnées Affichées** :
- Nom du document (éditable)
- Type de document
- Score d'importance (éditable 0-100)
- Date du document
- Échéance (éditable)
- Montant et devise (éditables)

**Informations Fichier** :
- Nom de fichier original
- Date d'upload
- Taille du fichier
- Confiance IA (%)

**Section "Analyser avec un Agent"** :
- 💼 **Agent Comptable** → Analyse financière
- ⚖️ **Agent Juridique** → Analyse légale
- Clic → Redirection vers chat avec document en contexte

#### 📄 **Onglet "Texte OCR"**

- Texte complet extrait par OCR
- Textarea scrollable
- Nombre de caractères affiché
- Format préservé (espaces, retours à la ligne)
- Lecture seule
- Astuce : "Ce texte est utilisé par les agents IA..."

#### 🤖 **Onglet "Analyse IA"**

**Mots-clés Détectés** :
- Badges bleus pour chaque mot-clé
- Exemple : `impôt` `sommation` `valais` `cantonal`

**Résumé IA** :
- Description générée automatiquement
- Contexte et action requise

**Facteurs d'Importance** :
- ✓ Has deadline (vert si true)
- ✓ Is urgent (vert si true)
- ✓ Has high amount (vert si true)
- ✓ Requires action (vert si true)

### Pied de Page

**Actions Disponibles** :
- 🗑️ **Supprimer** (à gauche, rouge)
- **Fermer** (à droite, gris)

## 🔧 Fonctionnalités

### 1. **Consultation des Données**
- ✅ Voir toutes les métadonnées extraites
- ✅ Lire le texte OCR complet
- ✅ Consulter l'analyse IA
- ✅ Vérifier les mots-clés détectés

### 2. **Édition des Métadonnées**
1. Cliquer sur "Éditer" (en haut à droite)
2. Modifier les champs :
   - Nom du document
   - Score d'importance
   - Échéance
   - Montant et devise
3. Cliquer sur "Sauvegarder"
4. Les changements sont appliqués immédiatement

**Cas d'usage** :
- L'IA s'est trompée sur le montant → Corriger
- Changer la deadline → Mettre la bonne date
- Ajuster le score d'importance → Augmenter/diminuer

### 3. **Analyse par Agent IA**

#### Agent Comptable 💼
- Clic → Redirection vers `/chat/accountant?documentId=10`
- L'agent a accès au document complet
- Questions possibles :
  - "Explique-moi cette facture"
  - "Dois-je payer maintenant ?"
  - "Comment budgéter ce paiement ?"

#### Agent Juridique ⚖️
- Clic → Redirection vers `/chat/legal?documentId=10`
- L'agent analyse les aspects légaux
- Questions possibles :
  - "Quels sont mes droits ?"
  - "Puis-je contester ?"
  - "Quels sont les délais légaux ?"

### 4. **Suppression**
- Bouton rouge en bas à gauche
- Confirmation requise
- Supprime le fichier ET les données

## 📊 Exemple de Données Affichées

### Document : Sommation Impôt Cantonal

**Vue d'ensemble** :
```
Nom : Sommation Impôt Cantonal Valais 2024 - 4737.70 CHF
Type : Facture
Score : 100 (Urgent)
Date document : 24/10/2025
Échéance : 03/11/2025
Montant : 4737.70 CHF

Fichier : WhatsApp Image 2025-12-03.jpeg
Uploadé le : 4 décembre 2025
Taille : 327 KB
Confiance IA : 68%
```

**Texte OCR** (1959 caractères) :
```
Département des finances et de l'énergie
Service cantonal des contributions
Taxation des personnes physiques

CANTON DU VALAIS
Av. de la Gare 35
1951 Sion

Monsieur
Gross Alexandre
Rue du Grand-Pont 33
1950 Sion

Sommation : Impôt cantonal 2024
Montant en CHF : 4,737.70

Solde à payer jusqu'au 03.11.2025
...
```

**Analyse IA** :
```
Mots-clés : [impôt] [sommation] [valais] [cantonal]

Résumé : Sommation de paiement d'impôt cantonal 2024 
pour un montant de 4737.70 CHF à payer avant le 3 novembre 2025.

Facteurs :
✓ Has deadline
✓ Is urgent
✓ Has high amount
✓ Requires action
```

## 🎓 Cas d'Usage

### Cas 1 : Vérifier une Facture
```
1. Cliquer sur la facture
2. Onglet "Vue d'ensemble" → Vérifier montant et échéance
3. Onglet "Texte OCR" → Lire les détails
4. Clic "Agent Comptable" → Demander conseil
```

### Cas 2 : Corriger une Erreur
```
1. Cliquer sur le document
2. Clic "Éditer"
3. Corriger le montant : 4737.70 → 4737.00
4. Clic "Sauvegarder"
5. Métadonnées mises à jour
```

### Cas 3 : Analyser un Courrier Juridique
```
1. Cliquer sur le courrier
2. Onglet "Texte OCR" → Lire le contenu
3. Clic "Agent Juridique" → Demander analyse
4. Chat s'ouvre avec le document en contexte
```

### Cas 4 : Vérifier les Mots-Clés
```
1. Cliquer sur le document
2. Onglet "Analyse IA"
3. Voir les mots-clés détectés
4. Vérifier si l'IA a bien compris le document
```

## 🔧 Édition des Champs

### Champs Éditables
- ✅ Nom du document (display_name)
- ✅ Score d'importance (0-100)
- ✅ Échéance (date picker)
- ✅ Montant (nombre décimal)
- ✅ Devise (texte, 3 lettres)

### Champs Non Éditables
- ❌ Type de document (détecté par IA)
- ❌ Texte OCR (extrait du fichier)
- ❌ Date du document (extraite du contenu)
- ❌ Mots-clés (générés par IA)
- ❌ Confiance IA (calculée)

**Pourquoi ?** Ces champs sont générés automatiquement et ne doivent pas être modifiés manuellement pour préserver l'intégrité des données.

## 🚀 API Utilisées

### GET /api/documents/{id}
```json
{
  "id": 10,
  "display_name": "Sommation Impôt Cantonal...",
  "document_type": "invoice",
  "importance_score": 100,
  "deadline": "2025-11-03",
  "extracted_amount": 4737.70,
  "currency": "CHF",
  "extracted_text": "Département des finances...",
  "keywords": "[\"impôt\", \"sommation\"]",
  "extracted_data": "{\"summary\": \"...\", \"importance_factors\": {...}}",
  ...
}
```

### PATCH /api/documents/{id}
```json
{
  "display_name": "Nouveau nom",
  "importance_score": 95,
  "deadline": "2025-12-31",
  "extracted_amount": 5000.00,
  "currency": "EUR"
}
```

## 💡 Astuces

### Éditer Rapidement
1. Clic sur document
2. Clic "Éditer"
3. Tab pour naviguer entre champs
4. Enter pour sauvegarder

### Envoyer à un Agent
1. Clic sur document
2. Scroll en bas
3. Clic sur l'agent désiré
4. Chat s'ouvre avec contexte

### Lire le Texte OCR
1. Clic sur document
2. Onglet "Texte OCR"
3. Scroll pour lire
4. Copier-coller possible

## 🐛 Dépannage

### Modal ne s'ouvre pas
→ Vérifier console navigateur (F12)
→ Hard refresh (Cmd+Shift+R)

### Données manquantes
→ Le document n'a peut-être pas été complètement analysé
→ Vérifier status = "completed"

### Édition ne fonctionne pas
→ Vérifier les logs backend
→ Vérifier que l'utilisateur est authentifié

### Agents ne répondent pas
→ Vérifier que la page chat existe
→ Vérifier OpenAI API key

## 📈 Performance

- **Ouverture modal** : <100ms (données déjà en cache)
- **Chargement données** : ~200-500ms (appel API)
- **Sauvegarde édition** : ~100-300ms
- **Redirection agent** : Instantané

## ✅ Checklist de Test

- [ ] Cliquer sur un document → Modal s'ouvre
- [ ] Voir les 3 onglets
- [ ] Onglet "Vue d'ensemble" → Voir métadonnées
- [ ] Onglet "Texte OCR" → Voir texte complet
- [ ] Onglet "Analyse IA" → Voir mots-clés
- [ ] Clic "Éditer" → Champs deviennent éditables
- [ ] Modifier un champ → Clic "Sauvegarder" → Succès
- [ ] Clic "Agent Comptable" → Redirection vers chat
- [ ] Clic "Fermer" → Modal se ferme

## 🎉 Résultat

Vous avez maintenant un **accès complet** à toutes les données de vos documents :
- ✅ Consultation
- ✅ Édition
- ✅ Analyse par agents
- ✅ Interface moderne et intuitive

---

**Rafraîchissez la page et cliquez sur un document pour tester !**

http://localhost:3001/documents + Cmd+Shift+R

