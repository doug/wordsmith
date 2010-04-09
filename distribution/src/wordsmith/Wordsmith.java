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


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;

import processing.core.PApplet;
import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.CharSequenceRemoveHTML;
import cc.mallet.pipe.Input2CharSequence;
import cc.mallet.pipe.Noop;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SaveDataInSource;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.Target2Label;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.TokenSequenceLowercase;
import cc.mallet.pipe.TokenSequenceRemoveStopwords;
import cc.mallet.pipe.iterator.FileIterator;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.topics.TopicAssignment;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.IDSorter;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelSequence;

/**
 * Wordsmith is a lib for doing topic modeling and text analysis based off of mallet
 * 
 * @example Wordsmith 
 * @author doug fritz
 * @author aaron zinman
 * 
 */
public class Wordsmith {

	private PApplet myParent;
	private ParallelTopicModel lda = null;
	private String outputStateFile = null, outputModelFile = null;
	private int k = -1;
	private double alpha = -1, beta = -1;
	private boolean createModel = true;
	private int showTopicsInterval = 10, showNTopWords = 7;

	public final String VERSION = "0.1.0";

	private boolean setInstances = false;
  private HashSet<String> stopwordsList = new HashSet<String>();
  private boolean addedEnglishStopwords = false;
  private boolean filterHtml = false;
  private int numIterations = -1;
  private int numThreads = 1; // 1 thread by default, for 1 core
  private Object[][] topWordsCache = null;
  private TreeSet<IDSorter>[] topicSortedWordsCache = null;

  private InstanceList ilist = new InstanceList (makeNewInstancePipe());
	
	/**
	 * a Constructor, usually called in the setup() method in your sketch to
	 * initialize and start the library.
	 * 
	 * @param theParent
	 */
	public Wordsmith(PApplet theParent) {
		myParent = theParent;
	}
	
	// CREATE MODEL ----------------------------------------------------------------------------------
	

	public void loadExistingModel(String file) {
    try {
      lda = ParallelTopicModel.read(new File(file));
    } catch (Exception e) {
      System.err.println("Unable to restore saved topic model " + file + ": " + e);
    }
    this.createModel = false;
	}
	
	
	public void createNewModel(int numberOfTopics, double alpha, double beta, String outputFile) {
    this.createModel = true;
	  this.k = numberOfTopics;
	  this.alpha = alpha;
	  this.beta = beta;
	  this.numIterations = 250;
    lda = new ParallelTopicModel(numberOfTopics, alpha, beta);
	}

	public void createNewModel(int numberOfTopics, String outputFile) {
    createNewModel(numberOfTopics, 50.0 / numberOfTopics, 0.03, outputFile);
  }
	
	// CONFIGURE MODEL -------------------------------------------------------------------------------

  public void setIntermediateResultsFrequency(int numberOfIterations) {
    this.showTopicsInterval = numberOfIterations;
  }
	
  public void setNumberShownWordsForIntermediateResults(int numberOfWords) {
    this.showNTopWords = numberOfWords;
  }
  
  public void setNumberOfProcessIterations(int numIterations) {
    if (numIterations < 10) {
      System.err.println("You must have at least 10 iterations, and that is probably too small. " +
      		"Think on the order of 100-400, where 250/300 is usually optimal.");
      return;
    } else if (numIterations < 100) {
      System.out.println("WARNING: Few iterations. Still processing, but for best results, " +
      		"think on the order of 100-400, where 250/300 is usually optimal.");
    }
    this.numIterations  = numIterations;
  }
  
  public void saveFinishedModelToFile(String filepath) {
    outputModelFile = filepath;
  }
  
  public void saveFinishedStateToFile(String filepath) {
    outputStateFile = filepath;
  }
  
  public void useMulticore(int numCores) {
    if (numCores < 0 || numCores > 8) {
      System.err.println("You must use between 1 & 8 cores. If your machine is not multi-core, " +
      		"anything other than 1 may slow it down a bit.");
    }
    numThreads = numCores;
  }

  // STOPWORDS -------------------------------------------------------------------------------------

  public void removeWordFromDocuments(String word) {
    stopwordsList.add(word);
  }
  
  public void removeCommonEnglishWordsFromDocuments() {
    if (addedEnglishStopwords) {
      System.err.println("Already added common english words... ignoring");
      return;
    }
  }
  
  public void removeHtmlFromDocuments() {
    filterHtml = true;
  }
  
  // USE MODEL -------------------------------------------------------------------------------
	
	public void extractTopicsFromDocuments() {
	  if (lda == null) {
	    System.err.println("You must first call either createNewModel or loadExistingModel. " +
	    		"You probably want to call createNewModel, as runAlgorithm does the inference process " +
	    		"over previously entered documents.");
	    return;
	  }
	  
	  if (!setInstances) {
	    System.err.println("You have not added any documents. You can use:\n" +
	                       "addDocumentInString(\"this is my short document\") or\n" +
	                       "addDocumentInFile(\"path/to/my/favorite/file\") or\n" + 
	                       "addDocumentsInDirectory(\"/path/to/a/directory/of/files\")");
	    return;
	  }
	  
    lda.setTopicDisplay(showTopicsInterval, showNTopWords);
    lda.setNumIterations(numIterations);
    if (numIterations < 1) {
      System.err.println("Sorry, you must do at least one iteration, and most likely 100-400.");
      return;
    } else if (numIterations >= 100) {
      lda.setOptimizeInterval(50);
      lda.setBurninPeriod(50);
    } else if (numIterations >= 50) {
      lda.setOptimizeInterval(30);
      lda.setBurninPeriod(25);
    } else {
      lda.setOptimizeInterval(0);
      lda.setBurninPeriod(0);
    }
    
    if (outputStateFile != null) {
      lda.setSaveState(10, outputStateFile);
    }

    if (outputModelFile != null) {
      lda.setSaveSerializedModel(10, outputModelFile);
    }

    lda.setNumThreads(numThreads);

	  System.out.println("Starting LDA Inference... this might take a while (read: hours) depending " +
	  		"on the number of topics, number of documents, and document length.");
    long startTime = System.currentTimeMillis();
		try {
	    lda.addInstances(ilist);
			lda.estimate();
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("LDA estimation failed: " + e.getMessage());
		}
		System.out.println("Inference finished! It took " + 
		                   HumanTime.exactly(System.currentTimeMillis() - startTime));
		
		if (outputModelFile != null) {
		  System.out.println("Saving model to disk.");
      assert (lda != null);
      try {
        ObjectOutputStream oos = new ObjectOutputStream (new FileOutputStream (outputModelFile));
        oos.writeObject (lda);
        oos.close();

      } catch (Exception e) {
        e.printStackTrace();
        throw new IllegalArgumentException (
             "Couldn't write topic model to filename " + outputModelFile);
      }
		}

	  if (outputStateFile != null) {
	    System.out.println("Saving model to disk.");
	    assert (lda != null);
	    try {
	      lda.printState(new File(outputStateFile));
	    } catch (Exception e) {
	      e.printStackTrace();
	      throw new IllegalArgumentException (
	           "Couldn't write topic model state to filename " + outputStateFile);
	    }
	  }
	}
	
	public String[] getTopWordsForTopic(int n) {
	  if (n > k) {
	    System.err.println("You must enter a topic number from 0 to " + k);
	    return null;
	  }
	  
    if (lda == null) {
      System.err.println("You must first create the model and extract the topics.");
      return null;
    }

    if (topWordsCache == null) {
	    topWordsCache = lda.getTopWords(1000);
	  }
	  
		return (String[]) topWordsCache[n];
	}

	public WeightedWord[] getTopWeightedWordsForTopic(int n) {
    if (n > k) {
	    System.err.println("You must enter a topic number from 0 to " + k);
      return null;
    }
	    
    if (lda == null) {
      System.err.println("You must first create the model and extract the topics.");
      return null;
    }
	  
    if (topicSortedWordsCache == null) {
	     topicSortedWordsCache = lda.getSortedWords();
    }
	    
    TreeSet<IDSorter> sortedWords = topicSortedWordsCache[n];
    Iterator<IDSorter> iterator = sortedWords.iterator();
    int word = 1;

    ArrayList<WeightedWord> words = new ArrayList<WeightedWord>(n);
    while (iterator.hasNext() && word < 1000) {
      IDSorter info = iterator.next();
      WeightedWord wword = new WeightedWord((String)lda.getAlphabet().lookupObject(info.getID()),
                                            info.getWeight());
      words.add(wword);
      word++;
    }
    return words.toArray(new WeightedWord[0]);
  }
	
	public int getNumProcessedDocuments() {
	  return lda.getData().size();
	}
	
	public TopicWordAssignment[] getTopicWordAssignmentsForDocument(int documentIndex) {
	  ArrayList<TopicAssignment> data = lda.getData();
    FeatureSequence tokenSequence = (FeatureSequence) data.get(documentIndex).instance.getData();
    LabelSequence topicSequence = (LabelSequence) data.get(documentIndex).topicSequence;
    
    TopicWordAssignment[] assignments = new TopicWordAssignment[topicSequence.getLength()];
    for (int pi = 0; pi < topicSequence.getLength(); pi++) {
      int type = tokenSequence.getIndexAtPosition(pi);
      int topic = topicSequence.getIndexAtPosition(pi);
      assignments[pi] = new TopicWordAssignment((String) lda.getAlphabet().lookupObject(type),
                                                topic); 
    }
    return assignments;
	}
	
	// ADD DOCUMENTS ---------------------------------------------------------------------------------

	private SerialPipes makeNewInstancePipe() {
	  return new SerialPipes (
	      new Pipe[] {
          new Target2Label(),
          new SaveDataInSource(),
          new Input2CharSequence(),
          (filterHtml
           ? (Pipe) new CharSequenceRemoveHTML()
           : (Pipe) new Noop()),
          new CharSequence2TokenSequence(),
          new TokenSequenceLowercase(),
          //new PrintInputAndTarget (), // xxx
          (addedEnglishStopwords
           ? (Pipe) new TokenSequenceRemoveStopwords(false, false)
                        .addStopWords(stopwordsList.toArray(new String[0]))
           : (Pipe) new Noop()),
          new TokenSequence2FeatureSequence(),
//          new FeatureSequence2AugmentableFeatureVector(false), // keep sequence
          // or FeatureSequence2FeatureVector
          //new PrintInputAndTarget ()
        });
	}
	
	public void addDocumentsInDirectory(String directory) {
	  InstanceList ilist = new InstanceList (makeNewInstancePipe());
	  boolean removeCommonPrefix=true;
	  ilist.addThruPipe(
        new FileIterator(directory, FileIterator.STARTING_DIRECTORIES, removeCommonPrefix));
		setInstances = true;
	}
	
  public void addDocumentInFile(String filepath) {
    addDocumentInFile(new File(filepath));
  }


  public void addDocumentInFile(File f) {
    ilist.addThruPipe(new Instance(f, null, f.toURI(), null));
    setInstances = true;
	}
	
	public void addDocumentInString(String document) {
    ilist.addThruPipe(new Instance(document, null, "added_document", null));
    setInstances = true;
  }

	// MISC ------------------------------------------------------------------------------------------
	
	/**
	 * return the version of the library.
	 * 
	 * @return String
	 */
	public String version() {
		return VERSION;
	}
}
