
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

public  class CommitAffirmation {

    public static void commitAffirmation(Affirmation affirmation) throws Exception{

        //Add any new parties to the database, and commit the event to their own private databases
        List<Party> parties = affirmation.getParty();
        User user;
        DB mongoDB = MongoUtils.getDatabase("users");

        for (Party party: parties){
             user = User.getOrCreateUser(party,mongoDB);
             user.commitAffirmation(affirmation);
        } 


    }
 

    public static void main(String [] args) throws Exception{
        ObjectMapper rosettaObjectMapper = RosettaObjectMapper.getDefaultRosettaObjectMapper();
        //Read the input arguments and read them into files
        String fileName = args[0];
        String fileContents = ReadAndWrite.readFile(fileName);

         //Read the event file into a CDM object using the Rosetta object mapper
        Affirmation affirmation = rosettaObjectMapper
                .readValue(fileContents, Affirmation.class);
 
        commitAffirmation(affirmation);
   
    }
}


     