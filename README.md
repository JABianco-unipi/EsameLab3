# EsameLab3 - Connections
Esame di Lab3/Laboratorio di Reti

## SEZ. 1: INTERPRETAZIONE DEL PROGETTO

### 1.1 Modellazione della partita e dello stato dei giocatori
La partita è stata modellata come un’entità globale unica, condivisa da tutti i giocatori durante il suo intervallo temporale di validità.

Parallelamente, per ciascun utente è mantenuto uno stato di partita separato, che tiene traccia delle proposte corrette, degli errori e del punteggio.

Questa separazione consente di gestire correttamente situazioni in cui giocatori diversi si trovano in fasi differenti della stessa partita globale, pur condividendo le stesse parole.

---

### 1.2 Politica di persistenza: quando salvare utenti e partite
La specifica richiede persistenza periodica e consistenza in caso di riavvio, senza imporre una strategia unica.

Nel progetto consegnato si è scelto di differenziare tra:

- Salvataggio delle partite: eseguito alla fine della partita, quando l’esito globale è determinato e i risultati sono stabili
- Salvataggio dei profili utente: eseguito periodicamente anche durante la partita, così da ridurre la perdita di dati (es. progressi, aggiornamenti credenziali, statistiche) in caso di chiusura inattesa del server

Questa scelta bilancia consistenza (partite salvate solo quando concluse) e robustezza (utenti aggiornati con maggiore frequenza).

---

### 1.3 Stato di login mantenuto sia lato server che lato client
La specifica non vincola dove mantenere lo stato di sessione; nel progetto si è scelto di mantenere:

- Stato di login lato server, necessario per autorizzare le operazioni e gestire correttamente la transizione tra “fase login/registrazione” e “fase partita”
- Stato di login lato client, usato principalmente per supportare la logica di auto-login alla partita successiva e per l’invio di comandi già filtrato lato client

Nel server questo si riflette nel ciclo di gestione delle richieste: finché l’utente non risulta loggato il server accetta solo operazioni di login/registrazione/aggiornamento credenziali; durante la sessione loggata vengono gestite le operazioni di gioco.

---

### 1.4 Auto-login alla partita successiva basato su notifica UDP e coda comandi
Il progetto prevede notifiche asincrone via UDP, ma non impone come gestire l’ingresso automatico nella partita successiva. La scelta adottata è:

- Il server invia un’unica notifica asincrona: la fine partita
- Quando il client riceve tale notifica, accoda automaticamente un comando di login nella propria queue interna
- Il comando accodato viene poi inviato al server per effettuare l’accesso immediato alla partita successiva senza intervento dell’utente

Questa soluzione mantiene il canale TCP dedicato alle normali richieste/risposte, e sfrutta UDP solo come “trigger” per un’azione automatica lato client.

---

### 1.5 Gestione cambio credenziali e coerenza dell’auto-login
La specifica consente l’operazione di aggiornamento credenziali anche in sessione. Per evitare incongruenze tra credenziali “vecchie” e “nuove” durante l’auto-login, il client conserva localmente eventuali credenziali aggiornate e:

- Tiene conto di cambi username/password durante la sessione di gioco
- Considera anche l’eventualità che l’utente aggiorni le credenziali prima del login effettivo

In questo modo, quando viene accodato il login automatico, il client usa sempre la versione più aggiornata delle credenziali disponibili.

---

### 1.5 Comportamento di fallback se la notifica UDP non arriva
Dato che UDP non garantisce consegna, è stata introdotta una regola di fallback: se la notifica non arriva, il giocatore non resta bloccato. In tale situazione il server consente solamente:

- logout
- login (alla partita successiva)

Questa scelta evita che il client possa continuare a inviare comandi di gioco su una partita che localmente non risulta terminata, ma che lato server può essere già conclusa o in transizione.

---

### 1.6 Centralizzazione della logica di gioco
Tutta la logica di validazione delle proposte è stata accentrata nel server, che rappresenta l’unica autorità in grado di determinare la correttezza delle azioni dei giocatori.

Questa scelta evita inconsistenze tra client e impedisce che comportamenti errati o malevoli possano compromettere lo stato del gioco.

---

### 1.7 Login consentito da un solo dispositivo
Nel progetto è stato scelto di consentire l’accesso a un utente da un solo dispositivo alla volta.

Il server mantiene per ciascun utente lo stato di login e rifiuta nuove richieste di login qualora l’utente risulti già autenticato da un’altra connessione attiva.

---

### 1.8 Gestione della chiusura del client
In caso di chiusura del client (comando exit o terminazione dell’applicazione) mentre l’utente risulta loggato, il server rileva la chiusura della connessione ed esegue automaticamente il logout lato server.

Questa scelta mantiene la consistenza dello stato di login, evitando che un utente resti erroneamente autenticato dopo una disconnessione improvvisa.

---

## SEZ. 2: SCHEMA GENERALE DEI THREAD

### 2.1 Thread lato server

#### Thread principale del server
È responsabile dell’avvio dell’applicazione, dell’inizializzazione delle strutture dati condivise (profili utenti, partita corrente) e dell’ascolto delle connessioni TCP in ingresso.

Per ogni nuova connessione accettata, il server delega la gestione del client a un thread del pool.

---

#### Thread del pool (ConnectionsHandler)
Ogni client connesso è gestito da un’istanza di ConnectionsHandler, eseguita all’interno di un thread del pool.

Questo thread:
- gestisce la fase di login/registrazione
- mantiene lo stato del giocatore associato alla connessione
- riceve e processa i comandi JSON del client
- invia le risposte sincrone tramite TCP

Il thread rimane attivo per l’intera durata della connessione TCP.

---

#### Thread di gestione della partita (PartitaHandler)
- marca la partita come terminata
- salva la partita su file JSON (GameId.json)
- aggiorna le statistiche globali
- invia la notifica UDP
- prepara la partita successiva

---

#### Thread per la persistenza (FileLogsHandler)
- salva periodicamente i profili utente su JSON

---

### 2.2 Thread lato client

#### Thread principale
Gestisce:
- TCP (connect, write, read)
- UDP (notifiche fine partita)
- auto-login

---

#### Thread input
- legge da stdin
- genera JSON
- inserisce nella commandQueue

---

## SEZ. 3: STRUTTURE DATI

### Lato server
- ConcurrentHashMap<String, Player>
- ConcurrentHashMap<String, RisultatiPartita>
- ConcurrentHashMap<String, SocketAddress>
- HashMap<String,String[]>
- TreeMap<Integer, HashSet<String>>
- HashSet<HashSet<String>>
- HashSet<String>

---

### Lato client
- BlockingQueue<String>
- ByteBuffer

---

## SEZ. 4: SINCRONIZZAZIONE

- synchronized (monitor Java)
- ConcurrentHashMap
- AtomicInteger / AtomicBoolean
- volatile
- BlockingQueue

---

## SEZ. 5: ISTRUZIONI SU COME COMPILARE E ESEGUIRE IL PROGETTO

### 5.1 Compilazione da file sorgente

Per compilare il progetto a partire dal codice sorgente è necessario aprire un terminale nella cartella del progetto  
**ConsegnaProgettoLAB3(671859)**, che include la libreria esterna Gson.

#### 1. Creazione dei file `.class`
```bash
mkdir -p bin
javac -cp ".:gson-2.13.2.jar" -d bin $(find . -name "*.java")
```
#### 2. Esecuzione del Server
```bash
java -cp "bin:gson-2.13.2.jar" connections_app.ServerMain
```
#### 3. Esecuzione del Client
```bash
java -cp "bin:gson-2.13.2.jar" connections_app.ClientMain
```
### 5.2 Esecuzione del file `.jar`
È possibile eseguire il progetto utilizzando direttamente i file .jar forniti.

#### 1. Esecuzione del Server
```bash
java -cp "Server.jar:gson-2.13.2.jar" connections_app.ServerMain
```
#### 2. Esecuzione del Client
```bash
java -cp "Client.jar:gson-2.13.2.jar" connections_app.ClientMain
```
### 5.3 Comandi per poter usufruire del programma

Una volta avviato prima il server e successivamente il client, e dopo la comparsa dei messaggi:
"Connessione stabilita con il server."
"Benvenuto nel gioco Connections!"

è possibile utilizzare i seguenti comandi tramite interfaccia a linea di comando.

---

#### Comandi sempre disponibili

| Comando | Descrizione |
|--------|------------|
| `help` | Mostra la lista completa dei comandi supportati dal client |
| `exit` | Termina il client e chiude correttamente la connessione |

---

#### Comandi disponibili se l’utente NON è loggato

| Comando | Descrizione |
|--------|------------|
| `register <username> <password>` | Registra un nuovo utente |
| `login <username> <password>` | Effettua il login e accede alla partita corrente (invia anche la porta UDP al server) |
| `updateCredentials <oldUsername> <oldPassword> <newUsername|-> <newPassword|->` | Aggiorna username e/o password (`-` = non modificare) |

**Nota:** qualsiasi altro comando viene rifiutato dal client (e dal server).

---

#### Comandi disponibili se l’utente è loggato

| Comando | Descrizione |
|--------|------------|
| `logout` | Effettua il logout e termina la partecipazione alla partita |
| `submitProposal <word1> <word2> <word3> <word4>` | Invia una proposta di 4 parole (usa `_` per parole composte) |
| `requestGameInfo <gameId>` | Informazioni su una partita (`-1` = partita corrente) |
| `requestGameStats <gameId>` | Statistiche aggregate (`-1` = partita corrente) |
| `requestLeaderboard <argomento>` | Classifica: username / top N / `-1` completa |
| `requestPlayerStats` | Statistiche personali |
| `updateCredentials <oldUsername> <oldPassword> <newUsername|-> <newPassword|->` | Aggiorna credenziali anche durante la sessione |

---

#### Note

- Se `gameId` coincide con la partita corrente, il server restituisce comunque i dati aggiornati della partita corrente.
- Le notifiche di fine partita vengono ricevute tramite UDP.
- L’auto-login alla partita successiva è gestito automaticamente dal client.
