package edu.escuelaing.arsw.ASE.app;

import java.net.*;
import java.io.*;
import java.nio.file.*;
import java.util.concurrent.*;

/**
 * A concurrent echo server that listens for incoming connections on a specified port
 * and handles multiple client requests concurrently using a fixed thread pool.
 */
public class EchoServerConcurrent {
    private static final int PORT = 35000;
    private static final int THREAD_POOL_SIZE = 5;

    /**
     * The main method that starts the echo server.
     * 
     * @param args command-line arguments (not used)
     * @throws IOException if an I/O error occurs when opening the socket
     */
    public static void main(String[] args) throws IOException {
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is listening on port " + PORT);

            while (true) {
                try {
                    executorService.submit(new ClientHandler(serverSocket));
                } catch (Exception e) {
                    System.err.println("Error submitting task to executor service: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Could not listen on port: " + PORT);
            System.exit(1);
        }
    }
}

/**
 * A handler for processing client connections. Implements the Runnable interface
 * to allow instances to be executed by a thread.
 */
class ClientHandler implements Runnable {
    private ServerSocket serverSocket;

    /**
     * Constructs a new ClientHandler.
     * 
     * @param serverSocket the server socket to accept connections from
     */
    public ClientHandler(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    /**
     * The run method is called by the executor service when a new client connection
     * is accepted. It handles the client's request and sends the appropriate response.
     */
    @Override
    public void run() {
        Socket clientSocket = null;
        try {
            clientSocket = serverSocket.accept();
            System.out.println("Accepted connection from " + clientSocket.getInetAddress());
            handleClient(clientSocket);
        } catch (IOException e) {
            System.err.println("Accept failed: " + e.getMessage());
        } finally {
            if (clientSocket != null) {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.err.println("Error closing client socket: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Handles the client's request by reading the input, processing the request,
     * and sending the appropriate response.
     * 
     * @param clientSocket the socket connected to the client
     */
    private void handleClient(Socket clientSocket) {
        try (OutputStream clientOutput = clientSocket.getOutputStream();
             PrintWriter out = new PrintWriter(clientOutput, true);
             BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {

            String inputLine;
            String requestedFile = null;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("Received: " + inputLine);
                if (inputLine.startsWith("GET")) {
                    requestedFile = inputLine.split(" ")[1];
                    if (requestedFile.equals("/")) {
                        requestedFile = "/index.html";
                    }
                    break;
                }
                if (!in.ready()) {
                    break;
                }
            }

            if (requestedFile != null) {
                serveFile(clientOutput, out, requestedFile);
            }

        } catch (IOException e) {
            System.err.println("Error handling client request: " + e.getMessage());
        }
    }


    /**
     * Serves the requested file to the client.
     * 
     * @param clientOutput   the output stream to send the file content to
     * @param out            the PrintWriter to send HTTP headers to the client
     * @param requestedFile  the file requested by the client
     */
    private void serveFile(OutputStream clientOutput, PrintWriter out, String requestedFile) {
        Path filePath = Paths.get("src\\main\\java\\edu\\escuelaing\\arsw\\ASE\\app\\webroot", requestedFile);
        if (Files.exists(filePath)) {
            try {
                String contentType = Files.probeContentType(filePath);
                byte[] fileContent = Files.readAllBytes(filePath);

                out.println("HTTP/1.1 200 OK");
                out.println("Content-Type: " + contentType);
                out.println("Content-Length: " + fileContent.length);
                out.println();
                out.flush();

                clientOutput.write(fileContent);
                clientOutput.flush();

                System.out.println("Served file: " + filePath.toString());
            } catch (IOException e) {
                System.err.println("Error serving file: " + e.getMessage());
                send404(out);
            }
        } else {
            send404(out);
        }
    }

    /**
     * Sends a 404 Not Found response to the client.
     * 
     * @param out the PrintWriter to send the 404 response to the client
     */

    private void send404(PrintWriter out) {
        String errorMessage = "<h1>404 Not Found</h1>";
        out.println("HTTP/1.1 404 Not Found");
        out.println("Content-Type: text/html");
        out.println("Content-Length: " + errorMessage.length());
        out.println();
        out.println(errorMessage);
        out.flush();
    }
}
