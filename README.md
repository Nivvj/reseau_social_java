# Distributed System with Java RMI

## Project Overview

This project implements a distributed system using Java RMI (Remote Method Invocation) to manage various types of agents and users. The system includes the following classes:

- **Agent**: Represents a generic agent interface with methods for interacting with the server.
- **Server**: Implements the `ServerInterface` to manage agents, topics, and facilitate communication among them using RMI.
- **User**: A basic user interacting with the system.
- **Influencer**: A specialized type of user with additional functionality for influencing topics or agents.
- **Critical Thinker**: A user type focused on critical analysis and evaluation within the system.
- **Consensus Finder**: A user specialized in finding consensus among agents or topics.
- **Polarimater**: A user specializing in identifying and analyzing polarized opinions within the system.

## How to Run

### 1. Compile Java Files
Ensure you are in the directory containing all Java source files (`Agent.java`, `Server.java`, `User.java`, etc.).

```bash
javac *.java
```

### 2. Start RMI Registry
Before running the server, start the RMI registry on a specific port. Replace `<port_number>` with your desired port number.

```bash
rmiregistry <port_number> &
```

Example:

```bash
rmiregistry 1099 &
```

### 3. Run the Server
Execute the Server class with the `--port=<port_number>` argument to specify the port number for RMI communication. Replace `<port_number>` with the same port number used for starting the RMI registry.

```bash
java Server --port=<port_number>
```

### 4. Create a Topic
Once the server is running, execute the Proposer class to create topics.

```bash
java Proposer --topic=<TopicName> --topic=<TopicName> ...
```

### 5. Execute Agent Classes
Once the server is running, execute any of the other classes (`User`, `Influencer`, `CriticalThinker`, `ConsensusFinder`, `Polarimater`) to simulate different types of agents interacting with the distributed system.

```bash
java User --<Username> --<Username> ...
java Influencer --<Username> --<Username> ...
java CriticalThinker --<Username> --<Username> ...
```

### 6. Execute Other Classes

```bash
java ConsensusFinder
java Polarimater --<delay>
```

## Notes
- Ensure all necessary dependencies are available in your classpath during compilation and execution.
- Adjust port numbers (`<port_number>`) in both RMI registry and server start commands as needed, ensuring they match.
- Monitor console outputs and log files (if configured) for system messages, errors, or other relevant information during execution.
