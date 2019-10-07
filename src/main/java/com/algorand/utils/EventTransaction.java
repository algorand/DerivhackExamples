package com.algorand.utils;



import java.util.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.isda.cdm.*;


public class EventTransaction{
	public String algorandTransactionID;
	public String algoExplorerLink;
	public Event event;
	public String globalKey;
	public Lineage lineage;

	public EventTransaction(){}
	
	public EventTransaction(Event event,String algorandTransactionID){
		this.event = event;
		this.algorandTransactionID = algorandTransactionID;
		this.algoExplorerLink = "https://testnet.algoexplorer.io/tx/"+algorandTransactionID;
		this.globalKey = event.getMeta().getGlobalKey();
		this.lineage = event.getLineage();
	}

}