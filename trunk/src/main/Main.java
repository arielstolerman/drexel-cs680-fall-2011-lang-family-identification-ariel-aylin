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
		
		// =============
		// weka analyzer
		// =============
		
		WekaAnalysisDriver wad = new WekaAnalysisDriver(new weka.classifiers.functions.LibSVM(),false);
		
		// =======
		// execute
		// =======
		
		Executer e = new Executer(ced, knownDocs, unknownDocs, wad);
		e.execute(true);
		e.writeSetToARFF("."+sep+"arff"+sep+corpusName+".arff", wad.getAllInstances());
	}

}
