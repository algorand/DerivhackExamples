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
import java.util.stream.Collectors;
import java.util.stream.*;

import com.fasterxml.jackson.databind.ObjectMapper;


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

            //Get the secret key for the faucet account
            String faucetSecret =  "only atom opera jealous obscure fade drama bicycle near cable company other hazard math argue anxiety corn approve crumble trust hunt cattle parent ability raw";
            Account faucetAccount = new Account(faucetSecret);
            System.out.println(faucetAccount.getAddress());
            //Give the account some algos from the faucet account
            signAndSubmit(Globals.ALGOD_API_ADDR,Globals.ALGOD_API_TOKEN, faucetSecret, act.getAddress().toString(), new byte [1024],BigInteger.valueOf(10000000));            

            return act;
    }


    /**
    * Recover Account
    *
    */
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
            String senderSecret, String receiverAddress, byte[] notes, BigInteger amount)  {
       
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
        SignedTransaction signedTx = null;
        try {
        Account src = new Account(senderSecret);
        BigInteger lastRound = firstRound.add(BigInteger.valueOf(1000)); // 1000 is the max tx window
        //Setup Transaction
        Transaction tx = new Transaction(src.getAddress(),  BigInteger.valueOf(1000), firstRound, lastRound, notes, amount, new Address(receiverAddress), genId, genesisHash);
      
        // Sign the Transaction
        signedTx = src.signTransaction(tx);

        // send the transaction to the network
        
            // Msgpack encode the signed transaction
            byte[] encodedTxBytes = Encoder.encodeToMsgPack(signedTx);
            TransactionID id = algodApiInstance.rawTransaction(encodedTxBytes);
            System.out.println("Successfully sent tx with id: " + id);
        } catch (ApiException e) {
            // This is generally expected, but should give us an informative error message.
            System.err.println("Exception when calling algod#rawTransaction: " + e.getResponseBody());
            System.out.println("Transaction note: "  + new String(notes) );
        }
        catch(Exception e){
           e.printStackTrace();
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
                    System.out.println(signedTx.transactionID);
                    System.out.println("Round: " + b3.getRound());
                    System.out.println("Waiting for confirmation... (pool error, if any:)" + b3.getPoolerror());
                    Thread.sleep(500);
                }
            } catch (ApiException | InterruptedException e) {
                
                System.err.println("Exception when calling algod#pendingTxInformation: " + e.getMessage());
                break;
            }

        }
        return b3;
    }


    /**
     * Sign and Submit a transaction example
     * Note in most cases you will split these operations
     * where signing occurs on an offline machine 
     */
    public static List<com.algorand.algosdk.algod.client.model.Transaction> signStringTransaction(String address, String token,
                String senderSecret, String receiverAddress, String notes, String indexNotesPrelude) throws Exception {
        
            List<com.algorand.algosdk.algod.client.model.Transaction> result = new ArrayList<com.algorand.algosdk.algod.client.model.Transaction>();
            // Convert the notes into bytes
            byte[] notesBytes = notes.getBytes();
            
            // The constant 1024 is the maximum number of bytes in an Algorand transaction 
            byte [][] notesChunks = splitBytes(notesBytes,1024);



            List<com.algorand.algosdk.algod.client.model.Transaction> transactions = 
                Arrays.stream(notesChunks)
                      .map(chunk -> signAndSubmit(address,token,senderSecret,receiverAddress,chunk,BigInteger.valueOf(1000)))
                      .collect(Collectors.toList());               
            
            String[] transactionIDs = 
                transactions.stream()
                            .map(tx -> tx.getTx())
                            .toArray(String[]::new);

            String [][] transactionIDLists = splitStrings(transactionIDs,10);
            int page = 0;
            String indexNotes = null;
           
            com.algorand.algosdk.algod.client.model.Transaction indexTransaction = null;
            for(String [] transactionIDList: transactionIDLists){
                indexNotes = indexNotesPrelude + "\"page\": " + String.valueOf(page) +
                                                ", \"transactions\":[ \""  + String.join("\",\"",transactionIDList) + "\"]}" ;
                
                indexTransaction = signAndSubmit(address,token,senderSecret,receiverAddress,indexNotes.getBytes(),BigInteger.valueOf(1000));
                result.add(indexTransaction);
                page = page + 1;
            }

            return result;
            //indexNotes = indexNotes + "transactions:[" + transactionLinks + "]}";

            //indexTransaction = signAndSubmit(address,token,senderSecret,receiverAddress,indexNotes.getBytes(),BigInteger.valueOf(1000));
            //return indexTransaction;
        
        }
        /** readTransaction
         * Read the transaction
         **/
        public static com.algorand.algosdk.algod.client.model.Transaction readTransaction(String address, String token, String transactionID){
        
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

    public static String readStringTransaction(String algorandPassphrase,String type){
        List<com.algorand.algosdk.algod.client.model.Transaction> transactions = null;
        try{
        transactions = 
            AlgorandUtils.getTransactions(Globals.ALGOD_API_ADDR, Globals.ALGOD_API_TOKEN, algorandPassphrase);
        }
        catch(Exception e){
            e.printStackTrace();
        }
        List<IndexTransaction> indexTransactions = new ArrayList<IndexTransaction>();
        ObjectMapper mapper = new ObjectMapper();
        
        for(com.algorand.algosdk.algod.client.model.Transaction transaction: transactions){
            try{
                String notes = new String(transaction.getNoteb64());
                IndexTransaction indexTransaction = mapper.readValue(notes,IndexTransaction.class);
                indexTransactions.add(indexTransaction); 
            }
            catch(java.io.IOException e){
                
                continue;
            }

        }
        
        Collections.sort(indexTransactions);
        String outputJSON = "";
        for(IndexTransaction indexTransaction: indexTransactions){
            List<String> transactionIDs = indexTransaction.transactions;
            for(String transactionID: transactionIDs){
                com.algorand.algosdk.algod.client.model.Transaction outputTransaction = AlgorandUtils.
                        readTransaction(Globals.ALGOD_API_ADDR, Globals.ALGOD_API_TOKEN, transactionID);
                outputJSON += new String(outputTransaction.getNoteb64());
            }


        }
        return outputJSON;

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

    public static String[][] splitStrings(final String[] data, final int chunkSize)
        {
          final int length = data.length;
          final String[][] dest = new String[(length + chunkSize - 1)/chunkSize][];
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




