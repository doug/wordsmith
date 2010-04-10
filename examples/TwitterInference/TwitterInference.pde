import wordsmith.*;
import pwitter.*;

Wordsmith wordsmith = new Wordsmith(this);

// Choosing the number of topics is tricky. Trial and error is not a bad way necessarily...
// Too few topics and it will be more summarization, but not capture a lot of the nuances.
// Too many topics, and they can become junky and repetitive,
// but more nuanced topics get through.
int numTopics = 6;
// Alpha usually should be between 0.5 & 3. Beta should be between 0.01 & 0.2.
// Hyperparameter optimization (built-in) makes the initial values not as important, given
// enough iterations.
wordsmith.createNewModel(numTopics, 2.5, 0.37); // Values for johnmaeda's twitter

// Speed up the algorithm if you have a multicore machine by setting the number of cores
// higher, but only do so for large data sets (like tens of thousands of large documents).
// Otherwise it will be slower.
int numCores = 1;
wordsmith.useMulticore(numCores);

// Not too much data so its relatively quick. Better to get higher accuracy instead.
wordsmith.setNumberOfProcessIterations(1000);

// Don't output so much debug data
wordsmith.setIntermediateResultsFrequency(500);
wordsmith.setNumberShownWordsForIntermediateResults(20);

// Strip out meaningless pieces of the tweet.
wordsmith.removeHtmlFromDocuments();
wordsmith.removeCommonEnglishWordsFromDocuments();
wordsmith.pruneWordsOccurringLessThanThreshold(3);

// Load the Tweets
Pwitter twitter = new Pwitter(this);
String user = "johnmaeda";
ArrayList tweets = twitter.loadTweets("data/" + user + ".xml");
wordsmith.removeWordFromDocuments(user);

for (int i = 0; i < tweets.size(); i++) {
  Pweet tweet = (Pweet) tweets.get(i);
  String tweetText = tweet.getText();
  wordsmith.addDocumentInString(tweetText);
}

// Fire it up!
wordsmith.extractTopicsFromDocuments();

// Report on what we found!
for (int topic = 0; topic < numTopics; topic++) {
  println("\nTopic " + topic + " ------------------");
  Object[] topWords = wordsmith.getTopWordsForTopic(topic);
  for (int i = 0; i < 10; i++) {
    String prefix;
    switch(i) {
      case 0:
        prefix = "st";
        break;
      case 1:
        prefix = "nd";
        break;
      case 2:
        prefix = "rd";
        break;
      default:
        prefix = "th";
    }

    println("  " + (i+1) + prefix + " most probable word: " + topWords[i]);
  }
}

// Grab a random tweet and go through its assignments.
Pweet tweet = (Pweet) tweets.get(5);
println("\n\n\nTopic assignments per word in tweet: " + tweet.getText());
TopicWordAssignment[] assignments = wordsmith.getTopicWordAssignmentsForDocument(5);
for (int i = 0; i < assignments.length; i++) {
  TopicWordAssignment assignment = assignments[i];
  println(assignment.getWord() + " is assigned to topic " + assignment.getTopic());
}
println("\nWords not printed have been excluded from the vocabulary using the filters above.");