package wordsmith;

public class WeightedWord {
  String word;
  float weight;
  
  public WeightedWord(String word, float weight) {
    this.word = word;
    this.weight = weight;
  }
  
  public String getWord() {
    return word;
  }
  
  public float getWeight() {
    return weight;
  }
}
