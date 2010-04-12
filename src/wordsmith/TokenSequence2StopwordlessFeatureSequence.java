package wordsmith;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

public class TokenSequence2StopwordlessFeatureSequence extends Pipe {
  public TokenSequence2StopwordlessFeatureSequence (Alphabet dataDict) {
    super (dataDict, null);
  }

  public TokenSequence2StopwordlessFeatureSequence () {
    super(new Alphabet(), null);
  }
  
  public Instance pipe (Instance carrier) {
    TokenSequence ts = (TokenSequence) carrier.getData();
    FeatureSequence ret =
      new FeatureSequence ((Alphabet)getDataAlphabet(), ts.size());
    for (int i = 0; i < ts.size(); i++) {
      Token t = ts.get(i);
      if (t.getProperties() == null || !t.getProperty("stopword").equals(true)) {
        ret.add (t.getText());
      }
    }
    carrier.setData(ret);
    return carrier;
  }
}
