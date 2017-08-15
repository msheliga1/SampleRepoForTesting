/* High Score File class for asteroids and likely other video games.  All methods static
 - started 8.3.17 - MJS
*/

package games;  // most games contain high scores  
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;  // allows formatted printf style writes
import java.io.File;
import java.time.LocalDate;  // new Java8 date-time classes
import java.util.List;       
import java.util.ArrayList; 
 
// -------------------------------------------
public class HighScoreFile {

    private static final int MIN_LINE_LENGTH = 48;    // line length in file (name=30,score=10,date=8)
    // private static final String HIGH_SCORE_FILENAME2 = "RoidHighScores2.txt";

    // instance variables - None, this is a static only class
    // ----------------------------------------------------------------------------------------------

    // ------------ HighScoreFile methods -----------------------------------------------------------
    // ----------------------------------------------------------------------------------------------


    // this routine should retrieve values in high score file 
    // can throw exception for many reasons - any exception implies value returned incorrect.
    public static List<HighScore> readHighScoresFromFile(BufferedReader br) throws Exception {
        List<HighScore> highList = new ArrayList<> ();
        String line;

        while ((line = br.readLine()) != null) {
            highList.add(HighScoreFile.lineParse(line));
        } // end while
        return highList;
    } // End readHighScoresFromFile()


    // this routine should retrieve values in high score file 
    // can throw exception for many reasons - any exception implies value returned incorrect.
    public static List<HighScore> readHighScoresFromFile(File file) throws Exception {
        try (BufferedReader br = new BufferedReader(new FileReader(file));) {
             return readHighScoresFromFile(br);
        } // end try
    } // End readHighScoresFromFile()

    // this routine should retrieve values in high score file 
    // can throw exception for many reasons - any exception implies value returned incorrect.
    public static List<HighScore> readHighScoresFromFile(HighScoreClassInfo classReference) throws Exception {
        // System.out.println(" Starting read High Scores from file");
        File filPath   = getClassPath(classReference);
        // System.out.println("filePath " + filPath + " exists? " + filPath.exists());
        File file = new File(filPath, classReference.getHighScoreFileName());
        // System.out.println("file " + file + " exists? " + file.exists());

        return HighScoreFile.readHighScoresFromFile(file);

    } // End readHighScoresFromFile()



    // Write values in high scores list to a file referenced by PrintWriter input
    public static void writeHighScoresToFile(List<HighScore> highScores, PrintWriter pw) {
        // System.out.println("Starting PrintWriter write High Scores to File: HS size " + highScores.size()); 
        for (HighScore hs: highScores) {
            LocalDate date = hs.getDate();
            pw.printf("%-30s,%9d,%tY-%<1tm-%<2td", hs.getName(), hs.getScore(), date); 
            pw.println();                
       } // end foreach
    } // End writeHighScoresToFile()


    // Write values in high score list to a file by callinb overloaded version with a PrintWriter 
    public static void writeHighScoresToFile(List<HighScore> highScores, File file) throws IOException {
        // System.out.println("Starting Write High Scores To File");
        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));) {
             System.out.println("Starting writes to file " + file); 
             writeHighScoresToFile(highScores, pw);
        } // end try-with resources
    } // End writeHighScoresToFile()

    // Write values in high score list to a file whose path/name is determined via classReference
    public static void writeHighScoresToFile(List<HighScore> highScores, HighScoreClassInfo classReference)
                                             throws FileNotFoundException, IOException {
        File filPath   = getClassPath(classReference);
        if (!filPath.exists() || filPath.isFile()) {
            // System.out.println("Error: High Score File Path " + filPath + " does not exist.");
            throw new FileNotFoundException("High Score File Path " + filPath + " does not exist.");
        }
        File file = new File(filPath, classReference.getHighScoreFileName());
        writeHighScoresToFile(highScores, file);
    } // End writeHighScoresToFile()


  public static HighScore lineParse(String s) throws IllegalArgumentException {
      if (s.length() < getMinLineLength()) {
          String eString = "HighScore: lineParse: Illegal line length of " + s.length();
          throw new IllegalArgumentException(eString + " for text: " + s);
      } 
      String newName = s.substring(0, 30);  // line(30) is a comma
      int newScore = Integer.parseInt(s.substring(31, 40).trim());  // must trim leading blanks!!
      LocalDate newDate = LocalDate.parse(s.substring(41).trim());  // must trim even trailing blanks
      return new HighScore(newName, newScore, newDate);
  } // End lineParse

  // get the minimum line length
  public static int getMinLineLength() {
      return MIN_LINE_LENGTH;  // use routine so subclasses can easily change and use this
  }

    // get the classes path.
    public static File getClassPath(HighScoreClassInfo inObject) {

        String sPath = inObject.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        // System.out.println("getClassPath inOjbect.class.name is " + inObject.getClass().getName());;
        // System.out.println("This class.simpleName is " + inObject.getClass().getSimpleName());
        // System.out.println("the original sPath is " + sPath);
        // for DOS change %20 back to space
        String sReplaced = (sPath + inObject.getClass().getName()).replace("%20", " ");  
        sReplaced = sReplaced.replace("." + inObject.getClass().getSimpleName() , "");
        sReplaced = sReplaced.replace(".", File.separator);
        File fReplaced = new File(sReplaced);
        // System.out.println("getClassPath: fReplaced " + fReplaced + " exists? " + fReplaced.exists());
        return fReplaced;

    } // End getClassPath(inObject objectType)

} // end class HighScoreFile

