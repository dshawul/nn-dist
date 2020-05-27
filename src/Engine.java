import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.regex.Pattern;

/** Super class for Engines
 */
public class Engine extends Thread {
    public  String cmdLine;
    public  String path;
    public  String name;
    public  String option;
    public  Manager myManager;
    public  int id;
    public  long start_t;
    public  Type type;
    public  volatile State done;
    public volatile boolean gotReply;
    protected enum State {
        STARTED, CONNECTED, WAITING, LOGGED, FAILED
    }
    protected enum Type {
        NONE, TCPS, TCP
    }
    public Engine() {
        myManager = null;
        cmdLine = "";
        path = "";
        name = "";
        option = "";
        type = Type.NONE;
        done = State.STARTED;
    }
    public void kill() {}
    public void send(String str) {}
    public void sendFile(byte[] content) {}
    public void recvFile(byte[] content, int size) {}
    boolean isDone() {
        return true;
    }
    boolean hasFailed() {
        return (done == State.FAILED);
    }
    public boolean isTcps() {
        return (type == Type.TCPS);
    }
    public boolean isTcp() {
        return (type == Type.TCP);
    }
    public boolean isServer() {
        return (isTcps());
    }
    void printDebug(String str) {
        myManager.printDebug(str,id);
    }
    public void setUserPass(String u,String p) {
    }
    public static boolean isSame(String str1,String str2) {
        return str1.equals(str2);
    }
    public static boolean hasString(String str1,String str2) {
        return (str1.indexOf(str2) != -1);
    }
}
/** Player through socket connection
 */
abstract class SocketEngine extends Engine {
    protected PrintWriter output;
    protected InputStream input_stream;
    protected OutputStream output_stream;
    protected Socket mySocket;
    protected String host;
    protected int port;
    protected String userName;
    protected String passWord;
    protected boolean isClient;
    
    private static final Pattern usernamePattern = Pattern.compile(
            ".*login:.*",
            Pattern.DOTALL);
    private static final Pattern passwordPattern = Pattern.compile(
            ".*password:.*",
            Pattern.DOTALL);
    
    SocketEngine(String cmd) {
        super();
        Scanner sc = new Scanner(cmd);
        host = sc.next();
        port = sc.nextInt();
        name = host;
        userName = "";
        passWord = "";
        while(sc.hasNext()) {
            userName = sc.next();
            if(userName.isEmpty()) continue;
            name = userName + "@" + host;
            break;
        }
        if(!userName.isEmpty()) {
            while(sc.hasNext()) {
                path = sc.next();
                if(path.isEmpty()) continue;
                passWord = path;
                break;
            }
        }
        cmdLine = cmd;
        path = "socket";
        mySocket = null;
        isClient = true;
        sc.close();
    }
    SocketEngine(String cmd,Socket skt) {
        this(cmd);
        mySocket = skt;
        isClient = false;
    }
    @Override
    public void setUserPass(String u,String p) {
        userName = u;
        passWord = p;
    }
    @Override
    boolean isDone() {
        return(done == State.CONNECTED ||
                done == State.FAILED);
    }
    @Override
    public void send(String str) {
        output.println(str);
    }
    public String readLn() {
        StringBuffer buffer = new StringBuffer();
        try {
            int c = input_stream.read();
            for (; c != -1; c = input_stream.read()) {
                if (c == '\n')
                    break;
                buffer.append((char)c);
            }
            if(c != -1)
                return buffer.toString();
        } catch (Exception e) {};
        return null;
    }
    @Override
    public void sendFile(byte[] content) {
        try {
            output_stream.write(content, 0, content.length);
            output_stream.flush();
        } catch (Exception e) {}
    }
    @Override
    public void recvFile(byte[] content, int length) {
        try {
            int rd = 0;
            while (rd < length) {
                int result = input_stream.read(content, rd, length - rd);
                if (result == -1) 
                    break;
                rd += result;
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
    boolean is_ready() {
        boolean ready = false;
        try {
            ready = (input_stream.available() >  0);
        } catch (Exception e) {};
        return ready;
    }

    @Override
    @SuppressWarnings("unused")
    public void run() {
        try {
            if(isClient) {
                printDebug("Trying to connect to server...");

                SocketAddress sockaddr = new InetSocketAddress(host, port);
                mySocket = new Socket();
                mySocket.connect(sockaddr, 10000);

                printDebug("Connected!");
            }

            input_stream = mySocket.getInputStream();
            output_stream = mySocket.getOutputStream();
            output = new PrintWriter( new OutputStreamWriter(output_stream), true);
            
            String str;

            if(!isClient) {
                final String Intro = 
                    "\n\n" +               
                    "           .---.            ,--,                                ____                    \n" +
                    "          /. ./|          ,--.'|                              ,'  , `.                  \n" +     
                    "      .--'.  ' ;          |  | :               ,---.       ,-+-,.' _ |                  \n" +     
                    "     /__./ \\ : |          :  : '              '   ,'\\   ,-+-. ;   , ||                \n" +     
                    " .--'.  '   \\' .   ,---.  |  ' |      ,---.  /   /   | ,--.'|'   |  || ,---.           \n" +     
                    "/___/ \\ |    ' '  /     \\ '  | |     /     \\.   ; ,. :|   |  ,', |  |,/     \\       \n" +     
                    ";   \\  \\;      : /    /  ||  | :    /    / ''   | |: :|   | /  | |--'/    /  |        \n" +     
                    " \\   ;  `      |.    ' / |'  : |__ .    ' / '   | .; :|   : |  | ,  .    ' / |         \n" +     
                    "  .   \\    .\\  ;'   ;   /||  | '.'|'   ; :__|   :    ||   : |  |/   '   ;   /|        \n" +     
                    "   \\   \\   ' \\ |'   |  / |;  :    ;'   | '.'|\\   \\  / |   | |`-'    '   |  / |     \n" +     
                    "    :   '  |--\" |   :    ||  ,   / |   :    : `----'  |   ;/        |   :    |         \n" +     
                    "     \\   \\ ;     \\   \\  /  ---`-'   \\   \\  /          '---'          \\   \\  /   \n" +     
                    "      '---\"       `----'             `----'                           `----'           \n" +     
                    "                                                                                        \n" +     
                    "\n\n" + 
                    "            Welcome to NTS (Network training server) for Scorpio Zero                   \n" +
                    "                        http://scorpiozero.ddns.net                                     \n" +
                    "                                                                                        \n";
                
                send(Intro);

                String info = "\n\n Please enter a username and password and an account will be created.\n" +
                              " The password will be stored in plain text, so avoid using passwords used \n" +
                              " for sensitive applications. \n\n";
                send(info);

                while(true) {
                    send("login:");
                    userName = readLn().trim();
                    send("password:");
                    passWord = readLn().trim();
                    if(!myManager.dbm.checkUser(userName, passWord)) {
                        send("\nUsername password combination is incorrect. Please try again.\n");
                        continue;
                    }
                    break;
                }
                
                name = userName + "@" + host;
                name = "[" + userName + "] [" + passWord + "]";
                printDebug("user = [" + name + "]");
                
                send("\n**** Starting NTS session as " + userName + " ****\n");
            }
            
            start_t = System.currentTimeMillis();
            done = State.CONNECTED;

            while((str = readLn()) != null) {
                if(!processCommands(str))
                    break;
            }
            if(!isClient)
                printDebug("Server disconnected.");
            else
                printDebug("Client disconnected.");
        } catch (Exception e) {
            printDebug("Engine failure: " + cmdLine + " Error message: " + e.getMessage());
        }
        
        done = State.FAILED;
        Manager.HandleDisconnects(this);
    }

    private int logging = 0;
    public boolean handleLogin(String str) {
        if(logging >= 6)
            return false;
        if(usernamePattern.matcher(str).matches()) {
            logging++;
            if(!userName.isEmpty())
                send(userName);
            else {
                Scanner in = new Scanner(System.in);
                userName = in.nextLine();
                send(userName);
            }
            return true;
        }
        if(passwordPattern.matcher(str).matches()) {
            logging++;
            if(!passWord.isEmpty())
                send(passWord);
            else {
                Scanner in = new Scanner(System.in);
                passWord = in.nextLine();
                send(passWord);
            }
            return true;
        }
        return false;
    }
    @Override
    public void kill() {
        send("quit");
        try {
            output.close();
            mySocket.close();
            printDebug("Connection lost!");
        } catch (IOException e) {
            printDebug("Kill engine failure: " + e.getMessage());
        }
        super.kill();
    }
    public void recvSaveFile(String name, boolean append) {
        String cmd = readLn();
        int length = Integer.parseInt(cmd.trim());
        byte[] buffer = new byte[length];
        printDebug("Recieving " + name + " : " + cmd + " bytes");
        recvFile(buffer, length);
        try {
            FileOutputStream fos = new FileOutputStream(name, append);
            synchronized(this) {
                fos.write(buffer, 0, length);
            }
            fos.close();
        } catch (Exception e) {}
        cmd = readLn();
        printDebug(cmd);
    }
    abstract boolean processCommands(String str);
}
/** A client engine for ScorpioZero server
 * @see TcpServerEngine
 */
class TcpClientEngine extends SocketEngine {
    private static final Pattern loginPattern = Pattern.compile(
            "^.*\\*\\*\\*\\* Starting NTS session as.*",
            Pattern.DOTALL);
    private String parameters = null;
    private boolean net_recieved;
    
    TcpClientEngine(String cmd) {
        super(cmd);
        path = "tcp";
        type = Type.TCP;
        net_recieved = false;
    }
    private boolean processLogin(String str) {
        if(loginPattern.matcher(str).matches()) {
            if(done != State.LOGGED) {
                done = State.LOGGED;
            }
            myManager.WriteEngines(userName, passWord);
            return true;
        }
        return false;
    }
    @Override
    boolean processCommands(String str) {
        printDebug(str);
        
        if(processLogin(str))
            return true;
        
        if(handleLogin(str))
            return true;
        
        String OS = System.getProperty("os.name");
        boolean isWindows = OS.startsWith("Windows");

        Scanner sc = new Scanner(str);
        sc.useDelimiter("[=\\s]");
        String cmd;
        
        while(sc.hasNext()) {
            cmd = sc.next();
            if(isSame(cmd,"<parameters>")) {
                parameters = "";
                while(sc.hasNext())
                    parameters += sc.next() + " ";
                cmd = readLn();
                printDebug(cmd);
            } else if(isSame(cmd,"<network-uff>") || isSame(cmd,"<network-pb>")) {
                String net;
                if ( isSame(cmd,"<network-uff>") ) {
                    net = "net.uff";
                    String dir = System.getProperty("user.dir") + File.separator;
                    String[] delcmd = null;
                    if(isWindows)
                        delcmd = new String[]{"cmd","/c","DEL " + dir + "*.trt"};
                    else
                        delcmd = new String[]{"bash","-c","rm -rf " + dir + "*.trt"};
                    try {
                        Process proc = Runtime.getRuntime().exec(delcmd);
                        proc.waitFor();
                    } catch (Exception e) {
                        printDebug("Could not delete trt files!");
                    }
                } else {
                    net = "net.pb";
                }
                net_recieved = true;
                recvSaveFile(net,false);
            }
        }
        
        sc.close();
        
        if(done != State.LOGGED || parameters == null)
            return true;
        
        if(!net_recieved)
            return true;
        
        while(!is_ready()) {
            
            // execute job
            try {
                String command = System.getProperty("user.dir") + File.separator +
                                 "scripts" + File.separator;
                if(isWindows) command += "job-client.bat";
                else command += "job-client.sh";
                printDebug("Executing job : " + command);
                command += " " + parameters;
                Process proc = Runtime.getRuntime().exec(command.split(" "));

                BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(proc.getInputStream()));
                String s = null;
                while ((s = stdInput.readLine()) != null)
                    System.out.println(s);

                proc.waitFor();
                printDebug("Finished executing job");
            } catch (Exception e) {
                printDebug("Could not execute job!");
                return false;
            }

            // send games
            try {
                byte[] content;
                String message;
                
                //games
                content = Files.readAllBytes(Paths.get("cgames.pgn"));
                
                int count = new String(content).split("Result").length - 1;
                
                message = "<games>\n";
                message += count + "\n";
                message += content.length;
                send(message);
                
                sendFile(content);
                
                message = "</games>";
                send(message);
                
                //train
                content = Files.readAllBytes(Paths.get("ctrain.epd"));
                        
                message = "<train>\n";
                message += content.length;
                send(message);
                
                sendFile(content);
                
                message = "</train>";
                send(message);
                
                if(output.checkError()) {
                    printDebug("Server down!");
                    return true;
                }
            } catch (Exception e) {
                printDebug("Could not send games to server!");
                return false;
            }
        }
        
        return true;
    }
}
/** ScorpioZero training server
 * @see TcpClientEngine
 */
class TcpServerEngine extends SocketEngine {

    TcpServerEngine(String cmd,Socket skt) {
        super(cmd,skt);
        path = "tcps";
        type = Type.TCPS;
    }
    @Override
    public void kill() {
    }
    @Override
    boolean processCommands(String str) {
        printDebug(str);
        Scanner sc = new Scanner(str);
        sc.useDelimiter("[=\\s]");
        String cmd;
        
        while(sc.hasNext()) {
            cmd = sc.next();
            if(isSame(cmd,"<games>")) {
                cmd = readLn();
                int games = Integer.parseInt(cmd.trim());
                try {
                    myManager.dbm.addContrib(userName,myManager.workID,games);
                } catch (Exception e) {
                    printDebug(e.getMessage());
                }
                recvSaveFile("cgames.pgn",true);
            } else if(isSame(cmd,"<train>")) {
                recvSaveFile("ctrain.epd",true);
            }
        }
        
        sc.close();
        return true;
    }
}
