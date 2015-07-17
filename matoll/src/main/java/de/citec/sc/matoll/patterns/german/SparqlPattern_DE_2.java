package de.citec.sc.matoll.patterns.german;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.hp.hpl.jena.rdf.model.Model;

import de.citec.sc.bimmel.core.FeatureVector;
import de.citec.sc.matoll.core.Language;
import de.citec.sc.matoll.core.LexiconWithFeatures;
import de.citec.sc.matoll.patterns.SparqlPattern;
import de.citec.sc.matoll.patterns.Templates;

public class SparqlPattern_DE_2 extends SparqlPattern{

	
	Logger logger = LogManager.getLogger(SparqlPattern_DE_2.class.getName());
	
	/*
	 * PropSubj:Islamisch Republikanische Partei
PropObj:1979 Februar 
sentence:Am 19. Februar 1979 gründeten die Gefolgsleute Chomeinis die Islamisch Republikanische Partei . 
1	Am	am	PREP	APPRART	Dat	5	pp	_	_ 
2	19.	19.	ADJA	ADJA	_|Masc|Dat|Sg|_|	3	attr	_	_ 
3	Februar	Februar	N	NN	Masc|Dat|Sg	1	pn	_	_ 
4	1979	1979	CARD	CARD	_	3	app	_	_ 
5	gründeten	gründen	V	VVFIN	3|Pl|Past|_	0	root	_	_ 
6	die	die	ART	ART	Def|Masc|Nom|Pl	7	det	_	_ 
7	Gefolgsleute	Gefolgsmann	N	NN	Masc|Nom|Pl	5	subj	_	_ 
8	Chomeinis	Chomeinis	N	NN	Masc|Nom|Pl	7	app	_	_ 
9	die	die	ART	ART	Def|_|_|_	10	det	_	_ 
10	Islamisch	islamisch	N	NN	Fem|Akk|Sg	5	obja	_	_ 
11	Republikanische	republikanisch	ADJA	ADJA	Pos|Fem|Akk|Sg|_|	12	attr	_	_ 
12	Partei	Partei	N	NN	Fem|Akk|Sg	10	app	_
	 */
	
	
	String query = "SELECT ?lemma ?prep ?dobj_form ?e1_arg ?e2_arg  WHERE {"
			+ "?y <conll:cpostag> \"V\" ."
			//VVFIN
			+ "?y <conll:lemma> ?lemma . "
			+ "?e1 <conll:head> ?p . "
			+ "?e1 <conll:deprel> ?deprel. "
			+ "FILTER regex(?deprel, \"pn\") ."
			+ "?p <conll:head> ?y . "
			+ "?p <conll:deprel> \"pp\" . "
			+" ?p <conll:postag> \"APPRART\". "
			+ "?p <conll:form> ?prep . "
			+ "?e2 <conll:head> ?y . "
			+ "?e2 <conll:deprel> \"obja\" . "
			+ "?e1 <own:senseArg> ?e1_arg. "
			+ "?e2 <own:senseArg> ?e2_arg. "
			+ "}";
	
	
	@Override
	public String getID() {
		return "SPARQLPattern_DE_2";
	}

	@Override
	public void extractLexicalEntries(Model model, LexiconWithFeatures lexicon) {
	    FeatureVector vector = new FeatureVector();
		
		vector.add("freq",1.0);
		vector.add(this.getID(),1.0);
		
		List<String> sentences = this.getSentences(model);
		
		QueryExecution qExec = QueryExecutionFactory.create(query, model) ;
                ResultSet rs = qExec.execSelect() ;
                String verb = null;
                String e1_arg = null;
                String e2_arg = null;
                String preposition = null;

                try {
                 while ( rs.hasNext() ) {
                         QuerySolution qs = rs.next();

                         // System.out.print("Query 3 matched\n!!!");

                         try{
                                 verb = qs.get("?lemma").toString();
                                 e1_arg = qs.get("?e1_arg").toString();
                                 e2_arg = qs.get("?e2_arg").toString();	
                                 preposition = qs.get("?prep").toString();	
                          }
	        	 catch(Exception e){
	     	    	e.printStackTrace();
	        		 //ignore those without Frequency TODO:Check Source of Error
                        }
                     }
                }
                catch(Exception e){
                    e.printStackTrace();
                }
                qExec.close() ;
    
		if(verb!=null && e1_arg!=null && e2_arg!=null && preposition!=null) {
                    Templates.getIntransitiveVerb(model, lexicon, vector, sentences, verb, e1_arg, e2_arg, preposition, this.getReference(model), logger, this.getLemmatizer(),Language.DE,getID());
            } 
		
	}

}
