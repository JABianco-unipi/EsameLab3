package connections_app;

// creazione della classe per gestione dei comandi ricevuti dal client

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashSet;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.gson.Gson;


public class ClientHandlerComandi implements Runnable {
    private final BlockingQueue<String> commandQueue; // coda dei comandi ricevuti dal client per poter gestirli in modo sincrono
    private final Selector selector; // per svegliare il thread NIO quando arriva un comando
    private final Gson json = new Gson();
    private final SelectionKey selectedKey;
    private final int udpPort;

    private final Login managerLogin;
    private final UpdateCredentials managerCredentials;
    private final AtomicBoolean loggedIn;

    public ClientHandlerComandi(BlockingQueue<String> commandQueue, Selector selector, SelectionKey selectedKey, int udpPort, Login managerLogin, UpdateCredentials managerCredentials, AtomicBoolean loggedIn) {
        this.commandQueue = commandQueue;
        this.selector = selector;
        this.selectedKey = selectedKey;
        this.udpPort = udpPort;
        this.managerLogin = managerLogin;
        this.managerCredentials = managerCredentials;
        this.loggedIn = loggedIn;
    }
    @Override
    public void run() {

        try (Scanner scanner = new Scanner(System.in)) {
            while(true){
                String message = scanner.nextLine();
                if (message.equalsIgnoreCase("help")) {
                    System.out.println("""
                            Comandi disponibili:
                            register <username> <password>
                            login <username> <password>
                            updateCredentials <oldUsername> <oldPassword> <newUsername> <newPassword>
                            logout
                            submitProposal <primaParola> <secondaParola> <terzaParola> <quartaParola>
                            requestGameInfo <gameID> oppure requestGameInfo <-1> per la partita corrente
                            requestGameStats <gameID> oppure requestGameStats <-1> per la partita corrente
                            requestLeaderboard <PlayerUsername> oppure requestLeaderboard <NtopPlayers> per sapere i primi N oppure requestLeaderboard <-1> per tutta la classifica
                            requestPlayerStats
                            exit""");
                    continue;
                }
                else if (message.equals("exit")){
                    System.out.println("Chiusura del client...");
                    commandQueue.put("exit");
                    selectedKey.interestOps(selectedKey.interestOps() | SelectionKey.OP_WRITE);
                    selector.wakeup(); // sveglio il thread NIO per gestire la scrittura
                    break;
                }
                

                String[] messageSplit = message.trim().split(" ");
                String command = messageSplit[0];

                if(!loggedIn.get()) {

                    if (command.equals("register")){
                        if (messageSplit.length != 3){
                            System.out.println("Comando register non valido. Uso corretto: register <username> <password>");
                            continue;
                        }
                        Register register = new Register();
                        register.setOperation(command);
                        register.setUsername(messageSplit[1]);
                        register.setPsw(messageSplit[2]);
                        String registerJson = json.toJson(register) + "\n";
                        commandQueue.put(registerJson);
                        selectedKey.interestOps(selectedKey.interestOps() | SelectionKey.OP_WRITE);
                        selector.wakeup(); // sveglio il thread NIO per gestire la scrittura
                        //System.out.println("Comando accodato: " + registerJson); --> per vedere cosa viene inviato
                    }



                    else if (command.equals("login")){
                        if (messageSplit.length != 3){
                            System.out.println("Comando login non valido. Uso corretto: login <username> <password>");
                            continue;
                        }
                        managerLogin.setOperation(command);
                        managerLogin.setUsername(messageSplit[1]);
                        managerLogin.setPsw(messageSplit[2]);
                        managerLogin.setPortaUdp(udpPort); // devo settare un indirizzo udp fittizio per inviarlo al server
                        String loginJson = json.toJson(managerLogin) + "\n";
                        commandQueue.put(loginJson);
                        selectedKey.interestOps(selectedKey.interestOps() | SelectionKey.OP_WRITE);
                        selector.wakeup(); // sveglio il thread NIO per gestire la scrittura
                        //System.out.println("Comando accodato: " + loginJson); --> per vedere cosa viene inviato
                    }



                    else if (command.equals("updateCredentials")){
                        if (messageSplit.length != 5){
                            System.out.println("""
                                    Comando updateCredentials non valido. 
                                    Uso corretto: updateCredentials <oldUsername> <oldPassword> <newPassword> <->
                                    oppure updateCredentials <oldUsername> <oldPassword> <-> <newUsername>
                                    oppure updateCredentials <oldUsername> <oldPassword> <newUsername> <newPassword>""");
                            continue;
                        }
                        // il formato tiene conto che i primi 2 sono oldUsername e OldPassword
                        managerCredentials.setOperation(command);
                        // Se hai i setter, decommenta queste due righe:
                        managerCredentials.setOldUsername(messageSplit[1]);
                        managerCredentials.setOldPsw(messageSplit[2]);
                        if (messageSplit[3].equals("-") && messageSplit[4].equals("-")){
                            // nessun cambiamento
                            managerCredentials.setNewUsername(null);
                            managerCredentials.setNewPsw(null);
                        }
                        else if (messageSplit[3].equals("-") && !messageSplit[4].equals("-")){
                            // cambio solo la password
                            managerCredentials.setNewUsername(null);
                            managerCredentials.setNewPsw(messageSplit[4]);
                        }
                
                        else if (messageSplit[4].equals("-") && !messageSplit[3].equals("-")){
                            // cambio solo il nome utente
                            managerCredentials.setNewUsername(messageSplit[3]);
                            managerCredentials.setNewPsw(null);
                        }
                        else{
                            // cambio entrambi
                            managerCredentials.setNewUsername(messageSplit[3]);
                            managerCredentials.setNewPsw(messageSplit[4]);
                        }
                        String credentialsJson = json.toJson(managerCredentials) + "\n";
                        commandQueue.put(credentialsJson);
                        selectedKey.interestOps(selectedKey.interestOps() | SelectionKey.OP_WRITE);
                        selector.wakeup(); // sveglio il thread NIO per gestire la scrittura
                        //System.out.println("Comando accodato: " + credentialsJson); --> per vedere cosa viene inviato
                        
                    }
                    else if(command.equals("logout") || command.equals("submitProposal") || command.equals("requestGameInfo") ||
                             command.equals("requestGameStats") || command.equals("requestLeaderboard") ||
                             command.equals("requestPlayerStats")){
                        System.out.println("Devi prima effettuare il login per poter eseguire questo comando.");

                    }
                    
                    else {
                        System.out.println("Comando non riconosciuto: " +command);
                    }
                }
                else {

                    if (command.equals("logout")){
                        if (messageSplit.length != 1){
                            System.out.println("Comando logout non valido. Uso corretto: logout");
                            continue;
                        }
                        Logout logout = new Logout();
                        logout.setOperation(command);
                        String logoutJson = json.toJson(logout) + "\n";
                        commandQueue.put(logoutJson);
                        selectedKey.interestOps(selectedKey.interestOps() | SelectionKey.OP_WRITE);
                        selector.wakeup(); // sveglio il thread NIO per gestire la scrittura
                        //System.out.println("Comando accodato: " + logoutJson); --> per vedere cosa vine inviato
                    }


                    else if (command.equals("submitProposal")){
                        if(messageSplit.length != 5){
                            System.out.println("Comando submitProposal non valido. Uso corretto: submitProposal <primaParola> <secondaParola> <terzaParola> <quartaParola>");
                            continue;
                        }
                        // levo gli underscore dalle parole che possono essere composte da spazi

                        SubmitProposal proposal = new SubmitProposal();
                        proposal.setOperation(command);
                        proposal.setWords(new HashSet<>());
                        // levo gli underscore dalle parole composte da spazi e aggiungo gli spazi
                        proposal.addWord(messageSplit[1].replace("_", " "));
                        proposal.addWord(messageSplit[2].replace("_", " "));
                        proposal.addWord(messageSplit[3].replace("_", " "));
                        proposal.addWord(messageSplit[4].replace("_", " "));
                        String proposalJson = json.toJson(proposal) + "\n";
                        commandQueue.put(proposalJson);
                        selectedKey.interestOps(selectedKey.interestOps() | SelectionKey.OP_WRITE);
                        selector.wakeup(); // sveglio il thread NIO per gestire la scrittura
                        //System.out.println("Comando accodato: " + proposalJson); --> per vedere cosa viene inviato
                    }




                    else if (command.equals("requestGameInfo")){
                        if (messageSplit.length != 2){
                            System.out.println("Comando requestGameInfo non valido. Uso corretto: requestGameInfo <gameID> oppure requestGameInfo <-1> per la partita corrente");
                            continue;
                        }

                        int gameId;
                        try {
                            gameId = Integer.parseInt(messageSplit[1]);
                            if (gameId < -1) {
                                System.out.println("gameId deve essere un numero intero (es. 0, 1, 2, ... oppure -1).");
                                continue;
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("gameId deve essere un numero intero (es. 0, 1, 2, ... oppure -1).");
                            continue;
                        }

                        RequestGameInfo request = new RequestGameInfo();
                        request.setOperation(command);
                        request.setGameId(gameId);
                        String requestJson = json.toJson(request) + "\n";
                        commandQueue.put(requestJson);
                        selectedKey.interestOps(selectedKey.interestOps() | SelectionKey.OP_WRITE);
                        selector.wakeup(); // sveglio il thread NIO per gestire la scrittura
                        //System.out.println("Comando accodato: " + requestJson); --> per vedere cosa viene inviato
                    }



                    else if(command.equals("requestGameStats")){
                        if (messageSplit.length != 2){
                            System.out.println("Comando requestGameStats non valido. Uso corretto: requestGameStats <gameID> oppure requestGameStats <-1> per la partita corrente");
                            continue;
                        }

                        int gameId;
                        try {
                            gameId = Integer.parseInt(messageSplit[1]);
                            if (gameId < -1) {
                                System.out.println("gameId deve essere un numero intero (es. 0, 1, 2, ... oppure -1).");
                                continue;
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("gameId deve essere un numero intero (es. 0, 1, 2, ... oppure -1).");
                            continue;
                        }

                        RequestGameStats requestStats = new RequestGameStats();
                        requestStats.setOperation(command);
                        requestStats.setGameId(gameId);
                        String requestStatsJson = json.toJson(requestStats) + "\n";
                        commandQueue.put(requestStatsJson);
                        selectedKey.interestOps(selectedKey.interestOps() | SelectionKey.OP_WRITE);
                        selector.wakeup(); // sveglio il thread NIO per gestire la scrittura
                        //System.out.println("Comando accodato: " + requestStatsJson); --> per vedere cosa viene inviato
                    }


                    else if(command.equals("requestLeaderboard")){
                        if(messageSplit.length != 2){
                            System.out.println("""
                                        Comando requestLeaderboard non valido. 
                                        Uso corretto: requestLeaderboard <PlayerUsername>
                                        oppure requestLeaderboard <NtopPlayers> per sapere i primi N 
                                        oppure requestLeaderboard <-1> per tutta la classifica""");
                            continue;
                        }

                        RequestLeaderBoard leaderboard = new RequestLeaderBoard();
                        leaderboard.setOperation(command);

                        // Se e' un intero: -1 (tutta), oppure N>0 (top N). Altrimenti e' un username.
                        try {
                            int value = Integer.parseInt(messageSplit[1]);
                            if (value == -1) {
                                leaderboard.setTopPlayers(-1);
                            } else if (value > 0) {
                                leaderboard.setTopPlayers(value);
                            } else {
                                // 0 o negativi diversi da -1: li tratto come input non valido
                                System.out.println("Valore numerico non valido. Usa -1 oppure un intero > 0, oppure inserisci un username.");
                                continue;
                            }
                        } catch (NumberFormatException e){
                            leaderboard.setPlayerName(messageSplit[1]);
                        }

                        String leaderboardJson = json.toJson(leaderboard) + "\n";
                        commandQueue.put(leaderboardJson);
                        selectedKey.interestOps(selectedKey.interestOps() | SelectionKey.OP_WRITE);
                        selector.wakeup(); // sveglio il thread NIO per gestire la scrittura
                        //System.out.println("Comando accodato: " + leaderboardJson); --> per vedere cosa viene inviato
                    }


                    else if(command.equals("requestPlayerStats")){
                        if(messageSplit.length != 1){
                            System.out.println("Comando requestPlayerStats non valido. Uso corretto: requestPlayerStats");
                            continue;
                        }
                        RequestPlayerStats playerStats = new RequestPlayerStats();
                        playerStats.setOperation(command);
                        String playerStatsJson = json.toJson(playerStats) + "\n";
                        commandQueue.put(playerStatsJson);
                        selectedKey.interestOps(selectedKey.interestOps() | SelectionKey.OP_WRITE);
                        selector.wakeup(); // sveglio il thread NIO per gestire la scrittura
                        //System.out.println("Comando accodato: " + playerStatsJson); --> per vedere cosa viene inviato
                    }

                    else if (command.equals("updateCredentials")){
                        if (messageSplit.length != 5){
                            System.out.println("""
                                    Comando updateCredentials non valido. 
                                    Uso corretto: updateCredentials <oldUsername> <oldPassword> <newPassword> <->
                                    oppure updateCredentials <oldUsername> <oldPassword> <-> <newUsername>
                                    oppure updateCredentials <oldUsername> <oldPassword> <newUsername> <newPassword>""");
                            continue;
                        }
                        // il formato tiene conto che i primi 2 sono oldUsername e OldPassword
                        managerCredentials.setOperation(command);
                        // Se hai i setter, decommenta queste due righe:
                        managerCredentials.setOldUsername(messageSplit[1]);
                        managerCredentials.setOldPsw(messageSplit[2]);
                        if (messageSplit[3].equals("-") && messageSplit[4].equals("-")){
                            // nessun cambiamento
                            managerCredentials.setNewUsername(null);
                            managerCredentials.setNewPsw(null);
                        }
                        else if (messageSplit[3].equals("-") && !messageSplit[4].equals("-")){
                            // cambio solo la password
                            managerCredentials.setNewUsername(null);
                            managerCredentials.setNewPsw(messageSplit[4]);
                        }
                
                        else if (messageSplit[4].equals("-") && !messageSplit[3].equals("-")){
                            // cambio solo il nome utente
                            managerCredentials.setNewUsername(messageSplit[3]);
                            managerCredentials.setNewPsw(null);
                        }
                        else{
                            // cambio entrambi
                            managerCredentials.setNewUsername(messageSplit[3]);
                            managerCredentials.setNewPsw(messageSplit[4]);
                        }
                        String credentialsJson = json.toJson(managerCredentials) + "\n";
                        commandQueue.put(credentialsJson);
                        selectedKey.interestOps(selectedKey.interestOps() | SelectionKey.OP_WRITE);
                        selector.wakeup(); // sveglio il thread NIO per gestire la scrittura
                        //System.out.println("Comando accodato: " + credentialsJson); --> per vedere cosa viene inviato
                        
                    }

                    else {
                        System.out.println("Comando non riconosciuto: " + command);
                    }
                }

            }

        }
        catch (Exception e) {
            // Scanner lancia NoSuchElementException quando System.in è chiuso
            // Chiusura normale del client
            System.out.println("Input chiuso. Terminazione del client.");
            System.exit(1);
        }
    }

}