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
		String corpusName = "drexel_1_short";
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
		
		// =======================================================
		// Create cumulative event driver for Koppel's feature set
		// =======================================================
		
		CumulativeEventDriver ced = new CumulativeEventDriver();
		
		Canonicizer normASCII = new NormalizeASCII();
		Canonicizer unifyCase = new UnifyCase();
		
		int i;
		
		// add event drivers with canonicizers and event cullers
		
		// 400 standard function words
		EventDriver koppelFuncWords = new WhiteListEventDriver();
		koppelFuncWords.setParameter("filename", "."+sep+"resources"+sep+"koppel_function_words.txt");
		i = ced.addEventDriver(koppelFuncWords);
		
		ced.addCanonicizer(normASCII, i);
		ced.addCanonicizer(unifyCase, i);
				
		EventCuller funcWordsCuller = new MostCommonEvents();
		funcWordsCuller.setParameter("numEvents", 400);
		//ced.addEventCuller(funcWordsCuller, i);
		
		// 200 letter trigrams
		EventDriver letterTrigrams = new LetterNGramEventDriver();
		letterTrigrams.setParameter("N", 3);
		i = ced.addEventDriver(letterTrigrams);
		
		ced.addCanonicizer(normASCII, i);
		ced.addCanonicizer(unifyCase, i);
		
		EventCuller letterTrigramsCuller = new MostCommonEvents();
		letterTrigramsCuller.setParameter("numEvents", 200);
		ced.addEventCuller(letterTrigramsCuller, i);
		
		// =============
		// weka analyzer
		// =============
		
		//WekaAnalysisDriver wad = new WekaAnalysisDriver(new weka.classifiers.functions.LibSVM(),false);
		//WekaAnalysisDriver wad = new WekaAnalysisDriver(new weka.classifiers.bayes.NaiveBayesMultinomial(),false);
		WekaAnalysisDriver wad = new WekaAnalysisDriver(new weka.classifiers.functions.MultilayerPerceptron(),false);
		
		// =======
		// execute
		// =======
		
		Executer e = new Executer(ced, knownDocs, unknownDocs, wad);
		e.execute(true);
		e.writeSetToARFF("."+sep+"arff"+sep+corpusName+".arff", wad.getAllInstances());
	}

}
