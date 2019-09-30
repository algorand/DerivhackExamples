package com.algorand.utils;

import com.algorand.algosdk.account.Account;
import com.algorand.algosdk.crypto.Address;

import org.jongo.Jongo;
import com.mongodb.DB;
import com.mongodb.BasicDBObject;
import org.jongo.MongoCollection;

import java.io.*;
import java.util.*;

import org.isda.cdm.Party;
import org.isda.cdm.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.regnosys.rosetta.common.serialisation.RosettaObjectMapper;

import com.algorand.algosdk.algod.client.model.Transaction;
import org.apache.commons.codec.digest.DigestUtils;

public class User{
	String globalKey;
	String algorandID;
	String algorandPassphrase;
	String name;
	Party party;



	public static User getOrCreateUser(Party party,DB mongoDB){
		String partyKey = party.getMeta().getGlobalKey();
		Jongo jongo = new Jongo(mongoDB);
		MongoCollection users = jongo.getCollection("users");

		User foundUser = users.findOne("{party.meta.globalKey: '" + partyKey + "'}").as(User.class);
		if (foundUser == null){
			return new User(party,mongoDB);
		}
		else{
			return foundUser;
		}
	}

	public User(Party party, String globalKey, String algorandID, String algorandPassphrase, String name){
		this.party = party;
		this.globalKey = globalKey;
		this.algorandID = algorandID;
		this.algorandPassphrase = algorandPassphrase;
		this.name = name;

	}


	public  User(Party party,DB mongoDB){
		try{
				Jongo jongo = new Jongo(mongoDB);
				MongoCollection users = jongo.getCollection("users");

				ArrayList<String> algorandInfo = createAlgorandAccount();
				this.algorandID = algorandInfo.get(0);
				this.algorandPassphrase = algorandInfo.get(1);
				this.party = party;
				this.globalKey = party.getMeta().getGlobalKey();
				this.name = party.getName().getValue();

				//TODO: Right now the password is the party's global key
				//      Set this way to make derivhack prototyping easier
				createMongoAccount(mongoDB,globalKey,globalKey);
				users.save(this);

			}
			catch(Exception e){
				e.printStackTrace();
				this.algorandID = null;
				this.algorandPassphrase = null;
				this.party = null;
				this.globalKey = null;
				this.name = null;
			}

		}
		


	public void commitEvent(Event event) throws Exception{
		String eventKey = event.getMeta().getGlobalKey();
		String message = eventKey;
		 //Create an Algorand Transaction
        Transaction transaction = null;
        try {
        	transaction = NotesTransaction.commitNotes(message);
        }
        catch(Exception e){
        	e.printStackTrace();
        	System.out.println("Could not commit Algorand transaction");
        	return;
        }

        if(transaction != null){
        	String txID = transaction.getTx();
        	EventTransaction eventTransaction = new EventTransaction(event,txID);
        	DB eventDB = MongoUtils.getDatabase(this.party.getName().getValue());
        	Jongo jongo = new Jongo(eventDB);
        	MongoCollection eventCollection = jongo.getCollection("Events");
        	eventCollection.save(eventTransaction);
        }


	}

		public void commitAffirmation(Affirmation affirmation) throws Exception{
			ObjectMapper mapper = new ObjectMapper();

			String message = DigestUtils.sha256Hex(mapper.writeValueAsString(affirmation));
			 //Create an Algorand Transaction
	        Transaction transaction = null;
	        try {
	        	transaction = NotesTransaction.commitNotes(message);
	        }
	        catch(Exception e){
	        	e.printStackTrace();
	        	System.out.println("Could not commit Algorand transaction");
	        	return;
	        }

	        if(transaction != null){
	        	String txID = transaction.getTx();
	        	AffirmationTransaction affirmationTransaction = new AffirmationTransaction(affirmation,txID);
	        	DB eventDB = MongoUtils.getDatabase(this.party.getName().getValue());
	        	Jongo jongo = new Jongo(eventDB);
	        	MongoCollection affirmationCollection = jongo.getCollection("Affirmations");
	        	affirmationCollection.save(affirmationTransaction);
	        }


	}

	public ArrayList<String> createAlgorandAccount() throws Exception{
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

	public static void createMongoAccount(DB db, String username, String password){
		  Map<String, Object> commandArguments = new HashMap<>();
		  commandArguments.put("createUser", username);
		  commandArguments.put("pwd", password);
		  String[] roles = { "readWrite" };
		  commandArguments.put("roles", roles);
		  BasicDBObject command = new BasicDBObject(commandArguments);
		  db.command(command);
		  db.getCollection(username);

	}

	public static void main(String [] args) throws IOException{

		DB mongoDB = MongoUtils.getDatabase("users");

		String partyObject = ReadAndWrite.readFile("./Files/PartyTest.json");
		System.out.println(partyObject);
		ObjectMapper rosettaObjectMapper = RosettaObjectMapper.getDefaultRosettaObjectMapper();
		Party party = rosettaObjectMapper.readValue(partyObject, Party.class);
		User user = getOrCreateUser(party,mongoDB);
	}


}