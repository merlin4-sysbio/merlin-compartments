package pt.uminho.ceb.biosystems.merlin.transporters.core.transport;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import pt.uminho.ceb.biosystems.merlin.database.connector.databaseAPI.TransportersAPI;
import pt.uminho.ceb.biosystems.merlin.database.connector.datatypes.Connection;
import pt.uminho.ceb.biosystems.merlin.database.connector.datatypes.DatabaseAccess;
import pt.uminho.ceb.biosystems.merlin.database.connector.datatypes.MySQLDatabaseAccess;


public class TransportersSchemaUpdate {

	//@Test
	public void test() throws SQLException {
		
		
		//DatabaseAccess m =  new DatabaseAccess("root", "password", "127.0.0.1", 3306, "transporters");
		DatabaseAccess m =  new MySQLDatabaseAccess("odias", "#!odias#2013@silico", "192.168.1.100", 3306, "transporters");
	
		
		Connection c = new Connection(m);
		
		
		Statement s = c.createStatement();
		
		
		Map<Integer, String[]> d = TransportersAPI.getAllTransportTypesData(s);
		
		List<Integer> l = TransportersAPI.getTransportTypeID(s);

		for(int i : l) {
			
			String query = "SELECT * FROM transport_types WHERE name = '"+d.get(i)[0]+"' AND directions ='"+d.get(i)[1]+"' AND NOT reversible;";
			
			int transportTypeNewId = -1;
			
			transportTypeNewId = TransportersAPI.getFromTransportTypes(transportTypeNewId, query, s);
			
			if(transportTypeNewId==-1){
				
				query = "INSERT INTO transport_types (name, directions, reversible) VALUES('"+d.get(i)[0]+"', '"+d.get(i)[1]+"', false);";
				transportTypeNewId = TransportersAPI.insertIntoTransportTypes(transportTypeNewId, query, s);
			}
			
			query = "UPDATE transport_systems SET transport_type_id = "+transportTypeNewId+" WHERE transport_type_id = "+i+" AND NOT reversible;";
			TransportersAPI.executeQuery(query, s);
		}
		
	}
	
	
	
}
