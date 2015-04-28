import java.util.ArrayList;

import org.junit.Test;

import pt.uminho.sysbio.common.transporters.core.transport.MIRIAM_Data;


public class TransportersTests {

	@Test
	public void test() {

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

}
