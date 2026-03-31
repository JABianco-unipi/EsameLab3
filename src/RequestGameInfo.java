package connections_app;

// creo la classe che implementa la richiesta delle informazioni della partita da parte del client
import com.google.gson.Gson;
public class RequestGameInfo {
    private String operation; // operazione di richiesta informazioni partita
    private int gameId; // id della partita di cui si richiedono le informazioni
    // definisco una variabile in più per la partita corrente
    //private boolean currentGame; // se true richiede le informazioni della partita corrente



    public String getOperation(){
        return this.operation;
    }
    public void setOperation(String operation){
        this.operation = operation;
    }
    public int getGameId(){
        return this.gameId;
    }
    public void setGameId(int gameId){
        this.gameId = gameId;
    }

     // modifico la doOperation per gestire sia la richiesta della partita corrente che di una partita storica
     private boolean currentGame(){
         return this.gameId == -1;
     }
    public String doOperation(InfoPartita partita, Player giocatore, Risposte risposta){
        // gestisco operazione di requestGameInfo
        Gson gson = new Gson();
        if(currentGame() && partita.partitaAvviata()){
            // richiedo le informazioni della partita corrente
            String msg = partita.getInfoPartitaInCorso(giocatore.getUsername());
            risposta.setDataMessaggio(20, "invio informazioni partita corrente", msg);
            String rispostaJson = gson.toJson(risposta); // converto in json la risposta
            risposta.clearMessaggio(); // pulisco il messaggio per future risposte
            return rispostaJson;

        }
        else{
            // richiedo le informazioni della partita storica
            // vado a cercare le informazioni della partita nei file di log della partita
            // controllo che l'id della partita non sia quello della partita corrente
            if(partita != null && partita.getGameId() == this.gameId && partita.partitaAvviata()){
                // l'id richiesto è quello della partita corrente
                String msg = partita.getInfoPartitaInCorso(giocatore.getUsername());
                risposta.setDataMessaggio(20, "invio informazioni partita corrente", msg);
                String rispostaJson = gson.toJson(risposta); // converto in json la risposta
                risposta.clearMessaggio(); // pulisco il messaggio per future risposte
                return rispostaJson;
            }
            else{
                InfoPartita partitaStorica = new GestoreRiavvioServer().recuperoPartita(gameId);
                if(partitaStorica == null){
                    // partita non trovata
                    risposta.setMessaggio(23, "partita storica non trovata");
                    String rispostaJson = gson.toJson(risposta); // converto in json la risposta
                    risposta.clearMessaggio(); // pulisco il messaggio per future risposte
                    return rispostaJson;
                }
                else{
                    // controllo anche che il giocatore abbia partecipato a quella partita
                    if(!partitaStorica.haPartecipatoGiocatore(giocatore.getUsername())){
                        // il giocatore non ha partecipato a quella partita
                        risposta.setMessaggio(22, "il giocatore non ha partecipato alla partita storica");
                        String rispostaJson = gson.toJson(risposta); // converto in json la risposta
                        risposta.clearMessaggio(); // pulisco il messaggio per future risposte
                        return rispostaJson;
                    }
                    // partita trovata allora vado a prenere le informazioni sulla partita
                    String msg = partitaStorica.getInfoPartitaConclusa(giocatore.getUsername());
                    risposta.setDataMessaggio(21, "invio informazioni partita storica", msg);
                    String rispostaJson = gson.toJson(risposta); // converto in json la risposta
                    risposta.clearMessaggio(); // pulisco il messaggio per future risposte
                    return rispostaJson;
                }
            }

        }
    }
}