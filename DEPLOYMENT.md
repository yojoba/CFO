# Guide de déploiement AgentCFO

## Configuration initiale

### 1. Variables d'environnement

Créez un fichier `.env` à la racine du projet avec les valeurs suivantes :

```bash
# Database
POSTGRES_DB=agentcfo
POSTGRES_USER=agentcfo
POSTGRES_PASSWORD=votre_mot_de_passe_securise
POSTGRES_PORT=5433

# Backend
BACKEND_PORT=8001
ENVIRONMENT=production

# OpenAI
OPENAI_API_KEY=sk-votre-clé-api-openai

# JWT (générez un secret sécurisé avec: openssl rand -hex 32)
JWT_SECRET=votre_secret_jwt_securise
JWT_ALGORITHM=HS256
JWT_EXPIRATION_MINUTES=1440

# Upload
MAX_UPLOAD_SIZE_MB=10

# Frontend
FRONTEND_PORT=3008
NEXT_PUBLIC_API_URL=https://votre-domaine.ch/api
NODE_ENV=production
```

### 2. Premier lancement

```bash
# Construction des images
docker-compose build

# Démarrage des services
docker-compose up -d

# Vérifier les logs
docker-compose logs -f
```

### 3. Accès aux services

- Frontend: http://localhost:3008
- Backend API: http://localhost:8001
- Documentation API: http://localhost:8001/docs

## Déploiement sur VPS Infomaniak

### 1. Préparation du serveur

```bash
# Se connecter au VPS
ssh user@votre-vps.infomaniak.ch

# Installer Docker et Docker Compose si nécessaire
sudo apt update
sudo apt install docker.io docker-compose

# Cloner le projet
git clone <votre-repo> /opt/agentcfo
cd /opt/agentcfo
```

### 2. Configuration pour production

Modifiez le `.env` pour la production :

```bash
# Utilisez votre domaine
NEXT_PUBLIC_API_URL=https://agentcfo.votre-domaine.ch/api

# Ports non utilisés sur le système
POSTGRES_PORT=5433
BACKEND_PORT=8001
FRONTEND_PORT=3008
```

### 3. Configuration du reverse proxy existant

Puisque vous avez déjà un reverse proxy sur votre serveur, configurez-le pour pointer vers vos services :

**Pour Nginx**, ajoutez cette configuration :

```nginx
# Backend API
upstream agentcfo_backend {
    server localhost:8001;
}

# Frontend
upstream agentcfo_frontend {
    server localhost:3008;
}

server {
    listen 80;
    server_name agentcfo.votre-domaine.ch;

    # Redirection HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name agentcfo.votre-domaine.ch;

    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;

    # Frontend
    location / {
        proxy_pass http://agentcfo_frontend;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
    }

    # Backend API
    location /api {
        proxy_pass http://agentcfo_backend/api;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # Pour les uploads de fichiers
        client_max_body_size 10M;
    }

    # Documentation API
    location /docs {
        proxy_pass http://agentcfo_backend/docs;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
    }

    location /openapi.json {
        proxy_pass http://agentcfo_backend/openapi.json;
    }
}
```

### 4. Démarrage en production

```bash
cd /opt/agentcfo

# Démarrer les services
docker-compose up -d

# Vérifier que tout fonctionne
docker-compose ps
docker-compose logs -f
```

### 5. Recharger le reverse proxy

```bash
# Pour Nginx
sudo nginx -t
sudo systemctl reload nginx

# Pour Apache
sudo apache2ctl configtest
sudo systemctl reload apache2
```

## Maintenance

### Backup de la base de données

Créez un script de backup automatique :

```bash
#!/bin/bash
# /opt/agentcfo/backup.sh

BACKUP_DIR="/opt/backups/agentcfo"
DATE=$(date +%Y%m%d_%H%M%S)

mkdir -p $BACKUP_DIR

docker-compose exec -T postgres pg_dump -U agentcfo agentcfo > \
    $BACKUP_DIR/agentcfo_$DATE.sql

# Garder seulement les 7 derniers jours
find $BACKUP_DIR -name "agentcfo_*.sql" -mtime +7 -delete
```

Ajoutez au crontab :

```bash
# Backup quotidien à 2h du matin
0 2 * * * /opt/agentcfo/backup.sh
```

### Mise à jour

```bash
cd /opt/agentcfo

# Sauvegarder la base de données
./backup.sh

# Tirer les dernières modifications
git pull

# Reconstruire et redémarrer
docker-compose down
docker-compose build
docker-compose up -d
```

### Logs

```bash
# Voir tous les logs
docker-compose logs -f

# Logs d'un service spécifique
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs -f postgres
```

### Monitoring

```bash
# Vérifier l'utilisation des ressources
docker stats

# Vérifier les services
docker-compose ps
```

## Sécurité en production

1. **Changez tous les mots de passe par défaut**
2. **Utilisez des secrets forts** pour JWT_SECRET
3. **Configurez un firewall** (ufw ou iptables)
4. **Activez SSL/TLS** (Let's Encrypt)
5. **Limitez l'accès SSH** au VPS
6. **Configurez les backups automatiques**
7. **Surveillez les logs** régulièrement

## Troubleshooting

### Le backend ne démarre pas

```bash
# Vérifier les logs
docker-compose logs backend

# Vérifier que la base de données est prête
docker-compose exec postgres pg_isready -U agentcfo
```

### Le frontend ne peut pas se connecter au backend

Vérifiez que `NEXT_PUBLIC_API_URL` dans `.env` est correct.

### Erreurs d'upload de documents

Vérifiez que :
1. Le dossier `/app/uploads` existe dans le container
2. Les permissions sont correctes
3. La taille max est configurée correctement

### Base de données corrompue

Restaurez depuis un backup :

```bash
docker-compose exec -T postgres psql -U agentcfo agentcfo < backup.sql
```
