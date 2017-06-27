package pt.uminho.sysbio.common.transporters.core.transport;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.uminho.sysbio.common.database.connector.datatypes.Connection;
import pt.uminho.sysbio.common.database.connector.datatypes.DatabaseAccess;
import pt.uminho.sysbio.common.database.connector.datatypes.MySQLDatabaseAccess;


public class TransportersSchemaUpdate {

	//@Test
	public void test() throws SQLException {
		
		
		//DatabaseAccess m =  new DatabaseAccess("root", "password", "127.0.0.1", 3306, "transporters");
		DatabaseAccess m =  new MySQLDatabaseAccess("odias", "#!odias#2013@silico", "192.168.1.100", 3306, "transporters");
	
		
		Connection c = new Connection(m);
		
		
		Statement s = c.createStatement();
		
		
		ResultSet r = s.executeQuery("SELECT * FROM transport_types;");
		
		
		Map<Integer, String[]> d = new HashMap<Integer, String[]>();
		
		
		while(r.next()) {
			
			d.put(r.getInt(1), new String[] {r.getString(2), r.getString(3)});
		}
		
		r = s.executeQuery("SELECT DISTINCT transport_type_id FROM transport_systems WHERE NOT reversible;");
		
		List<Integer> l = new ArrayList<Integer>();
				
		while(r.next()) {
			
			l.add(r.getInt(1));
		}
		
		for(int i : l) {
			
			r = s.executeQuery("SELECT * FROM transport_types WHERE name = '"+d.get(i)[0]+"' AND directions ='"+d.get(i)[1]+"' AND NOT reversible;");
			
			int transport_type_new_id = -1;
			
			if(r.next()) {
				
				transport_type_new_id = r.getInt(1);
			}
			else{
				
				s.execute("INSERT INTO transport_types (name, directions, reversible) VALUES('"+d.get(i)[0]+"', '"+d.get(i)[1]+"', false);");
				r = s.executeQuery("SELECT LAST_INSERT_ID();");
				r.next();				
				transport_type_new_id = r.getInt(1);
			}
			
			s.execute("UPDATE transport_systems SET transport_type_id = "+transport_type_new_id+" WHERE transport_type_id = "+i+" AND NOT reversible;");
		}
		
	}
	
	
	
}
