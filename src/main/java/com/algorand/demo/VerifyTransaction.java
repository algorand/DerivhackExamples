
package com.algorand.demo;

import com.algorand.utils.*;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.security.KeyPair;
import javax.crypto.SecretKey;

import com.algorand.algosdk.algod.client.model.Transaction;
import org.apache.commons.codec.digest.DigestUtils;


public class VerifyTransaction  {

    public static void main(String [] args) throws Exception{
        
        //Read CDM file and Transaction File as inputs
        String cdmFile = args[0];
        String transactionFile = args[1];
        String fileContents = ReadAndWrite.readFile(transactionFile );
        String cdmContents = ReadAndWrite.readFile(cdmFile);

        //Recover the transaction ID
        String[] words = fileContents.split(" ");
        String transactionID = words[1];
        System.out.println("Input CDM File: " + cdmFile);
        System.out.println( "Transaction Link: https://testnet.algoexplorer.io/tx/"+
            transactionID);

        //Output the transaction is verified if the hash of CDM file is the same as
        // hash encoded in transaction notes
        if (NotesTransaction.VerifyTransaction(transactionID, cdmContents)){
            System.out.println("Transaction Verified");
        }
        else{
            System.out.println("Transaction not verified");
        }
    }
}
