import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Random;
import java.util.UUID;
import java.net.InetAddress;

/**
 * The Influencer class represents an influential agent in a distributed opinion retrieval system
 * using Java RMI (Remote Method Invocation). Each Influencer instance extends the Agent class
 * and participates in the system by connecting to a remote server to receive and potentially influence
 * opinions on various topics. Influencers are identified by a unique UUID and are associated with
 * a username, IP address (localhost), and a randomly assigned port.
 * 
 * Design Choices:
 * - Extends the Agent class to inherit common properties and functionalities related to agents,
 *   such as critical thinking status, user type, influencer status, and interaction with the server.
 * - Uses Java RMI (Remote Method Invocation) to communicate with the remote ServerInterface,
 *   enabling operations such as agent registration, opinion retrieval, and potential influence on
 *   consensus finding processes.
 * - Implements multi-threading with Java threads to concurrently handle opinion retrieval on
 *   randomly assigned ports, ensuring responsiveness and non-blocking behavior during execution.
 * - Utilizes InetAddress.getLoopbackAddress() to set the influencer's IP address to the localhost,
 *   ensuring that the influencer interacts with the server on the local machine.
 * - Generates a random port within a specified range (5000-6000) to enable multiple Influencer instances
 *   to run simultaneously without port conflicts, enhancing scalability and flexibility of the system.
 */

public class Influencer extends Agent {

    /**
     * Constructor to initialize Influencer extending from Agent.
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
    public Influencer(UUID id, String username, InetAddress ipAddress, int port, boolean isCriticalThinker,
            boolean isUser, boolean isInfluencer, ServerInterface server) throws RemoteException {
        super(id, username, ipAddress, port, isCriticalThinker, isUser, isInfluencer, server);
    }

    /**
     * Main method to create instances of Influencer agents.
     * 
     * @param args Command-line arguments. Each argument is treated as a username for an Influencer agent.
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java Influencer --<Username1> --<Username2> ...");
            return;
        }

        try {
            for (String arg : args) {
                if (arg.startsWith("--")) {
                    // Generate a unique ID and get localhost IP address
                    UUID id = UUID.randomUUID();
                    InetAddress ipAddress = InetAddress.getLoopbackAddress();

                    // Define port range for the agent
                    int startPort = 5000;
                    int endPort = 6000;
                    Random random = new Random(System.currentTimeMillis());
                    int randomPort = startPort + random.nextInt(endPort - startPort + 1);

                    // Extract username from argument
                    String username = arg.substring(2);

                    // Connect to the RMI registry
                    Registry registry = LocateRegistry.getRegistry();
                    ServerInterface server = (ServerInterface) registry.lookup("Server");

                    // Create instance of Influencer agent
                    Influencer influencer = new Influencer(id, username, ipAddress, randomPort, false, false, true,
                            server);

                    // Start a new thread for receiving opinions on the randomPort
                    new Thread(() -> {
                        influencer.receiveOpinions(randomPort);
                    }).start();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
