package com.algorand.demo;

import com.google.inject.Inject;
import com.rosetta.model.lib.functions.MapperS;
import com.rosetta.model.lib.functions.RosettaFunction;


import com.regnosys.rosetta.common.hashing.*;
import com.rosetta.model.lib.process.PostProcessStep;
import com.rosetta.model.lib.records.DateImpl;
import org.isda.cdm.*;
import org.isda.cdm.AllocationPrimitive.AllocationPrimitiveBuilder;
import org.isda.cdm.metafields.FieldWithMetaString;
import org.isda.cdm.metafields.ReferenceWithMetaParty;
import org.isda.cdm.processor.EventEffectProcessStep;
import static org.isda.cdm.ConfirmationStatusEnum.CONFIRMED;
import static org.isda.cdm.PartyRoleEnum.CLIENT;
import org.isda.cdm.metafields.ReferenceWithMetaEvent.ReferenceWithMetaEventBuilder;
import org.isda.cdm.metafields.ReferenceWithMetaExecution.ReferenceWithMetaExecutionBuilder;



import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import com.google.common.collect.MoreCollectors;

/**
 * Sample Allocate implementation, should be used as a simple example only.
 */
public class ConfirmImpl{

	private final List<PostProcessStep> postProcessors;

	public ConfirmImpl() {
		RosettaKeyProcessStep rosettaKeyProcessStep = new RosettaKeyProcessStep(NonNullHashCollector::new);
		this.postProcessors = Arrays.asList(rosettaKeyProcessStep,
				new RosettaKeyValueProcessStep(RosettaKeyValueHashFunction::new),
				new ReKeyProcessStep(rosettaKeyProcessStep));
	}

	protected  Confirmation.ConfirmationBuilder doEvaluate(Event allocation, int tradeIndex){
		
		// Initialize the AffirmationBuilder
		Confirmation.ConfirmationBuilder confirmationBuilder = new Confirmation.ConfirmationBuilder();
	
		// Get the execution for the given trade index
		Execution execution = allocation
								.getPrimitive()
								.getAllocation().get(0)
								.getAfter()
								.getAllocatedTrade().get(tradeIndex)
								.getExecution();

		// Get client references from the execution
		List<String> clientReferences = execution.getPartyRole()
				.stream()
				.filter(r -> r.getRole() == CLIENT)
				.map(r -> r.getPartyReference().getGlobalReference())
				.collect(Collectors.toList());

		// Get client parties from the allocation
		List<Party> clientParties = allocation.getParty().stream()
				.filter(p -> clientReferences.contains(p.getMeta().getGlobalKey()))
				.collect(Collectors.toList());


		// Get client party's roles from the execution
		List<PartyRole> partyRoles = execution.getPartyRole().stream()
				.filter(p -> clientReferences.contains(p.getPartyReference().getGlobalReference()))
				.collect(Collectors.toList());

		// Get lineage
		String executionKey = execution.getMeta().getGlobalKey();
		String eventKey = allocation.getMeta().getGlobalKey();

		Lineage lineage = new Lineage.LineageBuilder()
							.addEventReference(new ReferenceWithMetaEventBuilder()
								.setGlobalReference(eventKey)
							.build())
							.addExecutionReference(new ReferenceWithMetaExecutionBuilder()
								.setGlobalReference(executionKey)
							.build())
						 .build();

		//Build the confirmation
		confirmationBuilder
			.addIdentifier(getIdentifier("confirmation_"+ String.valueOf(tradeIndex), 1))
			.addParty(clientParties)
			.addPartyRole(partyRoles)
			.setLineage(lineage)
			.setStatus(CONFIRMED);

		// Update keys / references
		postProcessors.forEach(postProcessStep -> postProcessStep.runProcessStep(Confirmation.class, confirmationBuilder));

		return confirmationBuilder;
	}


	private Identifier getIdentifier(String id, int version) {
		return Identifier.builder()
				.addAssignedIdentifierBuilder(AssignedIdentifier.builder()
						.setIdentifier(FieldWithMetaString.builder().setValue(id).build())
				.setVersion(version))
				.build();
	}
}
