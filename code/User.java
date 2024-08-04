/**
 * The User class represents a user participating in a distributed opinion retrieval system
 * using Java RMI (Remote Method Invocation). Each User instance acts as a client that can
 * connect to a remote server to retrieve opinions on specific topics. Users are identified
 * by a unique UUID and are associated with a username, IP address, and a randomly assigned port.
 * 
 * Design Choices:
 * - Extends the Agent class to inherit common properties and functionalities related to agents,
 *   such as critical thinking status, user type, influencer status, and interaction with the server.
 * - Uses Java RMI (Remote Method Invocation) to communicate with the remote ServerInterface,
 *   allowing the User to register itself, receive opinions on topics, and potentially influence
 *   consensus finding processes.
 * - Implements multi-threading with Java threads to concurrently handle opinion retrieval on
 *   randomly assigned ports, ensuring responsiveness and non-blocking behavior during execution.
 * - Utilizes InetAddress.getLoopbackAddress() to set the user's IP address to the localhost,
 *   ensuring that the user interacts with the server on the local machine.
 * - Generates a random port within a specified range (5000-6000) to enable multiple User instances
 *   to run simultaneously without port conflicts, enhancing scalability and flexibility.
 */


import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Random;
import java.util.UUID;
import java.net.InetAddress;

// User class extending Agent
public class User extends Agent {

     /**
     * Constructor for User class extending from Agent.
     * 
     * @param id               UUID of the agent.
     * @param username         Username of the agent.
     * @param ipAddress        IP Address of the agent.
     * @param port             Port number for communication.
     * @param isCriticalThinker Whether the agent is a critical thinker.
     * @param isUser           Whether the agent is a user.
     * @param isInfluencer     Whether the agent is an influencer.
     * @param server           Reference to the server interface.
     * @throws RemoteException If a remote communication error occurs.
     */

    public User(UUID id, String username, InetAddress ipAddress, int port, boolean isCriticalThinker, boolean isUser, boolean isInfluencer, ServerInterface server) throws RemoteException {
        super(id, username, ipAddress, port, isCriticalThinker, isUser, isInfluencer, server);
    }

     /**
     * Main method to create instances of Influencer agents.
     * 
     * @param args Command-line arguments. Each argument is treated as a username for an Influencer agent.
     */
    public static void main(String[] args) {
        // Check if arguments are provided
        if (args.length < 1) {
            System.out.println("Usage: java User --<Username1> --<Username2> ...");
            return;
        }
        
        try {
            // Iterate through command line arguments
            for (String arg : args) {
                if (arg.startsWith("--")) {
                    // Generate a unique UUID for the user
                    UUID id = UUID.randomUUID();
                    // Use loopback address for localhost
                    InetAddress ipAddress = InetAddress.getLoopbackAddress();
                    // Define the range of ports
                    int startPort = 5000;
                    int endPort = 6000;
                    // Generate a random port within the defined range
                    Random random = new Random(System.currentTimeMillis());
                    int randomPort = startPort + random.nextInt(endPort - startPort + 1);
                    // Extract username from the command line argument
                    String username = arg.substring(2);

                    // Get the RMI registry
                    Registry registry = LocateRegistry.getRegistry();
                    // Look up the remote ServerInterface
                    ServerInterface server = (ServerInterface) registry.lookup("Server");

                    // Create a new User instance
                    User user = new User(id, username, ipAddress, randomPort, false, true, false, server);

                    // Start a new thread to receive opinions on the random port
                    new Thread(() -> {
                        user.receiveOpinions(randomPort);
                    }).start();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
