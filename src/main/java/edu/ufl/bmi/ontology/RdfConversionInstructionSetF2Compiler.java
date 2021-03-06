package edu.ufl.bmi.ontology;

import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.IOException;

import java.text.ParseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import edu.ufl.bmi.misc.IriLookup;

/**
  * This class is a pseudocompiler that takes an instruction set file and stages
  *  appropriate instances of various instruction classes to carry out the 
  *  instruction in code.
  *  
  * Here, "F2" means format 2 for instruction set syntax.  This version of the 
  *  syntax is more data driven, meaning that if a field is either not present
  *  or has a blank value, the instructions associated with that field are 
  *  skipped.
 */
public class RdfConversionInstructionSetF2Compiler {
	String fileName;

	//ArrayList<RdfConversionInstruction> instructionList;
	IriLookup iriMap;
	OWLDataFactory odf;
	HashMap<String, HashMap<String, OWLNamedIndividual>> searchIndexes;
	IriRepository iriRepository;
	String iriRepositoryPrefix;
	String uniqueIdFieldName;
	String iriPrefix;

	static final String VARIABLE_PATTERN = "\\[(.*)\\]";
	static final String FOR_EACH_VARIABLE_PATTERN = "foreach \\[(.*)\\]";

	public RdfConversionInstructionSetF2Compiler(String fName, IriLookup iriMap, OWLDataFactory odf, 
		HashMap<String, HashMap<String, OWLNamedIndividual>> searchIndexes, IriRepository iriRepository, 
		String iriRepositoryPrefix, String uniqueIdFieldName, String iriPrefix) {
		this.fileName = fName;
		this.iriMap = iriMap;
		this.odf = odf;
		this.searchIndexes = searchIndexes;
		this.iriRepository = iriRepository;
		this.iriRepositoryPrefix = iriRepositoryPrefix;
		this.uniqueIdFieldName = uniqueIdFieldName;
		this.iriPrefix = iriPrefix;
	}

	public RdfConversionInstructionSetExecutor compile() throws ParseException {
		RdfConversionInstructionSetExecutor rcise = new RdfConversionInstructionSetExecutor();
		try {
			FileReader fr = new FileReader(fileName);
			LineNumberReader lnr = new LineNumberReader(fr);
			String line;

			ArrayList<RdfConversionInstruction> instructionList = null; 
			Pattern variablePattern = Pattern.compile(VARIABLE_PATTERN);
			Pattern foreachPattern = Pattern.compile(FOR_EACH_VARIABLE_PATTERN);

			String elementName = "";
			boolean multiple = false;
			while((line=lnr.readLine())!=null) {
				//System.err.println(line);
				line = line.trim();  //ignore any leading and trailing whitespace
				//skip all blank lines and comment lines
				if (line.length() == 0 || line.startsWith("#")) continue;

				Matcher mVariable = variablePattern.matcher(line);
				Matcher m = (mVariable.matches()) ? mVariable : foreachPattern.matcher(line);
				//if the line constitutes a variable name, then we're starting a new section
				if (m.matches()) {
					//the first time through the current variable name is empty, so only do 
					// this section if we're changing variable names
					if (elementName.length() > 0) {
						//save away the instruction set in the hash by its associated variable name
						RdfConversionInstructionSet s = (multiple) ? new RdfConversionMultipleValueConversionInstructionSet(elementName, instructionList) :
																	new RdfConversionInstructionSet(instructionList);
						boolean added = rcise.addInstructionSetForElement(elementName, s);
						multiple = !mVariable.matches();
						if (!added) {
							System.err.println("instructions for element " + elementName + " were not added to " +
								"the instruction set execution engine.");
						}
					}
					//prepare a new instruction list for the next variable
					instructionList = new ArrayList<RdfConversionInstruction>();
					// the variable name should be in group 1 of the instruction set
					elementName = m.group(1).trim();
					System.out.println("ELEMENT NAME IS " + elementName);
				} else {
					if (line.contains("\\[")) System.err.println("line has [ but pattern did not match.");
				
					//an instruction is two parts -- instruction type : instruction content
					String[] flds = line.split(Pattern.quote(":"), 2);
					//System.out.println(flds.length + ", " + flds[0] + ", " + line);
					String instructionType = flds[0].trim();
					if (flds.length==1) System.err.println(lnr.getLineNumber() + ": " + line);
					String instruction = flds[1].trim();
				
					RdfConversionInstruction rci = compileInstruction(instructionType, instruction);
					instructionList.add(rci);
				}
			}
			/*
			 *  Needed to get the last instruction set read in.  Can probably make this
			 *    more elegant by switching while() loop to do...while() loop.
			 */
			RdfConversionInstructionSet s = (multiple) ? new RdfConversionMultipleValueConversionInstructionSet(elementName, instructionList) :
														new RdfConversionInstructionSet(instructionList);
			boolean added = rcise.addInstructionSetForElement(elementName, s);
			if (!added) {
				System.err.println("instructions for element " + elementName + " were not added to " +
					"the instruction set execution engine.");
			}

		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

		rcise.initializeVariables(this.iriMap, this.odf);

		return rcise;
	}

	//public RdfConversionInstructionSet getInstructionSet() {
//
//	}

	public RdfConversionInstruction compileInstruction(String instructionType, String instruction) throws ParseException {
		String[] flds = instruction.split(Pattern.quote("\t"));
		if (instructionType.equals("annotation")) {		
			if (flds.length != 3) throw new ParseException(
				"annotation instruction must have three, tab delimited fields: " +  instruction, 1);
			String variableName = flds[0].trim();
			String annotationPropertyTxt = flds[1].trim();
			String annotationValueInstruction = flds[2].trim();
			RdfConversionAnnotationInstruction rcai = new RdfConversionAnnotationInstruction(iriMap, odf, 
				variableName, annotationPropertyTxt, annotationValueInstruction);
			return rcai;
		} else if (instructionType.equals("new-individual")) {
			if (flds.length != 4 && flds.length != 5 && flds.length!=6) throw new ParseException(
				"new individual instruction must have four, tab-delimited fields: " + instruction, 2);
			String variableName = flds[0].trim();
			String classIriTxt = flds[1].trim();
			String annotationPropertyTxt = flds[2].trim();
			String annotationValueInstruction = flds[3].trim();
			RdfConversionNewIndividualInstruction rcnii = new RdfConversionNewIndividualInstruction(
					iriMap, odf, variableName, classIriTxt, annotationPropertyTxt, annotationValueInstruction, 
					iriRepository, iriRepositoryPrefix, uniqueIdFieldName);
			if (flds.length == 5) {
				String fieldFive = flds[4].trim();
				if (fieldFive.startsWith("iri=")) {
					String iriTxt = fieldFive.substring(4);  System.out.println("individual IRI assignment = " + iriTxt);
					rcnii.setIriSourceVariableName(iriTxt);
				} else {
					rcnii.setCreationConditionLogic(fieldFive);
				}
			} else if (flds.length == 6) {
				String creationConditionLogic = flds[4].trim();
				String iriField = flds[5].trim();
				String iriTxt = (iriField.startsWith("iri=")) ? iriField.substring(4) : "";
				rcnii.setCreationConditionLogic(creationConditionLogic);
				rcnii.setIriSourceVariableName(iriTxt);
			}
			return rcnii;
		} else if (instructionType.equals("data-property-expression")) {
			if (flds.length != 4) throw new ParseException(
				"data property expression instruction must have four, tab-delimited fields: " + instruction, 3);
			String variableName = flds[0].trim();
			String dataPropertyIriTxt = flds[1].trim();
			String dataValueInstruction = flds[2].trim(); //.replace("[","").replace("]","");
			String dataType = flds[3].trim();
			RdfConversionDataInstruction rcdi = new RdfConversionDataInstruction(iriMap, odf, variableName, 
					dataPropertyIriTxt, dataValueInstruction, dataType);
			return rcdi;
		} else if (instructionType.equals("object-property-expression")) {
			if (flds.length != 3) throw new ParseException(
				"object property expression instructions require three, tab-delimited fields.", 4);
			String sourceVariableName = flds[0].trim();
			String objectPropertyIriTxt = flds[1].trim();
			String targetVariableName = flds[2].trim();
			RdfConversionObjectPropertylInstruction rcopi = new RdfConversionObjectPropertylInstruction(iriMap, 
					odf, sourceVariableName, objectPropertyIriTxt, targetVariableName);
			return rcopi;
		} else if (instructionType.equals("lookup-individual")) {
			if (flds.length != 2) throw new ParseException(
				"lookup individual instructions must have two, tab-delimited fields: " + instruction, 5);
			String variableName = flds[0].trim();
			String searchFieldName = flds[1].trim().replace("[","").replace("]","");
			RdfConversionLookupInstruction rclii = new RdfConversionLookupInstruction(iriMap, 
					odf, variableName, searchFieldName, searchIndexes);
			return rclii;
		} else if (instructionType.equals("lookup-individual-by-element-value")) {
			if (flds.length !=2) throw new ParseException(
				"lookup-individual-by-element-value instuctions must have two, tab-delimited fields: " + instruction, 9);
			String variableName = flds[0].trim();
			String lookupFieldName = flds[1].trim().replace("[","").replace("]","");
			RdfConversionLookupByElementValueInstruction rclbevi = new RdfConversionLookupByElementValueInstruction(
				iriMap, odf, variableName, lookupFieldName, searchIndexes);
			return rclbevi;
		} else if (instructionType.equals("class-assertion-expression")) {
			if (flds.length !=2) throw new ParseException(
				"class assertion expressions must have two, tab-delimited fields: " + instruction, 7);
			String variableName = flds[0].trim();
			String classIriHandle = flds[1].trim();
			RdfClassAssertionInstruction rcai = new RdfClassAssertionInstruction(iriMap, odf, 
				variableName, classIriHandle);
			return rcai;
		} else if (instructionType.equals("query-individual")) {
			if (flds.length != 4 && flds.length != 5) throw new ParseException(
				"query individual expressions must have four or five, tab-delimited fields." + instruction, 8);
			String variableName = flds[0].trim(); 					// e.g., affiliation-org
			String rowTypeName = flds[1].trim();					// e.g., organization
			String externalFileFieldName = flds[2].trim();			// e.g., ID
			String lookupValueFieldName = flds[3].trim();			// e.g., [OrganizationAffiliationID]
			/*
			IriLookup iriMap, HashMap<String,Integer> fieldNameToIndex, OWLDataFactory odf, String variableName, 
			IriRepository iriRepository, String iriRepositoryPrefix, String externalFileFieldName, String externalFileRowTypeName, String iriPrefix,
			String lookupValueFieldName
			*/
			RdfConversionQueryIndividualInstruction rcqii = new RdfConversionQueryIndividualInstruction(
				iriMap, odf, variableName, iriRepository, iriRepositoryPrefix, externalFileFieldName, 
				rowTypeName, iriPrefix, lookupValueFieldName);
			if (flds.length == 5) rcqii.setLookupVariableName(flds[4].trim());
			 
			return rcqii;
		} else if (instructionType.equals("query-individual-by-attribute-value")) {
			if (flds.length != 5 && flds.length != 6) throw new ParseException(
				"query individual expressions must have four or five, tab-delimited fields." + instruction, 10);
			String variableName = flds[0].trim(); 					// e.g., affiliation-org
			String rowTypeName = flds[1].trim();					// e.g., organization
			String externalFileFieldName = flds[2].trim();			// e.g., ID
			String lookupValueFieldName = flds[3].trim();			// e.g., [OrganizationAffiliationID]
			String lookupUniqueFieldName = flds[4].trim();
			String searchInstructions = (flds.length == 6) ? flds[5].trim() : null;
			/*
			IriLookup iriMap, HashMap<String,Integer> fieldNameToIndex, OWLDataFactory odf, String variableName, 
			IriRepository iriRepository, String iriRepositoryPrefix, String externalFileFieldName, String externalFileRowTypeName, String iriPrefix,
			String lookupValueFieldName
			*/
			RdfConversionQueryIndividualByAttributeValueInstruction rcqibav = new RdfConversionQueryIndividualByAttributeValueInstruction(
				iriMap, odf, variableName, iriRepository, iriRepositoryPrefix, externalFileFieldName, 
				rowTypeName, iriPrefix, lookupValueFieldName, lookupUniqueFieldName, searchInstructions);
						 
			return rcqibav;
		} else if (instructionType.equals("lookup-mapping-to-individual")) {
			if (flds.length != 4) throw new ParseException(
				"mapping lookup instructions must have four, tab-delimited fields.", 11);

			String variableName = flds[0].trim(); 
			String lookupValueFieldName = flds[1].trim();	
			String mappingRdfFileName = flds[2].trim();
			String sparqlQueryTemplate = flds[3].trim();
			//IriLookup iriMap, OWLDataFactory odf, String variableName, String searchFieldName,
			// HashMap<String, HashMap<String, OWLNamedIndividual>>  searchIndexes, String lookupFileLocation, String sparqlQueryTemplate)
			RdfConversionLookupMappingToIndividualInstruction rclmtii = new RdfConversionLookupMappingToIndividualInstruction(
				iriMap, odf, variableName, lookupValueFieldName, searchIndexes, mappingRdfFileName, sparqlQueryTemplate);
			return rclmtii; 
		} else {
			throw new ParseException("don't understand instruction type of " + instructionType, 6);
		}
	}

}