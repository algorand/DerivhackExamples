package com.algorand.demo;

import com.algorand.utils.*;
import static org.junit.Assert.assertTrue;
import com.algorand.algosdk.algod.client.AlgodClient;
import com.algorand.algosdk.algod.client.ApiException;
import com.algorand.algosdk.algod.client.api.AlgodApi;
import com.algorand.algosdk.algod.client.auth.ApiKeyAuth;
import com.algorand.algosdk.algod.client.model.Transaction;

import org.junit.Test;

/**
 * Unit tests for NotesTransaction.java
 */
public class NotesTransactionTest 
{
	
    final String ALGOD_API_ADDR = "http://r1.algorand.network:8161";
    final String ALGOD_API_TOKEN = "2b4e2a58208a0f5b624b77cb6b92749a1552f3d34bbc1e007609cde59852729f";

    @Test
    public void transactionWithNotesTest()
    {

	    AlgodClient client = (AlgodClient) new AlgodClient().setBasePath(ALGOD_API_ADDR);
	    ApiKeyAuth api_key = (ApiKeyAuth) client.getAuthentication("api_key");
	    api_key.setApiKey(ALGOD_API_TOKEN);
	    AlgodApi algodApiInstance = new AlgodApi(client);

    	try {
			String notes = "here are some notes";
			Transaction test = NotesTransaction.commitNotes(notes);
	        try {
	            Transaction tx = algodApiInstance.transactionInformation(test.getFrom(), test.getTx());
	            System.out.println("Tx = " + tx.toString());
	            assertTrue(true);
	        } catch (ApiException e) {
	        	e.printStackTrace();
	        	assertTrue(false);
	        }
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    		assertTrue(false);
    	}
    }

    @Test
    public void transactionNullNotesTest()
    {
	    AlgodClient client = (AlgodClient) new AlgodClient().setBasePath(ALGOD_API_ADDR);
	    ApiKeyAuth api_key = (ApiKeyAuth) client.getAuthentication("api_key");
	    api_key.setApiKey(ALGOD_API_TOKEN);
	    AlgodApi algodApiInstance = new AlgodApi(client);

    	try {
    		Transaction test = NotesTransaction.commitNotes(null);
	        try {
	            Transaction tx = algodApiInstance.transactionInformation(test.getFrom(), test.getTx());
	            System.out.println("Tx = " + tx.toString());
	            assertTrue(false);
	        } catch (ApiException e) {
	        	e.printStackTrace();
	        	assertTrue(true);
	        }    	
	    }
    	catch (Exception e) {
    		e.printStackTrace();
    		assertTrue(false);
    	}
    }

}
