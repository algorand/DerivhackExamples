package com.algorand.utils;
import com.algorand.algosdk.account.Account;
import com.algorand.algosdk.crypto.Address;
import com.algorand.algosdk.algod.client.AlgodClient;

import com.algorand.algosdk.algod.client.ApiException;
import com.algorand.algosdk.algod.client.api.AlgodApi;
import com.algorand.algosdk.algod.client.auth.ApiKeyAuth;
import com.algorand.algosdk.algod.client.model.*;

import com.algorand.algosdk.account.Account;

import com.algorand.algosdk.algod.client.model.TransactionID;
import com.algorand.algosdk.algod.client.model.TransactionParams;
import com.algorand.algosdk.crypto.Address;
import com.algorand.algosdk.crypto.Digest;
import com.algorand.algosdk.transaction.SignedTransaction;
import com.algorand.algosdk.transaction.Transaction;
import com.algorand.algosdk.util.Encoder;

import java.math.BigInteger;
import java.util.*;
import org.threeten.bp.LocalDate;

public class AlgorandUtils 
{

    /**
     * Create Account
     *
     */
    public static Account createAccount() throws Exception {
            //Create a random new account
            Account act = new Account();
            return act;

            //Give the account some algos from the faucet account
            

    }


    /**
    * Recover Account
    *
    **/
    public static Account recoverAccount(String backupString){
            Account recoveredAccount = null;
            try{
                 recoveredAccount = new Account(backupString);
            }
            catch (java.security.GeneralSecurityException e) {
                System.err.println("Exception when calling recoverAccount");
                e.printStackTrace();
            }

            return recoveredAccount;
    }




    /**
     * Get Block Example
     *
     */
    public static Block getBlock(String address,String token,BigInteger round) throws Exception {
        
  

        //Create an instance of the algod API client
        AlgodClient client = (AlgodClient) new AlgodClient().setBasePath(address);
        ApiKeyAuth api_key = (ApiKeyAuth) client.getAuthentication("api_key");
        api_key.setApiKey(token);
        AlgodApi algodApiInstance = new AlgodApi(client); 
        // Get the lastest Block
        Block blk = null;
        try {
            NodeStatus status = algodApiInstance.getStatus();
            //Get block for the latest round
             blk = algodApiInstance.getBlock(round);
        } catch (ApiException e) {
            System.err.println("Exception when calling algod#getStatus or getBlock");
            e.printStackTrace();
        }
        return blk;
    }








    /**
     * Sign and Submit a transaction example
     * Note in most cases you will split these operations
     * where signing occurs on an offline machine 
     */
    public static com.algorand.algosdk.algod.client.model.Transaction signAndSubmit(String address, String token,
            String senderSecret, String receiverAddress, byte[] notes, BigInteger amount) throws Exception {
       
        AlgodClient client = (AlgodClient) new AlgodClient().setBasePath(address);
        ApiKeyAuth api_key = (ApiKeyAuth) client.getAuthentication("api_key");
        api_key.setApiKey(token);
        AlgodApi algodApiInstance = new AlgodApi(client);


        // get last round and suggested tx fee

        //These are just initialization placeholders
        // suggestedFeePerByte and firstRound are initialized in
        // the try block below
        BigInteger suggestedFeePerByte = BigInteger.valueOf(1);
        BigInteger firstRound = BigInteger.valueOf(301);
        String genId = null;
        Digest genesisHash = null;
        try {
            
            // Get suggested parameters from the node
            TransactionParams params = algodApiInstance.transactionParams();
            suggestedFeePerByte = params.getFee();
            firstRound = params.getLastRound();
            // genesisID and genesisHash are optional on testnet, but will be mandatory on release
            // to ensure that transactions are valid for only a single chain. GenesisHash is preferred.
            // genesisID will be deprecated soon.
            genId = params.getGenesisID();
            genesisHash = new Digest(params.getGenesishashb64());

        } catch (ApiException e) {
            System.err.println("Exception when calling algod#transactionParams");
            e.printStackTrace();
        }

        
        // Instantiate the transaction
        Account src = new Account(senderSecret);
        BigInteger lastRound = firstRound.add(BigInteger.valueOf(1000)); // 1000 is the max tx window
        //Setup Transaction
        Transaction tx = new Transaction(src.getAddress(),  BigInteger.valueOf(1000), firstRound, lastRound, notes, amount, new Address(receiverAddress), genId, genesisHash);
      
        // Sign the Transaction
        SignedTransaction signedTx = src.signTransaction(tx);

        // send the transaction to the network
        try {
            // Msgpack encode the signed transaction
            byte[] encodedTxBytes = Encoder.encodeToMsgPack(signedTx);
            TransactionID id = algodApiInstance.rawTransaction(encodedTxBytes);
            System.out.println("Successfully sent tx with id: " + id);
        } catch (ApiException e) {
            // This is generally expected, but should give us an informative error message.
            System.err.println("Exception when calling algod#rawTransaction: " + e.getResponseBody());
        }

        // wait for transaction to be confirmed

        // Initialize the output transaction
        com.algorand.algosdk.algod.client.model.Transaction b3 = null;
        while(true) {
            try {
                //Check the pending tranactions
                b3 = algodApiInstance.pendingTransactionInformation(signedTx.transactionID);
                if (b3.getRound() != null && b3.getRound().longValue() > 0) {
                    System.out.println("Transaction " + b3.getTx() + " confirmed in round " + b3.getRound().longValue());
                    break;
                } else {
                    System.out.println("Waiting for confirmation... (pool error, if any:)" + b3.getPoolerror());
                }
            } catch (ApiException e) {
                System.err.println("Exception when calling algod#pendingTxInformation: " + e.getMessage());
            }
        }
        return b3;
    }


    /**
     * Sign and Submit a transaction example
     * Note in most cases you will split these operations
     * where signing occurs on an offline machine 
     */
    public static com.algorand.algosdk.algod.client.model.Transaction signStringTransaction(String address, String token,
                String senderSecret, String receiverAddress, String notes, String indexNotes) throws Exception {
        

            // Convert the notes into bytes
            byte[] notesBytes = notes.getBytes();
            
            // The constant 1024 is the maximum number of bytes in an Algorand transaction 
            byte [][] notesChunks = splitBytes(notesBytes,1024);

            //Initialize the transactions
            byte [] chunk = new byte[1024];
            com.algorand.algosdk.algod.client.model.Transaction transaction;
            com.algorand.algosdk.algod.client.model.Transaction indexTransaction;

            //Start the for loop
            int numChunks = notesChunks.length;

            //Add transaction data to the index notes
            indexNotes = indexNotes + "transactions: [";
            for(int i = 0; i < numChunks; i++){
                chunk = notesChunks[i];
                transaction = signAndSubmit(address,token,senderSecret,receiverAddress,chunk,BigInteger.valueOf(1000));
                if (i > 0){
                    indexNotes  = indexNotes + ", ";
                }
                indexNotes = indexNotes + "https://testnet.algoexplorer.io/tx/" + transaction.getTx();
            }
            indexNotes  = indexNotes + "]}";

            indexTransaction = signAndSubmit(address,token,senderSecret,receiverAddress,indexNotes.getBytes(),BigInteger.valueOf(1000));
            return indexTransaction;
        }

        /** readTransaction
         * Read the transaction
         **/
        public static com.algorand.algosdk.algod.client.model.Transaction readTransaction(String address, String token, String transactionID, String receiverAddress){
        
            AlgodClient client = (AlgodClient) new AlgodClient().setBasePath(address);
            ApiKeyAuth api_key = (ApiKeyAuth) client.getAuthentication("api_key");
            api_key.setApiKey(token);
            AlgodApi algodApiInstance = new AlgodApi(client);
            com.algorand.algosdk.algod.client.model.Transaction tx = null;
            try {

                tx = algodApiInstance.transaction(transactionID  );

            } catch (ApiException e) {
                e.printStackTrace();
            }
            return tx;
    }



/**
 * Get transactions for an account in the last 1000 blocks
 *
 */
    public static List<com.algorand.algosdk.algod.client.model.Transaction> getTransactions(String address, String token, String secret) throws Exception {
  

        AlgodClient client = (AlgodClient) new AlgodClient().setBasePath(address);
        ApiKeyAuth api_key = (ApiKeyAuth) client.getAuthentication("api_key");
        api_key.setApiKey(token);
        AlgodApi algodApiInstance = new AlgodApi(client);
        TransactionList tList = null;
        // First, get network status
        try {
            NodeStatus status = algodApiInstance.getStatus();
            BigInteger lastRound = status.getLastRound();
            BigInteger maxtx = BigInteger.valueOf(30);
            BigInteger firstRound = lastRound.subtract(BigInteger.valueOf(1000)); // 1000
            Account recoveredAccount = new Account(secret);
            //Get the transactions for the address in the last 1k rounds
            //Note that this call requires that the node is an archival node as we are going back 1k rounds
            LocalDate today = LocalDate.now();
            LocalDate yesterday = today.minusDays(1);

            tList = algodApiInstance.transactions(recoveredAccount.getAddress().toString(), firstRound, lastRound, yesterday, today, maxtx  );

        } catch (ApiException e) {
            e.printStackTrace();
        }
        return tList.getTransactions();

 
    }
    public static byte[][] splitBytes(final byte[] data, final int chunkSize)
        {
          final int length = data.length;
          final byte[][] dest = new byte[(length + chunkSize - 1)/chunkSize][];
          int destIndex = 0;
          int stopIndex = 0;

          for (int startIndex = 0; startIndex + chunkSize <= length; startIndex += chunkSize)
          {
            stopIndex += chunkSize;
            dest[destIndex++] = Arrays.copyOfRange(data, startIndex, stopIndex);
          }

          if (stopIndex < length)
            dest[destIndex] = Arrays.copyOfRange(data, stopIndex, length);

          return dest;
}

}




