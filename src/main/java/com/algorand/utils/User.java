package com.algorand.utils;

import com.algorand.algosdk.account.Account;
import com.algorand.algosdk.crypto.Address;


import java.util.Arrays;

import java.util.ArrayList;
import java.util.List;


import java.io.*;
import java.util.*;
import java.nio.*;
import java.nio.file.*;


import org.isda.cdm.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonInclude;

import com.fasterxml.jackson.core.type.TypeReference;
import com.regnosys.rosetta.common.serialisation.RosettaObjectMapper;

import com.algorand.algosdk.algod.client.model.Transaction;
import org.apache.commons.codec.digest.DigestUtils;

import com.google.common.collect.*;

/****
User Class: Creates an Algorand User given a CDM Party, and manages that user's
             keys and storage
****/

public class User{
	public String globalKey;
	public String algorandID;
	public String algorandPassphrase;
	public String name;
	public Party party;
	public String userHash;

	public User( Party party ) throws Exception{
	// Creates a user given a party object

				// Use a Jackson mapper to read the party object into a JSON object
				ObjectMapper mapper = new ObjectMapper();
				mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

				//Create an algorand account for the user
				ArrayList<String> algorandInfo = createAlgorandAccount();
				this.algorandID = algorandInfo.get(0);
				this.algorandPassphrase = algorandInfo.get(1);

				//Record the CDM party object, glogal key and name
				this.party = party;
				this.globalKey = party.getMeta().getGlobalKey();
				this.name = party.getName().getValue();

				//Create a hexadecimal hash, which can be used to create a user file
				// and folder storing the CDM events that this user is a party to
				this.userHash = DigestUtils.sha256Hex(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(party));
	}

	public static User getOrCreateUser(Party party) throws Exception{
		// Creates a user from a party if that user has not been recorded yet.
		// If the user has been recorded, then returns that user

		// Jackson mapper to deserialize and serialze objects to and from JSON
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

		// Compute the hexadecimal party hash
		String partyHash = DigestUtils.sha256Hex(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(party));
	
		//In this filesystem implementation, we check if there's a file corresponding to the
		// user's hexadecimal hash

		// Note this cannot be the user's CDM global key because the CDM key
		// may have forbidden characters such as slashes ("/")
		File[] usersArray = new File("./Users/").listFiles(File::isDirectory);
		List<File> userList = new ArrayList<File>(Arrays.asList(usersArray));
		File userFile;

		// Try finding the user. If the program finds the user's file in the filesystem,
		// we have already recorded the user
		try{
		 userFile = userList.stream()
							.filter(x -> x.getName().equals(partyHash + ".json"))
							.collect(MoreCollectors.onlyElement());
		
		 String userFileContent = ReadAndWrite.readFile("./Users/" + partyHash + ".json");
		 User readUser = mapper.readValue(userFileContent, User.class);
		 return readUser;
		}

		// If the program did not find the user, then it creates a user
		// with the User constructor
		// This includes constructing an Algorand account for the user
		// The details of the user, including the Algorand account and the CDM Party information
		// are stored in a JSON file in the users directory

		// This program also creates a directory for the user in ./UserDirectories/
		// This simulates a private data store where each user stores CDM events that they are party to
		// In this simplified example, all files live in the same datastore.
		// In practice, different users would have different data stores
		// Coherency of different datastores would be guaranteed by users verifying that all files
		// have the same hash on the blockchain
		catch(NoSuchElementException e){
				User user =  new User(party); 

				String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(user);
				ReadAndWrite.writePrettyJSON("./Users/"+partyHash+".json",json);
				Files.createDirectories(Paths.get("./UserDirectories/"+partyHash+"/Events/"));
				Files.createDirectories(Paths.get("./UserDirectories/"+partyHash+"/Affirmations/"));

				return user;
		}

		//If the program finds more than one user with a given key, it throws an exception
		catch(IllegalArgumentException e){
				System.out.println("More than one user by that key");
				e.printStackTrace();
				return null;
			}
			

	}



	
		


	public void commitEvent(Event event) throws Exception{
		//User specific function to commit a CDM event

		// Get the globla key of the event
		String eventKey = event.getMeta().getGlobalKey();

		//Because the global key is not hexadecimal, we need a hexadecimal hash as well
		// to create a file for the event with the hash as the name of the file
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

		String filename = DigestUtils.sha256Hex(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(event));

		 //Create an Algorand Transaction
		// Commit the filename, global key, and the lineage
        Transaction transaction = null;
        try {
        	transaction = NotesTransaction.commitNotes("{FileName: " + filename + ", GlobalKey:" + eventKey + "," + "Lineage: " + mapper.writerWithDefaultPrettyPrinter().writeValueAsString(event.getLineage())+"}");
        }
        catch(Exception e){
        	e.printStackTrace();
        	System.out.println("Could not commit Algorand transaction");
        	return;
        }

        if(transaction != null){
        	//If we committed the transaction, then save the link to the transaction and the 
        	// details of the Event to local storage
        	String txID = transaction.getTx();
        	EventTransaction eventTransaction = new EventTransaction(event,txID);
        	String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(eventTransaction);
        	ReadAndWrite.writePrettyJSON("./UserDirectories/"+this.userHash+"/Events/"+filename+".json",json);
        }


	}

		public void commitAffirmation(Affirmation affirmation) throws Exception{
			// User specific function to commit a CDM affirmation
			ObjectMapper mapper = new ObjectMapper();
			mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

			// Get the filename for this Affirmation as a hexadecimal hash of the affirmation
			// Note the affirmation does *not* have a CDM global key
			String filename = DigestUtils.sha256Hex(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(affirmation));
			
			 //Create an Algorand Transaction. Commit the filename and lineage.
	        Transaction transaction = null;
	        try {
        		transaction = NotesTransaction.commitNotes("{FileName: " + filename + "," + "Lineage: " + mapper.writerWithDefaultPrettyPrinter().writeValueAsString(affirmation.getLineage()) + "}");

	        }
	        catch(Exception e){
	        	e.printStackTrace();
	        	System.out.println("Could not commit Algorand transaction");
	        	return;
	        }

	        if(transaction != null){
	        	//If we committed the transaction, then save the link to the transaction and the 
        	// details of the Affirmation to local storage
	        	String txID = transaction.getTx();
	        	AffirmationTransaction affirmationTransaction = new AffirmationTransaction(affirmation,txID);
	        	String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(affirmationTransaction);
				ReadAndWrite.writePrettyJSON("./UserDirectories/"+this.userHash+"/Affirmations/"+filename+".json",json);
        
	        }


	}

	public static ArrayList<String> createAlgorandAccount() throws Exception{
			//Creates an Algorand account, including a public key and a secret passphrase

            Account act = new Account();
            
            //Get the new account address
            Address addr = act.getAddress();
            
            //Get the backup phrase
            String backup = act.toMnemonic();
            ArrayList<String> result = new ArrayList<String>();
            result.add(addr.toString());
            result.add(backup);
            return result;

	}


	public static void main(String [] args) throws Exception{
		// Testing code for the User class
		// Create a user from a JSON object representing a CDM party

		String partyObject = ReadAndWrite.readFile("./Files/PartyTest.json");
		System.out.println(partyObject);
		ObjectMapper rosettaObjectMapper = RosettaObjectMapper.getDefaultRosettaObjectMapper();
		Party party = rosettaObjectMapper.readValue(partyObject, Party.class);
		User user = getOrCreateUser(party);
	}


}