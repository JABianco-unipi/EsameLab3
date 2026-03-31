package connections_app;

// questo thread servirà per gestire la scrittura periodica del file di log delle partite e dei giocatori
// girera in uno scheduled thread pool;


import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class FilesLogHandler implements Runnable {
    private final ConcurrentHashMap<String,Player> profiliUtenti;
    private final String fileNameProfili; // file di configurazione
    private final SalvataggioFiles salvataggioFiles;
    
    
    public FilesLogHandler (WrapperPartita partitaCondivisa,ConcurrentHashMap<String,Player> profiliUtenti, SalvataggioFiles salvataggioFiles){
        this.profiliUtenti = profiliUtenti;
        //this.partitaCondivisa = partitaCondivisa;
        this.salvataggioFiles = salvataggioFiles;
        // inizio creando la cartella di salvataggio se non esiste
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("FileConfigServer.properties")) {
            props.load(fis);
            // carico il nome del file delle partite storiche dal file di configurazione
        } catch (IOException e) {
            System.err.println("Errore nella lettura del file di configurazione");
        }
        this.fileNameProfili = props.getProperty("FILE_PROFILI");
    }
    @Override
    public void run(){
        synchronized (salvataggioFiles) {
            if (!salvataggioFiles.salvataggioProfiliUtenti(profiliUtenti, fileNameProfili)){
                System.err.println("Errore nella scrittura del file di log dei profili utenti");
            }
        }


    }
}