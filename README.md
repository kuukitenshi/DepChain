# High Dependability Systems - 2024/2025 - DepChain

### Group 03:

- Guilherme Wind - ist1112167
- Laura Cunha - ist1112269
- Rodrigo Correia - ist1112270

---
## Project
- This report details DepChain, a project aimed at developing a simplified permissioned (closed membership) blockchain system with high dependability guarantees, implemented in Java. It assumes static membership and a pre-defined correct leader, eliminating the need for leader changes, while allowing all the blockchain members to exhibit arbitrary or Byzantine behavior.

- In a brief way, our client sends an append request to all the processes, with all communication occurring over UDP, along with some abstraction upper layers to ensure that messages eventually arrive and that their integrity and authenticity are verified. Then, each blockchain member initiates an instance of Byzantine Read/Write Epoch Consensus and eventually either decides on a value or aborts.

- As the clients have accounts in the system, they can perform transactions between them, which include transactions of a native coin called DepCoin or trigger transactions on smart contracts to exchange ISTCoins and manage the Acess Control to a blacklist, and the consensus algorithm will decide the order of the transactions that will be applied to all the members.

- We also added a set of tests to assess various Byzantine behaviors, at the consensus, network abstraction layers, and transaction levels.

---
## Compiling

To compile all projects run the following command in the project's root directory:

```bash
mvn clean package install
```

After that a `.jar` file for each project should be created inside its target directory

---

## Generating static system information

In order to generate the required static system information you can run the script available in the scripts directory that is created after compilation using:

```bash
./scripts/bin/depchain-genstatic <nr nodes> <nr clients> <keystore path> <members file>
```

Below is an example of parameters:

```bash
./scripts/bin/depchain-genstatic 4 2 pki.keystore members.toml
```

**Note:** the generated nodes will have ids `[node-0, ..., node-3]` with the above command.

---

## Running

### Node

To execute a blockchain node run the following command:

```bash
java -jar depchain-node/target/depchain-node-0.1.jar <node id> <members file> <keystore file> [byzantine mode]
```

An example execution could be:

```bash
java -jar depchain-node/target/depchain-node-0.1.jar node-0 members.toml pki.keystore crash
```

The byzantine mode argument is optional (if ommited the node will be correct) and the following options are available:

- **NO_RESPONSE** - the node will fail to respond to some messages;
- **CRASH** - the node will crash after a timeout (1 minute);
- **DIFFERENT_WRITE** - the node will write some different arbitrary value;
- **DIFFERENT_ACCEPT** - the node will send a different value than the calculated in the accpet;
- **DIFFERENT_CLIENT** - the node will send a different value than the one sent by the client.

In the client menu there is also a replay option so simulate a **replay attack**, by resending the last sent transaction.

### Client

To execute a client run the following command:

```bash
java -jar depchain-client/target/depchain-client-0.1.jar <key file> <members file> <keystore file>
```

An example execution could be:

```bash
java -jar depchain-client/target/depchain-client-0.1.jar client-0.key members.toml pki.keystore
```
