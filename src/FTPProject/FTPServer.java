package FTPProject;


import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * FTP Server class
 * 
 *
 */
public class FTPServer {
    private int controlPort = 2000;
    private ServerSocket serverSocket;
    boolean serverOn = true;
    public static void main(String[] args) {
        new FTPServer();
    }
    public FTPServer() {
        try {
            serverSocket = new ServerSocket(controlPort);
        } catch (IOException e) {
            System.out.println("Could not create control port server socket");
            System.exit(-1);
        }
        System.out.println("FTP Server listening on control port " + controlPort);
        int noOfThreads = 0;
        while (serverOn) {
            try {
            Socket client = serverSocket.accept();
            // Passive Mode ports
            int dataPort = controlPort + noOfThreads + 1;

            // Each connection gets a new worker thread
            FTPWorker worker = new FTPWorker(client, dataPort);

            System.out.println("New connection received, and a Worker was created.");
            noOfThreads++;
            worker.start();
            } catch (IOException e) {
            System.out.println("Exception when accepting new connection");
            e.printStackTrace();
            }

        }
        try {
            serverSocket.close();
            System.out.println("Server was stopped");
        } catch (IOException e) {
            System.out.println("Problem stopping server");
            System.exit(-1);
        }

    }

}