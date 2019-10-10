package com.algorand.utils;



import java.util.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.isda.cdm.*;


public class AffirmationTransaction{
	public String algorandTransactionID;
	public String algoExplorerLink;
	public Affirmation affirmation;
	public Lineage lineage;

	public AffirmationTransaction(){}

	public AffirmationTransaction(Affirmation affirmation,String algorandTransactionID){
		this.affirmation = affirmation;
		this.algorandTransactionID = algorandTransactionID;
		this.algoExplorerLink = "https://testnet.algoexplorer.io/tx/"+algorandTransactionID;
		this.lineage = affirmation.getLineage();
	}

}
