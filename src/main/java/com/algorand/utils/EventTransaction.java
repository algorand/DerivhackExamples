package com.algorand.utils;



import java.util.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.isda.cdm.*;


public class EventTransaction{
	String algorandTransactionID;
	String algoExplorerLink;
	Event event;
	String globalKey;
	Lineage lineage;

	public EventTransaction(Event event,String algorandTransactionID){
		this.event = event;
		this.algorandTransactionID = algorandTransactionID;
		this.algoExplorerLink = "https://testnet.algoexplorer.io/tx/"+algorandTransactionID;
		this.globalKey = event.getMeta().getGlobalKey();
		this.lineage = event.getLineage();
	}

}