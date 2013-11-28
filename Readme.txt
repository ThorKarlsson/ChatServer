basic chat server functionality
– listen on port 8080 for connection requests from clients
– support the two-staged login that your clients implement:

1. client opens TCP connection and
2. client signs in with USER <name> message.


A client is only allowed to send and receive messages after it is signed in with a user name. If a client tries to send messages before being signed in with a user name, an error message is returned to the client. The connection to the client is kept open and the server keeps waiting for a USER message from this client.
– when a TCP connection was opened the server welcomes the client with the message "Server: connected to <IP:port>" with "<IP:port>" being replaced with the appropriate server data.
– support connections to several clients at the same time
– broadcast received messages to all signed in users and appends <user name>: before each message.
– broadcast information on who joined or left the chat to all signed in users
including the one that just joined the chat:
 foo joined chat.
 bar left chat.