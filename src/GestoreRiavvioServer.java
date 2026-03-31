package connections_app;

// qui implemento la logca del parsing dei file di log per riavvio del server

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;





public class GestoreRiavvioServer {// gestisce il riavvio del server e il parsing dei file di log
    private final String logFilesPath;
    private final String fileProfili;
    private final String filePartite;
    private final Gson gson = new Gson();


    public GestoreRiavvioServer(){
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("FileConfigServer.properties")) {
            props.load(fis);
            // carico il nome del file delle partite storiche dal file di configurazione
        } catch (IOException e) {
            System.err.println("Errore nella lettura del file di configurazione");
        }
        this.logFilesPath = props.getProperty("CARTELLA_LOG"); // cartella dei file di log
        this.fileProfili = props.getProperty("FILE_PROFILI"); // file dei profili utenti
        this.filePartite = props.getProperty("FILE_PARTITE"); // file delle partite storiche
    }

    public void riavvioProfili(ConcurrentHashMap<String,Player> profiliUtenti){
        // qui apro i file in lettura e ripopolo la mappa dei profili utenti
        Path profiliPath = Paths.get(logFilesPath, fileProfili);// nel file di configurazione il nome del file di log
        File profiliFile = profiliPath.toFile(); // converto in file per controllare l'esistenza
        if (!profiliFile.exists()){
            System.err.println("File di log dei profili utenti non trovato durante il riavvio del server");
            return;
        }
        try(BufferedReader bufferedReader = new BufferedReader(new FileReader(profiliFile))){
            // deserializzo il json nella mappa dei profili utenti
            Type tipoMappa = new TypeToken<ConcurrentHashMap<String, Player>>() {}.getType(); // tipo della mappa da deserializzare
            ConcurrentHashMap<String, Player> profiliLetti = gson.fromJson(bufferedReader, tipoMappa);
            for (String key : profiliLetti.keySet()){
                Player p = profiliLetti.get(key);
                p.register(); // registro il giocatore per inizializzare i campi
                profiliUtenti.put(key, p);
            }
        } catch (IOException e) {
            System.err.println("Errore nella lettura del file di log dei profili utenti durante il riavvio del server");
        }
    }


    public InfoPartita riavviaPartita(){
        // qui apro i file in lettura e ripopolo la mappa delle partite storiche
        InfoPartita partitaCorrente = new InfoPartita();
        // prendo il path della cartella delle partite storiche
        Path CartellaPartitePath = Paths.get(logFilesPath, filePartite); // cartella delle partite storiche FILE DI CONFIGURAZIONE
        File cartellaPartiteFile = CartellaPartitePath.toFile();
        if (!cartellaPartiteFile.exists()){
            System.err.println("Cartella delle partite storiche non trovata durante il riavvio del server");
            return null;
        }
        File[] filesNellaCartella = cartellaPartiteFile.listFiles();
        if (filesNellaCartella.length == 0){
            System.err.println("Nessun file di log delle partite storiche trovato durante il riavvio del server");
            return null;
        }
        // prendo l'ultimo file di log che è rappresentato dall'ultimo id di partita salvato
        Set<Integer> idPartite = new TreeSet<>();
        for(File file : filesNellaCartella){
            String nomeFile = file.getName();// prendo il nome del file
            // estraggo l'id della partita dal nome del file
            String[] partiNome = nomeFile.split("\\."); // splitto per il punto
            if (partiNome.length != 2){
                System.err.println("Nome file di log delle partite storiche non valido: " + nomeFile);
                // elimino il file non valido
                try {
                    file.delete();
                } catch (Exception e) {
                    System.err.println("Errore nella cancellazione del file non valido: " + nomeFile);
                }
                continue;
            }
            try{
                int idPartita = Integer.parseInt(partiNome[0]);
                idPartite.add(idPartita);
            } catch (NumberFormatException e){
                System.err.println("Nome file di log delle partite storiche non valido: " + nomeFile);
            }
        }
        int ultimoIdPartita = ((TreeSet<Integer>)idPartite).last();
        // ora apro il file di log dell'ultima partita
        Path partitaPath = CartellaPartitePath.resolve(ultimoIdPartita + ".json");
        File partitaFile = partitaPath.toFile();
        try(BufferedReader bufferedReader = new BufferedReader(new FileReader(partitaFile))){
            // deserializzo il json nella partita corrente
            partitaCorrente = gson.fromJson(bufferedReader, InfoPartita.class);
            if(partitaCorrente == null){
                System.err.println("Errore nella deserializzazione del file di log della partita durante il riavvio del server");
                return null;
            }
            return partitaCorrente;

        } catch (IOException e) {
            System.err.println("Errore nella lettura del file di log delle partite storiche durante il riavvio del server");
        }
        return partitaCorrente;
    }


    public boolean presenzaProfiliFileLog(){
        // qui controllo la presenza del file di log dei profili utenti
        Path profiliPath = Paths.get(logFilesPath, fileProfili);// nel file di configurazione il nome del file di log
        File profiliFile = profiliPath.toFile(); // converto in file per controllare l'esistenza
        return profiliFile.exists();
    }


    public boolean presenzaPartiteFileLog(){
        // qui controllo la presenza della cartella delle partite storiche
        // e della presenza  di almeno un file di log delle partite
        Path cartellaPartitePath = Paths.get(logFilesPath, filePartite); // cartella delle partite storiche FILE DI CONFIGURAZIONE
        File cartellaPartiteFile = cartellaPartitePath.toFile();
        if (!cartellaPartiteFile.exists() || !cartellaPartiteFile.isDirectory()){
            return false;
        }
        File[] filesNellaCartella = cartellaPartiteFile.listFiles();
        return filesNellaCartella.length != 0;
    }
    
    public InfoPartita recuperoPartita(int gameId){
        // qui controllo la presenza del file di log della partita storica con id specificato
        Path partitaPath = Paths.get(logFilesPath, filePartite, gameId + ".json"); // cartella delle partite storiche FILE DI CONFIGURAZIONE
        File partitaFile = partitaPath.toFile();
        if (!partitaFile.exists() || !partitaFile.isFile()){
            return null;
        }
        try(BufferedReader bufferedReader = new BufferedReader(new FileReader(partitaFile))){
            // deserializzo il json nella partita corrente
            InfoPartita partitaStorica = gson.fromJson(bufferedReader, InfoPartita.class);
            return partitaStorica;
        } catch (IOException e) {
            System.err.println("Errore nella lettura del file di log della partita storica con id " + gameId + " durante il riavvio del server");
        }
        return null;
    }
}