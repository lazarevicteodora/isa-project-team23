# 🐳 UPUTSTVO ZA POKRETANJE BACKEND-A NA DOCKER-U

## 📋 Preduslov

Trebate instalirati:
- **Docker** - https://www.docker.com/products/docker-desktop
- **Docker Compose** - obično dolazi sa Docker Desktop-om

## ✅ Provera da li je Docker instaliran

Otvorite PowerShell i pokrenite:

```powershell
docker --version
docker-compose --version
```

Trebalo bi da vidite nešto kao:
```
Docker version 24.0.0, build 12345
Docker Compose version 2.20.0
```

## 🚀 POKRETANJE BACKEND-A

### Korak 1: Idite u projektni folder

```powershell
cd "C:\Users\PC\OneDrive\Desktop\Radna površina\ISA\isa-project-team23\isa-project"
```

### Korak 2: Pokrenite Docker Compose

```powershell
docker-compose up -d
```

Ovo će:
- Izgraditi Docker images (prvi put traje ~5-10 minuta)
- Pokrenuti 2 backend replika na portovima:
  - **Backend 1**: `http://localhost:8081`
  - **Backend 2**: `http://localhost:8082`
- Pokrenuti Nginx na portu `80`

### Korak 3: Čekajte da se build završi

```powershell
docker-compose logs -f backend1
```

Čekajte dok vidite:
```
Tomcat started on port(s): 8080 (http) with context path ''
Application 'isa-project' started successfully
```

Zatim pritisnite `CTRL+C` da izađete iz log-a.

### Korak 4: Provera da li je backend spreman

Otvorite браузер и idite na:

```
http://localhost:8081/health
```

Trebalo bi da vidite:
```json
{
  "status": "UP"
}
```

## 🧪 TESTIRANJE WEBSOCKET CHAT-a

1. **Ulogujte se na frontend**
   - Idite na `http://localhost` (ili gde god je frontend)
   - Ulogujte se kao korisnik

2. **Otvorite video za gledanje**
   - Kliknite na video

3. **Trebalo bi da vidite chat sa:**
   - ✅ Zelena tačka = "Chat konekcija uspostavljena"
   - Chat polje gde možete pisati poruke
   - Broj aktivnih korisnika

4. **Testirajte chat:**
   - Otvorite isti video u drugoj tabs-i (ili drugom web browseru)
   - Pošaljite poruku iz prvog taba
   - Trebalo bi da vidite u drugom taba odmah!

## 📊 MONITOROVANJE BACKEND-a

### Videti sve pokrenute kontejnere:

```powershell
docker ps
```

### Videti log-ove specifičnog kontejnera:

```powershell
docker-compose logs -f backend1
docker-compose logs -f backend2
```

### Izvršiti komandu u kontejneru:

```powershell
docker-compose exec backend1 bash
```

## 🛑 ZAUSTAVLJANJE BACKEND-a

```powershell
docker-compose down
```

Ovo će:
- Zaustaviti sve kontejnere
- Obrisati mrežu
- **ČUVATI** bazu podataka (jer je na host računaru)

## 🔄 RESTART BACKEND-a

```powershell
docker-compose restart
```

## 📦 PONOVNA IZGRADNJA BACKEND-a (ako ste promenili kod)

```powershell
docker-compose down
docker-compose up -d --build
```

## ⚠️ PROBLEMI I REŠENJA

### Problem: "Port 8081 je već u upotrebi"

```powershell
# Pronađite šta koristi port 8081
netstat -ano | findstr :8081

# Zatvorite taj proces (zamenite PID)
taskkill /PID <PID> /F
```

### Problem: "Database connection error"

Proverite da li je PostgreSQL pokrenut na `localhost:5432` sa kredencijalima:
- Username: `postgres`
- Password: `root`
- Database: `isa_project_db`

Ako nije, trebate da ga pokrenete na lokalnoj mašini.

### Problem: "WebSocket konekcija se ne uspostavlja"

Proverite:
1. Da li je backend zaista pokrenut: `http://localhost:8081/health`
2. Da li je frontend na istoj mreži
3. Očistite cache browser-a (CTRL+SHIFT+Delete)
4. Osvežite stranicu (CTRL+F5)

### Problem: "Docker command not found"

Docker nije instaliran. Preuzmite ga sa:
https://www.docker.com/products/docker-desktop

## 📍 VAŽNE NAPOMENE

1. **Port 8081** - Backend Replica 1 (glavni)
2. **Port 8082** - Backend Replica 2 (backup/load balancing)
3. **Port 80** - Nginx (ako ste ga konfigurirali)
4. **WebSocket URL**: `ws://localhost:8081/ws/stream-chat/{videoId}`
5. **REST API**: `http://localhost:8081/api/...`

## 🎯 ZAVRŠNI KORACI

```powershell
# 1. Pokrenite Docker
docker-compose up -d

# 2. Čekajte 30 sekundi da se build završi

# 3. Proverite zdravlje backend-a
curl http://localhost:8081/health

# 4. Osvežite browser (CTRL+F5)

# 5. Trebalo bi da vidite zelenu tačku "Chat konekcija uspostavljena" ✅
```

---

**Ako dalje ne radi, javite grešku koju vidite u browser Console (F12)!**

