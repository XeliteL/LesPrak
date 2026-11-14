package realty.commission;

/** Ступень шкалы: ставка действует для накопленной комиссии до верхней границы upper. */
public record Tier(double upper, double rate) {

    public Tier {
        if (upper <= 0 || rate < 0 || rate > 1) {
            throw new IllegalArgumentException("некорректная ступень шкалы");
        }
    }
}
