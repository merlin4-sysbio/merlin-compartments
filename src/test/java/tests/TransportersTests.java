package tests;

import java.io.File;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.uminho.sysbio.common.database.connector.datatypes.Connection;
import pt.uminho.sysbio.common.database.connector.datatypes.DatabaseAccess;
import pt.uminho.sysbio.common.database.connector.datatypes.MySQLDatabaseAccess;
import pt.uminho.sysbio.common.transporters.core.compartments.CompartmentResult;
import pt.uminho.sysbio.common.transporters.core.compartments.ReadPSort3;
import pt.uminho.sysbio.common.transporters.core.compartments.WoLFPSORT;
import pt.uminho.sysbio.common.transporters.core.transport.MIRIAM_Data;
import pt.uminho.sysbio.common.transporters.core.transport.reactions.TransportReactionsGeneration;
import pt.uminho.sysbio.common.transporters.core.transport.reactions.loadTransporters.LoadTransportersData;


public class TransportersTests {

	final static Logger logger = LoggerFactory.getLogger(TransportersTests.class);
	
	public void testTransportersLoading() {

		//DatabaseAccess msqlmt = new MySQLDatabaseAccess("root", "password", "127.0.0.1", "3306", "database_delete");
		//TransportReactionsGeneration tre = new TransportReactionsGeneration(msqlmt);
		//tre.parseAndLoadTransportersDatabase(new File("D:/OD/WORK/tc_annotation_database.out_checked"),false);
	}

	public void testOrg() throws Exception {

		DatabaseAccess msqlmt = new MySQLDatabaseAccess("root", "password", "127.0.0.1", "3306", "SCE_transporters");
		Connection connection = new Connection(msqlmt);
		Statement statment = connection.createStatement();
		TransportReactionsGeneration t = new TransportReactionsGeneration(-1);
		LoadTransportersData ltd = new LoadTransportersData(statment, msqlmt.get_database_type());

		t.setOrganismsTaxonomyScore(ltd);
		t.setOrigintaxonomy("ATP6V0A1");

		connection.closeConnection();
		System.out.println("ts "+t.getTaxonomyScore("Q93050")+"\tot");
	}

	public void runPSort() throws Exception {

		ReadPSort3 readPSort3 =new ReadPSort3(-1);

		Map<String, CompartmentResult> res = readPSort3.addGeneInformation(new File("D:/My Dropbox/WORK/Projecto_PEM/reu/PSort/psortb-results_extr2.txt"));

		for(CompartmentResult p : res.values()) {

			System.out.println(p.getGeneID());
		}
	}


	@Test
	public void test() throws Exception {

		List<String> mets = new ArrayList<>();
		mets.add("h+");
		mets.add("proton");
		mets.add("Zn2+");
		mets.add("zinc");
		mets.add("zinc ion");
		mets.add("acetan");
		mets.add("ferric ion");
		mets.add("ferric iron");
		mets.add("Fe2+");
		mets.add("hop");
		mets.add("sterol");
		mets.add("steroids");
		mets.add("calcium");
		mets.add("HCO3-");
		mets.add("bicarbonate");
		mets.add("ca2+");
		mets.add("small molecules");
		mets.add("small molecule");
		mets.add("3-hydroxy kynurenine");
		mets.add("3-hydroxykynurenine");
		mets.add("Ni2+");
		mets.add("nickel");

		for(String n : mets) {
			
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
		}
	}

	/**
	 * @param args
	 */
	public void wolfpSort () {

		//obj.getCompartments("C:/Users/ODias/Desktop/CR382121.faa", "fungi");

		String out = "C:/Users/ODias/Desktop/out.out";
		try {
			WoLFPSORT.getCompartments("fungi","C:/Users/Oscar/Desktop/CP004143.faa", out);
		}
		catch (Exception e) {

			e.printStackTrace();
		}
	}
	
	
//	public void mainTests() throws Exception {
//
//		LaunchTransportLoad ltl = new LaunchTransportLoad();
//
//		String db_name = "test_transporters"; //transporters database name
//		DatabaseAccess msqlmt = new DatabaseAccess("localhost","3306", db_name,"root","");
//
//		double threshold = 0.2;
//		double alpha = 0.3;
//		int minimalFrequency = 2;
//		double beta = 0.05;
//		boolean validateReaction = true;
//		boolean saveOnlyReactionsWithKEGGmetabolites = false;
//
//
//		String filePrefix = "Th_"+threshold+"__al_"+alpha+"__be_"+beta;
//		String dir = (msqlmt.get_database_name()+"/"+filePrefix+"/reactionValidation"+validateReaction+"/kegg_only"+saveOnlyReactionsWithKEGGmetabolites);
//		String path = FileUtils.getCurrentTempDirectory(dir);
//		String fileName = path + msqlmt.get_database_name() + "__" + filePrefix + ".transContainer";
//
//		TransportContainer transportContainer = ltl.createTransportContainer(msqlmt, alpha, minimalFrequency, beta, threshold, validateReaction, 
//				saveOnlyReactionsWithKEGGmetabolites, fileName, filePrefix, path, 1, true);
//
//		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//		//compartmentalization
//		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//		String psort_db_name = "test_transporters";			//psort dbName
//
//		msqlmt = new DatabaseAccess("localhost","3306", psort_db_name,"root","");
//		Connection conn = new Connection(msqlmt); // connection to psort_db_name
//
//		KINGDOM k;
//
//		CompartmentsInterface obj;
//		///////////////////////////
//
//		//for Eukaryotes
//		k = KINGDOM.Eukaryota;
//
//		String org = "fungi"; //"animal"; "plant";
//		int counter=0;
//
//		WoLFPSORT euk_obj = new WoLFPSORT(conn, ""+counter);
//		String genome_dir ="../transport_systems/test";
//		File genome_files = new File(genome_dir);
//
//		if(genome_files.isDirectory()) {
//
//			for(File genome_file:genome_files.listFiles()) {
//
//				if(genome_file.isFile()) {
//
//					euk_obj.getCompartments(org, genome_file.getAbsolutePath());
//					euk_obj.loadCompartmentsInformation(true);
//					counter++;
//					euk_obj = new WoLFPSORT(conn, ""+counter);
//
//				}
//			}
//		}
//		obj = euk_obj;
//
//		///////////////////////////
//
//		//for bacteria
//		k = KINGDOM.Bacteria;
//		String psort_prediction_file_path=""; // predictions file from psort
//
//		ReadPSort3 bact_obj = new ReadPSort3(conn);
//
//
//		if(genome_files.isDirectory()) {
//
//			for(File genome_file:genome_files.listFiles()) {
//
//				if(genome_file.isFile()) {
//
//					List<PSort3Result>  results_list = bact_obj.addGeneInformation(new File(psort_prediction_file_path));
//
//					bact_obj = new ReadPSort3(conn,results_list);
//					bact_obj.loadCompartmentsInformation();
//
//				}
//			}
//		}
//
//		obj = bact_obj;
//
//		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//		//compartmentalization
//		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//		transportContainer = ltl.compartmentaliseTransportContainer(path,transportContainer, obj, k);
//
//	}

}
