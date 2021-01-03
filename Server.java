import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Server class
 * A chat room server side
 * uses TCP server, listens on port 7777
 *
 * @author - Eldar Erel
 * @version - 21.12.20
 */
public class Server {
    private final int MAX_USERS = 10; // the maximum users allowed to connect
    private final Set<String> activeUsers = new TreeSet<>(); // current active users name
    private final Set<UserManager> usersThreads = new HashSet<>(); // current active users threads

    /**
     * Starts the server
     * Listens on port 7777
     */
    public void run() {
        try {
            final ServerSocket srv = new ServerSocket(7777);
            console("Server is running.");
            console("Type - stop to exit.");
            console("Max connection allowed: " + MAX_USERS);
            new Thread(new Runnable() {
                @Override
                public void run() { // exiting when typing "exit"
                    Scanner scan = new Scanner(System.in);
                    while (!scan.next().equals("stop")) ;
                    try {
                        srv.close();
                        System.exit(0);
                    } catch (IOException ignored) {
                    }
                }
            }).start();
            while (true) {
                srv.setSoTimeout(10000000);
                Socket socket = srv.accept();
                console("New Connection - Total connection: " + (activeUsers.size() + 1));
                if (activeUsers.size() == MAX_USERS) // no more connections allowed
                {
                    console("Users OverLoad, Rejecting!");
                    socket.close();
                    console("Total connection: " + activeUsers.size());
                    continue;
                }
                UserManager user = new UserManager(socket, this);
                user.start();
            }
        } catch (BindException e) {
            console("Server already Running.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds a new user
     *
     * @param user     - user connection manager
     * @param userName - user name
     * @return - true if added successfully , false otherwise
     */
    public boolean addUser(UserManager user, String userName) {
        if (user == null || userName == null)
            return false;
        if (!activeUsers.add(userName))
            return false;
        usersThreads.add(user);
        return true;
    }

    /**
     * Sends data to all the active users
     *
     * @param data - the data to send
     */
    public void sendAll(Object data) {
        for (UserManager u : usersThreads) {
            u.send(data);
        }
    }

    /**
     * Removes a user
     *
     * @param userManager - user connection manager
     * @param userName    - user name
     */
    public void removeUser(UserManager userManager, String userName) {
        if (userName != null)
            activeUsers.remove(userName);
        if (userManager != null)
            usersThreads.remove(userManager);
        console("Users disconnected, Total Connection: " + activeUsers.size());
    }

    /**
     * Announce to all active user when a new user joined the room
     *
     * @param user - new user connection manager
     * @param name - new user name
     */
    public void announceNewConnection(UserManager user, String name) {
        for (UserManager u : usersThreads) {
            if (u != user)
                u.send(name + " Has Joined The Room.");
        }
    }

    /**
     * Sends a list of the active users
     */
    public void sendUsersList() {
        sendAll(activeUsers.toArray());
    }

    /**
     * prompt messages to server's console
     *
     * @param message - the message
     */
    public void console(String message) {
        SimpleDateFormat fo = new SimpleDateFormat("[hh:mm:ss] - ");
        Date date = new Date();
        System.out.println(fo.format(date) + message);

    }

    public static void main(String[] args) {
        Server srv = new Server();
        srv.run();
    }
}
