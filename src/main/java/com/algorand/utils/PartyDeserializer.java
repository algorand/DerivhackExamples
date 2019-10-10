package com.algorand.utils;

import org.isda.cdm.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.regnosys.rosetta.common.serialisation.RosettaObjectMapper;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.*;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;


public	class PartyDeserializer extends JsonDeserializer<Party>{
		public PartyDeserializer(){super();};

 	 	@Override
  		public Party deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		    String jsonString = jp.readValueAsTree().toString();
		    ObjectMapper rosettaMapper = RosettaObjectMapper.getDefaultRosettaObjectMapper();
		    return rosettaMapper.readValue(jsonString,Party.class);
		    
  		}
  	}
