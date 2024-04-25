package org.example;


import javax.naming.ldap.SortKey;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//We will be using Runnable to create this ChatRoom
//REMEMBER we must always Override run when using Runnable Interface
public class Server implements Runnable {

    private ArrayList<ConnectionHandler> connections;
    private ServerSocket server;
    private boolean done;
    private ExecutorService pool;



    public Server() {
        connections = new ArrayList<>();
        done = false;
    }


    public void broadcast(String message){
        for (ConnectionHandler ch : connections){
            if (ch != null){
                ch.sendMessage(message);
            }
        }
    }

    public void shutdown(){
        done = true;
        pool.shutdown();
        if(!server.isClosed()){
            try {
                server.close();
                for (ConnectionHandler ch : connections){
                    ch.shutdown();
                }
            } catch (IOException e) {
                //Ignore
            }
        }

    }





    /**
     * Here we have the code that is executed when we RUN or START the runnable class
     * SO, the sever will constantly listen for
     * incoming connections so for org.example.Client requests to CONNECT
     * then accept these connection request
     * then we're going to open a new connection handler for each
     * client that connects
     */
    @Override
    public void run() {
        try {
            server = new ServerSocket(9999);
            pool = Executors.newCachedThreadPool();
            while (!done){

                Socket client = server.accept();

                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);
                //Every time we add a new connection  we want
                pool.execute(handler);

            }


        } catch (Exception e) {
            //TODO: handle
            shutdown();
        }


    }





    public class ConnectionHandler implements Runnable{
        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String name;

        /**
         * So the BufferReader is going to lbe used to get the stream from the SOCKET
         * --> so when the client sends something where to get it from  'in' &
         * ---> and when we want to write something to the client we're going to use
         * ----> out
         */
        public ConnectionHandler(Socket client){
            this.client = client;

        }



        @Override
        public void run(){


            try{
                Server server = new Server();
                //autoFlush = true, so we don't need to always manually flush the output stream
                //in order to actually send the messages.
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));

                // Assuming client is entering a valid name
                out.println("Please enter your name ?");
                name = in.readLine();

                System.out.println(name + " connected!");
                broadcast(name + " joined the chat");

                String message;
                while ((message = in.readLine()) != null){
                    if (message.startsWith("/change-name")){
                        // TODO: handle name
                        String[] messageSplit = message.split(" ", 2);

                        if (messageSplit.length == 2 ){
                            broadcast(name + " Renamed themselves to " + messageSplit[1]);
                            System.out.println(name + " Renamed themselves to " + messageSplit[1]);
                            name = messageSplit[1];

                            out.println("Successfully changed nickname to " + name);
                        }else {
                            out.println("No name was provided.");
                        }



                    } else if (message.startsWith("/quit")){
                        // TODO: quit
                        broadcast(name + " left the chat");
                        shutdown();




                    }else {
                        broadcast(name + ": " + message);
                    }
                }


            }catch (IOException e){
                //TODO: handle
                shutdown();
            }

        }



        public void sendMessage(String message){
            out.println(message);
        }

        public void shutdown(){
            try {
                in.close();
                out.close();
                if(!client.isClosed()){
                    client.close();
                }

            }catch (IOException e){
                // Ignore

            }
        }





    }



    public static void main(String[] args) {
        Server server = new Server();
        server.run();

    }


}
