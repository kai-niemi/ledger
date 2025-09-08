package se.cockroachdb.ledger.web.api;

public interface CsvWriter<T> {
    void writeHeader();

    void writeItem(T item);

    void writeFooter();
}
