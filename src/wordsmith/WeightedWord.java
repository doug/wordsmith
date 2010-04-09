package wordsmith;

public class WeightedWord {
  public String word;
  public double weight;
  public WeightedWord(String word, double weight) {
    this.word = word;
    this.weight = weight;
  }
  
  public String word() {
    return word;
  }
  
  public double weight() {
    return weight;
  }
}
