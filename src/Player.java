package connections_app;

// creazione della classe player per memorizzare le informazioni riguardo il giocatore e il suo stato nel gioco
import java.util.HashSet;

public class Player{
    private volatile String username;// questa è una cosa personale del giocatore 
    private volatile String password;// questa è una cosa personale del giocatore
    private volatile int puzzlesFailed = 0;// questo è il numero di puzzle non risolti dal giocatore
    private volatile int puzzlesAbandoned = 0;// questo è il numero di puzzle abbandonati dal giocatore
    private volatile int currentStreak = 0;// questo è lo streak corrente del giocatore
    private volatile int maxStreak = 0;// questo è il massimo streak raggiunto dal giocatore
    private volatile String status;// questo è lo stato del giocatore (loggato o non loggato)
    private volatile int totalPoints = 0;// questo è il totale dei punti accumulati dal giocatore
    private volatile int gamesPlayed = 0;// questo è il numero di partite giocate dal giocatore
    private volatile Vittorie vittorie = new Vittorie(); // oggetto che conta le vittorie del giocatore


    private volatile RisultatiPartita risultatiPartita; // risultati della partita corrente del giocatore



    

    public Player (String username, String password){
        this.username = username;
        this.password = password;
    }

    public static Vittorie VittorieSnapshot(Vittorie original) {
        synchronized (original) {
            Vittorie snapshot = new Vittorie();
            snapshot.setPuzzlesSolved(original.getPuzzlesSolved());
            snapshot.setPerfectPuzzles(original.getPerfectPuzzles());
            snapshot.setPuzzlesWithOneMistake(original.getPuzzlesWithOneMistake());
            snapshot.setPuzzlesWithTwoMistakes(original.getPuzzlesWithTwoMistakes());
            snapshot.setPuzzlesWithThreeMistakes(original.getPuzzlesWithThreeMistakes());
            return snapshot;
        }
    }

    public static Player PlayerSnapshot(Player original) { // crea uno snapshot del giocatore per la scrittura su file
        synchronized (original) {
            Player snapshot = new Player(original.username, original.password);
            snapshot.puzzlesFailed = original.puzzlesFailed;
            snapshot.puzzlesAbandoned = original.puzzlesAbandoned;
            snapshot.currentStreak = original.currentStreak;
            snapshot.maxStreak = original.maxStreak;
            snapshot.status = original.status;
            snapshot.totalPoints = original.totalPoints;
            snapshot.gamesPlayed = original.gamesPlayed;
            snapshot.vittorie = Player.VittorieSnapshot(original.vittorie);

            // Non copiamo i risultati della partita corrente
            return snapshot;
        }
    }

    public synchronized void addProposta(HashSet<String> proposta){
        if (risultatiPartita == null){
            return;
        }
        risultatiPartita.addProposta(proposta); // aggiungo la proposta corretta ai risultati della partita corrente
        if (risultatiPartita.isPartitaPerfetta()) {
            vittorie.incrementPerfectPuzzles();
            currentStreak++;
            if (currentStreak > maxStreak) {
                maxStreak = currentStreak;
            }
        }
        else if (risultatiPartita.isPartitaVinta()) {
            currentStreak++;
            if (currentStreak > maxStreak) {
                maxStreak = currentStreak;
            }
            int mistakes = risultatiPartita.getCurrentMistakes();
            if (mistakes == 1) {
                vittorie.incrementPuzzlesWithOneMistake();
            } else if (mistakes == 2) {
                vittorie.incrementPuzzlesWithTwoMistakes();
            } else if (mistakes == 3) {
                vittorie.incrementPuzzlesWithThreeMistakes();
            }
        }
            
    }

    public boolean controllaParole(HashSet<String> paroleProposte){// serve a capire se la proposta del giocatore è accettabile nel senso che contiene parole valide
        RisultatiPartita risultati = this.risultatiPartita;
        if (risultati == null){
            return false;
        }
        return risultati.controllaParole(paroleProposte);
    }

    public synchronized void updateStatisticsAfterGame(RisultatiPartita risultati){ // aggiorno le statistiche del giocatore dopo la fine della partita
        if (risultati == null){
            return;
        }
        this.totalPoints += risultati.getPunteggioFinale();
        this.gamesPlayed += 1;
    }
    public int getTotalPoints(){
        return this.totalPoints;
    }
    public synchronized void addPuzzlesAbandoned(){
        this.puzzlesAbandoned += 1;
        this.currentStreak = 0; // resetto lo streak corrente
    }
    public Boolean isPartitaVinta(){ // ritorno l'esito della partita
        RisultatiPartita risultati = this.risultatiPartita;
        if (risultati == null){
            return false;
        }
        return risultati.isPartitaVinta();
    }
    public Boolean isPartitaPerfetta(){ // ritorno l'esito della partita
        RisultatiPartita risultati = this.risultatiPartita;
        if (risultati == null){
            return false;
        }
        return risultati.isPartitaPerfetta();
    }
    public Boolean isPartitaPersa(){ // ritorno l'esito della partita
        RisultatiPartita risultati = this.risultatiPartita;
        if (risultati == null){
            return false;
        }
        return risultati.isPartitaPersa();
    }
    public boolean propostaGiaFatta (HashSet<String> proposta){
        RisultatiPartita risultati = this.risultatiPartita;
        if (risultati == null){
            return false;
        }
        return risultati.propostaGiaFatta(proposta);
    }
    public synchronized void addErrore(){
        RisultatiPartita risultati = this.risultatiPartita;
        if (risultati == null){
            return;
        }
        risultati.addErrore();
        if(risultati.isPartitaPersa()){
            this.currentStreak = 0; // resetto lo streak corrente
            this.puzzlesFailed += 1;
        }
    }
    public synchronized void setRisultatiPartita(RisultatiPartita risultati){
        this.risultatiPartita = risultati;
    }
    public RisultatiPartita getRisultatiPartita(){ // ritorno i risultati della partita corrente del giocatore
        return this.risultatiPartita;
    }
    public synchronized void login(InfoPartita partitaCorrente){ // metodo per fare il login di un giocatore
        this.status = "LOGGED_IN";
        risultatiPartita = partitaCorrente.aggiungiGiocatore(username); // chiama il metodo nella partita per recuperare il puntatore
    }
    public synchronized void logout(){ // metodo per fare il logout di un giocatore
        this.risultatiPartita = null; // resetto i risultati della partita corrente
        this.status = "REGISTERED";
    }
    public synchronized void register(){ // metodo per registrare un nuovo giocatore
        this.status = "REGISTERED";
    }
    public boolean isLoggedIn(){
        return status != null && status.equals("LOGGED_IN");
    }

    public synchronized String getPlayerStats(){
        // ritorno le statistiche del giocatore in formato stringa
        StringBuilder sb = new StringBuilder();
        sb.append("Player: ").append(username).append("\n");
        sb.append("Puzzles Solved: ").append(vittorie.getPuzzlesSolved()).append("\n");
        sb.append("Perfect Puzzles: ").append(vittorie.getPerfectPuzzles()).append("\n");
        sb.append("Current Streak: ").append(currentStreak).append("\n");
        sb.append("Max Streak: ").append(maxStreak).append("\n");
        sb.append("Win Rate: ");
        if(gamesPlayed == 0){
            sb.append("0%\n");
        }
        else{
            int winRate = (int) (((double) vittorie.getPuzzlesSolved() / (double) gamesPlayed) * 100);
            sb.append(winRate).append("%\n");
        }
        sb.append("Loss Rate: ");
        if(gamesPlayed == 0){
            sb.append("0%\n");
        }
        else{
            int lossRate = (int) (((double) (gamesPlayed - vittorie.getPuzzlesSolved()) / (double) gamesPlayed) * 100);
            sb.append(lossRate).append("%\n");
        }
        sb.append("Mistake Holograms: ").append("\n\n");
        // implemento l'istogramma con trattini e asterischi
        sb.append("0 mistakes: ").append("*".repeat(vittorie.getPerfectPuzzles())).append("\n");
        sb.append("1 mistake: ").append("*".repeat(vittorie.getPuzzlesWithOneMistake())).append("\n");
        sb.append("2 mistakes: ").append("*".repeat(vittorie.getPuzzlesWithTwoMistakes())).append("\n");
        sb.append("3 mistakes: ").append("*".repeat(vittorie.getPuzzlesWithThreeMistakes())).append("\n");
        sb.append("failed puzzles: ").append("*".repeat(puzzlesFailed)).append("\n");
        sb.append("abandoned puzzles: ").append("*".repeat(puzzlesAbandoned)).append("\n");
        return sb.toString();

    }

    public int getCurrentMistakes(){
        RisultatiPartita risultati = this.risultatiPartita;
        if (risultati == null){
            return 0;
        }
        return risultati.getCurrentMistakes();
    }
    public int getCurrentPoints(){
        RisultatiPartita risultati = this.risultatiPartita;
        if (risultati == null){
            return 0;
        }
        return risultati.getPunteggioFinale();
    }

    public String getStatus(){
        return this.status;
    }
    public void setPassword(String newPassword){
        this.password = newPassword;
    }
    public void setUsername(String newUsername){
        this.username = newUsername;
    }
    public String getUsername(){
        return this.username;
    }
    public String getPassword(){
        return this.password;
    }
    public synchronized Vittorie getVittorie(){
        return this.vittorie;
    }

}