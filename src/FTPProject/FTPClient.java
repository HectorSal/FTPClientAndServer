package FTPProject;

import java.net.Socket;

import java.net.ServerSocket;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;

public class FTPClient {

    private static final String FTP_SERVER = "127.0.0.1";
    private static final int FTP_PORT = 2000; // Replace with your FTP server port

    private String root;
    private String currentDirectory;

    public static void main(String[] args) {
        try {
            Socket socket = new Socket(FTP_SERVER, FTP_PORT);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));

            // Read the welcome message
            String response;
            response = reader.readLine();
            System.out.println("Response from server: " + response);
            while (true) {
                System.out.print("Enter FTP command (QUIT to exit): ");
                String command = userInput.readLine();

                writer.println(command);

                if ("QUIT".equalsIgnoreCase(command)) {
                    socket.close();
                    break;
                }

                response = reader.readLine();
                System.out.println("Response from server: " + response);
                executeResponse(response);
            }
            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void executeResponse(String response) {
        try {
            int indexOfSpace = response.indexOf(' ');
            String code = response.substring(0, indexOfSpace).toUpperCase();
            switch (code) {
                case "227":
                    handlePasv(response);
                    break;
                default:
                    break;
            }
        } catch (NullPointerException e) {
            System.err.println("This command requires a parameter.");
        }
    }

    /**
     * 
     */
    private static void handlePasv(String response) {
        try {
            int indexOfParentheses = response.indexOf('(');
            String[] addressAndPort = response.substring(indexOfParentheses + 1).split(",");
            String address = addressAndPort[0] + "." + addressAndPort[1] + "." + addressAndPort[2] + "." + addressAndPort[3];
            int port = Integer.valueOf(addressAndPort[4]) * 256 + Integer.valueOf(addressAndPort[5]);            
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }
}
