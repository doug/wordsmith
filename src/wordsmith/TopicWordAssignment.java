package wordsmith;

public class TopicWordAssignment {
  public static final int STOPWORD = -1;
  String word;
  int topic;
  
  public TopicWordAssignment(String word, int topic) {
    this.word = word;
    this.topic = topic;
  }
  
  public TopicWordAssignment(String word) {
    this.word = word;
    this.topic = STOPWORD;
  }

  public boolean isStopword() {
    return topic == STOPWORD;
  }
  
  public String getWord() {
    return word;
  }
  
  public int getTopic() {
    return topic;
  }
}
