package connections_app;

// usato per inviare le risposte al client

public class Risposte{
    private String descrizione;
    private int codice;
    private String data; // campo opzionale per inviare dati aggiuntivi

    public Risposte(){}

    public void setDataMessaggio(int codice, String descrizione, String data){
        this.descrizione = descrizione;
        this.codice = codice;
        this.data = data;
    }
    public String getDescrizione(){
        return descrizione;
    }
    public int getCodice(){
        return codice;
    }
    public String getData(){
        return data;
    }
    public void setMessaggio(int codice, String descrizione){
        this.descrizione = descrizione;
        this.codice = codice;
        this.data = null;
    }
    public void clearMessaggio(){
        this.descrizione = null;
        this.codice = 0;
        this.data = null;
    }
    
}