package connections_app;


// creo la classe che memorizza le informazioni della partita per poterle poi aggiornare a fine gioco e per recuperare informazioni come json

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;



public class InfoPartita{
    private int gameId;
    private final HashMap<String,String[]> parole = new HashMap<>(); // parole come tema e le sue combinazioni valide
    private volatile Date dataInizioPartita; 
    private volatile Date dataFinePartita; 
    private volatile String statoPartita; // IN_CORSO, TERMINATA

    private volatile TreeMap<Integer, HashSet<String>> classificaFinale; // classifica finale della partita con il punteggio
    private final AtomicInteger numeroGiocatori = new AtomicInteger(0);

    private final AtomicInteger numeroGiocatoriVincenti = new AtomicInteger(0);
    private final AtomicInteger numeroGiocatoriPerdenti = new AtomicInteger(0);

    private volatile double punteggioMedio;

    private final ConcurrentHashMap<String, RisultatiPartita> giocatori = new ConcurrentHashMap<>();// giocatori nella partita

    private transient int tempoPartita; // tempo partita in secondi

 


    
    public boolean haPartecipatoGiocatore(String username) {
        return this.giocatori.containsKey(username);
    }
    public ConcurrentHashMap<String, RisultatiPartita> getGiocatori(){
        return this.giocatori;
    }

    

    public synchronized RisultatiPartita aggiungiGiocatore(String username){ // aggiunta di un giocatore alla partita
        if(!this.giocatori.containsKey(username)){
            RisultatiPartita risultati = new RisultatiPartita();
            // inizializzo larole mancanti per il giocatore
            HashSet<String> paroleMancanti = new HashSet<>();
            for(String tema : this.parole.keySet()){
                String[] combinazioneCorretta = parole.get(tema); // prendo le comibinazioni valide per la partita corretta
                paroleMancanti.addAll(Arrays.asList(combinazioneCorretta));
            }
            risultati.setParoleMancanti(paroleMancanti);
            this.numeroGiocatori.incrementAndGet();
            this.giocatori.put(username, risultati);
            return risultati;
        }
        else{
            return this.giocatori.get(username); // se il giocatore esiste già ritorno i suoi risultati
        }
    }
    


    public String displayPartita() {
        List<String> words = new ArrayList<>();
        for (String tema : this.parole.keySet()) {
            String[] combinazioneCorretta = parole.get(tema);
            words.addAll(Arrays.asList(combinazioneCorretta));
        }
        // shuffle vero
        Collections.shuffle(words);

        // calcolo larghezza colonna
        int maxLen = 0;
        for (String w : words) {
            if (w != null && w.length() > maxLen) {
                maxLen = w.length();
            }
        }
        int larghezzaColonna = maxLen + 2;

        StringBuilder sb = new StringBuilder();
        sb.append("Parole della partita:\n");

        int count = 0;
        for (String parola : words) {
            sb.append(String.format("%-" + larghezzaColonna + "s", parola));
            count++;
            if (count % 4 == 0) sb.append("\n");
        }
        if (count % 4 != 0) sb.append("\n");

        return sb.toString();
    }

    public boolean verificaParole(HashSet<String> paroleProposte){// serve per capire se la proposta del giocatore è corretta
        for( String tema : this.parole.keySet()){
            boolean tutteCorrette = true;
            String[] combinazioneCorretta = parole.get(tema); // prendo le comibinazioni valide per la partita corretta
            for(String parolaCombinazione : combinazioneCorretta){
                if(!paroleProposte.contains(parolaCombinazione)){
                    tutteCorrette = false;
                    break;
                }
            }
            if(tutteCorrette){
                return true; // se anche un tema è corretto ritorno true
            }
        }
        return false;
    }

    public String tempoRimanente() {

        long ora = System.currentTimeMillis();
        Date inizio = this.dataInizioPartita;
        if (inizio == null) {
            return "10:00\n"; // se la partita non è ancora iniziata, tutto il tempo è rimanente
        }
        long trascorsoMs = ora - inizio.getTime();
        long rimastoMs = Math.max(0, tempoPartita * 60 * 1000 - trascorsoMs);

        long totalSeconds = rimastoMs / 1000;
        long minuti = totalSeconds / 60;
        long secondi = totalSeconds % 60;

        return String.format("%02d:%02d\n", minuti, secondi);
    }

    public String getInfoPartitaConclusa(String username){
        RisultatiPartita risultati = this.giocatori.get(username);
        StringBuilder sb = new StringBuilder();
        sb.append("Informazioni partita conclusa:\n");
        // metto anche l'assegnazione delle parole corrette per gruppo
        for(String tema : this.parole.keySet()){
            String[] combinazioneCorretta = parole.get(tema); // prendo le comibinazioni valide per la partita corretta
            sb.append("Tema: ").append(tema).append(" - Combinazione corretta: ");
            for(String parolaCombinazione : combinazioneCorretta){
                sb.append(parolaCombinazione).append(" ");
            }
            sb.append("\n");
        }
        sb.append("Punteggio finale: ").append(risultati.getPunteggioFinale()).append("\n"); // chiamo il metodo in RisultatiPartita
        sb.append("Proposte corrette: ").append(risultati.getPropCorrette()).append("\n"); // chiamo il metodo in RisultatiPartita
        sb.append("Errori commessi: ").append(risultati.getCurrentMistakes()).append("\n"); // chiamo il metodo in RisultatiPartita
        if(risultati.isPartitaVinta()){
            sb.append("Hai vinto la partita!\n");
        }
        else{
            sb.append("Non hai vinto la partita.\n");
        }
        return sb.toString();
    }
    // implementazione del metodo per RequestGameInfo


    public String getInfoPartitaInCorso(String username){
        RisultatiPartita risultati = this.giocatori.get(username);
        StringBuilder sb = new StringBuilder();
        sb.append("Informazioni partita corrente:\n");
        sb.append("Tempo rimanente: ").append(this.tempoRimanente()).append("\n");
        sb.append("Proposte corrette: ").append("\n").append(risultati.risposteCorrette()).append("\n");
        sb.append("Parole ancora da indovinare:\n");
        int count = 0;
        HashSet<String> paroleMancanti = risultati.getParoleMancanti(username);
        for(String parola : paroleMancanti){
            sb.append(parola).append("\t");
            count++;
            if(count % 4 == 0){
                sb.append("\n");
            }
        }
        sb.append("\n");
        sb.append("Errori commessi: ").append(risultati.getCurrentMistakes()).append("\n");
        sb.append("Punteggio attuale: ").append(risultati.getPunteggioFinale()).append("\n");
        return sb.toString();
    }



    public String getStatistichePartitaConclusa(){
        StringBuilder sb = new StringBuilder();
        sb.append("Statistiche partita conclusa:\n");
        sb.append("Numero giocatori partecipanti: ").append(this.numeroGiocatori.get()).append("\n");
        sb.append("Numero giocatori vincenti: ").append(this.numeroGiocatoriVincenti.get()).append("\n");
        sb.append("Numero giocatori che hanno concluso: ").append(this.numeroGiocatoriPerdenti.get() + this.numeroGiocatoriVincenti.get()).append("\n");
        sb.append("Punteggio medio giocatori della partita: ").append(punteggioMedio).append("\n");
        
        return sb.toString();
    }

    public String getStatistichePartitaInCorso(){
        StringBuilder sb = new StringBuilder();
        sb.append("Statistiche partita in corso:\n");
        sb.append("Tempo rimanente: ").append(this.tempoRimanente()).append("\n");
        sb.append("Numero giocatori partecipanti: ").append(this.numeroGiocatori.get()).append("\n");
        sb.append("Numero giocatori che hanno concluso finora: ").append(this.numeroGiocatoriPerdenti.get() + this.numeroGiocatoriVincenti.get()).append("\n");
        sb.append("Numero giocatori vincenti finora: ").append(this.numeroGiocatoriVincenti.get()).append("\n");

        return sb.toString();
    }

    public synchronized void terminaPartita(){ // setto la data finale della partita e lo stato ma anche la classifica finale
        this.statoPartita = "TERMINATA";
        // quando termino la partita devo chiaramente settare la data di fine partita
        this.dataFinePartita = new Date();
        creaClassifica();// creo la classifica finale ama anche il punteggio medio dei giocatori

    }

    public void aggiornaStatisticheGiocatori(ConcurrentHashMap<String,Player> profiliUtenti){ // versione concorrente per evitare lock esterni
        for(Map.Entry<String,RisultatiPartita> entry : this.giocatori.entrySet()){
            String username = entry.getKey();
            RisultatiPartita risultati = entry.getValue();
            Player p = profiliUtenti.get(username);
            if (p == null){
                continue; // se il profilo utente non esiste, salto l'aggiornamento
            }
            p.updateStatisticsAfterGame(risultati); // aggiorno le statistiche del giocatore
            if(!risultati.isPartitaVinta() && !risultati.isPartitaPersa()){
                p.addPuzzlesAbandoned(); // se la partita non è stata vinta o persa allora il giocatore ha abbandonato quindi lo conto come errore
            }
        }
    }


    public void creaClassifica() { // creo la classifica finale della partita
        TreeMap<Integer, HashSet<String>> classificaTemp = new TreeMap<>((a,b) -> b - a); // ordinamento decrescente dei punteggi

        int sommaPunteggi = 0;
        for (String giocatore : this.giocatori.keySet()) {
            RisultatiPartita risultati = this.giocatori.get(giocatore);
            classificaTemp.computeIfAbsent(risultati.getPunteggioFinale(), k -> new HashSet<>()).add(giocatore); // gestisco i pareggi
            sommaPunteggi += risultati.getPunteggioFinale();
        }
        double media = (double) sommaPunteggi / Math.max(this.numeroGiocatori.get(), 1); // evito divisione per zero
        punteggioMedio = BigDecimal.valueOf(media).setScale(2, RoundingMode.HALF_UP).doubleValue();
        this.classificaFinale = classificaTemp;
    }

    public void aggiungiVincitore(){
        this.numeroGiocatoriVincenti.incrementAndGet();
    }

    public void aggiungiPerdente(){
        this.numeroGiocatoriPerdenti.incrementAndGet();
    }

    public synchronized void avviaPartita(int tempoPartitaInSecondi){ // setto la data di inizio partita e lo stato
        this.tempoPartita = tempoPartitaInSecondi;
        this.statoPartita = "IN_CORSO";
        this.dataInizioPartita = new Date();
    }

    public  boolean partitaTerminata(){
        return this.statoPartita != null && this.statoPartita.equals("TERMINATA");
    }

    public  boolean partitaAvviata(){
        return this.statoPartita != null && this.statoPartita.equals("IN_CORSO");
    }


    public TreeMap<Integer, HashSet<String>> getClassificaFinale(){
        return this.classificaFinale;
    }

    public void setTema(String tema){
        this.parole.put(tema,null);
    }
    public void setGameId(int gameId){
        this.gameId = gameId;
    }
    public int getGameId(){
        return this.gameId;
    }
    public void addCombinazione(String tema, String[] parola){ // aggiungo le combinazioni valide per tema
        this.parole.put(tema,parola);
    }


}