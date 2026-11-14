package realty.matching;

/** Общая часть составляющих оценки: хранение веса. */
public abstract class WeightedScore implements ScoreComponent {
    private final double weight;

    protected WeightedScore(double weight) {
        if (weight < 0) {
            throw new IllegalArgumentException("вес не может быть отрицательным");
        }
        this.weight = weight;
    }

    @Override
    public final double weight() {
        return weight;
    }
}
