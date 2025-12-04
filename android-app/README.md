# AgentCFO Android App

Application Android native pour AgentCFO - Gestion financière intelligente avec agents IA.

## Architecture

- **Langage**: Kotlin
- **UI Framework**: Jetpack Compose
- **Minimum SDK**: 24 (Android 7.0)
- **Target SDK**: 35 (Android 15)

## Technologies utilisées

- **Jetpack Compose** - UI moderne et déclarative
- **Retrofit + OkHttp** - API REST client
- **Coroutines** - Programmation asynchrone
- **CameraX** - Capture de photos de documents
- **Coil** - Chargement d'images
- **Biometric API** - Authentification biométrique
- **DataStore** - Stockage sécurisé
- **Navigation Compose** - Navigation entre écrans

## Configuration

### Prérequis

- Android Studio Ladybug | 2024.2.1 ou supérieur
- JDK 11 ou supérieur
- Android SDK 35

### Backend API

L'application se connecte au backend FastAPI AgentCFO :

- **Dev** : `http://10.0.2.2:8001` (émulateur Android → localhost)
- **Production** : `https://api.agentcfo.com`

Assurez-vous que le backend est démarré avant de lancer l'app :

```bash
cd ../
docker-compose up -d
```

### Compilation et exécution

1. Ouvrir le projet dans Android Studio
2. Synchroniser les dépendances Gradle
3. Connecter un appareil ou lancer un émulateur
4. Cliquer sur Run (▶️)

Ou en ligne de commande :

```bash
./gradlew assembleDebug
./gradlew installDebug
```

## Structure du projet

```
app/src/main/
├── java/com/agentcfo/
│   ├── MainActivity.kt              # Point d'entrée
│   ├── network/                     # API services & models
│   │   ├── AgentCfoApiService.kt
│   │   ├── ApiModels.kt
│   │   └── RetrofitClient.kt
│   ├── auth/                        # Authentification
│   │   ├── TokenManager.kt
│   │   ├── BiometricAuthManager.kt
│   │   └── AuthenticationState.kt
│   ├── data/                        # Repositories
│   │   ├── AuthRepository.kt
│   │   └── DocumentRepository.kt
│   ├── viewmodel/                   # ViewModels
│   │   ├── AuthViewModel.kt
│   │   └── DocumentViewModel.kt
│   ├── ui/                          # Composables UI
│   │   ├── auth/
│   │   ├── documents/
│   │   ├── camera/
│   │   ├── navigation/
│   │   └── theme/
│   └── utils/                       # Utilitaires
│       ├── FileUtils.kt
│       └── PermissionHandler.kt
└── res/                             # Ressources
    ├── values/
    └── xml/
```

## Fonctionnalités

### MVP (v1.0)

- ✅ Authentification JWT + biométrie
- ✅ Upload de documents (photo + galerie)
- ✅ Liste et visualisation des documents
- ✅ Capture photo avec CameraX
- ✅ Navigation fluide entre écrans
- ✅ Gestion des erreurs réseau

### Post-MVP

- Dashboard financier
- Chat avec agents IA
- Classeur virtuel hiérarchique
- Notifications pour deadlines
- Mode offline avec synchronisation
- Scan multi-pages
- Export et partage de documents

## Permissions

L'application requiert les permissions suivantes :

- `INTERNET` - Appels API backend
- `CAMERA` - Capture de documents
- `READ_MEDIA_IMAGES` - Accès galerie photos
- `USE_BIOMETRIC` - Authentification biométrique

## Tests

Exécuter les tests unitaires :

```bash
./gradlew test
```

Exécuter les tests instrumentés :

```bash
./gradlew connectedAndroidTest
```

## Build Release

Pour créer un APK de production :

```bash
./gradlew assembleRelease
```

L'APK sera généré dans `app/build/outputs/apk/release/`

## Dépannage

### Erreur de connexion au backend

- Vérifiez que le backend est démarré : `docker-compose ps`
- Sur émulateur Android, utilisez `10.0.2.2` pour localhost
- Sur appareil physique, utilisez l'IP locale de votre machine

### Problèmes de build

```bash
./gradlew clean
./gradlew build --refresh-dependencies
```

### Permission caméra refusée

Allez dans Paramètres → Apps → AgentCFO → Permissions et activez Caméra

## License

Propriétaire - Tous droits réservés

