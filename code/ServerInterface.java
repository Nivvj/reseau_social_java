import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.*;
import java.util.UUID;
import java.net.InetAddress;

/**
 * The ServerInterface defines the remote interface for managing agents and topics
 * in a distributed opinion retrieval system using Java RMI (Remote Method Invocation).
 * This interface allows clients to register agents, create topics, retrieve agent
 * IP addresses and ports, and obtain random agents and topics for interaction.
 * 
 * Design Choices:
 * - Extends java.rmi.Remote to mark methods as capable of remote invocation,
 *   enabling distributed communication between clients and the server.
 * - Defines methods for registering agents, managing topics, and interacting with
 *   agents by retrieving their IP addresses, ports, and random or specific agent lists.
 * - Uses RemoteException to handle remote communication errors transparently,
 *   ensuring robustness in handling network-related exceptions during method calls.
 * - Returns and accepts standard Java types (UUID, AgentInterface, InetAddress, int, List),
 *   ensuring compatibility and ease of use across different Java environments
 *   without reliance on platform-specific types or libraries.
 */

public interface ServerInterface extends Remote {
    void registerAgent(UUID id, AgentInterface agent, InetAddress ip, int port)throws RemoteException;
    void createTopic(String topic) throws RemoteException;
    InetAddress receiveIp(AgentInterface agent) throws RemoteException;
    int receivePort(AgentInterface agent) throws RemoteException;
    AgentInterface getRandomAgent() throws RemoteException;
    List<AgentInterface> getListOfAgent()  throws RemoteException;
    List<AgentInterface> getTwoAgent() throws RemoteException;
    String getRandomTopic() throws RemoteException;

}

