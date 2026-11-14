package realty;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import realty.commission.CommissionCalculator;
import realty.documents.DealInspector;
import realty.matching.ObjectMatcher;
import realty.model.Apartment;
import realty.model.BuyerRequest;
import realty.model.Deal;
import realty.model.DealStatus;
import realty.model.Document;
import realty.model.DocumentType;
import realty.model.MarketListing;
import realty.model.ObjectStatus;
import realty.model.PropertyObject;
import realty.valuation.ComparativeValuator;

/**
 * Замер времени работы алгоритмов при росте объёма данных (аналог bench.py).
 * Синтетические данные строятся по тем же правилам, что и в процедурной реализации.
 * Выводит таблицу Markdown: медиана из REPEATS запусков после прогрева, мс.
 */
public final class Bench {
    static final int[] SIZES = {1_000, 10_000, 100_000, 1_000_000};
    static final int REPEATS = 11;
    static final int MIN_WARMUP_RUNS = 3;
    static final long WARMUP_MS = 500;
    static final LocalDate TODAY = LocalDate.of(2026, 11, 1);
    static final String[] DISTRICTS = {"Тропарёво-Никулино", "Раменки", "Хорошёво-Мнёвники", "Марьино",
            "Бутово Северное"};
    static final int[] BASE_PRICE_M2 = {330_000, 380_000, 350_000, 270_000, 250_000};
    static final int[] ROOMS = {1, 1, 2, 2, 2, 3, 3, 4};
    static final int[] FLOORS = {5, 9, 12, 14, 17, 22, 25};

    private Bench() {
    }

    static double timed(Runnable task) {
        // прогрев: JIT-компилятор оптимизирует код после многократного выполнения
        long warmupEnd = System.nanoTime() + WARMUP_MS * 1_000_000L;
        for (int i = 0; i < MIN_WARMUP_RUNS || System.nanoTime() < warmupEnd; i++) {
            task.run();
        }
        double[] times = new double[REPEATS];
        for (int i = 0; i < REPEATS; i++) {
            long t0 = System.nanoTime();
            task.run();
            times[i] = (System.nanoTime() - t0) / 1e6;
        }
        Arrays.sort(times);
        return times[REPEATS / 2];
    }

    public static void main(String[] args) {
        Random rng = new Random(1);
        Apartment subject = new Apartment(DISTRICTS[1], 2, 54.0, 7, 17, 2005);
        BuyerRequest request = new BuyerRequest(0, "", 0, 16_000_000, Set.of(2, 3),
                List.of(DISTRICTS[0], DISTRICTS[1], DISTRICTS[2]), 45, false, false);
        System.out.println("| n | Алгоритм 1, мс | Алгоритм 2, мс | Алгоритм 3, мс | Алгоритм 4, мс |");
        System.out.println("|---|---|---|---|---|");
        for (int n : SIZES) {
            List<MarketListing> listings = new ArrayList<>(n);
            List<PropertyObject> objects = new ArrayList<>(n);
            for (int i = 1; i <= n; i++) {
                String district = DISTRICTS[rng.nextInt(DISTRICTS.length)];
                int rooms = ROOMS[rng.nextInt(ROOMS.length)];
                double area = Math.round((20 + rooms * 12 + rng.nextDouble() * (10 + rooms * 6)) * 10) / 10.0;
                int floors = FLOORS[rng.nextInt(FLOORS.length)];
                int floor = 1 + rng.nextInt(floors);
                int year = 1960 + rng.nextInt(65);
                double priceM2 = BASE_PRICE_M2[Arrays.asList(DISTRICTS).indexOf(district)] * (0.85 + rng.nextDouble() * 0.3);
                if (floor == 1) {
                    priceM2 *= 0.95;
                }
                long price = Math.round(priceM2 * area / 10_000) * 10_000;
                listings.add(new MarketListing(i, district, rooms, area, floor, floors, year, price,
                        TODAY.minusDays(rng.nextInt(366))));
                objects.add(new PropertyObject(i, district, rooms, area, floor, floors, year, price,
                        ObjectStatus.ACTIVE));
            }
            int typeCount = n / 100;
            List<DocumentType> types = new ArrayList<>(typeCount);
            for (int i = 0; i < typeCount; i++) {
                types.add(new DocumentType("t" + i, "Тип " + i, null, 30));
            }
            List<Document> docs = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                docs.add(new Document("t" + rng.nextInt(typeCount), LocalDate.of(2026, 10, 20)));
            }
            Deal deal = Deal.builder(0, 0, 9_000_000, 0.03).signingDate(LocalDate.of(2026, 11, 20))
                    .documents(docs).build();
            List<Deal> deals = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                deals.add(Deal.builder(i, rng.nextInt(50), (3 + rng.nextInt(28)) * 1_000_000L, 0.03)
                        .status(DealStatus.CLOSED).closed(LocalDate.of(2026, 10, 1 + rng.nextInt(31))).build());
            }

            ComparativeValuator valuator = ComparativeValuator.standard(listings);
            ObjectMatcher matcher = ObjectMatcher.standard();
            DealInspector inspector = DealInspector.standard(types);
            CommissionCalculator calculator = CommissionCalculator.standard();
            double t1 = timed(() -> valuator.estimate(subject, TODAY));
            double t2 = timed(() -> matcher.match(request, objects, 10));
            double t3 = timed(() -> inspector.inspect(deal, TODAY));
            double t4 = timed(() -> calculator.monthly(deals, YearMonth.of(2026, 10)));
            System.out.printf(Locale.ROOT, "| %s | %.2f | %.2f | %.2f | %.2f |%n",
                    String.format(Locale.ROOT, "%,d", n).replace(',', ' '), t1, t2, t3, t4);
        }
    }
}
