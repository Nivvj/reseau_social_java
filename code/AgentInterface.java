import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.UUID;
import java.net.InetAddress;
import java.io.*;
import java.util.*;
/**
 * The AgentInterface defines the remote interface for interacting with agents
 * in a distributed opinion retrieval system using Java RMI (Remote Method Invocation).
 * Agents implementing this interface can notify new topics, manage opinions,
 * and interact with other agents by fetching and setting their influence levels.
 * 
 * Design Choices:
 * - Extends java.rmi.Remote to mark methods as capable of remote invocation,
 *   enabling distributed communication between agents and the server.
 * - Defines essential methods for fetching and updating agent information,
 *   opinions, and degrees of influence with other agents, facilitating
 *   collaborative opinion retrieval and management across the network.
 * - Uses RemoteException to handle remote communication errors transparently,
 *   ensuring robustness in handling network-related exceptions during method calls.
 * - Returns and accepts standard Java types (UUID, String, InetAddress, double, Map),
 *   ensuring compatibility and ease of use across different Java environments
 *   without reliance on platform-specific types or libraries.
 */

public interface AgentInterface extends java.rmi.Remote {
    void notifyNewTopic(String topic) throws RemoteException;
    UUID getId() throws RemoteException;
    String getUsername() throws RemoteException;
    InetAddress getIpAddress() throws RemoteException;
    int getPort() throws RemoteException;
    Map<String, Double> getOpinions() throws RemoteException;
    double getInfluenceWithAgent(UUID agentId) throws RemoteException;
    void setInfluenceWithAgent(UUID agentId, double influence) throws RemoteException;
    void addOpinion(String topic, double opinion) throws RemoteException;
    void setOpinion(String topic, double opinion) throws RemoteException;
    double getOpinionValue(String  topic) throws RemoteException;
}
