import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Polarimeter class for measuring polarization based on agent opinions retrieved via socket communication.
 *
 * This class utilizes a scheduled executor service for periodic measurement of polarization for a specified topic.
 * Communication with agents (representing opinions) is established using socket connections. The current topic
 * for polarization measurement is updated periodically from a remote server using RMI (Remote Method Invocation).
 *
 * Design choices:
 * - ScheduledExecutorService: Used for scheduling the periodic tasks of measuring polarization and updating the topic.
 * - Synchronized Topic Handling: The topic variable is marked as volatile and accessed within synchronized blocks to
 *   ensure thread safety when updating and retrieving the current topic.
 * - Socket Communication: Agents' opinions are obtained via socket connections. Timeout handling (set with a configurable
 *   delay) ensures timely retrieval of opinions, contributing to accurate polarization measurement.
 * - RMI (Remote Method Invocation): The Polarimeter communicates with a remote server to fetch a random topic for
 *   polarization measurement. This interaction is crucial for ensuring the Polarimeter operates with up-to-date and
 *   relevant topics.
 *
 * Usage:
 * - The Polarimeter can be started with an optional delay parameter (--delay=<milliseconds>) to customize the interval
 *   between successive polarization measurements and topic updates.
 *
 * Dependencies:
 * - ServerInterface: Defines methods for fetching agent lists and random topics from the remote server.
 * - AgentInterface: Represents agents whose opinions contribute to polarization measurement.
 *
 */
public class Polarimeter {

    private static final Logger logger = Logger.getLogger(Polarimeter.class.getName());
    private final ScheduledExecutorService scheduler;
    private final ServerInterface server;
    private final InetAddress ipAddress;
    private final int port;
    private boolean canPolarize = true;
    private static int delay;
    private volatile String topic;

    /**
     * Constructor to initialize the Polarimeter.
     *
     * @param server    Reference to the server interface.
     * @param ipAddress IP Address of the Polarimeter.
     * @param port      Port number for communication.
     */
    public Polarimeter(ServerInterface server, InetAddress ipAddress, int port) {
        this.server = server;
        this.ipAddress = ipAddress;
        this.port = port;
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    /**
     * Method to compute and print polarization for the current topic.
     */
    public void measurePolarization() {
        try {
            String currentTopic;
            synchronized (this) {
                currentTopic = topic;
            }
            if (currentTopic == null) {
                logger.log(Level.WARNING, "No topic available from server. Waiting for a new topic...");
                updateTopic(); // Try to get a new topic
                return;
            }

            logger.log(Level.INFO, "Measuring polarization for topic: {0}", currentTopic);

            // Retrieve opinions from all agents for the specified topic
            List<AgentInterface> agents = server.getListOfAgent();
            List<Double> opinions = agents.parallelStream()
                    .map(agent -> {
                        try (
                                // Open a socket connection to the agent
                                Socket socket = new Socket(agent.getIpAddress(), agent.getPort());
                                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                                ObjectInputStream in = new ObjectInputStream(socket.getInputStream())
                        ) {
                            // Send request to agent
                            out.writeObject("it's polarimeter");
                            out.writeObject(currentTopic);

                            // Set socket timeout for reading agent's response
                            socket.setSoTimeout(delay); // Timeout of delay seconds

                            // Receive response from agent
                            double value = (double) in.readObject();
                            return value;
                        } catch (SocketTimeoutException e) {
                            logger.log(Level.WARNING, "Timeout communicating with agent");
                            canPolarize = false; // Set flag to false if timeout occurs
                            return null;
                        } catch (IOException | ClassNotFoundException e) {
                            logger.log(Level.WARNING, "Error communicating with agent");
                            canPolarize = false; // Set flag to false if any other exception occurs
                            return null;
                        }
                    })
                    .filter(Objects::nonNull) // Filter out null values (failed responses)
                    .collect(Collectors.toList());

            if (!canPolarize) {
                logger.log(Level.WARNING, "Can't polarize because not all agents sent opinion on time.");
                return;
            }

            // Calculate polarization based on the retrieved opinions
            double polarization = computePolarization(opinions);
            logger.log(Level.INFO, "Polarization for topic {0}: {1}", new Object[]{currentTopic, polarization});
        } catch (RemoteException e) {
            logger.log(Level.SEVERE, "Error during polarization measurement", e);
        }
    }

    /**
     * Method to update the current topic from the server.
     */
    private void updateTopic() {
        try {
            String newTopic = server.getRandomTopic();
            while (newTopic == null) {
                logger.log(Level.INFO, "No topic available from server. Waiting for a new topic...");
                Thread.sleep(5000); // Wait for 5 seconds before trying again
                newTopic = server.getRandomTopic();
            }
            synchronized (this) {
                topic = newTopic;
            }
            logger.log(Level.INFO, "New topic received: {0}", newTopic);
        } catch (InterruptedException | RemoteException e) {
            Thread.currentThread().interrupt();
            logger.log(Level.SEVERE, "Error updating topic from server", e);
        }
    }

    /**
     * Method to compute polarization based on a list of opinions.
     *
     * @param opinions List of opinions for which polarization is to be computed.
     * @return Calculated polarization value.
     */
    private double computePolarization(List<Double> opinions) {
        int numIntervals = 10;
        double minOpinion = 0.0;
        double maxOpinion = 1.0;
        double intervalSize = (maxOpinion - minOpinion) / numIntervals;

        // Arrays to hold histogram and midpoints
        int[] histogram = new int[numIntervals];
        double[] midpoints = new double[numIntervals];

        // Calculate midpoints of each interval
        for (int i = 0; i < numIntervals; i++) {
            midpoints[i] = minOpinion + (i + 0.5) * intervalSize;
        }

        // Populate histogram based on opinions
        for (double opinion : opinions) {
            int index = (int) ((opinion - minOpinion) / intervalSize);
            if (index >= numIntervals)
                index = numIntervals - 1;
            histogram[index]++;
        }

        // Constants for polarization calculation
        double polarization = 0.0;
        double K = 1.0;
        double alpha = 1.6;

        // Calculate polarization using histogram and midpoints
        for (int i = 0; i < numIntervals; i++) {
            for (int j = 0; j < numIntervals; j++) {
                polarization += Math.pow(histogram[i], 1 + alpha) * histogram[j] * Math.abs(midpoints[i] - midpoints[j]);
            }
        }

        polarization *= K;
        return polarization;
    }

    /**
     * Main method to start the Polarimeter application.
     *
     * @param args Command-line arguments. Optional --delay parameter.
     */
    public static void main(String[] args) {
        delay = 5000; // Default delay in milliseconds
        final Polarimeter polarimeter;

        // Parse command-line arguments
        for (String arg : args) {
            if (arg.startsWith("--delay=")) {
                delay = Integer.parseInt(arg.substring(8));
            }
        }

        try {
            // Set up IP address, port, and connect to the server (simulation for testing without RMI registry)
            InetAddress ipAddress = InetAddress.getLoopbackAddress();
            int startPort = 5000;
            int endPort = 6000;
            Random random = new Random(System.currentTimeMillis());
            int randomPort = startPort + random.nextInt(endPort - startPort + 1);

            Registry registry = LocateRegistry.getRegistry();
            ServerInterface server = (ServerInterface) registry.lookup("Server");

            // Create instance of Polarimeter with the selected parameters
            polarimeter = new Polarimeter(server, ipAddress, randomPort);

            // Start Polarimeter and schedule periodic polarization measurement
            logger.log(Level.INFO, "Starting Polarimeter with delay: {0}ms", delay);
            polarimeter.scheduler.scheduleAtFixedRate(() -> {
                // Measure polarization with a new random topic each time
                polarimeter.measurePolarization();
            }, 0, delay, TimeUnit.MILLISECONDS);

            // Schedule updateTopic() to be called at the beginning and then at every delay period
            polarimeter.scheduler.scheduleAtFixedRate(() -> {
                polarimeter.updateTopic();
            }, 0, delay, TimeUnit.MILLISECONDS);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in main method", e);
        }
    }
}
