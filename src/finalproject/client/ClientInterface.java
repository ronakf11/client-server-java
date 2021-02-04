package finalproject.client;

import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.sql.*;
import java.util.Arrays;
import java.util.List;
import javax.swing.*;

import finalproject.client.ClientInterface.ComboBoxItem;
import finalproject.db.DBInterface;
import finalproject.entities.Person;
import finalproject.server.Server;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.Socket;

public class ClientInterface extends JFrame {

    private static final long serialVersionUID = 1L;

    public static final int DEFAULT_PORT = 8001;

    private static final int FRAME_WIDTH = 800;
    private static final int FRAME_HEIGHT = 600;
    final int AREA_ROWS = 10;
    final int AREA_COLUMNS = 40;

    JComboBox peopleSelect;
    JFileChooser jFileChooser;
    Socket socket;
    int port;

    JLabel adb;
    JLabel acn;
    JLabel dbName;
    JLabel cname;
    JTextArea textArea2;
    Connection conn;

    DataOutputStream toServer = null;
    DataInputStream fromServer = null;

    public ClientInterface() {
        this(DEFAULT_PORT);
        JMenuBar mb = new JMenuBar();
        mb.add(createFileMenu());
        this.setJMenuBar(mb);
        this.add(this.createContentPane());
    }

    public ClientInterface(int port) {
        this.port = port;

    }

    public JPanel createContentPane() {

        JPanel panel = new JPanel();
        JPanel dbpanel = new JPanel();
        JPanel cnpanel = new JPanel();
        JPanel connPanel = new JPanel();
        JPanel queryPanel = new JPanel();
        adb = new JLabel("Active DB: ");
        dbName = new JLabel("<None>");
        acn = new JLabel("Active Connection: ");
        cname = new JLabel("<None>");
        dbpanel.add(adb);
        dbpanel.add(dbName);
        cnpanel.add(acn);
        cnpanel.add(cname);

        panel.add(dbpanel);
        panel.add(cnpanel);

        peopleSelect = new JComboBox();
        peopleSelect.setPreferredSize(new Dimension(100, 25));
        panel.add(peopleSelect);

        JButton oConnBtn = new JButton("Open Connection");
        JButton eConnBtn = new JButton("Close Connection");
        connPanel.add(oConnBtn);
        oConnBtn.addActionListener(new OpenConnectionListener());
        connPanel.add(eConnBtn);
        eConnBtn.addActionListener((e) -> {
            try {
                socket.close();
            } catch (Exception e1) {
                System.err.println("error");
            }
        });
        panel.add(connPanel);

        JButton sendDataBtn = new JButton("Send Data");
        JButton queryDataBtn = new JButton("Query DB Data");
        queryDataBtn.addActionListener(new QueryButtonListener());
        sendDataBtn.addActionListener(new SendButtonListener());
        queryPanel.add(sendDataBtn);
        queryPanel.add(queryDataBtn);
        panel.add(queryPanel);

        textArea2 = new JTextArea(28, 50);
        JScrollPane listScroller = new JScrollPane(textArea2);
        textArea2.setEditable(false);
        panel.add(textArea2);

        return panel;


    }

    public JMenu createFileMenu() {
        JMenu menu = new JMenu("File");
        menu.add(createFileOpenItem());
        menu.add(createFileExitItem());
        return menu;
    }

    private JMenuItem createFileExitItem() {
        JMenuItem item2 = new JMenuItem("Exit");
        item2.addActionListener((event) -> System.exit(0));
        return item2;
    }


    private void fillComboBox() throws SQLException {

        List<ComboBoxItem> l = getNames();
        peopleSelect.setModel(new DefaultComboBoxModel(l.toArray()));


    }

    class OpenConnectionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            try {
                socket = new Socket("localhost", DEFAULT_PORT);
                cname.setText("localhost:8001");
            } catch (IOException e1) {
                e1.printStackTrace();
                textArea2.append("connection Failure");
            }
        }

    }

    private JMenuItem createFileOpenItem() {
        JMenuItem item = new JMenuItem("Open DB");
        class OpenDBListener implements ActionListener {
            public void actionPerformed(ActionEvent event) {
                jFileChooser = new JFileChooser();
                int returnVal = jFileChooser.showOpenDialog(getParent());
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    System.out.println("You chose to open this file: " + jFileChooser.getSelectedFile().getAbsolutePath());
                    String dbFileName = jFileChooser.getSelectedFile().getAbsolutePath();
                    try {
                        connectToDB(dbFileName);
                        dbName.setText(dbFileName.substring(dbFileName.lastIndexOf("\\") + 1));
                        clearComboBox();
                        fillComboBox();

                    } catch (Exception e) {
                        System.err.println("error connection to db: " + e.getMessage());
                        e.printStackTrace();
                        dbName.setText("<None>");
                        clearComboBox();
                    }

                }
            }


        }

        item.addActionListener(new OpenDBListener());
        return item;
    }

    private void clearComboBox() {
        peopleSelect.removeAllItems();
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

    class QueryButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            try {
                textArea2.setText("");
                PreparedStatement stmt = conn.prepareStatement("select * from People");
                ResultSet rset;
                rset = stmt.executeQuery();
                ResultSetMetaData rsmd = rset.getMetaData();
                int numColumns = rsmd.getColumnCount();
                System.out.println("numcolumns is " + numColumns);
                String columnString = "";
                String rowString = "";

                for (int i = 1; i <= numColumns; i++) {
                    String name = rsmd.getColumnName(i);
                    // Do stuff with name
                    columnString += name + "\t";
                }
                columnString += "\n";
                textArea2.setText(columnString);
                while (rset.next()) {
                    for (int i = 1; i <= numColumns; i++) {
                        Object o = rset.getObject(i);
                        rowString += o.toString() + "\t";
                    }
                    rowString += "\n";
                }
                textArea2.append(rowString);
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    class SendButtonListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {

            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                ObjectOutputStream toServer = new ObjectOutputStream(socket.getOutputStream());
                ComboBoxItem personEntry = (ComboBoxItem) peopleSelect.getSelectedItem();

                int pid = personEntry.getId();
                PreparedStatement stmt = conn.prepareStatement("select id,first,last,age,city from People where id = ?");
                stmt.setInt(1, pid);
                ResultSet rset;
                rset = stmt.executeQuery();
                int id = rset.getInt("id");
                String firstName = rset.getString("first");
                String lastName = rset.getString("last");
                int age = rset.getInt("age");
                String city = rset.getString("city");
                Person obj = new Person(id, firstName, lastName, age, city);
                toServer.writeObject(obj);

                String response = br.readLine();
                if (response.contains("Success")) {
                    System.out.println("Success");
                    PreparedStatement stmt2 = conn.prepareStatement("UPDATE People set sent=1 where id=?");
                    stmt2.setInt(1, pid);
                    stmt2.executeUpdate();
                    fillComboBox();
                } else {
                    System.out.println("Failed");
                }
            } catch (IOException | SQLException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }

    }

    private List<ComboBoxItem> getNames() throws SQLException {
        List<ComboBoxItem> a = new ArrayList<>();
        PreparedStatement stmt = conn.prepareStatement("select id, first || ' ' || last AS FullName from People where sent=0");
        ResultSet rset;
        rset = stmt.executeQuery();
        while (rset.next()) {
            String name = rset.getString("FullName");
            int id = rset.getInt("id");
            ComboBoxItem obj1 = new ComboBoxItem(id, name);
            a.add(obj1);
        }


        return a;
    }

    class ComboBoxItem {
        private int id;
        private String name;

        public ComboBoxItem(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return this.id;
        }

        public String getName() {
            return this.name;
        }

        public String toString() {
            return this.name;
        }
    }

    public static void main(String[] args) {
        ClientInterface ci = new ClientInterface();
        ci.setSize(600, 600);
        ci.setResizable(false);
        ci.setVisible(true);
    }
}
