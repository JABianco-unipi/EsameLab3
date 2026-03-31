package connections_app;

//creo la classe che implementa il submit della proposta del giocatore

import java.util.HashSet;

import com.google.gson.Gson;

public class SubmitProposal {
    private String operation; // operazione di submit proposta
    private HashSet<String> words = new HashSet<>(); // proposta del giocatore

    public String getOperation(){
        return this.operation;
    }
    public void setOperation(String operation){
        this.operation = operation;
    }
    public HashSet<String> getWords(){
        return this.words;
    }
    public void setWords(HashSet<String> words){
        this.words = words;
    }
    public void addWord(String word){
        if (this.words == null) {
            this.words = new HashSet<>();
        }
        this.words.add(word);
    }

    
    public String doOperation(InfoPartita partita, Player giocatore, Risposte risposta){
        
        Gson gson = new Gson();
        if (giocatore.isPartitaVinta()){
            risposta.setMessaggio(50,"hai vinto la partita aspetta una nuova partita per poter fare altre proposte");
            String rispostaJson = gson.toJson(risposta); // converto in json la risposta
            risposta.clearMessaggio(); // pulisco il messaggio per future risposte
            return rispostaJson;
        }
        else if (giocatore.isPartitaPersa()){
            risposta.setMessaggio(51,"hai perso la partita aspetta una nuova partita per poter fare altre proposte");
            String rispostaJson = gson.toJson(risposta); // converto in json la risposta
            risposta.clearMessaggio(); // pulisco il messaggio per future risposte
            return rispostaJson;
        }
        else if (this.words.size() != 4){ // la proposta non contiene esattamente 4 parole
            risposta.setMessaggio(52,"la proposta deve contenere esattamente 4 parole");
            String rispostaJson = gson.toJson(risposta); // converto in json la risposta
            risposta.clearMessaggio(); // pulisco il messaggio per future risposte
            return rispostaJson;
        }
        else if(!giocatore.controllaParole(words)){// le parole proposte non sono valide
            risposta.setMessaggio(53,"la proposta contiene parole non valide");
            String rispostaJson = gson.toJson(risposta); // converto in json la risposta
            risposta.clearMessaggio(); // pulisco il messaggio per future risposte
            return rispostaJson;
        }
        else if(giocatore.propostaGiaFatta(words)){// la proposta e' gia' stata effettuata in precedenza
            risposta.setMessaggio(54,"la proposta e' gia' stata effettuata in precedenza");
            String rispostaJson = gson.toJson(risposta); // converto in json la risposta
            risposta.clearMessaggio(); // pulisco il messaggio per future risposte
            return rispostaJson;
        }
        else{
            // le parole allora sono valide e non sono state gia' proposte
            if(partita.verificaParole(words)){
                giocatore.addProposta(words);
                String msg;
                if(giocatore.isPartitaPerfetta()){
                    partita.aggiungiVincitore();
                    msg = "proposta corretta! hai vinto la partita in modo perfetto!";
                    risposta.setDataMessaggio(55, "proposta accettata dal server", msg);
                    String rispostaJson = gson.toJson(risposta); // converto in json la risposta
                    risposta.clearMessaggio(); // pulisco il messaggio per future risposte
                    return rispostaJson;
                }
                else if(giocatore.isPartitaVinta()){
                    partita.aggiungiVincitore();
                    msg = "proposta corretta! hai vinto la partita!";
                    risposta.setDataMessaggio(55, "proposta accettata dal server", msg);
                    String rispostaJson = gson.toJson(risposta); // converto in json la risposta
                    risposta.clearMessaggio(); // pulisco il messaggio per future risposte
                    return rispostaJson;
                }
                
                else{
                    msg = "proposta corretta!";
                    risposta.setDataMessaggio(55, "proposta accettata dal server", msg);
                    String rispostaJson = gson.toJson(risposta); // converto in json la risposta
                    risposta.clearMessaggio(); // pulisco il messaggio per future risposte
                    return rispostaJson;
                }
            }
            else{
                giocatore.addErrore();
                if (giocatore.isPartitaPersa()){
                    partita.aggiungiPerdente();
                    String msg = "proposta errata! hai perso la partita!";
                    risposta.setDataMessaggio(55, "proposta accettata dal server", msg);
                    String rispostaJson = gson.toJson(risposta); // converto in json la risposta
                    risposta.clearMessaggio(); // pulisco il messaggio per future risposte
                    return rispostaJson;
                }
                else{
                    String msg = "risposta errata!";
                    risposta.setDataMessaggio(55, "proposta accettata dal server", msg);
                    String rispostaJson = gson.toJson(risposta); // converto in json la risposta
                    risposta.clearMessaggio(); // pulisco il messaggio per future risposte
                    return rispostaJson;
                }
            }
        }
    }
}