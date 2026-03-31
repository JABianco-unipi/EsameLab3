package connections_app;

// creazione classe gestione delle notifiche asincorne da inviare al client
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;

public class GestoreNotifiche {
    private final int maxClassifica;
    
    public GestoreNotifiche(){
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("FileConfigServer.properties")) {
            props.load(fis);
            // carico il nome del file delle partite storiche dal file di configurazione
        } catch (IOException e) {
            System.err.println("Errore nella lettura del file di configurazione");
        }
        this.maxClassifica = Integer.parseInt(props.getProperty("MAX_CLASSIFICA"));
    }
    
    public void inviaNotificaFinePartita(ConcurrentHashMap<String, SocketAddress> clientAddresses, InfoPartita partitaCorrente){
        // costruisco il messaggio da inviare ai client
        Risposte rispostaFinePartita = new Risposte();
        Gson gson = new Gson();
        StringBuilder sb = new StringBuilder();
        

        // e prendo anche la classifica finale
        StringBuilder sbClassifica = new StringBuilder();
        sbClassifica.append("Classifica finale:\n");
        TreeMap<Integer, HashSet<String>> classificaFinale = partitaCorrente.getClassificaFinale();
        int count = 0;
        for (Map.Entry<Integer, HashSet<String>> entry : classificaFinale.entrySet()){
            if(count >= maxClassifica){
                break;
            }
            // Rendo piu' leggibile l'elenco di giocatori a pari punteggio (niente [..] del Set)
            sbClassifica.append((count + 1)).append(". ")
                       .append(String.join(", ", entry.getValue()))
                       .append(" - ").append(entry.getKey()).append(" punti\n");
            count++; 
        }

        for(Map.Entry<String, SocketAddress> entry : clientAddresses.entrySet()){
            try(DatagramSocket socket = new DatagramSocket()){
                sb.append("La partita con ID ").append(partitaCorrente.getGameId()).append(" è terminata!\n\n");
                // mi servono gamestats e gameInfo
                String infoPartita = partitaCorrente.getInfoPartitaConclusa(entry.getKey()); // prendo le info della partita per la partita conclusa
                sb.append(infoPartita).append("\n");
                String statsPartita = partitaCorrente.getStatistichePartitaConclusa();// prendo le statistiche della partita
                sb.append(statsPartita).append("\n");
                sb.append(sbClassifica.toString()); // aggiungo la classifica finale al messaggio
                rispostaFinePartita.setDataMessaggio(255, "notifica fine partita", sb.toString());
                String rispostaJson = gson.toJson(rispostaFinePartita); // converto in json la risposta
                sb.setLength(0); // pulisco il buffer per il prossimo messaggio
                rispostaFinePartita.clearMessaggio(); // pulisco il messaggio per future risposte
                byte[] rispostaBytes = rispostaJson.getBytes(StandardCharsets.UTF_8); // uso UTF-8 e lunghezza in byte (non in char)
                DatagramPacket packet = new DatagramPacket(rispostaBytes, rispostaBytes.length, entry.getValue());
                socket.send(packet);
            } catch (IOException e) {
                System.err.println("Errore nell'invio del pacchetto di fine partita a " + entry.getValue() + ": " + e.getMessage());
            }
        }
        // fine invio notifiche svuoto la mappa degli indirizzi
        clientAddresses.clear();
    }
}