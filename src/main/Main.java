package main;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

import com.jgaap.canonicizers.*;
import com.jgaap.eventCullers.*;
import com.jgaap.eventDrivers.*;
import com.jgaap.generics.*;

import eventDrivers.*;
import generics.*;

public class Main {
	
	public static String sep = File.separator;

	/* ================
	 * main for testing
	 * ================
	 */
	public static void main(String[] args) throws Exception {
		// ================
		// create documents
		// ================

		// exclude folders / files
		final List<String> exclude = new ArrayList<String>();
		exclude.add(".*\\.svn.*");
		exclude.add(".*stylo");
		exclude.add(".*jgaap");

		FileFilter corpusFilter = new FileFilter() {
			public boolean accept(File pathname) {
				for (Iterator<String> it = exclude.iterator(); it.hasNext();) {
					if (Pattern.matches(it.next(),pathname.toString())) return false;
				}
				return true;
			}
		};

		// create documents
		String corpusName = "drexel_1";
		String corpusPath = "."+sep+"corpora"+sep+corpusName;

		File corpus = new File(corpusPath);
		File[] authors = corpus.listFiles(corpusFilter);

		List<Document> knownDocs = new ArrayList<Document>();
		List<Document> unknownDocs = new ArrayList<Document>();

		// iterate over all authors, take last document in author folder as unknown
		for (File author: authors) {
			File[] docs = author.listFiles(corpusFilter);
			assert(docs.length > 0);
			int j = docs.length-1;
			for (int i = 0; i < docs.length; i++) {
				Document doc = new Document(docs[i].getPath(),author.getName(),docs[i].getName());
				if (i == j) unknownDocs.add(doc);
				else knownDocs.add(doc);
			}
		}
		
		// ==============================
		// create cumulative event driver
		// ==============================
		
		CumulativeEventDriver ced = new CumulativeEventDriver();
		
		// add event drivers with canonicizers and event cullers
		
		// #1 - character bigrams, ascii normalization and unified case, 50 most common events 
		EventDriver ed1 = new CharacterNGramEventDriver();
		int i = ced.addEventDriver(ed1);
		
		Canonicizer c1 = new NormalizeASCII();
		ced.addCanonicizer(c1, i);
		
		Canonicizer c2 = new UnifyCase();
		ced.addCanonicizer(c2, i);
		
		EventCuller ec1 = new MostCommonEvents();
		ec1.setParameter("numEvents", 50);
		ced.addEventCuller(ec1, i);
		
		// #2 - word bigrams, ascii normalization, 250 most common events
		EventDriver ed2 = new WordNGramEventDriver();
		i = ced.addEventDriver(ed2);
		
		ced.addCanonicizer(c1, i);
		
		EventCuller ec2 = new MostCommonEvents();
		ec2.setParameter("numEvents", 250);
		ced.addEventCuller(ec2, i);
		
		// #3 - word lengths, ascii normalization, 10 least common events
		EventDriver ed3 = new WordLengthEventDriver();
		i = ced.addEventDriver(ed3);
		
		ced.addCanonicizer(c1, i);
		
		EventCuller ec3 = new LeastCommonEvents();
		ec3.setParameter("numEvents", 10);
		ced.addEventCuller(ec3, i);
		
		// #4 - POS trigrams, 150 most common events
		EventDriver ed4 = new POSNGramEventDriver();
		ed4.setParameter("N", 3);
		i = ced.addEventDriver(ed4);
		
		EventCuller ec4 = new MostCommonEvents();
		ec4.setParameter("numEvents", 150);
		ced.addEventCuller(ec4, i);
		
		// #5 - experimental (change as will)
		EventDriver ed5 = new LetterNGramEventDriver();
		i = ced.addEventDriver(ed5);
		
		EventCuller ec5 = new MostCommonEvents();
		ec5.setParameter("numEvents", 50);
		ced.addEventCuller(ec5, i);
		
		// =============
		// weka analyzer
		// =============
		
		WekaAnalysisDriver wad = new WekaAnalysisDriver(new weka.classifiers.bayes.NaiveBayesMultinomial(),false);
		//WekaAnalysisDriver wad = new WekaAnalysisDriver(new weka.classifiers.functions.LibSVM(),false);
		//WekaAnalysisDriver wad = new WekaAnalysisDriver(new weka.classifiers.functions.MultilayerPerceptron(),false);
		
		// =======
		// execute
		// =======
		
		Executer e = new Executer(ced, knownDocs, unknownDocs, wad);
		e.execute(true);
		e.writeSetToARFF("."+sep+"arff"+sep+corpusName+".arff", wad.getAllInstances());
	}

}
