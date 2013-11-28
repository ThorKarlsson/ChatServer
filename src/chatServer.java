import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: Toggi
 * Date: 8.3.2012
 * Time: 01:04
 * To change this template use File | Settings | File Templates.
 */
public class chatServer extends Thread {
    private int port;
    private ServerSocket serverSocket;
    private ArrayList<userSocket> users; //arraylist of connected clients
    private DataOutputStream output;
    private int maxClients;

    public int clientCount;
    public PrintStream logFile;
    public boolean logInUse; //semaphore for log file

    public method logMethod;
    public enum method{silent, control, all};

    /**
     * Initializes and starts chat server, creates log file, server socket, and client arrayList
     *
     * @param logMethod method of logging which will be used
     * @param maxClients max number of clients allowed in server
     */
    public chatServer(int logMethod, int maxClients){
        try{
            this.maxClients = maxClients;
            port = 8080;
            switch(logMethod){
                case 0:
                    this.logMethod = method.silent;
                    break;
                case 1:
                    this.logMethod = method.control;
                    break;
                case 2:
                    this.logMethod = method.all;
                    break;
                default:
                    this.logMethod = method.silent;
                    System.out.println("Log method error");
                    break;
            }
            logFile = new PrintStream(new FileOutputStream("Log.txt"));
            logInUse = false;
            serverSocket = new ServerSocket(port);
            log("Server started successfully... listening at port " + port);
            users = new ArrayList<userSocket>();
            clientCount = 0;

            start();
        }
        catch(IOException e){
            log(e.getMessage());
        }
    }

    /**
     * Broadcasts message from userName to all users
     * @param message message which userName is sending
     *
     * @param userName user name of user who is broadcasting
     */
    public void output(String message, String userName){
        String outMessage = "<" + userName + "> "  + message + '\n';
        String logMessage = "Sent: " + outMessage + " to ";
        try{
            for(userSocket u : users){
                u.toClient.writeBytes(outMessage);
                logMessage = logMessage.concat(u.connectionSocket.getRemoteSocketAddress().toString());
            }
        }
        catch(IOException e){
            log("error broadcasting message");
        }
        if(logMethod == method.all){
            log(logMessage);
        }
    }

    /**
     * Same as above but simply broadcasts message and nothing else
     * @param message message to be sent to all users
     */
    public void output(String message){
        try{
            for(userSocket u : users){
                u.toClient.writeBytes(message + '\n');
            }
        }
        catch(IOException e){
            log("error broadcasting message");
        }
    }

    /**
     * Removes a user with given index and userName. Broadcasts to all users that userName has
     * disconnected. Also decrements clientCount to free up room in chat room
     *
     * @param index index of the user who disconnected
     * @param userName user name of the user who disconnected
     */
    public void remove(int index, String userName){
        try{
            users.remove(index);
        }
        catch(IndexOutOfBoundsException e){//Something terribly wrong has occurred in the indexing of users
            log("Unexpected error removing user " + userName);
            System.exit(-1);//Server must restart if this occurs because our users list will be rendered useless
        }
        clientCount--;
        if(userName != null){
            output("*" + userName + " has left the chat");
            if(logMethod == method.control || logMethod == method.all){
                log("unregistered " + userName);
            }
        }
        for(userSocket u : users){//update index after a user leaves chat
            u.index = users.indexOf(u);
        }
    }

    /**
     * Runs indefinitely or at least until an exception is thrown. Waits for client to connect then adds client
     * to users. If server is full the connection is terminated, otherwise a new thread is created and started.
     * Then we wait for another client
     */
    public void run() {
        while(true){
            try{
                Socket connectionSocket = serverSocket.accept();//waits for connections
                clientCount++;
                output = new DataOutputStream(connectionSocket.getOutputStream());
                output.writeBytes("Server: Connected to " + connectionSocket.getLocalAddress().toString() + '\n');
				userSocket us = new userSocket(connectionSocket, this, clientCount-1);
                if(logMethod == method.control || logMethod == method.all){
                    log("Connection established to " + connectionSocket.getRemoteSocketAddress().toString() + " (" +
                            clientCount + " open connections)");
                }
                if(clientCount > maxClients){//if chat room is full
                    output.writeBytes("Chat room is full. Terminating connection...");
                    us.connectionSocket.close();
                    clientCount--;
                    if(logMethod == method.control || logMethod == method.all){
                        log("Connection terminated with " + connectionSocket.getRemoteSocketAddress().toString() +
                                " Chat server is full (" + clientCount + " open connections)");
                    }
                }
                else{
                    us.start();
                    users.add(us);
                }
            }
            catch (Exception e){
                log(e.getMessage());
            }
        }
    }

    /**
     * takes in string and outputs it to log file
     *
     * @param logMessage string which will be placed in log file
     */
    public void log(String logMessage){
        while(logInUse){}//wait for log file to become available
        logInUse = true;//thread now owns log file
        try{
            logFile.println(logMessage);
        }
        catch (Exception e){
            System.out.println("Unexpected error in log file log.txt");//log this error... oh wait :)
            System.out.println(e.getMessage());
        }
        logInUse = false; //free log file for other threads
    }

    public static void main(String args[]){
        int logMethod = 0;
        int maxClients = 0;
        //log argument
        try{
            if(args[0].equals("SILENT")){
                logMethod = 0;
            }
            else if(args[0].equals("CONTROL")){
                logMethod = 1;
            }
            if(args[0].equals("ALL")){
                logMethod = 2;
            }

            if(args[1].equals("-c")){
                maxClients = Integer.valueOf(args[2]);
            }
            chatServer cs = new chatServer(logMethod, maxClients);

        }
        catch(Exception e){
            System.out.println("Usage:");
            System.out.println("chatServer <SILENT | CONTROL | ALL> -c <Maximum number of users>");
        }
    }
}
