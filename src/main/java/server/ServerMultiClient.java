package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerMultiClient {

    HashMap<String, Socket> logedSocketMap = new HashMap<String, Socket>();

    public static void main(String[] args) {
        new ServerMultiClient().startServer();
    }

    public void startServer() {
        final ExecutorService clientProcessingPool = Executors.newFixedThreadPool(10);
        System.out.println("1. -> " + Thread.currentThread().getName());

        Runnable serverTask = new Runnable() {
            @Override
            public void run() {
                try {
                    ServerSocket serverSocket = new ServerSocket(5000);
                    System.out.println("Waiting for clients to connect...");
                    System.out.println("2. -> " + Thread.currentThread().getName());

                    while (true) {
                        System.out.println("3. -> " + Thread.currentThread().getName());
                        Socket clientSocket = serverSocket.accept();
                        clientProcessingPool.submit(new ClientTask(clientSocket));
                    }
                } catch (IOException e) {
                    System.err.println("Unable to process client request");
                    e.printStackTrace();
                }
            }
        };
        System.out.println("Server starting...");
        Thread serverThread = new Thread(serverTask);
        serverThread.start();

    }

    private class ClientTask implements Runnable {
        private final Socket clientSocket;
        PrintStream clientOut;
        BufferedReader clientIn;

        private ClientTask(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            System.out.println("4. -> " + Thread.currentThread().getName());
            System.out.println("Got a client !");

            try {
                clientOut = new PrintStream(clientSocket.getOutputStream());

                clientIn = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream()
                        ));

                //clientOut.println("Please give your login: ");
                String line = "";

                while(line.compareToIgnoreCase("logout") != 0) {

                    if(line.startsWith("LOGIN:")){

                        String messages[] = line.split(":");
                        System.out.println(clientSocket);
                        logedSocketMap.put(messages[1], clientSocket);
                        System.out.println("Logged!" + logedSocketMap.get(messages[1]).getInetAddress());

                    } else if (line.startsWith("BROADCAST:")) {

                        broadcast(logedSocketMap, line);
                        System.out.println(line);

                    } else if(line.startsWith("PRIVATE:")) {
                        String logins[] = line.split(":");
                        String sendTo = logins[1];

                        sendMessage(logedSocketMap.get(sendTo), line);
                    } else {
                        System.out.println(line);
                    }
                    line = clientIn.readLine();
                }

                System.out.println("Zamykamy streamy i polaczenie");
                close();

            } catch (IOException e) {
                System.out.println("Blad " + e.getMessage());
                e.printStackTrace();
            }
        }

        public void broadcast(HashMap<String, Socket> socketMap, String message) {
            for (Socket socket : socketMap.values()) {
                try {
                    System.out.println("Broadcastuje" + socket.toString());
                    PrintStream clientOut = new PrintStream(socket.getOutputStream());
                    clientOut.println(message);
                    clientOut.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void sendMessage(Socket clientSocket, String message) {

            try {
                PrintStream o = new PrintStream(clientSocket.getOutputStream());
                o.println(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void close(){
            try {
                System.out.println("Closed!");
                clientIn.close();
                clientOut.close();
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

