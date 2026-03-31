package connections_app;

// creo la classe che implementa la logica dell'aggiornamento delle credenziali

import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;

public class UpdateCredentials {
    private String operation;
    private String oldUsername;
    private String newUsername;
    private String oldPsw;
    private String newPsw;
    public void setOperation(String operation) {
        this.operation = operation;
    }
    public void setOldUsername(String oldUsername) {
        this.oldUsername = oldUsername;
    }
    public void setNewUsername(String newUsername) {
        this.newUsername = newUsername;
    }
    public void setOldPsw(String oldPsw) {
        this.oldPsw = oldPsw;
    }
    public void setNewPsw(String newPsw) {
        this.newPsw = newPsw;
    }
    public String getOperation() {
        return operation;
    }
    public String getOldUsername() {
        return oldUsername;
    }
    public String getNewUsername() {
        return newUsername;
    }
    public String getOldPsw() {
        return oldPsw;
    }
    public String getNewPsw() {
        return newPsw;
    }

    public String doOperation (Player giocatore, ConcurrentHashMap<String,Player> profiliUtenti, ConcurrentHashMap<String, SocketAddress> clientAddresses, Risposte risposte){
        Gson gson = new Gson();
    

        // controlli minimi
        if(oldUsername == null || oldPsw == null){
            risposte.setMessaggio(7, "richiesta non valida, impossibile aggiornare le credenziali");
            String rispostaUpdateJson = gson.toJson(risposte);
            risposte.clearMessaggio();
            return rispostaUpdateJson;
        }

        // se non cambio nulla
        if(newUsername == null && newPsw == null){
            risposte.setMessaggio(8, "nessuna modifica richiesta");
            String rispostaUpdateJson = gson.toJson(risposte);
            risposte.clearMessaggio();
            return rispostaUpdateJson;
        }

        // recupero profilo (evito containsKey+get, che e' ridondante su ConcurrentHashMap)
        Player p = profiliUtenti.get(oldUsername);
        if(p == null){
            // username non esistente
            risposte.setMessaggio(12, "username non esistente, impossibile aggiornare le credenziali");
            String rispostaUpdateJson = gson.toJson(risposte);
            risposte.clearMessaggio();
            return rispostaUpdateJson;
        }

        // Se sono loggato, posso aggiornare SOLO le mie credenziali.
        if(giocatore != null && giocatore.isLoggedIn()){
            String sessionUser = giocatore.getUsername();
            if(sessionUser != null && !sessionUser.equals(oldUsername)){
                risposte.setMessaggio(13, "impossibile aggiornare le credenziali, giocatore non corrispondente");
                String rispostaUpdateJson = gson.toJson(risposte);
                risposte.clearMessaggio();
                return rispostaUpdateJson;
            }
        }

        // Se NON sono loggato, non posso aggiornare le credenziali di un giocatore loggato
        if(giocatore == null || !giocatore.isLoggedIn()){
            if(p.isLoggedIn()){
                risposte.setMessaggio(14, "impossibile aggiornare le credenziali di un giocatore loggato");
                String rispostaUpdateJson = gson.toJson(risposte);
                risposte.clearMessaggio();
                return rispostaUpdateJson;
            }
        }


        // verifica password corrente
        if(p.getPassword() == null || !p.getPassword().equals(oldPsw)){
            risposte.setMessaggio(11, "password errata, impossibile aggiornare le credenziali");
            String rispostaUpdateJson = gson.toJson(risposte);
            risposte.clearMessaggio();
            return rispostaUpdateJson;
        }

        // cambio SOLO password
        if(newUsername == null || newUsername.equals(oldUsername)){
            if(newPsw != null){
                p.setPassword(newPsw);
                risposte.setMessaggio(9, "aggiornamento credenziali avvenuto con successo");
                String rispostaUpdateJson = gson.toJson(risposte);
                risposte.clearMessaggio();
                return rispostaUpdateJson;
            }
            else{
                risposte.setMessaggio(8, "nessuna modifica richiesta");
                String rispostaUpdateJson = gson.toJson(risposte);
                risposte.clearMessaggio();
                return rispostaUpdateJson;
            }
        }

        // cambio username (e forse anche password)

        // inserisco in modo atomico sul nuovo username (riserva)
        Player prev = profiliUtenti.putIfAbsent(newUsername, p);
        if(prev != null){
            // username gia' in uso
            risposte.setMessaggio(10, "username gia' in uso, impossibile aggiornare le credenziali");
            String rispostaUpdateJson = gson.toJson(risposte);
            risposte.clearMessaggio();
            return rispostaUpdateJson;
        }

        // rimuovo in modo condizionale il vecchio mapping (evito di perdere utenti)
        boolean removed = profiliUtenti.remove(oldUsername, p);
        if(!removed){
            // rollback: tolgo il nuovo mapping se e' ancora quello che ho inserito
            profiliUtenti.remove(newUsername, p);
            risposte.setMessaggio(11, "errore durante aggiornamento credenziali");
            String rispostaUpdateJson = gson.toJson(risposte);
            risposte.clearMessaggio();
            return rispostaUpdateJson;
        }

        // ora che lo switch di mapping e' riuscito, aggiorno i campi del player
        p.setUsername(newUsername);
        if(newPsw != null){
            p.setPassword(newPsw);
        }

        // aggiorno anche la mappa degli indirizzi client
        if(clientAddresses != null){
            SocketAddress addr = clientAddresses.remove(oldUsername);
            if(addr != null){
                // non sovrascrivo se esiste gia' un mapping per newUsername
                SocketAddress prevAddr = clientAddresses.putIfAbsent(newUsername, addr);
                if(prevAddr != null){
                    // rollback indirizzo
                    clientAddresses.putIfAbsent(oldUsername, addr);
                }
            }
        }

        risposte.setMessaggio(9, "aggiornamento credenziali avvenuto con successo");
        String rispostaUpdateJson = gson.toJson(risposte);
        risposte.clearMessaggio();
        return rispostaUpdateJson;
    }
}