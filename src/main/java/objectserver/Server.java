package objectserver;

import shared.ChatMessage;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server extends Thread {

    private Socket socket;
    private int port;
    private ServerSocket serverSocket;
    private Map<String, Socket> loginMap = new HashMap<String, Socket>();

    public static void main(String[] args) {
        System.out.println(Thread.currentThread().getName());
        new Server().start();
    }

    public void start(){

        boolean running = true;
        int i = 1;

        try {
            System.out.println("Server starts...");
            ExecutorService executorService = Executors.newFixedThreadPool(10);
            serverSocket = new ServerSocket(5000);

            System.out.println(Thread.currentThread().getName());

            while(running){
                System.out.println("Waiting for a client..");
                socket = serverSocket.accept();
                executorService.submit(new TaskServer(socket));
                loginMap.put("client" + i, socket);
                i++;
                System.out.println("Got a client.");
            }

        } catch (IOException e) {
            System.out.println("Problem with a server " + e.getMessage());
        }
    }

    class TaskServer implements Runnable{

        private ObjectInputStream streamIn;
        private ObjectOutputStream streamOut;
        private Socket socket;
        private ChatMessage chatMessage;
        private boolean keepGoing = true;

        public TaskServer(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {

            System.out.println("Thread: " + Thread.currentThread().getName());

            try {
                streamOut = new ObjectOutputStream(socket.getOutputStream());
                streamIn = new ObjectInputStream(socket.getInputStream());
                System.out.println("elelo");

                while(keepGoing){

                    chatMessage = (ChatMessage) streamIn.readObject();

                    if(chatMessage.getType()== 0){
                        keepGoing = false;
                    }
                    else if(chatMessage.getType() == 1){
                        //whois
                        streamOut.writeObject(new ChatMessage(ChatMessage.BROADCAST, listUsers()));

                    } else if(chatMessage.getType() == 2){
                        //broadcast
                        System.out.println("Start broadcast");
                        broadcast(chatMessage.getMessage());

                    } else if(chatMessage.getType() == 3){
                        //private
                        String[]  login = chatMessage.getMessage().split(":");
                        sendPrivate(chatMessage.getMessage(), login[0]);
                    }

                }

                logout();

            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

        }

        private String listUsers() {
            String userList = "";
            for (String user : loginMap.keySet()) {
                userList += user + " ";
            }
            return userList;
        }

        private void sendPrivate(String message, String s) {
        }

        private void broadcast(String message) {

            ObjectOutputStream outputStream;
            for (Socket s : loginMap.values()) {
                try {
                    if(s.equals(this.socket)) {
                        streamOut.writeObject(new ChatMessage(ChatMessage.BROADCAST, message));
                    } else {
                        outputStream = new ObjectOutputStream(s.getOutputStream());
                        outputStream.writeObject(new ChatMessage(ChatMessage.BROADCAST, message));
                        outputStream.flush();
                    }

                } catch (IOException e) {
                    System.out.println("Cant send to socket " + e.getMessage());
                }
            }
        }

        private void logout() {
            try {
                loginMap.remove(this.socket);
                streamIn.close();
                streamOut.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}

