package connections_app;

// serve per condividere l'istnanza della partita tra i vari thread
public class WrapperPartita {
    private InfoPartita partita = null;

    public synchronized InfoPartita getPartita() {
        return partita;
    }

    public synchronized void setPartita(InfoPartita partita) {
        this.partita = partita;
    }

    public synchronized boolean isNull(){ // controllo se il puntatore è null
        return this.partita == null;
    }
    public synchronized void putNull(){ // metto il puntatore a null
        this.partita = null;
    }
}