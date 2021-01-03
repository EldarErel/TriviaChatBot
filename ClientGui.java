import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * ClientGui class
 * Client's graphic interface
 * uses to a connection manager to connect to server
 *
 * @author - Eldar Erel
 * @version - 24.12.20
 */
public class ClientGui implements ActionListener {
    private Client connectionManager; // connection manager
    private JTextArea txtRoom; // room's txt area
    private JTextField txtClient; // client's txt field
    private JButton cmdSend; // send msg button
    private DefaultListModel<String> userNames; // active users list model
    // menu items
    private JMenuItem connect;
    private JMenuItem disconnect;
    private JMenuItem exit;
    private JMenuItem about;

    /**
     * Create the Client's window
     */
    public void createWindow() {
        // initializing variables
        connectionManager = new Client();
        JFrame frame = new JFrame();
        JPanel chatRoom = new JPanel();
        JPanel south = new JPanel();
        userNames = new DefaultListModel<>();
        JList<String> usersList = new JList<>(userNames);
        JMenu menuFile = new JMenu("File");
        JMenu menuHelp = new JMenu("Help");
        JMenuBar mb = new JMenuBar();
        connect = new JMenuItem("Connect");
        disconnect = new JMenuItem("Disconnect");
        exit = new JMenuItem("Exit");
        about = new JMenuItem("About");
        disconnect.setEnabled(false);
        txtRoom = new JTextArea("");
        txtClient = new JTextField("");
        cmdSend = new JButton("Send");
        JScrollPane sp = new JScrollPane(txtRoom);
        DefaultCaret caret = (DefaultCaret) txtRoom.getCaret(); // auto scroll bottom
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        // creating menu
        menuFile.add(connect);
        menuFile.add(disconnect);
        menuFile.add(exit);
        mb.add(menuFile);
        menuHelp.add(about);
        mb.add(menuHelp);
        //creating main view
        frame.setJMenuBar(mb);
        chatRoom.setLayout(new BorderLayout());
        usersList.setPreferredSize(new Dimension(100, chatRoom.getHeight()));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setTitle("Chat Room");
        frame.setSize(800, 600);
        usersList.setBorder(new LineBorder(Color.BLACK, 1));
        chatRoom.add(sp, BorderLayout.CENTER);
        chatRoom.add(usersList, BorderLayout.EAST);
        south.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        south.add(txtClient, gbc);
        gbc.weightx = 0;
        south.add(cmdSend, gbc);
        chatRoom.add(south, BorderLayout.SOUTH);
        txtRoom.setBorder(new LineBorder(Color.BLACK, 1));
        txtRoom.setEditable(false);
        frame.add(chatRoom, BorderLayout.CENTER);
        frame.add(usersList, BorderLayout.EAST);
        cmdSend.addActionListener(this);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        // adding listeners
        disconnect.addActionListener(this);
        exit.addActionListener(this);
        about.addActionListener(this);
        connect.addActionListener(this);
        connect.addPropertyChangeListener("enabled", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) { // enable/disable view for disconnect menu button
                disconnect.setEnabled(!connect.isEnabled());
            }
        });
        frame.addWindowListener(new WindowListener() {
            @Override
            public void windowOpened(WindowEvent e) {
            }

            @Override
            public void windowClosing(WindowEvent e) {
                connectionManager.closeConnection();
                System.exit(0);

            }

            @Override
            public void windowClosed(WindowEvent e) {
            }

            @Override
            public void windowIconified(WindowEvent e) {
            }

            @Override
            public void windowDeiconified(WindowEvent e) {
            }

            @Override
            public void windowActivated(WindowEvent e) {
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
            }
        }); // action on close
        txtClient.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendMsg();
                }
            }
        });
    }

    /**
     * Sends a message
     */
    private void sendMsg() {
        if (txtClient.getText().isEmpty()) // nothing to send
            return;
        connectionManager.sendMsg(txtClient.getText());
        txtClient.setText("");
        txtClient.requestFocusInWindow();
    }

    @Override
    public void actionPerformed(ActionEvent e) { // buttons actions
        if (e.getSource() == connect) { /// connect button pressed
            String host = JOptionPane.showInputDialog(null, "Please Enter Server Address", "Connect", JOptionPane.INFORMATION_MESSAGE);
            if (host == null)
                return;
            showToClient("Trying to connect to " + host);
            if (!connectionManager.connect(host)) { // connect to host
                showToClient("Couldn't connect to " + host);
                connect.setEnabled(true);
                return;
            }
            showToClient("Connected to " + host);
            listen(); // listens to messages
            connect.setEnabled(false);
            txtClient.requestFocusInWindow();
        } else if (e.getSource() == cmdSend) { // send msg
            sendMsg();
        } else if (e.getSource() == disconnect || e.getSource() == exit) { // disconnect or exit button
            connectionManager.closeConnection();
            connect.setEnabled(true);
            showToClient("Connection Closed");
            if (e.getSource() == exit) // exit menu item pressed
                System.exit(0);
        } else if (e.getSource() == about) { // about pressed
            JFrame frame = new JFrame("About");
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
            JLabel lblAbout = new JLabel("(c) All right reserved to Eldar Erel (c)");
            panel.add(lblAbout);
            frame.add(panel);
            frame.setResizable(false);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setSize(250, 80);
            frame.setVisible(true);
            frame.setLocationRelativeTo(null);
        }
    }

    /**
     * Updates the active users list view
     *
     * @param input - the new users list
     */
    private void updateUsersList(Object input) { // updating the view of the active users in the chat
        if (!(input instanceof Object[]))
            return;
        Object[] in = (Object[]) input;
        userNames.clear();
        for (Object o : in) {
            if (o instanceof String)
                userNames.addElement((String) o);
        }
    }

    /**
     * Show txt in chat room
     *
     * @param txt - the text to show
     */
    private void showToClient(String txt) {
        txtRoom.append(txt + "\n");
        txtRoom.revalidate();
    }

    /**
     * Performing safe close when losing conection
     */
    private void connectionLost() {
        showToClient("Connection Lost");
        connectionManager.closeConnection();
        userNames.clear();
        connect.setEnabled(true);
    }

    /**
     * A data handler
     * listens to incoming inputs from the server
     */
    private void listen() { // dealing with income messages from server
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (connectionManager.isConnected()) {
                    Object input = connectionManager.in();
                    if (input instanceof String) { // if its a chat msg show it in the app screen
                        SimpleDateFormat fo = new SimpleDateFormat("[hh:mm:ss] ");
                        String date = fo.format(new Date());
                        showToClient(date + input);
                    } else if (input instanceof Object[]) { // if its the usernames list update it
                        updateUsersList(input);
                    }
                }
                if (!connect.isEnabled()) // connect button is disabled = lost connection
                    connectionLost();
            }
        }).start();

    }

    public static void main(String[] args) {
        ClientGui client = new ClientGui();
        client.createWindow();
    }
}
