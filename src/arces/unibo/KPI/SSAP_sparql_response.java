package arces.unibo.KPI;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.jdom2.*;
import org.jdom2.input.*;
import org.jdom2.output.*;


/**
 * This class represents the SSAP RESPONSE to a generic SPARQL query.
 * This class offers methods to extract and manage the results inside the response.
 * This class is coherent with the entire library project and uses java.util.Vector instead of better performing ArrayList or similar
 * A refactory of this library is suggested.
 * 
 * The computation time is reduced: the instantiated object of this class loads into memory the SSAP response as a string,
 * AND the entire structure with the results (which may be many).
 * Thanks to the methods offered by the class, we obtain the desired structures NOT at run time.
 * This programming model looks at the execution time, and can therefore have effects on occupation of space in memory.
 * 
 * @author patruzzrock and Alfredo
 *
 */
public class SSAP_sparql_response {

	private Vector<Vector<String[]>> sparql_response_results;
	private Vector<String> var_names;
	private Vector<String> link_hrefs;
	private Vector<String> booleans;
	private int seek;

	private Element resultGraph;

	private boolean has_results;
	private boolean has_links;
	private boolean has_booleans;

	private boolean query_type_select_ask = false;
	private boolean query_type_construct_describe = false;

	private final static int VARNAME=0;
	private final static int CATEGORY=1;
	private final static int VALUE=2;
	private final static int TYPE=3;
	
	
	public static final String URI = "uri";
	public static final String LITERAL = "literal";
	public static final String BNODE = "bnode";
	public static final String UNBOUND = "unbound";
	

	/**
	 * The constructor of the SSAP_sparql_response Class
	 * 
	 * each result in the SSAP sparql response message is saved as a Vector of String[].
	 * Each element of this Vector represents the detail of the "binding" element: [name, category, value, type].
	 * "name" is the binding name of the result element; "value" is the proper value of the result element; 
	 * To understand the meaning of the category and type field, there is a classification of possible bindings:
	 *
	 * The value of a query variable "binding", which is an RDF Term, is included as the content of the binding as follows:
	 *		category URI, for a binding element of the form: <binding><uri>U</uri></binding>
	 *		category LITERAL, for a binding element of one of the forms: 
	 *			<binding><literal>S</literal></binding> (type SIMPLE, nothing added)
	 *			<binding><literal xml:lang="L">S</literal></binding> (TYPE WLANG, the value of the attribute "xml:lang" is added)
	 *			<binding><literal datatype="D">S</literal></binding> (TYPE WDATT, the value of the attribute "datatype" is added)
	 *		category BNODE, for a binding element of the form <binding><bnode>I</bnode></binding>
	 *		category UNBOUND, for an unbound variable (no binding element for that variable is included in the result element).
	 * In case of UNBOUND, BNODE or URI category, the "type" field is NOT present. 
	 *
	 * @return This function returns all the results of the SSAP sparql response message as a Vector<Vector<String[]>>. 
	 */


	public void initializeFromResponseXML(String xml)
	{
		this.seek = -1;
		this.has_results = false;
		this.has_links = false;
		this.has_booleans = false;

		this.query_type_select_ask = false;
		this.query_type_construct_describe = false;

		this.var_names=new Vector<String>();
		this.link_hrefs=new Vector<String>();
		this.booleans=new Vector<String>();
		this.sparql_response_results = new Vector<Vector<String[]>>();


		try
		{


			Document sparql_response_document=loadXMLFromString(xml);

			//	        GET ROOT ELEMENT + NAMESPACE
			Element root = sparql_response_document.getRootElement();
			//Namespace ns;
			List<Element> sparqlChildren= root.getChildren();
			//ns = root.getNamespace();
			Iterator <Element> sparqlIter = sparqlChildren.iterator();

			while(sparqlIter.hasNext())
			{
				Element sparqlChild = sparqlIter.next();


				//				INTO <HEAD>
				if(sparqlChild.getName().equalsIgnoreCase("head"))
				{
					List <Element> headList = sparqlChild.getChildren();
					Iterator <Element> headListIter = headList.iterator();

					while(headListIter.hasNext())
					{
						Element headchildren = headListIter.next();
						List<Attribute> attribute = headchildren.getAttributes();

						for (int i =0;i<attribute.size();i++)
						{
							Attribute att = (Attribute) attribute.get(i);

							//										IT'S A VARIABLE!
							if(att.getName().equals("name")){
								this.var_names.add(att.getValue());
							}
							//										IT'S A LINK!
							if(att.getName().equals("href")){
								this.has_links = true;
								this.link_hrefs.add(att.getValue());
							}
						}
					}//headListIter
				}//endif head

				//				INTO <RESULTS>
				if(sparqlChild.getName().equalsIgnoreCase("results"))
				{											
					List <Element> resultsList = sparqlChild.getChildren();
					Iterator <Element> resultsListIter = resultsList.iterator();

					//				INTO <RESULT>. it iterates the result(s)!
					while(resultsListIter.hasNext())
					{
						//				FLAG HAS RESULTS
						this.has_results = true;

						Element result = resultsListIter.next();
						List <Element> bindingList = result.getChildren();
						Iterator<Element> bindingListIter = bindingList.iterator();

						//									THE SINGLE RESULT STRUCTURE, it is going to be populated
						Vector<String[]> single_result = new Vector<String[]>();

						//				INTO <RESULT> it iterates the bindings!
						while(bindingListIter.hasNext())
						{
							//										here we populate the cell_temp structure (dynamic one, to be transfered to single_cell static String[])
							Vector<String> cell_temp = new Vector<String>();
							Element binding = bindingListIter.next();
							List<Attribute> attribute = binding.getAttributes();

							for (int i =0;i<attribute.size();i++)
							{
								Attribute att = (Attribute) attribute.get(i);

								//											IT'S A BINDING!
								if(att.getName().equals("name")){
									cell_temp.add(att.getValue());
								}
							}

							List <Element> bindingChildren = binding.getChildren();
							Iterator<Element> bindindChildrenIter = bindingChildren.iterator();

							//				INTO a single <BINDING>. it iterates the cell elements
							while (bindindChildrenIter.hasNext()) {
								Element bindingchild = bindindChildrenIter.next();

								if(bindingchild.getName().equals("uri"))
								{
									cell_temp.add(bindingchild.getName());
									cell_temp.add(bindingchild.getValue());
								}
								else if(bindingchild.getName().equals("bnode"))
								{
									cell_temp.add(bindingchild.getName());
									cell_temp.add(bindingchild.getValue());

								}
								else if(bindingchild.getName().equalsIgnoreCase("unbound"))
								{

									cell_temp.add(bindingchild.getName());

								}
								else if(bindingchild.getName().equals("literal"))
								{
									cell_temp.add(bindingchild.getName());
									cell_temp.add(bindingchild.getValue());

									List<Attribute> lit_attribute = bindingchild.getAttributes();

									for (int i =0;i<lit_attribute.size();i++)
									{
										Attribute att = (Attribute) lit_attribute.get(i);

										if(att.getQualifiedName().equals("xml:lang")){
											cell_temp.add(att.getQualifiedName()+"=\""+att.getValue()+"\"");
										}
										else if(att.getQualifiedName().equals("datatype")){
											cell_temp.add(att.getQualifiedName()+"=\""+att.getValue()+"\"");
										}
										else;
									}
								}
							}

							//										now we put the single_cell (Vector) inside the proper structure cell (which is a String[])
							String[] single_cell = null;
							if(cell_temp.size()==3)
							{
								single_cell= new String[3];
								single_cell[VARNAME]=cell_temp.elementAt(VARNAME);
								single_cell[CATEGORY]=cell_temp.elementAt(CATEGORY);
								single_cell[VALUE]=cell_temp.elementAt(VALUE);

							}else if(cell_temp.size()==4)
							{										
								single_cell= new String[4];
								single_cell[VARNAME]=cell_temp.elementAt(VARNAME);
								single_cell[CATEGORY]=cell_temp.elementAt(CATEGORY);
								single_cell[VALUE]=cell_temp.elementAt(VALUE);
								single_cell[TYPE]=cell_temp.elementAt(TYPE);

							}
							else if(cell_temp.size()==2)//unbound in redland SIB
							{			

								single_cell= new String[3];
								single_cell[VARNAME]=cell_temp.elementAt(VARNAME);
								single_cell[CATEGORY]=cell_temp.elementAt(CATEGORY);
								single_cell[VALUE]=null;
							}
							//										else
							//										{																			
							//											//test
							//											System.err.println("Something went wrong in parsing results, sorry!");
							//											System.exit(1);
							//										}

							//										ADDING THE CELL TO SINGLE_RESULT
							single_result.add(single_cell);
						}//bindingListIter

						//								ADDING THE SINGLE_RESULT TO RESULTS									
						this.sparql_response_results.add(single_result);	
					}//resultsListIter
				}//endif results

				//							INTO <BOOLEAN>
				else if(sparqlChild.getName().equalsIgnoreCase("boolean"))
				{
					this.has_booleans = true;
					this.booleans.add(sparqlChild.getValue());
				}//endif boolean
			}//while





		} catch (Exception e)
		{
			System.err.println("EXCEPTION:");
			e.printStackTrace();
			//System.exit(1);
		}

	}

	public SSAP_sparql_response(String ssap_sparql_response)
	{		
		this.seek = -1;
		this.has_results = false;
		this.has_links = false;
		this.has_booleans = false;

		this.query_type_select_ask = false;
		this.query_type_construct_describe = false;

		this.var_names=new Vector<String>();
		this.link_hrefs=new Vector<String>();
		this.booleans=new Vector<String>();
		this.sparql_response_results = new Vector<Vector<String[]>>();

		if(ssap_sparql_response==null)
		{
			System.out.println("ERROR:SSAP_sparql_response:Constructor:XML message is null");
			/*System.exit(2);*/
		}

		try
		{
			//			string formatting and parsing to an XML document
			//			String response_string=formatString(sparql_response);
			//			Document sparql_response_document=loadXMLFromString(response_string);
			Document sparql_response_document=loadXMLFromString(ssap_sparql_response);

			//	        GET ROOT ELEMENT + NAMESPACE
			Element root = sparql_response_document.getRootElement();
			//Namespace ns;

			//			GET ROOT CHILDREN (with name PARAMETER)
			List <Element> rootchidrenList = root.getChildren("parameter");
			Iterator <Element> rootIter = rootchidrenList.iterator();

			while(rootIter.hasNext())
			{
				Element rootchildren = rootIter.next();

				//				GET ROOT CHILDREN (with name PARAMETER and value RESULTS)
				if(rootchildren.getAttributeValue("name").equals("results"))
				{
					List<Element> sparqlList = rootchildren.getChildren();
					Iterator <Element> parameterIter = sparqlList.iterator();

					while(parameterIter.hasNext())
					{
						Element parameterchildren = parameterIter.next();
						//ns=parameterchildren.getNamespace();


						//					QUERY TYPE CONFIRM GRAPH (DESCRIBE/CONSTRUCT)
						if(parameterchildren.getName().equals("RDF")){
							this.query_type_construct_describe = true;
							this.resultGraph = parameterchildren;
						}

						//					QUERY TYPE SPARQL (SELECT/ASK)
						else if(parameterchildren.getName().equals("sparql")){
							this.query_type_select_ask = true;
							List<Element> SparqlChildren= parameterchildren.getChildren();
							Iterator <Element> sparqlIter = SparqlChildren.iterator();

							while(sparqlIter.hasNext())
							{
								Element sparqlchild = sparqlIter.next();
								//ns=sparqlchild.getNamespace();

								//				INTO <HEAD>
								if(sparqlchild.getName().equalsIgnoreCase("head"))
								{
									List <Element> headList = sparqlchild.getChildren();
									Iterator <Element> headListIter = headList.iterator();

									while(headListIter.hasNext())
									{
										Element headchildren = headListIter.next();
										List<Attribute> attribute = headchildren.getAttributes();

										for (int i =0;i<attribute.size();i++)
										{
											Attribute att = (Attribute) attribute.get(i);

											//										IT'S A VARIABLE!
											if(att.getName().equals("name")){
												this.var_names.add(att.getValue());
											}
											//										IT'S A LINK!
											if(att.getName().equals("href")){
												this.has_links = true;
												this.link_hrefs.add(att.getValue());
											}
										}
									}//headListIter
								}//endif head

								//				INTO <RESULTS>
								if(sparqlchild.getName().equalsIgnoreCase("results"))
								{											
									List <Element> resultsList = sparqlchild.getChildren();
									Iterator <Element> resultsListIter = resultsList.iterator();

									//				INTO <RESULT>. it iterates the result(s)!
									while(resultsListIter.hasNext())
									{
										//				FLAG HAS RESULTS
										this.has_results = true;

										Element result = resultsListIter.next();
										List <Element> bindingList = result.getChildren();
										Iterator<Element> bindingListIter = bindingList.iterator();
										int varIndex = 0;

										//									THE SINGLE RESULT STRUCTURE, it is going to be populated
										Vector<String[]> single_result = new Vector<String[]>();

										//				INTO <RESULT> it iterates the bindings!
										while(bindingListIter.hasNext())
										{
											//										here we populate the cell_temp structure (dynamic one, to be transfered to single_cell static String[])
											Vector<String> cell_temp = new Vector<String>();
											Element binding = bindingListIter.next();
											List<Attribute> attribute = binding.getAttributes();

											for (int i =0;i<attribute.size();i++)
											{
												Attribute att = (Attribute) attribute.get(i);

												//											IT'S A BINDING!
												if(att.getName().equals("name")){
													cell_temp.add(att.getValue());
												}
											}

											List <Element> bindingChildren = binding.getChildren();
											Iterator<Element> bindindChildrenIter = bindingChildren.iterator();

											//				INTO a single <BINDING>. it iterates the cell elements
											while (bindindChildrenIter.hasNext()) {
												Element bindingchild = bindindChildrenIter.next();

												if(bindingchild.getName().equals("uri"))
												{
													cell_temp.add(bindingchild.getName());
													cell_temp.add(bindingchild.getValue());
												}
												else if(bindingchild.getName().equals("bnode"))
												{
													cell_temp.add(bindingchild.getName());
													cell_temp.add(bindingchild.getValue());

												}
												else if(bindingchild.getName().equalsIgnoreCase("unbound"))
												{

													cell_temp.add(bindingchild.getName());

												}
												else if(bindingchild.getName().equals("literal"))
												{
													cell_temp.add(bindingchild.getName());
													cell_temp.add(bindingchild.getValue());

													List<Attribute> lit_attribute = bindingchild.getAttributes();

													for (int i =0;i<lit_attribute.size();i++)
													{
														Attribute att = (Attribute) lit_attribute.get(i);

														if(att.getQualifiedName().equals("xml:lang")){
															cell_temp.add(att.getQualifiedName()+"=\""+att.getValue()+"\"");
														}
														else if(att.getQualifiedName().equals("datatype")){
															cell_temp.add(att.getQualifiedName()+"=\""+att.getValue()+"\"");
														}
														else;
													}
												}
											}

											//										now we put the single_cell (Vector) inside the proper structure cell (which is a String[])
											String[] single_cell = null;
											if(cell_temp.size()==3)
											{
												single_cell= new String[3];
												single_cell[VARNAME]=cell_temp.elementAt(VARNAME);
												single_cell[CATEGORY]=cell_temp.elementAt(CATEGORY);
												single_cell[VALUE]=cell_temp.elementAt(VALUE);

											}else if(cell_temp.size()==4)
											{										
												single_cell= new String[4];
												single_cell[VARNAME]=cell_temp.elementAt(VARNAME);
												single_cell[CATEGORY]=cell_temp.elementAt(CATEGORY);
												single_cell[VALUE]=cell_temp.elementAt(VALUE);
												single_cell[TYPE]=cell_temp.elementAt(TYPE);

											}
											else if(cell_temp.size()==2)//unbound in redland SIB
											{			
												//System.out.println("HELLO!!!");
												single_cell= new String[3];
												single_cell[VARNAME]=cell_temp.elementAt(VARNAME);
												single_cell[CATEGORY]=cell_temp.elementAt(CATEGORY);
												single_cell[VALUE]=null;
											}

											//Here we check if there are unbound variables omitted in the xml.
											//If this happens we put an unbound cell in our representation
											while(!single_cell[VARNAME].equals(this.var_names.elementAt(varIndex)))
											{
												String[] dummy_cell = new String[3];
												dummy_cell[VARNAME]=this.var_names.elementAt(varIndex);
												dummy_cell[CATEGORY]="unbound";
												dummy_cell[VALUE]=null;
												single_result.add(dummy_cell);
												varIndex++;
											}


											//										else
											//										{																			
											//											//test
											//											System.err.println("Something went wrong in parsing results, sorry!");
											//											System.exit(1);
											//										}

											//										ADDING THE CELL TO SINGLE_RESULT



											single_result.add(single_cell);
											varIndex++;
										}//bindingListIter

										//								ADDING THE SINGLE_RESULT TO RESULTS									
										this.sparql_response_results.add(single_result);	
									}//resultsListIter
								}//endif results

								//							INTO <BOOLEAN>
								else if(sparqlchild.getName().equalsIgnoreCase("boolean"))
								{
									this.has_booleans = true;
									this.booleans.add(sparqlchild.getValue());
								}//endif boolean

							}//sparqlIter
						}//parameterIter
					}//endif SPARQL type query
				}//endif
			}//rootIter	
		} catch (org.xml.sax.SAXException e) {
			System.err.println("SAX EXCEPTION");
			e.printStackTrace();
			//System.exit(1);
		} catch (java.io.IOException e) {
			System.err.println("IO EXCEPTION");
			e.printStackTrace();
			//System.exit(1);
		}catch (JDOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			System.err.println("Sorry, this string is an invalid XML SSAP RESPONSE to a sparql query");
			e.printStackTrace();
			//System.exit(1);
		}
	}

	/**
	 * Constructor with a Jdom2 Document representing the response as input
	 * @param select_ask_xmlElement
	 */
	public SSAP_sparql_response(Element ssap_sparql_response_element)
	{		
		this.seek = -1;
		this.has_results = false;
		this.has_links = false;
		this.has_booleans = false;

		this.query_type_select_ask = false;
		this.query_type_construct_describe = false;

		this.var_names=new Vector<String>();
		this.link_hrefs=new Vector<String>();
		this.booleans=new Vector<String>();
		this.sparql_response_results = new Vector<Vector<String[]>>();


		try
		{



			List<Element> sparqlChildren= ssap_sparql_response_element.getChildren();
			//Namespace ns = ssap_sparql_response_element.getNamespace();
			Iterator <Element> sparqlIter = sparqlChildren.iterator();

			while(sparqlIter.hasNext())
			{
				Element sparqlChild = sparqlIter.next();


				//				INTO <HEAD>
				if(sparqlChild.getName().equalsIgnoreCase("head"))
				{
					List <Element> headList = sparqlChild.getChildren();
					Iterator <Element> headListIter = headList.iterator();

					while(headListIter.hasNext())
					{
						Element headchildren = headListIter.next();
						List<Attribute> attribute = headchildren.getAttributes();

						for (int i =0;i<attribute.size();i++)
						{
							Attribute att = (Attribute) attribute.get(i);

							//										IT'S A VARIABLE!
							if(att.getName().equals("name")){
								this.var_names.add(att.getValue());
							}
							//										IT'S A LINK!
							if(att.getName().equals("href")){
								this.has_links = true;
								this.link_hrefs.add(att.getValue());
							}
						}
					}//headListIter
				}//endif head

				//				INTO <RESULTS>
				if(sparqlChild.getName().equalsIgnoreCase("results"))
				{											
					List <Element> resultsList = sparqlChild.getChildren();
					Iterator <Element> resultsListIter = resultsList.iterator();

					//				INTO <RESULT>. it iterates the result(s)!
					while(resultsListIter.hasNext())
					{
						//				FLAG HAS RESULTS
						this.has_results = true;

						Element result = resultsListIter.next();
						List <Element> bindingList = result.getChildren();
						Iterator<Element> bindingListIter = bindingList.iterator();

						//									THE SINGLE RESULT STRUCTURE, it is going to be populated
						Vector<String[]> single_result = new Vector<String[]>();


						int varIndex = 0;
						//				INTO <RESULT> it iterates the bindings!
						while(bindingListIter.hasNext())
						{
							//										here we populate the cell_temp structure (dynamic one, to be transfered to single_cell static String[])
							Vector<String> cell_temp = new Vector<String>();
							Element binding = bindingListIter.next();
							List<Attribute> attribute = binding.getAttributes();

							for (int i =0;i<attribute.size();i++)
							{
								Attribute att = (Attribute) attribute.get(i);

								//											IT'S A BINDING!
								if(att.getName().equals("name")){
									cell_temp.add(att.getValue());
								}
							}

							List <Element> bindingChildren = binding.getChildren();
							Iterator<Element> bindindChildrenIter = bindingChildren.iterator();

							//				INTO a single <BINDING>. it iterates the cell elements
							while (bindindChildrenIter.hasNext()) {
								Element bindingchild = bindindChildrenIter.next();

								if(bindingchild.getName().equals("uri"))
								{
									cell_temp.add(bindingchild.getName());
									cell_temp.add(bindingchild.getValue());
								}
								else if(bindingchild.getName().equals("bnode"))
								{
									cell_temp.add(bindingchild.getName());
									cell_temp.add(bindingchild.getValue());

								}
								else if(bindingchild.getName().equalsIgnoreCase("unbound"))
								{

									cell_temp.add(bindingchild.getName());

								}
								else if(bindingchild.getName().equals("literal"))
								{
									cell_temp.add(bindingchild.getName());
									cell_temp.add(bindingchild.getValue());

									List<Attribute> lit_attribute = bindingchild.getAttributes();

									for (int i =0;i<lit_attribute.size();i++)
									{
										Attribute att = (Attribute) lit_attribute.get(i);

										if(att.getQualifiedName().equals("xml:lang")){
											cell_temp.add(att.getQualifiedName()+"=\""+att.getValue()+"\"");
										}
										else if(att.getQualifiedName().equals("datatype")){
											cell_temp.add(att.getQualifiedName()+"=\""+att.getValue()+"\"");
										}
										else;
									}
								}
							}

							//										now we put the single_cell (Vector) inside the proper structure cell (which is a String[])
							String[] single_cell = null;
							if(cell_temp.size()==3)
							{
								single_cell= new String[3];
								single_cell[VARNAME]=cell_temp.elementAt(VARNAME);
								single_cell[CATEGORY]=cell_temp.elementAt(CATEGORY);
								single_cell[VALUE]=cell_temp.elementAt(VALUE);

							}else if(cell_temp.size()==4)
							{										
								single_cell= new String[4];
								single_cell[VARNAME]=cell_temp.elementAt(VARNAME);
								single_cell[CATEGORY]=cell_temp.elementAt(CATEGORY);
								single_cell[VALUE]=cell_temp.elementAt(VALUE);
								single_cell[TYPE]=cell_temp.elementAt(TYPE);

							}
							else if(cell_temp.size()==2)//unbound in redland SIB
							{			

								single_cell= new String[3];
								single_cell[VARNAME]=cell_temp.elementAt(VARNAME);
								single_cell[CATEGORY]=cell_temp.elementAt(CATEGORY);
								single_cell[VALUE]=null;
							}

							//Here we check if there are unbound variables omitted in the xml.
							//If this happens we put an unbound cell in our representation
							while(!single_cell[VARNAME].equals(this.var_names.elementAt(varIndex)))
							{
								String[] dummy_cell = new String[3];
								dummy_cell[VARNAME]=this.var_names.elementAt(varIndex);
								dummy_cell[CATEGORY]="unbound";
								dummy_cell[VALUE]=null;
								single_result.add(dummy_cell);
								varIndex++;
							}




							//										else
							//										{																			
							//											//test
							//											System.err.println("Something went wrong in parsing results, sorry!");
							//											System.exit(1);
							//										}

							//										ADDING THE CELL TO SINGLE_RESULT
							single_result.add(single_cell);
							varIndex++;
						}//bindingListIter

						//								ADDING THE SINGLE_RESULT TO RESULTS									
						this.sparql_response_results.add(single_result);	
					}//resultsListIter
				}//endif results

				//							INTO <BOOLEAN>
				else if(sparqlChild.getName().equalsIgnoreCase("boolean"))
				{
					this.has_booleans = true;
					this.booleans.add(sparqlChild.getValue());
				}//endif boolean
			}//while





		} catch (Exception e)
		{
			System.err.println("EXCEPTION:");
			e.printStackTrace();
			//System.exit(1);
		}

	}


	/*
	 * Returns a human readable representation of the results
	 */
	public String print_as_string()
	{
		String temp ="";
		for (int i = 0; i < sparql_response_results.size();i++)
		{
			temp = temp + i + ": " ;
			for (int j = 0; j< sparql_response_results.elementAt(i).size();j++)
			{
				if(sparql_response_results.elementAt(i).elementAt(j)!= null)
					temp = temp+ SSAP_sparql_response.getCellName(sparql_response_results.elementAt(i).elementAt(j))+ " = " + SSAP_sparql_response.getCellValue(sparql_response_results.elementAt(i).elementAt(j)) + " ";
			}
			temp = temp + "\n" ;
		}
		return temp ;
	}

	/*
	 * Empty constructor, it does not anything
	 */
	public SSAP_sparql_response()
	{

	}

	/*
	 * Builds the fields of SSAP_sparql_response starting from a string written in xml and accordin to the W3C xml schema for sparql responses
	 * of type select or ask
	 */
	public void init_from_sparql_response_string(String select_ask_response_String)
	{		
		this.seek = -1;
		this.has_results = false;
		this.has_links = false;
		this.has_booleans = false;

		this.query_type_select_ask = false;
		this.query_type_construct_describe = false;

		this.var_names=new Vector<String>();
		this.link_hrefs=new Vector<String>();
		this.booleans=new Vector<String>();
		this.sparql_response_results = new Vector<Vector<String[]>>();

		if(select_ask_response_String==null)
		{
			System.out.println("ERROR:SSAP_sparql_response:Constructor:XML message is null");
			//System.exit(2);
		}

		try
		{
			//			string formatting and parsing to an XML document
			//			String response_string=formatString(sparql_response);
			//			Document sparql_response_document=loadXMLFromString(response_string);


			//	        GET ROOT ELEMENT + NAMESPACE
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(new ByteArrayInputStream(select_ask_response_String.getBytes()));
			Element root = doc.getRootElement();
			//Namespace ns;


			List<Element> head= root.getChildren();
			Iterator <Element> sparqlIter = head.iterator();

			while(sparqlIter.hasNext())
			{
				Element sparqlchildren = sparqlIter.next();
				//ns=sparqlchildren.getNamespace();

				//				INTO <HEAD>
				if(sparqlchildren.getName().equalsIgnoreCase("head"))
				{
					List <Element> headList = sparqlchildren.getChildren();
					Iterator <Element> headListIter = headList.iterator();

					while(headListIter.hasNext())
					{
						Element headchildren = headListIter.next();
						List<Attribute> attribute = headchildren.getAttributes();

						for (int i =0;i<attribute.size();i++)
						{
							Attribute att = (Attribute) attribute.get(i);

							//										IT'S A VARIABLE!
							if(att.getName().equals("name")){
								this.var_names.add(att.getValue());
							}
							//										IT'S A LINK!
							if(att.getName().equals("href")){
								this.has_links = true;
								this.link_hrefs.add(att.getValue());
							}
						}
					}//headListIter
				}//endif head

				//				INTO <RESULTS>
				if(sparqlchildren.getName().equalsIgnoreCase("results"))
				{											
					List <Element> resultsList = sparqlchildren.getChildren();
					Iterator <Element> resultsListIter = resultsList.iterator();

					//				INTO <RESULT>. it iterates the result(s)!
					while(resultsListIter.hasNext())
					{
						//				FLAG HAS RESULTS
						this.has_results = true;

						Element result = resultsListIter.next();
						List <Element> bindingList = result.getChildren();
						Iterator<Element> bindingListIter = bindingList.iterator();

						//									THE SINGLE RESULT STRUCTURE, it is going to be populated
						Vector<String[]> single_result = new Vector<String[]>();

						//				INTO <RESULT> it iterates the bindings!
						while(bindingListIter.hasNext())
						{
							//										here we populate the cell_temp structure (dynamic one, to be transfered to single_cell static String[])
							Vector<String> cell_temp = new Vector<String>();
							Element binding = bindingListIter.next();
							List<Attribute> attribute = binding.getAttributes();

							for (int i =0;i<attribute.size();i++)
							{
								Attribute att = (Attribute) attribute.get(i);

								//											IT'S A BINDING!
								if(att.getName().equals("name")){
									cell_temp.add(att.getValue());
								}
							}

							List <Element> bindingChildren = binding.getChildren();
							Iterator<Element> bindindChildrenIter = bindingChildren.iterator();

							//				INTO a single <BINDING>. it iterates the cell elements
							while (bindindChildrenIter.hasNext()) {
								Element bindingchild = bindindChildrenIter.next();

								if(bindingchild.getName().equals("uri"))
								{
									cell_temp.add(bindingchild.getName());
									cell_temp.add(bindingchild.getValue());
								}
								else if(bindingchild.getName().equals("bnode"))
								{
									cell_temp.add(bindingchild.getName());
									cell_temp.add(bindingchild.getValue());

								}
								else if(bindingchild.getName().equalsIgnoreCase("unbound"))
								{

									cell_temp.add(bindingchild.getName());

								}
								else if(bindingchild.getName().equals("literal"))
								{
									cell_temp.add(bindingchild.getName());
									cell_temp.add(bindingchild.getValue());

									List<Attribute> lit_attribute = bindingchild.getAttributes();

									for (int i =0;i<lit_attribute.size();i++)
									{
										Attribute att = (Attribute) lit_attribute.get(i);

										if(att.getQualifiedName().equals("xml:lang")){
											cell_temp.add(att.getQualifiedName()+"=\""+att.getValue()+"\"");
										}
										else if(att.getQualifiedName().equals("datatype")){
											cell_temp.add(att.getQualifiedName()+"=\""+att.getValue()+"\"");
										}
										else;
									}
								}

							}

							//										now we put the single_cell (Vector) inside the proper structure cell (which is a String[])
							String[] single_cell = null;
							if(cell_temp.size()==3)
							{
								single_cell= new String[3];
								single_cell[VARNAME]=cell_temp.elementAt(VARNAME);
								single_cell[CATEGORY]=cell_temp.elementAt(CATEGORY);
								single_cell[VALUE]=cell_temp.elementAt(VALUE);

							}else if(cell_temp.size()==4)
							{										
								single_cell= new String[4];
								single_cell[VARNAME]=cell_temp.elementAt(VARNAME);
								single_cell[CATEGORY]=cell_temp.elementAt(CATEGORY);
								single_cell[VALUE]=cell_temp.elementAt(VALUE);
								single_cell[TYPE]=cell_temp.elementAt(TYPE);

							}
							else if(cell_temp.size()==2)//unbound in redland SIB
							{			

								single_cell= new String[3];
								single_cell[VARNAME]=cell_temp.elementAt(VARNAME);
								single_cell[CATEGORY]=cell_temp.elementAt(CATEGORY);
								single_cell[VALUE]=null;
							}
							//										else
							//										{																			
							//											//test
							//											System.err.println("Something went wrong in parsing results, sorry!");
							//											System.exit(1);
							//										}

							//										ADDING THE CELL TO SINGLE_RESULT
							single_result.add(single_cell);
						}//bindingListIter

						//								ADDING THE SINGLE_RESULT TO RESULTS									
						this.sparql_response_results.add(single_result);	
					}//resultsListIter
				}//endif results

				//							INTO <BOOLEAN>
				else if(sparqlchildren.getName().equalsIgnoreCase("boolean"))
				{
					this.has_booleans = true;
					this.booleans.add(sparqlchildren.getValue());
				}//endif boolean

			}//sparqlIter

		} 
		catch (Exception e) 
		{
			System.err.println("SAX EXCEPTION");
			e.printStackTrace();
			//System.exit(1);
		} 

	}

	/**
	 * This method can be used to remove spaces, tabulations, new lines and other characters not needed for computation, but for visualization 
	 * @param human readable sparql_response
	 * @return sparql_response without characters not needed for computation
	 */
	public String formatString(String sparql_res) {

		String new_response = new String();

		//	   replaces end of lines
		new_response = sparql_res.replace("\n", "");

		//	   replaces double spaces
		boolean double_space = sparql_res.contains("  ");
		while(double_space){
			new_response = new_response.replace("  ", " ");
			double_space = new_response.contains("  ");
		}
		new_response = new_response.replace("> <", "><");

		return new_response;
	}


	/**
	 * A simple XML parser from a string. Returns a Document which represents the root of the document tree.
	 * 
	 * @param xml
	 * @return the XML Document for the message received as a string
	 * @throws Exception
	 */
	private static Document loadXMLFromString(String xml) throws Exception
	{
		SAXBuilder builder = new SAXBuilder();
		//System.out.println("xml = " + xml);
		Document doc = builder.build(new ByteArrayInputStream(xml.getBytes(Charset.forName("UTF-8"))));
		

		return doc;
	}

	/**
	 * Returns the ordered sequence of variable names as the value of the attribute "name" of the result elements
	 * This method is public
	 * @return
	 */
	public Vector<String> getVariablesNames(){
		return this.var_names;
	}

	/**
	 * If the SSAP sparql query response contains at least one link child element with an href attribute
	 * (containing a relative URI that provides a link to some additional metadata about the query results) that could
	 * appear after any variable elements that are present.
	 * 
	 * @return Returns the ordered sequence of links contained in the <head> element of the SSAP message
	 */
	public Vector<String> getLinksHrefs(){
		return this.link_hrefs;
	}

	/**
	 * Returns the ordered sequence of results
	 * @return
	 */
	public Vector<Vector<String[]>> getResults(){
		return this.sparql_response_results;
	}

	/**
	 * This method tells if the SSAP sparql query response contains at least one result.
	 * This method is public.
	 * @return
	 */
	public boolean hasResults(){
		return this.has_results;
	}

	public boolean hasBooleans(){
		return this.has_booleans;
	}

	/**
	 * This method tells if the SSAP sparql query response contains at least one link child element with an href attribute
	 * (containing a relative URI that provides a link to some additional metadata about the query results) that could
	 * appear after any variable elements that are present.
	 * This method is public.
	 * @return
	 */
	public boolean hasLinks(){
		return this.has_links;
	}

	/**
	 * Checks if seek is set to the maximum value possible (i.e. the results size)
	 * @return true if it possible to get another result through getNextRow(), false otherwise
	 */
	public boolean hasNext(){
		return ((size() - 1) > seek);
	}

	/**
	 * 
	 * @param cell
	 * @return the variable name of the binding element
	 */
	public static String getCellName(String[] cell){
		if(cell!=null)
		{
			return cell[VARNAME];
		}
		return null;
	}

	/**
	 * 
	 * @param cell
	 * @return the value of the binding element
	 */
	public static String getCellValue(String[] cell){
		if(cell!=null)
		{
			return cell[VALUE];
		}
		return null;
	}

	/**
	 * 
	 * @param cell
	 * @return the category of the binding element ("uri", "literal", "bnode", "unbound")
	 */
	public static String getCellCategory(String[] cell){
		if(cell!=null)
		{
			return cell[CATEGORY];
		}
		return null;
	}

	/**
	 * 
	 * @param cell
	 * @return the type of the binding element ("xml:lang" plus its value or "datatype" plus its value) in an unique string
	 */
	public static String getCellType(String[] cell){
		//	        return cell[3] containing the  VALUE of the type (xml:lang = ..., or datatype=...)
		if(cell!=null)
		{
			if(cell[CATEGORY].equals("literal"))
			{
				if (cell.length==4)	return cell[TYPE];
				else {
					System.err.println("Sorry this cell is a simple \"literal\"");
					return null;
				}
			}
			else{
				System.err.println("Sorry this cell is not a \"literal\"");
				//	System.exit(1);
				return null;
			}
		}
		return null;
	}

	/**
	 * 
	 * @param cell
	 * @return the type name of the literal element ("xml:lang" or "datatype")
	 */
	public static String getCellLiteralTypeName(String[] cell){
		if(cell!=null)
		{
			if(cell[CATEGORY].equals("literal"))
			{
				if ( cell[TYPE].contains("xml:lang")) return "xml:lang";
				else if (cell[TYPE].contains("datatype")) return "datatype";
				else{
					System.err.println("Sorry this cell is neither a xml:lang nor a datatype");
					//System.exit(1);
					return null;
				}
			}
			else{
				System.err.println("Sorry this cell is not a \"literal\"");
				//System.exit(1);
				return null;
			}
		}
		return null;

	}

	/**
	 * 
	 * @param cell
	 * @return the type value of the literal element (after "xml:lang" or "datatype")
	 */
	public static String getCellLiteralTypeValue(String[] cell){
		if(cell!=null)
		{
			if(cell[CATEGORY].equals("literal"))
			{
				if ( cell[TYPE].contains("xml:lang")){
					String [] split = cell[TYPE].split("xml:lang=");
					return split[1];
				}
				else if (cell[TYPE].contains("datatype")){
					String [] split = cell[TYPE].split("dataype=");
					return split[1];
				}
				else{
					System.err.println("Sorry this cell is neither a xml:lang nor a datatype");
					//		System.exit(1);
					return null;
				}
			}
			else{
				System.err.println("Sorry this cell is not a \"literal\"");
				//	System.exit(1);
				return null;
			}
		}
		return null;
	}


	/**
	 * This method tells if the SSAP sparql query response contains at least one boolean.
	 * This method is public.
	 * @return
	 */
	public Vector<String> getBooleans() {
		return this.booleans;

	}

	/**
	 * 
	 * @return the number of the results inside the ssap sparql response
	 */
	public int size(){
		return sparql_response_results.size();
	}

	/**
	 * 
	 * @param index
	 * @return a Vector<String[]> representing the single result row referred by the index
	 */
	public Vector<String[]> getRow(int index){

		//		TODO: maybe add a "Throws Exception" declaration to avoid syserr messages
		if(this.sparql_response_results.size()<=index){
			System.err.println("index out of bounds. The dimension of the result set is " + sparql_response_results.size());
			//			return null;
			//	System.exit(1);
		}
		Vector<String[]> single_result_vector = this.sparql_response_results.elementAt(index);
		return single_result_vector;
	}

	/**
	 * Returns a Vector<String[]> representing the single result row referred by the seek+1 value
	 * This method updates the seek value before returning the row.
	 * @return
	 * @deprecated
	 */
	public Vector<String[]> getNextRow(){

		//		UPDATE SEEK VALUE!
		this.seek(seek+1);


		if(this.sparql_response_results.size()<=this.getSeek()){
			System.err.println("Already at the last element. Nothing done.");
			return null;
			//	System.exit(1);
		}
		Vector<String[]> single_result_vector = this.sparql_response_results.elementAt(this.getSeek());
		return single_result_vector;
	}

	/**
	 * Returns the result correspondding to the current value of seek
	 * @return a row of results
	 */
	public Vector<String[]> getRow(){


		if(this.sparql_response_results.size()==this.getSeek())
		{
			System.err.println("Already at the last element. Nothing done.");
			return null;	
		}
		Vector<String[]> single_result_vector = this.sparql_response_results.elementAt(this.seek);
		return single_result_vector;
	}

	public boolean next()
	{
		if (this.seek < this.sparql_response_results.size())
		{
			this.seek = this.seek + 1;
			return true;
		}
		else
		{
			System.err.println("SSAP_sparql_response already at the last element. Nothing done.");
			return false;
		}
	}
	/**
	 * Gives the number of results for the current response or -1
	 * @return the number of results for the current response or -1
	 */
	public int resultsNumber()
	{
		if(this.sparql_response_results != null)
		{
			return this.sparql_response_results.size();
		}
		else
		{
			return -1;
		}
	}
	/**
	 * Sets the seek index to the specified value.
	 * @param seek goes from 0 to length-1
	 */
	public void seek (int seek){

		//		TODO: maybe add a "Throws IndexOutOfBoundsException" declaration to avoid syserr messages
		if(this.sparql_response_results.size()<=seek){
			System.err.println("ERROR: index out of bounds. The dimension of the result set is" + sparql_response_results.size());
			//System.exit(1);
		}

		this.seek=seek;
	}


	/**
	 * 
	 * @return the value of the seek index
	 */
	public int getSeek(){
		return this.seek;
	}


	/**
	 * This method returns all the <binding> elements with the specified variable name
	 * @param varname
	 * @return return a Vector<String[]> with all the <binding> elements containing the specified variable name
	 */
	public Vector<String[]> getResultsForVar(String varname){

		Vector<String[]> res = new Vector<String[]>();

		for (int i = 0; i < this.sparql_response_results.size(); i++)
		{
			Vector<String[]> single_result_vector = sparql_response_results.elementAt(i);

			for (int j = 0; j < single_result_vector.size(); j++) 
			{
				String[] row = single_result_vector.elementAt(j);
				if(row[0].equals(varname)){
					res.add(row);
				}
			}
		}
		return res;
	}

	public String getValueForVarName(String varName)
	{
		return SSAP_sparql_response.getCellValue(this.getRow().get(var_names.indexOf(varName)));
	}
	
	public String getValueCategoryForVarName(String varName)
	{
		return SSAP_sparql_response.getCellCategory(this.getRow().get(var_names.indexOf(varName)));
	}


	public boolean isQueryTypeSelect(){
		return this.query_type_select_ask;
	}

	public boolean isQueryTypeConstruct(){
		return this.query_type_construct_describe;
	}

	public Element getGraph(){
		if (this.query_type_construct_describe)
		{
			return this.resultGraph;
		}else
		{
			System.err.println("Sorry, this query is a sparql SELECT/ASK type. No graph to return");
			return null;
		}
	}
	/**
	 * Print the RDF/XML representation of a graph returned in response to a CONSTRUCT or DESCRIBE query
	 */
	public void printGraph(Element graph){
		if (this.query_type_construct_describe)
		{
			try
			{
				XMLOutputter out = new XMLOutputter();
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				out.output(graph, baos);
				System.out.println(baos.toString());
			} catch (Exception e)
			{
				System.err.println("This object represent the result of query that is a sparql SELECT/ASK type. No graph to print");
			}
		}else 
		{
			System.err.println("Sorry, this query is a sparql SELECT/ASK type. No graph to print");
		}
	}
}
