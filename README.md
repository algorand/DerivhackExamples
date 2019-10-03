# Introduction

The goal of the ISDA Common Domain Model (CDM)  is to allow financial institutions to have a coherent representation of financial instruments and events. This document shows how institutions can use the CDM and the Algorand blockchain to maintain separately owned but financial databases with the following properties:

1. **Coherency**: All institutions participating in a trade agree on the digital representation of that trade at any point in time.

2. **Privacy**: The details of the trade are only revealed to the institutions which participate in it. Any other agent cannot learn anything about the trade.

3. **Lineage**: Any modification in the state of a trade can refer to the previous state, generating a traceable lineage for the history of that trade.

4. **Ease of Use**: Because the Algorand blockchain is a permissionless blockchain, institutions can interact with it using the software of their choice, and without the need to set up their own distributed system. Algorand provides easy to use APIs that read to and write from the blockchain, and SDKs in [Python](https://developer.algorand.org/docs/python-sdk), [Go](https://developer.algorand.org/docs/go-sdk), [Java](https://developer.algorand.org/docs/java-sdk) and [Javascript](https://developer.algorand.org/docs/javascript-sdk). 


## The Algorand Blockchain

The Algorand blockchain is a permissionless blockchain with hundreds of independently operating nodes distributed around the world. The Algorand blockchain allows developers to create their applications without having to set up their own distributed systems. Algorand provides extensive [documentation](https://developer.algorand.org/docs/getting-started), and provides SDKs in four languages (Go, Python, Java and Javascript) to interact with the blockchain. 

![Figure 1: Nodes running the Algorand client software around the world](https://github.com/algorand/isdasample/blob/master/blob/image_1.png)
*Figure 1: Nodes running the Algorand client software around the world*


# Installing, Compiling and Running the Code

## Dependencies

Running the code in this repository requires that you have

1. A Unix-based OS such as Mac OS X or Linux
2. Java
3. Maven

## Java and Maven Installation
There are bash scripts (written for OS X) which install Java and Maven and set the correct paths to use them. These scripts are in the `INSTALL` folder and should be run in the following order

1. `install_brew.sh` if the user does not have Hombrew installed (OS X utility to install programs)
2. `install_java.sh` if the user does not have Java installed. This installs the OpenJDK 
3. `install_maven.sh` if the user does not have Maven installed

## Java library Installation

The main directory contains a pom.xml file which Maven uses to download Java libraries that the code depends on, including the Algorand Java SDK, and the Java implementation of the ISDA CDM.

The code has been tested on a computer running OS X  Version 10.14.5, OpenJDK 13, and Maven version 3.6.1.

##  Compilation
A `settings.xml` file is provided in the project root directory, use it install dependencies as below: 
```bash
mvn -s settings.xml clean install

```

You can also run 
```bash
sh compile.sh
```
from the root directory.

## Running the Code
To run the example code, type 
```bash
sh run.sh 
```
in the root directory.

# Example Use Cases
## Execution
In the Derivhack Hackathon, users  are given a [trade execution file](https://github.com/algorand/isdasample/Files/UC1_block_execute_BT1.json) and need to 

1. Load the JSON file into their system
2. Create users in their distributed ledger corresponding to the parties in the execution
3. Create a report of the execution

In this example, we use the Algorand blockchain to ensure different parties have consistent versions of the file, while keeping their datastores private.  The information stored in the chain includes the global key of the execution, its lineage, and the file path where the user stored the Execution JSON object in their private data store. 

Figure 2 shows the code from the main function in the class ```CommitEvent.java```, which reads a CDM Event, creates Algorand accounts for all parties in the event, and then commits the global key and lineage of the event to the blockchain. 

![Figure 2: Committing an Execution Event](https://github.com/algorand/isdasample/blob/master/blob/commit_event.png)

The corresponding shell command to execute this function is 
```bash
##Commit the execution file to the blockchain
mvn -s settings.xml exec:java -Dexec.mainClass="com.algorand.demo.CommitEvent" \
 -Dexec.args="./Files/UC1_block_execute_BT1.json" -e -q
```






# Algorand’s Framework for Processing CDM events

## Processing CDM Events

Financial institutions that use the Algorand blockchain can use any programming language to process CDM events. For example, they can use the Java implementation of the CDM provided by Regnosys to create events, and serialize them to JSON. 

Example 1 shows a snippet of code that creates an Allocation Primitive using the Java implementation of the CDM based on inputs read from a JSON file.

![Example 1: Code snippet using the Java implementation of CDM to create a JSON object with Allocation Details](https://github.com/algorand/isdasample/blob/master/blob/image_0.png)
*Example 1: Code snippet using the Java implementation of CDM to create a JSON object with Allocation Details*

## Committing Hashed Event to the Algorand Blockchain


Once users have serialized a CDM Object to JSON, they can use a hash function (including Regnosys’ provided hash function that generates canonical keys of CDM objects) to obtain a compact digital fingerprint of the CDM object. This digital fingerprint can then be uploaded to the Algorand blockchain using any of Algorand’s SDKs. Example 2 below shows a program that reads a JSON representation of a CDM Allocation Primitive, deserializes it to Java, computes the Rosetta Key of the Allocation Primitive, and commits this key to the blockchain. 

![Example 2:  Java Program that reads a CDM file, computes the Rosetta Key, and commits the hash to the Algorand blockchain](https://github.com/algorand/isdasample/blob/master/blob/image_2.png)
*Example 2:  Java Program that reads a CDM file, computes the Rosetta Key, and commits the hash to the Algorand blockchain*

The utility of committing only the Rosetta Key to the blockchain is two-fold

1. All participants in the contract can verify in real-time, by referring to the key on the blockchain, that they have the same representation of the CDM object.

2. Because only a hash is committed to the blockchain, no information is revealed to outsiders who do not have the original JSON object. Thus, any details of the financial transaction are known only to the participating institutions. 

The Rosetta Key committed to the chain serves as time-stamped evidence of a CDM event. Algorand has native support for [cryptographic multisignatures](https://en.wikipedia.org/wiki/Multisignature). Using this feature, a CDM event may not be committed to the blockchain unless all relevant parties  provide their cryptographic signature confirming that the event has happened, and all parties have the same JSON representation of the event.

In addition to committing a CDM event’s Rosetta Key to the blockchain, parties can also commit the Rosetta Key of any objects  referenced by that event. In this way, a complete lineage of a CDM event can be recorded on the blockchain. 

## Verifying Consistency of Hashed Events

In case of disputes or discrepancies, parties can read the Rosetta keys from the blockchain to verify that a given CDM JSON object is the one that has been agreed to. Example 3 below shows Java code that verifies the Rosetta Key of a CDM object against the committed key of that object on the blockchain.

![Example 3:  Java program that verifies that the Rosetta Key of a CDM object matches the Rosetta Key of that
object on the blockchain](https://github.com/algorand/isdasample/blob/master/blob/image_3.png)
*Example 3:  Java program that verifies that the Rosetta Key of a CDM object matches the Rosetta Key of that
object on the blockchain*

## Putting it All Together

Example 4a shows a script that combines the three steps described above. The first step is processing input trades and creating a CDM event. The second step is committing the Rosetta Key of that CDM event to the Algorand blockchain. The last step is verifying that the Rosetta Key committed to the blockchain corresponds to the given CDM event (this last step is necessary, for example, when there are multiple parties that want to ensure their CDM representations stay consistent across the life cycle of the trade). 

![Example 4a: Sample Maven scripts to process a CDM event, commit its Rosetta Key  to the Algorand blockchain, and verifies the Rosetta Key that has been committed](https://github.com/algorand/isdasample/blob/master/blob/image_4.png)
*Example 4a: Sample Maven scripts to process a CDM event, commit its Rosetta Key  to the Algorand blockchain, and verifies the Rosetta Key that has been committed*

Example 4b shows the output of running this script, including the CDM JSON file name, the link to the Algorand transaction committing this event, and the Rosetta Keys of the committed file. 

![Example 4b: Output from sample Maven scripts, including Rosetta Key of CDM object and link to transaction on Algorand blockchain](https://github.com/algorand/isdasample/blob/master/blob/image_5.png)
*Example 4b: Output from sample Maven scripts, including Rosetta Key of CDM object and link to transaction on Algorand blockchain*

Example 4c shows that the transaction, including the Rosetta Key corresponding to the CDM event, can be verified on the Algorand blockchain explorer. 
![Example 4c: Transaction with details, including hashed Rosetta Key, on the Algorand blockchain explorer](https://github.com/algorand/isdasample/blob/master/blob/image_6.png)
*Example 4c: Transaction with details, including hashed Rosetta Key, on the Algorand blockchain explorer*

