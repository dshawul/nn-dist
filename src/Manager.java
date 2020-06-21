import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Date;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.sql.ResultSet;
import java.util.zip.Adler32;
import java.util.zip.Checksum;

/** Database manager
 */
class DatabaseManager {
    private final String url = "jdbc:postgresql://localhost/scorpiozero";
    private final String user = "postgres";
    private final String password = "postgres";
    private Connection conn = null;

    public void connect() {
        try {
            conn = DriverManager.getConnection(url,user,password);
            if (conn != null) {
                System.out.println("Connected to the database!");
            } else {
                System.out.println("Failed to make connection!");
            }
            createTables();
        } catch (SQLException e) {
            System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void createTables() throws SQLException {
        String sqlUsers = "CREATE TABLE IF NOT EXISTS users"
                + "("
                + "   user_id            SERIAL PRIMARY KEY,"
                + "   username           VARCHAR(50) UNIQUE NOT NULL,"
                + "   password           VARCHAR(50) NOT NULL,"
                + "   created_on         TIMESTAMP NOT NULL,"
                + "   last_login         TIMESTAMP"
                + ")";
        String sqlContrib = "CREATE TABLE IF NOT EXISTS contrib"
                + "("
                + "   user_id            INTEGER NOT NULL,"
                + "   work_id            INTEGER NOT NULL,"
                + "   games              INTEGER NOT NULL"
                + ")";
        String sqlWork = "CREATE TABLE IF NOT EXISTS work"
                + "("
                + "   work_id            INTEGER UNIQUE NOT NULL,"
                + "   parameters         VARCHAR(512)"
                + ")";
        Statement stmt = conn.createStatement();
        stmt.execute(sqlUsers);
        stmt.execute(sqlContrib);
        stmt.execute(sqlWork);
        stmt.close();
    }
    public boolean checkUser(String user, String pass) throws SQLException {
        String sqlStr;
        ResultSet rs;
        Statement stmt = conn.createStatement();

        sqlStr = "SELECT * FROM users WHERE username = '" + user + "'";
        rs = stmt.executeQuery(sqlStr);
        if(rs.next()) {
            String password = rs.getString("password");
            stmt.close();
            if(!password.equals(pass))
                return false;
            sqlStr = "UPDATE users" +
                     " SET last_login=?" +
                     " WHERE username = '" + user + "'";
            PreparedStatement pstmt = conn.prepareStatement(sqlStr);
            pstmt.setTimestamp(1,getCurrentTimeStamp());
            pstmt.executeUpdate();
            pstmt.close();
            return true;
        }
        stmt.close();

        sqlStr = "INSERT INTO users"
                + "(username,password,created_on,last_login) VALUES"
                + "('" + user + "','" + pass + "',?,?)";
        PreparedStatement pstmt = conn.prepareStatement(sqlStr);
        pstmt.setTimestamp(1,getCurrentTimeStamp());
        pstmt.setTimestamp(2,getCurrentTimeStamp());
        pstmt.executeUpdate();
        pstmt.close();
        return true;
    }
    public void checkWork(int workID, String[] parameters) throws SQLException {
        String sqlStr;
        ResultSet rs;
        Statement stmt = conn.createStatement();

        sqlStr = "SELECT * FROM work WHERE work_id = " + workID;
        rs = stmt.executeQuery(sqlStr);
        if(!rs.next()) {
            String params = "";
            for(int i = 0; i < parameters.length; i++)
                params += parameters[i] + " ";
            sqlStr = "INSERT INTO work"
                    + "(work_id,parameters) VALUES"
                    + "(" + workID + ",'" + params.trim() + "')";
            stmt.executeUpdate(sqlStr);
        }
        stmt.close();
    }
    public void addContrib(String username, int workID, int games) throws SQLException {
        String sqlStr;
        ResultSet rs;
        Statement stmt = conn.createStatement();

        sqlStr = "SELECT * FROM users WHERE username = '" + username + "'";
        rs = stmt.executeQuery(sqlStr);
        rs.next();
        int user_id = rs.getInt("user_id");

        sqlStr = "SELECT * FROM contrib WHERE user_id = " + user_id + " AND work_id = " + workID;
        rs = stmt.executeQuery(sqlStr);

        if(!rs.next()) {
            sqlStr = "INSERT INTO contrib"
                    + "(user_id,work_id,games) VALUES"
                    + "(" + user_id + "," + workID + "," + games + ")";
        } else {
            sqlStr = "UPDATE contrib SET games = games + " + games
                    + " WHERE user_id = " + user_id + " AND work_id = " + workID;
        }
        stmt.executeUpdate(sqlStr);
        stmt.close();
    }

    private static Timestamp getCurrentTimeStamp() {
        Date today = new Date();
        return new Timestamp(today.getTime());
    }
}

/** Base manager class
 */
public class Manager {
    public static DatabaseManager dbm;
    public static final String server_address = "scorpiozero.ddns.net";
    public static final int version = 5;
    public static final int min_version = 5;
    public static ServerSocket server;
    public static int server_port;
    public static boolean isServer;
    public static String network_uff;
    public static String network_uff_url;
    public static long net_checksum = 0;
    public static String[] parameters;
    
    public static List<Manager> allManagers;
    public static List<Engine> InstalledEngines;
    public static List<Engine> ObserverEngines;
    public List<Engine> WorkObservers;
    public static int workID;

    private boolean isVerbose = false;
    
    static {
        allManagers = new LinkedList<Manager>();
        InstalledEngines = new ArrayList<Engine>();
        ObserverEngines = new ArrayList<Engine>();
        server = null;
        server_port = 48555;
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
        try {
            File settings = new File("settings.ini");
            if(settings.exists()) {
                BufferedReader reader = new BufferedReader( 
                    new FileReader("settings.ini"));
                String engine = reader.readLine();
                Install(engine);
                reader.close();
            } else {
                Install(server_address + " " + server_port + "=tcp");
            }
        } catch(Exception e) {
            System.out.println("InstallEngines: " + e.getMessage());
        }
    }
    static void WriteEngines(String user, String pass) {
        try {
            File settings = new File("settings.ini");
            if(!settings.exists()) {
                BufferedWriter writer = new BufferedWriter(
                    new FileWriter("settings.ini"));
                String str = server_address + " " + server_port + 
                    " " + user + " " + pass + "=tcp";
                writer.write(str);
                writer.close();
            }
        } catch(Exception e) {
            System.out.println("WriteEngines: " + e.getMessage());
        }
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
    public static long computeChecksum(String uff) {
        try {
            File net_uff = new File(uff);
            byte[] content = Files.readAllBytes(Paths.get(uff));
            Checksum checksum = new Adler32();
            checksum.update(content, 0, content.length);
            return checksum.getValue();
        } catch (Exception e) {
            System.out.println("computeCheckusm: " + e.getMessage());
            return 0;
        }
    }
    public static void startServer() {
        if(isServer)
            return;
        isServer = true;
        dbm = new DatabaseManager();
        dbm.connect();

        final Manager m1 = allManagers.get(0);
        try {
            server = new ServerSocket(server_port);
        } catch (Exception e) {
            m1.printDebug("startServer: " + e.getMessage(),0);
        }
        
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {   
                    while(isServer) {
                        Socket skt = server.accept();
                        m1.printDebug(skt.toString(),0);       
                        String cmd = skt.getInetAddress().getHostAddress() + " " + skt.getPort();
                        Engine eng = new TcpServerEngine(cmd,skt);
                        eng.myManager = m1;
                        ObserverEngines.add(eng);
                        ObserverEngines.get(ObserverEngines.size() - 1).start();
                    }
                } catch (Exception e) {
                    m1.printDebug("startServer: " + e.getMessage(),0);
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
            m1.printDebug("stopServer: " + e.getMessage(),0);
        }
    }
    static String getWho() {
        String str = "\n============== " + ObserverEngines.size() +
             " clients connected  ===============\n";
        int i = 1;
        for(Engine e: ObserverEngines) {
            str += i + ". " + e.name + "\n";
            i++;
        }
        str += "===================================================\n";
        return str;
    }
    static void killObserver(int engine_id) {
        int i = 1;
        for(Engine e: ObserverEngines) {
            if(engine_id == 0 || i == engine_id) {
                try {
                    e.myManager.printDebug("Sending kill signal to engine " + engine_id, 0);
                    e.send("kill");
                } catch (Exception ex) {
                    System.out.println("Error sending kill signal!");
                }
                if(engine_id != 0)
                    break;
            }
            i++;
        }
    }
    static String getAllObservers() {
        String str = "\n=============== " + allManagers.size() + " active trainings ================\n";
        for(Manager m: allManagers) {
            str += "======= " + m.WorkObservers.size() + 
               " clients working on training run " + m.workID + " =======\n";
            int i = 1;
            for(Engine e: m.WorkObservers) {
                str += i + ". " + e.name + "\n";
                i++;
            }
            str += "===================================================\n";
        }
        str += "===================================================\n";
        return str;
    }
    public void addLastObserver() {
    }
    public void removeObserver(Engine e) {
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
    static void SendChecksum(Engine e) {
        String message = "";
        try {
            message = "<checksum>\n";
            message += net_checksum + "\n";
            message += network_uff_url + "\n";
            message += "</checksum>";
            e.send(message);
        } catch (Exception ex) {
            System.out.println("Error sending checksum: " + ex.getMessage());
        }
    }
    static void SendVersion(Engine e) {
        String message = "";
        try {
            message = "<version>\n";
            message += version + "\n";
            message += min_version + "\n";
            message += "</version>";
            e.send(message);
        } catch (Exception ex) {
            System.out.println("Error sending version: " + ex.getMessage());
        }
    }
    static void SendNetworkAll(int ID) {
        net_checksum = computeChecksum(network_uff);
        for(Manager m: allManagers) {
            if(m.workID == ID) {
                for(Engine e: m.WorkObservers) {
                    SendChecksum(e);
                }
            }
        }
    }
    static void Observe(Engine e,int ID) {
        for(Manager m: allManagers) {
            if(m.workID == ID) {
                m.WorkObservers.add(e);
                SendVersion(e);
                SendParameters(e);
                SendChecksum(e);
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
            e.kill();
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
            } else if(Engine.isSame(cmd,"-kill")) {
                int id = Integer.parseInt(args[count++]);
                Manager.killObserver(id);
            } else if(Engine.isSame(cmd,"-killall")) {
                Manager.killObserver(0);
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
                network_uff_url = args[count++];
            } else if(Engine.isSame(cmd,"-parameters")) {
                workID = Integer.parseInt(args[count++].trim());
                parameters = new String[args.length - 2];
                for(int i = 0; i < parameters.length; i++)
                    parameters[i] = args[count++];

                try {
                    dbm.checkWork(workID,parameters);
                } catch (SQLException e) {
                    printDebug("Database update parametters: " + e.getMessage(),0);
                }
            } else if(Engine.isSame(cmd,"-update-network")) {
                SendNetworkAll(workID);
            } else if(Engine.isSame(cmd,"-help")) {
                String msg = "-startServer | start server\n" +
                             "-stopServer | stop server\n" +
                             "-startClient | start client\n" +
                             "-port | set port for the server\n" +
                             "-kill | kill client with id\n" +
                             "-killall | kill all clients\n" +
                             "-debug | turn on debugging\n" +
                             "-who | list connected clients\n" +
                             "-workers | list connected clients working on each net\n" +
                             "-parameters | network training parameters\n" +
                             "-network-uff | location of the uff net followed by its http address\n" +
                             "-update-network | update all clients with a new network\n" +
                             "-help | display this usage message." +
                             "\n\tUse '-<command>' when invoking from command line!" +
                             "\n\tUse '<command>' without hyphen once application started.\n";
                printInfo(msg);
            }
        }
    }   
}
