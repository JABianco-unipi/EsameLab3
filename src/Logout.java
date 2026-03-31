package connections_app;

import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
// cro la classe che implementa il logout del giocatore dal sistema
public class Logout {
    private String operation; // username del giocatore che effettua il logout

    public String getOperation(){
        return this.operation;
    }
    public void setOperation(String operation){
        this.operation = operation;
    }
    public String doOperation(InfoPartita partita, Player giocatore, ConcurrentHashMap<String, SocketAddress> clientAddresses, Risposte risposte){
        // implemento l'operazioone di logout
        Gson gson = new Gson();
        if(giocatore != null && giocatore.isLoggedIn()){
            giocatore.logout(); // setto lo stato di logout del giocatore dalla partita corrente
            clientAddresses.remove(giocatore.getUsername()); // rimuovo l'indirizzo del client dalla mappa degli indirizzi da notificare
            
            risposte.setMessaggio(3, "logout avvenuto con successo"); // setto il messaggio da inviare
            String rispostaJson = gson.toJson(risposte); // converto in json la risposta
            risposte.clearMessaggio(); // pulisco il messaggio per future risposte
            //System.out.println("utente " + giocatore.getUsername() + " ha effettuato il logout");
            //giocatore = null; // resetto il giocatore per permettere un nuovo login
            return rispostaJson;
        }
        else{
            risposte.setMessaggio(4, "errore: utente non loggato"); // setto il messaggio di errore
            String rispostaJson = gson.toJson(risposte); // converto in json la risposta
            risposte.clearMessaggio(); // pulisco il messaggio per future risposte
            return rispostaJson;
        }
    }
}