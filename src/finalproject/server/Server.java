package finalproject.server;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.*;
import java.util.Date;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import finalproject.client.ClientInterface;
import finalproject.db.DBInterface;
import finalproject.entities.Person;

public class Server extends JFrame implements Runnable {

    public static final int DEFAULT_PORT = 8001;
    private static final int FRAME_WIDTH = 600;
    private static final int FRAME_HEIGHT = 600;
    final int AREA_ROWS = 30;
    final int AREA_COLUMNS = 50;

    JLabel a1;
    JLabel a2;
    JTextArea textArea1;
    Connection conn;

    public Server() throws IOException, SQLException {
        this(DEFAULT_PORT, "server.db");
        this.add(this.createContentPane());
        a2.setText("server.db");
    }

    public Server(String dbFile) throws IOException, SQLException {
        this(DEFAULT_PORT, dbFile);
        this.add(this.createContentPane());
        a2.setText(dbFile);
        connectToDB("server.db");
        Thread t = new Thread(this);
        t.start();
    }

    public Server(int port, String dbFile) throws IOException, SQLException {

        this.setSize(Server.FRAME_WIDTH, Server.FRAME_HEIGHT);
        this.setResizable(false);
        this.add(this.createContentPane());
        a2.setText(dbFile);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    }

    private void connectToDB(String dbFileName) {
        String url = "jdbc:sqlite:" + dbFileName;
        try {
            conn = DriverManager.getConnection(url);

        } catch (SQLException e) {
            System.err.println("Connection error: " + e);
            System.exit(1);
        }
    }

    public JPanel createContentPane() {

        JPanel panel = new JPanel();
        JPanel apanel = new JPanel();
        JPanel cpanel = new JPanel();
        JMenuBar mb = new JMenuBar();
        JMenu m = new JMenu("File");
        JMenuItem eMenuItem = new JMenuItem("Exit");
        eMenuItem.addActionListener((event) -> System.exit(0));
        m.add(eMenuItem);
        mb.add(m);
        this.setJMenuBar(mb);
        a1 = new JLabel("DB: ");
        a2 = new JLabel("");
        apanel.add(a1);
        apanel.add(a2);

        JButton btn = new JButton("Query DB");
        cpanel.add(btn);
        btn.addActionListener(new QueryButtonListener());
        panel.add(apanel);
        panel.add(cpanel);

        textArea1 = new JTextArea(AREA_ROWS, AREA_COLUMNS);
        JScrollPane listScroller = new JScrollPane(textArea1);
        textArea1.setEditable(false);
        textArea1.append("Listening on port " + DEFAULT_PORT + "\n");
//		listScroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
//		listScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        panel.add(listScroller);
        return panel;


    }

    class QueryButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            try {
                textArea1.append("DB Data:\n");
                PreparedStatement stmt = conn.prepareStatement("Select * from People");
                ResultSet rset;
                rset = stmt.executeQuery();
                ResultSetMetaData rsmd = rset.getMetaData();
                int numColumns = rsmd.getColumnCount();
                System.out.println("numcolumns is " + numColumns);
                String columnString = "";
                String rowString = "";

                for (int i = 1; i <= numColumns; i++) {
                    String name = rsmd.getColumnName(i);
                    columnString += name + "\t";
                }
                columnString += "\n";
                textArea1.append(columnString);
                while (rset.next()) {
                    for (int i = 1; i <= numColumns; i++) {
                        Object o = rset.getObject(i);
                        rowString += o.toString() + "\t";
                    }
                    rowString += "\n";
                }
                System.out.print("rowString  is  " + rowString);
                textArea1.append(rowString + "\n");
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {

        Server sv;
        try {
            sv = new Server("server.db");
            sv.setVisible(true);
        } catch (IOException | SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void run() {

        try {
            ServerSocket serverSocket = new ServerSocket(DEFAULT_PORT);

            while (true) {

                Socket socket = serverSocket.accept();
                Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                textArea1.append("Starting thread for client 1 at " + timestamp + "\n");
                InetAddress inetAddress = socket.getInetAddress();
                textArea1.append("Client 1's host name is localhost" + "\n");
                textArea1.append("Client 1's ip address is " + inetAddress.getHostAddress() + "\n");
                textArea1.append("listening for input from client 1\n");
                new Thread(new HandleAClient(socket)).start();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    class HandleAClient implements Runnable {
        private Socket socket;

        public HandleAClient(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                while (true) {
                    ObjectInputStream inServer = new ObjectInputStream(socket.getInputStream());
                    DataOutputStream outputToClient = new DataOutputStream(socket.getOutputStream());
                    Person a = (Person) inServer.readObject();
                    int id = a.getId();
                    String firstName = a.getFirstName();
                    String lastName = a.getLastName();
                    int age = a.getAge();
                    String city = a.getCity();
                    textArea1.append("got person Person [last=" + firstName + ", first=" + lastName + ", age=" + age + ", city=" + city + ", id=" + id + "]inserting into DB \n");
                    PreparedStatement stmt = conn.prepareStatement("INSERT INTO People (first, last, age, city, sent, id) VALUES (?, ?, ?, ?, ?, ?);");
                    stmt.setString(2, firstName);
                    stmt.setString(1, lastName);
                    stmt.setInt(3, age);
                    stmt.setString(4, city);
                    stmt.setInt(5, 1);
                    stmt.setInt(6, id);
                    stmt.executeUpdate();
                    textArea1.append("Inserted Successfully\n");
                    outputToClient.writeUTF("Success\n");
                }
            } catch (IOException | SQLException | ClassNotFoundException ex) {
                ex.printStackTrace();
                textArea1.append("I/O error: null, Ending connection\n");
            }
        }
    }
}

