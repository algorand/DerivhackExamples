
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

        




     