package connections_app;

// creo la classe per gestire la memorizzazione dei file di salvataggio delle partite e dei profili utenti
import java.io.IOException;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;

public class SalvataggioFiles{
    private final String logFilesPath;

    public SalvataggioFiles(String logFilesPath){
        this.logFilesPath = logFilesPath;
    }
    public boolean creaDirectorySalvataggio(){
        // qui implemento la logica per creare la directory di salvataggio se non esiste
        try {
            Files.createDirectories(Path.of(logFilesPath));
        } catch (IOException e) {
            System.out.println("Errore nella creazione della cartella dei log");
            return false;
        }
        return true;
    }

    public boolean salvataggioPartita(String fileNamePartite, InfoPartita partita){
        // qui implemento la logica per salvare la partita su file

        boolean scritturaSuccesso; // resetto la variabile per la scrittura delle partite storiche
        Gson json = new Gson();
        json = json.newBuilder().setPrettyPrinting().create();

        // Voglio un file dedicato per ogni partita storica, dentro una cartella dedicata
        Path cartellaPartite = Paths.get(logFilesPath, fileNamePartite);// nel file di configurazione il nome della cartella delle partite storiche
        try {
            Files.createDirectories(cartellaPartite);
        } catch (IOException e) {
            System.err.println("Errore nella creazione della cartella delle partite storiche");
            return false;
        }
        Integer gameId = partita.getGameId();
        Path partitaTarget = cartellaPartite.resolve(gameId + ".json");
        Path partitaTmp = cartellaPartite.resolve(gameId + "_tmp.json");

        try (FileChannel fileChannel = FileChannel.open(partitaTmp, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
                Writer fileWriter = Channels.newWriter(fileChannel, StandardCharsets.UTF_8)) {
            // serializzare in json la partita
            json.toJson(partita, fileWriter);// serializzo con Data Binding API e scrivo senza allocare troppa memoria
            fileWriter.flush();
            fileChannel.force(true);
            scritturaSuccesso = true;

        } catch (IOException e) {
            System.err.println("Errore nella scrittura del file di log della partita " + gameId);
            return false;
        }

        if (scritturaSuccesso) {
            try {
                Files.move(partitaTmp, partitaTarget, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                try {
                    Files.move(partitaTmp, partitaTarget, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ex) {
                    try {
                        Files.deleteIfExists(partitaTmp); // pulisco il file tmp in caso di errore
                    } catch (IOException deleteEx) {
                        System.err.println("Errore nella cancellazione del file temporaneo della partita " + gameId);
                    }
                    System.err.println("Errore nello spostamento del file di log della partita " + gameId);
                    return false;
                }
            } catch (IOException e) {
                try {
                    Files.deleteIfExists(partitaTmp); // pulisco il file tmp in caso di errore
                } catch (IOException deleteEx) {
                    System.err.println("Errore nella cancellazione del file temporaneo della partita " + gameId);
                }
                System.err.println("Errore nello spostamento del file di log della partita " + gameId);
                return false;
            }
        }
        else {
            try {
                Files.deleteIfExists(partitaTmp); // pulisco il file tmp in caso di errore
            } catch (IOException e) {
                System.err.println("Errore nella cancellazione del file temporaneo della partita " + gameId);
            }
        }
        return true;
    }

    public boolean salvataggioProfiliUtenti(ConcurrentHashMap<String,Player> profiliUtenti, String fileNameProfili){
        // qui implemento la logica per salvare i profili utenti su file
        Path profiliTarget = Paths.get(logFilesPath, fileNameProfili);// nel file di configurazione il nome del file di log
        Path profiliTmp = Paths.get(logFilesPath, fileNameProfili + "_tmp.json");// nel file di configurazione il file di log temporaneo

        Gson json = new Gson();
        json = json.newBuilder().setPrettyPrinting().create();
        // inizio la scrittura dei file di log
        // apertura del file di log per profili utenti usando i channel
        boolean scritturaSuccesso; // variabile per tenere traccia del successo della scrittura
        try(FileChannel fileChannel = FileChannel.open(profiliTmp, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            Writer fileWriter = Channels.newWriter(fileChannel, StandardCharsets.UTF_8)) {
            // serializzare in json la hashmap dei profili utenti
            // faccio uno snapshot della hashmap per evitare problemi di concorrenza
            ConcurrentHashMap<String,Player> profiliUtentiSnapshot = new ConcurrentHashMap<>();
            for (String username : profiliUtenti.keySet()){
                Player original = profiliUtenti.get(username);
                Player snapshot = Player.PlayerSnapshot(original);
                profiliUtentiSnapshot.put(username, snapshot);
            }
            json.toJson(profiliUtentiSnapshot, fileWriter);// serializzo con Data Binding API
            fileWriter.flush();
            fileChannel.force(true); // forzo la scrittura su disco
            scritturaSuccesso = true;

        } catch (IOException e) {
            System.err.println("Errore nella scrittura del file di log dei profili utenti");
            return false;
        }
        if(scritturaSuccesso){
            try{// vado a spostera il file temporaneo in quello definitivo provando a fare un operazione atomica
                Files.move(profiliTmp, profiliTarget, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); // sposto il file temporaneo in quello definitivo in modo atomico
        
            } catch (AtomicMoveNotSupportedException e) {
                // fallback: non atomico, ma almeno sostituisce
                try {
                    Files.move(profiliTmp, profiliTarget, StandardCopyOption.REPLACE_EXISTING);// cerco comunque di spostare il file anche se non atomico
                } 
                catch (IOException ex) {
                    System.out.println("Errore nello spostamento del file di log dei profili utenti");
                    try {
                        Files.deleteIfExists(profiliTmp); // pulisco il file tmp in caso di errore
                    } catch (IOException deleteEx) {
                        System.err.println("Errore nella cancellazione del file temporaneo dei profili utenti");
                    }
                    return false;
                }
            }
            catch (IOException e) {
                System.err.println("Errore nello spostamento del file di log dei profili utenti");
                try {
                    Files.deleteIfExists(profiliTmp); // pulisco il file tmp in caso di errore
                } catch (IOException deleteEx) {
                    System.err.println("Errore nella cancellazione del file temporaneo dei profili utenti");
                }
                return false;
            }
        }
        else{
            try {
                Files.deleteIfExists(profiliTmp); // pulisco il file tmp in caso di errore
            } catch (IOException e) {
                System.err.println("Errore nella cancellazione del file temporaneo dei profili utenti");
            }
        }
        return true;
    }
}