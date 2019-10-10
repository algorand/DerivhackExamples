
package com.algorand.demo;
import com.algorand.utils.*;
import com.algorand.algosdk.algod.client.model.Transaction;

import org.jongo.Jongo;
import com.mongodb.DB;

import com.algorand.algosdk.algod.client.model.Transaction;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.regnosys.rosetta.common.serialisation.RosettaObjectMapper;

import java.util.*;

import com.google.inject.Inject;
import com.google.inject.Guice;
import com.google.inject.Injector;

import org.isda.cdm.*;
import org.isda.cdm.PartyRoleEnum.*;

import java.util.stream.Collectors;
import com.google.common.collect.MoreCollectors;

import java.math.BigInteger;

public  class CommitAllocation {

 

    public static void main(String [] args) throws Exception{
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
        DB mongoDB = MongoUtils.getDatabase("users");
        parties.parallelStream()
                .map(party -> User.getOrCreateUser(party,mongoDB))
                .collect(Collectors.toList());
      
      
        //Get the allocated trades
        List<Trade> allocatedTrades = event
                                .getPrimitive()
                                .getAllocation().get(0)
                                .getAfter()
                                .getAllocatedTrade();
             
        //Get the executions of the allocated trades
        List<Execution> executions = allocatedTrades.stream()
                                    .map(trade -> trade.getExecution())
                                    .collect(Collectors.toList());                  

        //For each execution, the executing party (broker) sends
        // a notification of the allocation event to each client account

        //Collect a list of clients that need to affirm the allocation
        String clients="";

        for(Execution execution: executions){
            //Get the executing party reference
            String executingPartyReference = execution.getPartyRole()
                    .stream()
                    .filter(r -> r.getRole() == PartyRoleEnum.EXECUTING_ENTITY)
                    .map(r -> r.getPartyReference().getGlobalReference())
                    .collect(MoreCollectors.onlyElement());

            // Get the other parties
            Set<String> otherPartyReferences = execution.getPartyRole()
                    .stream()
                    .filter(r -> r.getRole() != PartyRoleEnum.EXECUTING_ENTITY)
                    .map(r -> r.getPartyReference().getGlobalReference())
                    .collect(Collectors.toSet());

            // Get the client
            String clientReference = execution.getPartyRole()
                    .stream()
                    .filter(r -> r.getRole() == PartyRoleEnum.CLIENT)
                    .map(r -> r.getPartyReference().getGlobalReference())
                    .collect(MoreCollectors.onlyElement());

            // Get the executing user
            User executingUser = User.getUser(executingPartyReference,mongoDB);             

            // Get all other users
            List<User> otherUsers =  otherPartyReferences.stream()
                    .map(reference -> User.getUser(reference,mongoDB))
                    .collect(Collectors.toList());
            
            //Send all other parties the contents of the event as a set of blockchain transactions
            List<Transaction> transactions =   otherUsers
                                                .parallelStream()
                                                .map( u->  executingUser
                                                .sendEventTransaction(u,event,"allocation"))
                                              .collect(Collectors.toList());
            
            clients += clientReference + "\n";
            }
        
        //Write client references to a file consumed by "CommitAffirmation.java"
        ReadAndWrite.writeFile("./Files/AffirmationInputs.txt",clients);
        }

    }


        




     