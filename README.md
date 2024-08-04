/**
 * Distributed System with Java RMI
 * AUTHORS: Yakaré Koite (12112812) & Nivetha Vijayataran (11905642)
 *
 * This project implements a distributed system using Java RMI (Remote Method Invocation) to manage various types
 * of agents and users. It includes different classes such as Agent, Server, User, Influencer, Critical Thinker,
 * Consensus Finder, and Polarimater.
 *
 * Project Overview:
 * - Agent: Represents a generic agent interface with methods for interacting with the server.
 * - Server: Implements the ServerInterface to manage agents, topics, and facilitate communication among them using RMI.
 * - User: A basic user interacting with the system.
 * - Influencer: A specialized type of user with additional functionality for influencing topics or agents.
 * - Critical Thinker: A user type focused on critical analysis and evaluation within the system.
 * - Consensus Finder: A user specialized in finding consensus among agents or topics.
 * - Polarimater: A user specializing in identifying and analyzing polarized opinions within the system.
 *
 * How to Run:
 * 1. Compile Java Files:
 *    ```bash
 *    javac *.java
 *    ```
 *    Ensure you are in the directory containing all Java source files (Agent.java, Server.java, User.java, etc.).
 *
 * 2. Start RMI Registry:
 *    Before running the server, start the RMI registry on a specific port. Replace <port_number> with your desired port number.
 *    ```bash
 *    rmiregistry <port_number> &
 *    ```
 *    Example:
 *    ```bash
 *    rmiregistry 1099 &
 *    ```
 *
 * 3. Run the Server:
 *    Execute the Server class with the --port=<port_number> argument to specify the port number for RMI communication.
 *    ```bash
 *    java Server --port=<port_number>
 *    ```
 *    Replace <port_number> with the same port number used for starting the RMI registry.
 *

 * 4. Create a topic:
 *    Once the server is running, execute of the Proposer classe to create topic 
 *    ```bash
 *    java Proposer --topic=<TopicName > --topic=<TopicName > ....
 *    ```
 *

 * 5.  Execute Agent Classes:
 *    Once the server is running, execute any of the other classes (User, Influencer, CriticalThinker, ConsensusFinder, Polarimater)
 *    to simulate different types of Agent interacting with the distributed system.
 *    ```bash
 *    java User --<Username> --<Username> ...
 *    java Influencer --<Username>  --<Username> ...
 *    java CriticalThinker --<Username>  --<Username> ...
 *    ```

 * 6. Execute other Classes:
 *    ```bash
 *    java Consensus Finder
 *    java Polarimeter --<delay>
 *    ```

 * Notes:
 * - Ensure all necessary dependencies are available in your classpath during compilation and execution.
 * - Adjust port numbers (<port_number>) in both RMI registry and server start commands as needed, ensuring they match.
 * - Monitor console outputs and log files (if configured) for system messages, errors, or other relevant information during execution.
 */

