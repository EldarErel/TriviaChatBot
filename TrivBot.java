import javax.swing.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * A TriviaBOT, based on TCP connection
 * uses a connection manager
 * joins the chat room and creates a trivia game
 */
public class TrivBot extends Thread {
    private final int NUM_TO_WIN = 3; // how many scores to win
    private final int TIME_BETWEEN_QUESTIONS = 30; // time to answer a question
    private final Client connectionManager; // Client connection manager
    private int questionNumber = 0; // question counter
    private boolean LOCKED; // flag for running threads
    private boolean GAME_OVER; // game over flag
    private boolean IN_GAME; // game is running flag
    private boolean GOT_ANSWER; // if someone answered right
    private String question; // to store the question
    private String answer; // to store the answer
    private HashMap<String, Integer> scores; // players score
    private ArrayList<String> questions; // list of all questions
    private ArrayList<String> answers; // list of all answers

    /**
     * Creates a Trivia Bot
     * Loads questions and answers from files
     */
    public TrivBot() {
        scores = new HashMap<>();
        connectionManager = new Client();
        try { // adding questions and answer from files
            questions = addFromFile(); // add questions file
            answers = addFromFile(); // add answers file
        } catch (FileNotFoundException e) {
            console("Could not load the questions/answers");
        }
    }

    /**
     * Connect to a chat room host
     *
     * @param host - host to connect
     * @return - true if the connection was successful, false otherwise
     */
    public boolean connect(String host) {
        if (connectionManager.connect(host)) {
            msgManager(); // listening to incoming messages
            return true; // connected

        }
        console("Could not connect to server");
        return false;
    }

    /**
     * Prompt text to the client's bot
     *
     * @param txt - the txt
     */
    public void console(String txt) {
        SimpleDateFormat fo = new SimpleDateFormat("[hh:mm:ss] ");
        System.out.println(fo.format(new Date()) + txt);
    }

    /**
     * Locks the threads
     */
    private void lock() {
        LOCKED = true;

    }

    /**
     * Unlocks the threads
     */
    public synchronized void unlock() {
        LOCKED = false;
        notifyAll();
    }

    /**
     * Manages the income messages
     * Analyzes player's answers and maintaining the game's logic
     */
    private void msgManager() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (connectionManager.isConnected()) { // running while connected
                    String[] txt = getAnswer(); // storing incoming input
                    if (GAME_OVER) // game has ended, just prompting the incoming messages
                        continue;
                    // analyzing the input, first cell is name, second is answer
                    if (txt != null && txt[1] != null && txt[1].equals(answer) && !txt[0].equals(connectionManager.getName())) {
                        out(txt[0] + " - You are right! The right answer was: " + answer); // right answer
                        score(txt[0]); // adding score
                        if (win(txt[0])) { // player won
                            out(txt[0] + " Has WON!!!");
                            GAME_OVER = true;
                            IN_GAME = false;
                        }
                        gotAnswer(); // got the right answer
                    }
                }
                console("Disconnected from host");
            }
        }).start();
    }
    /**
     * Sends message to server
     *
     * @param msg - the message
     */
    private void out(String msg) {
        connectionManager.sendMsg(msg);
    }
    /**
     * Starts a new trivia game
     */
    public void startGame() {
        if (IN_GAME) { // if game already running
            console("-Error - Game already going.");
            return;
        }
        if (questions.isEmpty() || answers.isEmpty()) { // no questions or answers file
            console("-Error - Couldn't load questions or answers.");
            return;
        }
        GAME_OVER = false;
        IN_GAME = true;
        scores.clear();
        questionNumber = 0;
        out("Game has been started!");
        out("You have " + TIME_BETWEEN_QUESTIONS + " seconds to answer every question.");
        out("First player who gets " + NUM_TO_WIN + " points wins!");
        out("GoodLuck!!");
        out("Starting in 5 Seconds.");
        new Thread(this).start(); // Starts the game
    }
    /**
     * Stop a running game
     */
    public void stopGame() {
        if (!GAME_OVER) {
            GAME_OVER = true;
            IN_GAME = false;
            out("Game has been stopped.");
        }
    }
    /**
     * Checking win condition for a given player
     *
     * @param player - the player
     * @return - true if the player has won, false otherwise
     */
    private boolean win(String player) {
        return scores.containsKey(player) && scores.get(player) == NUM_TO_WIN;
    }

    /**
     * Updates player's score
     *
     * @param name - player's name
     */
    private void score(String name) {
        if (scores.containsKey(name)) // if player exists, increasing score
            scores.put(name, scores.get(name) + 1);
        else
            scores.put(name, 1); // not exists, adding it
    }
    /**
     * Generates a random index represents a random question in the questions list
     *
     * @return -  a random index , -1 if question list is empty
     */
    private int getRandomIndex() {
        if (questions.isEmpty())
            return -1;
        Random r = new Random();
        return r.nextInt(questions.size());
    }

    /**
     * Send a question to the server
     * NOTE: waits TIME_BETWEEN_QUESTIONS seconds before new question can be send
     */
    private synchronized void sendQuestion() {
        if (LOCKED) { // question already sent,
            try {  //  waiting before sending a new question
                wait((TIME_BETWEEN_QUESTIONS - 5) * 1000);
            } catch (InterruptedException ignored) {
            }
        }
        if (GAME_OVER) // if game over dont send the question
            return;
        if (!GOT_ANSWER && answer != null) { // if didnt get the right answer counting 5 sec and announcing time is up
            countDown();
            out("Times Up. The right answer was: " + answer);
        }
        lock();
        GOT_ANSWER = false;
        int index = getRandomIndex(); // random question index
        question = questions.get(index);
        answer = answers.get(index);
        out("#################################################");
        out("Get Ready for question number (" + ++questionNumber + ")...");
        out("#################################################");
        try {
            wait(5 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        out("The Question is: " + question); // sending question
    }

    /**
     * Sends questions while in game
     */
    @Override
    public void run() {
        try {
            Thread.sleep(5 * 1000);
        } catch (InterruptedException ignored) {
        }
        while (!GAME_OVER) {
            sendQuestion();
        }
    }


    /**
     * Gets the name and the answer from the incoming server's input
     * returns a size 2 string array, first cell contains the player's name,
     * second cell contains the sting represents the answer
     *
     * @return - String array with the name of the player and his answer, null if there was nothing to input
     */
    private String[] getAnswer() {
        Object txt = connectionManager.in(); //reading input
        if (!(txt instanceof String)) // if not string return null
            return null;
        console((String) txt); // prompt income message
        String input = (String) txt;
        for (int i = 0; i < input.length(); i++) {
            if (input.charAt(i) == ':') {
                return new String[]{input.substring(0, i), input.substring(i + 2)}; // first cell is the player's name, second is the answer
            }
        }
        return null;
    }

    /**
     * Mark when receives the right answer
     */
    private void gotAnswer() {
        GOT_ANSWER = true;
        unlock();
    }

    /**
     * Counting down 5 seconds
     */
    private void countDown() {
        lock();
        int i = 5;
        out("Hurry Up!!");
        while (i > 0) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            out(((Integer) i--).toString() + "...");
        }
        unlock();
    }

    /**
     * Creates a list of data from a file
     * uses to load questions/answers
     *
     * @return - String ArrayList represents the data loaded from the file
     * @throws FileNotFoundException - couldn't find file
     */
    private ArrayList<String> addFromFile() throws FileNotFoundException {
        JFileChooser fc = new JFileChooser();
        int s = fc.showOpenDialog(null);
        if (s != JFileChooser.APPROVE_OPTION) // no file selected selected
            return null;
        File file = fc.getSelectedFile();
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        try { // loading data to the list
            ArrayList<String> arr = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                arr.add(line);
            }
            return arr; // returns the data
        } catch (IOException ignored) {
        }
        return null; // file is empty
    }
    public static void main(String[] args) {
        TrivBot g = new TrivBot();
        Scanner in = new Scanner(System.in);
        String host = JOptionPane.showInputDialog("Please provide host to connect:");
        if (host == null)
            return;
        if (!g.connect(host))
            return;
        System.out.println("Connected.");
        System.out.println("Welcome to TriviaBot!\nCommands are:\nstart - Starts the trivia game\nstop - Stop current game\nexit - Exit\nEnjoy!");
        label:
        while (true) {
            String s = in.next();
            switch (s) {
                case "start":
                    g.startGame();
                    break;
                case "stop":
                    g.stopGame();
                    break;
                case "exit":
                    break label;
            }
        }
    }

}