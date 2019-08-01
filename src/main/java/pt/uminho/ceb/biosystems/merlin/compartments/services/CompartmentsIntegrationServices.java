package pt.uminho.ceb.biosystems.merlin.compartments.services;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.bytebuddy.asm.Advice.This;
import pt.uminho.ceb.biosystems.merlin.compartments.datatype.AnnotationCompartmentsGenes;
import pt.uminho.ceb.biosystems.merlin.compartments.processes.CompartmentsProcesses;
import pt.uminho.ceb.biosystems.merlin.core.containers.model.ReactionContainer;
import pt.uminho.ceb.biosystems.merlin.core.datatypes.Workspace;
import pt.uminho.ceb.biosystems.merlin.core.utilities.Enumerators.InformationType;
import pt.uminho.ceb.biosystems.merlin.database.connector.databaseAPI.CompartmentsAPI;
import pt.uminho.ceb.biosystems.merlin.database.connector.databaseAPI.ModelAPI;
import pt.uminho.ceb.biosystems.merlin.database.connector.datatypes.Connection;
import pt.uminho.ceb.biosystems.merlin.services.ProjectServices;
import pt.uminho.ceb.biosystems.merlin.services.model.ModelEnzymesServices;
import pt.uminho.ceb.biosystems.merlin.services.model.ModelGenesServices;
import pt.uminho.ceb.biosystems.merlin.services.model.ModelReactionsServices;
import pt.uminho.ceb.biosystems.merlin.services.model.loaders.ModelDatabaseLoadingServices;

public class CompartmentsIntegrationServices {

	/**
	 * @return
	 * @throws SQLException 
	 */
	public String autoSetInteriorCompartment(String interiorCompartment, Connection connection) {

		String out = null;

		try {

			Statement stmt = connection.createStatement();

			out = CompartmentsIntegrationServices.autoSetInteriorCompartment(interiorCompartment, stmt);

			stmt.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return out;
	}

	/**
	 * @param interiorCompartment
	 * @param statement
	 * @return
	 */
	public static String autoSetInteriorCompartment(String interiorCompartment, Statement statement){

		try {

			interiorCompartment = CompartmentsAPI.getCompartmentAbbreviation(interiorCompartment, statement);

		} 
		catch (SQLException e) {
			e.printStackTrace();
		}

		return interiorCompartment;
	}


	/**
	 * @param compartment
	 * @throws SQLException
	 */
	public static int getCompartmentID(String compartment, Connection connection) {

		Statement stmt;
		int compartmentID = -1;

		try {
			stmt = connection.createStatement();

			String abbreviation;

			if(compartment.length()>3) {

				abbreviation=compartment.substring(0,3).toUpperCase();
			}
			else {

				abbreviation=compartment.toUpperCase().concat("_");

				while(abbreviation.length()<4)
					abbreviation=abbreviation.concat("_");
			}

			abbreviation=abbreviation.toUpperCase();

			compartmentID = CompartmentsAPI.selectCompartmentID(compartment, abbreviation, stmt);

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return compartmentID;

	}
	
}
