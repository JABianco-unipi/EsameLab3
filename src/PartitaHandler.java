package connections_app;

// creazione della classe per gestire parsing delle partite


import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;


public class PartitaHandler implements Runnable{

    private final String fileNamePartite;
    private final String fileNameProfili;

    private final ConcurrentHashMap<String,Player> profiliUtenti;
    private InfoPartita partitaCorrente = null;
    private final WrapperPartita partitaCondivisa;
    private final PartitaParser parserPartita;
    private int gameIdCorrente;
    private int gameIdVecchio;
    private final SalvataggioFiles salvataggioFiles;
    private final ConcurrentHashMap<String, SocketAddress> clientAddresses;
    private final GestoreNotifiche notifiche = new GestoreNotifiche(); // gestore per le notifiche udp ai client
    private final ServerSocket serverSocket;
    private final int tempoPartita;

    private void chiudiServer(){
        try{
            serverSocket.close();
        }
        catch(IOException e){
            System.err.println("Errore nella chiusura della server socket: " + e.getMessage());
        }
    }

    public PartitaHandler(WrapperPartita partitaCondivisa, ConcurrentHashMap<String,Player> profiliUtenti, ConcurrentHashMap<String, SocketAddress> clientAddresses, SalvataggioFiles salvataggioFiles, ServerSocket serverSocket){
        this.partitaCondivisa = partitaCondivisa;
        this.profiliUtenti = profiliUtenti;
        this.clientAddresses = clientAddresses;
        this.salvataggioFiles = salvataggioFiles; // riferimento al salvataggio file
        this.gameIdCorrente = 0;// id della partita corrente
        this.gameIdVecchio = 0;// id della partita appena letta
        this.serverSocket = serverSocket;
        this.parserPartita = new PartitaParser();
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("FileConfigServer.properties")) {
            props.load(fis);
            // carico il nome del file delle partite storiche dal file di configurazione
        } catch (IOException e) {
            System.err.println("Errore nella lettura del file di configurazione");
        }
        this.fileNamePartite = props.getProperty("FILE_PARTITE");
        this.fileNameProfili = props.getProperty("FILE_PROFILI");
        this.tempoPartita = Integer.parseInt(props.getProperty("TIMEOUT")); // tempo partita in minuti

    }

    @Override
    public void run(){
        // controllo dei profili utenti
        if (profiliUtenti.isEmpty()){
            GestoreRiavvioServer riavvio = new GestoreRiavvioServer();
            if(riavvio.presenzaProfiliFileLog()){
                System.out.println("Caricamento file di log dei profili utenti...");
                riavvio.riavvioProfili(profiliUtenti);
            }
            else{
                System.out.println("nessun file di log degli utenti trovato");
            }

        }
        // controllo se la mappa delle partite storiche è vuota
        // in tal caso devo chimare il gestore delle parsing per riavvio del server
        if(partitaCorrente == null){
            GestoreRiavvioServer riavvio = new GestoreRiavvioServer();
            if(riavvio.presenzaPartiteFileLog()){
                System.out.println("Caricamento file di log della partita...");
                partitaCorrente = riavvio.riavviaPartita();// ripristino la partita
                if(partitaCorrente == null){
                    System.err.println("Errore nel riavvio della partita dal file di log");
                    chiudiServer(); // chiudo la socket server
                    return;
                }
                else{
                    gameIdVecchio = gameIdCorrente;
                    gameIdCorrente = partitaCorrente.getGameId();
                    partitaCorrente = parserPartita.parseRecuperoPartita(gameIdCorrente);
                    if(partitaCorrente == null){
                        System.err.println("Errore nel parsing della nuova partita");
                        chiudiServer();
                        return;
                    }
                    else{
                        gameIdVecchio = gameIdCorrente;
                        gameIdCorrente = partitaCorrente.getGameId();
                        partitaCorrente.avviaPartita(tempoPartita); // imposto lo stato della partita a in corso
                        partitaCondivisa.setPartita(partitaCorrente); // la rendo disponibile agli altri thread
                        System.out.println("Partita parsata con successo, id partita: " + partitaCorrente.getGameId());
                    }
                }
                
            }
            else{
                System.out.println("Nessun file di log della partita trovato, avvio normale del server...");
                partitaCorrente = parserPartita.parseNuovaPartita();
                System.out.println("Parsing nuova partita in corso...");
                if(partitaCorrente == null){
                    System.err.println("Errore nel parsing della nuova partita");
                    chiudiServer();
                    return;
                }
                else{
                    gameIdVecchio = gameIdCorrente;
                    gameIdCorrente = partitaCorrente.getGameId();
                    partitaCorrente.avviaPartita(tempoPartita); // imposto lo stato della partita a in corso
                    partitaCondivisa.setPartita(partitaCorrente); // la rendo disponibile agli altri thread
                    System.out.println("Partita parsata con successo, id partita: " + partitaCorrente.getGameId());
                }
            }
        }
        else if(gameIdCorrente - gameIdVecchio == 1){
            // allora devo leggere normalmente la partita parsando la prossima
            System.out.println("Parsing partita in corso...");
            partitaCorrente.terminaPartita(); // termino la partita corrente
            partitaCondivisa.putNull();
            partitaCorrente.aggiornaStatisticheGiocatori(profiliUtenti); // aggiorno le statistiche dei giocatori
            //partitaCorrente.creaClassifica();
            synchronized (salvataggioFiles) {
                if (!salvataggioFiles.salvataggioPartita(fileNamePartite, partitaCorrente)) {
                    System.err.println("Errore nella scrittura del file di log delle partite storiche");
                    chiudiServer();
                    return;
                }
                if (!salvataggioFiles.salvataggioProfiliUtenti(profiliUtenti, fileNameProfili)) { // FILE DI CONFIGURAZIONE
                    System.err.println("Errore nella scrittura del file di log dei profili utenti");
                    chiudiServer();
                    return;
                }
            }
            
            notifiche.inviaNotificaFinePartita(clientAddresses, partitaCorrente); // invio la notifica di fine partita a tutti i client connessi
            partitaCorrente = parserPartita.parseNuovaPartita();

            if(partitaCorrente == null){
                System.err.println("Errore nel parsing della nuova partita");
                chiudiServer();
                return;
            }
            else{
                gameIdVecchio = gameIdCorrente;
                gameIdCorrente = partitaCorrente.getGameId();
                partitaCorrente.avviaPartita(tempoPartita); // imposto lo stato della partita a in corso
                partitaCondivisa.setPartita(partitaCorrente); // la rendo disponibile agli altri thread
                System.out.println("Partita parsata con successo, id partita: " + partitaCorrente.getGameId());
            }
    
        }
        else {
            // significa che devo parsare una nuova partita
            System.out.println("Parsing nuova partita in corso...");
            partitaCorrente.terminaPartita(); // termino la partita corrente
            partitaCondivisa.putNull();
            partitaCorrente.aggiornaStatisticheGiocatori(profiliUtenti); // aggiorno le statistiche dei giocatori
            //partitaCorrente.creaClassifica();
            synchronized (salvataggioFiles) {
                if (!salvataggioFiles.salvataggioPartita(fileNamePartite, partitaCorrente)) {
                    System.err.println("Errore nella scrittura del file di log delle partite storiche");
                    chiudiServer();
                    return;
                }
                if (!salvataggioFiles.salvataggioProfiliUtenti(profiliUtenti, fileNameProfili)) { // FILE DI CONFIGURAZIONE
                    System.err.println("Errore nella scrittura del file di log dei profili utenti");
                    chiudiServer();
                    return;
                }
            }
            
            notifiche.inviaNotificaFinePartita(clientAddresses, partitaCorrente); // invio la notifica di fine partita a tutti i client connessi
            partitaCorrente = parserPartita.parseRecuperoPartita(gameIdCorrente);
            
            if(partitaCorrente == null){
                System.err.println("Errore nel parsing della nuova partita");
                chiudiServer();
                return;
            }
            
            else{
                gameIdVecchio = gameIdCorrente;
                gameIdCorrente = partitaCorrente.getGameId();
                partitaCorrente.avviaPartita(tempoPartita); // imposto lo stato della partita a in corso
                partitaCondivisa.setPartita(partitaCorrente); // la rendo disponibile agli altri thread
                System.out.println("Nuova partita parsata con successo, id partita: " + partitaCorrente.getGameId());
            }

        }
        
    }
}
