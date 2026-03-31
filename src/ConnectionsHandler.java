package connections_app;

// creazione del clienthandler per interazione tra client e e server
// implementazione del clienthandler in multithreading threadpooling
// con BufferedReader e PrintWriter per la comunicazione tra client e server


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ConnectionsHandler implements Runnable {
    private final Socket clientSocket;
    private InfoPartita partita = null; // la inizializzo a null, verrà settata al login
    private final ConcurrentHashMap<String,Player> profiliUtenti;
    private final WrapperPartita partitaCondivisa;
    private final ConcurrentHashMap<String, SocketAddress> clientAddresses;
    private final Risposte risposte = new Risposte();
    private final Gson gson = new Gson();
    private Player giocatore = null; // il giocatore connesso, inizializzato a null

    public ConnectionsHandler (Socket client, WrapperPartita partita, ConcurrentHashMap<String,Player> profiliUtenti, ConcurrentHashMap<String, SocketAddress> clientAddresses){
        this.clientSocket = client;
        this.profiliUtenti = profiliUtenti;
        this.partitaCondivisa = partita;
        this.clientAddresses = clientAddresses;

    }
    @Override
    public void run(){
        try(BufferedReader in =  new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true);){
            
            // entro nel ciclo della partita
            while(true){

                // ora gestisco il ciclo di login/registrazione
                while (giocatore == null || !giocatore.isLoggedIn()) { // rimango nel ciclo di login/registrazione finché il giocatore non è loggato
                    
                    String comando = in.readLine(); // leggo la linea inviata dal client
                    if (comando == null) {
                        // connessione chiusa dal client
                        System.out.println("connessione chiusa dal client durante login/registrazione");
                        return; // esco dal thread
                    }
                    JsonElement richiesta = JsonParser.parseString(comando);
                    // leggo la linea ma uso treemodel api per fare il parsing del json
                    
                    JsonObject richiestaObj = richiesta.getAsJsonObject();
                    // controllo che ci sia l'operazione
                    if (richiestaObj.get("operation") == null || richiestaObj.get("operation").isJsonNull()) {
                        risposte.setMessaggio(100, "parametro operation mancante"); // setto il messaggio da inviare
                        String rispostaOperationInvalidJson = gson.toJson(risposte); // converto in json la risposta
                        risposte.clearMessaggio(); // pulisco il messaggio per future risposte
                        out.println(rispostaOperationInvalidJson); // invio la risposta al client
                        System.out.println("parametro operation mancante nella richiesta di login/registrazione");
                        continue; // rimango nel ciclo di login/registrazione
                    }
                    String operazione = richiestaObj.get("operation").getAsString();

                    switch (operazione){
                        case "login":
                            Login opLogin = new Login();
                            opLogin.setOperation(operazione);

                            // voglio fare controllo su null pointer exception
                            if (richiestaObj.get("username") == null || richiestaObj.get("username").isJsonNull() 
                                || richiestaObj.get("psw") == null || richiestaObj.get("psw").isJsonNull() 
                                || richiestaObj.get("portaUdp") == null || richiestaObj.get("portaUdp").isJsonNull()) {

                                risposte.setMessaggio(101, "parametri di login mancanti"); // setto il messaggio da inviare
                                String rispostaLoginInvalidJson = gson.toJson(risposte); // converto in json la risposta
                                risposte.clearMessaggio(); // pulisco il messaggio per future risposte
                                out.println(rispostaLoginInvalidJson); // invio la risposta al client
                                continue; // rimango nel ciclo di login/registrazione
                            }

                            opLogin.setUsername(richiestaObj.get("username").getAsString());
                            opLogin.setPsw(richiestaObj.get("psw").getAsString());
                            // nel Json di login ho anche la porta udp del client
                            opLogin.setPortaUdp(richiestaObj.get("portaUdp").getAsInt());
                            String rispostaLoginJson = opLogin.doOperation(partitaCondivisa, profiliUtenti, risposte, clientSocket, clientAddresses);
                            out.println(rispostaLoginJson); // invio la risposta al client
                            // Se il login e' andato a buon fine, aggiorno lo stato del handler
                            if (opLogin.getLoggedPlayer() != null) {
                                this.giocatore = opLogin.getLoggedPlayer();
                                this.partita = opLogin.getLoggedPartita();
                            }
                            continue; // rimango nel ciclo di login/registrazione
                            
                        case "register":
                            Register opRegister = new Register();
                            opRegister.setOperation(operazione);
                            // controllo su null pointer exception
                            if (richiestaObj.get("username") == null || richiestaObj.get("username").isJsonNull() 
                                || richiestaObj.get("psw") == null || richiestaObj.get("psw").isJsonNull()) {
                                
                                risposte.setMessaggio(102, "parametri di registrazione mancanti"); // setto il messaggio da inviare
                                String rispostaRegisterInvalidJson = gson.toJson(risposte); // converto in json la risposta
                                risposte.clearMessaggio(); // pulisco il messaggio per future risposte
                                out.println(rispostaRegisterInvalidJson); // invio la risposta al client
                                continue; // rimango nel ciclo di login/registrazione
                            }

                            opRegister.setUsername(richiestaObj.get("username").getAsString());
                            opRegister.setPsw(richiestaObj.get("psw").getAsString());
                            String rispostaRegisterJson = opRegister.doOperation(risposte, profiliUtenti);
                            out.println(rispostaRegisterJson); // invio la risposta al client
                            continue; // rimango nel ciclo di login/registrazione

                        case "updateCredentials":
                            UpdateCredentials opUpdate = new UpdateCredentials();
                            opUpdate.setOperation(operazione);
                            // voglio fare controllo su null pointer exception
                            if (richiestaObj.get("oldUsername") == null || richiestaObj.get("oldUsername").isJsonNull()
                                || richiestaObj.get("oldPsw") == null || richiestaObj.get("oldPsw").isJsonNull()) {

                                risposte.setMessaggio(103, "parametri updateCredentials mancanti");
                                String rispostaUpdateInvalidJson = gson.toJson(risposte);
                                risposte.clearMessaggio();
                                out.println(rispostaUpdateInvalidJson);
                                continue;
                            }
                            opUpdate.setOldUsername(richiestaObj.get("oldUsername").getAsString());
                            JsonElement newUsername = richiestaObj.get("newUsername"); // prendo il jsonelement cosi da controllare se è null
                            if(newUsername != null && !newUsername.isJsonNull()){
                                opUpdate.setNewUsername(newUsername.getAsString());
                            }
                            opUpdate.setOldPsw(richiestaObj.get("oldPsw").getAsString());
                            JsonElement newPsw = richiestaObj.get("newPsw"); // prendo il jsonelement cosi da controllare se è null
                            if(newPsw != null && !newPsw.isJsonNull()){
                                opUpdate.setNewPsw(newPsw.getAsString());
                            }
                            String rispostaUpdateJson = opUpdate.doOperation(giocatore, profiliUtenti, clientAddresses, risposte);
                            out.println(rispostaUpdateJson); // invio la risposta al client
                            continue; // rimango nel ciclo di login/registrazione
                            
                        default:
                            // operazione non valida
                            risposte.setMessaggio(104, "operazione non valida"); // setto il messaggio da inviare
                            String rispostaInvalidJson = gson.toJson(risposte); // converto in json la risposta
                            risposte.clearMessaggio(); // pulisco il messaggio per future risposte
                            out.println(rispostaInvalidJson); // invio la risposta al client
                            System.out.println("operazione non valida richiesta dal client durante login/registrazione");   
                            
                    }
                
                    
                    
                }
                // esco dal ciclo di login/registrazione e procedo con la gestione della partita
                while(giocatore.isLoggedIn()){
                    // gestione della partita da implementare
                    
                    String comandoPartita = in.readLine(); // leggo la linea inviata dal client
                    if (comandoPartita == null) {
                        // connessione chiusa dal client
                        clientAddresses.remove(giocatore.getUsername()) ;// rimuovo l'indirizzo del client dalla mappa degli indirizzi da notificare
                        giocatore.logout(); // faccio il logout del giocatore
                        System.out.println("connessione chiusa dal client durante la partita");
                        return; // esco dal thread
                    }
                    JsonElement richiestaPartita = JsonParser.parseString(comandoPartita);
                    // leggo la linea ma uso treemodel api per fare il parsing del json
                    JsonObject richiestaPartitaObj = richiestaPartita.getAsJsonObject();
                    // controllo che ci sia l'operazione
                    if (richiestaPartitaObj.get("operation") == null || richiestaPartitaObj.get("operation").isJsonNull()) {
                        risposte.setMessaggio(100, "parametro operation mancante"); // setto il messaggio da inviare
                        String rispostaOperationInvalidJson = gson.toJson(risposte); // converto in json la risposta
                        risposte.clearMessaggio(); // pulisco il messaggio per future risposte
                        out.println(rispostaOperationInvalidJson); // invio la risposta al client
                        System.out.println("parametro operation mancante nella richiesta della partita");
                        continue; // rimango nel ciclo della partita
                    }

                    String operazionePartita = richiestaPartitaObj.get("operation").getAsString();
                    // implemento il login automatico alla nuova partita se c'è stato un cambio partita
                    if (partita == null || partita.partitaTerminata() || partitaCondivisa.isNull()){
                        
                        if(operazionePartita.equals("login")) {
                            Login opLogin = new Login();
                            opLogin.setOperation(operazionePartita);
                            // voglio fare controllo su null pointer exception
                            
                            if (richiestaPartitaObj.get("username") == null || richiestaPartitaObj.get("username").isJsonNull() 
                                || richiestaPartitaObj.get("psw") == null || richiestaPartitaObj.get("psw").isJsonNull() 
                                || richiestaPartitaObj.get("portaUdp") == null || richiestaPartitaObj.get("portaUdp").isJsonNull()) {
                                    
                                risposte.setMessaggio(101, "parametri di login mancanti"); // setto il messaggio da inviare
                                String rispostaLoginInvalidJson = gson.toJson(risposte); // converto in json la risposta
                                risposte.clearMessaggio(); // pulisco il messaggio per future risposte
                                out.println(rispostaLoginInvalidJson); // invio la risposta al client
                                continue; // rimango nel ciclo della partita
                            }
                            
                            opLogin.setUsername(richiestaPartitaObj.get("username").getAsString());
                            opLogin.setPsw(richiestaPartitaObj.get("psw").getAsString());
                            // nel Json di login ho anche la porta udp del client
                            opLogin.setPortaUdp(richiestaPartitaObj.get("portaUdp").getAsInt());
                            String rispostaLoginJson = opLogin.doOperation( partitaCondivisa, profiliUtenti, risposte, clientSocket, clientAddresses);
                            if (opLogin.getLoggedPlayer() != null) {
                                this.giocatore = opLogin.getLoggedPlayer();
                                this.partita = opLogin.getLoggedPartita();
                            }
                            out.println(rispostaLoginJson); // invio la risposta al client
                        }

                        else if (operazionePartita.equals("logout")) {
                            Logout opLogout = new Logout();
                            opLogout.setOperation(operazionePartita);
                            String rispostaLogoutJson = opLogout.doOperation(partita, giocatore, clientAddresses, risposte);
                            out.println(rispostaLogoutJson);
                        }
                        else {
                            // Qualsiasi altra operazione durante fine partita/transizione
                            // (es. requestGameInfo -1) deve ricevere SEMPRE una risposta.
                            risposte.setMessaggio(254, "sessione scaduta, effettua il login o logout");
                            String rispostaFinePartita = gson.toJson(risposte);
                            risposte.clearMessaggio();
                            out.println(rispostaFinePartita);
                        }
                        
                    }

                    else{
                        switch (operazionePartita){
                            case "logout":
                                // gestisco il logout del giocatore
                                Logout opLogout = new Logout();
                                opLogout.setOperation(operazionePartita);
                                // eseguo l'operazione di logout
                                String rispostaLogoutJson = opLogout.doOperation(partita, giocatore, clientAddresses, risposte);
                                out.println(rispostaLogoutJson); // invio la risposta al client
                                continue; // esco dal ciclo della partita e torno al ciclo di login/registrazione

                            case "submitProposal":
                                // gestisco l'invio del risultato del giocatore
                                SubmitProposal opSubmit = new SubmitProposal();
                                opSubmit.setOperation(operazionePartita);
                                // controllo che ci siano le parole proposte

                                if (richiestaPartitaObj.get("words") == null || richiestaPartitaObj.get("words").isJsonNull()) {
                                    risposte.setMessaggio(105, "parametri updateProposal mancanti"); // setto il messaggio da inviare
                                    String rispostaSubmitInvalidJson = gson.toJson(risposte); // converto in json la risposta
                                    risposte.clearMessaggio(); // pulisco il messaggio per future risposte
                                    out.println(rispostaSubmitInvalidJson); // invio la risposta al client
                                    System.out.println("parametri mancanti nella submit proposta");
                                    continue; // rimango nel ciclo della partita
                                }


                                // prendo le parole proposte
                                JsonArray wordsArray = richiestaPartitaObj.getAsJsonArray("words");
                                HashSet<String> wordsProposte = new HashSet<>();
                                for(JsonElement wordElem : wordsArray){
                                    wordsProposte.add(wordElem.getAsString().toUpperCase()); // le salvo in maiuscolo per uniformità
                                }
                                opSubmit.setWords(wordsProposte);
                                // eseguo l'operazione di submit proposta
                                String rispostaSubmitJson = opSubmit.doOperation(partita, giocatore, risposte);
                                out.println(rispostaSubmitJson); // invio la risposta al client
                                continue; // rimango nel ciclo della partita

                            case "requestGameInfo":
                                // gestisco la richiesta di informazioni sulla partita
                                RequestGameInfo opRequest = new RequestGameInfo();
                                opRequest.setOperation(operazionePartita);


                                // voglio fare il controllo che il gameId sia presente
                                if (richiestaPartitaObj.get("gameId") == null || richiestaPartitaObj.get("gameId").isJsonNull()) {
                                    risposte.setMessaggio(106, "parametro requestGameInfo mancante"); // setto il messaggio da inviare
                                    String rispostaRequestInvalidJson = gson.toJson(risposte); // converto in json la risposta
                                    risposte.clearMessaggio(); // pulisco il messaggio per future risposte
                                    out.println(rispostaRequestInvalidJson); // invio la risposta al client
                                    continue; // rimango nel ciclo della partita
                                }


                                // eseguo l'operazione di richiesta info partita
                                opRequest.setGameId(richiestaPartitaObj.get("gameId").getAsInt());
                                
                                String rispostaRequestJson = opRequest.doOperation(partita, giocatore, risposte);
                                out.println(rispostaRequestJson); // invio la risposta al client
                                continue; // rimango nel ciclo della partita


                            case "requestGameStats":
                                // gestisco la richiesta di statistiche della partita
                                RequestGameStats opStats = new RequestGameStats();
                                opStats.setOperation(operazionePartita);

                                
                                // voglio fare il controllo che il gameId sia presente
                                if (richiestaPartitaObj.get("gameId") == null || richiestaPartitaObj.get("gameId").isJsonNull()) {
                                    risposte.setMessaggio(107, "parametro requestGameStats mancante"); // setto il messaggio da inviare
                                    String rispostaStatsInvalidJson = gson.toJson(risposte); // converto in json la risposta
                                    risposte.clearMessaggio(); // pulisco il messaggio per future risposte
                                    out.println(rispostaStatsInvalidJson); // invio la risposta al client
                                    continue; // rimango nel ciclo della partita
                                }


                                opStats.setGameId(richiestaPartitaObj.get("gameId").getAsInt());
                                // eseguo l'operazione di richiesta statistiche partita
                                String rispostaStatsJson = opStats.doOperation(partita, giocatore, risposte);
                                out.println(rispostaStatsJson); // invio la risposta al client
                                continue; // rimango nel ciclo della partita


                            case "requestLeaderboard":
                                // gestisco la richiesta della classifica
                                RequestLeaderBoard opLeaderboard = new RequestLeaderBoard();
                                opLeaderboard.setOperation(operazionePartita);
                                // controllo che se nella richiesta c'è il playername
                                JsonElement playerNameElem = richiestaPartitaObj.get("playerName"); // prendo il jsonelement cosi da controllare se è null
                                if(playerNameElem != null && !playerNameElem.isJsonNull()){
                                    opLeaderboard.setPlayerName(playerNameElem.getAsString());
                                }
                                else {
                                    // controllo se c'è il topPlayers
                                    if (richiestaPartitaObj.get("topPlayers") == null || richiestaPartitaObj.get("topPlayers").isJsonNull()) {
                                        risposte.setMessaggio(108, "parametri requestLeaderboard mancanti"); // setto il messaggio da inviare
                                        String rispostaLeaderboardInvalidJson = gson.toJson(risposte); // converto in json la risposta
                                        risposte.clearMessaggio(); // pulisco il messaggio per future risposte
                                        out.println(rispostaLeaderboardInvalidJson); // invio la risposta al client
                                        continue; // rimango nel ciclo della partita
                                    }
                                    else{
                                        opLeaderboard.setTopPlayers(richiestaPartitaObj.get("topPlayers").getAsInt());
                                    }
                                }
                                String rispostaLeaderboardJson = opLeaderboard.doOperation(profiliUtenti, risposte);
                                out.println(rispostaLeaderboardJson); // invio la risposta al client
                                continue; // rimango nel ciclo della partita

                            case "requestPlayerStats":
                                // gestisco la richiesta delle statistiche del giocatore
                                RequestPlayerStats opPlayerStats = new RequestPlayerStats();
                                opPlayerStats.setOperation(operazionePartita);
                                // eseguo l'operazione di richiesta statistiche giocatore
                                String rispostaPlayerStatsJson = opPlayerStats.doOperation(profiliUtenti, giocatore, risposte);
                                out.println(rispostaPlayerStatsJson); // invio la risposta al client
                                continue; // rimango nel ciclo della partita


                            case "updateCredentials":
                                UpdateCredentials opUpdate = new UpdateCredentials();
                                opUpdate.setOperation(operazionePartita);
                                // voglio fare controllo su null pointer exception
                                if (richiestaPartitaObj.get("oldUsername") == null || richiestaPartitaObj.get("oldUsername").isJsonNull()
                                    || richiestaPartitaObj.get("oldPsw") == null || richiestaPartitaObj.get("oldPsw").isJsonNull()) {

                                    risposte.setMessaggio(103, "parametri updateCredentials mancanti");
                                    String rispostaUpdateInvalidJson = gson.toJson(risposte);
                                    risposte.clearMessaggio();
                                    out.println(rispostaUpdateInvalidJson);
                                    continue;
                                }
                                
                                opUpdate.setOldUsername(richiestaPartitaObj.get("oldUsername").getAsString());
                                JsonElement newUsername = richiestaPartitaObj.get("newUsername"); // prendo il jsonelement cosi da controllare se è null
                                if(newUsername != null && !newUsername.isJsonNull()){
                                    opUpdate.setNewUsername(newUsername.getAsString());
                                }
                                opUpdate.setOldPsw(richiestaPartitaObj.get("oldPsw").getAsString());
                                JsonElement newPsw = richiestaPartitaObj.get("newPsw"); // prendo il jsonelement cosi da controllare se è null
                                if(newPsw != null && !newPsw.isJsonNull()){
                                    opUpdate.setNewPsw(newPsw.getAsString());
                                }
                                String rispostaUpdateJson = opUpdate.doOperation(giocatore, profiliUtenti, clientAddresses, risposte);
                                out.println(rispostaUpdateJson); // invio la risposta al client
                                continue; // rimango nel ciclo della partita

                            default:
                                // operazione non valida
                                risposte.setMessaggio(104, "operazione non valida"); // setto il messaggio da inviare
                                String rispostaJsonDefault = gson.toJson(risposte); // converto in json la risposta
                                risposte.clearMessaggio(); // pulisco il messaggio per future risposte
                                out.println(rispostaJsonDefault); // invio la risposta al client
                                System.out.println("operazione non valida richiesta dal client durante la partita");
                        }
                    }

                }

            }

        }
        catch(IOException e){
            System.err.println("errore di comunicazione con il client " + e.getMessage());
        }
        finally{
            try{
                clientSocket.close();
            }
            catch(IOException e){
                System.err.println("errore nella chiusura della socket del client " + e.getMessage());
            }
        }

    }

}