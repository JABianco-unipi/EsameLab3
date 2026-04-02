# EsameLab3 - Connections
Esame di Lab3/Laboratorio di Reti

# 📌 Connections Game – Progetto Client/Server

## 📖 Descrizione
Questo progetto implementa un sistema client-server per il gioco *Connections*, basato su:
- comunicazione **TCP** per richieste sincrone
- comunicazione **UDP** per notifiche asincrone

Il server gestisce la logica di gioco e la persistenza, mentre il client fornisce un'interfaccia a linea di comando per l’interazione.

---

## 📚 Indice
- [1. Interpretazione del progetto](#-1-interpretazione-del-progetto)
- [2. Architettura dei thread](#-2-architettura-dei-thread)
- [3. Strutture dati](#-3-strutture-dati)
- [4. Sincronizzazione](#-4-sincronizzazione)
- [5. Compilazione ed esecuzione](#-5-compilazione-ed-esecuzione)
- [6. Comandi](#-6-comandi)

---

# 🧠 1. Interpretazione del progetto

## 1.1 Modellazione della partita
La partita è modellata come:
- un’entità **globale condivisa**
- uno **stato per ogni giocatore**

Lo stato per utente include:
- proposte corrette
- errori
- punteggio

👉 I giocatori possono trovarsi in fasi diverse della stessa partita.

---

## 1.2 Politica di persistenza
Scelta adottata:

- **Partite**
  - salvate alla fine della partita
- **Utenti**
  - salvati periodicamente durante l’esecuzione

👉 Consistenza + tolleranza ai guasti.

---

## 1.3 Stato di login
Gestito sia:
- lato **server** → autorizzazione operazioni
- lato **client** → supporto logica interna (auto-login)

---

## 1.4 Auto-login con UDP
- Il server invia una notifica di fine partita via UDP
- Il client:
  - riceve la notifica
  - accoda automaticamente un comando di login

👉 UDP usato come trigger asincrono.

---

## 1.5 Gestione credenziali
Il client:
- supporta aggiornamenti durante la sessione
- usa sempre le credenziali più aggiornate

---

## 1.6 Fallback UDP
Poiché UDP non garantisce consegna:

Se la notifica non arriva:
- il client può solo:
  - fare login
  - fare logout

👉 Evita inconsistenze di stato.

---

## 1.7 Centralizzazione della logica
Tutta la logica di gioco è gestita dal server:
- validazione proposte
- gestione punteggi

---

## 1.8 Login da un solo dispositivo
Un utente può essere autenticato su una sola connessione attiva.

---

## 1.9 Chiusura del client
In caso di disconnessione:
- il server rileva la chiusura
- esegue automaticamente il logout

---

# 🧵 2. Architettura dei thread

## 2.1 Thread lato server

### Thread principale
- inizializzazione strutture dati
- ascolto connessioni TCP
- assegnazione thread dal pool

---

### Thread pool (ConnectionsHandler)
Per ogni client:
- gestione login/registrazione
- gestione stato utente
- ricezione comandi JSON
- invio risposte TCP

---

### Thread partita (PartitaHandler)
Gestisce il tempo di gioco:
- fine partita:
  - marca partita terminata
  - salva su file JSON
  - aggiorna statistiche
  - invia notifiche UDP
  - inizializza nuova partita

---

### Thread persistenza (FileLogsHandler)
- salvataggio periodico utenti su file JSON

---

## 2.2 Thread lato client

### Thread principale (rete)
Gestisce:
- connessione TCP (OP_CONNECT)
- invio comandi (OP_WRITE)
- ricezione risposte (OP_READ)
- ricezione notifiche UDP

---

### Thread input
- legge comandi da stdin
- genera JSON
- inserisce nella `BlockingQueue`
- attiva `selector.wakeup()`

---

# 🗃️ 3. Strutture dati

## 3.1 Server

- `ConcurrentHashMap<String, Player>`
- `ConcurrentHashMap<String, RisultatiPartita>`
- `ConcurrentHashMap<String, SocketAddress>`
- `HashMap<String, String[]>`
- `TreeMap<Integer, HashSet<String>>`
- `HashSet<HashSet<String>>`
- `HashSet<String>`

---

## 3.2 Client

- `BlockingQueue<String>`
- `ByteBuffer`

---

# 🔒 4. Sincronizzazione

## 4.1 Monitor Java (`synchronized`)
Usati per:
- gestione partita
- aggiornamento dati
- scrittura file

---

## 4.2 Collezioni concorrenti
- `ConcurrentHashMap`

---

## 4.3 Variabili atomiche
- `AtomicInteger`
- `AtomicBoolean`

---

## 4.4 Visibilità
- variabili `volatile`

---

## 4.5 Client
- modello produttore-consumatore con `BlockingQueue`

---

# ⚙️ 5. Compilazione ed esecuzione

## 5.1 Da sorgente

```bash
mkdir -p bin
javac -cp ".:gson-2.13.2.jar" -d bin $(find . -name "*.java")
