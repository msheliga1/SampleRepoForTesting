/* High Score Processor for video games
8.2.17 - MJS
Get high scores from blockingQ, get current high scores from file, if new high score get name and 
insert high score into list, write to file.
*/

package games;                  // most games contain high scores 
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile; 
import java.nio.channels.Channels;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.time.LocalDate;  // new Java8 date-time classes
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.BlockingQueue;
import java.util.Collections;
import java.util.List;       
import java.util.ArrayList; 
import javax.swing.JOptionPane;
import static util.MyTryLock.myTryLock;
   
// -------------------------------------------
public class HighScoreProcessor implements Runnable {

    private static AtomicLong count = new AtomicLong(); // total objects of this type created
    static final int MAX_HIGH_SCORES = 5;
    static final int MAX_READ_WRITE_FAILURES = 2;       // Maximum RW failures before giving up.

    // instance variables
   protected long id;
   protected BlockingQueue<Integer> queue;  // get msgs from child games
   protected List<HighScore> highScores = new ArrayList<HighScore>();
   protected File file;
   protected int readWriteFailures = 0;  
   // if file can be read and written, use it, otherwise use a local copy.
   protected boolean canReadWriteFile = false;
   protected boolean appDone;  // if process interrupted, application is done.       

  // ----------- HighScoreProcessor Run Method ---------------------
  // ---------------------------------------------------------------

    public void run( ) {
     int gameScore = 0; 
     while (!appDone) {
        try { 
            gameScore = queue.take();
        } catch (InterruptedException e) {
            System.out.println("GameManager interrupted while queue.take trying.");
            Thread.currentThread().interrupt();  // allow interrupt to occur.
        } // end try queue.take - catch interrupt
        System.out.println("HighScoreProcessor Run: found score of " + gameScore);
        addNewScore(gameScore);
     }
    } // end run

  // ----------- HighScoreProcessor Constructors -------------------
  // ---------------------------------------------------------------


   // HighScoreProcessor Constructors 
   public HighScoreProcessor( )  {  // need no-param for children to auto-call
       // System.out.println("Starting HighScoreProcessor constructor");
    } // end HighScoreProcessor constructor

   // HighScoreProcessor Constructors 
   public HighScoreProcessor(HighScoreClassInfo classReference, BlockingQueue<Integer> queue)  {
       this.id = count.incrementAndGet();
       // System.out.println("Starting HighScoreProcessor constructor");
       this.queue = queue; 
       this.file = new File(HighScoreFile.getClassPath(classReference), 
                            classReference.getHighScoreFileName());
       // get HighScores from file, or default if file doesnt exist or is locked or inaccessible
       highScores = getOriginalHighScores(file);  // from file or default
       // write these to a file . . . in case they are defaults ... need file to test 
       // can read and write file . . .
       try {
           if (!file.exists()) HighScoreFile.writeHighScoresToFile(highScores, file);   
       } catch (Exception e) { 
           // if we cant write to highScore file . . . just do nothing.
       }
       testReadWriteFile(file);
    } // end HighScoreProcessor constructor


 // HighScoreProcessor methods -------------------------------------------------------------------
 // ----------------------------------------------------------------------------------------------

 // HighScoreProcessor getter and setters ----------------------------
 // ---------------------------------------------------------

    // no setter for these methods
    public List<HighScore> getHighScores() {
        return highScores;  
    }


  // ----------- HighScoreProcessor non-get-set methods ---------------------------------
  // ---------------------------------------------------------------

    // Try to read and write to the file if its not locked.  
    // Results stored in canReadWriteFile and readWriteFailures. Msgs possibly displayed.
    public void testReadWriteFile(File file, int timeout, boolean queryUser, String lockMsg)  {
        List<HighScore> highScoresFromFile = null;
        boolean result = false;
        StringBuilder msg = new StringBuilder("");
        String newLine = System.lineSeparator();

        try (RandomAccessFile raf = new RandomAccessFile(file, "rw");
             FileChannel chanRaf = raf.getChannel();
             InputStream is    = Channels.newInputStream(chanRaf);
             BufferedReader br = new BufferedReader(new InputStreamReader(is));
             FileLock lockRaf  = myTryLock(chanRaf, timeout, queryUser, lockMsg); 
             OutputStream os   = Channels.newOutputStream(chanRaf);
             PrintWriter pw    = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os)));
        		)   {
             // FileLock must be created after PrintWriter, BufferedReader created or else ClosedChannelException
             // ClosedChannelException thrown even though last line of try block executes 
             // hence, this exception must be thrown while try-with-resources are auto-closing.
             // Cant find this in a book anywhere.                    
             // if queryUser false => dont query user to continue waiting.
            if (lockRaf == null) {
                return;    // dont update canReadWriteFile or readWriteFailures
            }
            highScoresFromFile = HighScoreFile.readHighScoresFromFile(br);
            raf.seek(0);     // return to beginning before writing.
            HighScoreFile.writeHighScoresToFile(highScoresFromFile, pw);
            result = true;   // can still have exception thrown by auto-close
            } catch (FileNotFoundException e) {
                    msg.append("the file is not found. File name: " + file);
                    result = false;
            } catch (OverlappingFileLockException e) {
                    msg.append("the file is already locked. File Name: " + file);
                    result = false;
            } catch (ClosedChannelException e) {
                    // this can be thrown during twr autoclose!! MJS 8.6.17
                    msg.append("of a closed channel, which is likely a programming error." + newLine);
                    msg.append("The program should try locking file after all reader/writers created!");
                    result = false;
                    // e.printStackTrace();
            } catch (IOException e) {
                    msg.append("of an IO exception of type " + e.getClass().getSimpleName());
                    result = false;
            } catch (NumberFormatException e) {
                    msg.append("of an incorrectly formatted number. Try deleting the file " + file);
                    result = false;
            } catch (InterruptedException e) {
                    appDone = true;
                    System.exit(-1);
            } catch (Exception e) {
                    msg.append("of a general non-IO exception of type " + e.getClass().getSimpleName());
                    result = false;
                    return;
            } // end try-catch-finally lock-file
        if (result == false) {
            // increment possibly displaying a message
            displayReadWriteFailureMessage(++readWriteFailures, msg.toString());
        } else {
            canReadWriteFile = true;  
        }             
    } // End testReadWriteFile()

    // Try to read and write to the file if its not locked.  
    // Results stored in canReadWriteFile and readWriteFailures. Msgs possibly displayed.
    public void testReadWriteFile(File file)  {
        int timeout = 0;  // how long to try to lock before timing out (in millisecs)
        // 0=>return immediately if file locked, false=>dont ask to repeat, "" => no msg
        testReadWriteFile(file, timeout, false, "");
    } // End testReadWriteFile()

    // Try to read and write highScores from file, returning true/false if RW suceeds or not
    // Since RW may fail based on a fileLock, this routine superseded by testReadWriteFile.
    public boolean canReadAndWriteFile(File file)  {
        List<HighScore> highScoresFromFile = null;

        try {
            highScoresFromFile = HighScoreFile.readHighScoresFromFile(file);
        } catch (Exception e) { 
            // System.out.println("HighScoreProcessor: canReadAndWriteFile: Cant read.");
            return false;
        }
        try {
            HighScoreFile.writeHighScoresToFile(highScoresFromFile, file);
        } catch (Exception e) { 
             // System.out.println("HighScoreProcessor: canReadAndWriteFile: Cant write.");
             return false;
        }  
        return true;         
    } // End canReadAndWriteFile()


    // if file has never been read and written to sucessfully before and only failed a few times, try again.
    public boolean unsureIfCanReadWriteFile()  {
        return ((canReadWriteFile == false) && (readWriteFailures < MAX_READ_WRITE_FAILURES));    
    } // End unsureIfCanReadWriteFile()


    // Ddisplay a message dialog - in children of this class.
    public void displayReadWriteFailureMessage(int failures, String reason)  {
        // display no message for this highScoreProcessor - just silently save score locally
        // Other HSPs such as FileLockDemo will display a message.   
    } // End updateReadWriteFailures



    // Add newScore if it is a high score - possible for another player to record high score after this 
    // score has been tenatively verified as a high score.  Since it takes a "long" time for user to 
    // type their name in, this must be permitted.
    // Depends on values in high score file (or high score array).
    // Routine reads highscores in file, checks if newscore is a highscore, and gets user name.
    // Routine then locks file, reads, updates, writes, unlocks. Needed in case another process also 
    // is trying to write to the file.
    public void addNewScore(int newScore)  {
        System.out.println(" HighScoreProcessor - Adding New Score of " + newScore);
        // reason must be set to non-null for any failure, or routine wont save 
        // newScore to local file.  
        String reason = "";          // exception reason
        String displayTitle = null;  // Title of window for when displaying high scores
        String lockMsg = "Continue waiting to permanently save your high score to a file?";
        lockMsg += " (Otherwise it will be saved only during this application.)";
        List<HighScore> fileScores = null;
        List<HighScore> oldScores = null;

        if (unsureIfCanReadWriteFile()) {
            // update canRWFile or readWritefailures if file not locked.
            // false => dont ask user to continue waiting if file is locked. 1000 milleSecs
            testReadWriteFile(file, 1000, false, ""); 
        }

        // get new highScores from file if possible (else use old high scores)
        int lowScore;
        if (canReadWriteFile) oldScores = reReadHighScores(file);
        if (oldScores.size() < MAX_HIGH_SCORES) {
            lowScore = 0;
        } else {
            lowScore = oldScores.get(oldScores.size()-1).getScore();
        }
        // System.out.println("The lowest high score is " + lowScore);
        if (newScore <= lowScore)  return;  // no need to update high scores

        // get highScore name outside of fileLock since user input may take very long
        HighScore newHigh = null; 
        newHigh = new HighScore(inputHighScoreName(newScore), newScore);
 
        // ready to add new high score - lock for reread-update-write
        // if can RW File ever ... wait for file to be unlocked . . . 
        // try to add it ... if added, update localHS, else add to localHS
        if (!canReadWriteFile) {
            reason = " because file can't be read and written";
        } else {  // canRWFile, try to do so
            try (RandomAccessFile raf = new RandomAccessFile(file, "rw");
                FileChannel chanRaf = raf.getChannel();
                InputStream is = Channels.newInputStream(raf.getChannel());
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                OutputStream os = Channels.newOutputStream(chanRaf);
                PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os)));
                // true => query user
                FileLock lockRaf  = myTryLock(raf.getChannel(), 10000, true, lockMsg); ) {  
        
                if (lockRaf == null) {
                    reason = " because file is locked";  // save high-score locally
                } else {
                    System.out.println("Successfully lockedx1 File: " + file);
                    fileScores = HighScoreFile.readHighScoresFromFile(br);

                    // Update - add, sort, delete if too large
                    fileScores.add(newHigh);  // add to list of highScores
                    Collections.sort(fileScores);
                    while (fileScores.size() > MAX_HIGH_SCORES) fileScores.remove(MAX_HIGH_SCORES);
 
                    // now write data
                    raf.seek(0);  // must move to start of file after reading!!
                    HighScoreFile.writeHighScoresToFile(fileScores, pw);
                    System.out.println("Add: Wrote HighScores to File: " + file);
                } // end if fileLocked or not  
            } catch (OverlappingFileLockException e) {
                reason = " because file already locked.";
            } catch (ClosedChannelException e) {
                // this can be thrown during twr autoclose!! MJS 8.6.17
                reason = " because of a file lock problem (" + e.getClass().getSimpleName();
                reason += " exception). Try locking file after all reader/writers created!";
            } catch (FileNotFoundException e) {
                reason = " because the file is not found: " + file;
            } catch (IOException e) {
                reason = "IO Exception for file: " + e.getClass().getSimpleName();
            } catch (Exception e) {
                reason = "Non-IO Exception creating file: " + e.getClass().getSimpleName();
            } // end try-catch lock-file
        } // end if canReadWriteFile
        if (reason == null || reason == "") {
            highScores = fileScores;
            displayTitle = "High Scores from File";
        } else { // Couldnt read-write to file => use local array values
            // add, sort, delete if too large
            if (newHigh == null) newHigh = new HighScore(inputHighScoreName(newScore), newScore);
            highScores.add(newHigh);  // add to list of highScores
            Collections.sort(highScores);
            while (highScores.size() > MAX_HIGH_SCORES) highScores.remove(MAX_HIGH_SCORES);
            displayTitle = "Local Copy of High Scores";
        } // end if reason null or not
        displayHighScores(highScores, displayTitle);
    
    } // end addNewScore()


    // Get updated high scores if readable from high score file, otherwise return old values.
    public List<HighScore> reReadHighScores(BufferedReader br)  {

        // System.out.println(" HighScoreProcessor(br) - reReading high scores ");
        List<HighScore> newScores = highScores;
        if (canReadWriteFile) {
            try {
                newScores = HighScoreFile.readHighScoresFromFile(br);
                Collections.sort(newScores);
                while (newScores.size() > MAX_HIGH_SCORES) newScores.remove(MAX_HIGH_SCORES);
            } catch (Exception e) { 
                System.out.println("HighScoreProcessor: reRead(br): Error reading from file.");
                System.out.println("Using old copy of high scores.");
                return highScores; // any problem => just return old high scores
            } // end try catch
        } 
        return newScores;    
    } // End reReadHighScores()



    // Get updated high scores from high score file if readable, otherwise return old values.
    public List<HighScore> reReadHighScores(File file)  {

        List<HighScore> newScores = highScores;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            newScores = reReadHighScores(br);
        } catch (IOException e) {
            // return old highScores
            return highScores;
        } // end try with resources
        return newScores;
    } // End reReadHighScores()
 

   // Display high scores - get scores from file or high score array.
    public void showHighScores(File file)  {
        // System.out.println(" HSP - Showing High Scores");
        if (canReadWriteFile) highScores = reReadHighScores(file);   
        displayHighScores(highScores); 
    } // End showHighScores()

    public void showHighScores( )  {
        // System.out.println(" HSP - Showing High Scores");
        if (canReadWriteFile) highScores = reReadHighScores(file);   
        displayHighScores(highScores); 
    } // End showHighScores()



    // Display values in high score list - use HTML formatting since showMessageDialog formats poorly.
    public void displayHighScores(List<HighScore> highScores, String title, String errorMsg)  {

        // Displaying with ShowMessageDialog will compress text - aka columns cant be 
        // aligned, even if spaces replaced with dots, so we use HTML.
        StringBuilder builder = new StringBuilder();
        System.out.println("dhs - errorMsg " + errorMsg);

        builder.append("<HTML><table border=1>");
        builder.append("<tr><td>Player Name</td><td> Game Score </td>");
        builder.append("<td> Date </td></tr><tr></tr>"); 
        if (highScores.size() == 0) {
            builder.append("<tr><td colspan='3'> No High Scores Yet Recorded </td></tr>");
        } 
        for (HighScore hs: highScores) {
           builder.append(" <tr><td>" + hs.getName());
           builder.append("</td><td>" + hs.getScore());
           builder.append("</td><td>" + hs.getDate());
           builder.append("</td></tr>");        
        }  
        if (errorMsg != null && errorMsg != "")  {
        	builder.append("<tr><td colspan='3'> " + errorMsg + "</td></tr>");
        }
        // need pane, frame or panel here . . . 
        builder.append("</table></HTML>");
        JOptionPane.showMessageDialog(null, builder.toString(), title, JOptionPane.INFORMATION_MESSAGE);  
    } // End displayHighScores()
    
    // Display values in high score list - use HTML formatting since showMessageDialog formats poorly.
    public void displayHighScores(List<HighScore> highScores, String title)  {
        displayHighScores(highScores, title, "");   
    } // End displayHighScores()


    // Display values in high score list - use HTML formatting since showMessageDialog formats poorly.
    public void displayHighScores(List<HighScore> highScores)  {
        displayHighScores(highScores, "High Scores");  
    } // End displayHighScores()




    // Retrieve values in high score file, or use defaults if file cant be read.
    public List<HighScore> getOriginalHighScores(File file) {

        List<HighScore> newList = getDefaultHighScores( );
        try {
             List<HighScore> readList = HighScoreFile.readHighScoresFromFile(file);
             newList = readList;
        } catch (IllegalArgumentException e) {
            System.out.println("Bad data found while trying to show original scores.");
            System.out.println(e.getMessage());
        } catch (FileNotFoundException e) {
            System.out.println("File not found while trying to show original scores.");
        } catch (IOException ex) {
            System.out.println("Could not read in file while trying to show original scores.");
        } catch (Exception e) {
            System.out.println("Could not read high Score data while trying to show original scores.");
            e.printStackTrace();
        } // end try-catch
        // sort and eliminate extra entries (we only want some)
        Collections.sort(newList);  // highest to lowest order
        while (newList.size() > MAX_HIGH_SCORES) newList.remove(MAX_HIGH_SCORES);
        return newList;
    } // End getOriginalHighScores()

    // this routine sets some default high scores (for when HighScore file cant be found).
    public List<HighScore> getDefaultHighScores( ) {
        List<HighScore> highList = new ArrayList<> ();

        highList.add(new HighScore("Scott Safran",   41336440, LocalDate.of(1982, 11, 13))); 
        highList.add(new HighScore("John Doe",          35000, LocalDate.of(1979, 11, 1)));
        highList.add(new HighScore("Michael Sheliga",    2500, LocalDate.of(2017, 8, 2)));
        return highList;
    } // End getDefaultHighScores()

    // get user name for a new high score
    public static String inputHighScoreName(int newScore, String prompt) {
        // user input can take very long, so dont make it thread protected.
        String title = "High Score of " + newScore + "Achieved!";
        int msgType = JOptionPane.PLAIN_MESSAGE;
        String def = "Your name";
        String name = (String) JOptionPane.showInputDialog(null, prompt, title, msgType, null, null, def);        
        if (name == null) name = "---";
        return name;
    } // End inputHighScoreName()

    // get user name for a new high score
    public static String inputHighScoreName(int newScore) {
        // user input can take very long, so dont make this thread protected normally.
        String title = "New High Score of " + newScore + " Achieved.";
        return inputHighScoreName(newScore, title);
    } // End inputHighScoreName()

    // Get the game score from the user for a new high score. 0=>exit
    // Keep this routine in general highScoreProcessor even though not called from it, 
    // in order to keep input routines in same file. 
    public static Integer inputHighScoreGameScore(String prompt) {
        // user input can take very long, so normally dont make routine synch protected.
        String title = "Set your score as desired (for testing).";
        int msgType = JOptionPane.PLAIN_MESSAGE;
        String def = "5200";
        String stringScore = (String) JOptionPane.showInputDialog(null, prompt, title, msgType, null, null, def);        
        Integer gameScore = 0;
        if (stringScore != null) { 
            try { 
                gameScore = Integer.valueOf(stringScore); 
            } catch (Exception e) {
                System.out.println("Bad number value. Setting score to 0."); 
            }
        }  // end-if
        if (gameScore < 0) gameScore = 0;  
        return gameScore;
    } // End inputHighScoreGameScore(prompt)

    // get the game score from the user for a new high score. 0=>exit
    public static Integer inputHighScoreGameScore() {

        // user input can take very long, so normally this routine not inside synched.
        String prompt = "Please input a GameScore.";
        return inputHighScoreGameScore(prompt);
    } // End inputHighScoreGameScore()

} // end class HighScoreProcessor

