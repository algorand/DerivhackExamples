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
import com.fasterxml.jackson.core.type.TypeReference;
import com.regnosys.rosetta.common.serialisation.RosettaObjectMapper;

import com.algorand.algosdk.algod.client.model.Transaction;
import org.apache.commons.codec.digest.DigestUtils;

import com.google.common.collect.*;


public class User{
	public String globalKey;
	public String algorandID;
	public String algorandPassphrase;
	public String name;
	public Party party;
	public String userHash;

	public User( Party party ) throws Exception{
				ObjectMapper mapper = new ObjectMapper();

				ArrayList<String> algorandInfo = createAlgorandAccount();
				this.algorandID = algorandInfo.get(0);
				this.algorandPassphrase = algorandInfo.get(1);
				this.party = party;
				this.globalKey = party.getMeta().getGlobalKey();
				this.name = party.getName().getValue();
				this.userHash = DigestUtils.sha256Hex(mapper.writeValueAsString(party));
	}

	public static User getOrCreateUser(Party party) throws Exception{
		ObjectMapper mapper = new ObjectMapper();
		String partyHash = DigestUtils.sha256Hex(mapper.writeValueAsString(party));
	

		File[] usersArray = new File("./Users/").listFiles(File::isDirectory);
		List<File> userList = new ArrayList<File>(Arrays.asList(usersArray));
		File userFile;

		try{
		 userFile = userList.stream()
							.filter(x -> x.getName().equals(partyHash + ".json"))
							.collect(MoreCollectors.onlyElement());
		
		 String userFileContent = ReadAndWrite.readFile("./Users/" + partyHash + ".json");
		 User readUser = mapper.readValue(userFileContent, User.class);
		 return readUser;
		}
		catch(NoSuchElementException e){
				User user =  new User(party); 

				String json = mapper.writeValueAsString(user);
				ReadAndWrite.writePrettyJSON("./Users/"+partyHash+".json",json);
				Files.createDirectories(Paths.get("./UserDirectories/"+partyHash+"/Events/"));
				Files.createDirectories(Paths.get("./UserDirectories/"+partyHash+"/Affirmations/"));

				return user;
		}
		catch(IllegalArgumentException e){
				System.out.println("More than one user by that key");
				e.printStackTrace();
				return null;
			}
			

	}



	
		


	public void commitEvent(Event event) throws Exception{
		String eventKey = event.getMeta().getGlobalKey();
		ObjectMapper mapper = new ObjectMapper();

		String filename = DigestUtils.sha256Hex(mapper.writeValueAsString(event));

		 //Create an Algorand Transaction
        Transaction transaction = null;
        try {
        	transaction = NotesTransaction.commitNotes("{File Name: " + filename + "," + "Lineage: " + mapper.writeValueAsString(event.getLineage())+"}");
        }
        catch(Exception e){
        	e.printStackTrace();
        	System.out.println("Could not commit Algorand transaction");
        	return;
        }

        if(transaction != null){
        	String txID = transaction.getTx();
        	EventTransaction eventTransaction = new EventTransaction(event,txID);
        	String json = mapper.writeValueAsString(eventTransaction);
        	ReadAndWrite.writePrettyJSON("./UserDirectories/"+this.userHash+"/Events/"+filename+".json",json);
        }


	}

		public void commitAffirmation(Affirmation affirmation) throws Exception{
			ObjectMapper mapper = new ObjectMapper();

			String filename = DigestUtils.sha256Hex(mapper.writeValueAsString(affirmation));
			 //Create an Algorand Transaction
	        Transaction transaction = null;
	        try {
        		transaction = NotesTransaction.commitNotes("{File Name: " + filename + "," + "Lineage: " + mapper.writeValueAsString(affirmation.getLineage()) + "}");

	        }
	        catch(Exception e){
	        	e.printStackTrace();
	        	System.out.println("Could not commit Algorand transaction");
	        	return;
	        }

	        if(transaction != null){
	        	String txID = transaction.getTx();
	        	AffirmationTransaction affirmationTransaction = new AffirmationTransaction(affirmation,txID);
	        	String json = mapper.writeValueAsString(affirmationTransaction);
				ReadAndWrite.writePrettyJSON("./UserDirectories/"+this.userHash+"/Affirmations/"+filename+".json",json);
        
	        }


	}

	public static ArrayList<String> createAlgorandAccount() throws Exception{
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

	/*public static void createMongoAccount(MongoDatabase db, String username, String password){
		  Map<String, Object> commandArguments = new HashMap<>();
		  commandArguments.put("createUser", username);
		  commandArguments.put("pwd", password);
		  String[] roles = { "readWrite" };
		  commandArguments.put("roles", roles);
		  BasicDBObject command = new BasicDBObject(commandArguments);
		  db.command(command);
		  
	}*/

	public static void main(String [] args) throws Exception{


		String partyObject = ReadAndWrite.readFile("./Files/PartyTest.json");
		System.out.println(partyObject);
		ObjectMapper rosettaObjectMapper = RosettaObjectMapper.getDefaultRosettaObjectMapper();
		Party party = rosettaObjectMapper.readValue(partyObject, Party.class);
		User user = getOrCreateUser(party);
	}


}