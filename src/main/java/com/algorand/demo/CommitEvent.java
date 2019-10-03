
package com.algorand.demo;
import com.algorand.utils.*;
import com.algorand.algosdk.algod.client.model.Transaction;


import com.algorand.algosdk.algod.client.model.Transaction;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.regnosys.rosetta.common.serialisation.RosettaObjectMapper;

import java.util.*;

import com.google.inject.Inject;
import com.google.inject.Guice;
import com.google.inject.Injector;

import org.isda.cdm.*;

public  class CommitEvent {
// A class to commit CDM Events to the Algorand Blockchain

 

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
}


     