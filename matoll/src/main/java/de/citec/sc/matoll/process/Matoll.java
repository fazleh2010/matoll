package de.citec.sc.matoll.process;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

//import learning.SVMClassifier;


//import core.
import de.citec.sc.bimmel.learning.*;
import de.citec.sc.bimmel.core.Dataset;
import de.citec.sc.bimmel.core.FeatureVector;
import de.citec.sc.bimmel.core.Instance;
import de.citec.sc.bimmel.core.Label;



import de.citec.sc.matoll.classifiers.FreqClassifier;
import de.citec.sc.matoll.core.LexicalEntry;
import de.citec.sc.matoll.core.Lexicon;
import de.citec.sc.matoll.core.LexiconWithFeatures;
import de.citec.sc.matoll.core.Provenance;
import de.citec.sc.matoll.core.Reference;
import de.citec.sc.matoll.evaluation.LexiconEvaluation;
import de.citec.sc.matoll.io.Config;
import de.citec.sc.matoll.io.LexiconLoader;
import de.citec.sc.matoll.io.LexiconSerialization;
import de.citec.sc.matoll.patterns.PatternLibrary;
import de.citec.sc.matoll.preprocessor.ModelPreprocessor;
import de.citec.sc.matoll.utils.StanfordLemmatizer;

import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.xml.sax.SAXException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import de.citec.sc.matoll.core.Language;

public class Matoll {
 
        
	private static Logger logger = LogManager.getLogger(Matoll.class.getName());
        
        
        
	/**
         * 
         * @param args
         * @throws IOException
         * @throws ParserConfigurationException
         * @throws SAXException
         * @throws InstantiationException
         * @throws IllegalAccessException
         * @throws ClassNotFoundException 
         */
	public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException, InstantiationException, IllegalAccessException, ClassNotFoundException, Exception {
			
		//Logger logger = LogManager.getLogger(Matoll.class.getName());

                

		String directory;
		String mode = "train";
		String gold_standard_lexicon;
		String model_file = "model";
		String output_lexicon;
		String configFile;
		Language language;
		String classi;
		Config config = null;
		boolean coreference = false;
		int no_entries = 1000;
		String output;
		double frequency = 2.0;
		
		HashMap<String,Double> maxima; 
		maxima = new HashMap<String,Double>();
		
		
		//Provenance provenance;
		
		Pattern p = Pattern.compile("^--(\\w+)=(\\w+|\\d+)$");
		  
		Matcher matcher;
		 
		if (args.length < 3)
		{
			System.out.print("Usage: Matoll --mode=train/test <DIRECTORY> <CONFIG>\n");
			return;
		
		}
		
		Classifier classifier;
		
		directory = args[1];
		configFile = args[2];
		
		config = new Config();
		
		config.loadFromFile(configFile);
		
		classi = config.getClassifier();
	
		gold_standard_lexicon = config.getGoldStandardLexicon();
		
		model_file = config.getModel();
		
		output_lexicon = config.getOutputLexicon();
		output = config.getOutput();
		coreference = config.getCoreference();
		
		language = config.getLanguage();
				
		for (int i=0; i < args.length; i++)
		{
			matcher = p.matcher(args[i]);
				 
			if (matcher.matches())
			{
			    if (i== 0 && matcher.group(1).equals("mode"))
			    {
			    	mode = matcher.group(2);
			    	System.out.print("Starting MATOLL with mode: "+mode+"\n");
			    	System.out.print("Language: "+language+"\n");
			    	System.out.print("Processing directory: "+directory+"\n");
			    	System.out.print("Using gold standard: "+gold_standard_lexicon+"\n");
			    	System.out.print("Using model file: "+model_file+"\n");
			    	System.out.print("Output lexicon: "+output_lexicon+"\n");
			    	System.out.print("Output: "+output+"\n");
			    	System.out.print("Using coreference: "+coreference+"\n");
			    	
			    }
			    else
			    {
			    	System.out.print("Usage: Matoll --mode=train/test <DIRECTORY> <CONFIG>\n");
			    	return;
			    }
			}		
		}
		
		if (classi.equals("de.citec.sc.matoll.classifiers.FreqClassifier"))
		{
			classifier = new FreqClassifier("freq", frequency);
			System.out.print("Instantiating" + classifier.getClass() + "\n");
		}
		else
		{
			classifier = (Classifier) Class.forName(classi).newInstance();
			System.out.print("Instantiating "+classifier.getClass()+ "\n");
		}
		
//		LexiconLoader loader = new LexiconLoader();
//		
//		logger.info("Loading lexicon from: "+gold_standard_lexicon+"\n");
//		
//		Lexicon gold = loader.loadFromFile(gold_standard_lexicon);
                Lexicon gold = new Lexicon();
		
		File folder = new File(directory);
		
		Lexicon lexicon;
                
		
		// Creating preprocessor
		
		ModelPreprocessor preprocessor = new ModelPreprocessor(language);
                preprocessor.setCoreferenceResolution(coreference);
				
		LexiconWithFeatures lexiconwithFeatures = new LexiconWithFeatures();
                //lexiconwithFeatures.setBaseURI("http://localhost:8080/");
		

		PatternLibrary library = new PatternLibrary();
		StanfordLemmatizer sl = new StanfordLemmatizer(language);
		library.setLemmatizer(sl);
		
		library.setPatterns(config.getPatterns());
		
	
		String subj;
		String obj;
		
		String reference = null;
		
		List<Model> sentences;
		File[] files = folder.listFiles();

                int file_counter = 0;
		for (final File file : files) {
			
			if (file.isFile() && file.toString().endsWith(".ttl")) {
                            
                                file_counter+=1;

				logger.info("Processing: "+file.toString()+"  "+file_counter+"/"+files.length);
                                
								
				Model model = RDFDataMgr.loadModel(file.toString());
			 
				sentences = getSentences(model);
			 
				for (Model sentence: sentences)
				{
					//System.out.println(sentence.toString());
					obj = getObject(sentence);
			 
					subj = getSubject(sentence);
			 
					reference = getReference(sentence);
			
					preprocessor.preprocess(sentence,subj,obj);
			
					//logger.info("Extract lexical entry for: "+sentence.toString()+"\n");	
					library.extractLexicalEntries(sentence, lexiconwithFeatures);
				}
                                model.close();
				// FileOutputStream output = new FileOutputStream(new File(file.toString().replaceAll(".ttl", "_pci.ttl")));
			
				// RDFDataMgr.write(output, model, RDFFormat.TURTLE) ;
			
			}
		}
		
		logger.info("Extracted all entries \n");
		logger.info("Lexicon contains "+Integer.toString(lexiconwithFeatures.getEntries().size())+" entries\n");
		
		LexiconSerialization serializer = new LexiconSerialization(language);
		
		List<LexicalEntry> entries = new ArrayList<LexicalEntry>();
                
                Model model = ModelFactory.createDefaultModel();
		
		serializer.serialize(lexiconwithFeatures, model);
		
		FileOutputStream out = new FileOutputStream(new File(output_lexicon.replace(".lex","_beforeTraining.ttl")));
		
		RDFDataMgr.write(out, model, RDFFormat.TURTLE) ;
                
                out.close();
		
		FeatureVector vector;
		logger.info("Starting "+mode.toString()+"\n");	
		boolean predict = true;
		if (mode.equals("train"))
		{
			Dataset trainingSet = new Dataset();
			
			// process features
			
			int numPos = 0;
			
			int numNeg = 0;
			
			// get maxima
			for (LexicalEntry entry: lexiconwithFeatures.getEntries())
			{
				vector = lexiconwithFeatures.getFeatureVector(entry);
				
				for (String feature: vector.getFeatures())
				{
					updateMaximum(feature,vector.getFeatureMap().get(feature),maxima);
				}
				
			}
			
			for (LexicalEntry entry: lexiconwithFeatures.getEntries())
			{
                                
				//entry.setMappings(entry.computeMappings(entry.getSense()));
				
				// System.out.println("Checking entry with label: "+entry.getCanonicalForm()+"\n");
				
				// System.out.println(entry);
				
				vector = lexiconwithFeatures.getFeatureVector(entry);
				
				// preprocessing vector
				
				List<LexicalEntry> list = gold.getEntriesWithCanonicalForm(entry.getCanonicalForm());
				
                                /*
                                TODO:
                                the contains function does not work here.
                                why?
                                Because the euqals methode is set in the lexical entry, but there it is also check on the URI level, if the entry is equals or not.
                                This works perfect for creating and checking entries created by M-ATOLL, but not for lexica, like gold, with a different namespace etc.
                                */
				if (gold.contains(entry))
				{
					//System.out.print("Lexicon contains "+entry+"\n");
					
					trainingSet.addInstance(new Instance(normalize(vector,maxima), new Label(1)));
					logger.debug("Adding training example: "+entry.getCanonicalForm()+" with label "+1);
					numPos++;
				
				}
			
				else
				{
					// System.out.print("Lexicon does not contain "+entry+"\n");
					
					if (numNeg < numPos)
					{
						trainingSet.addInstance(new Instance(normalize(vector,maxima), new Label(0)));
						// logger.info("Adding training example: "+entry.toString()+"\n");
						logger.debug("Adding training example: "+entry.getCanonicalForm()+" with label "+0);
						numNeg++;
					}
				}
				
			}
			
			if (trainingSet.size() > 0)
			
			classifier.train(trainingSet);
			
			else
			{
				logger.info("Can not train classifier as there are no training instances\n");
				writeByReference(lexiconwithFeatures);
				logger.info("Exit MATOLL\n");
				return;
				
			}
		
		}
		
		writeByReference(lexiconwithFeatures);
		for (LexicalEntry entry: lexiconwithFeatures.getEntries())
		{
			// System.out.println(entry);
			vector = lexiconwithFeatures.getFeatureVector(entry);

			logger.info("Prediction: for "+ entry.getCanonicalForm() + " is " +classifier.predict(vector)+"\n");

                        System.err.println("Adapt to new provenance style");
//			if (classifier.predict(vector)==1)
//			{
//				Provenance provenance = new Provenance();
//                                
//
//				provenance.setConfidence(classifier.predict(vector, 1));
//
//				provenance.setAgent("http://sc.citec.uni-bielefeld.de/matoll");
//
//				provenance.setEndedAtTime(new Date());
//
//				entry.setProvenance(provenance);
//
//				entries.add(entry);
//			}
//			else
//			{
//
//			}
			
		}
		

//		  Collections.sort(entries, new Comparator<LexicalEntry>() {
//	
//			 
//			            public int compare(LexicalEntry one, LexicalEntry two) {
//                                        System.err.println("Adapt to new provenance style");
//			            		return (((LexicalEntry) one).getProvenance().getConfidence() >= ((LexicalEntry) two).getProvenance().getConfidence()) ? -1 : 1;               
//				            }
//				             
//				        });
		  
		 /*
		  * TODO
		  Exception in thread "main" java.lang.IllegalArgumentException: Comparison method violates its general contract!
	at java.util.TimSort.mergeHi(TimSort.java:895)
	at java.util.TimSort.mergeAt(TimSort.java:512)
	at java.util.TimSort.mergeForceCollapse(TimSort.java:453)
	at java.util.TimSort.sort(TimSort.java:250)
	at java.util.Arrays.sort(Arrays.java:1435)
	at java.util.Collections.sort(Collections.java:230)
	at de.citec.sc.matoll.process.Matoll.main(Matoll.java:370)
		  */
		
		
		lexicon = new Lexicon();
		
		LexiconEvaluation eval = new LexiconEvaluation();
		
		FileWriter writer = new FileWriter(output);
		
		for (int i=0; i < entries.size(); i++)
		{
			lexicon.addEntry(entries.get(i));
			
			eval.setReferences(lexicon.getReferences());
			
			eval.evaluate(lexicon,gold);
			
			System.out.print("Considering entry "+entries.get(i)+"("+i+")\n");
			
			writer.write(i+"\t"+eval.getPrecision("lemma")+"\t"+eval.getRecall("lemma")+"\t"+eval.getFMeasure("lemma")+"\t"+eval.getPrecision("syntactic")+"\t"+eval.getRecall("syntactic")+"\t"+eval.getFMeasure("syntactic")+"\t"+eval.getPrecision("mapping")+"\t"+eval.getRecall("mapping")+"\t"+eval.getFMeasure("mapping")+"\n");
			
			System.out.println(i+"\t"+eval.getPrecision("lemma")+"\t"+eval.getRecall("lemma")+"\t"+eval.getFMeasure("lemma")+"\t"+eval.getPrecision("syntactic")+"\t"+eval.getRecall("syntactic")+"\t"+eval.getFMeasure("syntactic")+"\t"+eval.getPrecision("mapping")+"\t"+eval.getRecall("mapping")+"\t"+eval.getFMeasure("mapping"));
			
			writer.flush();
			
		}
		
		writer.close();
		
		
	
		logger.info("Write lexicon to "+output_lexicon+"\n");
		model = ModelFactory.createDefaultModel();
		
		serializer.serialize(lexiconwithFeatures, model);
		
		out = new FileOutputStream(new File(output_lexicon));
		
		RDFDataMgr.write(out, model, RDFFormat.TURTLE) ;
                
                out.close();
		
		// System.out.print("Lexicon: "+output.toString()+" written out\n");
		
		
			
	}
        /**
         * 
         * @param lexicon
         * @throws IOException 
         */
	private static void writeByReference(Lexicon lexicon) throws IOException {
		List<LexicalEntry> entries;
		FileWriter writer;
		Set<Reference> references = lexicon.getReferences();
		
		
		for (Reference ref: references)
		{
			String filename = ref.toString().replaceAll("http:\\/\\/","").replaceAll("\\/","_").replaceAll("\\.","_")+".lex";
			System.out.println("Write lexicon for reference "+ref.toString()+" to "+filename);
			writer = new FileWriter(filename);
			entries = lexicon.getEntriesForReference(ref.toString());
			
			for (LexicalEntry entry: entries)
			{
				writer.write(entry.toString()+"\n");
				writer.flush();
			}
			
			writer.close();
			
			
			
		}
	}
        /**
         * 
         * @param vector
         * @param max
         * @return 
         */
	private static FeatureVector normalize(FeatureVector vector, HashMap<String,Double> max) {
		
		HashMap<String,Double> map;
		
		map = vector.getFeatureMap();
		
		for (String feature: map.keySet())
		{
			map.put(feature, new Double(map.get(feature).doubleValue() / max.get(feature).doubleValue()));
		}
		
		return vector;
		
	}
        /**
         * 
         * @param feature
         * @param value
         * @param map 
         */
	private static void updateMaximum(String feature, Double value, HashMap<String,Double> map) {
		
		
		if (map.containsKey(feature))
		{
		
			if (value > map.get(feature))
			{
				map.put(feature, value);
			}
		}
		else
		{
			map.put(feature, value);
		}
		
	}
        /**
         * 
         * @param model
         * @return 
         */
	private static String getReference(Model model) {
		StmtIterator iter = model.listStatements(null,model.getProperty("own:reference"), (RDFNode) null);
		
		Statement stmt;
		
		while (iter.hasNext()) {
						
			stmt = iter.next();
			
	        return stmt.getObject().toString();
	    }
		
		return null;
	}
        
        /**
         * 
         * @param model
         * @return
         * @throws FileNotFoundException 
         */
	private static List<Model> getSentences(Model model) throws FileNotFoundException {
		
		// get all ?res <conll:sentence> 
		
		List<Model> sentences = new ArrayList<Model>();
		
		StmtIterator iter, iter2, iter3;
		
		Statement stmt, stmt2, stmt3;
		
		Resource resource;
		
		Resource token;
		
		iter = model.listStatements(null,model.getProperty("conll:language"), (RDFNode) null);
		
		while (iter.hasNext()) {
					
			Model sentence = ModelFactory.createDefaultModel();
			
			stmt = iter.next();
			
			resource = stmt.getSubject();
			
			iter2 = model.listStatements(resource , null, (RDFNode) null);
			
			while (iter2.hasNext())
			{
				stmt2 = iter2.next();
				
				sentence.add(stmt2);
				
			}
			
			iter2 = model.listStatements(null , model.getProperty("own:partOf"), (RDFNode) resource);
			
			while (iter2.hasNext())
			{
				stmt2 = iter2.next();
				
				token = stmt2.getSubject();
				
				iter3 = model.listStatements(token , null, (RDFNode) null);
				
				while (iter3.hasNext())
				{
					stmt3 = iter3.next();
					
					sentence.add(stmt3);
					
				}
			}
			
			sentences.add(sentence);
			
			// RDFDataMgr.write(new FileOutputStream(new File(resource+".ttl")), sentence, RDFFormat.TURTLE) ;
			
		}
		

		return sentences;
		
	}

        /**
         * 
         * @param model
         * @return 
         */
	private static String getSubject(Model model) {
		
		StmtIterator iter = model.listStatements(null,model.getProperty("own:subj"), (RDFNode) null);
		
		Statement stmt;
		
		while (iter.hasNext()) {
						
			stmt = iter.next();
			
	        return stmt.getObject().toString();
	    }
		
		return null;
	}
        
        /**
         * 
         * @param model
         * @return 
         */
	private static String getObject(Model model) {
		StmtIterator iter = model.listStatements(null,model.getProperty("own:obj"), (RDFNode) null);
		
		Statement stmt;
		
		while (iter.hasNext()) {
						
			stmt = iter.next();
	        
	        return stmt.getObject().toString();
	    }
		
		return null;
	}
}
