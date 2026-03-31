package connections_app;

import java.util.HashSet;
// creo la classe che memorizza le informazioni della parita per poterle poi aggiornare a fine gioco e per recuperare informazioni come json

public class RisultatiPartita{

    private String esito;// questo riguarda la partita in corso (vittoria, sconfitta, vittoria perfetta)
    private int currentMistakes = 0;// numero di errori commessi nella partita corrente
    private int propCorrette = 0;// numero di proposte corrette fatte nella partita corrente
    private int currentPoints = 0;// punti accumulati nella partita corrente
    private transient HashSet<HashSet<String>> proposteCorrette; //ora memorizzo tutte le proposte corrette fatte dal giocatore
    private transient HashSet<String> paroleMancanti; // memorizzo le parole ancora da indovinare nella partita corrente

    // i metodi devono essere sincronizzati per evitare problemi di concorrenza
    public synchronized void addProposta(HashSet<String> proposta){
        HashSet<String> propostaCopy = new HashSet<>(proposta); // creo una copia della proposta per evitare problemi di concorrenza
        if (proposteCorrette == null){ // inizializzazione lazy della struttura dati
            proposteCorrette = new HashSet<>();
            proposteCorrette.add(propostaCopy); // prima proposta corretta inserita
            propCorrette++;
            currentPoints += 6; // aggiungo i punti per la proposta corretta 
            if (paroleMancanti != null) {
                for (String parola : propostaCopy) {
                    paroleMancanti.remove(parola);
                }
            }
        }
        else{
            proposteCorrette.add(propostaCopy); // aggiungo la nuova proposta corretta tanto il controllo dei duplicati lo faccio nel handler
            propCorrette++;
            if (paroleMancanti != null) {
                for(String parola : propostaCopy){
                    paroleMancanti.remove(parola); // rimuovo la parola indovinata dalle parole mancanti
                }
            }
            currentPoints += 6; // aggiungo i punti per la proposta corretta 
            if(propCorrette == 3 && paroleMancanti != null && paroleMancanti.size() == 4){ // l'ultima proposta viene di conseguenza corretta automaticamente
                HashSet<String> ultimeParole = new HashSet<>(paroleMancanti); // copio le parole mancanti
                proposteCorrette.add(ultimeParole); // aggiungo l'ultima proposta corretta
                propCorrette++;
                paroleMancanti.clear(); // ora non ci sono più parole mancanti
            }
            if(propCorrette == 4 && currentMistakes == 0){
                esito = "VITTORIA_PERFETTA"; // aggiorno lo stato del giocatore a vittoria perfetta
            }
            else if(propCorrette == 4){
                esito = "VITTORIA"; // aggiorno lo stato del giocatore a vittoria
            }
        }
    }

    public synchronized boolean controllaParole(HashSet<String> paroleProposte){// serve a capire se la proposta del giocatore è accettabile nel senso che contiene parole valide
        if (paroleMancanti == null) {
            return false;
        }
        for (String parola : paroleProposte){
            if (!paroleMancanti.contains(parola)){
                return false;
            }
        }
        return true;
    }

    public synchronized void setParoleMancanti(HashSet<String> parole){
        if (parole == null) {
            this.paroleMancanti = null;
            return;
        }
        this.paroleMancanti = new HashSet<>(parole); // creo una copia per evitare modifiche esterne
    }
    public synchronized HashSet<String> getParoleMancanti(String username){
        if (this.paroleMancanti == null) {
            return new HashSet<>();
        }
        HashSet<String> copie = new HashSet<>(this.paroleMancanti); // ritorno una copia per evitare modifiche esterne
        return copie;
    }

    public synchronized boolean propostaGiaFatta (HashSet<String> proposta){// controllo se la proposta è già stata fatta
        if (proposteCorrette == null) {
            return false;
        }
        return proposteCorrette.contains(proposta);
    }
    public synchronized String risposteCorrette(){ // ritorno le proposte corrette fatte dal giocatore
        if(proposteCorrette == null){
            return "Nessuna proposta corretta fatta.";
        }
        StringBuilder sb = new StringBuilder();
        for(HashSet<String> proposta : proposteCorrette){
            sb.append(proposta.toString()).append("\n");
        }
        return sb.toString();
    }

    public synchronized void addErrore(){
        currentMistakes += 1;
        currentPoints -= 4; // tolgo i punti per l'errore 
        if(currentMistakes == 4){
            esito = "SCONFITTA"; // aggiorno lo stato del giocatore a sconfitta
        }
    }

    public synchronized int getCurrentMistakes(){
        return currentMistakes;
    }

    public synchronized Boolean isPartitaVinta(){ // ritorno l'esito della partita
        return esito != null && (esito.equals("VITTORIA") || esito.equals("VITTORIA_PERFETTA"));
    }
    public synchronized Boolean isPartitaPersa(){
        return esito != null && esito.equals("SCONFITTA");
    }
    public synchronized Integer getPropCorrette(){
        return propCorrette;
    }

    public synchronized boolean isPartitaPerfetta(){
        return esito != null && esito.equals("VITTORIA_PERFETTA");
    }
    public synchronized int getPunteggioFinale(){ // ritorno il punteggio finale del giocatore
        return currentPoints;
    }
}