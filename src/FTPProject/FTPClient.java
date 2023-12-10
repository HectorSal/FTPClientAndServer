    package FTPProject;

import java.net.Socket;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedReader;

public class FTPClient {

    private Socket controlSocket;
    private Socket dataSocket;
    private BufferedReader controlReader;
    private BufferedReader dataReader;
    private PrintWriter controlWriter;
    private static final String FTP_SERVER = "127.0.0.1";
    private static final int FTP_PORT = 2000; // Replace with your FTP server port

    private String currentDirectory;
    private boolean isConnected;

    public FTPClient() {
        this.controlSocket = null;
        this.dataSocket = null;
        this.controlReader = null;
        this.dataReader = null;
        this.controlWriter = null;
    }
    public static void main(String[] args) {
        try {
            FTPClient client = new FTPClient();
            client.currentDirectory = System.getProperty("user.dir") + "/test";
            System.out.println(client.currentDirectory);
            client.controlSocket = new Socket(FTP_SERVER, FTP_PORT);
            client.controlReader = new BufferedReader(new InputStreamReader(client.controlSocket.getInputStream()));
            client.controlWriter = new PrintWriter(client.controlSocket.getOutputStream(), true);
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));

            client.isConnected = true;
            // Read the welcome message
            String response = client.controlReader.readLine();
            System.out.println("Response from server: " + response);
            while (client.isConnected) {
                System.out.print("Enter FTP command (QUIT to exit): ");
                String command = userInput.readLine();
                client.executeCommand(command);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void executeCommand(String line) throws IOException {
        String response;
        controlWriter.println(line);
        response = controlReader.readLine();
        System.out.println("Response from server: " + response);
        int indexOfSpaceLine = line.indexOf(' ');
        int indexOfSpaceResponse = response.indexOf(' ');
        String codeString = response.substring(0, indexOfSpaceResponse).toUpperCase();
        int code = Integer.valueOf(codeString);
        if (code != 530) {
            String command;
            String args;
            if (indexOfSpaceLine == -1) {
                command = line.toUpperCase();
                args = null;
            }
            else {
                command = line.substring(0, indexOfSpaceLine).toUpperCase();
                args = line.substring(indexOfSpaceLine + 1);
            }
            switch (command) {
                case "DELE":
                    handleDele();
                    break;
                case "STOR":
                    handleStor(args, code);
                    break;
                case "RETR":
                    handleRetr(args, code);
                    break;
                case "NLST":
                    handleNlst(code);
                    break;
                case "PASV":
                    handlePasv(response);
                    break;
                case "QUIT":
                    controlSocket.close();
                    isConnected = false;
                    break;
                default:
                    break;
            }
        }
    }
    private void handleDele() {
        
    }
    private void handleNlst(int code) {
        if (code == 125) {
            BufferedInputStream input = null;
            try {
                input = new BufferedInputStream(this.dataSocket.getInputStream());
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            try {
                System.out.println(convertStreamToString(input));
                input.close();
                String response = controlReader.readLine();
                System.out.println("Response from server: " + response);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        closeDataConnection();
    }

    public String convertStreamToString(InputStream is) throws IOException {
        if (is != null) {
            Writer writer = new StringWriter();

            char[] buffer = new char[1024];
            try
            {
                Reader reader = new BufferedReader(
                        new InputStreamReader(is, "UTF-8"));
                int n;
                while ((n = reader.read(buffer)) != -1) 
                {
                    writer.write(buffer, 0, n);
                }
            }
            finally 
            {
                is.close();
            }
            return writer.toString();
        }
        else {       
            return "";
        }
    }

    private void handleRetr(String filename, int code) {
        if (code == 150) {
            File storedFile = new File(currentDirectory + "/" + filename);
            BufferedOutputStream output = null;
            BufferedInputStream input = null;
            try {
                output = new BufferedOutputStream(new FileOutputStream(storedFile));
                input = new BufferedInputStream(this.dataSocket.getInputStream());
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
                String response = controlReader.readLine();
                System.out.println("Response from server: " + response);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        closeDataConnection();
    }
    private void closeDataConnection() {
        try {
            if (dataReader != null) {
                dataReader.close();
            }
            if (dataSocket != null) {
                dataSocket.close();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        dataReader = null;
        dataSocket = null;
    }
    private void handleStor(String filename, int code) {
        File retrievedFile = new File(currentDirectory + "/" + filename);
        if (code == 150) {
            BufferedOutputStream output = null;
            BufferedInputStream input = null;
            
            // output is where we send the file to the data connection Output Stream
            // input requestedile input stream
            try {
                output = new BufferedOutputStream(dataSocket.getOutputStream());
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
                String response = controlReader.readLine();
                System.out.println("Response from server: " + response);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        closeDataConnection();
            
    }
    /**
     * 
     */
    private void handlePasv(String response) {
        try {
            int indexOfParentheses = response.indexOf('(');
            String[] addressAndPort = response.substring(indexOfParentheses + 1).split(",");
            String address = addressAndPort[0] + "." + addressAndPort[1] + "." + addressAndPort[2] + "." + addressAndPort[3];
            int length = addressAndPort[5].length();
            addressAndPort[5] = addressAndPort[5].substring(0, length - 1);
            int port = Integer.valueOf(addressAndPort[4]) * 256 + Integer.valueOf(addressAndPort[5]);

            System.out.println("PORT" + port);
            this.dataSocket = new Socket(address, port);
            this.dataReader = new BufferedReader(new InputStreamReader(dataSocket.getInputStream()));
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }
}
