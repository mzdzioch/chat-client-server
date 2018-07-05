package client;

import shared.ChatMessage;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ClientChat{
    private Socket socket;
    //    private BufferedReader streamIn;
//    private PrintStream streamOut;
    private ObjectOutputStream streamOut;
    private ObjectInputStream streamIn;
    private String server;
    private int port;
    private Thread thread;
    private ListenFromServer myRunnable;
    private ChatMessage chatMessage;

    public ClientChat(String server, int port) {
        this.server = server;
        this.port = port;
    }

    public boolean start(){

        try {
            display("Starting Chat Clients...");
            socket = new Socket(server, port);

//            streamIn = new BufferedReader(
//                    new InputStreamReader(socket.getInputStream())
//            );
//            streamOut = new PrintStream(socket.getOutputStream());
            streamIn = new ObjectInputStream(socket.getInputStream());
            streamOut = new ObjectOutputStream(socket.getOutputStream());

            myRunnable = new ListenFromServer();
            thread = new Thread(myRunnable);
            thread.start();

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void sendMessage(ChatMessage message){
        try {
            streamOut.writeObject(message);
            streamOut.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        String serverIp = "localhost";
        int portNumber = 5000;
        String msg;

        ClientChat client = new ClientChat(serverIp, portNumber);

        if(!client.start())
            return;

        Scanner scanner = new Scanner(System.in);

        while (true){
            System.out.print("> ");
            msg = scanner.nextLine();
            if(msg.compareToIgnoreCase("LOGOUT") == 0){
                client.sendMessage(new ChatMessage(ChatMessage.LOGOUT, null));
                break;
            } else if(msg.compareToIgnoreCase("WHOIS") == 0){
                client.sendMessage(new ChatMessage(ChatMessage.WHOIS, null));
            } else if(msg.startsWith("BROADCAST")) {
                client.sendMessage(new ChatMessage(ChatMessage.BROADCAST, msg));
            } else if(msg.startsWith("PRIVATE")){
                client.sendMessage(new ChatMessage(ChatMessage.PRIVATE, msg));
            }
        }
        client.disconnect();
    }

    public class ListenFromServer implements Runnable{

        private boolean running = true;
        private ChatMessage chatMessage;

        @Override
        public void run() {
            while(running){
                try {
                    chatMessage = (ChatMessage) streamIn.readObject();
                    display(chatMessage.getMessage());
                    display("> ");
                } catch (IOException e) {
                    display("Cannot receive message from server " + e.getMessage());
                    destroyThread();
                } catch (ClassNotFoundException e){
                    display("Object not found " + e.getMessage());
                    destroyThread();
                }
            }
        }

        private void destroyThread() {
            running = false;
        }
    }

    private void disconnect(){
        try{
            if(streamIn != null) streamIn.close();
            if(streamOut != null) streamOut.close();
            if(socket != null) socket.close();
            if(myRunnable != null) myRunnable.destroyThread();
        } catch(IOException error){
            System.out.println("Problem with disconnect " + error.getMessage());
        }
    }

    private void display(String msg){
        System.out.println(msg);
    }

}