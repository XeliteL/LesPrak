package realty.valuation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import realty.model.Apartment;
import realty.model.MarketListing;

/**
 * Алгоритм 1. Оценка стоимости квартиры сравнительным подходом по объектам-аналогам.
 *
 * <p>Шаги: отбор аналогов; корректировка удельной цены цепочкой {@link PriceAdjustment};
 * исключение выбросов относительно медианы; средневзвешенная удельная цена.
 * Сложность O(n + k log k), где n — число предложений, k — число аналогов.
 */
public final class ComparativeValuator implements Valuator {
    public static final double AREA_TOLERANCE = 0.25;
    public static final int MAX_AGE_DAYS = 180;
    public static final double BARGAIN_DISCOUNT = 0.03;
    public static final double OUTLIER_TOLERANCE = 0.25;
    public static final int MIN_ANALOGS = 3;
    public static final long ROUND_TO = 10_000;

    private final Collection<MarketListing> listings;
    private final List<PriceAdjustment> adjustments;

    public ComparativeValuator(Collection<MarketListing> listings, List<PriceAdjustment> adjustments) {
        this.listings = listings;
        this.adjustments = List.copyOf(adjustments);
    }

    /** Оценщик с корректировками по умолчанию: скидка на торг и поправка на этаж. */
    public static ComparativeValuator standard(Collection<MarketListing> listings) {
        return new ComparativeValuator(listings,
                List.of(new BargainDiscount(BARGAIN_DISCOUNT), FloorAdjustment.standard()));
    }

    /** Шаг 1: объявления, сопоставимые с оцениваемой квартирой. */
    public List<MarketListing> selectAnalogs(Apartment subject, LocalDate today) {
        List<MarketListing> result = new ArrayList<>();
        for (MarketListing item : listings) {
            if (!item.district().equals(subject.district()) || item.rooms() != subject.rooms()) {
                continue;
            }
            if (Math.abs(item.area() - subject.area()) / subject.area() > AREA_TOLERANCE) {
                continue;
            }
            if (item.ageDays(today) > MAX_AGE_DAYS) {
                continue;
            }
            result.add(item);
        }
        return result;
    }

    /** Шаг 2: удельная цена аналога после всех корректировок. */
    public double adjustedUnitPrice(Apartment subject, MarketListing analog) {
        double unit = analog.unitPrice();
        for (PriceAdjustment adjustment : adjustments) {
            unit = adjustment.apply(unit, subject, analog);
        }
        return unit;
    }

    /** Вес аналога 1 / (1 + d): чем ближе аналог по площади и году постройки, тем больше вес. */
    static double similarityWeight(Apartment subject, Apartment analog) {
        double d = Math.abs(analog.area() - subject.area()) / subject.area();
        d += Math.abs(analog.yearBuilt() - subject.yearBuilt()) / 50.0;
        return 1 / (1 + d);
    }

    @Override
    public ValuationResult estimate(Apartment subject, LocalDate today) {
        List<MarketListing> analogs = selectAnalogs(subject, today);
        if (analogs.size() < MIN_ANALOGS) {
            return new ValuationResult.Insufficient(analogs.size());
        }

        List<Analog> adjusted = new ArrayList<>(analogs.size());
        for (MarketListing a : analogs) {
            adjusted.add(new Analog(a, adjustedUnitPrice(subject, a)));
        }
        double med = median(adjusted);
        List<Analog> kept = adjusted.stream()
                .filter(a -> Math.abs(a.unitPrice() - med) / med <= OUTLIER_TOLERANCE)
                .toList();
        if (kept.size() < MIN_ANALOGS) {
            return new ValuationResult.Insufficient(analogs.size());
        }

        double totalWeight = 0;
        double total = 0;
        double min = Double.MAX_VALUE;
        double max = 0;
        for (Analog a : kept) {
            double w = similarityWeight(subject, a.listing());
            totalWeight += w;
            total += w * a.unitPrice();
            min = Math.min(min, a.unitPrice());
            max = Math.max(max, a.unitPrice());
        }
        double unitPrice = total / totalWeight;
        return new ValuationResult.Estimate(
                (long) Math.rint(unitPrice),
                roundPrice(unitPrice * subject.area()),
                roundPrice(min * subject.area()),
                roundPrice(max * subject.area()),
                analogs.size(),
                kept.stream().map(a -> a.listing().id()).toList());
    }

    private static long roundPrice(double value) {
        return (long) Math.rint(value / ROUND_TO) * ROUND_TO;
    }

    private static double median(List<Analog> analogs) {
        double[] units = analogs.stream().mapToDouble(Analog::unitPrice).sorted().toArray();
        int mid = units.length / 2;
        return units.length % 2 == 1 ? units[mid] : (units[mid - 1] + units[mid]) / 2;
    }

    /** Аналог со скорректированной удельной ценой. */
    private record Analog(MarketListing listing, double unitPrice) {
    }
}
