package connections_app;

import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
// creo la classe che implementa la richiesta delle statistiche personali dei giocatori

public class RequestPlayerStats {
    private String operation; // operazione di richiesta statistiche personali

    public String getOperation(){
        return this.operation;
    }
    public void setOperation(String operation){
        this.operation = operation;
    }
    public String doOperation (ConcurrentHashMap<String, Player> profiliUtenti, Player giocatore, Risposte risposte){
        // creo la logica per restituire le statistiche personali del giocatore
        Gson gson = new Gson();
        StringBuilder sb = new StringBuilder();
        if(!profiliUtenti.containsKey(giocatore.getUsername())){
            // giocatore non trovato
            risposte.setMessaggio(40, "giocatore non trovato per le statistiche personali");
            String rispostaJson = gson.toJson(risposte); // converto in json la risposta
            risposte.clearMessaggio(); // pulisco il messaggio per future risposte
            return rispostaJson;
        }
        else{
            // prendo le statistiche del giocatore
            sb.append(giocatore.getPlayerStats());
            String risultatoStatistica = sb.toString();
            risposte.setDataMessaggio(41, "Statistiche personali del giocatore", risultatoStatistica);
            String rispostaJson = gson.toJson(risposte); // converto in json la risposta
            risposte.clearMessaggio(); // pulisco il messaggio per future risposte
            return rispostaJson;
        }
    }
}