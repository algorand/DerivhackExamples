package com.algorand.utils;



import com.algorand.algosdk.algod.client.AlgodClient;
import com.algorand.algosdk.algod.client.ApiException;
import com.algorand.algosdk.algod.client.api.AlgodApi;
import com.algorand.algosdk.algod.client.auth.ApiKeyAuth;
import com.algorand.algosdk.algod.client.model.NodeStatus;

import com.algorand.algosdk.account.Account;
import com.algorand.algosdk.algod.client.model.Supply;
import com.algorand.algosdk.algod.client.model.TransactionID;
import com.algorand.algosdk.algod.client.model.TransactionParams;
import com.algorand.algosdk.crypto.Address;
import com.algorand.algosdk.crypto.Digest;
import com.algorand.algosdk.kmd.client.KmdClient;
import com.algorand.algosdk.kmd.client.api.KmdApi;
import com.algorand.algosdk.kmd.client.model.APIV1POSTWalletResponse;
import com.algorand.algosdk.kmd.client.model.CreateWalletRequest;
import com.algorand.algosdk.transaction.SignedTransaction;
import com.algorand.algosdk.transaction.Transaction;
import com.algorand.algosdk.util.Encoder;

import java.security.Security;
import java.math.BigInteger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileReader;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.apache.commons.codec.digest.DigestUtils;


import java.io.IOException;
import java.io.File;

import java.util.ArrayList;

import org.isda.cdm.rosettakey.SerialisingHashFunction;
import org.isda.cdm.AllocationPrimitive;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.regnosys.rosetta.common.serialisation.RosettaObjectMapper;

public class NotesTransaction{

    public static com.algorand.algosdk.algod.client.model.Transaction commitNotes(String note) throws Exception {
        if (note == null) {
            return new com.algorand.algosdk.algod.client.model.Transaction();
        }

        final String ALGOD_API_ADDR =  "http://hackathon.algodev.network:9100";
        final String ALGOD_API_TOKEN = "ef920e2e7e002953f4b29a8af720efe8e4ecc75ff102b165e0472834b25832c1";

        AlgodClient client = (AlgodClient) new AlgodClient().setBasePath(ALGOD_API_ADDR);
        ApiKeyAuth api_key = (ApiKeyAuth) client.getAuthentication("api_key");
        api_key.setApiKey(ALGOD_API_TOKEN);
        AlgodApi algodApiInstance = new AlgodApi(client);

        //Using a backup mnemonic to recover the source account to send tokens from
        final String SRC_ACCOUNT = "only atom opera jealous obscure fade drama bicycle near cable company other hazard math argue anxiety corn approve crumble trust hunt cattle parent ability raw";
        final String DEST_ADDR = "KV2XGKMXGYJ6PWYQA5374BYIQBL3ONRMSIARPCFCJEAMAHQEVYPB7PL3KU";
        //if an account drops below the minimum where should the remainding funds be sent
        final String REM_ADDR = "KV2XGKMXGYJ6PWYQA5374BYIQBL3ONRMSIARPCFCJEAMAHQEVYPB7PL3KU";

        // get last round and suggested tx fee
        BigInteger suggestedFeePerByte = BigInteger.valueOf(1);
        BigInteger firstRound = BigInteger.valueOf(301);
        String genId = null;
        Digest genesisHash = null;
        try {
            // Get suggested parameters from the node
            TransactionParams params = algodApiInstance.transactionParams();
            suggestedFeePerByte = params.getFee();
            firstRound = params.getLastRound();
            //System.out.println("Suggested Fee: " + suggestedFeePerByte);
            genId = params.getGenesisID();
            genesisHash = new Digest(params.getGenesishashb64());

        } catch (ApiException e) {
            System.out.println("Exception when calling algod#transactionParams");
            e.printStackTrace();
        }

        // add some notes to the transaction
        byte[] notes = note.getBytes();
           
        // Instantiate the transaction
        Account src = new Account(SRC_ACCOUNT);
        BigInteger amount = BigInteger.valueOf(10000);
        BigInteger lastRound = firstRound.add(BigInteger.valueOf(1000)); // 1000 is the max tx window
        //Setup Transaction
        //Use a fee of 0 as we will set the fee per 
        //byte when we sign the tx and overwrite it
        Transaction tx = new Transaction(src.getAddress(),  BigInteger.valueOf(1000), firstRound, lastRound, notes, amount, new Address(DEST_ADDR), genId, genesisHash);        

        // Sign the Transaction
        SignedTransaction signedTx = src.signTransaction(tx);

        // send the transaction to the network
        try {
            // Msgpack encode the signed transaction
            byte[] encodedTxBytes = Encoder.encodeToMsgPack(signedTx);
            TransactionID id = algodApiInstance.rawTransaction(encodedTxBytes);
            //System.out.println("Successfully sent tx with id: " + id);
        } catch (ApiException e) {
            // This is generally expected, but should give us an informative error message.
            System.out.println("Exception when calling algod#rawTransaction: " + e.getResponseBody());
        }

        // wait for transaction to be confirmed
        while(true) {
            try {
                //Check the pending tranactions
                com.algorand.algosdk.algod.client.model.Transaction b3 = algodApiInstance.pendingTransactionInformation(signedTx.transactionID);
                if (b3.getRound() != null && b3.getRound().longValue() > 0) {
                    System.out.println("Transaction " + b3.getTx() + " confirmed in round " + b3.getRound().longValue());
                    break;
                } else {
                    //System.out.println("Waiting for confirmation... (pool error, if any:)" + b3.getPoolerror());
                }
            } catch (ApiException e) {
                e.printStackTrace();
                System.err.println("Exception when calling algod#pendingTxInformation: " + e.getMessage());
            }
        }
        //Read the transaction
        try {
            com.algorand.algosdk.algod.client.model.Transaction rtx = algodApiInstance.transactionInformation(DEST_ADDR, signedTx.transactionID);
            System.out.println("Transaction information (with notes): " + rtx.toString());
            System.out.println("Decoded notes: [" + new String(rtx.getNoteb64()) + "]");
            return rtx;
        } catch (ApiException e) {
            System.err.println("Exception when calling algod#transactionInformation: " + e.getCode());
        }
        return null;
    }

    public static Boolean VerifyTransaction(String txID,String notes) throws Exception {
        if (txID == null) {
            return false;
        }

        final String ALGOD_API_ADDR = "http://hackathon.algodev.network:9100";
        final String ALGOD_API_TOKEN = "ef920e2e7e002953f4b29a8af720efe8e4ecc75ff102b165e0472834b25832c1";

        AlgodClient client = (AlgodClient) new AlgodClient().setBasePath(ALGOD_API_ADDR);
        ApiKeyAuth api_key = (ApiKeyAuth) client.getAuthentication("api_key");
        api_key.setApiKey(ALGOD_API_TOKEN);
        AlgodApi algodApiInstance = new AlgodApi(client);

        //Using a backup mnemonic to recover the source account to send tokens from
        final String SRC_ACCOUNT = "only atom opera jealous obscure fade drama bicycle near cable company other hazard math argue anxiety corn approve crumble trust hunt cattle parent ability raw";
        final String DEST_ADDR = "KV2XGKMXGYJ6PWYQA5374BYIQBL3ONRMSIARPCFCJEAMAHQEVYPB7PL3KU";
        //if an account drops below the minimum where should the remainding funds be sent
        final String REM_ADDR = "KV2XGKMXGYJ6PWYQA5374BYIQBL3ONRMSIARPCFCJEAMAHQEVYPB7PL3KU";

        //Read the transaction
        try {
            com.algorand.algosdk.algod.client.model.Transaction rtx = algodApiInstance.transactionInformation(DEST_ADDR, txID);
            
            String decodedNotes =  new String(rtx.getNoteb64());
            System.out.println("Transaction Hash: " + decodedNotes);

            //Deserialize input CDM object to Java
            ObjectMapper rosettaObjectMapper = RosettaObjectMapper.getDefaultRosettaObjectMapper();
            AllocationPrimitive allocationPrimitive = rosettaObjectMapper.readValue(notes, AllocationPrimitive.class);

            //Compute global key of object using Rosetta provided hash function
            SerialisingHashFunction hashFunction = new SerialisingHashFunction();
            String message = hashFunction.hash(allocationPrimitive);
            

            System.out.println("Hash of Input File: " + message);
            if (decodedNotes.equals(message)){
                return true;

            }
            else{
                return false;
            }    
                } catch (ApiException e) {
            System.err.println("Exception when calling algod#transactionInformation: " + e.getCode());
            return false;
        }
    }
}