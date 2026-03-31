package connections_app;

// creazione classe per il parsin delle partite dal file json

import java.io.FileReader;
import java.io.IOException;

import com.google.gson.stream.JsonReader;

// quest classe serve per il parsing del file sequenziale delle partite e anche per ikl recupero dopo un riavvio del server
public class PartitaParser {
    JsonReader jReader;

    public PartitaParser(){
        try {
            jReader = new JsonReader(new FileReader("Connections_Data.json"));
            jReader.beginArray(); // inizio dell'array principale 
        } catch (IOException e) {
            System.err.println("Errore nell'apertura del file di log delle partite: " + e.getMessage());
        }
    }
    
    
    public InfoPartita parseNuovaPartita(){
        InfoPartita partitaCorrente = new InfoPartita();
        try {
            // inizio la lettura del file json
            if(jReader.hasNext()){ // ciclo su ogni partita del file json

                jReader.beginObject();
                while(jReader.hasNext()){ // ciclo per leggere i campi principali della singola partita
                    String name = jReader.nextName();

                    if(name.equals("gameId")){ // controllo il primo name e prendo l'intero identificativo della partita
                        int gameId = jReader.nextInt();
                        partitaCorrente.setGameId(gameId);// serve per richiesta info riguardo la partita
                    }
                    else if (name.equals("groups")){ // controllo il secondo name e scorro l'array dei gruppi
                        jReader.beginArray();

                        while(jReader.hasNext()){ // ciclo su ogni gruppo della partita
                            jReader.beginObject();

                            String tema = null; // definisco la variabile per poter mettere il tema nella mappa
                            String[] combinazione = null; // ogni combinazione ha 4 parole

                            while(jReader.hasNext()){ // ciclo sui campi del singolo gruppo (theme, words)
                                String campo = jReader.nextName();

                                if(campo.equals("theme")){
                                    tema = jReader.nextString(); // recupero il tema
                                }
                                else if(campo.equals("words")){
                                    jReader.beginArray();// scorro le parole
                                    combinazione = new String[4];
                                    int i = 0;
                                    while(jReader.hasNext()){
                                        String parola = jReader.nextString();
                                        if(i < 4){
                                            combinazione[i++] = parola;
                                        }
                                    }
                                    jReader.endArray();
                                    partitaCorrente.addCombinazione(tema, combinazione); // aggiungo alla mappa il tema e la combinazione
                                }
                                else{
                                    jReader.skipValue(); // campo del gruppo non rilevante
                                }
                            }

                            jReader.endObject();

                            if(tema == null || combinazione == null){
                                throw new IOException("formato json non valido: group senza theme o words");
                            }
                        }

                        jReader.endArray();
                    }
                    else{
                        jReader.skipValue(); // campo del file json che non mi interessa
                    }
                }
                jReader.endObject();
            }
            else {
                jReader.endArray(); // chiudo l'array principale
                jReader.close(); // chiudo il reader
                return null; // non ci sono più partite da leggere
            }
            
        } catch (IOException e) {
            System.err.println("Errore nel parsing della nuova partita: " + e.getMessage());
            return null;
        }
        return partitaCorrente;
    }
    
    
    public InfoPartita parseRecuperoPartita(int idPartita){ // recupero della partita dopo la prima partita parsata con i file di recupero
        InfoPartita partitaCorrente = new InfoPartita();
        try {
            // inizio la lettura del file json
            while(jReader.hasNext()){ // ciclo su ogni partita del file json

                jReader.beginObject();
                boolean skipGame = false; // se true salto la partita già letta
                while(jReader.hasNext()){ // ciclo per leggere i campi principali della singola partita
                    String name = jReader.nextName();

                    if(name.equals("gameId")){ // controllo il primo name e prendo l'intero identificativo della partita
                        int gameId = jReader.nextInt();
                        if(gameId <= idPartita){
                            skipGame = true; // imposto il flag per saltare la partita
                        }
                        else{
                            partitaCorrente.setGameId(gameId);// serve per richiesta info riguardo la partita
                        }
                    }
                    else if (name.equals("groups")){ // controllo il secondo name e scorro l'array dei gruppi
                        if(skipGame){
                            jReader.skipValue(); // salto il contenuto della partita già letta
                            continue;
                        }
                        jReader.beginArray();

                        while(jReader.hasNext()){ // ciclo su ogni gruppo della partita
                            jReader.beginObject();

                            String tema = null; // definisco la variabile per poter mettere il tema nella mappa
                            String[] combinazione = null; // ogni combinazione ha 4 parole

                            while(jReader.hasNext()){ // ciclo sui campi del singolo gruppo (theme, words)
                                String campo = jReader.nextName();

                                if(campo.equals("theme")){
                                    tema = jReader.nextString(); // recupero il tema
                                }
                                else if(campo.equals("words")){
                                    jReader.beginArray();// scorro le parole
                                    combinazione = new String[4];
                                    int i = 0;
                                    while(jReader.hasNext()){
                                        String parola = jReader.nextString();
                                        if(i < 4){
                                            combinazione[i++] = parola;
                                        }
                                    }
                                    jReader.endArray();
                                    partitaCorrente.addCombinazione(tema,combinazione);
                                }
                                else{
                                    jReader.skipValue(); // campo del gruppo non rilevante
                                }
                            }

                            jReader.endObject();

                            if(tema == null || combinazione == null){
                                throw new IOException("formato json non valido: group senza theme o words");
                            }
                        }
                        jReader.endArray();
                    }
                    else{
                        jReader.skipValue(); // campo della partita non rilevante
                    }
                }
                jReader.endObject();
                if(!skipGame){
                    return partitaCorrente; // ritorno la partita recuperata
                }
            }
            if(!jReader.hasNext()){
                jReader.endArray(); // chiudo l'array principale
                jReader.close(); // chiudo il reader
            }
        } catch (IOException e) {
            System.err.println("Errore nel parsing della partita di recupero: " + e.getMessage());
            return null;
        }
       return null;
    }

}