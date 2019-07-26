import java.io.File;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

/** Base manager class
 */
public class Manager {
    public static ServerSocket server;
    public static int server_port;
    public static boolean isServer;
    public static List<Manager> allManagers;
    public static List<Engine> InstalledEngines;
    public static List<Engine> ObserverEngines;
    public List<Engine> WorkObservers;
    public int workID;
    public static String network_uff;
    public static String network_pb;
    public static String[] parameters;

    private boolean isVerbose = false;
    
    static {
        allManagers = new LinkedList<Manager>();
        InstalledEngines = new ArrayList<Engine>();
        ObserverEngines = new ArrayList<Engine>();
        server = null;
        server_port = 6000;
        isServer = false;
        InstallEngines();
    }
    public static String getWorkName(int ID) {
        return "training network";
    }
    public Manager() {      
        workID = 1;
        WorkObservers = new LinkedList<Engine>();
        allManagers.add(this);
    }
    public static Engine getNewEngine(String dir,String cmd) {
        Engine eng;
        eng = new TcpClientEngine(cmd);
        return eng;
    }
    public static void Install(String str) {
        Scanner sc = new Scanner(str);
        sc.useDelimiter("[=\n\r\"]");
        String cmd,dir;
        if(sc.hasNext()) {
            cmd = sc.next();
            dir = sc.next();
            Engine eng = getNewEngine(dir,cmd);
            InstalledEngines.add(eng);
        }
        sc.close();
    }
    static void InstallEngines() {
        Install("danidesti.ddns.net " + Integer.toString(server_port) + " user=tcp");
    }
    public void printDebug(String str,int id) {
        if(isVerbose)
            System.out.println("" + id + ": " + str);
    }
    public void printInfo(String str) {
        System.out.println(str);
    }
    public void onStart() {
    }
    public static void startServer() {
        if(isServer)
            return;
        isServer = true;
        
        final Manager m1 = allManagers.get(0);
        try {
            server = new ServerSocket(server_port);
        } catch (Exception e) {
            m1.printDebug(e.getMessage(),0);
        }
        
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {   
                    while(isServer) {
                        Socket skt = server.accept();
                        m1.printDebug(skt.toString(),0);       
                        String cmd = skt.getInetAddress().toString() + " " + skt.getPort();
                        Engine eng = new TcpServerEngine(cmd,skt);
                        eng.myManager = m1;
                        ObserverEngines.add(eng);
                        ObserverEngines.get(ObserverEngines.size() - 1).start();
                        while(!eng.isDone())
                            ;
                        m1.addLastObserver();
                        Observe(eng,m1.workID);
                    }
                } catch (Exception e) {
                    m1.printDebug(e.getMessage(),0);
                }
            }
        };
        new Thread(r).start();
        
        m1.printDebug("Started server : " + server,0);
    }
    public static void stopServer() {
        isServer = false;
        
        final Manager m1 = allManagers.get(0);
        try {
            m1.printDebug("Stopping server : " + server,0);
            server.close();
        } catch (Exception e) {
            m1.printDebug(e.getMessage(),0);
        }
    }
    static String getWho() {
        String str = "<who>\n";
        for(Engine e: ObserverEngines) {
            int index = e.name.indexOf('@');
            str += e.name.substring(0,index);
            str += "\n";
        }
        str += "</who>";
        return str;
    }
    static String getAllObservers() {
        String str = "<workers>\n\r";
        for(Manager m: allManagers) {
            str += "<Work> " + m.workID + "\n";
            for(Engine e: m.WorkObservers) {
                int index = e.name.indexOf('@');
                str += e.name.substring(0,index);
                str += "\n";
            }
            str += "</Work>\n";
        }
        str += "</workers>";
        return str;
    }
    public void addLastObserver() {
    }
    public void removeObserver(Engine e) {
    }
    static void SendNetwork(Engine e) {
        String message = "";
        try {
            byte[] content;
            
            //uff
            File net_uff = new File(network_uff);
            if(net_uff.exists())
                content = Files.readAllBytes(Paths.get(network_uff));
            else
                content = new byte[1];
            
            message = "<network-uff>\n";
            message += content.length;
            e.send(message);
            
            e.sendFile(content);
            
            message = "</network-uff>";
            e.send(message);
            
            //pb
            content = Files.readAllBytes(Paths.get(network_pb));
            
            message = "<network-pb>\n";
            message += content.length;
            e.send(message);
            
            e.sendFile(content);
            
            message = "</network-pb>";
            e.send(message);
        } catch (Exception ex) {
            System.out.println("Error sending networks: " + ex.getMessage());
        }
    }
    static void SendParameters(Engine e) {
        String message = "";
        try {
            message = "<parameters> ";
            for(int i = 0; i < parameters.length; i++)
                message += parameters[i] + " ";
            message += "\n</parameters>";
            e.send(message);
        } catch (Exception ex) {
            System.out.println("Error sending parameters: " + ex.getMessage());
        }
    }
    static void SendNetworkAll(int ID) {
        for(Manager m: allManagers) {
            if(m.workID == ID) {
                for(Engine e: m.WorkObservers) {
                    SendParameters(e);
                    SendNetwork(e);
                }
            }
        }
    }
    static void Observe(Engine e,int ID) {
        for(Manager m: allManagers) {
            if(m.workID == ID) {
                m.WorkObservers.add(e);
                m.printDebug("Sending network",0);
                SendParameters(e);
                SendNetwork(e);
            }
        }
    }
    static void HandleDisconnects(Engine e) {
        if(e.hasFailed()) {
            for(Manager m: allManagers) {
                m.removeObserver(e);
                m.WorkObservers.remove(e);
            }
            ObserverEngines.remove(e);
        }
    }
    public void LoadEngine(Engine e2) {
        Engine e = getNewEngine(e2.path,e2.cmdLine);
        e.myManager = this;
        e.id = 0;
        e.start();
        while(!e.isDone())
            ;
    }
    public void ProcessCommands(String[] args) {
        String cmd;
        int count = 0;
        while(count < args.length) {
            cmd = args[count++];
            if(Engine.isSame(cmd,"-startServer")) {
                startServer();
            } else if(Engine.isSame(cmd,"-stopServer")) {
                stopServer();
            } else if(Engine.isSame(cmd,"-startClient")) {
                LoadEngine(InstalledEngines.get(0));
            } else if(Engine.isSame(cmd,"-port")) {
                server_port = Integer.parseInt(args[count++]);
            } else if(Engine.isSame(cmd,"-debug")) {
                isVerbose = !isVerbose;
                if(isVerbose) printDebug("debugging on",0);
                else printDebug("debugging off",0);
            } else if(Engine.isSame(cmd,"-who")) {
                printInfo(Manager.getWho());
            } else if(Engine.isSame(cmd,"-workers")) {
                printInfo(Manager.getAllObservers());
            } else if(Engine.isSame(cmd,"-network-uff")) {
                network_uff = args[count++];
            } else if(Engine.isSame(cmd,"-network-pb")) {
                network_pb = args[count++];
            } else if(Engine.isSame(cmd,"-parameters")) {
                parameters = new String[4];
                for(int i = 0; i < 4; i++)
                    parameters[i] = args[count++];
            } else if(Engine.isSame(cmd,"-update-network")) {
                SendNetworkAll(workID);
            } else if(Engine.isSame(cmd,"-help")) {
                String msg = "-startServer | start server\n" +
                             "-stopServer | stop server\n" +
                             "-port | set port for the server\n" +
                             "-help | display this usage message." +
                             "\n\tUse '-<command>' when invoking from command line!" +
                             "\n\tUse '<command>' without hyphen once application started.\n";
                printDebug(msg,0);
            }
        }
    }   
}
