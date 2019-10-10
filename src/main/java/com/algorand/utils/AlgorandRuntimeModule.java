package com.algorand.utils;

import org.isda.cdm.CdmRuntimeModule;
import org.isda.cdm.functions.AllocateImpl;
import org.isda.cdm.functions.Allocate;
import org.isda.cdm.functions.NewAllocationPrimitive;
import org.isda.cdm.functions.NewAllocationPrimitiveImpl;


public class AlgorandRuntimeModule extends CdmRuntimeModule {
	
	@Override
	public void configure() {
		super.configure();
		bind(Allocate.class).to(AllocateImpl.class);
		bind(NewAllocationPrimitive.class).to(NewAllocationPrimitiveImpl.class);
	}
}
