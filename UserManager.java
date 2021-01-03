import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * UserManager class
 * Connection handler for a server's user
 * maintaining open stream for sending and receiving information
 */
public class UserManager extends Thread {
    private Socket socket; // server socket
    private Server server; // server
    private String userName; // client's username
    private ObjectOutputStream out; /// out stream
    private BufferedReader in; // in stream

    /**
     * Creates a new user connection manager
     *
     * @param s  - socket connection
     * @param sr - the server
     */
    public UserManager(Socket s, Server sr) { // creating user thread
        try { // creating connection
            socket = s;
            server = sr;
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            System.out.println("Could not open stream");
        }
    }

    /**
     * Handler,
     * deals with the input and output streams
     */
    @Override
    public void run() {
        try {
            boolean addedSucceeded;
            do { //getting input from client (first input must be the username)
                userName = in.readLine();
                addedSucceeded = server.addUser(this, userName); // trying to add username, if username not already exists
                if (addedSucceeded)
                    out.writeObject("APPROVED");
                else
                    out.writeObject("NOT APPROVED");
            }
            while (!addedSucceeded);
            server.announceNewConnection(this, userName); // notifying all clients about the connection
            send("You Joined the room");
            server.sendUsersList(); // send updated users list
            String input;
            while (socket.isConnected() && (input = in.readLine()) != null) { // reading msg from client
                server.sendAll((userName + ": " + input));
            }
            disconnect(); // disconnect
            server.sendUsersList();
            // closing
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) { // if error occurred
            disconnect(); // remove and notify all
            server.sendUsersList();
        }
    }

    /**
     * Sends data to the client
     *
     * @param data - the data
     */
    public void send(Object data) {
        try {
            out.writeObject(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Disconnecting
     */
    private void disconnect() { // notifying all client when disconnecting
        server.removeUser(this, userName);
        server.sendAll(userName + " Has Left The Room.");
    }
}