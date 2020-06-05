import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.net.URL;
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
    public boolean sendFile(byte[] content) { return false; }
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
        try {
            output.println(str);
        } catch(Exception e) {
            printDebug("Send: " + e.getMessage());
        }
    }
    public String readLn() {
        StringBuffer buffer = new StringBuffer();
        try {
            int c = input_stream.read();
            while(true) {
                if (c == -1)
                    break;
                if (c == '\n')
                    break;
                buffer.append((char)c);
                c = input_stream.read();
            }
            if(c != -1)
                return buffer.toString();
        } catch (Exception e) {
            printDebug("ReadLn: " + e.getMessage());
        };
        return null;
    }
    boolean is_ready() {
        boolean ready = false;
        try {
            ready = (input_stream.available() >  0);
        } catch (Exception e) {
            printDebug("is_ready: " + e.getMessage());
        };
        return ready;
    }

    @Override
    @SuppressWarnings("unused")
    public void run() {

        boolean reconnect = false;

        do {

            try {

                if(isClient) {
                    printDebug("Trying to connect to server...");

                    SocketAddress sockaddr = new InetSocketAddress(host, port);
                    mySocket = new Socket();
                    mySocket.setSoTimeout(0);
                    mySocket.connect(sockaddr, 10000);

                    printDebug("Connected!");
                    reconnect = false;
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
                    printDebug("user = [" + name + "]");

                    send("\n**** Starting NTS session as " + userName + " ****\n");
                }

                start_t = System.currentTimeMillis();
                done = State.CONNECTED;

                while((str = readLn()) != null) {
                    if(!processCommands(str))
                        break;
                }
            } catch (Exception e) {
                printDebug("Engine failure: " + cmdLine);
                printDebug("Error message: " + e.getMessage());
            }

            if(isClient) {
                if(reconnect) {
                    try {
                        printDebug("Waiting for 30 sec ...");
                        Thread.sleep(30000);
                    } catch(Exception e) {
                        printDebug("Sleep: " + e.getMessage());
                    }
                }
                reconnect = true;
                printDebug("Reconnecting ...");
            }

        } while(reconnect);

        if(!isClient)
            printDebug("Client " + name + " disconnected.");
        else
            printDebug("Server " + name + " disconnected.");
        
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
    @Override
    public boolean sendFile(byte[] content) {
        try {
            output_stream.write(content, 0, content.length);
            output_stream.flush();
            return true;
        } catch (Exception e) {
            System.out.println("Error sending file: " + e.getMessage());
            return false;
        }
    }
    public void recvSaveFile(String fname, boolean append) {
        String cmd = readLn();
        int length = Integer.parseInt(cmd.trim());
        byte[] buffer = new byte[length];
        try {
            printDebug("Recieving " + fname + " : " + length + " bytes from " + name);
            int rd = 0;
            while (rd < length) {
                int result = input_stream.read(buffer, rd, length - rd);
                if (result == -1) 
                    break;
                rd += result;
            }
            FileOutputStream fos = new FileOutputStream(fname, append);
            synchronized(this) {
                fos.write(buffer, 0, length);
            }
            fos.close();
        } catch (Exception e) {
            System.out.println("Error recieving file: " + e.getMessage());
        }
        cmd = readLn();
        printDebug(cmd);
    }
    public boolean sendGames() {
        try {
            File file = new File("cgames.pgn");

            if(file.exists()) {
                byte[] content;
                String message;

                //games
                content = Files.readAllBytes(Paths.get("cgames.pgn"));

                int count = new String(content).split("Result").length - 1;
                printDebug("Sending " + count + " games to server");

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
                    printDebug("Connection lost!");
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            printDebug("Could not send games to server!");
            return false;
        }
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
    private boolean reconnect = false;
    
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
    private long getChecksum() {
        try {
            File settings = new File("checksum.txt");
            if(settings.exists()) {
                BufferedReader reader = new BufferedReader( 
                    new FileReader("checksum.txt"));
                String cmd = reader.readLine();
                long checksum = Long.parseLong(cmd.trim());
                reader.close();
                return checksum;
            } else {
                return 0;
            }
        } catch(Exception e) {
            printDebug("getChecksum: " + e.getMessage());
            return 0;
        }
    }
    private void deleteFiles(String names, boolean isWindows) {
        String dir = System.getProperty("user.dir") + File.separator;
        String[] delcmd = null;
        if(isWindows)
            delcmd = new String[]{"cmd","/c","DEL " + dir + names};
        else
            delcmd = new String[]{"bash","-c","rm -rf " + dir + names};
        try {
            Process proc = Runtime.getRuntime().exec(delcmd);
            proc.waitFor();
        } catch (Exception e) {
            printDebug("Could not delete files: " + names);
        }
    }
    private boolean downloadNet(String FILE_URL, String FILE_NAME) {
        try {
            printDebug("Downloading new network from: " + FILE_URL);
            URL website = new URL(FILE_URL);
            ReadableByteChannel rbc = Channels.newChannel(website.openStream());
            FileOutputStream fos = new FileOutputStream(FILE_NAME);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            printDebug("Finished downloading netowrk!");
            return true;
        } catch (Exception e) {
            printDebug("Error in downloading network: " + FILE_URL);
            return false;
        }
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
            } else if(isSame(cmd,"<checksum>")) {

                cmd = readLn();
                printDebug(cmd);
                long checksum = Long.parseLong(cmd.trim());
                String net_url = readLn();
                printDebug(net_url);
                cmd = readLn();
                printDebug(cmd);

                long checksum_exist = getChecksum();
                if(checksum == checksum_exist) {
                    net_recieved = true;
                    printDebug("Skipping net download.");
                } else {
                    if(downloadNet(net_url,"net.uff")) {
                        try {
                            BufferedWriter writer = new BufferedWriter(
                                new FileWriter("checksum.txt"));
                            String ck = Long.toString(checksum);
                            writer.write(ck + "\n");
                            writer.write(net_url + "\n");
                            writer.close();
                            net_recieved = true;
                            deleteFiles("*.trt",isWindows);
                        } catch (Exception e) {
                            printDebug("Checksum: " + e.getMessage());
                        }
                    }
                }
            } else if(isSame(cmd,"<version>")) {
                cmd = readLn();
                printDebug(cmd);
                Integer version = Integer.parseInt(cmd.trim());
                cmd = readLn();
                Integer min_version = Integer.parseInt(cmd.trim());
                printDebug(cmd);
                cmd = readLn();
                printDebug(cmd);

                if(Manager.version < min_version) {
                    printDebug(
                        "\n****************************************************************\n" +
                         "Please updgrade client to atleast version: " + version +
                        "\n****************************************************************\n"
                        );
                    System.exit(0);
                } else if(Manager.version < version) {
                    printDebug(
                         "\n****************************************************************\n" +
                         "We recommend you updgrade client to version: " + Manager.version +
                         "\n****************************************************************\n");
                }
            }
        }
        
        sc.close();
        
        if(done != State.LOGGED || parameters == null)
            return true;
        
        if(!net_recieved)
            return true;
        
        while(!is_ready()) {
            //resend left-over games
            if(reconnect) {
                sendGames();
                reconnect = false;
            }
            
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
                while ((s = stdInput.readLine()) != null) {
                    if(s.startsWith("[")) {
                        if(hasString(s,"generated"))
                            System.out.println("\n" + s);
                        else
                            System.out.print(s + "\r");
                    } else if(hasString(s,"Calibrating")) {
                        System.out.print(s + "\r");
                    }
                }

                proc.waitFor();
                printDebug("Finished executing job!                     ");
            } catch (Exception e) {
                printDebug("Could not execute job!");
                return false;
            }

            // send games            
            if(!sendGames()) {
                reconnect = true;
                net_recieved = false;
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
                printDebug(cmd);
                int games = Integer.parseInt(cmd.trim());
                try {
                    myManager.dbm.addContrib(userName,myManager.workID,games);
                } catch (Exception e) {
                    printDebug("Database update: " + e.getMessage());
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
