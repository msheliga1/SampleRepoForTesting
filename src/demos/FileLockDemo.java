/* File Lock Demo program - uses highScore class and extends highScoreProcessor to display 
file locking and high score file and local array storing of high scores.
8.2.17 - MJS
Get high scores from user, get current high scores from file or local copy if file cant be read,
 if new high score, get name and 
insert high score into list, write to file.
*/

/* Results: tryLock always seems to allow execution to proceed, but subsequent read on file 
will crash.  Lock seems to forcefully block the second process to call it.
*/
// Comment added to Eclispe Project copied from GitHub 2.2.18 MJS
// Comment with Skip.

package demos;             // demo program to test locks - mainly using high score samples 
import games.HighScore;  
import games.HighScoreFile;
import games.HighScoreClassInfo;
import games.HighScoreProcessor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile; 
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.OverlappingFileLockException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;      
import javax.swing.JOptionPane;
import util.MyTryLock;

// ---------------------------------------------------------------------
public class FileLockDemo extends HighScoreProcessor implements Runnable, HighScoreClassInfo {

    private static AtomicLong count = new AtomicLong();   // total objects of this type created
    static final String HIGH_SCORE_FILENAME  = "LockDemoHighScores.txt";
    static final int MAX_HIGH_SCORES = 5;     // high scores to be kept track of
    // after this number of rw failures without sucess, give up - doesnt count locked file as failure
    static final int MAX_READ_WRITE_FAILURES = 2;  

    // inherited instance variables
    // protected long id;
    // protected BlockingQueue<Integer> queue;  // unused here since user just types in new score
    // protected List<HighScore> highScores = new ArrayList<HighScore>();
    // protected File file;
    // protected int readWriteFailures; 
    // protected boolean canReadWriteFile;
    // protected boolean appDone;   // appDone when interruped or user indicates.      


  // ----------- FileLockDemo Main-Run ---------------------------------
  // -------------------------------------------------------------------

    public static void main(String[] args) {
 
      Thread lockDemo = new Thread(new FileLockDemo());
      ExecutorService executor = Executors.newSingleThreadExecutor();  
      executor.execute(lockDemo);    
      // shutdown allows already submitted processes to finish. 
      executor.shutdown();
      // wait for all tasks to finish - not strictly necessary if no code afterards.
      try { 
          // this should never timeout
          executor.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS); 
      } catch (InterruptedException e) {
          executor.shutdownNow();              // shutdown all threads immediately
          Thread.currentThread().interrupt();  // preserve interrupt status 
      }
    } // end main


    public void run( ) { 
      displayIntroMessage();
      while (!appDone) { 
        String prompt = "Input Score. Enter 0 to exit. (Process will attempt to lock file once score is entered.)";   
        Integer newScore = HighScoreProcessor.inputHighScoreGameScore(prompt);
        if (newScore == 0) {
            appDone = true;
        } else {
            addNewScore(newScore);
        }
      }  // end while
    } // end main

  // ----------- FileLockDemo Constructors ---------------------------------
  // -----------------------------------------------------------------------
   // FileLockDemo Constructors 
   public FileLockDemo()  {
       // cant pass this to super() so dont rely on super call to initialize anything
       // System.out.println("Starting FileLockDemo constructor");
       id = count.incrementAndGet();
       queue = null;   // queue unused since user just types in high score.
       // save high score file in same directory as this demo application.
       HighScoreClassInfo classRef = (HighScoreClassInfo) this;
       file = new File(HighScoreFile.getClassPath(classRef), 
                            classRef.getHighScoreFileName());
       readWriteFailures = 0; 
       canReadWriteFile = false;  
       appDone = false;
       // get HighScores from file, or default if file doesnt exist or is locked or inaccessible
       // System.out.println("FileLockDemo constructor file is " + file);
       highScores = getOriginalHighScores(file); 
       // displayHighScores(highScores);
       // write these to a file . . . in case they are defaults ... need file to testRWing .
       try {
           if (!file.exists()) HighScoreFile.writeHighScoresToFile(highScores, file);
           // next routine presumes highScoreFile exists. Updates canRWFile or failures if file not locked  
       } catch (Exception e) { 
           // if we cant write to highScore file . . . 
       } // end try-catch 
       testReadWriteFile(file);   // might update canRWFile or RWFailures
       // System.out.println("Ending FileLockDemo constructor - canRWFile: " + canReadWriteFile + 
       //                    " Failures " + readWriteFailures);
    } // end FileLockDemo constructor


 // ------------ FileLockDemo methods ----------------------------------------------------------
 // --------------------------------------------------------------------------------------------

 // ------------ FileLockDemo getter and setters ----------------------------------
 // -------------------------------------------------------------------------------
    // from superclass
    // public List<HighScore> getHighScores() {


  // ----------- FileLockDemo non-get-set methods ---------------------------------
  // ------------------------------------------------------------------------------


    // public void testReadWriteFile(File file, int timeout, boolean queryUser, String lockMsg)  

    // Display a message of file RW failure if its called for.
    @Override
    public void displayReadWriteFailureMessage(int failures, String reason)  {
    	// failures - number of times attempt to RW file has failed.
    	// reason - text message explaining why RW failed.
        // System.out.println("displayReadWriteFailureMessage - Starting");
        String newLine = System.lineSeparator();
        if (failures == 1) {
            StringBuilder msg = new StringBuilder("");
            msg.append("This app demonstrates file locks for different processes." + newLine + newLine);
            msg.append("However it appears that it can not read and write the file." + newLine);  // used for exception message.
            msg.append("While it will still function, to see it fully function, file access must be permitted." + newLine);
            msg.append("The file will be saved in the same directory as the java program, so it will not ");
            msg.append("function if run from a .jar archive." + newLine + newLine);
            msg.append("The file could not be written because of a ");
            JOptionPane.showMessageDialog(null, msg.toString() + reason);
        }
        if (failures == MAX_READ_WRITE_FAILURES) {
            StringBuilder msg = new StringBuilder("Could not read-write file after ");
            msg.append(MAX_READ_WRITE_FAILURES + " tries." + newLine + newLine);
            msg.append("Giving up on saving score to the file and saving all scores locally only.");
            JOptionPane.showMessageDialog(null, msg.toString());
        }         
    } // End displayReadWriteFailureMessage


    // this works - seek(0) after reading will overwrite file.
    @Override
    public void addNewScore(int newScore)  {
        // ready to add a new score - synch for reread-update-write 
        String prompt = null;
        HighScore newHigh = null;
        List<HighScore> fileScores = new ArrayList<>();
        // reason must be set to non-null for any failure, or routine wont save 
        // newScore to local buffer.
        String displayTitle = "";  // Title for pop-up window when displaying high scores
        String reason = "";        // reason could not use file (no rw, locked, or exception)
        String lockMsg = "Continue waiting to permanently save your high score to a file?";
        lockMsg += " (Otherwise it will be saved only during this application.)";

        // if can RW File ever ... wait for file to be unlocked . . . 
        // try to add it ... if added, update localHS, else add to localHS
        if (unsureIfCanReadWriteFile()) { 
            // update canRWFile or readWritefailures if file not locked.
            // true => prompt user to continue waiting, 10000 => 10 seconds
            testReadWriteFile(file, 10000, true, lockMsg);  
        }

        // if file is read-write-able, wait for file lock to be released.
        if (unsureIfCanReadWriteFile()) {
        	reason = " becuase it is uncertain if the file can be read and writtenm.";
        } else if (!canReadWriteFile) {
            reason = " because file can't be read and written";
        } else { // canRWFile . . . try to do so now.
            try (RandomAccessFile raf = new RandomAccessFile(file, "rw");
                 FileChannel chanRaf  = raf.getChannel();
                 InputStream is = Channels.newInputStream(raf.getChannel()); 
                 BufferedReader br = new BufferedReader(new InputStreamReader(is));
                 // true => ask user to continue waiting, 10000 => 10 seconds
                 OutputStream os = Channels.newOutputStream(raf.getChannel()); 
                 PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os)));
                 FileLock lockRaf = MyTryLock.myTryLock(chanRaf, 10000, true, lockMsg);
            		                                 									) { 
 
                System.out.println("FileLockDemo:Add - canRW file - tried lock = " + lockRaf);
                if (lockRaf == null) {
                    reason = " because the file is locked"; 
                } else {                   
                    // User input can take very long, so dont make it thread protected *normally*.
                    // but for tesing purposes, we do so here.
                    String prompt2 = "Please input your name. (This process currently has a lock on the file.)";
                    String newName = HighScoreProcessor.inputHighScoreName(newScore, prompt2);
                    newHigh = new HighScore(newName, newScore);                  

                    // BufferedReader br2 = new BufferedReader(new FileReader(new File("tempx")));  
                    fileScores = HighScoreFile.readHighScoresFromFile(br);
                    System.out.println("FileLockDemo:fileScores.size " + fileScores.size());
                    fileScores.add(newHigh);  // add to list of highScores
                    // sort, delete if too large
                    Collections.sort(fileScores);
                    while (fileScores.size() > MAX_HIGH_SCORES) fileScores.remove(MAX_HIGH_SCORES);

                    raf.seek(0);  // without this print will write to after last read location.
                    HighScoreFile.writeHighScoresToFile(fileScores, pw);
                } // end if locked or not 
            } catch (OverlappingFileLockException e) {
                    reason = " because the file is already locked.";
            } catch (ClosedChannelException e) {
                    // this can be thrown during twr autoclose!! MJS 8.6.17
                    reason = " because of a file lock problem (" + e.getClass().getSimpleName();
                    reason += " exception). Try locking file after all reader/writers created!";
            } catch (FileNotFoundException e) {
                    reason = " because the file is not found: " + file;
            } catch (IOException e) {
                    reason = " because of an IO Exception for file: " + e.getClass().getSimpleName();
            } catch (Exception e) {
                    reason = " because of a Non-IO Exception creating file: " + e.getClass().getSimpleName();
            } // end try-catch lock-file
        } // end if canRWFile
        if (reason == null || reason == "") {   // data added sucessfully - no fileLock or exceptions
            highScores = fileScores;
            displayTitle = "High Scores from File";
        } else {   // add data to local copy of high scores
            if (newHigh == null) {  // only ask for name if not already input.
                prompt = "Input your name (To be recorded temporarily" + reason + ").";
                String newName = HighScoreProcessor.inputHighScoreName(newScore, prompt);
                newHigh = new HighScore(newName, newScore);
            }  // end if newHigh==null      
            highScores.add(newHigh);  // add to list of highScores
            // sort, delete if too large
            Collections.sort(highScores);
            System.out.println("FileLockDemo:Add - added local HS size is " + highScores.size());
            while (highScores.size() > MAX_HIGH_SCORES) highScores.remove(MAX_HIGH_SCORES);
            displayTitle = "High Scores (Local Copy)";
            reason = "Could not save the new score to a file " + reason;
        }
        displayHighScores(highScores, displayTitle, reason);
    
    } // End addNewScore()


    // Display introductory message about this demo file locking program.
    public static void displayIntroMessage( ) {
      String newLine = System.lineSeparator();
      String msg = "Welcome to the filelock and score saving demonstartion program. " + newLine + newLine;
      msg += "To see filelocks in action you will need to start 2 versions of this program at once." + newLine;
      msg += "You may do so using a single version of Eclipse, or another IDE." + newLine; 
      msg += "Alternatively, two different command windows coould be used by typing 'java demos.FileLockDemo'," + newLine;
      msg += "(presuming the program is in the demos subdirectory of your java classes directory)." + newLine; 
      msg += "Alternatively you may invoke multiple versions of this program using " + newLine;
      msg += "the application manager if you have access to it." + newLine + newLine;
      msg += "In each case the program will need to be able to read and write files to ";
      msg += "fully function." + newLine + newLine;
      msg += "This program also demonstrates new score being added properly wether or not the " + newLine;
      msg += "file is currently locked, or even if it is totally inaccessible." + newLine;
      msg += "If the file is inaccessible new scores are stored to a local buffer." + newLine;
      JOptionPane.showMessageDialog(null, msg);

    } // End displayIntroMessage( ) 

    // ------------ getHighScoreClassInfo interface methods -------------------------
    public String getHighScoreFileName( ) {
        return HIGH_SCORE_FILENAME;
    }


} // end class FileLockDemo


