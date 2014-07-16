package pt.uminho.sysbio.common.transporters.core.transport.reactions.annotateTransporters;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.htmlparser.Node;
import org.htmlparser.Parser;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

/**
 * @author ODias
 *
 */
public class TCDB_Parser {

	private Set<String> excludeWords;

	/**
	 * 
	 */
	public TCDB_Parser(Set<String> exclude) {
		excludeWords = new TreeSet<String>();
		excludeWords.add("! test"); 
		excludeWords.add("&nbsp;");
		excludeWords.add("i");
		excludeWords.add("/i");
		excludeWords.add("b");
		excludeWords.add("/b");
		excludeWords.add("B");
		excludeWords.add("/B");
		excludeWords.add("I");
		excludeWords.add("/I");
		excludeWords.add("/i");
		excludeWords.add("br /");
		excludeWords.add("sup");
		excludeWords.add("/sup");
		excludeWords.add("SUP");
		excludeWords.add("/SUP");
		excludeWords.add("br");
		excludeWords.add("strong");
		excludeWords.add("/strong");
		excludeWords.addAll(exclude);
	}

	/**
	 * @param url
	 */
	public Map<String,String[]> parseTCDB(String url, Map<String,String[]> entry){

		try 
		{
			//String inputHTML = readFile("filemakerExport.htm","UTF-8");
			//String inputHTML = readFile();
			Parser parser = new Parser(new URL(url).openConnection());
			//parser.setInputHTML(new HtmlStream(url));
			parser.setEncoding("UTF-8");
			NodeList nl = parser.parse(null);
			parser.elements();
			//NodeIterator nodeIterator = parser.elements();
			//System.out.println(parser.getURL());
			//			while(nodeIterator.hasMoreNodes()){System.out.println(nodeIterator.nextNode().getText());}
			//iterate table ->tr
			boolean familyNotRead=true;
			NodeList tableRows = nl.extractAllNodesThatMatch(new TagNameFilter("tr"),true);
			for(int i=0;i<tableRows.size();i++) 
			{
				//iterate rows ->td
				NodeList tableCells  = tableRows.elementAt(i).getChildren().extractAllNodesThatMatch(new TagNameFilter("td"),true);
				if(tableCells.size()==1)
				{
					String readNode = readNodeRecursive(tableCells.elementAt(0),"");
					if(familyNotRead)
					{
						String[] data = new String[1];
						data[0] = readNode.substring(readNode.indexOf(":"));
						String family=readNode.substring(0,(readNode.indexOf(":")));
						entry.put(family,data);
						familyNotRead=false;
					}
				}
				if(tableCells.size()==4)
				{
					familyNotRead=true;
					String[] data = new String[3];
					data[0] = readNodeRecursive(tableCells.elementAt(1),"");
					data[1] = readNodeRecursive(tableCells.elementAt(2),"");
					data[2] = readNodeRecursive(tableCells.elementAt(3),"");
					entry.put(readNodeRecursive(tableCells.elementAt(0),""), data);

				}
			}
			return entry;
		}
		catch (ParserException e) {e.printStackTrace();}
		catch (MalformedURLException e) {e.printStackTrace();}
		catch (IOException e) {e.printStackTrace();}
		return null;
	}

	/**
	 * @param url
	 */
	public Map<String,String[]> parseTCDBrecord(String url, Map<String,String[]> recordEntries) {
		String family=null;
		String[] data = null;
		try 
		{
			Parser parser = new Parser(new URL(url).openConnection());
			parser.setEncoding("UTF-8");
			NodeList nl = parser.parse(null);
			parser.elements();
			NodeList table = nl.extractAllNodesThatMatch(new TagNameFilter("table"),true);
			//iterate FIRST table ->tr
			NodeList tableRows = table.elementAt(0).getChildren().extractAllNodesThatMatch(new TagNameFilter("tr"),true);
			data = new String[6];
			for(int i=0;i<tableRows.size();i++) 
			{
				String readNode = this.readNodeRecursive(tableRows.elementAt(i), "");
				if(i==0)
				{
					if(readNode.contains(" "))
					{
						family = readNode.substring(0,readNode.indexOf(" ")).trim();
						data[0] = readNode.substring(readNode.indexOf(" ")).trim();
					}
					else
					{
						family = readNode;
						data[0] = readNode;
					}
				}
				else
				{
					if(readNode.startsWith("Accession")){data[1] = readNode.substring(readNode.indexOf(":")+1).trim();}
					if(readNode.startsWith("Protein")){data[2] = readNode.substring(readNode.indexOf(":")+1).trim();}
					if(readNode.startsWith("Species")){data[3] = readNode.substring(readNode.indexOf(":")+1).trim();}
					if(readNode.startsWith("Number")){data[4] = readNode.substring(readNode.indexOf(":")+1).trim();}
					if(readNode.startsWith("Location")){data[5] = readNode.substring(readNode.indexOf(":")+1,readNode.lastIndexOf("1")).trim();}
				}
			}
			recordEntries.put(family, data);
			return recordEntries;
		}
		catch (ParserException e) {e.printStackTrace();}
		catch (MalformedURLException e) {e.printStackTrace();}
		catch (IOException e) {e.printStackTrace();}
		return null;
	}


	/**
	 * @param url
	 * @param ytpdbEntries
	 * @return 
	 */
	public Map<String, String[]> parseYTPDBrecord(String url, Map<String,String[]> ytpdbEntries) {
		String locusTag=null;
		String[] data = null;
		try 
		{
			Parser parser = new Parser(new URL(url).openConnection());
			parser.setEncoding("UTF-8");
			NodeList nl = parser.parse(null);
			parser.elements();
			NodeList table = nl.extractAllNodesThatMatch(new TagNameFilter("table"),true);
			//iterate FIRST table ->tr
			NodeList tableRows = table.elementAt(0).getChildren().extractAllNodesThatMatch(new TagNameFilter("tr"),true);
			data=new String[4];
			for(int i=0;i<tableRows.size();i++) 
			{
				String readNode = this.readNodeRecursive(tableRows.elementAt(i), "");
				if(readNode.startsWith("Name")){data[0] = readNode.substring(readNode.indexOf(" ")).trim();}
				if(data[0].equals("{NAME}")){return ytpdbEntries;};
				if(readNode.startsWith("ORF")){locusTag = readNode.substring(readNode.indexOf(" ")).trim();}
				if(readNode.startsWith("Description")){data[1] = readNode.substring(readNode.indexOf(" ")).trim();}
				if(readNode.startsWith("Substrates"))
				{
					if(readNode.indexOf(" ")!=-1)
					{data[2] = readNode.substring(readNode.indexOf(" ")).trim();}
					else{data[2] = "";}
				}
				if(readNode.startsWith("Location")){data[3] = readNode.substring(readNode.indexOf(" ")).trim();}

				//System.out.println(readNode);
			}
			ytpdbEntries.put(locusTag, data);
			return ytpdbEntries;
		}
		catch (ParserException e) {e.printStackTrace();}
		catch (MalformedURLException e) {e.printStackTrace();}
		catch (IOException e) {e.printStackTrace();}
		return ytpdbEntries;
	}

	/**
	 * @param node
	 * @param text
	 * @return
	 */
	public String readNodeRecursive(Node node, String text){
		try 
		{
			if(node.getChildren()!=null)
			{
				NodeIterator nodeIt = node.getChildren().elements();
				while(nodeIt.hasMoreNodes())
				{
					text=readNodeRecursive(nodeIt.nextNode(), text.trim().concat(" ")).trim();
				}
				return text.trim();
			}
			else
			{
				String nodeString=node.getText().replaceAll("&nbsp;","").replaceAll("\t","").replaceAll("\n","").replaceAll("\r","").replaceAll("\\<.*?>","").trim();
				if(node.getText().trim().isEmpty() || excludeWords.contains(nodeString) || nodeString.startsWith("A id=")) 
				{
					return text.trim();
				}
				return (text.concat(nodeString).trim());
			}
		}
		catch (ParserException e) {e.printStackTrace();}
		return null;
	}


	//
	//	/**
	//	 * @param args
	//	 */
	//	public static void main(String[] args) {
	//		Map<String,String[]> entry =new TreeMap<String, String[]>();
	//		Map<String,String[]> recordEntries =new TreeMap<String, String[]>();
	//		Map<String,String[]> ytpdbEntries =new TreeMap<String, String[]>();
	//		TCDB_Parser tp = new TCDB_Parser(new TreeSet<String>());
	//		tp.parseTCDB("http://www.tcdb.org/search/result.php?tc=3.A.1.209#3.A.1.209",entry);
	//		tp.parseTCDBrecord("http://www.tcdb.org/search/result.php?tc=3.A.1.209.1&acc=Q03518",recordEntries);
	//		tp.parseYTPDBrecord("http://homes.esat.kuleuven.be/~sbrohee/ytpdb/index.php/Ytpdbgene:YBR040W",ytpdbEntries);
	//	}
}
