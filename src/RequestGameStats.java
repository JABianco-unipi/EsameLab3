package connections_app;
// creo la classe che implementa la richiesta delle statistiche di gioco da parte del client

import com.google.gson.Gson;


public class RequestGameStats {
    private String operation; // operazione di richiesta statistiche di gioco
    private int gameId; // id della partita di cui si richiedono le statistiche
    //private boolean currentGame; // se true richiede le statistiche della partita corrente
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
    private boolean currentGame(){
        return this.gameId == -1;
    }
    public String doOperation (InfoPartita partita, Player giocatore, Risposte risposte){
        // creo la logica per restituire le statistiche di gioco
        Gson gson = new Gson();
        
        if(currentGame()){
            // richiedo le statistiche della partita corrente
            String msg = partita.getStatistichePartitaInCorso();
            risposte.setDataMessaggio(24, "invio statistiche partita corrente", msg);
            String rispostaJson = gson.toJson(risposte); // converto in json la risposta
            risposte.clearMessaggio(); // pulisco il messaggio per future risposte
            return rispostaJson;
        }
        else {
            // richiedo le statistiche della partita storica
            // vado a cercare le informazioni della partita nei file di log della partita
            // controllo che l'id della partita non sia quello della partita corrente
            if(partita != null && partita.getGameId() == this.gameId){
                // l'id richiesto è quello della partita corrente
                String msg = partita.getStatistichePartitaInCorso();
                risposte.setDataMessaggio(24, "invio statistiche partita corrente", msg);
                String rispostaJson = gson.toJson(risposte); // converto in json la risposta
                risposte.clearMessaggio(); // pulisco il messaggio per future risposte
                return rispostaJson;
            }
            // altrimenti vado a cercare la partita storica nei file di log
            else {
                InfoPartita partitaStorica = new GestoreRiavvioServer().recuperoPartita(gameId);
                if(partitaStorica == null){
                    // partita non trovata
                    risposte.setMessaggio(23, "partita storica non trovata");
                    String rispostaJson = gson.toJson(risposte); // converto in json la risposta
                    risposte.clearMessaggio(); // pulisco il messaggio per future risposte
                    return rispostaJson;
                }
                else{
                    // partita trovata allora vado a prenere le statistiche sulla partita
                    String msg = partitaStorica.getStatistichePartitaConclusa();
                    risposte.setDataMessaggio(26, "invio statistiche partita storica", msg);
                    String rispostaJson = gson.toJson(risposte); // converto in json la risposta
                    risposte.clearMessaggio(); // pulisco il messaggio per future risposte
                    return rispostaJson;
                }
            }
        }
    }
}