import java.io.File;
import java.util.ArrayList;
import java.util.Map;

import org.junit.Test;

import pt.uminho.sysbio.common.database.connector.datatypes.Connection;
import pt.uminho.sysbio.common.database.connector.datatypes.MySQLMultiThread;
import pt.uminho.sysbio.common.transporters.core.compartments.CompartmentResult;
import pt.uminho.sysbio.common.transporters.core.compartments.PSort3;
import pt.uminho.sysbio.common.transporters.core.compartments.WoLFPSORT;
import pt.uminho.sysbio.common.transporters.core.transport.MIRIAM_Data;
import pt.uminho.sysbio.common.transporters.core.transport.reactions.TransportReactionsGeneration;
import pt.uminho.sysbio.common.transporters.core.transport.reactions.containerAssembly.PopulateTransportContainer;
import pt.uminho.sysbio.common.transporters.core.transport.reactions.loadTransporters.LoadTransportersData;


public class TransportersTests {

	@Test
	public void testTransportersLoading() {
		
		MySQLMultiThread msqlmt = new MySQLMultiThread("root", "password", "127.0.0.1", "3306", "database_delete");
		
		TransportReactionsGeneration tre = new TransportReactionsGeneration(msqlmt);
		tre.parseAndLoadTransportersDatabase(new File("D:/OD/WORK/tc_annotation_database.out_checked"),false);
		
	}
	
	
	public void testOrg() throws Exception {
		
		MySQLMultiThread msqlmt = new MySQLMultiThread("root", "password", "127.0.0.1", "3306", "SCE_transporters");
		
		TransportReactionsGeneration t = new TransportReactionsGeneration(msqlmt, true, -1);
		
		Connection conn = new Connection(msqlmt);
		
		LoadTransportersData ltd = new LoadTransportersData(conn.createStatement());

		t.setOrganismsTaxonomyScore(ltd);
		t.setOrigintaxonomy("ATP6V0A1");

		System.out.println("ts "+t.getTaxonomyScore("Q93050")+"\tot");
		
	}
	
	public void runPSort() throws Exception {

		PSort3 pSort3 =new PSort3(true, -1);

		Map<String, CompartmentResult> res = pSort3.addGeneInformation(new File("D:/My Dropbox/WORK/Projecto_PEM/reu/PSort/psortb-results_extr2.txt"));

		for(CompartmentResult p : res.values()) {

			System.out.println(p.getGeneID());
		}
	}
	
	
	public void test() throws Exception {
		

		Connection conn = new Connection("127.0.0.1", "3306", "SCE_tranporters", "root", "password");
		
		PopulateTransportContainer p = new PopulateTransportContainer(conn);
		
		System.out.println(p.getOriginTaxonomy(4932,1));
		

			String n = "h+";

			//List<String> cNames = new ArrayList<String>();
			//		BufferedReader in = new BufferedReader(new FileReader("compounds.txt"));
			//		String str;
			//
			//		while ((str = in.readLine()) != null) {
			//
			//			if(!str.trim().isEmpty()) {
			//
			//				cNames.add(str.trim().replace("__"," ").replace("_","-"));
			//			}
			//		}
			//		in.close();
			//for(String n:cNames) 
			{
				System.out.print(n+"\t");
				String[] codes = MIRIAM_Data.getMIRIAM_codes(n,new ArrayList<String>(), true);
				String[] names = MIRIAM_Data.getMIRIAM_Names(codes[0],codes[1], 0 ,true);
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
		//System.out.println(get_chebi_miriam_child_metabolites(ExternalRefSource.CHEBI.getSourceId(codes[1]),0,new HashMap<String,ChebiER>()).keySet());
	}
	
	/**
	 * @param args
	 */
	public void wolfpSort () {

		//obj.getCompartments("C:/Users/ODias/Desktop/CR382121.faa", "fungi");

		String out = "C:/Users/ODias/Desktop/out.out";
		try {
			WoLFPSORT.getCompartments("fungi","C:/Users/Oscar/Desktop/CP004143.faa", out);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
