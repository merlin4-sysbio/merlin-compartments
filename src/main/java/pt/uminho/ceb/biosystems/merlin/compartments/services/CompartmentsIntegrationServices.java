package pt.uminho.ceb.biosystems.merlin.compartments.services;

import java.sql.SQLException;

import pt.uminho.ceb.biosystems.merlin.services.model.ModelCompartmentServices;

public class CompartmentsIntegrationServices {


	/**
	 * @param interiorCompartment
	 * @param statement
	 * @return
	 * @throws Exception 
	 */
	public static String autoSetInteriorCompartment(String databaseName, String interiorCompartment) throws Exception{

		try {

			interiorCompartment = ModelCompartmentServices.getCompartmentAbbreviation(databaseName);
		} 
		catch (Exception e) {
			e.printStackTrace();
		}

		return interiorCompartment;
	}


	/**
	 * @param compartment
	 * @throws SQLException
	 */
	public static int getCompartmentID(String databaseName, String compartment) {

		Integer compartmentID = null;

		try {

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
			
			compartmentID = ModelCompartmentServices.getCompartmentIdByNameAndAbbreviation(databaseName, compartment, abbreviation);
			
			if(compartmentID == null)
				compartmentID = ModelCompartmentServices.insertNameAndAbbreviation(databaseName, compartment, abbreviation);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return compartmentID;

	}
	
}
