import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ServerImplement.java
 *
 * This class implements the ServerInterface and provides methods to manage agents and topics
 * in a distributed system. It utilizes Java RMI (Remote Method Invocation) to allow remote
 * communication and method invocation.
 *
 * Design Choices:
 * - **RMI Protocol**: Used to enable remote method invocation, allowing agents and proposers to
 *   interact with the server remotely.
 * - **Synchronization**: Methods that modify shared resources (e.g. `registerAgent`)
 *   are synchronized to ensure thread safety.
 * - **Logging**: Uses Java's built-in logging framework (`java.util.logging`) to log important events
 *   such as topic creation, agent registration, and exceptions. This aids in debugging and monitoring
 *   the system's behavior.
 * - **Data Structures**: Utilizes `HashMap` and `ArrayList` to store and manage agents and topics.
 *   The data structures are protected using synchronized methods to prevent concurrent modification issues.
 */

class ServerImplement implements ServerInterface {

    private static final Logger logger = Logger.getLogger(ServerImplement.class.getName());
    
    // Maps to store agents, their IP addresses and ports, and the list of topics
    private Map<UUID, AgentInterface> agents;
    private Map<AgentInterface, InetAddress> agentAddresses;
    private Map<AgentInterface, Integer> agentPort;
    private List<String> topics;

    // Default constructor
    protected ServerImplement() {
        agents = new HashMap<>();
        agentAddresses = new HashMap<>();
        agentPort = new HashMap<>();
        topics = new ArrayList<>();
    }

   /**
     * Creates a new topic and notifies all registered agents about it.
     *
     * @param topic The new topic to be created.
     */
    @Override
    public void createTopic(String topic) {
        logger.log(Level.INFO, "New topic created: {0}", topic);
        topics.add(topic);
        // Notify all agents about the new topic
        for (AgentInterface agent : agents.values()) {
            try {
                agent.notifyNewTopic(topic);
                logger.log(Level.INFO, "Notified agent about new topic: {0}", topic);
            } catch (RemoteException e) {
                logger.log(Level.SEVERE, "Failed to notify agent: {0}", e.toString());
                e.printStackTrace();
            }
        }
    }

    /**
     * Registers a new agent with the server, storing its details.
     *
     * @param id     The unique identifier for the agent.
     * @param agent  The agent interface object to be registered.
     * @param ip     The IP address of the agent.
     * @param port   The port number on which the agent is listening.
     */
    @Override
    public synchronized void registerAgent(UUID id, AgentInterface agent, InetAddress ip, int port) {
        agents.put(id, agent);
        agentAddresses.put(agent, ip);
        agentPort.put(agent, port);
        try {
            String username = agent.getUsername();
            logger.log(Level.INFO, "New Agent registered: {0} on port {1}", new Object[]{username, port});
        } catch (RemoteException e) {
            logger.log(Level.SEVERE, "Failed to get agent details: {0}", e.toString());
            e.printStackTrace();
        }
    }

    /**
     * Retrieves the IP address of the specified agent.
     *
     * @param agent The agent whose IP address is to be retrieved.
     * @return The IP address of the agent, or null if not found.
     */
    @Override
    public InetAddress receiveIp(AgentInterface agent) {
        return agentAddresses.get(agent); // Returns the IP address if present, else returns null
    }

    /**
     * Retrieves the port number of the specified agent.
     *
     * @param agent The agent whose port number is to be retrieved.
     * @return The port number of the agent, or null if not found.
     */
    @Override
    public int receivePort(AgentInterface agent) {
        return agentPort.get(agent); // Returns the port if present, else returns null
    }


     /**
     * Retrieves a random agent from the registered agents.
     *
     * @return A randomly selected agent, or null if no agents are registered.
     */
    @Override
    public AgentInterface getRandomAgent() {
        if (agents.isEmpty()) {
            logger.log(Level.WARNING, "No agents available to select");
            return null;
        } else {
            Random rand = new Random();
            List<UUID> keys = new ArrayList<>(agents.keySet());
            UUID randomKey = keys.get(rand.nextInt(keys.size()));
            logger.log(Level.INFO, "Random agent selected: {0}", randomKey);
            return agents.get(randomKey);
        }
    }

   /**
     * Retrieves a random topic from the available topics.
     *
     * @return A randomly selected topic, or null if no topics are available.
     */
    @Override
    public String getRandomTopic() {
        if (topics.isEmpty()) {
            logger.log(Level.WARNING, "No topics available to select");
            return null;
        } else {
            Random rand = new Random();
            int randomIndex = rand.nextInt(topics.size());
            logger.log(Level.INFO, "Random topic selected: {0}", topics.get(randomIndex));
            return topics.get(randomIndex);
        }
    }

     /**
     * Retrieves a list of all registered agents.
     *
     * @return A list containing all registered agents.
     */
    @Override
    public List<AgentInterface> getListOfAgent() {
        logger.log(Level.INFO, "Returning list of agents");
        return new ArrayList<>(agents.values());
    }

     /**
     * Retrieves two randomly selected agents from the registered agents.
     *
     * @return A list containing two randomly selected agents, or an empty list if fewer than two agents are registered.
     */
    @Override
    public List<AgentInterface> getTwoAgent() {
        if (agents.size() >= 2) {
            Random rand = new Random();
            List<UUID> keys = new ArrayList<>(agents.keySet());
            List<AgentInterface> selectedAgents = new ArrayList<>();
            
            UUID randomKey1 = keys.get(rand.nextInt(keys.size()));
            UUID randomKey2;
            do {
                randomKey2 = keys.get(rand.nextInt(keys.size()));
            } while (randomKey2.equals(randomKey1)); 
            
            selectedAgents.add(agents.get(randomKey1));
            selectedAgents.add(agents.get(randomKey2));
            
            logger.log(Level.INFO, "Two random agents selected: {0} and {1}", new Object[]{randomKey1, randomKey2});
            return selectedAgents;
        } else {
            logger.log(Level.WARNING, "Not enough agents available to select two");
            return new ArrayList<>(); // Returns an empty list if fewer than two agents
        }
    }
}

// Main server class
public class Server {
    private static final Logger logger = Logger.getLogger(Server.class.getName());

    public static void main(String[] args) {
        // Check if arguments are provided
        if (args.length == 0) {
            logger.log(Level.SEVERE, "Usage: java Server --port=<PortNumber>");
            return;
        }

        // Iterate through arguments to retrieve the port number
        for (String arg : args) {
            if (arg.startsWith("--port=")) {
                try {
                    // Get the port number from the argument
                    int port = Integer.parseInt(arg.substring("--port=".length()));
                    
                    // Create an instance of the server implementation
                    ServerImplement server = new ServerImplement();
                    
                    // Export the server object as a remote object with the specified port
                    ServerInterface stub = (ServerInterface) UnicastRemoteObject.exportObject(server, port);
                    
                    // Get the local RMI registry
                    Registry registry = LocateRegistry.getRegistry();
                    
                    // Bind the server interface stub to the registry with the name "Server"
                    registry.bind("Server", stub);
                    
                    logger.log(Level.INFO, "Server is ready on port {0}", port);
                } catch (NumberFormatException e) {
                    logger.log(Level.SEVERE, "Invalid port number format: {0}", arg);
                    return;
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Server exception: {0}", e.toString());
                    e.printStackTrace();
                }
            }
        }
    }
}
