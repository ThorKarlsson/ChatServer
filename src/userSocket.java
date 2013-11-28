import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * Created by IntelliJ IDEA.
 * User: Toggi
 * Date: 9.3.2012
 * Time: 00:02
 * To change this template use File | Settings | File Templates.
 */
public class userSocket extends Thread{

    Socket connectionSocket;
    String userName;
    boolean signedIn;
    BufferedReader fromClient;
    DataOutputStream toClient;
    String message;
    chatServer cs;
    int index;

    /**
     * Initializes userSocket which is associated with a client connected to the server.
     *
     * @param socket socket connecting server and client
     * @param cs chatServer which owns userSocket
     * @param index variable keeps track of where in cs.users this particular thread is
     */
    public userSocket(Socket socket, chatServer cs, int index){
        try{
            this.connectionSocket = socket;
            signedIn = false;
            this.index = index;
            this.cs = cs;
            fromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
            toClient = new DataOutputStream(connectionSocket.getOutputStream());
        }
        catch (IOException e){
            cs.log("Error in input output stream: " + e.getMessage());
        }
    }

    /**
     * Terminates connection to server. Called when chat room is full and
     * cannot sustain anymore connections
     */
    public void full(){
        terminateConnection();
    }

    /**
     * runs until user quits client. Oversees logging in of the user and validating input
     * output and logging is passed on back to cs
     */
    public void run() {
        try{
            while(true){
                if(!connectionSocket.isConnected()){//if client is no longer connected throw IOException
                    throw new IOException();
                }
                message = fromClient.readLine();//read in FIRST message from client
                if(message == null){//if message is null then client is gone
                    throw new IOException();
                }
                String tokens[] = message.split(" ");//parse client message into tokens

                if(tokens[0].equals("USER") && tokens.length > 1){
                    userName = tokens[1];
                    signedIn = true;
                    message = "<"+userName+"> has joined the chat";
                    if(cs.logMethod == chatServer.method.control || cs.logMethod == chatServer.method.all){
                        cs.log("Registered: " + userName);
                    }
                    cs.output(message);
                }
                else{
                    toClient.writeBytes("You must sign in before sending messages" + '\n');
                    if(cs.logMethod == chatServer.method.control || cs.logMethod == chatServer.method.all){
                        cs.log("Unsuccessful sign in from " + connectionSocket.getRemoteSocketAddress().toString());
                    }
                }

                while(signedIn){
                    message = fromClient.readLine();
                    if(message == null){//if message is null user has disconnected
                        throw new IOException();
                    }
                    if(cs.logMethod == chatServer.method.all){
                        cs.log("Received: " + message +" from " + connectionSocket.getRemoteSocketAddress());
                    }
                    cs.output(message, userName);
                }
            }
        }
        catch(IOException e){
            terminateConnection();
        }
    }

    /**
     * Terminates connection associated with this thread and outputs
     */
    public void terminateConnection(){

        cs.remove(index, userName);
        try{
            connectionSocket.close();
            if(cs.logMethod == chatServer.method.control || cs.logMethod == chatServer.method.all){
                cs.log("Connection terminated with " + connectionSocket.getRemoteSocketAddress().toString() +
                        "(" + cs.clientCount + " open connections)");
            }

        } catch (IOException e1){
            if(cs.logMethod == chatServer.method.control || cs.logMethod == chatServer.method.all){
                cs.log("Error closing connection with" + connectionSocket.getRemoteSocketAddress().toString());
            }
        }
    }
}
