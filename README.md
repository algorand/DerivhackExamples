# Introduction

The goal of the ISDA Common Domain Model (CDM)  is to allow financial institutions to have a coherent representation of financial instruments and events. This document shows how institutions can use the CDM and the Algorand blockchain to maintain separately owned but financial databases with the following properties:

1. **Coherency**: All institutions participating in a trade agree on the digital representation of that trade at any point in time.

2. **Privacy**: The details of the trade are only revealed to the institutions which participate in it. Any other agent cannot learn anything about the trade.

3. **Lineage**: Any modification in the state of a trade can refer to the previous state, generating a traceable lineage for the history of that trade.

4. **Ease of Use**: Because the Algorand blockchain is a permissionless blockchain, institutions can interact with it using the software of their choice, and without the need to set up their own distributed system. Algorand provides easy to use APIs that read to and write from the blockchain, and SDKs in [Python](https://developer.algorand.org/docs/python-sdk), [Go](https://developer.algorand.org/docs/go-sdk), [Java](https://developer.algorand.org/docs/java-sdk) and [Javascript](https://developer.algorand.org/docs/javascript-sdk). 


## Installing, Compiling and Running the Code

### Dependencies

Running the code in this repository requires that you have

1. A Unix-based OS such as Mac OS X or Linux
2. Java
3. Maven

### Java and Maven Installation
There are bash scripts (written for OS X) which install Java and Maven and set the correct paths to use them. These scripts are in the `INSTALL` folder and should be run in the following order

1. `install_brew.sh` if the user does not have Hombrew installed (OS X utility to install programs)
2. `install_java.sh` if the user does not have Java installed. This installs the OpenJDK 
3. `install_maven.sh` if the user does not have Maven installed
4. `install_mongo.sh` if the user does not have MongoDB installed

### Java library Installation

The main directory contains a pom.xml file which Maven uses to download Java libraries that the code depends on, including the Algorand Java SDK, and the Java implementation of the ISDA CDM.

The code has been tested on a computer running OS X  Version 10.14.5, OpenJDK 13, and Maven version 3.6.1.

###  Compilation
A `settings.xml` file is provided in the project root directory, use it install dependencies as below: 
```bash
mvn -s settings.xml clean install

```





### Running the Code
To run the example code and store the output in a file called ```output.txt```, run
```bash
sh run.sh > output.txt
```

### (OPTIONAL): Starting and Stopping MongoDB

The code needs to have a Mongo DB service running to persist some information.
Right now the ```run.sh``` script starts this service automatically if it is not running.
However, we have provided scripts  to start and stop this automatically 

To run the mongodb service, run
```bash
sh start_mongo.sh
```

To stop the mongodb service, run

```bash
sh stop_mongo.sh
``` 

## Ubuntu
These are bash scripts which install Java and Maven and set the correct paths to use them. These scripts are in the `INSTALL` folder and should be run in the following order

1. `install_java_for_ubuntu.sh` if the user does not have Java installed. This installs the OpenJDK 
2. `install_maven_for_ubuntu.sh` if the user does not have Maven installed



## Java library Installation

The main directory contains a pom.xml file which Maven uses to download Java libraries that the code depends on, including the Algorand Java SDK, and the Java implementation of the ISDA CDM.

The code has been tested on a computer running OS X  Version 10.14.5, OpenJDK 13, and Maven version 3.6.1. and on an AWS instance ("4.15.0-1044-aws") running Ubuntu 18.04.2 LTS, OpenJDK 11 and Maven version 3.6.0

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

The following function, from the class ```CommitExecution.java``` reads a CDM Event, creates Algorand accounts for all parties in the event. It gets the executing party (Client 1's broker), and has this party send details of the execution to all other parties on the Algorand blockchain.

```java
 public  class CommitExecution {

    public static void main(String [] args) throws Exception{
        
        //Read the input arguments and read them into files
        String fileName = args[0];
        String fileContents = ReadAndWrite.readFile(fileName);

         //Read the event file into a CDM object using the Rosetta object mapper
        ObjectMapper rosettaObjectMapper = RosettaObjectMapper.getDefaultRosettaObjectMapper();
        Event event = rosettaObjectMapper
                .readValue(fileContents, Event.class);
        
        //Create Algorand Accounts for all parties
        // and persist accounts to filesystem/database
        List<Party> parties = event.getParty();
        User user;
        DB mongoDB = MongoUtils.getDatabase("users");
        parties.parallelStream()
                .map(party -> User.getOrCreateUser(party,mongoDB))
                .collect(Collectors.toList());

        //Get the execution
        Execution execution = event
                                .getPrimitive()
                                .getExecution().get(0)
                                .getAfter()
                                .getExecution();


        // Get the executing party  reference
        String executingPartyReference = execution.getPartyRole()
                .stream()
                .filter(r -> r.getRole() == PartyRoleEnum.EXECUTING_ENTITY)
                .map(r -> r.getPartyReference().getGlobalReference())
                .collect(MoreCollectors.onlyElement());

        // Get the executing party
        Party executingParty = event.getParty().stream()
                .filter(p -> executingPartyReference.equals(p.getMeta().getGlobalKey()))
                .collect(MoreCollectors.onlyElement());

        // Get all other parties
        List<Party> otherParties =  event.getParty().stream()
                .filter(p -> !executingPartyReference.equals(p.getMeta().getGlobalKey()))
                .collect(Collectors.toList());

        // Find or create the executing user
        User executingUser = User.getOrCreateUser(executingParty, mongoDB);
       
        //Send all other parties the contents of the event as a set of blockchain transactions
        List<User> users = otherParties.
                            parallelStream()
                            .map(p -> User.getOrCreateUser(p,mongoDB))
                            .collect(Collectors.toList());

        List<Transaction> transactions = users
                                            .parallelStream()
                                            .map(u->executingUser.sendEventTransaction(u,event,"execution"))
                                            .collect(Collectors.toList());
        
    }
}

```

The corresponding shell command to execute this function with the Block trades file is 
```bash
##Commit the execution file to the blockchain
mvn -s settings.xml exec:java -Dexec.mainClass="com.algorand.demo.CommitExecution" \
 -Dexec.args="./Files/UC1_block_execute_BT1.json" -e -q
```

## Allocation
The second use case for Derivhack is allocation of trades. That is, the block trade execution given in use case 1 will be allocated among multiple accounts. Participants are also given a JSON CDM file specifying the [allocation] (https://github.com/algorand/DerivhackExamples/blob/master/Files/UC2_allocation_execution_AT1.json). Since allocations are CDM events, the same logic applies as in the Execution use case. To commit the allocation event to the blockchain, participants can use the following shell command

```bash
mvn -s settings.xml exec:java -Dexec.mainClass="com.algorand.demo.CommitAllocation" \
 -Dexec.args="./Files/UC2_allocation_execution_AT1.json" -e -q
```


## Affirmation
The third use case is the affirmation of the trade by the clients. In contrast with the other cases, the Participants can look at the classes ```CommitAffirmation.java``` (https://github.com/algorand/DerivhackExamples/blob/master/src/main/java/com/algorand/demo/CommitAffirmation.java) and ```AffirmImpl.java``` (https://github.com/algorand/DerivhackExamples/blob/master/src/main/java/com/algorand/demo/AffirmationImpl.java) for examples on how to derive the Affirmation of a trade from its allocation.

In the affirmation step, the client produces a CDM affirmation from the Allocation Event,
and sends the affirmation to the broker over the Algorand Chain.

```java

``` class CommitAffirmation {
public static void main(String[] args){

        //Load the database to lookup users
        DB mongoDB = MongoUtils.getDatabase("users");

        //Load a file with client global keys
        String allocationFile = args[0];
        String allocationCDM = ReadAndWrite.readFile(allocationFile);
        ObjectMapper rosettaObjectMapper = RosettaObjectMapper.getDefaultRosettaObjectMapper();
        Event allocationEvent = null;
            try{
                allocationEvent = rosettaObjectMapper
                                    .readValue(allocationCDM, Event.class);
            }
            catch(java.io.IOException e){
                e.printStackTrace();
            }
                
       
        List<Trade> allocatedTrades = allocationEvent.getPrimitive().getAllocation().get(0).getAfter().getAllocatedTrade();
        //Keep track of the trade index
        int tradeIndex = 0;

        //Collect the affirmation transaction id and broker key in a file
        String result = "";
        //For each trade...
        for(Trade trade: allocatedTrades){

        //Get the broker that we need to send the affirmation to
        String brokerReference = trade.getExecution().getPartyRole()
            .stream()
            .filter(r -> r.getRole() == PartyRoleEnum.EXECUTING_ENTITY)
            .map(r -> r.getPartyReference().getGlobalReference())
            .collect(MoreCollectors.onlyElement());

            User broker = User.getUser(brokerReference,mongoDB);

        //Get the client reference for that trade
        String clientReference = trade.getExecution()
                                        .getPartyRole()
                                        .stream()
                                        .filter(r-> r.getRole()==PartyRoleEnum.CLIENT)
                                        .map(r->r.getPartyReference().getGlobalReference())
                                        .collect(MoreCollectors.onlyElement());
                
        // Load the client user, with algorand passphrase
        User user = User.getUser(clientReference,mongoDB);
        String algorandPassphrase = user.algorandPassphrase;

        // Confirm the user has received the global key of the allocation from the broker
        String receivedKey = AlgorandUtils.readEventTransaction( algorandPassphrase, allocationEvent.getMeta().getGlobalKey());
        assert receivedKey == allocationEvent.getMeta().getGlobalKey() : "Have not received allocation event from broker";
            //Compute the affirmation
            Affirmation affirmation = new AffirmImpl().doEvaluate(allocationEvent,tradeIndex).build();
                    
             //Send the affirmation to the broker
            Transaction transaction = 
                        user.sendAffirmationTransaction(broker, affirmation);
                    
            result += transaction.getTx() + "," + brokerReference +"\n";
                    
                
            tradeIndex = tradeIndex + 1;
        }
        try{
           ReadAndWrite.writeFile("./Files/AffirmationOutputs.txt", result);
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

}
```

The affirmation step can be run with the shell command
```bash
## Create Affirmations from the Allocation file and Commit Them
mvn -s settings.xml exec:java -Dexec.mainClass="com.algorand.demo.CommitAffirmation"\
 -Dexec.args="./Files/UC2_allocation_execution_AT1.json"   -e  -q 
```
