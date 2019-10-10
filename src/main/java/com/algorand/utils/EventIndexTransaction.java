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

public class EventIndexTransaction{
	public String globalKey;
	public String type;
	public String senderKey;

	public EventIndexTransaction(){}

	public EventIndexTransaction(String globalKey, String type, String senderKey){
		this.globalKey = globalKey;
		this.type = type;
		this.senderKey = senderKey;
	}

}