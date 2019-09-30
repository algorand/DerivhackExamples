
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
import org.json.JSONArray;
import org.json.JSONObject;
import org.isda.cdm.*;
import java.math.BigDecimal;
import org.isda.cdm.metafields.MetaFields;

import com.algorand.algosdk.algod.client.model.Transaction;
import org.apache.commons.codec.digest.DigestUtils;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.*;
import org.isda.cdm.functions.example.services.identification.IdentifierService;
import org.isda.cdm.processor.PostProcessorProvider;
import org.isda.cdm.Party;
import org.isda.cdm.Account;
import org.isda.cdm.AllocationPrimitive;
import org.isda.cdm.AllocationPrimitive.AllocationPrimitiveBuilder;
import org.isda.cdm.Trade.TradeBuilder;
import org.isda.cdm.Trade;
import org.isda.cdm.ContractualProduct.ContractualProductBuilder;
import org.isda.cdm.Contract.ContractBuilder;
import org.isda.cdm.TradeDate.TradeDateBuilder;
import com.rosetta.model.lib.records.DateImpl;
import org.isda.cdm.EconomicTerms.EconomicTermsBuilder;
import org.isda.cdm.Cashflow.CashflowBuilder;
import org.isda.cdm.Money.MoneyBuilder;
import org.isda.cdm.Payout.PayoutBuilder;
import org.isda.cdm.PayerReceiver.PayerReceiverBuilder;
import com.google.inject.Inject;
import com.google.inject.Guice;
import com.google.inject.Injector;

import org.isda.cdm.metafields.FieldWithMetaString.FieldWithMetaStringBuilder;
import org.isda.cdm.metafields.FieldWithMetaDate.FieldWithMetaDateBuilder;
import org.isda.cdm.metafields.FieldWithMetaDate;
import org.isda.cdm.metafields.ReferenceWithMetaParty.ReferenceWithMetaPartyBuilder;
import org.isda.cdm.metafields.ReferenceWithMetaAccount.ReferenceWithMetaAccountBuilder;
import org.isda.cdm.Party.PartyBuilder;
import org.isda.cdm.Account.AccountBuilder;
import org.isda.cdm.Execution.ExecutionBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regnosys.rosetta.common.serialisation.RosettaObjectMapper;
import org.isda.cdm.ExecutionTypeEnum;
import org.isda.cdm.Quantity.QuantityBuilder;
import org.isda.cdm.Product.ProductBuilder;
import org.isda.cdm.Security.SecurityBuilder;
import org.isda.cdm.rosettakey.SerialisingHashFunction;
import org.isda.cdm.Identifier;
import org.isda.cdm.ActionEnum;
import org.isda.cdm.metafields.FieldWithMetaString;
import org.isda.cdm.Account.AccountBuilder;
import org.isda.cdm.metafields.*;
import org.isda.cdm.PartyRole.PartyRoleBuilder;
import org.isda.cdm.Price.PriceBuilder;

import org.isda.cdm.Security.SecurityBuilder;
import org.isda.cdm.Bond.BondBuilder;
import org.isda.cdm.ProductIdentifier.ProductIdentifierBuilder;
import org.isda.cdm.Event.EventBuilder;
import org.isda.cdm.EventEffect.EventEffectBuilder;

import org.isda.cdm.metafields.ReferenceWithMetaEvent.ReferenceWithMetaEventBuilder;
import org.isda.cdm.metafields.ReferenceWithMetaExecution.ReferenceWithMetaExecutionBuilder;

import org.isda.cdm.Identifier.IdentifierBuilder;
import org.isda.cdm.AssignedIdentifier.AssignedIdentifierBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regnosys.rosetta.common.serialisation.RosettaObjectMapper;
import org.isda.cdm.Trade.TradeBuilder;
import org.isda.cdm.PrimitiveEvent.PrimitiveEventBuilder;
import org.isda.cdm.AllocationOutcome.AllocationOutcomeBuilder;
import java.time.ZonedDateTime;

import org.isda.cdm.EventTimestamp.EventTimestampBuilder;

import org.isda.cdm.AllocationBreakdown;
import org.isda.cdm.AllocationBreakdown.AllocationBreakdownBuilder;

import org.isda.cdm.AllocationInstructions;
import org.isda.cdm.AllocationInstructions.AllocationInstructionsBuilder;
import org.isda.cdm.functions.Allocate;
import org.isda.cdm.functions.AllocateImpl;

public  class AffirmationStep {


        private static MetaFields buildMeta(String hash, String externalKey){
            return MetaFields.builder().setGlobalKey(hash).setExternalKey(externalKey).build();

        }
    

        private static Party buildParty(FieldWithMetaString partyId, FieldWithMetaString partyName,String externalKey,SerialisingHashFunction hashFunction){
            PartyBuilder builder = new PartyBuilder();
            builder.addPartyId(partyId)
                .setName(partyName);

            Party party = builder.build();

            String hash = hashFunction.hash(party);
            party = party.toBuilder().setMeta(buildMeta(hash,externalKey)).build();
            return party;

        }



    public static void main(String [] args) throws Exception{
        ObjectMapper rosettaObjectMapper = RosettaObjectMapper.getDefaultRosettaObjectMapper();
        SerialisingHashFunction hashFunction = new SerialisingHashFunction();
        //Read the input arguments and read them into files
        String allocationFile = args[0];
        String allocationCDM = ReadAndWrite.readFile(allocationFile);

         //Read the executions CDM into a CDM object using the Rosetta object mapper
        Event allocationEvent = rosettaObjectMapper
                .readValue(allocationCDM, Event.class);

        int numTrades = allocationEvent.getPrimitive().getAllocation().get(0).getAfter().getAllocatedTrade().size();
        for(int tradeNumber = 0; tradeNumber < numTrades; tradeNumber++){       
            Affirmation affirmation = new AffirmImpl().doEvaluate(allocationEvent,tradeNumber).build();
            String json = rosettaObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(affirmation);
            ReadAndWrite.writeFile("./Files/Affirmation_"+String.valueOf(tradeNumber)+".json",json);
            CommitAffirmation.commitAffirmation(affirmation);            
        }
    }
}


        //Read the details of the Allocation from the Allocation JSON file
        //For this demo, we will only use the first allocation that we read
        /*
        JSONObject jb = jArray.getJSONObject(0);
        Integer DH_TradeID = (Integer) jb.get("DH_TradeID");
        JSONArray allocations = (JSONArray) jb.get("Allocations");
        for(int allocation_idx = 0; allocation_idx < allocations.length(); allocation_idx++ ){
               
                JSONObject allocation = allocations.getJSONObject(allocation_idx);
                BigDecimal quantity = new BigDecimal((Double) allocation.get("Quantity"));
                JSONObject partyJSON =  (JSONObject) ( (JSONObject) (allocation.get("ClientAccount"))).get("Party");
                JSONObject accountJSON = (JSONObject) ( (JSONObject) ( allocation.get("ClientAccount"))).get("Account");

                AccountBuilder accountBuilder = new AccountBuilder();
                PartyBuilder partyBuilder = new PartyBuilder();

                Account account = accountBuilder.
                        setAccountName(
                            new FieldWithMetaStringBuilder().
                                setValue((String) accountJSON.get("accountName") ).
                            build()).
                        setAccountNumber(
                            new FieldWithMetaStringBuilder().
                                setValue((String)accountJSON.get("accountNumber")  ).
                            build()).
                build();

                Party party = partyBuilder.
                            setAccount(account).
                            setName(
                            new FieldWithMetaStringBuilder().
                                setValue((String)accountJSON.get("accountNumber")  ).
                             build()).
                build();

                String hash = hashFunction.hash(party);
                party = party.toBuilder().setMeta(MetaFields.builder().setGlobalKey(hash).build()).build();
                party = party.toBuilder().setMeta(MetaFields.builder().setExternalKey(hash).build()).build();


                AllocationPrimitive allocationPrimitive = buildAllocationPrimitive(quantity,party,account,identifierService);
                //System.out.println(allocationPrimitive.toString());

                ObjectMapper rosettaObjectMapper = RosettaObjectMapper.getDefaultRosettaObjectMapper();

                String json = rosettaObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(allocationPrimitive);

                //System.out.println("Serialise to JSON");
                System.out.println(json);
            }
        }
        //Map<String,Object> objectMap;
        //List<Map<String,Object>> objectList = ObjectMapper.readValue(fileContents, new TypeReference<List<Map<String,Object>>>(){});
        //for(int i = 0; i < objectList.size(); i++){
         //   objectMap = objectList.get(i);
         //   System.out.println(objectMap.keySet().toString());
         //   System.out.println(objectMap.values().toString());


        
        */
 
/*


        String contract = req.getParameter("editTemplate").toString();
        Transaction txdetails = null;
        try {
            // KeyPair generateKeyPair = CryptographyUtil.generateKeyPair();
            // SecretKey secKey = CryptographyUtil.generateSymmetricKey();

            // byte[] encrypted_contract = CryptographyUtil.symmetricEncrypt(secKey, contract.getBytes());

            // byte[] publicKey = generateKeyPair.getPublic().getEncoded();
            // byte[] privateKey = generateKeyPair.getPrivate().getEncoded();

            // byte[] encryptedKey = CryptographyUtil.encrypt(publicKey, secKey.getEncoded());

            // String message = new String(encrypted_contract) + " | " + new String(encryptedKey);
            // System.out.println(message);
            String message = DigestUtils.sha256Hex(contract)+"";
            System.out.println("The hashcode is: " + message);
            txdetails = NotesTransaction.commitNotes(message);

            String txlink = "https://testnet.algoexplorer.io/tx/"+txdetails.getTx();
            req.setAttribute("txdetails", txdetails.toString());
            req.setAttribute("txlink", txlink);
            RequestDispatcher view = req.getRequestDispatcher("committed.jsp");
            view.forward(req, resp);

        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }
}
*/
