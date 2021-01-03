import javax.swing.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Client class
 * A TCP Client connection manager for joining a chat room
 *
 * @author - Eldar Erel
 * @version - 23.12.20
 */
public class Client {
    private boolean CONNECTED = false; // connection flag
    private String userName; // the user name
    private Socket socket; // connection socket
    private ObjectInputStream in; // input stream
    private PrintWriter out; // output stream

    /**
     * Send message to the server
     *
     * @param message - the message
     */
    public void sendMsg(String message) {
        if (!message.isEmpty() && out != null)
            out.println(message);
    }

    /**
     * Connect to a given host (port 7777)
     *
     * @param host - the host
     * @return - true if the connection was successful, false otherwise
     */
    public boolean connect(String host) {
        try {
            socket = new Socket(host, 7777); // creates connection
            in = new ObjectInputStream(socket.getInputStream());
            out = new PrintWriter(socket.getOutputStream(), true);
            String name = JOptionPane.showInputDialog(null, "Enter your name:");
            if (name == null) { // no name entered
                name = JOptionPane.showInputDialog(null, "Must write name:");
            }
            if (name == null) { // closing app on second time
                closeConnection();
                return false;
            }
            CONNECTED = true;
            out.println(name); // sending name to get approval
            Object nameFeedback = in.readObject();
            while (!nameFeedback.equals("APPROVED")) { // server didnt approve the name
                name = JOptionPane.showInputDialog(null, nameFeedback + " - Name already taken, please write a new name:");
                if (name == null) {
                    closeConnection();
                    return false;
                }
                out.println(name);
                nameFeedback = in.readObject();
            }
            userName = name; // update name
        } catch (IOException e) {
            closeConnection();
            return false;
        } catch (ClassNotFoundException ignored) {
        }
        return true;
    }

    /**
     * Close the connection
     */
    public void closeConnection() {
        // performing safe shutdown
        if (!CONNECTED)
            return;
        CONNECTED = false;
        out.close();
        try {
            in.close();

        } catch (IOException ignored) {
        }
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    /**
     * Input data from the server
     *
     * @return - the data, null if not connected or nothing has received
     */
    public Object in() {
        if (in == null) // not connected
            return null;
        try {
            return in.readObject();
        } catch (IOException ei) {
            closeConnection(); // problem occurred
            return null;
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * returns the user's name
     *
     * @return - the user's name
     */
    public String getName() {
        return userName;
    }

    /**
     * Returns true if connection is active, false otherwise
     *
     * @return - true if connection is active, false otherwise
     */
    public boolean isConnected() {
        return CONNECTED;
    }
}
