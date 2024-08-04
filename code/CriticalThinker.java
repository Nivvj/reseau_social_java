import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Random;
import java.util.UUID;
import java.net.InetAddress;

/**
 * The CriticalThinker class represents an agent in a distributed opinion retrieval system
 * using Java RMI (Remote Method Invocation), specializing in critical thinking. Each CriticalThinker
 * instance extends the Agent class and interacts with a remote server to receive and critically
 * evaluate opinions on various topics. Critical thinkers are identified by a unique UUID and are
 * associated with a username, IP address (localhost), and a randomly assigned port.
 * 
 * Design Choices:
 * - Extends the Agent class to inherit common properties and functionalities related to agents,
 *   such as critical thinking status, user type, influencer status, and interaction with the server.
 * - Utilizes Java RMI (Remote Method Invocation) to communicate with the remote ServerInterface,
 *   enabling operations such as agent registration, opinion retrieval, and critical evaluation
 *   of opinions during consensus finding processes.
 * - Implements multi-threading with Java threads to concurrently handle opinion retrieval on
 *   randomly assigned ports, ensuring responsiveness and non-blocking behavior during execution.
 * - Utilizes InetAddress.getLoopbackAddress() to set the critical thinker's IP address to the
 *   localhost, ensuring that the agent interacts with the server on the local machine.
 * - Generates a random port within a specified range (5000-6000) to enable multiple CriticalThinker
 *   instances to run simultaneously without port conflicts, enhancing scalability and flexibility
 *   of the system.
 */

// CriticalThinker class extending Agent
public class CriticalThinker extends Agent {

     /**
     * Constructor to initialize CT extending from Agent.
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
    public CriticalThinker(UUID id, String username, InetAddress ipAddress, int port, boolean isCriticalThinker, boolean isUser, boolean isInfluencer, ServerInterface server) throws RemoteException {
        super(id, username, ipAddress, port, isCriticalThinker, isUser, isInfluencer, server);
    }

     /**
     * Main method for creating CriticalThinker instances
     * 
     * @param args Command-line arguments. Each argument is treated as a username for a CT agent.
     */
    public static void main(String[] args) {
        // Check if arguments are provided
        if (args.length < 1) {
            System.out.println("Usage: java CriticalThinker --<Username1> --<Username2> ...");
            return;
        }
        
        try {
            // Iterate through command line arguments
            for (String arg : args) {
                if (arg.startsWith("--")) {
                    // Generate a unique UUID for the critical thinker
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

                    // Create a new CriticalThinker instance
                    CriticalThinker criticalThinker = new CriticalThinker(id, username, ipAddress, randomPort, true, false, false, server);

                    // Start a new thread to receive opinions on the random port
                    new Thread(() -> {
                        criticalThinker.receiveOpinions(randomPort);
                    }).start();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
