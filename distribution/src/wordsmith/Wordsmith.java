/*
  you can put a one sentence description of your library here.
  
  (c) copyright
  
  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 2.1 of the License, or (at your option) any later version.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General
  Public License along with this library; if not, write to the
  Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  Boston, MA  02111-1307  USA
 */

package wordsmith;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import cc.mallet.pipe.*;
import cc.mallet.topics.*;
import cc.mallet.types.InstanceList;
import cc.mallet.util.CharSequenceLexer;

import processing.core.PApplet;

/**
 * Wordsmith is a lib for doing topic modeling and text analysis based off of mallet
 * 
 * @example Wordsmith 
 * @author doug fritz
 * @author aaron zinman
 * 
 */
public class Wordsmith {

	PApplet myParent;
	ParallelTopicModel lda = null;

	public final String VERSION = "0.1.0";

	/**
	 * a Constructor, usually called in the setup() method in your sketch to
	 * initialize and start the library.
	 * 
	 * @param theParent
	 */
	public Wordsmith(PApplet theParent) {
		myParent = theParent;
		createLDA(10);
	}
	
	public void generateTopics() {
		try {
			lda.estimate();
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("LDA estimation failed, maybe you forgot to call createLDA?");
		}
	}
	
	public Object[][] getTopWords(int n) {
		return lda.getTopWords(n);
	}	
	
	public void createLDA(int numTopics) {
		lda = new ParallelTopicModel(numTopics);
	}
	
	public void createLDA(int numTopics, double alpha, double beta) {
		lda = new ParallelTopicModel(numTopics, alpha, beta);
	}
	
	public void setTopicDisplay(int showTopicsInterval, int topWords) {
		lda.setTopicDisplay(showTopicsInterval, topWords);
	}
	
	Pipe instancePipe;
	InstanceList instanceList;
	
	public void setupParsingPipeline() {
		setupParsingPipeline(false);
	}
	
	public void setupParsingPipeline(boolean useLabels) {
		ArrayList<Pipe> pipeList = new ArrayList<Pipe>();

		// Convert the "target" object into a numeric index
		//  into a LabelAlphabet.
		if (useLabels) {
			// If the label field is not used, adding this
			//  pipe will cause "Alphabets don't match" exceptions.
			pipeList.add(new Target2Label());
		}
		
		//
		// Tokenize the input: first compile the tokenization pattern
		// 

		Pattern tokenPattern = CharSequenceLexer.LEX_NONWHITESPACE_CLASSES;

		// Add the tokenizer
		pipeList.add(new CharSequence2TokenSequence(tokenPattern));

		// 
		// Normalize the input as necessary
		// 
		
		pipeList.add(new TokenSequenceLowercase());
		
		// remove non-words
		pipeList.add(new TokenSequenceRemoveNonAlpha(true));

		pipeList.add(new TokenSequenceRemoveStopwords(false, true));

		pipeList.add(new TokenSequence2FeatureSequenceWithBigrams());

		instancePipe = new SerialPipes(pipeList);
	}
	

	public void addFile(String f) {
		if(instancePipe == null) {
			setupParsingPipeline();
		}
	}
	
	public void addDocuments(String f) {
		InstanceList instances = InstanceList.load (new File(f));
		lda.addInstances(instances);
	}
	
	public void addDocument(BufferedReader br) {
		
	}
	
	public void addDocument(File f) {
		
	}
	
	public void addDocument(String str) {
		
	}
	
	public void extractTopics() {
		
	}

	/**
	 * return the version of the library.
	 * 
	 * @return String
	 */
	public String version() {
		return VERSION;
	}
	
	

}
