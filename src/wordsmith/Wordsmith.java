/*
  A wrapper for Mallet's LDA implementation for the Processing community.
  
  Copyright (c) 2010 MIT Media Lab
  Authors: Aaron Zinman <azinman@media.mit.edu>
           Doug Fritz <doug@media.mit.edu>
  
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
  Boston, MA  02111-1307 USA
*/

package wordsmith;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
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
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.TokenSequenceLowercase;
import cc.mallet.pipe.TokenSequenceRemoveStopwords;
import cc.mallet.pipe.iterator.FileIterator;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.topics.TopicAssignment;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSelection;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.IDSorter;
import cc.mallet.types.InfoGain;
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
  public final String VERSION = "0.1.0";

	private ParallelTopicModel lda = null;
	private int k = -1;
	private boolean createModel = true;
	private int showTopicsInterval = 10, showNTopWords = 7;
  private boolean setInstances = false;
  private int numIterations = -1;
  private int numThreads = 1; // 1 thread by default, for 1 core
  
  private String outputStateFile = null, outputModelFile = null;
  private String outputIntermediateStateFile = null;
  private int outputIntermediateStateFrequency = 10;
  private String outputIntermediateModelFile = null;
  private int outputIntermediateModelFrequency = 10;

	private HashSet<String> stopwordsList = new HashSet<String>();
  private boolean addedEnglishStopwords = false;
  private boolean filterHtml = false;
  private boolean pruneUsingInfoGain = false;
  private int pruneToTopN = 5000;
  private boolean pruneBottomN = true;
  private int pruneBottomThreshold = 3;
 
  private Object[][] topWordsCache = null;
  private TreeSet<IDSorter>[] topicSortedWordsCache = null;

  private InstanceList ilist = null;
	
	/**
	 * a Constructor, usually called in the setup() method in your sketch to
	 * initialize and start the library.
	 * 
	 * @param theParent
	 */
	public Wordsmith(PApplet theParent) {}
	
	// CREATE MODEL ----------------------------------------------------------------------------------
	

	public void loadExistingModel(String file) {
    try {
      lda = ParallelTopicModel.read(new File(file));
    } catch (Exception e) {
      System.err.println("Unable to restore saved topic model " + file + ": " + e);
    }
    this.createModel = false;
	}
	
	
	public void createNewModel(int numberOfTopics, double alpha, double beta) {
    this.createModel = true;
	  this.k = numberOfTopics;
	  this.numIterations = 250;
    lda = new ParallelTopicModel(numberOfTopics, alpha, beta);
	}

	public void createNewModel(int numberOfTopics) {
    createNewModel(numberOfTopics, 50.0 / numberOfTopics, 0.03);
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
  
  public void saveIntermediateStateToFile(String filepath, int frequency) {
    outputIntermediateStateFile = filepath;
    outputIntermediateStateFrequency = frequency;
  }

  public void saveIntermediateModelToFile(String filepath, int frequency) {
    outputIntermediateModelFile = filepath;
    outputIntermediateModelFrequency = frequency;
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
    stopwordsList.add(word.toLowerCase());
  }
  
  public void removeCommonEnglishWordsFromDocuments() {
    if (addedEnglishStopwords) {
      System.err.println("Already added common english words... ignoring");
      return;
    }
    addedEnglishStopwords = true;
  }
  
  public void removeHtmlFromDocuments() {
    filterHtml = true;
  }
  
  public void pruneToTopWordsUsingInformationGain(int numDesiredWords) {
    this.pruneUsingInfoGain = true;
    this.pruneToTopN = numDesiredWords;
  }
  
  public void pruneWordsOccurringLessThanThreshold(int threshold) {
    this.pruneBottomN = true;
    this.pruneBottomThreshold = threshold;
  }

  // INFERENCE -------------------------------------------------------------------------------------
	
	public void extractTopicsFromDocuments() {
	  if (!canStartLda()) {
	    return;
	  }
	  
    doPrune();
	  configureLda();

	  System.out.println("Starting LDA Inference... this might take a while (read: hours) depending " +
	  		"on the number of topics, number of documents, and document length.");
		try {
	    lda.addInstances(ilist);
			lda.estimate();
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("LDA estimation failed: " + e.getMessage());
		}

		saveState();
	}
	
  private void doPrune() {
    if (pruneBottomN) {
      long startTime = System.currentTimeMillis();

      // Version for feature sequences

      Alphabet oldAlphabet = ilist.getDataAlphabet();
      Alphabet newAlphabet = new Alphabet();

      // It's necessary to create a new instance list in
      //  order to make sure that the data alphabet is correct.
      Noop newPipe = new Noop (newAlphabet, ilist.getTargetAlphabet());
      InstanceList newInstanceList = new InstanceList (newPipe);

      // Iterate over the ilist in the old list, adding
      //  up occurrences of features.
      int numFeatures = oldAlphabet.size();
      double[] counts = new double[numFeatures];
      for (int ii = 0; ii < ilist.size(); ii++) {
        Instance instance = ilist.get(ii);
        FeatureSequence fs = (FeatureSequence) instance.getData();

        fs.addFeatureWeightsTo(counts);
      }

      // Next, iterate over the same list again, adding 
      //  each instance to the new list after pruning.
      while (ilist.size() > 0) {
        Instance instance = ilist.get(0);
        FeatureSequence fs = (FeatureSequence) instance.getData();

        fs.prune(counts, newAlphabet, pruneBottomThreshold);

        newInstanceList.add(newPipe.instanceFrom(new Instance(fs, instance.getTarget(),
                                                              instance.getName(),
                                                              instance.getSource())));
        ilist.remove(0);
      }

      System.out.println("Reduced vocab from " + oldAlphabet.size() + 
                         " words to " + newAlphabet.size());

      // Make the new list the official list.
      ilist = newInstanceList;

      System.out.println("Finishing pruning uncommon words! It took " + 
                         HumanTime.exactly(System.currentTimeMillis() - startTime));
    }

    if (pruneUsingInfoGain) {
      long startTime = System.currentTimeMillis();

      Alphabet alpha2 = new Alphabet ();
      Noop pipe2 = new Noop (alpha2, ilist.getTargetAlphabet());
      InstanceList instances2 = new InstanceList (pipe2);
      InfoGain ig = new InfoGain (ilist);
      FeatureSelection fs = new FeatureSelection (ig, pruneToTopN);
      for (int ii = 0; ii < ilist.size(); ii++) {
        Instance instance = ilist.get(ii);
        FeatureVector fv = (FeatureVector) instance.getData();
        FeatureVector fv2 = FeatureVector.newFeatureVector (fv, alpha2, fs);
        instance.unLock();
        instance.setData(null); // So it can be freed by the garbage collector
        instances2.add(pipe2.instanceFrom(new Instance(fv2, instance.getTarget(), instance.getName(), instance.getSource())),
                 ilist.getInstanceWeight(ii));
      }
      ilist = instances2;
      
      System.out.println("Finishing pruning vocabulary to top " + pruneToTopN + "! It took " + 
                         HumanTime.exactly(System.currentTimeMillis() - startTime));
    }
  }
  
  private void configureLda() {
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
    
    if (outputIntermediateStateFile != null) {
      lda.setSaveState(outputIntermediateStateFrequency, outputIntermediateStateFile);
    }

    if (outputIntermediateModelFile != null) {
      lda.setSaveSerializedModel(outputIntermediateModelFrequency, outputIntermediateModelFile);
    }

    lda.setNumThreads(numThreads);
  }
  
  private boolean canStartLda() {
    if (lda == null) {
      System.err.println("You must first call either createNewModel or loadExistingModel. " +
          "You probably want to call createNewModel, as runAlgorithm does the inference process " +
          "over previously entered documents.");
      return false;
    }
    
    if (!setInstances) {
      System.err.println("You have not added any documents. You can use:\n" +
                         "addDocumentInString(\"this is my short document\") or\n" +
                         "addDocumentInFile(\"path/to/my/favorite/file\") or\n" + 
                         "addDocumentsInDirectory(\"/path/to/a/directory/of/files\")");
      return false;
    }
    return true;
  }
  
  private void saveState() {
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

  // USE INFERRED RESULTS --------------------------------------------------------------------------

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
	  
    String[] toString = new String[topWordsCache[n].length];
    System.arraycopy(topWordsCache[n], 0, toString, 0, topWordsCache[n].length);
    
		return toString;
	}

	@SuppressWarnings("unchecked")
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
          new SaveDataInSource(),
//          new PrintInputAndTarget ("SaveDataInSource"),
          new Input2CharSequence(),
//          new PrintInputAndTarget ("Input2CharSequence"),
          (filterHtml
           ? (Pipe) new CharSequenceRemoveHTML()
           : (Pipe) new Noop()),
//          new PrintInputAndTarget ("CharSequenceRemoveHTML (" + filterHtml + ") or noop"),
          new CharSequence2TokenSequence(),
//          new PrintInputAndTarget ("CharSequence2TokenSequence"), 
          new TokenSequenceLowercase(),
//          new PrintInputAndTarget ("TokenSequenceLowercase"),
          (addedEnglishStopwords
           ? (Pipe) new TokenSequenceRemoveStopwords(false, false)
                        .addStopWords(stopwordsList.toArray(new String[0]))
                        .addStopWords(Stopwords.augmentedEnglishStopWords)
           : (Pipe) new TokenSequenceRemoveCustomStopwords(stopwordsList)),
//          new PrintInputAndTarget ("TokenSequenceRemoveStopwords (" + addedEnglishStopwords + ") or TokenSequenceRemoveCustomStopwords"), // xxx
          (filterHtml
                  ? (Pipe) new TokenSequenceRemoveCustomStopwords(new HashSet<String>())
                           .addStopWords(Stopwords.htmlStopWords)
                  : (Pipe) new Noop()),

          
          new TokenSequence2FeatureSequence(),
//          new PrintInputAndTarget ("TokenSequence2FeatureSequence"),
        });
	}
	
	public void addDocumentsInDirectory(String directory) {
    if (ilist == null) { ilist = new InstanceList (makeNewInstancePipe());}
	  boolean removeCommonPrefix=true;
	  ilist.addThruPipe(
        new FileIterator(directory, FileIterator.STARTING_DIRECTORIES, removeCommonPrefix));
		setInstances = true;
	}
	
  public void addDocumentInFile(String filepath) {
    addDocumentInFile(new File(filepath));
  }


  public void addDocumentInFile(File f) {
    if (ilist == null) { ilist = new InstanceList (makeNewInstancePipe());}
    ilist.addThruPipe(new Instance(f, null, f.toURI(), null));
    setInstances = true;
	}
	
	public void addDocumentInString(String document) {
    if (ilist == null) { ilist = new InstanceList (makeNewInstancePipe());}
	  //System.out.println("Adding document: " + document);
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
