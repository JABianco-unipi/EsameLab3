package connections_app;

import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
// creo la classe che implementa la logica della registrazione alla piattaforma
public class Register {
    private String operation;
    private String username;
    private String psw;
    public void setOperation(String operation) {
        this.operation = operation;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public void setPsw(String psw) {
        this.psw = psw;
    }
    public String getOperation() {
        return operation;
    }
    public String getUsername() {
        return username;
    }
    public String getPsw() {
        return psw;
    }
    public String doOperation(Risposte risposta, ConcurrentHashMap<String, Player> profiliUtenti) {
        Gson gson = new Gson();
        if(!profiliUtenti.containsKey(username)){
            // posso registrare l'utente
            Player nuovoGiocatore = new Player(username, psw);
            profiliUtenti.put(username, nuovoGiocatore);
            risposta.setMessaggio(5, "registrazione avvenuta con successo"); // setto il messaggio da inviare
            String rispostaJson = gson.toJson(risposta); // converto in json la risposta
            risposta.clearMessaggio(); // pulisco il messaggio per future risposte
            //System.out.println("nuovo utente registrato: " + username);
            nuovoGiocatore.register();
            return rispostaJson;
        }
        else{
            // username gia esistente
            risposta.setMessaggio(6, "username gia esistente, scegline un altro"); // setto il messaggio da inviare
            String rispostaJson = gson.toJson(risposta); // converto in json la risposta
            risposta.clearMessaggio(); // pulisco il messaggio per future risposte
            //System.err.println("tentativo di registrazione fallito: username " + username + " gia esistente");
            return rispostaJson;
        }
    }
}