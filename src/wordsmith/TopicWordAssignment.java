package wordsmith;

public class TopicWordAssignment {
  public String word;
  public int topic;
  
  public TopicWordAssignment(String word, int topic) {
    this.word = word;
    this.topic = topic;
  }
  
  public String word() {
    return word;
  }
  
  public int topic() {
    return topic;
  }
}
