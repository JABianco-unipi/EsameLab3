package connections_app;

// classe che conta tutte le possibili vittorie ottenibili in una partita


public class Vittorie {
    private int PuzzlesSolved; // puzzle risolti
    private int PerfectPuzzles; // 0 errori in una partita
    private int puzzlesWithOneMistake; // puzzle risolti con 1 errore
    private int puzzlesWithTwoMistakes; // puzzle risolti con 2 errori
    private int puzzlesWithThreeMistakes; // puzzle risolti con 3 errori

    public Vittorie(){
        this.PuzzlesSolved = 0;
        this.PerfectPuzzles = 0;
        this.puzzlesWithOneMistake = 0;
        this.puzzlesWithTwoMistakes = 0;
        this.puzzlesWithThreeMistakes = 0;
    }


    public void incrementPerfectPuzzles(){
        this.PuzzlesSolved += 1;
        this.PerfectPuzzles += 1;
    }
    public void incrementPuzzlesWithOneMistake(){
        this.PuzzlesSolved += 1;
        this.puzzlesWithOneMistake += 1;
    }
    public void incrementPuzzlesWithTwoMistakes(){
        this.PuzzlesSolved += 1;
        this.puzzlesWithTwoMistakes += 1;
    }
    public void incrementPuzzlesWithThreeMistakes(){
        this.PuzzlesSolved += 1;
        this.puzzlesWithThreeMistakes += 1;
    }

    public int getPuzzlesSolved(){
        return this.PuzzlesSolved;
    }
    public int getPerfectPuzzles(){
        return this.PerfectPuzzles;
    }
    public int getPuzzlesWithOneMistake(){
        return this.puzzlesWithOneMistake;
    }
    public int getPuzzlesWithTwoMistakes(){
        return this.puzzlesWithTwoMistakes;
    }
    public int getPuzzlesWithThreeMistakes(){
        return this.puzzlesWithThreeMistakes;
    }

    public void setPuzzlesSolved(int count){
        this.PuzzlesSolved = count;
    }
    public void setPerfectPuzzles(int count){
        this.PerfectPuzzles = count;
    }
    public void setPuzzlesWithOneMistake(int count){
        this.puzzlesWithOneMistake = count;
    }
    public void setPuzzlesWithTwoMistakes(int count){
        this.puzzlesWithTwoMistakes = count;
    }
    public void setPuzzlesWithThreeMistakes(int count){
        this.puzzlesWithThreeMistakes = count;
    }
}