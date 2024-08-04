/**
 * Proposer.java
 *
 * This class is responsible for creating topics on a remote server via RMI (Remote Method Invocation).
 * The Proposer connects to an RMI registry, looks up the server interface, and invokes the method to create topics.
 * The topics to be created are provided as command-line arguments.
 *
 * Design Choices:
 * - **RMI Protocol**: This class uses the RMI protocol to communicate with a remote server, allowing the creation of topics on that server.
 * - **Command-Line Arguments**: Topics are passed as command-line arguments in the format "--topic=<TopicName>".
 * - **Logging**: Utilizes Java's built-in logging framework (`java.util.logging`) to log significant events such as connecting to the RMI registry, looking up the server, creating topics, and handling exceptions.
 * - **Error Handling**: Logs detailed error messages when exceptions occur, aiding in troubleshooting and debugging.
 *
 * Usage:
 * - Compile the class: `javac Proposer.java`
 * - Run the class with topics: `java Proposer --topic=Topic1 --topic=Topic2 ...`
 */

 import java.rmi.registry.LocateRegistry;
 import java.rmi.registry.Registry;
 import java.util.logging.Level;
 import java.util.logging.Logger;
 
 public class Proposer {
     private static final Logger logger = Logger.getLogger(Proposer.class.getName());
 
       /**
     * Main method to create instances of Proposer .
     * 
     * @param args Command-line arguments. Each argument is treated as a topic.
     */
     public static void main(String[] args) {
         // Check if there are any arguments
         if (args.length == 0) {
             logger.log(Level.SEVERE, "Usage: java Proposer --topic=<TopicName1> --topic=<TopicName2> ...");
             return;
         }
 
         try {
             // Connect to the RMI registry on localhost
             logger.log(Level.INFO, "Connecting to the RMI registry on localhost...");
             Registry registry = LocateRegistry.getRegistry("127.0.0.1");
 
             // Look up the ServerInterface from the registry
             logger.log(Level.INFO, "Looking up the ServerInterface...");
             ServerInterface server = (ServerInterface) registry.lookup("Server");
 
             // Process each argument to create topics
             for (String arg : args) {
                 if (arg.startsWith("--topic=")) {
                     // Extract topic name from argument
                     String topic = arg.substring("--topic=".length());
 
                     // Call the server's method to create the topic
                     server.createTopic(topic);
                     logger.log(Level.INFO, "Topic '{0}' created.", topic);
                     System.out.println("Topic '" + topic + "' created.");
                 }
             }
         } catch (Exception e) {
             logger.log(Level.SEVERE, "Exception occurred: {0}", e.toString());
             e.printStackTrace();
         }
     }
 }
 