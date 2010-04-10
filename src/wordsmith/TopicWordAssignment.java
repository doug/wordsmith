package wordsmith;

public class TopicWordAssignment {
  String word;
  int topic;
  
  public TopicWordAssignment(String word, int topic) {
    this.word = word;
    this.topic = topic;
  }
  
  public String getWord() {
    return word;
  }
  
  public int getTopic() {
    return topic;
  }
}
