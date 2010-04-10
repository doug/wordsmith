package wordsmith;

import java.io.Serializable;
import java.util.HashSet;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

public class TokenSequenceRemoveCustomStopwords extends Pipe implements Serializable {   
  private static final long serialVersionUID = -3837506072873411162L;
  HashSet<String> stoplist = null;

  public TokenSequenceRemoveCustomStopwords (HashSet<String> stopwords) {
    this.stoplist = stopwords;
  }

  public TokenSequenceRemoveCustomStopwords addStopWords (String[] words) {
    for (int i = 0; i < words.length; i++)
      stoplist.add (words[i]);
    return this;
  }

  public Instance pipe (Instance carrier) {
    TokenSequence ts = (TokenSequence) carrier.getData();
    // xxx This doesn't seem so efficient.  Perhaps have TokenSequence
    // use a LinkedList, and remove Tokens from it? -?
    // But a LinkedList implementation of TokenSequence would be quite inefficient -AKM
    TokenSequence ret = new TokenSequence ();
    for (int i = 0; i < ts.size(); i++) {
      Token t = ts.get(i);
      if (! stoplist.contains (t.getText().toLowerCase())) {
        // xxx Should we instead make and add a copy of the Token?
        ret.add (t);
      }
    }
    carrier.setData(ret);
    return carrier;
  }
  
}