import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.net.*;
import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Agent class represents an abstract agent in a distributed opinion retrieval system
 * using Java RMI (Remote Method Invocation). Agents can be users, influencers, or critical
 * thinkers, identified by a unique UUID and associated with a username, IP address, and port
 * for communication. This class implements the AgentInterface for remote method invocation,
 * providing methods to manage opinions, degrees of influence, and interaction with the server.
 * 
 * Design Choices:
 * - Extends UnicastRemoteObject to make instances of this class remotely accessible via Java RMI,
 *   enabling communication with other agents and the server using remote method invocation.
 * - Implements the AgentInterface, defining remote methods for fetching and updating opinions,
 *   interacting with other agents, and participating in consensus calls and critical thinking processes.
 * - Utilizes logging with java.util.logging to record agent activities and communication events,
 *   ensuring traceability and debugging capabilities for remote method invocation operations.
 * - Uses ExecutorService and multi-threading for concurrent handling of opinion notifications and
 *   consensus processes, improving system responsiveness and scalability under multiple agent interactions.
 * - Manages synchronized access to shared data structures like opinions and degreesOfInfluence
 *   to maintain data consistency and integrity during concurrent read and write operations.
 */

public class Agent extends UnicastRemoteObject implements AgentInterface {
    private static final Logger logger = Logger.getLogger(Agent.class.getName());

    private String username;
    private UUID id;
    private InetAddress ipAddress;
    private int port;
    private Map<UUID, Double> degreeOfInfluence;
    private Map<String, Double> opinions;
    private boolean isCriticalThinker;
    private boolean isUser;
    private boolean isInfluencer;
    private ServerInterface server;

    public Agent(UUID id, String username, InetAddress ipAddress, int port,
                 boolean isCriticalThinker, boolean isUser, boolean isInfluencer,
                 ServerInterface server) throws RemoteException {
        this.id = id;
        this.username = username;
        this.ipAddress = ipAddress;
        this.port = port;
        this.isCriticalThinker = isCriticalThinker;
        this.isUser = isUser;
        this.isInfluencer = isInfluencer;
        this.degreeOfInfluence = new HashMap<>();
        this.opinions = new HashMap<>();
        this.server = server;
        server.registerAgent(id, this, ipAddress, port);

        logger.log(Level.INFO, "Agent created: {0}, ID: {1}, IP: {2}, Port: {3}",
                new Object[]{username, id, ipAddress, port});
    }

/* Setters and getters */

    @Override
    public UUID getId() throws RemoteException {
        return id;
    }

    @Override
    public String getUsername() throws RemoteException {
        return username;
    }

    @Override
    public InetAddress getIpAddress() throws RemoteException {
        return ipAddress;
    }

    @Override
    public int getPort() throws RemoteException {
        return port;
    }

    @Override
    public Map<String, Double> getOpinions() throws RemoteException {
        return opinions;
    }

    @Override
    public double getOpinionValue(String topic) throws RemoteException {
        return opinions.getOrDefault(topic, 0.0);
    }

    @Override
    public double getInfluenceWithAgent(UUID agentId) throws RemoteException {
        return degreeOfInfluence.getOrDefault(agentId, 0.0);
    }

    @Override
    public void setInfluenceWithAgent(UUID agentId, double influence) throws RemoteException {
        degreeOfInfluence.put(agentId, influence);
    }

    @Override
    public void addOpinion(String topic, double opinion) throws RemoteException {
        opinions.put(topic, opinion);
    }

    @Override
    public void setOpinion(String topic, double opinion) throws RemoteException {
        opinions.put(topic, opinion);
    }

    /**
     * Notifies the agent of a new topic and initiates the process of sharing opinions.
     * If the agent is an influencer, it sends its opinion on the new topic to all other agents.
     * Otherwise, it sends its opinion to a randomly selected agent.
     *
     * @param topic The new topic to notify the agent about.
     * @throws RemoteException If a remote communication error occurs.
     */

    @Override
    public void notifyNewTopic(String topic) throws RemoteException {
        // Log the reception of a new topic
        logger.log(Level.INFO, "{0} received new topic: {1}", new Object[]{username, topic});
    
        // Create a thread pool with a fixed number of threads to handle concurrent tasks
        ExecutorService executor = Executors.newFixedThreadPool(10);
    
        try {
            // Check if the agent is an influencer
            if (isInfluencer) {
                // Get the list of all agents from the server
                List<AgentInterface> listAgent = server.getListOfAgent();
    
                // Filter out the current agent from the list and send opinions to the rest
                listAgent.stream()
                    .filter(receiver -> {
                        try {
                            return !receiver.getId().equals(this.getId());
                        } catch (RemoteException e) {
                            logger.log(Level.SEVERE, "Error checking receiver ID", e);
                            return false;
                        }
                    })
                    .forEach(receiver -> {
                        try {
                            // Obtenir l'adresse IP et le port de l'agent récepteur
                            InetAddress receiverIp = server.receiveIp(receiver);
                            int receiverPort = server.receivePort(receiver);
    
                            // Generate a random opinion
                            Double senderOpinion = Math.random();
    
                            // Submit a task to the executor to send the opinion to the receiver agent
                            executor.submit(() -> {
                                try {
                                    sendOpinion(this, receiver, receiverIp, receiverPort, senderOpinion, topic);
                                } catch (RemoteException e) {
                                    logger.log(Level.SEVERE, "Error sending opinion", e);
                                }
                            });
                        } catch (RemoteException e) {
                            logger.log(Level.SEVERE, "Error in notifyNewTopic", e);
                        }
                    });
            } else {
                // If the agent is not an influencer, get a random agent from the server
                AgentInterface receiver = server.getRandomAgent();
    
                // Ensure the selected agent is not the current agent
                while (receiver != null && receiver.getId().equals(this.getId())) {
                    logger.log(Level.INFO, "Selected agent is self, retrying...");
                    receiver = server.getRandomAgent();
                }
    
                // If a valid receiver agent is found, send the opinion
                if (receiver != null) {
                    final AgentInterface finalReceiver = receiver;
                    InetAddress receiverIp = server.receiveIp(finalReceiver);
                    int receiverPort = server.receivePort(finalReceiver);
                    Double senderOpinion = Math.random();
    
                    // Submit a task to the executor to send the opinion to the receiver agent
                    executor.submit(() -> {
                        try {
                            sendOpinion(this, finalReceiver, receiverIp, receiverPort, senderOpinion, topic);
                        } catch (RemoteException e) {
                            logger.log(Level.SEVERE, "Error sending opinion", e);
                        }
                    });
                } else {
                    // Log a warning if no suitable agent is found
                    logger.log(Level.WARNING, "No suitable agent found to send opinion.");
                }
            }
        } catch (RemoteException e) {
            logger.log(Level.SEVERE, "Error in notifyNewTopic", e);
        } finally {
            // Shut down the executor service gracefully
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    

    /**
     * Sends an opinion from the sender agent to the receiver agent about a specific topic.
     * If the receiver is a critical thinker, additional evidence is provided to support the opinion.
     *
     * @param sender   The agent sending the opinion.
     * @param receiver The agent receiving the opinion.
     * @param ip       The IP address of the receiver agent.
     * @param port     The port number of the receiver agent.
     * @param opinion  The opinion value to be sent.
     * @param topic    The topic of the opinion.
     * @throws RemoteException If a remote communication error occurs.
     */
    protected void sendOpinion(AgentInterface sender, AgentInterface receiver, InetAddress ip, int port,
        Double opinion, String topic) throws RemoteException {
        // Log the intention to send an opinion
        logger.log(Level.INFO, "Agent {0} sending opinion {1} about topic {2} to Agent {3}",
        new Object[]{sender.getUsername(), opinion, topic, receiver.getUsername()});

        // Try to establish a socket connection to the receiver agent
        try (Socket socket = new Socket(ip, port);
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            // Send initial message and opinion details to the receiver agent
            out.writeObject("it's agent");
            int areYouCritical = 0; // Initially assume the receiver is not a critical thinker
            out.writeObject(sender);
            out.writeObject(topic);
            out.writeObject(opinion);
            out.writeObject(areYouCritical);

            // Verify if the receiver is a critical thinker
            try {
                int critical = (Integer) in.readObject();
                if (critical == 1) {
                    // If the receiver is a critical thinker, send additional evidence
                    logger.log(Level.INFO, "Receiver {0} is a critical thinker", receiver.getUsername());
                    Random random = new Random();
                    int evidence = random.nextInt(1000) + 1; // Generate random evidence
                    out.writeObject(evidence);
                    logger.log(Level.INFO, "Evidence sent: {0}", evidence);
                    String update = (String) in.readObject(); // Receive response from the CT
                    logger.log(Level.INFO, "Response from critical thinker: {0}", update);
                }
            } catch (ClassNotFoundException e) {
            // Log any errors that occur during the reading of objects
                logger.log(Level.SEVERE, "Class not found during opinion sending", e);
            }
            } catch (IOException e) {
            // Log any I/O errors that occur during the communication
                logger.log(Level.SEVERE, "IO Exception during opinion sending", e);
        }
    }

    /**
     * Listens for incoming connections on the specified port and processes opinions
     * from other agents, consensus calls, and polarimeter requests.
     *
     * @param listenPort The port on which the agent listens for incoming connections.
     */
    protected void receiveOpinions(int listenPort) {
        // Open a server socket to listen on the specified port
        try (ServerSocket serverSocket = new ServerSocket(listenPort)) {
            // Continuously accept and process incoming connections
            while (true) {
                try (Socket clientSocket = serverSocket.accept();
                    ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
                    ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream())) {

                    // Read the identity of the incoming message
                    String identity = (String) in.readObject();

                    if ("it's agent".equals(identity)) {
                        // Process opinion sent from another agent
                        Agent sender = (Agent) in.readObject();
                        String topic = (String) in.readObject();
                        double opinion = (double) in.readObject();
                        int areYouCritical = (Integer) in.readObject();

                        if (isCriticalThinker) {
                            // If the receiver is a critical thinker, expect evidence
                            out.writeObject(1);
                            int evidence = (Integer) in.readObject();
                            logger.log(Level.INFO, "Received evidence: {0}", evidence);

                            if (evidence % 7 == 0) {
                                // Validate the evidence and update opinion if valid
                                logger.log(Level.INFO, "Evidence is valid");
                                processOpinion(sender, topic, opinion);
                                out.writeObject("Opinion updated");
                            } else {
                                // Log invalid evidence
                                logger.log(Level.WARNING, "Evidence is invalid");
                                out.writeObject("Cannot update opinion");
                            }
                        } else {
                            // Update opinion without evidence if not a critical thinker
                            out.writeObject(0);
                            processOpinion(sender, topic, opinion);
                        }
                    } else if ("it's consensus".equals(identity)) {
                        // Process a consensus call
                        logger.log(Level.INFO, "Received consensus call");
                        String topic = (String) in.readObject();
                        boolean accept = new Random().nextBoolean();

                        if (isCriticalThinker) {
                            // Critical thinkers reject consensus calls
                            accept = false;
                            logger.log(Level.INFO, "Critical thinker rejected consensus call");
                        }

                        logger.log(Level.INFO, "Consensus call {0}", accept ? "accepted" : "rejected");
                        out.writeObject(accept);

                        try {
                            if (accept) {
                                // Fetch and update opinions based on consensus call
                                String requestType = (String) in.readObject();
                                if ("fetch opinion".equals(requestType)) {
                                    double opinion = getOpinionValue(topic);
                                    out.writeObject(opinion);
                                    double newOpinion = (double) in.readObject();
                                    setOpinion(topic, newOpinion);
                                    logger.log(Level.INFO, "Opinion updated from consensus call: was {0}, now {1}",
                                            new Object[]{opinion, newOpinion});
                                }
                            }
                        } catch (ClassNotFoundException e) {
                            // Log errors during consensus call
                            logger.log(Level.SEVERE, "Class not found during consensus call", e);
                        }
                    } else {
                        // Process a polarimeter request
                        logger.log(Level.INFO, "Received Polarimeter request");
                        try {
                            String topicName = (String) in.readObject();
                            double value = getOpinionValue(topicName);
                            out.writeObject(value);
                            logger.log(Level.INFO, "Opinion value sent to polarimeter");
                        } catch (ClassNotFoundException e) {
                            // Log errors during polarimeter request
                            logger.log(Level.SEVERE, "Class not found during polarimeter request", e);
                        }
                    }
                } catch (ClassNotFoundException e) {
                    // Log errors during the reception of opinions
                    logger.log(Level.SEVERE, "Class not found during opinion reception", e);
                }
            }
        } catch (IOException e) {
            // Log I/O errors during the reception of opinions
            logger.log(Level.SEVERE, "IO Exception during opinion reception", e);
        }
    }

   /**
 * Processes an opinion received from another agent on a specified topic.
 * Updates the influence of the sender and recalculates the opinion on the topic.
 *
 * @param sender  The agent sending the opinion.
 * @param topic   The topic of the opinion.
 * @param opinion The opinion value received from the sender.
 */
private void processOpinion(AgentInterface sender, String topic, double opinion) {
    try {
        // Log the received opinion
        logger.log(Level.INFO, "Received opinion {0} from {1} on topic {2}",
                new Object[]{opinion, sender.getUsername(), topic});

        // Get the current influence of the sender
        double influence = getInfluenceWithAgent(sender.getId());
        
        if (influence == 0.0) {
            // If it's the first communication, initialize the influence
            influence = Math.random() * 0.1 + 0.1;
            setInfluenceWithAgent(sender.getId(), influence);
            logger.log(Level.INFO, "First communication, influence set to: {0}", influence);
        } else {
            // Increment the influence by 0.1, ensuring it does not exceed 1.0
            influence += 0.1;
            if (influence > 1.0) {
                influence = 1.0;
            }
            setInfluenceWithAgent(sender.getId(), influence);
        }

        // Get the current opinion on the topic, defaulting to 0.3 if not set
        double currentOpinion = opinions.getOrDefault(topic, 0.3);
        
        // Recalculate the new opinion based on the influence
        double newOpinion = currentOpinion * (1 - influence) + opinion * influence;
        setOpinion(topic, newOpinion);

        // Log the updated opinion
        logger.log(Level.INFO, "Opinion updated for topic {0}: was {1}, now {2}",
                new Object[]{topic, currentOpinion, newOpinion});
    } catch (RemoteException e) {
        // Log any RemoteException that occurs
        logger.log(Level.SEVERE, "Error during processOpinion", e);
    }
}

}
