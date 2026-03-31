package connections_app;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
// creo la classe che implementa la richiesta della classifica da parte del client
public class RequestLeaderBoard {
    private String operation; // operazione di richiesta classifica
    private String playerName; // nome del giocatore che richiede la classifica
    private int topPlayers; // numero di giocatori da visualizzare nella classifica
    //private boolean allLeaderboard; // flag per richiedere l'intera classifica

    public String getOperation(){
        return this.operation;
    }
    public void setOperation(String operation){
        this.operation = operation;
    }
    public String getPlayerName(){
        return this.playerName;
    }
    public void setPlayerName(String playerName){
        this.playerName = playerName;
    }
    public int getTopPlayers(){
        return this.topPlayers;
    }
    public void setTopPlayers(int topPlayers){
        this.topPlayers = topPlayers;
    }

    public boolean allLeaderboard(){
        return this.topPlayers == -1;
    }
    public String doOperation (ConcurrentHashMap<String, Player> profiliUtenti, Risposte risposte){
        // creo la logica per restituire la classifica
        // prendo la classifica dalla partita
        Gson gson = new Gson();
        StringBuilder sb = new StringBuilder();
        int posizione = 0;
        if(playerName != null){
            // controllo la presenza del giocatore nella classifica
            if(!profiliUtenti.containsKey(playerName)){
                // giocatore non trovato
                risposte.setMessaggio(31, "giocatore non trovato nella classifica");
                String rispostaJson = gson.toJson(risposte); // converto in json la risposta
                risposte.clearMessaggio(); // pulisco il messaggio per future risposte
                return rispostaJson;
            }
            else{
                TreeMap<Integer, HashSet<String>> classificaTemp = new TreeMap<>((a,b) -> b - a); // ordinamento decrescente
                for (String utente : profiliUtenti.keySet()){
                    Player giocatore = profiliUtenti.get(utente);
                    classificaTemp.computeIfAbsent(giocatore.getTotalPoints(), k -> new HashSet<>()).add(utente);
                }
                for(Map.Entry<Integer,HashSet<String>> entry : classificaTemp.entrySet()){
                    posizione += 1;
                    HashSet<String> giocatoriStessoLiv = entry.getValue();
                    if(giocatoriStessoLiv.contains(playerName)){
                        sb.append("Player: ").append(playerName).append(" - Position: ").append(posizione).append(" - Total Points: ").append(entry.getKey()).append("\n");
                        break;
                    }
                }
                String msg = sb.toString();
                risposte.setDataMessaggio(32, "posizione del giocatore nella classifica", msg);
                String rispostaJson = gson.toJson(risposte); // converto in json la risposta
                risposte.clearMessaggio(); // pulisco il messaggio per future risposte
                return rispostaJson;
            }
        }
        else{
            TreeMap<Integer, HashSet<String>> classificaTemp = new TreeMap<>((a,b)-> b - a); // ordinamento decrescente
            for (String utente : profiliUtenti.keySet()){
                Player giocatore = profiliUtenti.get(utente);
                classificaTemp.computeIfAbsent(giocatore.getTotalPoints(), k -> new HashSet<>()).add(utente);
            }
            // calcolo la dimensione della mia classifica temporanea
            int size = 0;
            for(Map.Entry<Integer,HashSet<String>> entry : classificaTemp.entrySet()){
                HashSet<String> giocatoriStessoLiv = entry.getValue();
                size += giocatoriStessoLiv.size();
            }
            if(topPlayers > 0){
                // cerco la classifica dei top N giocatori
                if(size < topPlayers){
                    // ho meno giocatori in classifica rispetto alla richiesta
                    posizione = 0;
                    for(Map.Entry<Integer,HashSet<String>> entry : classificaTemp.entrySet()){
                        posizione += 1;
                        HashSet<String> giocatoriStessoLiv = entry.getValue();
                        for (String name : giocatoriStessoLiv){
                            sb.append("Player: ").append(name).append(" - Position: ").append(posizione).append(" - Total Points: ").append(entry.getKey()).append("\n");
                        }
                    }
                    String msg = sb.toString();
                    risposte.setDataMessaggio(33, "numero giocatori partecipanti inferiore a topPlayers, massima classifica generata", msg);
                    String rispostaJson = gson.toJson(risposte); // converto in json la risposta
                    risposte.clearMessaggio(); // pulisco il messaggio per future risposte
                    return rispostaJson;
                }
                else{
                    posizione = 0;
                    boolean limit = false;
                    int numero = 0; // numero effettivo dei giocatori
                    for(Map.Entry<Integer,HashSet<String>> entry : classificaTemp.entrySet()){
                        posizione += 1;
                        HashSet<String> giocatoriStessoLiv = entry.getValue();
                        for (String name : giocatoriStessoLiv){
                            sb.append("Player: ").append(name).append(" - Position: ").append(posizione).append(" - Total Points: ").append(entry.getKey()).append("\n");
                            numero++; // incremento il contatore
                            if(topPlayers == numero) {// verifico di non sforare la richiesta
                                limit = true; // sono arrivato
                                break;
                            }
                        }
                        if (limit) break;
                    }
                    String msg = sb.toString();
                    risposte.setDataMessaggio(34, "classifica topPlayers generata", msg);
                    String rispostaJson = gson.toJson(risposte); // converto in json la risposta
                    risposte.clearMessaggio(); // pulisco il messaggio per future risposte
                    return rispostaJson;
                }
                
            }
            else if(allLeaderboard()){
                // cerco l'intera classifica
                posizione = 0;
                for(Map.Entry<Integer,HashSet<String>> entry : classificaTemp.entrySet()){
                    posizione += 1;
                    HashSet<String> giocatoriStessoLiv = entry.getValue();
                    for (String name : giocatoriStessoLiv){
                        sb.append("Player: ").append(name).append(" - Position: ").append(posizione).append(" - Total Points: ").append(entry.getKey()).append("\n");
                    }
                }
                String msg = sb.toString();
                risposte.setDataMessaggio(30, "intera classifica generata", msg);
                String rispostaJson = gson.toJson(risposte); // converto in json la risposta
                risposte.clearMessaggio(); // pulisco il messaggio per future risposte
                return rispostaJson;
            }
            else{
                // nessun parametro valido fornito
                risposte.setMessaggio(35, "nessun parametro valido fornito per la classifica");
                String rispostaJson = gson.toJson(risposte); // converto in json la risposta
                risposte.clearMessaggio(); // pulisco il messaggio per future risposte
                return rispostaJson;
            }
        }
        
    }
}