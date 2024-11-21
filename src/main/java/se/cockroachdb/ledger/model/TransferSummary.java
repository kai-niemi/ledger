package se.cockroachdb.ledger.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import se.cockroachdb.ledger.util.Money;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransferSummary {
    public static TransferSummary empty(City city) {
        TransferSummary summary = new TransferSummary() ;
        summary.setCity(city.getName());
        summary.setNumberOfTransfers(0);
        summary.setNumberOfLegs(0);
        summary.setTotalTurnover(Money.zero(city.getCurrencyInstance()));
        summary.setTotalCheckSum(Money.zero(city.getCurrencyInstance()));
        return summary;
    }

    private String city;

    private long numberOfTransfers;

    private long numberOfLegs;

    private Money totalTurnover;

    private Money totalCheckSum;

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public long getNumberOfTransfers() {
        return numberOfTransfers;
    }

    public void setNumberOfTransfers(long numberOfTransfers) {
        this.numberOfTransfers = numberOfTransfers;
    }

    public long getNumberOfLegs() {
        return numberOfLegs;
    }

    public void setNumberOfLegs(long numberOfLegs) {
        this.numberOfLegs = numberOfLegs;
    }

    public Money getTotalTurnover() {
        return totalTurnover;
    }

    public void setTotalTurnover(Money totalTurnover) {
        this.totalTurnover = totalTurnover;
    }

    public Money getTotalCheckSum() {
        return totalCheckSum;
    }

    public void setTotalCheckSum(Money totalCheckSum) {
        this.totalCheckSum = totalCheckSum;
    }

    @Override
    public String toString() {
        return "TransactionSummary{" +
               "city=" + city +
               ", numberOfTransactions=" + numberOfTransfers +
               ", numberOfLegs=" + numberOfLegs +
               ", totalTurnover=" + totalTurnover +
               ", totalCheckSum=" + totalCheckSum +
               '}';
    }
}
