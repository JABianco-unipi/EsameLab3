/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package connections_app;

// creazione del servere che implementa il comportamento del gioco: CONNECTIONS
// implmentazione del server in multithreading threadpooling


import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class ServerMain {

    public static void main(String[] args){

        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("FileConfigServer.properties")) {
            props.load(fis);
            // carico il nome del file delle partite storiche dal file di configurazione
        } catch (IOException e) {
            System.err.println("Errore nella lettura del file di configurazione");
        }

        ExecutorService pool = Executors.newCachedThreadPool();// metto un cached thread pool perchè voglio gestire tante richieste
        //meglio fare una classe con metodi syncronized per gestire le parole delle partite e anche per memorizzare
        ConcurrentHashMap<String,Player> profiliUtenti = new ConcurrentHashMap<>(); // mappa per memorizzare i profili degli utenti registrati
        WrapperPartita partitaInCorso = new WrapperPartita(); // partita in corso
        ConcurrentHashMap<String, SocketAddress> clientAddresses = new ConcurrentHashMap<>();// devo registrare gli indirizzi dei clienti connessi per mandare la notifica udp di fine partita
        SalvataggioFiles salvataggioFiles = new SalvataggioFiles(props.getProperty("CARTELLA_LOG")); //FILE DI CONFIGURAZIONE
        if (!salvataggioFiles.creaDirectorySalvataggio()){
            System.err.println("Errore nella creazione della cartella di salvataggio dei file di log");
            System.exit(1);
        }


        // prima di tutto devo leggere i file di log e poi ripopolare la hashmap dei profili utenti e delle partite storiche
        // e gestire la terminazione della partita e la creazione di una nuova partita
        ScheduledExecutorService poolLettura = Executors.newSingleThreadScheduledExecutor();

        // creo lo scheduled thread pool per la scrittura periodica dei file di log
        ScheduledExecutorService scheduledPool = Executors.newSingleThreadScheduledExecutor();
        
       
        try(ServerSocket connectionsServer = new ServerSocket(Integer.parseInt(props.getProperty("PORT")))){
            scheduledPool.scheduleAtFixedRate(new FilesLogHandler(partitaInCorso, profiliUtenti, salvataggioFiles), Integer.parseInt(props.getProperty("TIMESTARTLOG")), Integer.parseInt(props.getProperty("TIMECHECKLOG")), TimeUnit.MINUTES); // faccio partire il thread di scrittura ogni TOT minuti
            poolLettura.scheduleAtFixedRate(new PartitaHandler(partitaInCorso, profiliUtenti, clientAddresses, salvataggioFiles, connectionsServer), 0, Integer.parseInt(props.getProperty("TIMEOUT")), TimeUnit.MINUTES); // faccio partire il thread di lettura ogni 10 minuti

            //System.out.println("Server avviato, in ascolto sulla porta " + props.getProperty("PORT"));
            System.out.println("In attesa del parsing/creazione della prima partita...");
            while (true){
                Socket clientSocket = connectionsServer.accept();
                pool.execute(new ConnectionsHandler(clientSocket, partitaInCorso, profiliUtenti, clientAddresses));// creo un nuovo thread per gestire la connessione con il client
            }
        }
        catch(IOException e){
            System.err.println("errore di apertura della server socket" + e.getMessage());
            System.exit(1);
        }
        
        // chiusura dei pool
        pool.shutdown();
        scheduledPool.shutdown();
        poolLettura.shutdown();
        try {
            if(! pool.awaitTermination(10, TimeUnit.SECONDS)){
                pool.shutdownNow();
            }
            if(! scheduledPool.awaitTermination(10, TimeUnit.SECONDS)){
                scheduledPool.shutdownNow();
            }
            if(! poolLettura.awaitTermination(10, TimeUnit.SECONDS)){
                poolLettura.shutdownNow();
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            scheduledPool.shutdownNow();
            poolLettura.shutdownNow();
            System.err.println("Esecuzione interrotta: " + e.getMessage());
            System.exit(1);
        }


    }
}