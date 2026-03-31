package connections_app;

// creazione dell'applicazione lato client per il gioco CONNECTIONS con implementazione in NIO non bloccante

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;




public class ClientMain{


    public static void main(String[] args){

        // apertura file di configurazione per il client
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("FileConfigClient.properties")) {
            props.load(fis);
            // carico il nome del file delle partite storiche dal file di configurazione
        } catch (IOException e) {
            System.err.println("Errore nella lettura del file di configurazione");
        }

        // implementazione del client con NIO non bloccante
        Gson json = new Gson();// devo parsare la richiesta json dal client
        BlockingQueue<String> commandQueue = new LinkedBlockingQueue<>(); // coda per i comandi da inviare al server
        //boolean partitaTerminata = false; // variabile per gestire la terminazione della partita
        ExecutorService commandExecutor = Executors.newSingleThreadExecutor(); // dichiarazione esterna per visibilità nel finally


        AtomicBoolean isLogged = new AtomicBoolean(false); // variabile per gestire lo stato di login
        Login managerLogin = new Login();
        UpdateCredentials managerCredentials = new UpdateCredentials(); // gestore per l'aggiornamento delle credenziali


       try (SocketChannel clientChannel = SocketChannel.open();
            DatagramChannel udpChannel = DatagramChannel.open();
            Selector selector = Selector.open()){

            udpChannel.configureBlocking(false); // configurazione non bloccante
            clientChannel.configureBlocking(false); // configurazione non bloccante
            udpChannel.bind(new InetSocketAddress(0)); // bind a una porta casuale disponibile
            int udpPort = ((InetSocketAddress) udpChannel.getLocalAddress()).getPort();
           
            StringBuilder sb = new StringBuilder(); // accomulatore per i messaggi ricevuti dal server

            // da implementare udp per notifiche asincrone
            
            clientChannel.connect(new InetSocketAddress(props.getProperty("HOST"), Integer.parseInt(props.getProperty("PORT"))));// avvio la connessione al server
            
            SelectionKey tcpKey = clientChannel.register(selector, SelectionKey.OP_CONNECT);
            ByteBuffer buffer = ByteBuffer.allocate(Integer.parseInt(props.getProperty("BUFFER_SIZE"))); // alooco il buffer per la comunicazione
            tcpKey.attach(buffer); // attacco il buffer alla chiave TCP




            while(true){
                try {
                    selector.select();
                } catch (IOException e) {
                    System.err.println("Errore nel selettore: " + e.getMessage());
                    break;
                }
                Set<SelectionKey> readyKeys = selector.selectedKeys(); // creo il set delle chiavi pronte
                Iterator<SelectionKey> iterator = readyKeys.iterator();
                while (iterator.hasNext()){
                    SelectionKey readyKey = iterator.next();
                    iterator.remove();
                    try {
                        if (readyKey.isConnectable()){
                            SocketChannel s = (SocketChannel) readyKey.channel();
                            if (s.finishConnect()){
                                System.out.println("Connessione stabilita con il server.");
                                tcpKey.interestOps(SelectionKey.OP_WRITE); // cambio l'interest set per la scrittura
                                SelectionKey udpKey = udpChannel.register(selector, SelectionKey.OP_READ); // registro il canale udp al selettore senza interessi particolari
                                ByteBuffer udpBuffer = ByteBuffer.allocate(Integer.parseInt(props.getProperty("DATAGRAM_PACKET_SIZE"))); // buffer per ricevere l'intero datagramma (1 receive = 1 datagramma)
                                udpKey.attach(udpBuffer); // attacco il buffer alla chiave udp
                                // avvio il thread per la gestione dei comandi da tastiera
                                // creo un thread separato per la gestione dei comandi da tastiera
                                System.out.println("Benvenuto nel gioco Connections!");
                                commandExecutor.execute(new ClientHandlerComandi(commandQueue, selector, tcpKey, udpPort, managerLogin, managerCredentials, isLogged));
                            }
                            else{
                                System.err.println("Impossibile connettersi al server.");
                                return;
                            }
                        }
                        if (readyKey.isReadable()){
                            if (readyKey.channel() instanceof DatagramChannel){
                                // gestione della notifica di fine partita
                                DatagramChannel dc = (DatagramChannel) readyKey.channel();
                                ByteBuffer udpBuffer = (ByteBuffer) readyKey.attachment();

                                // leggo il datagramma UDP (non bloccante): 1 receive = 1 datagramma
                                udpBuffer.clear();
                                SocketAddress senderAddress = dc.receive(udpBuffer);
                                if (senderAddress != null){
                                    udpBuffer.flip();
                                    byte[] result = new byte[udpBuffer.remaining()];
                                    udpBuffer.get(result);
                                    String udpMessage = new String(result, StandardCharsets.UTF_8);

                                    // se il server invia JSON, posso provare a parsarlo (opzionale)
                                    try {
                                        Risposte rispostaUdp = json.fromJson(udpMessage, Risposte.class);

                                        if (rispostaUdp != null) {
                                            if (rispostaUdp.getData() == null) {
                                                System.out.printf("Codice: %d Descrizione: %s \n" , rispostaUdp.getCodice(),rispostaUdp.getDescrizione());
                                            } else {
                                                System.out.printf("Codice: %d Descrizione: %s \nDati:\n%s\n" ,rispostaUdp.getCodice(),rispostaUdp.getDescrizione(), rispostaUdp.getData());
                                            }
                                            // Se sono gia' loggato, mi reiscrivo automaticamente alla prossima partita
                                            if (isLogged.get()) {
                                                String loginJson = json.toJson(managerLogin) + "\n";
                                                try {
                                                    commandQueue.put(loginJson);
                                                    tcpKey.interestOps(tcpKey.interestOps() | SelectionKey.OP_WRITE); // abilito la scrittura sul canale TCP
                                                    selector.wakeup();
                                                } catch (InterruptedException e) {
                                                    System.err.println("Interruzione durante l'inserimento del comando: " + e.getMessage());
                                                }
                                            }
                                        }
                                    } catch (JsonSyntaxException e) {
                                        System.err.println("Errore nel parsing della notifica UDP JSON: " + e.getMessage());
                                    }
                                }
                                continue; // passo alla prossima chiave
                            }
                            else{
                                SocketChannel s = (SocketChannel) readyKey.channel(); // recupero il channel pronto per la lettura
                                ByteBuffer readBuffer = (ByteBuffer) readyKey.attachment(); // recupero il buffer attaccato alla chiave
                                // leggo la risposta dal server (NIO non bloccante): 0 non vuol dire "fine messaggio"
                                int bytesRead = 0;
                                while ((bytesRead = s.read(readBuffer)) > 0) {
                                    readBuffer.flip();
                                    byte[] result = new byte[readBuffer.remaining()];
                                    readBuffer.get(result);
                                    sb.append(new String(result, StandardCharsets.UTF_8));
                                    readBuffer.clear();
                                }

                                if (bytesRead == -1){
                                    s.close();
                                    return;
                                }

                                // provo a convertire il json in oggetto risposta: parsifico SOLO righe complete (terminate da \n)
                                int indiceNuovaLinea;
                                while ((indiceNuovaLinea = sb.indexOf("\n")) >= 0) {
                                    String rispostaSingola = sb.substring(0, indiceNuovaLinea).trim();
                                    sb.delete(0, indiceNuovaLinea + 1); // consumo anche il delimitatore

                                    if (!rispostaSingola.isEmpty()) {
                                        try {
                                            Risposte risposta = json.fromJson(rispostaSingola, Risposte.class); // provo a parsare la risposta
                                            if (risposta.getData() == null) {
                                                System.out.printf("Codice: %d Descrizione: %s \n" , risposta.getCodice(),risposta.getDescrizione());
                                            } else {
                                                System.out.printf("Codice: %d Descrizione: %s \nDati:\n%s\n" ,risposta.getCodice(),risposta.getDescrizione(), risposta.getData());
                                            }
                                            // Dopo aver ricevuto una risposta, posso permettere l'invio di un nuovo comando

                                            switch (risposta.getCodice()) {
                                                case 0: // login avvenuto con successo
                                                    System.out.println("utente loggato con successo");
                                                    isLogged.set(true);
                                                    break;
                                                case 3: // logout avvenuto con successo
                                                    System.out.println("utente disconnesso con successo");
                                                    isLogged.set(false);
                                                    break;
                                                case 9: // credenziali
                                                    System.out.println("Credenziali aggiornate con successo");
                                                    // devo aggiornare le credenziali nel manager
                                                    if(managerCredentials.getNewUsername() != null){
                                                        managerLogin.setUsername(managerCredentials.getNewUsername());
                                                    }
                                                    if(managerCredentials.getNewPsw() != null){
                                                        managerLogin.setPsw(managerCredentials.getNewPsw());
                                                    }
                                                    break;
                                                    
                                            }
                                        } catch (JsonSyntaxException e) {
                                            System.err.println("Errore nel parsing della risposta JSON: " + e.getMessage());
                                        }
                                    }
                                }

                                // Dopo aver gestito eventuali risposte complete, mantengo OP_READ e abilito OP_WRITE se ci sono comandi in coda.
                                int ops = SelectionKey.OP_READ;
                                if (!commandQueue.isEmpty()) {
                                    ops |= SelectionKey.OP_WRITE;
                                }
                                readyKey.interestOps(ops);
                            }
                        }
                        if (readyKey.isWritable()){
                            // invio l'indirizzo udp al server una sola volta all'inizio della connessione
                            SocketChannel s = (SocketChannel) readyKey.channel(); // recupero il channel pronto per la scrittura
                            ByteBuffer writeBuffer = (ByteBuffer) readyKey.attachment(); // recupero il buffer attaccato alla chiave
                            
                            // creo il json da mandare al server
                            String jsonCommand = commandQueue.poll(); // non bloccante, ritorna null se non c'è nulla
                            if (jsonCommand == null){
                                // nessun comando da inviare, rimango in attesa
                                int ops = SelectionKey.OP_READ;
                                if (!commandQueue.isEmpty()) {
                                    ops |= SelectionKey.OP_WRITE;
                                }
                                readyKey.interestOps(ops);
                                continue;
                            }
                            if (jsonCommand.equals("exit")){
                                System.out.println("Chiusura della connessione con il server...");
                                return; // esco dal main e chiudo tutto
                            }
                            byte[] output = jsonCommand.getBytes();
                            int bufferInserted = 0;
                            // scrivo il comando al server
                            while (bufferInserted < output.length) {// adatto nel caso il messaggio al buffer per dimensioni di messaggi maggiori del buffer
                                int chunk = Math.min(output.length - bufferInserted, writeBuffer.capacity());
                                writeBuffer.put(output, bufferInserted, chunk);

                                writeBuffer.flip();
                                while (writeBuffer.hasRemaining()) {
                                    s.write(writeBuffer);
                                }
                                writeBuffer.clear();
                                bufferInserted += chunk; // aggiungo solo ciò che ho scritto
                            }
                            int ops = SelectionKey.OP_READ;
                            if (!commandQueue.isEmpty()) {
                                ops |= SelectionKey.OP_WRITE;
                            }
                            readyKey.interestOps(ops);
                        }
                    }
                    catch (IOException e) {
                        readyKey.cancel();
                        try {
                            readyKey.channel().close();
                        } catch (IOException ex) {
                            System.err.println("Errore nella chiusura del channel: " + ex.getMessage());
                            return;
                        }
                        System.err.println("Errore di I/O sulla connessione: " + e.getMessage());
                        return;
                    }
                }
            }
        
        } catch (IOException e) {
            System.err.println("Errore nella connessione al server: " + e.getMessage());
            System.exit(1);
        }
        finally {
            try {
                System.in.close(); // Chiude System.in per sbloccare Scanner.nextLine()
            } catch (IOException ex) {
                // Ignora errori di chiusura
            }
            commandExecutor.shutdown(); // Chiude il thread executor in modo ordinato
            try {
                if(!commandExecutor.awaitTermination(2, TimeUnit.SECONDS)){
                    System.err.println("Thread di input ancora attivo - richiede input utente.");
                    commandExecutor.shutdownNow(); // Forza la chiusura del thread executor
                }
            }
            catch (InterruptedException e){
                commandExecutor.shutdownNow(); // Forza la chiusura del thread executor
            }
        }

    }
}