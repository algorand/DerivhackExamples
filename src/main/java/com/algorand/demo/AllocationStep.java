
package com.algorand.demo;
import com.algorand.utils.*;
import com.algorand.algosdk.algod.client.model.Transaction;

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

public  class AllocationStep {


        private static MetaFields buildMeta(String hash, String externalKey){
            return MetaFields.builder().setGlobalKey(hash).setExternalKey(externalKey).build();

        }
    

        private static Party buildParty(FieldWithMetaString partyId, FieldWithMetaString partyName,String externalKey){
            SerialisingHashFunction hashFunction = new SerialisingHashFunction();
            PartyBuilder builder = new PartyBuilder();
            builder.addPartyId(partyId)
                .setName(partyName);

            Party party = builder.build();

            String hash = hashFunction.hash(party);
            party = party.toBuilder().setMeta(buildMeta(hash,externalKey)).build();
            return party;

        }

        private static AllocationBreakdown buildAllocationBreakdown(JSONObject allocationBreakdown){


            BigDecimal quantity = new BigDecimal((Double) allocationBreakdown.get("Quantity"));
            JSONObject clientAccount = (JSONObject) allocationBreakdown.get("ClientAccount");

            JSONObject partyJSON = (JSONObject) clientAccount.get("Party");
            String partyID = (String) partyJSON.get("partyId");
            String partyName = (String) partyJSON.get("name");

            JSONObject accountJSON = (JSONObject) clientAccount.get("Account");
            String accountNumber = (String) accountJSON.get("accountNumber");
            String accountName = (String) accountJSON.get("accountName");

            String role= (String) clientAccount.get("role");

            FieldWithMetaString partyIDWithMeta = new FieldWithMetaStringBuilder().setValue(partyID).build();
            FieldWithMetaString partyNameWithMeta = new FieldWithMetaStringBuilder().setValue(partyName).build();

            FieldWithMetaString accountNumberWithMeta = new FieldWithMetaStringBuilder().setValue(accountNumber).build();
            FieldWithMetaString accountNameWithMeta = new FieldWithMetaStringBuilder().setValue(accountName).build();


        
            Party party = buildParty(partyIDWithMeta,partyNameWithMeta,partyID);
            ReferenceWithMetaParty partyReference = new ReferenceWithMetaPartyBuilder()
                                                         .setGlobalReference(party.getMeta().getGlobalKey())
                                                     .build();

            AllocationBreakdown breakdown = new AllocationBreakdownBuilder()
                                            .setQuantity(
                                                new QuantityBuilder()
                                                    .setAmount(quantity)
                                                .build())
                                            .setPartyReference(partyReference)
                                            .build();
            return breakdown;
        }

        private static AllocationInstructions buildAllocationInstructions(String allocationInstructionsDH){

        //Read the allocation instructions into a JSON array
        JSONArray jArray = new JSONArray(allocationInstructionsDH);

        //TODO: Change this if there are multiple allocations
        JSONObject jObject = (JSONObject) jArray.get(0);
        AllocationInstructionsBuilder instructionsBuilder = new AllocationInstructionsBuilder();
        JSONArray allocationBreakdowns = (JSONArray) jObject.get("Allocations");
        for(int allocation_idx = 0; allocation_idx < allocationBreakdowns.length(); allocation_idx++){
            
            JSONObject allocationBreakdown = (JSONObject) allocationBreakdowns.get(allocation_idx);
            AllocationBreakdown breakdown = buildAllocationBreakdown(allocationBreakdown);
            instructionsBuilder.addBreakdowns(breakdown);            
        }
        return instructionsBuilder.build();

        }

    public static void main(String [] args) throws Exception{
        ObjectMapper rosettaObjectMapper = RosettaObjectMapper.getDefaultRosettaObjectMapper();
        //Read the input arguments and read them into files
        String allocationInstructionFile = args[0];
        String executionsCDMFile = args[1];
        String allocationInstructionsDH = ReadAndWrite.readFile(allocationInstructionFile);
        String executionsCDM = ReadAndWrite.readFile(executionsCDMFile);

         //Read the executions CDM into a CDM object using the Rosetta object mapper
        Event executionEvent = rosettaObjectMapper
                .readValue(executionsCDM, Event.class);
        
       
        Execution execution = executionEvent
                                .getPrimitive()
                                .getExecution()
                                .get(0)
                                .getAfter()  
                                .getExecution(); 

        AllocationInstructions allocationInstructions = buildAllocationInstructions(allocationInstructionsDH);
        Injector injector = Guice.createInjector(new AlgorandRuntimeModule());
        Allocate allocationFunction = injector.getInstance(Allocate.class);
        Event allocationEvent = allocationFunction.evaluate(execution,allocationInstructions);
        List<Party> parties = allocationEvent.getParty();
        
        User user;

        for (Party party: parties){
             user = User.getOrCreateUser(party);
        }

        String json = rosettaObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(allocationEvent);

        System.out.println(json);
    }
}


     