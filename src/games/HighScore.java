/* High Score class for asteroids and likely other video game
 - started 7.31.17 - MJS
*/

package games;  // most games contain high scores  
import java.time.LocalDate;  // new Java8 date-time classes


// -------------------------------------------
public class HighScore implements Comparable<HighScore> {

    private static long COUNT = 0;          // total objects of this type created

    // instance variables
    private long ID;                        // serial ID of all high score objects (for debugging)
    private String name;                    // name of person getting score
    private int score;                      // the score itself
    private LocalDate date;                 // date score was achieved

   // HighScore Constructors 
   public HighScore( )  {
       // System.out.println("Starting HighScore constructor");
       this.ID = ++COUNT;
       this.date = LocalDate.now();      
    } // end HighScore constructor

   public HighScore(String name, int score)  {
       // System.out.println("Starting HighScore(name, score) constructor");
       this();
       this.name = name;
       this.score = score;      
    } // end HighScore constructor

    public HighScore(String name, int score, LocalDate date)  {
        // System.out.println("Starting HighScore(name, score, date) constructor");
        this(name, score);
        this.date = date;      
    } // end HighScore constructor

 // ------------ Comparable interface method ------------------------
   @Override
   public int compareTo(HighScore that) {
       if (that == null) throw new NullPointerException("Cant compare HighScore to null.");
       // next line better than using subtraction (avoids overflow)
       return Integer.compare(that.score, this.score);  // make higher scores first
   }

 // HighScore methods ----------------------------------------------------------------------------
 // ----------------------------------------------------------------------------------------------

 // HighScore getter and setters ----------------------------
 // ---------------------------------------------------------

    // no setters for any of these
    public long getID() {
        return ID;  // ID mainly for debugging.
    }

    public int getScore() {
        return score;  
    }

    public String getName() {
        return name;  
    }

    public LocalDate getDate() {
        return date;  
    }

  // ----------- HighScore non-get-set methods ---------------------------------
  // ---------------------------------------------------------------



} // end class HighScore

