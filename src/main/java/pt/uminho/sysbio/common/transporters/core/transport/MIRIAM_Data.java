/**
 * 
 */
package pt.uminho.sysbio.common.transporters.core.transport;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.ws.WebServiceException;

import pt.uminho.sysbio.common.bioapis.externalAPI.ExternalRefSource;
import pt.uminho.sysbio.common.bioapis.externalAPI.chebi.ChebiAPIInterface;
import pt.uminho.sysbio.common.bioapis.externalAPI.chebi.ChebiER;
import pt.uminho.sysbio.common.bioapis.externalAPI.kegg.KeggAPI;
import pt.uminho.sysbio.common.bioapis.externalAPI.sbml_semantics.SemanticSbmlAPI;
import pt.uminho.sysbio.common.bioapis.externalAPI.sbml_semantics.SemanticSbmlSearchQueryResult;

/**
 * @author ODias
 *
 */
public class MIRIAM_Data {


	/**
	 * @param metabolite
	 * @param metabolitesToBeVerified 
	 * @param errorCount
	 * @return
	 */
	public static String[] getMIRIAM_codes(String metabolite, List<String> metabolitesToBeVerified, boolean verbose) {

		metabolite = metabolite.replace(" -", "-");

		String[] xRefs = new String[2];

		String[] results = MIRIAM_Data.getKEGGMetabolites(metabolite, 0);

		Set<String> kegg_compounds_collection = new TreeSet<String>();//retrieve all ids

		if(results.length<100) {

			xRefs = MIRIAM_Data.getKEGGData(results, metabolite, kegg_compounds_collection, xRefs, 0);

			if(xRefs[0]!= null && xRefs[1]!= null) {

				return xRefs;
			}
		}

		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		// only reaches here if kegg_id is null or kegg_id is not null but chebi_id s null 
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		xRefs = MIRIAM_Data.getChEBIData(metabolite, xRefs, 0, verbose);

		if(xRefs[0]!= null && xRefs[1]!= null) {

			return xRefs;
		}

		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		// only reaches here if kegg_id or chebi_id s null 
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		//System.out.println("VERIFY COMPOUND "+metabolite);
		metabolitesToBeVerified.add(metabolite);
		xRefs = MIRIAM_Data.getSBMLSemanticsData(metabolite, xRefs, kegg_compounds_collection,0, verbose);

		if(xRefs[0]!= null && xRefs[1]!= null) {

			return xRefs;
		}

		// if more than 100 results on kegg just process them after the other two approaches fail
		if(results.length>100 && xRefs[0]==null && xRefs[1]==null) {

			xRefs = MIRIAM_Data.getKEGGData(results, metabolite, kegg_compounds_collection, xRefs, 0);

			if(xRefs[0]!= null && xRefs[1]!= null) {

				return xRefs;
			}
		}

		return xRefs; 
	}

	/**
	 * @param metabolite
	 * @param counter
	 * @return
	 */
	private static String[] getKEGGMetabolites(String metabolite, int counter) {

		try {

			counter = counter+1;

			return KeggAPI.getCompoundIdsByName(metabolite);
		}
		catch (Exception e) {

			if(counter<10) {

				return getKEGGMetabolites(metabolite, counter++);
			}
			else {

				System.out.println(metabolite);
				e.printStackTrace();
			}
		}
		return new String[0];
	}

	/**
	 * @param results
	 * @param metabolite
	 * @param kegg_compounds_collectionCHEBI
	 * @param xRefs 
	 * @return
	 */
	private static String[] getKEGGData(String[] results, String metabolite, Set<String> kegg_compounds_collection, String[] xRefs, int counter) {

		xRefs[0]=null;
		xRefs[1]=null;

		for (int i = 0; i < results.length; i++) {

			String kegg_miriam_from_kegg_api = ExternalRefSource.KEGG_CPD.getMiriamCode(results[i].replace("cpd:", ""));

			try {

				List<String> list_of_compound_id = KeggAPI.getCompoundByKeggId(results[i].replace("cpd:", "")).getNames();

				//for(int l=0;l<list_of_compound_id.size();l++) {
				for(String syn:list_of_compound_id) {

					//String syn = KEGGAPI.get_compound_by_keggId(results[i].replace("cpd:", "")).getNames().get(l);

					if(syn.replace(";", "").trim().equalsIgnoreCase(metabolite.trim())) {

						xRefs[0]=kegg_miriam_from_kegg_api;
						//l=list_of_compound_id.size();
						i=results.length;
						break;
					}
				}

				if(xRefs[0]==null) {

					kegg_compounds_collection.add(kegg_miriam_from_kegg_api);	
				}
			}
			catch (Exception e) {

				kegg_compounds_collection.remove(kegg_miriam_from_kegg_api);
			}
		}
		return xRefs;
	}


	/**
	 * @param metabolite
	 * @return
	 */
	private static String[] getChEBIData(String metabolite, String xRefs[], int counter, boolean verbose) {

		try {

			String chebi_id = ChebiAPIInterface.getChebiEntityByExactName(metabolite);

			if(chebi_id!=null) {

				xRefs[1]= ExternalRefSource.CHEBI.getMiriamCode(chebi_id);

				String kegg_miriam_from_chebi_id=null;

				try {

					kegg_miriam_from_chebi_id = MIRIAM_Data.getKeggMiriamFromChebiMiriam(xRefs[1]);
				}
				catch (Exception e) {

					//e.printStackTrace();
				}

				if(xRefs[0]==null) {

					if(kegg_miriam_from_chebi_id!=null) {

						try {

							KeggAPI.getCompoundByKeggId(ExternalRefSource.KEGG_CPD.getSourceId(kegg_miriam_from_chebi_id)).getName();
							xRefs[0]=kegg_miriam_from_chebi_id;
							return xRefs;
						}
						catch (NullPointerException e)
						{
							//System.out.print(kegg_id+" is deprecated! & ");
							xRefs[0]=null;
						}
					}
				}
				else {

					if(kegg_miriam_from_chebi_id!=null && !kegg_miriam_from_chebi_id.equals(xRefs[0])) {

						System.out.println(metabolite+" KEGG MIRIAM from KEGG ("+xRefs[0]+") is different from KEGG MIRIAM from ChEBI ("+kegg_miriam_from_chebi_id+"). Going with KEGG.");
					}
					return xRefs;
				}
			}
			return xRefs;
		}
		catch (Exception e) {

			counter = counter+1;

			if(counter<50) {

				if(verbose) {

					System.out.println("Exception ChEBI Data. Trial number: "+counter+". Metabolite "+ metabolite+".");
				}

				if(e.getMessage().contains("Failed to access the WSDL at: http://www.ebi.ac.uk/webservices/chebi/2.0/webservice?wsdl")) {

					try {

						Thread.sleep(600000);

					} catch(InterruptedException ex) {

						Thread.currentThread().interrupt();
					}
				}

				return MIRIAM_Data.getChEBIData(metabolite, xRefs, counter, verbose);
			}
			else {

				if(verbose) { 

					e.printStackTrace();
				}
				else {

					System.err.println("Exception ChEBI Data. Trial number: "+counter+". Metabolite "+ metabolite+".");
				}
			}
		}

		return xRefs;
	}

	/**
	 * @param metabolite
	 * @param xRefs
	 * @param kegg_compounds_collection
	 * @return
	 */
	private static String[] getSBMLSemanticsData(String metabolite, String xRefs[], Set<String> kegg_compounds_collection, int counter, boolean verbose) {

		try {

			SemanticSbmlSearchQueryResult result = SemanticSbmlAPI.searchFirstEntity(metabolite, 1);

			String kegg_miriam_from_semantics_api = null, chebi_miriam_from_semantics_api = null;

			if(result!=null && result.getResults().size()>0 && result.getResults().get(0).getMiriamCodes().size()>0)
			{
				kegg_miriam_from_semantics_api=(SemanticSbmlAPI.getXRef(result.getResults().get(0).getMiriamCodes().get(0), SemanticSbmlAPI.Database.KEGG_Compound));
				try
				{
					KeggAPI.getCompoundByKeggId(ExternalRefSource.KEGG_CPD.getSourceId(kegg_miriam_from_semantics_api)).getName();
				}
				catch (Exception e)
				{
					//System.out.print(kegg_id+" is deprecated! & ");
					kegg_miriam_from_semantics_api=null;
				}

				chebi_miriam_from_semantics_api=(SemanticSbmlAPI.getXRef(result.getResults().get(0).getMiriamCodes().get(0), SemanticSbmlAPI.Database.ChEBI));
				if(xRefs[0]!=null && chebi_miriam_from_semantics_api!=null)
				{
					String kegg_miriam_from_chebi_miriam_from_semantics = MIRIAM_Data.getKeggMiriamFromChebiMiriam(chebi_miriam_from_semantics_api);
					if(kegg_miriam_from_chebi_miriam_from_semantics!=null && kegg_miriam_from_chebi_miriam_from_semantics.equals(xRefs[0]))
					{
						xRefs[1]=chebi_miriam_from_semantics_api;
						return xRefs;
					}
				}

				if(xRefs[0]==null && kegg_miriam_from_semantics_api!=null && kegg_compounds_collection.contains(kegg_miriam_from_semantics_api))
				{
					xRefs[0]=kegg_miriam_from_semantics_api;
					String chebi_miriam_from_kegg_miriam_from_semantics = ChebiAPIInterface.getChebiEntityByName("KEGG "+ExternalRefSource.KEGG_CPD.getSourceId(xRefs[0]),-1);
					if(chebi_miriam_from_kegg_miriam_from_semantics!=null)
					{
						xRefs[1]= ExternalRefSource.CHEBI.getMiriamCode(chebi_miriam_from_kegg_miriam_from_semantics);
						return xRefs;
					}
				}

				if(xRefs[1]!=null && kegg_miriam_from_semantics_api!=null)
				{
					String chebi_miriam_from_kegg_miriam_from_semantics = ChebiAPIInterface.getChebiEntityByName("KEGG "+kegg_miriam_from_semantics_api,-1);
					if(chebi_miriam_from_kegg_miriam_from_semantics!=null && chebi_miriam_from_kegg_miriam_from_semantics.equals(xRefs[1]))
					{
						xRefs[0]=kegg_miriam_from_semantics_api;
						return xRefs;
					}
				}

				if(xRefs[1]==null && chebi_miriam_from_semantics_api!=null)
				{
					xRefs[1] = chebi_miriam_from_semantics_api;
					String kegg_miriam_from_chebi_miriam_from_semantics = MIRIAM_Data.getKeggMiriamFromChebiMiriam(chebi_miriam_from_semantics_api);
					if(xRefs[0] == null && kegg_miriam_from_chebi_miriam_from_semantics!=null)
					{
						try
						{
							KeggAPI.getCompoundByKeggId(ExternalRefSource.KEGG_CPD.getSourceId(kegg_miriam_from_chebi_miriam_from_semantics)).getName();
							xRefs[0]=kegg_miriam_from_chebi_miriam_from_semantics;;
						}
						catch (Exception e) {

							//System.out.print(kegg_id+" is deprecated! & ");
							xRefs[0]=null;
						} 
						return xRefs;
					}
				}

				if(xRefs[0]==null)
				{
					String chebi_id = ChebiAPIInterface.getChebiEntityByName(metabolite,5);
					if(chebi_id!=null)
					{
						xRefs[1]= ExternalRefSource.CHEBI.getMiriamCode(chebi_id);
						String kegg_miriam_from_chebi_id = MIRIAM_Data.getKeggMiriamFromChebiMiriam(xRefs[1]);
						if(kegg_miriam_from_chebi_id!=null)
						{
							xRefs[0]=kegg_miriam_from_chebi_id;
							try
							{
								KeggAPI.getCompoundByKeggId(ExternalRefSource.KEGG_CPD.getSourceId(kegg_miriam_from_chebi_id)).getName();
								xRefs[0]=kegg_miriam_from_chebi_id;
								return xRefs;
							}
							catch (Exception e)
							{
								//System.out.print(kegg_id+" is deprecated! & ");
								xRefs[0]=null;
							}
						}
						return xRefs;
					}
				}
			}
			return xRefs;
		}
		catch (Exception e) {

			counter = counter+1;

			if(counter<100) {

				if(verbose) {

					System.out.println("Exception getSBMLSemanticsData. Trial number: "+counter+". Metabolite "+ metabolite+".");
				}

				return MIRIAM_Data.getSBMLSemanticsData(metabolite, xRefs, kegg_compounds_collection, counter, verbose);
			}
			else {

				if(verbose) { 

					e.printStackTrace();
				}
				else {

					System.err.println("Exception getSBMLSemanticsData. Trial number: "+counter+". Metabolite "+ metabolite+".");
				}
			}
		}
		catch (ExceptionInInitializerError e) {

			if(counter<10) {

				if(verbose) {

					System.out.println("ExceptionInInitializerError getSBMLSemanticsData. Trial number: "+counter+". Metabolite "+ metabolite+".");
				}

				return MIRIAM_Data.getSBMLSemanticsData(metabolite, xRefs, kegg_compounds_collection, counter, verbose);
			}
			else {

				if(verbose) { 

					e.printStackTrace();
				}
				else {

					System.err.println("ExceptionInInitializerError getSBMLSemanticsData. Trial number: "+counter+". Metabolite "+ metabolite+".");
				}
			}
		}
		catch (NoClassDefFoundError e) {

			if(counter<10) {

				if(verbose) {

					System.out.println("NoClassDefFoundError getSBMLSemanticsData. Trial number: "+counter+". Metabolite "+ metabolite+".");
				}

				return MIRIAM_Data.getSBMLSemanticsData(metabolite, xRefs, kegg_compounds_collection, counter, verbose);
			}
			else {

				if(verbose) { 

					e.printStackTrace();
				}
				else {

					System.err.println("NoClassDefFoundError getSBMLSemanticsData. Trial number: "+counter+". Metabolite "+ metabolite+".");
				}
			}
		}
		return xRefs;
	}


	/**
	 * @param result
	 * @param errorCount
	 * @return
	 */
	public static String[] getMIRIAM_Names(String[] result, int errorCount, boolean verbose) {

		String[] names = new String[2];

		try  {

			String kegg_id = result[0], chebi_id=result[1], source_id;
			if(kegg_id!=null) {

				source_id=ExternalRefSource.KEGG_CPD.getSourceId(kegg_id);
				names[0]= KeggAPI.getCompoundByKeggId(source_id).getName().replace(";","");
			}
			if(chebi_id!=null) {

				source_id=ExternalRefSource.CHEBI.getSourceId(chebi_id);
				if(source_id!=null) {

					try {

						ChebiER entity	=	ChebiAPIInterface.getExternalReference(source_id);
						if(entity!=null) {

							names[1]=entity.getName().replace(";","");
						}
					}
					catch (Exception e) {

						if(e.getMessage().contains("BD does not contain the id CHEBI:")) {

							result[1] = null;
							names[1] = null;
						}
						else {

							throw e;
						}
					}
				}
			}
			return names;
		}
		catch (Exception e) {

			errorCount = errorCount+1;

			if(errorCount<30) {

				if(verbose) {

					System.out.println("Exception retrieving name. Trial number: "+errorCount+". Processing: KEGG:"+result[0]+" CHEBI:"+result[1]);
				}

				names=getMIRIAM_Names(result, errorCount, verbose);
			}
			else {

				if(verbose) { 

					e.printStackTrace();
				}
				else {

					System.err.println("Exception retrieving name.  Trial counter: "+errorCount+". Processing: KEGG:"+result[0]+" CHEBI:"+result[1]);
				}
			}
		}
		catch (ExceptionInInitializerError e) {

			if(verbose) { 

				e.printStackTrace();
			}
			else {

				System.err.println("ExceptionInInitializerError Server error! Please try again later.\n");
			}
		}
		catch (NoClassDefFoundError e) {

			if(verbose) { 

				e.printStackTrace();
			}
			else {

				System.err.println("NoClassDefFoundError Server error! Please try again later.\n");
			}
		}

		return null;
	}

	/**
	 * @param chebi_id
	 * @return
	 */
	public static Map<String,ChebiER> get_chebi_miriam_child_metabolites(String chebi_id) {

		Map<String,ChebiER> child_metabolites_map = new HashMap<String,ChebiER>();
		return get_chebi_miriam_child_metabolites(chebi_id,0,child_metabolites_map);
	}


	/**
	 * @param chebi_id
	 * @param errorCount
	 * @param child_metabolites_map
	 * @return
	 */
	private static Map<String,ChebiER> get_chebi_miriam_child_metabolites(String chebi_id,int errorCount, Map<String,ChebiER> child_metabolites_map){

		try {

			ChebiER chebi_entity = ChebiAPIInterface.getChildElements(chebi_id);
			child_metabolites_map.put(chebi_id, chebi_entity);

			Set<String> entity_children = chebi_entity.getFunctional_children();

			if(!entity_children.isEmpty()) {

				for(String child:entity_children) {

					if(!child_metabolites_map.keySet().contains(child)) {

						child_metabolites_map.putAll(MIRIAM_Data.get_chebi_miriam_child_metabolites(child,0,child_metabolites_map));
					}
				}	
			}
		}
		catch (Exception e) {

			errorCount = errorCount+1;

			System.out.println("Child_metabolites trial number: "+errorCount+" processing: "+chebi_id); 
			if(errorCount<50) {

				try {

					Thread.sleep(15000); //miliseconds
				}
				catch (InterruptedException e1){

					Thread.currentThread().interrupt();
				}

				child_metabolites_map=get_chebi_miriam_child_metabolites(chebi_id, errorCount,child_metabolites_map);
			}
			else {

				e.getMessage();
				return child_metabolites_map;
			}
		} 

		return child_metabolites_map;
	}


	/**
	 * @param chebi_miriam
	 * @return
	 * @throws WebServiceException
	 * @throws ChebiWebServiceFault_Exception
	 */
	private static String getKeggMiriamFromChebiMiriam(String chebi_miriam) throws Exception {

		String kegg_chebi_Miriam = null;
		String chebi_id=ExternalRefSource.CHEBI.getSourceId(chebi_miriam);
		String kegg_id = ChebiAPIInterface.getKeggIdRef(chebi_id);
		if(kegg_id==null) {

			ChebiER chebiER= ChebiAPIInterface.getMetabolites(chebi_id, 0);
			chebi_id = chebiER.getId();
			kegg_id = ChebiAPIInterface.getKeggIdRef(chebi_id);
			if(kegg_id!=null) {

				kegg_chebi_Miriam = ExternalRefSource.KEGG_CPD.getMiriamCode(kegg_id);
			}
		}
		else {

			kegg_chebi_Miriam = ExternalRefSource.KEGG_CPD.getMiriamCode(kegg_id);
		}
		return kegg_chebi_Miriam;
	}

	/**
	 * @param child_metabolites
	 * @return

	public static List<String[]> get_compounds_existing_in_KEGG_miriam(Set<String> child_metabolites){
		List<String[]> result = new ArrayList<String[]>();

		for(String chebi_miriam :child_metabolites)
		{
			String chebi_id = ExternalRefSource.CHEBI.getSourceId(chebi_miriam);
			String kegg_id = ChebiAPIInterface.getKeggIdRef(chebi_id);

			if(kegg_id!=null)
			{
				String[] codes = new String[2];
				codes[0]=ExternalRefSource.KEGG_CPD.getMiriamCode(kegg_id);
				codes[1]=chebi_miriam;
				result.add(codes);
			}
			else
			{
				String[] codes = new String[2];
				codes[0]=null;
				codes[1]=chebi_miriam;
				result.add(codes);
			}
		}
		return result;
	}
	 */

	/**
	 * @param args
	 * @throws IOException 
	 * @throws ServiceException 
	 * @throws NullPointerException 
	 * @throws RemoteException 
	 */
	public static void main(String[] args) throws IOException {

		//System.out.println(UniprotAPI.get_uniprot_entry_organism("Q9WUT2", (0)));

		//String[] codes = getMIRIAM_codes("strontium ion",new ArrayList<String>(),true);
		//String[] codes = getMIRIAM_codes("gamma-butyrobetaine",new ArrayList<String>());
		//String[] codes = getMIRIAM_codes("sulfate",new ArrayList<String>());
		//String[] codes = getMIRIAM_codes("beta-glucoside",new ArrayList<String>());
		//String[] codes = getMIRIAM_codes("TPP",new ArrayList<String>());
		//String[] codes = getMIRIAM_codes("TGDG",new ArrayList<String>());
		//String[] codes = getMIRIAM_codes("orthophosphate",new ArrayList<String>());
		//String[] codes = getMIRIAM_codes("ion",new ArrayList<String>());
		//String[] codes = getMIRIAM_codes("Mn2+",new ArrayList<String>());
		//		String[] names = getMIRIAM_Names(codes,0,true);
		//		System.out.println(codes[0]);		
		//		System.out.println(names[0]);
		//		System.out.println(codes[1]);		
		//		System.out.println(names[1]);
		//System.out.println(MIRIAM_Data.get_chebi_miriam_child_metabolites(ExternalRefSource.CHEBI.getSourceId(codes[1])));

		//System.out.println(ChebiAPIInterface.getFunctionalParents("CHEBI:15525",0));
		//System.out.println(ChebiAPIInterface.getChebiEntityByName("long chain fatty acid",5));
		//System.out.println(ChebiAPIInterface.getChebiEntityByName("miltefosine"));
		//System.out.println(ChebiAPIInterface.getChebiEntityByName("fatty acyl-CoA"));
		//System.out.println(ChebiAPIInterface.getChebiEntityByName("KEGG C00009",-1));
		//System.out.println(ChebiAPIInterface.getChebiEntityByName("unknown"));

		//		Map<String, CheAxisFaultbiER> temp = MIRIAM_Data.get_chebi_miriam_child_metabolites("17996");
		//		//System.out.println(temp.size());
		//		//
		//		for(String key: temp.keySet()) {
		//
		//			System.out.println(key+"\t"+temp.get(key).getKegg_id());
		//			for(String child:temp.get(key).getFunctional_children()) {
		//
		//				System.out.println("\t\t"+child+"\t"+temp.get(child).getKegg_id());
		//			}
		//			//MIRIAM_Data.get_compounds_existing_in_KEGG_miriam(temp.get(key).getFunctional_children());
		//		}

		List<String> cNames = new ArrayList<String>();
		//
		BufferedReader in = new BufferedReader(new FileReader("C:/Users/ODias/Desktop/kLactis_compounds.txt"));
		String str;

		while ((str = in.readLine()) != null) {

			if(!str.trim().isEmpty()) {

				cNames.add(str.trim().replace("__"," ").replace("_","-"));
			}
		}
		in.close();

		//	String n = "ATP";

		for(String n:cNames) 
		{

			String[] codes = getMIRIAM_codes(n,new ArrayList<String>(), true);
			String[] names = getMIRIAM_Names(codes,0 ,true);
			System.out.print(n+"\t");
			if(codes[0]!=null) {

				System.out.print(codes[0].replace("urn:miriam:kegg.compound:",""));		
			}
			System.out.print("\t");		
			System.out.print(names[0]+"\t");

			if(codes[1]!=null) {

				System.out.print(codes[1].replace("urn:miriam:obo.chebi:CHEBI:",""));		
			}
			System.out.print("\t");
			System.out.print(names[1]);
			System.out.println();

			//System.out.println(MIRIAM_Data.get_chebi_miriam_child_metabolites(codes[1].replace("urn:miriam:obo.chebi:CHEBI:","")));
		}

	}

	//System.out.println(get_chebi_miriam_child_metabolites(ExternalRefSource.CHEBI.getSourceId(codes[1]),0,new HashMap<String,ChebiER>()).keySet());

}
