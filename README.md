# Introduction

The goal of the ISDA Common Domain Model (CDM)  is to allow financial institutions to have a coherent representation of financial instruments and events. This document shows how institutions can use the CDM and the Algorand blockchain to maintain separately owned but financial databases with the following properties:

1. **Coherency**: All institutions participating in a trade agree on the digital representation of that trade at any point in time.

2. **Privacy**: The details of the trade are only revealed to the institutions which participate in it. Any other agent cannot learn anything about the trade.

3. **Lineage**: Any modification in the state of a trade can refer to the previous state, generating a traceable lineage for the history of that trade.

4. **Ease of Use**: Because the Algorand blockchain is a permissionless blockchain, institutions can interact with it using the software of their choice, and without the need to set up their own distributed system. Algorand provides easy to use APIs that read to and write from the blockchain, and SDKs in [Python](https://developer.algorand.org/docs/python-sdk), [Go](https://developer.algorand.org/docs/go-sdk), [Java](https://developer.algorand.org/docs/java-sdk) and [Javascript](https://developer.algorand.org/docs/javascript-sdk). 


## The Algorand Blockchain

The Algorand blockchain is a permissionless blockchain with hundreds of independently operating nodes distributed around the world. The Algorand blockchain allows developers to create their applications without having to set up their own distributed systems. The Algorand blockchain can process 1000 transactions per second with a latency of less than 5 secods.

In addition, Algorand provides extensive [documentation](https://developer.algorand.org/docs/getting-started), and provides SDKs in four languages (Go, Python, Java and Javascript) to interact with the blockchain. 

![Figure 1: Nodes running the Algorand client software around the world](https://github.com/algorand/DerivhackExamples/blob/master/blob/nodes_map.png)
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
In the Derivhack Hackathon, users  are given a [trade execution file](https://github.com/algorand/DerivhackExamples/blob/master/Files/UC1_block_execute_BT1.json) and need to 

1. Load the JSON file into their system
2. Create users in their distributed ledger corresponding to the parties in the execution
3. Create a report of the execution

In this example, we use the Algorand blockchain to ensure different parties have consistent versions of the file, while keeping their datastores private.  The information stored in the chain includes the global key of the execution, its lineage, and the file path where the user stored the Execution JSON object in their private data store. 

The following function, from the class ```CommitEvent.java``` reads a CDM Event, creates Algorand accounts for all parties in the event, and then commits the global key and lineage of the event to the blockchain. 


```java
 public static void main(String [] args) throws Exception{
        // This function 
        // 1. Reads a CDM Event from a JSON file
        // 2. Creates Algorand accounts for all parties in the event
        // 3. Commits information about the event to the Algorand blockchain
        // and to the participant's private datastores

        ObjectMapper rosettaObjectMapper = RosettaObjectMapper.getDefaultRosettaObjectMapper();
        //Read the input arguments and read them into files
        String fileName = args[0];
        String fileContents = ReadAndWrite.readFile(fileName);

         //Read the event file into a CDM object using the Rosetta object mapper
        Event event = rosettaObjectMapper
                .readValue(fileContents, Event.class);
        
        //Add any new parties to the database, and commit the event to their own private databases
        List<Party> parties = event.getParty();
        User user;

        for (Party party: parties){
             user = User.getOrCreateUser(party);
             user.commitEvent(event);
        }    
    }
    
```

The corresponding shell command to execute this function with the Block trades file is 
```bash
##Commit the execution file to the blockchain
mvn -s settings.xml exec:java -Dexec.mainClass="com.algorand.demo.CommitEvent" \
 -Dexec.args="./Files/UC1_block_execute_BT1.json" -e -q
```

## Allocation
The second use case for Derivhack is allocation of trades. That is, the block trade execution given in use case 1 will be allocated among multiple accounts. Participants are also given a JSON CDM file specifying the [allocation] (https://github.com/algorand/DerivhackExamples/blob/master/Files/UC2_allocation_execution_AT1.json). Since allocations are CDM events, the same logic applies as in the Execution use case. To commit the allocation event to the blockchain, participants can use the following shell command

```bash
mvn -s settings.xml exec:java -Dexec.mainClass="com.algorand.demo.CommitEvent" \
 -Dexec.args="./Files/UC2_allocation_execution_AT1.json" -e -q
```

### Bonus: Creating the Allocation Event from the Execution Event
Participants who want to generate their own allocation event from a file of [allocation instructions](https://github.com/algorand/DerivhackExamples/blob/master/Files/input_allocations.json) can look at the class ```AllocationStep.java```  (https://github.com/algorand/DerivhackExamples/blob/master/src/main/java/com/algorand/demo/AllocationStep.java) which has code that uses functions bundled with the ISDA CDM to generate Allocations from Executions and Allocation Instructions. The code below shows how to process the allocation instructions, and generate the allocation. It is illustrative for other tasks, because it shows how to use functions bundled with the CDM (in this case ```Allocate``` and ```AllocateImpl``` to generate new CDM objects from existing ones.

```java

    public static void main(String [] args) throws Exception{
        ObjectMapper rosettaObjectMapper = RosettaObjectMapper.getDefaultRosettaObjectMapper();
        //Read the input arguments and read them into files
        String allocationInstructionFile = args[0];
        String executionsCDMFile = args[1];
        String allocationInstructionsDH = ReadAndWrite.readFile(allocationInstructionFile);
        String executionsCDM = ReadAndWrite.readFile(executionsCDMFile);

         //Read the executions CDM into a CDM object using the Rosetta object mapper
        Event executionEvent = rosettaObjectMapper
                .readValue(executionsCDM, Event.class);
        
       // Get the execution associated with the "Execution Event"
        Execution execution = executionEvent
                                .getPrimitive()
                                .getExecution()
                                .get(0)
                                .getAfter()  
                                .getExecution(); 

        //Call a helper method to build the allocation instructions
        AllocationInstructions allocationInstructions 
            = buildAllocationInstructions(allocationInstructionsDH);

        //Use Guice to instantiate the allocation function at runtime
        Injector injector = Guice.createInjector(new AlgorandRuntimeModule());
        Allocate allocationFunction = injector.getInstance(Allocate.class);

        // Use the CDM's Allocate function to create an allocation event
        Event allocationEvent = allocationFunction.evaluate(execution,allocationInstructions);

        // Get all the parties for the event, and have them commit the event
        // to their own datastores, and to the blockchain
        List<Party> parties = allocationEvent.getParty();
        User user;
        for (Party party: parties){
             user = User.getOrCreateUser(party);
             user.commitEvent(allocationEvent);
        }

    }
```


## Affirmation
The third use case is the affirmation of the trade, by each party. In contrast with the other cases, the Participants can look at the classes ```AffirmationStep.java``` (https://github.com/algorand/DerivhackExamples/blob/master/src/main/java/com/algorand/demo/AffirmationStep.java) and ```AffirmImpl.java``` (https://github.com/algorand/DerivhackExamples/blob/master/src/main/java/com/algorand/demo/AffirmationImpl.java) for examples on how to derive the Affirmation of a trade from its allocation, and how to commit details of the affirmation to the Blockchain. 

The affirmation step can be run with the shell command
```bash
## Create Affirmations from the Allocation file and Commit Them
mvn -s settings.xml exec:java -Dexec.mainClass="com.algorand.demo.AffirmationStep"\
 -Dexec.args="./Files/UC2_allocation_execution_AT1.json"   -e  -q 
```

# Verifying Outputs
Each party in a CDM object stores their own copy of the object, together with its lineage, global key (if available), and a link to the event in the Algorand Blockchain. The blockchain ensures coherency: all users have the same CDM event, and there are no incoherencies between their representations This is almost immediate in an example setting where all parties' CDM objects are generated using the same code. In practice, different institutions may implement their code differently and run the risk of having different CDM objects. The blockchain generates consensus that all participants do have the same CDM  object, regardless of their own implementation

For example, in the Allocation use case, each party may have a CDM object like the following one
```json
{
    "globalKey": "PuitVBWvjXiUF2+7PaG+pXAASnYeIatGIVPTKfPVwQE=",
    "algorandTransactionID": "DGM76CDTM6KTIEWRIHMCHJGBO5T5VNCIFXHJTXMKYHHL76M5R37A",
    "algoExplorerLink": "https://testnet.algoexplorer.io/tx/DGM76CDTM6KTIEWRIHMCHJGBO5T5VNCIFXHJTXMKYHHL76M5R37A",
    "event": {
        "lineage": {
            "executionReference": [{"globalReference": "xxdy/Zsa8dH/GeGisnjJhdqR8cGAuJEU2idvHFlCsuo="}],
            "eventReference": [{"globalReference": "zeOwGVI5iP80wGc+OHIFp/0QNgcIr88yrAa1M4j576Q="}]
        },
        "primitive": {"allocation": [{
            "before": {"execution": {
                "identifier": [{
                    "meta": {"globalKey": "568EPdYtUvpI+Qm08z1yOHoaXH8eCwU/7IP/bDoq0MY="},
                    "issuerReference": {"globalReference": "3vqQOOnXah+v+Cwkdh/hSyDP7iD6lLGqRDW/500GvjU="},
                    "assignedIdentifier": [{
                        "identifier": {"value": "W3S0XZGEM4S82"},
                        "version": 1
                    }]
                }],
                "executionVenue": {"name": {"value": "Execution Venue"}},
                "product": {"security": {"bond": {"productIdentifier": {
                    "identifier": [{"value": "DH0371475458"}],
                    "source": "CUSIP"
                }}}},
                "quantity": {"amount": 800000},
                "settlementTerms": {
                    "settlementAmount": {
                        "amount": 786720,
                        "currency": {"value": "USD"}
                    },
                    "settlementDate": {"adjustableDate": {"unadjustedDate": {
                        "month": 10,
                        "year": 2019,
                        "day": 17
                    }}}
                },
...

```


