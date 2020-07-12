import java.io.BufferedReader;
import java.io.InputStreamReader;

/** A console interface
 */
public class ConsoleInterface {
    public static void main(String args[]) {
        ConsoleManager manager = new ConsoleManager();
        manager.ProcessCommands(args);
        manager.onStart();
    }
}

/** Console manager that reads input from console
 */
class ConsoleManager extends Manager {
    @Override
    public void onStart() {
        if(isServer) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    try {   
                        BufferedReader input = new BufferedReader(
                                new InputStreamReader(System.in));
                        
                        String str;
                        while ((str = input.readLine()) != null) {
                            str = "-" + str;
                            String[] command = str.split(" ");
                            ProcessCommands(command);
                        }
                    } catch (Exception e) {
                        printDebug("Server error message: " + e.getMessage(),0);
                    }
                }
            };
            new Thread(r).start();
        }
    }
}
