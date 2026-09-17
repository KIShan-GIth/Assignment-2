package in.simplifymoney.ledgersync.store;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import in.simplifymoney.ledgersync.model.Category;
import in.simplifymoney.ledgersync.model.Direction;
import in.simplifymoney.ledgersync.model.NormalizedTxn;
import org.bson.Document;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MongoDocumentStore implements DocumentStore, LedgerStore {

    private final MongoCollection<Document> collection;

    public MongoDocumentStore() {
        MongoClient mongoClient = MongoClients.create("mongodb://root:password@localhost:27017/?authSource=admin");
        MongoDatabase database = mongoClient.getDatabase("simplifymoney");
        this.collection = database.getCollection("transactions");
    }

    @Override
    public List<NormalizedTxn> forAccountMonth(String accountLast4, YearMonth month) {
        List<NormalizedTxn> results = new ArrayList<>();
        String startStr = month.atDay(1).atStartOfDay().toString();
        String endStr = month.atEndOfMonth().atTime(23, 59, 59).toString();

        for (Document doc : collection.find(Filters.and(
                Filters.eq("accountLast4", accountLast4),
                Filters.gte("occurredAt", startStr),
                Filters.lte("occurredAt", endStr + "Z")
        ))) {
            results.add(mapToTxn(doc));
        }
        return results;
    }

    @Override
    public Map<Category, BigDecimal> categoryTotals(String accountLast4) {
        Map<Category, BigDecimal> totals = new HashMap<>();
        for (Document doc : collection.find(Filters.eq("accountLast4", accountLast4))) {
            Category cat = Category.valueOf(doc.getString("category"));
            BigDecimal amt = BigDecimal.valueOf(doc.getDouble("amount")).setScale(2, RoundingMode.HALF_UP);
            totals.put(cat, totals.getOrDefault(cat, BigDecimal.ZERO).add(amt));
        }
        return totals;
    }

    @Override
    public Optional<NormalizedTxn> byMessageId(String messageId) {
        Document doc = collection.find(Filters.in("sourceMessageIds", messageId)).first();
        if (doc == null) {
            return Optional.empty();
        }
        return Optional.of(mapToTxn(doc));
    }

    @Override
    public void save(NormalizedTxn txn) {
        Document doc = new Document("sourceMessageIds", txn.sourceMessageIds())
                .append("accountLast4", txn.accountLast4())
                .append("occurredAt", txn.occurredAt().toString())
                .append("direction", txn.direction().name())
                .append("amount", txn.amount().doubleValue())
                .append("category", txn.category().name())
                .append("merchant", txn.merchant());
        collection.insertOne(doc);
    }

    @Override
    public List<NormalizedTxn> all() {
        List<NormalizedTxn> results = new ArrayList<>();
        for (Document doc : collection.find()) {
            results.add(mapToTxn(doc));
        }
        return results;
    }

    @Override
    public long count() {
        return collection.countDocuments();
    }

    private NormalizedTxn mapToTxn(Document doc) {
        List<String> sourceIds = doc.getList("sourceMessageIds", String.class);
        BigDecimal amount = BigDecimal.valueOf(doc.getDouble("amount")).setScale(2, RoundingMode.HALF_UP);
        return new NormalizedTxn(
                doc.getString("accountLast4"),
                OffsetDateTime.parse(doc.getString("occurredAt")),
                Direction.valueOf(doc.getString("direction")),
                amount,
                Category.valueOf(doc.getString("category")),
                doc.getString("merchant"),
                sourceIds
        );
    }
}