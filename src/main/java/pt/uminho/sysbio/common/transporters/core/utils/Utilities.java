package pt.uminho.sysbio.common.transporters.core.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import pt.uminho.sysbio.common.transporters.core.compartments.GeneCompartments;

public class Utilities {
	
	static public Map<String, Set<String>> compartmentGenes(Map<String,GeneCompartments> geneComparments){
		
		Map<String, Set<String>> compartmentGenes = new HashMap<String, Set<String>>();
		
		for(String gene:geneComparments.keySet()){
			
			Set<String> compSet = new HashSet<String>(geneComparments.get(gene).getSecondary_location().keySet());
			System.out.print(gene + "\tsec: " + compSet);
			
			GeneCompartments geneCompartment = geneComparments.get(gene); 
			compSet.add(geneCompartment.getPrimary_location());
			
			System.out.println("\t"+geneCompartment.getPrimary_location()+"\tall: " +compSet);
			
			for(String compartment: compSet){
			
				Set<String> genes = compartmentGenes.get(compartment);
				if(genes== null)
					genes =  new HashSet<String>();
				
				genes.add(geneCompartment.getGeneID());
				compartmentGenes.put(compartment, genes);
			}
		}
		
		return compartmentGenes;
	}

}
