package tests;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.uminho.ceb.biosystems.merlin.database.connector.datatypes.Connection;
import pt.uminho.ceb.biosystems.merlin.database.connector.datatypes.DatabaseAccess;
import pt.uminho.ceb.biosystems.merlin.database.connector.datatypes.MySQLDatabaseAccess;
import pt.uminho.ceb.biosystems.merlin.transporters.core.compartments.CompartmentResult;
import pt.uminho.ceb.biosystems.merlin.transporters.core.compartments.ReadPSort3;
import pt.uminho.ceb.biosystems.merlin.transporters.core.compartments.WoLFPSORT;
import pt.uminho.ceb.biosystems.merlin.transporters.core.transport.MIRIAM_Data;
import pt.uminho.ceb.biosystems.merlin.transporters.core.transport.reactions.TransportReactionsGeneration;
import pt.uminho.ceb.biosystems.merlin.transporters.core.transport.reactions.loadTransporters.LoadTransportersData;


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

		ReadPSort3 readPSort3 = new ReadPSort3();

		Map<String, CompartmentResult> res = readPSort3.addGeneInformation(new File("D:/My Dropbox/WORK/Projecto_PEM/reu/PSort/psortb-results_extr2.txt"));

		for(CompartmentResult p : res.values()) {

			System.out.println(p.getGeneID());
		}
	}


	//@Test
	public void test() throws Exception {

		List<String> mets = new ArrayList<>();
		mets.add("D-mannose");
//		mets.add("D-glucose");
//		mets.add("h+");
//		mets.add("proton");
//		mets.add("Zn2+");
//		mets.add("zinc");
//		mets.add("zinc ion");
//		mets.add("acetan");
//		mets.add("ferric ion");
//		mets.add("ferric iron");
//		mets.add("Fe2+");
//		mets.add("hop");
//		mets.add("sterol");
//		mets.add("steroids");
//		mets.add("calcium");
//		mets.add("HCO3-");
//		mets.add("bicarbonate");
//		mets.add("ca2+");
//		mets.add("small molecules");
//		mets.add("small molecule");
//		mets.add("3-hydroxy kynurenine");
//		mets.add("3-hydroxykynurenine");
//		mets.add("Ni2+");
//		mets.add("nickel");

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
	@Test
	public void wolfpSort () {

		String out = "C:/Users/ODias/Desktop/out.out";
		try {
			
			WoLFPSORT.getCompartments("fungi","D:/Dropbox/Public_/merlin_releases/griffin.faa", out);
		}
		catch (Exception e) {

			e.printStackTrace();
		}
	}
	
	
	public void doTrustToCertificates() throws Exception {
        Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
        X509TrustManager[] trustAllCerts = new X509TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
                        return;
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {
                        return;
                    }
                }
        };

        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        HostnameVerifier hv = new HostnameVerifier() {
            public boolean verify(String urlHostName, SSLSession session) {
                if (!urlHostName.equalsIgnoreCase(session.getPeerHost())) {
                    System.out.println("Warning: URL host '" + urlHostName + "' is different to SSLSession host '" + session.getPeerHost() + "'.");
                }
                return true;
            }
        };
        HttpsURLConnection.setDefaultHostnameVerifier(hv);
    }
	
	
	
	
	
	
//	@Test
	public void connectToUrl() throws Exception{
	     doTrustToCertificates();//  
	     URL url = new URL("http://www.tcdb.org/search/result.php?acc=q04162");
	     HttpURLConnection conn = (HttpURLConnection)url.openConnection(); 
	     System.out.println("ResponseCode ="+conn.getResponseCode());
	     
	     BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
	     String inputLine;
	     
	     String html = null;
	     
		 while ((inputLine = in.readLine()) != null){
	    	 html = inputLine;
     		 Document doc = Jsoup.parse(html);
     		 String text = doc.body().text();
     		 System.out.println(text);
		}
	     
	     in.close();
	}
	
	
	public void readLocTree () {

		try {

			URL url = new URL("https://www.rostlab.org/services/loctree3/results.php?id=caed4a6a-6baf-4111-a5bf-8325c447abc6");
			String file = "/Users/davidelagoa/Desktop/file.txt";
		
		    System.out.println("opening connection");
		    InputStream in = url.openStream();
		    FileOutputStream fos = new FileOutputStream(new File(file));

		    System.out.println("reading file...");
		    int length = -1;
		    byte[] buffer = new byte[1024];// buffer for portion of data from
		    // connection
		    while ((length = in.read(buffer)) > -1) {
		        fos.write(buffer, 0, length);
		    }

		    
		    
		    

		    fos.close();
		    in.close();
		    System.out.println("file was downloaded");
		
	
			
//		    URL myURL = new URL("https://www.rostlab.org/services/loctree3/results.php?id=caed4a6a-6baf-4111-a5bf-8325c447abc6.xml");
////		    URLConnection myURLConnection = myURL.openConnection();
////		    myURLConnection.getInputStream();
//		    
////		    URL oracle = new URL("http://www.oracle.com/");
//	        BufferedReader in = new BufferedReader(new InputStreamReader(myURL.openStream()));
//	        System.out.println("URLFJ");
//	        System.out.println("URL"+myURL);
//	        String inputLine;
//	        while ((inputLine = in.readLine()) != null)
//	            System.out.println(inputLine+"linha");
//	        in.close();
		    
		} 
		catch (Exception e) { 
		   e.printStackTrace();
		}
		System.out.println("fim");
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
