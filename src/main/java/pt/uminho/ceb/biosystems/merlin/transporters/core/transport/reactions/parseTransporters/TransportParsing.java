/**
 * 
 */
package pt.uminho.ceb.biosystems.merlin.transporters.core.transport.reactions.parseTransporters;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import pt.uminho.ceb.biosystems.merlin.transporters.core.transport.reactions.TransportReactionsGeneration;
import pt.uminho.ceb.biosystems.merlin.transporters.core.transport.reactions.containerAssembly.TransportMetaboliteDirectionStoichiometryContainer;
import pt.uminho.ceb.biosystems.merlin.transporters.core.utils.Enumerators.TransportType;


/**
 * @author ODias
 *
 */
public class TransportParsing {

	private List<List<TransportMetaboliteDirectionStoichiometryContainer>> transportMetaboliteDirectionStoichiometryContainerLists;



	/**
	 * 
	 */
	public TransportParsing() {

		this.transportMetaboliteDirectionStoichiometryContainerLists = new ArrayList<List<TransportMetaboliteDirectionStoichiometryContainer>>();
	}


	/**
	/**
	 * @param metabolitesString
	 * @param directions
	 */
	public void parseMetabolites(String metabolitesString, String directions) {

		StringTokenizer metabolitesToken = new StringTokenizer(metabolitesString,"||");
		StringTokenizer directionToken = new StringTokenizer(directions,"||");
		String direction=TransportReactionsGeneration.selectDirection(directionToken.nextToken());

		while(metabolitesToken.hasMoreTokens()) {

			if(directionToken.hasMoreTokens()) {

				direction=TransportReactionsGeneration.selectDirection(directionToken.nextToken());
				//System.out.println(direction+" TRANSPORT PARSING LINE 55");
			}
			List<List<TransportMetaboliteDirectionStoichiometryContainer>> transportMetaboliteDirectionStoichiometryContainerLists = new ArrayList<List<TransportMetaboliteDirectionStoichiometryContainer>>();

			int transportType=TransportType.valueOf(direction).getTransport();

			this.parseMetabolitesToken(metabolitesToken.nextToken().trim(), transportMetaboliteDirectionStoichiometryContainerLists,transportType);

			this.transportMetaboliteDirectionStoichiometryContainerLists.addAll(transportMetaboliteDirectionStoichiometryContainerLists);
		}
		//		System.out.println("LISTS BELOW\n"+metabolites);
		//		System.out.println(directions);
	}

	/**
	 * @param dataToParse
	 * @param metabolitesLists
	 * @param directionsLists
	 * @param direction
	 */
	private void parseMetabolitesToken(String dataToParse, List<List<TransportMetaboliteDirectionStoichiometryContainer>> transportMetaboliteDirectionStoichiometryContainerLists, int direction){

		while(dataToParse.contains(":")||dataToParse.contains("//")) {
			
			String transportdirection = appearsFirst(dataToParse,":","//");
			//System.out.println(dataToParse);
			String data=dataToParse.substring(0, dataToParse.indexOf(transportdirection)).trim();

			parseMetabolitesToken(data, transportMetaboliteDirectionStoichiometryContainerLists, direction);

			if(transportdirection.equals("//")){direction=1;}
			dataToParse=dataToParse.substring(dataToParse.indexOf(transportdirection)).replaceFirst(transportdirection,"".trim());
		}

		List<List<TransportMetaboliteDirectionStoichiometryContainer>> newtransportMetaboliteDirectionStoichiometryContainerLists
		= new ArrayList<List<TransportMetaboliteDirectionStoichiometryContainer>>(transportMetaboliteDirectionStoichiometryContainerLists);

		transportMetaboliteDirectionStoichiometryContainerLists.removeAll(transportMetaboliteDirectionStoichiometryContainerLists);

		for(String metabolite:this.getMetabolites(dataToParse)) {
			
			if(newtransportMetaboliteDirectionStoichiometryContainerLists.size()==0) {

				List<TransportMetaboliteDirectionStoichiometryContainer> newTransportMetaboliteDirectionStoichiometryContainerList = new ArrayList<TransportMetaboliteDirectionStoichiometryContainer>();

				TransportMetaboliteDirectionStoichiometryContainer tmds = new TransportMetaboliteDirectionStoichiometryContainer(metabolite);
				tmds.setDirection(MetabolitesEntry.getDirection(direction));
				tmds.setStoichiometry(1);
				newTransportMetaboliteDirectionStoichiometryContainerList.add(tmds);

				transportMetaboliteDirectionStoichiometryContainerLists.add(newTransportMetaboliteDirectionStoichiometryContainerList);
			}
			else {

				for(int i=0; i<newtransportMetaboliteDirectionStoichiometryContainerLists.size(); i++) {

					List<TransportMetaboliteDirectionStoichiometryContainer> transportMetaboliteDirectionStoichiometryContainerList = newtransportMetaboliteDirectionStoichiometryContainerLists.get(i);

					List<TransportMetaboliteDirectionStoichiometryContainer> newTransportMetaboliteDirectionStoichiometryContainerList = new ArrayList<TransportMetaboliteDirectionStoichiometryContainer>();

					newTransportMetaboliteDirectionStoichiometryContainerList.addAll(transportMetaboliteDirectionStoichiometryContainerList);

					int tmdsIndex = -1;
					
					for(int t = 0; t < newTransportMetaboliteDirectionStoichiometryContainerList.size(); t++) {

						if(newTransportMetaboliteDirectionStoichiometryContainerList.get(t).getName().equalsIgnoreCase(metabolite)) {

							tmdsIndex = t;
						}
					}
					
					if(tmdsIndex>-1) {

						TransportMetaboliteDirectionStoichiometryContainer tmds = newTransportMetaboliteDirectionStoichiometryContainerList.get(tmdsIndex);

						if(MetabolitesEntry.getDirection(direction).equalsIgnoreCase(tmds.getDirection())) {
							
							tmds.setStoichiometry(tmds.getStoichiometry()+1);
						}
						else {

							TransportMetaboliteDirectionStoichiometryContainer newTmds = new TransportMetaboliteDirectionStoichiometryContainer(metabolite);
							newTmds.setDirection(MetabolitesEntry.getDirection(direction));
							newTmds.setStoichiometry(1);
							newTransportMetaboliteDirectionStoichiometryContainerList.add(newTmds);
						}
					}
					else {

						TransportMetaboliteDirectionStoichiometryContainer newTmds = new TransportMetaboliteDirectionStoichiometryContainer(metabolite);
						newTmds.setDirection(MetabolitesEntry.getDirection(direction));
						newTmds.setStoichiometry(1);
						newTransportMetaboliteDirectionStoichiometryContainerList.add(newTmds);
					}

					transportMetaboliteDirectionStoichiometryContainerLists.add(newTransportMetaboliteDirectionStoichiometryContainerList);
				}
			}

			//System.out.println(metabolitesLists);
			//System.out.println(directionsLists);

		}
	}


	/**
	 * @param reacting_metabolites
	 */
	public void parseReactingMetabolites(String reacting_metabolites) {

		List<List<TransportMetaboliteDirectionStoichiometryContainer>> metabolites_temp = this.copyListofLists(this.transportMetaboliteDirectionStoichiometryContainerLists);

		this.transportMetaboliteDirectionStoichiometryContainerLists = new ArrayList<List<TransportMetaboliteDirectionStoichiometryContainer>>();

		StringTokenizer reactionsToken = new StringTokenizer(reacting_metabolites,"//");

		while(reactionsToken.hasMoreTokens()) {

			String reactions_data = reactionsToken.nextToken().trim();			

			StringTokenizer metabolitesToken = new StringTokenizer(reactions_data,"||");
			List<List<String>> reactants_data = this.parse_reacting_data(metabolitesToken.nextToken().trim());
			List<List<String>> products_data = this.parse_reacting_data(metabolitesToken.nextToken().trim());

			List<String> reactants_name = reactants_data.get(0);
			List<String> reactants_stoichiometry = reactants_data.get(1);

			List<String> products_name = products_data.get(0);
			List<String> products_stoichiometry = products_data.get(1);


			if(metabolites_temp.size()<1)
				metabolites_temp.add(new ArrayList<TransportMetaboliteDirectionStoichiometryContainer>());

			List<List<TransportMetaboliteDirectionStoichiometryContainer>> metabolites_temp_clone = this.copyListofLists(metabolites_temp);

			for(int i=0; i<metabolites_temp_clone.size();i++) {

				for(int j=0; j<reactants_name.size();j++) {

					TransportMetaboliteDirectionStoichiometryContainer tmds = new TransportMetaboliteDirectionStoichiometryContainer(reactants_name.get(j));
					tmds.setDirection(MetabolitesEntry.getDirection(4));
					tmds.setStoichiometry(Double.valueOf(reactants_stoichiometry.get(j)));

					metabolites_temp_clone.get(i).add(tmds);
				}

				for(int j=0; j<products_name.size();j++) {

					TransportMetaboliteDirectionStoichiometryContainer tmds = new TransportMetaboliteDirectionStoichiometryContainer(products_name.get(j));
					tmds.setDirection(MetabolitesEntry.getDirection(5));
					tmds.setStoichiometry(Double.valueOf(products_stoichiometry.get(j)));

					metabolites_temp_clone.get(i).add(tmds);
				}
			}
			
			this.transportMetaboliteDirectionStoichiometryContainerLists.addAll(metabolites_temp_clone);
		}
	}



	/**
	 * @param data
	 * @return
	 */
	private List<List<TransportMetaboliteDirectionStoichiometryContainer>> copyListofLists(List<List<TransportMetaboliteDirectionStoichiometryContainer>>  data){

		List<List<TransportMetaboliteDirectionStoichiometryContainer>> data_clone= new ArrayList<List<TransportMetaboliteDirectionStoichiometryContainer>>();

		for(List<TransportMetaboliteDirectionStoichiometryContainer> object_lists:data) {

			List<TransportMetaboliteDirectionStoichiometryContainer> data_list = new ArrayList<TransportMetaboliteDirectionStoichiometryContainer>();

			for(TransportMetaboliteDirectionStoichiometryContainer data_Object : object_lists) {

				data_list.add(data_Object);
			}
			data_clone.add(data_list);
		}
		return data_clone;
	}


	/**
	 * @param metabolitesToken
	 * @return
	 */
	private List<List<String>> parse_reacting_data(String reacting_metabolites) {
		
		StringTokenizer metabolitesToken = new StringTokenizer(reacting_metabolites,";");
		List<List<String>> result =  new ArrayList<List<String>>();
		List<String> metabolites = new ArrayList<String>();
		result.add(metabolites);
		List<String> stoichiometries = new ArrayList<String>();
		result.add(stoichiometries);
		
		while(metabolitesToken.hasMoreTokens()) {
			
			String[] metabolite_data = metabolitesToken.nextToken().trim().split("\\:");
			//System.out.println(reacting_metabolites);System.out.println(metabolite_data[0].trim());System.out.println(metabolite_data[1].trim()); System.out.println();
			stoichiometries.add(metabolite_data[0].trim()); 
			metabolites.add(metabolite_data[1].trim()); 
		}
		return result;
	}


	/**
	 * @param stringToSearch
	 * @param search1
	 * @param search2
	 * @return
	 */
	private String appearsFirst(String stringToSearch, String search1, String search2){

		int position1 = stringToSearch.indexOf(search1);
		int position2 = stringToSearch.indexOf(search2);

		if(position1>0)
		{
			if(position2>0)
			{
				if(position1<position2)
				{
					return search1;
				}
				else
				{
					return search2;
				}
			}
			return search1;
		}
		return search2;


	}

	/**
	 * @param data
	 * @return
	 */
	private Set<String> getMetabolites(String data) {

		Set<String> metabolites = new TreeSet<String>();
		//System.out.println(data);
		StringTokenizer tokenizer = new StringTokenizer(data,";");
		//metabolites.add(tokenizer.nextToken().trim());
		while(tokenizer.hasMoreTokens()) {

			metabolites.add(tokenizer.nextToken().trim());
		}

		//System.out.println(metabolites);		
		return metabolites;		
	}


	/**
	 * @return the transportMetaboliteDirectionStoichiometryContainerLists
	 */
	public List<List<TransportMetaboliteDirectionStoichiometryContainer>> getTransportMetaboliteDirectionStoichiometryContainerLists() {
		return transportMetaboliteDirectionStoichiometryContainerLists;
	}


	/**
	 * @param transportMetaboliteDirectionStoichiometryContainerLists the transportMetaboliteDirectionStoichiometryContainerLists to set
	 */
	public void setTransportMetaboliteDirectionStoichiometryContainerLists(
			List<List<TransportMetaboliteDirectionStoichiometryContainer>> transportMetaboliteDirectionStoichiometryContainerLists) {
		this.transportMetaboliteDirectionStoichiometryContainerLists = transportMetaboliteDirectionStoichiometryContainerLists;
	}

}
