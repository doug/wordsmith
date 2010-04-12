package wordsmith;

import java.util.ArrayList;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.TokenSequence;

public class TokenSequenceCacher extends Pipe {
  private ArrayList<TokenSequence> sequenceCache;

  public TokenSequenceCacher(ArrayList<TokenSequence> sequenceCache) {
    this.sequenceCache = sequenceCache;
  }
  
  public Instance pipe (Instance carrier) {
    TokenSequence ts = (TokenSequence) carrier.getData();
    sequenceCache.add(new TokenSequence(ts));
    return carrier;
  }
}
