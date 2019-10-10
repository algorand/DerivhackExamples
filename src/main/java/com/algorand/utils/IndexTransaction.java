package com.algorand.utils;



import java.util.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.isda.cdm.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.*;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

public class IndexTransaction implements Comparable<IndexTransaction>{
	public List<String> transactions;
	public String globalKey;
	public String type;
	public String senderKey;
	public int page;

	@Override
    public int compareTo(IndexTransaction other){
    	if(this.page < other.page){
    		return -1;
    	}
    	else if(this.page > other.page){
    		return 1;
    	}
    	else{
    		return 0;
    	}
    }
	public IndexTransaction(){}

	public IndexTransaction(String globalKey, String type, String senderKey,List<String> algorandTransactionIDs, int page){
		this.globalKey = globalKey;
		this.transactions = algorandTransactionIDs;
		this.type = type;
		this.senderKey = senderKey;
		this.page = page;
	}

}