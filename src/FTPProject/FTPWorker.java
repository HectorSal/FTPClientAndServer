package FTPProject;


import java.net.Socket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedReader;


/**
 * FTP Worker class
 * 
 *
 */
public class FTPWorker extends Thread{

    private boolean quitCommandLoop = false;

    // data
    private Socket dataConnection;
    private ServerSocket dataSocket;
    private PrintWriter dataOutWriter;
    private int dataPort;

    // control
    private Socket controlSocket;
    private PrintWriter controlOutputWriter;
    private BufferedReader controlInputReader;

    private String root;
    private String currentDirectory;

    private enum userStatus {
        SIGNEDOUT, ENTEREDNAME, SIGNEDIN
    }
    private String validUser = "estrella";
    private String validPassword ="root";
    private userStatus currentUserStatus = userStatus.SIGNEDOUT;
    /**
     * Creates an FTP worker with the client and data port
     * 
     * @param client current client socket
     * @param dataPort data connection port
     */
    public FTPWorker(Socket client, int dataPort) {
        super();
        this.controlSocket = client;
        this.dataPort = dataPort;
        this.currentDirectory = System.getProperty("user.dir") + "/root";
        this.root = currentDirectory;
    }
    /**
     * Java thread run method
     */
    public void run() {
        try {
            // input reader and output writer
            controlInputReader = new BufferedReader(new InputStreamReader(controlSocket.getInputStream()));
            controlOutputWriter = new PrintWriter(controlSocket.getOutputStream(), true);
            notifyClient("The ftp server is running woooo");
            
            // Client sends command
            while (!quitCommandLoop) {
                executeCommand(controlInputReader.readLine());
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                controlOutputWriter.close();
                controlInputReader.close();
                controlSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Parses command and args from line and calls the specific function to run the command
     * 
     * @param line socket input containing commmand and args
     */

    private void executeCommand(String line) {
        int indexOfSpace = line.indexOf(' ');
        String command;
        String args;
        if (indexOfSpace == -1) {
            command = line.toUpperCase();
            args = null;
        }
        else {
            command = line.substring(0, indexOfSpace).toUpperCase();
            args = line.substring(indexOfSpace + 1);
        }

        System.out.println(command);
        switch (command) {
            case "USER":
            handleUser(args);
            break;

            case "PASS":
            handlePass(args);
            break;

            case "MKD":
            handleMkd(args);
            break;

            case "RMD":
            handleRmd(args);
            break;

            case "CWD":
            handleCwd(args);
            break;

            case "XPWD":
            handlePwd();
            break;

            case "PWD":
            handlePwd();
            break;

            case "PASV":
            handlePasv();
            break;

            case "NLST":
            handleNlst(args);
            break;

            case "STOR":
            handleStor(args);
            break;

            case "RETR":
            handleRetr(args);
            break;

            case "DELE":
            handleDele(args);
            break;

            case "QUIT":
            handleQuit();
            break;

            default:
                notifyClient("501 Not Implemented");
                break;
        }

    }

    /**
     * PWD command handler
     */
    private void handlePwd() {
        if (this.currentUserStatus.equals(userStatus.SIGNEDIN)){
            notifyClient("257 /" + currentDirectory + "/");
        }
        else {
            notifyClient("530 Not logged in.");
        }
    }

    /**
     * Quit command handler
     * 
     */
    private void handleQuit() {
        notifyClient("221 Goodbye!");
        quitCommandLoop = true;
    }

    /**
     * DELE command handler
     * @param args 
     */
    private void handleDele(String file) {
        if (this.currentUserStatus.equals(userStatus.SIGNEDIN)){
            String fileName = currentDirectory;
            if (file != null && file.matches("^[a-zA-Z0-9_\\-/]+/[a-zA-Z0-9_\\-]+\\.[a-zA-Z0-9]+$")) {
                fileName = fileName + "/" + file;
                File deletedFile = new File(fileName);
                if (deletedFile.exists() && deletedFile.isFile()) {
                    deletedFile.delete();
                    notifyClient("250 File deleted");
                }
                else {
                    notifyClient("550 Requested action not taken; file unavailable");
                }
            }
            else {
                notifyClient("550 Invalid file name.");
            }
        }
        else {
            notifyClient("530 Not logged in.");
        }

    }
    private void handleRetr(String file) {
        if (this.currentUserStatus.equals(userStatus.SIGNEDIN)){
            File retrievedFile = new File(currentDirectory + "/" + file);
            if (file==null) {
                notifyClient("501 Syntax error. No file given");
            }
            else if (!retrievedFile.exists()) {
                notifyClient("550 File does not exist");
            }
            else {
                notifyClient("150 File status okay; about to open data connection.");
                BufferedOutputStream output = null;
                BufferedInputStream input = null;
                
                // output is where we send the file to the data connection Output Stream
                // input requestedile input stream
                try {
                    output = new BufferedOutputStream(dataConnection.getOutputStream());
                    input = new BufferedInputStream(new FileInputStream(retrievedFile));
                }
                catch (Exception e) {
                    e.printStackTrace();
                }

                byte[] buffer = new byte [1024];
                
                int length = 0;

                try {
                    // while the end of the stream has not been reached
                    while ((length = input.read(buffer, 0, 1024)) != -1) {
                        output.write(buffer, 0, length);
                    }
                }
                catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    input.close();
                    output.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                notifyClient("226 File successfuly retrieved. Closing data connection.");
            }
            closeDataConnection();
        }
        else {
            notifyClient("530 Not logged in.");
        }
        
    }

    /**
     * STOR command handler
     * @param filename the file that will be stored
     */
    private void handleStor(String filename) {
        if (this.currentUserStatus.equals(userStatus.SIGNEDIN)){
            if (dataConnection == null) {
                notifyClient("425 Can’t open data connection.");
            }
            else if (filename == null) {
                notifyClient("501 Syntax error. No file given");
            }
            else {
                File storedFile = new File(currentDirectory + "/" + filename);
                notifyClient("150 File status okay; about to open data connection.");
                BufferedOutputStream output = null;
                BufferedInputStream input = null;
    
                try {
                    // like the retrHandler but output and input are reversed
                    output = new BufferedOutputStream(new FileOutputStream(storedFile));
                    input = new BufferedInputStream(dataConnection.getInputStream());
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
    
                byte[] buffer = new byte[1024];
                int length = 0;
                try {
                    while ((length = input.read(buffer, 0, 1024)) != -1) {
                        output.write(buffer, 0, length);
                    }
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                
                try {
                    input.close();
                    output.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                notifyClient("226 File successfuly stored. Closing data connection");
            }
            closeDataConnection();
        }
        else {
            notifyClient("530 Not logged in.");
        }
    }
    /**
     * Close data connections 
     */
    private void closeDataConnection() {
        try {
            dataConnection.close();
            dataOutWriter.close();
            if (dataSocket != null) {
                dataSocket.close();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        dataConnection = null;
        dataOutWriter = null;
        dataSocket = null;
    }

    /**
     * Handler for NLST command which lists the names of files and directories
     * @param args
     */
    private void handleNlst(String args) {
        if (this.currentUserStatus.equals(userStatus.SIGNEDIN)){
            if (dataConnection == null || dataConnection.isClosed()) {
                notifyClient("425 Can't open data connection");
            }
            else {
                String[] list = null;
                String filename = currentDirectory;
                // nlst command could have no argument in which case the filename would just be the current directory. Otherwise the argument is added
                if (args != null) {
                    filename = filename + "/" + args;
                }
                
                //check if the file is a directory
                File file = new File(filename);
                if (file.exists() && file.isDirectory()) {
                    list = file.list();
                }
                // the file is a file
                else if (file.exists() && file.isFile()) {
                    list = new String[1];
                    list[0] = file.getName();
                }
    
                if (list == null) {
                    notifyClient("550 File does not exist");
                }
                else {
                    notifyClient("125 Opening data connection");
                    for (int i = 0; i < list.length; i ++) {
                        dataOutWriter.print(list[i] + '\r' + '\n');
                    }
                }
                notifyClient("226 List successfully transfered");
                closeDataConnection();
            }

        }
        else {
            notifyClient("530 Not logged in.");
        }

    }
    private void handlePasv() {
        if (this.currentUserStatus.equals(userStatus.SIGNEDIN)){
            String ipAddress = "127.0.0.1";
            int port1 = dataPort / 256;
            int port2 = dataPort % 256;
            String response = "227 Entering Passive Mode (" + ipAddress.replace('.', ',') +
                        "," + String.valueOf(port1) + "," + String.valueOf(port2) + ")";
            notifyClient(response);
            enterPassiveMode(dataPort);    
        }
        else {
            notifyClient("530 Not logged in.");
        }
        
    }

    /**
     * Helper function for handlePasv which opens a connection socket which the client will connect to 
     * 
     * @param port port to listen on
     */
    private void enterPassiveMode(int port) {
        try {
            dataSocket = new ServerSocket(dataPort);
            dataConnection = dataSocket.accept();
            //set auto flush to true
            dataOutWriter = new PrintWriter(dataConnection.getOutputStream(), true);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * CWD command handler
     * @param directory Directory to change to
     */
    private void handleCwd(String directory) {
        if (this.currentUserStatus.equals(userStatus.SIGNEDIN)){
            String directoryName = currentDirectory;
            if (directory != null && directory.equals("..")) {
                int index = directoryName.lastIndexOf("/");
                if (index > 0 )
                    directoryName = directoryName.substring(0, index);
            }
            else if (directory != null && (!directory.equals("."))) {
                directoryName = directoryName + "/" + directory;
            }
            File file = new File(directoryName);
            
            // the directory being changed to cannot be above the root directory 
            if (file.exists() && file.isDirectory() && (directoryName.length() >= root.length())) {
                currentDirectory = directoryName;
                notifyClient("250 Directory changed succesfully to " + currentDirectory);
            }
            else {
                notifyClient("550 Requested action not taken. Directory unavailable.");
            }
        }
        else {
            notifyClient("530 Not logged in.");
        }

    }
    /**
     * RMD command handler
     * @param directory deleted directory
     */
    private void handleRmd(String directory) {
        if (this.currentUserStatus.equals(userStatus.SIGNEDIN)){
            String directoryName = currentDirectory;
            if (directory != null && directory.matches("^[a-zA-Z0-9_\\-/]+$")) {
                directoryName = directoryName + "/" + directory;
                File file = new File(directoryName);
                if (file.exists() && file.isDirectory()) {
                    file.delete();
                    notifyClient("250 Directory deleted");
                }
                else {
                    notifyClient("550 Requested action not taken; directory unavailable");
                }
            }
            else {
                notifyClient("550 Requested action not taken; Invalid directory name.");
            }    
        }
        else {
            notifyClient("530 Not logged in.");
        }
    }
    /**
     * MKD handler
     * @param directoryName name of directory to be created
     */
    private void handleMkd(String directoryName) {
        if (this.currentUserStatus.equals(userStatus.SIGNEDIN)){
            if (directoryName != null && directoryName.matches("^[a-zA-Z0-9]+$")) {
                File directory = new File(currentDirectory + "/" + directoryName);
                if (!directory.mkdir()) {
                    notifyClient("550 Requested action not taken; Unable to create new directory");
                }
                else {
                    notifyClient("250 Directory created");
                }
            }
            else {
                notifyClient("550 Requested action not taken; Invalid directory name");
            }
        }
        else {
            notifyClient("530 Not logged in.");
        }
    }
    /**
     * PASS handler
     * 
     * @param password inputted password
     */
    private void handlePass(String password) {
        if (currentUserStatus == userStatus.ENTEREDNAME && password.equals(validPassword)) {
            currentUserStatus = userStatus.SIGNEDIN;
            notifyClient("230 User logged in, proceed.");
        }

        else
            notifyClient("530 User cannot log in");

    }
    /**
     * USER handler
     * @param username inputted username
     */
    private void handleUser(String username) {
        if (username != null && username.equals(validUser)) {
            notifyClient("331 User is correct. Password is needed.");
            currentUserStatus = userStatus.ENTEREDNAME;
        }
        else
            notifyClient("530 User cannot log in");
    }

    /**
     * Notifies client with a message
     * 
     * @param msg message to be sent to client
     */
    private void notifyClient(String msg) {
        controlOutputWriter.println(msg);
    }


}

