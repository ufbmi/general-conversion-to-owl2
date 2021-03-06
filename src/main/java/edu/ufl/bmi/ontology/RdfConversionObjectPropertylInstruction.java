package edu.ufl.bmi.ontology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import edu.ufl.bmi.misc.DataObject;
import edu.ufl.bmi.misc.IriLookup;

public class RdfConversionObjectPropertylInstruction extends RdfConversionInstruction {

	String sourceVariableName;
	String targetVariableName;
	IRI objectPropertyIri;
		
	public RdfConversionObjectPropertylInstruction(IriLookup iriMap, OWLDataFactory odf, String sourceVariableName, 
		String objectPropertyIriTxt, String targetVariableName) {
		super(iriMap, odf);
		this.sourceVariableName = sourceVariableName;
		this.objectPropertyIri = iriMap.lookupObjPropIri(objectPropertyIriTxt);
		this.targetVariableName = targetVariableName;
	}

/*
	@Override
	public void execute(OWLNamedIndividual rowIndividual, ArrayList<String> recordFields, HashMap<String, OWLNamedIndividual> variables, OWLOntology oo) {
		OWLNamedIndividual sourceInd = (sourceVariableName.equals("[row-individual]")) ? rowIndividual : variables.get(sourceVariableName);
		OWLNamedIndividual targetInd = (targetVariableName.equals("[row-individual]")) ? rowIndividual : variables.get(targetVariableName);
		if (sourceInd != null && targetInd != null && objectPropertyIri != null) {
			GenericOwl2Converter.createOWLObjectPropertyAssertion(sourceInd, objectPropertyIri, targetInd, oo);
		} else {
			System.err.println("null individual or OP for OP assertion for " + sourceVariableName + " & " + targetVariableName + "\t" + sourceInd + ", " + targetInd);
			//System.out.println("OP IRI = " + objectPropertyIri);
		}
	}
*/
	@Override
	public void execute(OWLNamedIndividual rowIndividual, DataObject dataObject, DataObject parentObject, HashMap<String, OWLNamedIndividual> variables, OWLOntology oo) {
		OWLNamedIndividual sourceInd = (sourceVariableName.equals("[row-individual]")) ? rowIndividual : variables.get(sourceVariableName);
		OWLNamedIndividual targetInd = (targetVariableName.equals("[row-individual]")) ? rowIndividual : variables.get(targetVariableName);
		if (sourceInd != null && targetInd != null && objectPropertyIri != null) {
			GenericOwl2Converter.createOWLObjectPropertyAssertion(sourceInd, objectPropertyIri, targetInd, oo);
		} else {
			System.err.println("null individual or OP for OP assertion for " + sourceVariableName + " & " + targetVariableName + "\t" + sourceInd + ", " + objectPropertyIri + ", " + targetInd);
			//System.out.println("OP IRI = " + objectPropertyIri);
		}
	}
}
