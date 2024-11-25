package se.cockroachdb.ledger.web.api.csv;

public interface CsvWriter<T> {
    void writeHeader();

    void writeItem(T item);

    void writeFooter();
}
