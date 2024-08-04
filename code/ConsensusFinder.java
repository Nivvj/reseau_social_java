
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The ConsensusFinder class facilitates the consensus process between two agents
 * on a specified topic using Java sockets for direct communication. It interacts
 * with a remote server via RMI (Remote Method Invocation) to retrieve agent details
 * and topic information. This class employs socket-based communication to send
 * consensus requests, fetch opinions, and update agents with a new consensus opinion.
 * 
 * Design Choices:
 * - Utilizes Java's logging framework (java.util.logging.Logger) to log events
 *   and errors throughout the consensus process for visibility and debugging.
 * - Implements network communication via Java sockets (java.net.Socket) to establish
 *   connections with agents and exchange consensus-related information.
 * - Employs ObjectOutputStream and ObjectInputStream to send and receive serialized
 *   objects (including consensus requests, topic information, and opinion values)
 *   between the ConsensusFinder instance and agents.
 * - Handles exceptions such as IOException, ClassNotFoundException, and SocketException
 *   to ensure robust error handling and recovery during network operations.
 * - Starts the consensus process in a new thread to avoid blocking the main application
 *   thread, using a lambda expression for concise thread initialization.
 *
 * Implementation Details:
 * - ConsensusFinder extends UnicastRemoteObject to enable RMI communication for remote
 *   method invocations, ensuring seamless interaction with the server.
 * - Communication with agents is established using socket-based streams, ensuring efficient
 *   and reliable data exchange between the ConsensusFinder instance and each agent involved.
 * - Methods such as requestConsensus, fetchOpinion, and updateOpinion handle specific phases
 *   of the consensus process by sending requests, receiving responses, and updating opinions
 *   respectively, while logging events using the configured Logger instance.
 * - The main method initializes the consensus process by obtaining server details, selecting
 *   two agents randomly, retrieving a topic from the server, and launching the consensus process
 *   in a new thread, which continues until consensus is reached.
 */

public class ConsensusFinder extends UnicastRemoteObject {
    private static final Logger logger = Logger.getLogger(ConsensusFinder.class.getName());
    private final ServerInterface server;

    public ConsensusFinder(ServerInterface server, InetAddress ipAddress, int port) throws RemoteException {
        this.server = server;
    }

    /**
     * Initiates the consensus process between two agents on a specified topic.
     * 
     * @param agent1 First agent involved in the consensus.
     * @param agent2 Second agent involved in the consensus.
     * @param topic  The topic on which consensus is sought.
     * @throws RemoteException If a remote communication error occurs.
     */
    public void findConsensus(AgentInterface agent1, AgentInterface agent2, String topic) throws RemoteException {
        logger.log(Level.INFO, "Trying to reach consensus on topic: {0}", topic);
        logger.log(Level.INFO, "Agents involved: {0} and {1}", new Object[]{agent1.getUsername(), agent2.getUsername()});
    
        boolean consensusReached = false;
    
        while (!consensusReached) {
            try (Socket socket1 = new Socket(agent1.getIpAddress(), agent1.getPort());
                 Socket socket2 = new Socket(agent2.getIpAddress(), agent2.getPort());
                 ObjectOutputStream out1 = new ObjectOutputStream(socket1.getOutputStream());
                 ObjectInputStream in1 = new ObjectInputStream(socket1.getInputStream());
                 ObjectOutputStream out2 = new ObjectOutputStream(socket2.getOutputStream());
                 ObjectInputStream in2 = new ObjectInputStream(socket2.getInputStream())) {
    
                // Request consensus from both agents
                boolean response1 = requestConsensus(agent1, topic, out1, in1);
                boolean response2 = requestConsensus(agent2, topic, out2, in2);
    
                if (response1 && response2) {
                    logger.log(Level.INFO, "Both agents accepted the consensus call.");
    
                    // Fetch opinions from both agents
                    double opinion1 = fetchOpinion(agent1, topic, out1, in1);
                    double opinion2 = fetchOpinion(agent2, topic, out2, in2);
    
                    // Calculate the new consensus opinion as the average of both opinions
                    double newOpinion = (opinion1 + opinion2) / 2;
                    logger.log(Level.INFO, "New consensus opinion on {0}: {1}", new Object[]{topic, newOpinion});
    
                    // Update both agents with the new consensus opinion
                    updateOpinion(agent1, topic, newOpinion, out1);
                    updateOpinion(agent2, topic, newOpinion, out2);
    
                    consensusReached = true; // Set to true to exit the loop
                } else {
                    logger.log(Level.WARNING, "Consensus call was not accepted by both agents.");
    
                    // Inform the agents who accepted the consensus call
                    if (response1) {
                        informAgent(agent1, true, out1);
                    } else if (response2) {
                        informAgent(agent2, true, out2);
                    }
    
                    // Sleep for 5 seconds before trying again
                    Thread.sleep(5000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.log(Level.SEVERE, "Thread interrupted while waiting", e);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error during consensus process", e);
            }
        }
    
        logger.log(Level.INFO, "Consensus process completed.");
        System.exit(0);
    }
    /**
     * Informs an agent about the consensus call acceptance or rejection.
     * 
     * @param agent    The agent to inform.
     * @param response Whether the consensus call was accepted or rejected.
     * @param out      The ObjectOutputStream for sending the response.
     */
    private void informAgent(AgentInterface agent, boolean response, ObjectOutputStream out) {
        try {
            out.writeObject(response ? "Consensus call accepted" : "Consensus call rejected");
            logger.log(Level.INFO, "Informed agent {0} about consensus call: {1}", 
                new Object[]{agent.getUsername(), response ? "Accepted" : "Rejected"});
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error informing agent about consensus call", e);
        }
    }

    /**
     * Sends a consensus request to an agent and retrieves its response.
     * 
     * @param agent The agent to send the consensus request to.
     * @param topic The topic for which consensus is requested.
     * @param out   The ObjectOutputStream for sending the request.
     * @param in    The ObjectInputStream for receiving the response.
     * @return True if the agent accepts the consensus call, false otherwise.
     */
    private boolean requestConsensus(AgentInterface agent, String topic, ObjectOutputStream out, ObjectInputStream in) {
        try {
            out.writeObject("it's consensus");
            out.writeObject(topic);
            boolean response = (boolean) in.readObject();
            logger.log(Level.INFO, "{0} response: {1}", 
                new Object[]{agent.getUsername(), response ? "Accepted" : "Rejected"});
            return response;
        } catch (SocketException e) {
            logger.log(Level.SEVERE, "SocketException: Connection reset. Check server status and network settings.", e);
            return false;
        } catch (IOException | ClassNotFoundException e) {
            logger.log(Level.SEVERE, "Error requesting consensus from agent", e);
            return false;
        }
    }

    /**
     * Fetches the opinion of an agent on a specified topic.
     * 
     * @param agent The agent whose opinion is to be fetched.
     * @param topic The topic for which the opinion is requested.
     * @param out   The ObjectOutputStream for sending the request.
     * @param in    The ObjectInputStream for receiving the opinion.
     * @return The opinion value as a double, or -1 if there's an error.
     */
    private double fetchOpinion(AgentInterface agent, String topic, ObjectOutputStream out, ObjectInputStream in) {
        try {
            out.writeObject("fetch opinion");
            double opinion = (double) in.readObject();
            logger.log(Level.INFO, "Fetched opinion from {0} on topic {1}: {2}", 
                new Object[]{agent.getUsername(), topic, opinion});
            return opinion;
        } catch (SocketException e) {
            logger.log(Level.SEVERE, "SocketException: Connection reset. Check server status and network settings.", e);
            return -1;
        } catch (IOException | ClassNotFoundException e) {
            logger.log(Level.SEVERE, "Error fetching opinion from agent", e);
            return -1;
        }
    }

    /**
     * Updates the opinion of an agent on a specified topic with a new opinion value.
     * 
     * @param agent     The agent whose opinion is to be updated.
     * @param topic     The topic for which the opinion is updated.
     * @param newOpinion The new opinion value to set.
     * @param out       The ObjectOutputStream for sending the new opinion.
     */
    private void updateOpinion(AgentInterface agent, String topic, double newOpinion, ObjectOutputStream out) {
        try {
            out.writeObject(newOpinion);
            logger.log(Level.INFO, "Updated opinion of {0} on topic {1} to {2}", 
                new Object[]{agent.getUsername(), topic, newOpinion});
        } catch (SocketException e) {
            logger.log(Level.SEVERE, "SocketException: Connection reset. Check server status and network settings.", e);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error updating opinion of agent", e);
        }
    }

    /**
     * Main method to start the consensus process between two randomly selected agents on a random topic.
     * 
     * @param args Command-line arguments (not used).
     */
    public static void main(String[] args) {
        try {
            // Generate a unique ID for this process
            UUID id = UUID.randomUUID();
            InetAddress ipAddress = InetAddress.getLoopbackAddress();
            int startPort = 5000;
            int endPort = 6000;
            Random random = new Random(System.currentTimeMillis());
            int randomPort = startPort + random.nextInt(endPort - startPort + 1);

            // Connect to the RMI registry
            Registry registry = LocateRegistry.getRegistry();
            ServerInterface server = (ServerInterface) registry.lookup("Server");

            // Create an instance of ConsensusFinder
            ConsensusFinder consensus = new ConsensusFinder(server, ipAddress, randomPort);

            // Get two agents randomly from the server
            List<AgentInterface> twoAgents = server.getTwoAgent();
            AgentInterface A = twoAgents.get(0);
            AgentInterface B = twoAgents.get(1);

            // Initialize topic and ensure it is not null
            String topic = null;
            while (topic == null) {
                logger.log(Level.INFO, "There is no topic, waiting for one...");
                topic = server.getRandomTopic();
            }

            // Use a lambda expression to start a new thread for the consensus process
            String finalTopic = topic; // Final variable for use in lambda expression
            new Thread(() -> {
                try {
                    consensus.findConsensus(A, B, finalTopic);
                } catch (RemoteException e) {
                    logger.log(Level.SEVERE, "Error during consensus process", e);
                }
            }).start();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in main method", e);
        }
    }
}
