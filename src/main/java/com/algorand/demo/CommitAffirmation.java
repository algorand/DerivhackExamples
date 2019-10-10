
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
import java.util.stream.Collectors;
import com.google.common.collect.MoreCollectors;


import org.isda.cdm.*;
import org.isda.cdm.PartyRoleEnum.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.*;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonParseException;

import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigInteger;
import com.regnosys.rosetta.common.serialisation.RosettaObjectMapper;


public  class CommitAffirmation {

    public static void commitAffirmation(String userKey,String executionKey, String allocationKey){

        
        
        

    }

    public static void main(String[] args){
        DB mongoDB = MongoUtils.getDatabase("users");

        String fileName = args[0];
        String fileContents = ReadAndWrite.readFile(fileName);
        String[] clients = fileContents.split("\n");
       
        for(String client: clients){
            User user = User.getUser(client,mongoDB);
            String algorandPassphrase = user.algorandPassphrase;
            String allocationCDM = AlgorandUtils.readStringTransaction(algorandPassphrase,"allocation");

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
            int tradeIndex = 0;
            for(Trade trade: allocatedTrades){

                String brokerReference = trade.getExecution().getPartyRole()
                    .stream()
                    .filter(r -> r.getRole() == PartyRoleEnum.EXECUTING_ENTITY)
                    .map(r -> r.getPartyReference().getGlobalReference())
                    .collect(MoreCollectors.onlyElement());

                User broker = User.getUser(brokerReference,mongoDB);

                String clientReference = trade.getExecution()
                                            .getPartyRole()
                                            .stream()
                                            .filter(r-> r.getRole()==PartyRoleEnum.CLIENT)
                                            .map(r->r.getPartyReference().getGlobalReference())
                                            .collect(MoreCollectors.onlyElement());
                
                if (clientReference.equals(client)){
                    Affirmation affirmation = new AffirmImpl().doEvaluate(allocationEvent,tradeIndex).build();
                    List<Transaction> transactionList = 
                        user.sendAffirmationTransaction(broker, affirmation,"affirmation",BigInteger.valueOf(1000));
                    for(Transaction transaction: transactionList){
                        System.out.println(transaction.getTx());
                    }
                }
                tradeIndex = tradeIndex + 1;
            
            }
        } 

    }
}

  
 
/*
    public static void main(String [] args) throws Exception{
        ObjectMapper rosettaObjectMapper = RosettaObjectMapper.getDefaultRosettaObjectMapper();
        //Read the input arguments and read them into files
        String fileName = args[0];
        String fileContents = ReadAndWrite.readFile(fileName);

        //Read the event file into a CDM object using the Rosetta object mapper
        Affirmation affirmation = rosettaObjectMapper
                .readValue(fileContents, Affirmation.class);
 
        //Get the client party

        String clientReference = affirmation.getPartyRole()
                .stream()
                .filter(r -> r.getRole() == PartyRoleEnum.CLIENT)
                .map(r -> r.getPartyReference().getGlobalReference())
                .collect(MoreCollectors.onlyElement());



        // Get the executing party
        Party client = affirmation.getParty().stream()
                .filter(p -> clientReference.equals(p.getMeta().getGlobalKey()))
                .collect(MoreCollectors.onlyElement());

        
   
    }
}

*/
     