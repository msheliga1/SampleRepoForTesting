/* Class MyTryLock - MJS 8.5.17
   This class implements 4 overloaded versions of static MyTryLock function. 
   The method with all parameters calls tryLock with a total wait time, a boolean 
   indicating if to ask the user to continue waiting, a prompt for this message, 
   and a boolean to indicate if the user should be asked how long to contine waiting.
   The other 3 overloaded methods call the full parameter method with default values.
   Ideally this would extend nio.channels.FileChannel, but since FileChannel is abstract
   and has around 30 methods, this would take quite a bit of doing.
   New Line in Unicorn Branch
*/
    
package util;
import java.io.IOException;
import java.nio.channels.*;
import javax.swing.JOptionPane;

public class MyTryLock {

    static final int TICK_TIME = 50;  // divide total wait time into chunks of this amount in millisecs

    // Method expands on tryLock method from fileChannels - adds timeout time if we should keep
    // trying, and queryUser to see if user should be queried after timeout time, msg to 
    // display to ask if to wait further, and resetWaitTime boolean if method should ask user 
    // how long to wait until next timeout. 
    public static FileLock myTryLock(FileChannel fc, int timeout, boolean queryUser, String msg, 
           boolean resetWaitTime) throws InterruptedException, IOException {
    // fc - fileChannel used to access fileLock
    // timeout - total amount of time to wait if file is already locked
    // queryUser - true => ask user if they wish to continue waiting
    // msg - message to display when asking if user if they wish to continue
    // resetWaitTime - true => ask user how long to wait in future.

    // System.out.println("Starting MyTryLock. - msg: " + msg);
    FileLock fl = fc.tryLock();  // throws IOException
    if (fl != null) return fl;
    int waitLoops;

    String timeString = null;
    int newTime = 0;
    int waitTime = timeout;  // in milliSeconds

    boolean done = (waitTime <= 0);  // exit if no timeout since we just did a tryLock
    while (!done) {
        // System.out.println("My try lock. Could not acquire lock. Waiting millisecs: " + waitTime);
        waitLoops = waitTime / TICK_TIME + 1;  
        for (int i = 1; i <= waitLoops; i++)  { 
            Thread.sleep((long) TICK_TIME);  // throws interruptedException
            fl = fc.tryLock();
            if (fl != null) return fl;
        } // end for waitLoops

        if (!queryUser) return null;
        if (waitTime > 2000) {
            timeString = "" + waitTime/1000 + " seconds";
        } else {
            timeString = "" + waitTime + " milliSeconds";
        }
        String prompt = "File busy for " + timeString + ". " + msg;
        String title = "File Busy Notification.";
        int options = JOptionPane.YES_NO_OPTION;
        int dialogAnswer = JOptionPane.showConfirmDialog(null, prompt, title, options);
        if (dialogAnswer != JOptionPane.YES_OPTION) {
            return null;
        } else if (resetWaitTime) {
            timeString = JOptionPane.showInputDialog("Keep waiting for how many milliseconds?");
            try {
                newTime = Integer.parseInt(timeString);
            } catch (Exception e) {
                newTime = waitTime;
            }
            newTime = (newTime < 0) ? 0 : newTime;
            waitTime = newTime; 
        } // end if-else
    } // end while !done
    return fl;
    } // end myTryLock 

    public static FileLock myTryLock(FileChannel fc, int timeout, boolean queryUser, String msg) 
                                     throws InterruptedException, IOException {
    // fc - fileChannel used to access fileLock
    // timeout - total amount of time to wait if file is already locked
    // queryUser - true => ask user if they wish to continue waiting
    // msg - message to display when asking if user if they wish to continue
    // resetWaitTime - true => ask user how long to wait in future. (false below).
        return myTryLock(fc, timeout, queryUser, msg, false); 
    }
 
    public static FileLock myTryLock(FileChannel fc, int timeout, boolean queryUser) 
                           throws InterruptedException, IOException {
    // fc - fileChannel used to access fileLock
    // timeout - total amount of time to wait if file is already locked
    // queryUser - true => ask user if they wish to continue waiting
    // msg - message to display when asking if user if they wish to continue (empty string below)
        return myTryLock(fc, timeout, queryUser, ""); 
    } // end myTryLock 

    public static FileLock myTryLock(FileChannel fc, int timeout) 
                           throws InterruptedException, IOException {
    // fc - fileChannel used to access fileLock
    // timeout - total amount of time to wait if file is already locked
    // queryUser - true => ask user if they wish to continue waiting (false below)
        return myTryLock(fc, timeout, false); 
    } // end myTryLock 

}  // end class myTryLock
