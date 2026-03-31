package connections_app;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;

public class Login {
    private String operation;
    private String username;
    private String psw;
    private int portaUdp;

    private transient Player loggedPlayer = null;
    private transient InfoPartita loggedPartita = null;

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
    public int getPortaUdp() {
        return portaUdp;
    }
    public void setPortaUdp(int portaUdp) {
        this.portaUdp = portaUdp;
    }


    // getters per i campi di stato
    public Player getLoggedPlayer() { 
        return loggedPlayer; 
    }
    public InfoPartita getLoggedPartita() { 
        return loggedPartita; 
    }



    public String doOperation( WrapperPartita partitaCondivisa, ConcurrentHashMap<String, Player> profiliUtenti, Risposte risposta, Socket clientSocket, ConcurrentHashMap<String, SocketAddress> clientAddresses) {
        Gson gson = new Gson();
        StringBuilder sb = new StringBuilder();


        if(profiliUtenti.containsKey(username)){
            Player giocatore = profiliUtenti.get(username);
            if(giocatore != null && giocatore.getPassword().equals(psw)){
                // login riuscito

                // Problema di login multipli con lo stesso utente:
                // blocco SOLO se il login arriva da un indirizzo diverso. Se e' lo stesso client, permetto il re-login
                // (utile per auto-login alla nuova partita dopo notifica UDP).

                InetAddress inetClient = clientSocket.getInetAddress();
                SocketAddress newClientAddress = new InetSocketAddress(inetClient, portaUdp);

                if (giocatore.isLoggedIn()) {
                    SocketAddress oldAddress = clientAddresses.get(giocatore.getUsername()); // controllo che l'indirizzo di login sia lo stesso e ce quindi non mi sto loggando da un altro dispositivo
                    if (oldAddress != null && !oldAddress.equals(newClientAddress)) {
                        // utente gia loggato da un altro dispositivo
                        risposta.setMessaggio(2, "utente gia loggato da un altro dispositivo");
                        String rispostaJson = gson.toJson(risposta);
                        risposta.clearMessaggio();
                        //System.err.println("tentativo di login fallito per utente " + username + ": utente gia loggato");
                        return rispostaJson;
                    }
                    // se oldAddress e' null oppure coincide con newClientAddress, considero questo login come re-login
                }

                loggedPartita = partitaCondivisa.getPartita();
                if(loggedPartita == null){
                    // non esiste ancora una partita in corso, riprovo piu tardi a fare un login
                    risposta.setMessaggio(200, "nessuna partita in corso, riprova piu tardi");
                    String rispostaJson = gson.toJson(risposta);
                    risposta.clearMessaggio();
                    //System.err.println("tentativo di login fallito per utente " + username + ": nessuna partita in corso");
                    return rispostaJson;
                }
                giocatore.login(loggedPartita); // setto lo stato di login del giocatore alla partita corrente
                loggedPlayer = giocatore;

                clientAddresses.put(giocatore.getUsername(), newClientAddress); // aggiorno/registro indirizzo per notifiche

                // setto anche il payload da inviare al client
                sb.append(loggedPartita.displayPartita());
                sb.append(loggedPartita.tempoRimanente());
                sb.append("Numero di errori commessi finora: ").append(giocatore.getCurrentMistakes()).append("\n");
                sb.append("Punti attuali: ").append(giocatore.getCurrentPoints()).append("\n");
                String loginPayload = sb.toString();

                risposta.setDataMessaggio(0, "login avvenuto con successo", loginPayload);

                String rispostaJson = gson.toJson(risposta);
                risposta.clearMessaggio();
                return rispostaJson;
            }
            else{
                // password errata
                risposta.setMessaggio(1, "username o password errati"); // setto il messaggio da inviare
                String rispostaJson = gson.toJson(risposta); // converto in json la risposta
                risposta.clearMessaggio(); // pulisco il messaggio per future risposte
                //System.err.println("tentativo di login fallito per utente " + username + ": password errata");
                return rispostaJson;
            }
        }
        else{
            // username non esistente
            risposta.setMessaggio(1, "username o password errati"); // setto il messaggio da inviare
            String rispostaJson = gson.toJson(risposta); // converto in json la risposta
            risposta.clearMessaggio(); // pulisco il messaggio per future risposte
            //System.err.println("tentativo di login fallito per utente " + username + ": username non esistente");
            return rispostaJson;
        }

    
    }
}