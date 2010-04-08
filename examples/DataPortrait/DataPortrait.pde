import wordsmith.*;
import pwitter.*;

Wordsmith wordsmith;
Pwitter twitter;
ArrayList tweets;

void setup() {
  size(1024,768);
  prettyDefaults();
  twitter = new Pwitter(this);
  tweets = twitter.getLatestTweets("_doug");
  wordsmith = new Wordsmith(this);
  for(int i=0;i<tweets.size();i++) {
    Pweet tweet = (Pweet)tweets.get(i);
    println(tweet.getText());
  }
}

void draw() {
  background(0);
}


void prettyDefaults() {
  smooth();
  hint(ENABLE_NATIVE_FONTS);
}