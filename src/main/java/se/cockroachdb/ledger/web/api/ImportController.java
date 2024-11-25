package se.cockroachdb.ledger.web.api;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import se.cockroachdb.ledger.ApplicationProperties;
import se.cockroachdb.ledger.domain.Account;
import se.cockroachdb.ledger.domain.AccountType;
import se.cockroachdb.ledger.model.AccountPlan;
import se.cockroachdb.ledger.model.City;
import se.cockroachdb.ledger.model.Region;
import se.cockroachdb.ledger.service.RegionServiceFacade;
import se.cockroachdb.ledger.util.Money;
import se.cockroachdb.ledger.web.api.csv.AccountCsvWriter;
import se.cockroachdb.ledger.web.api.csv.CsvWriter;

@RestController
@RequestMapping(value = "/api/import")
public class ImportController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private RegionServiceFacade regionServiceFacade;

    @Autowired
    private ApplicationProperties applicationProperties;

    @GetMapping(value = "/account.csv/{region}",
            produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<StreamingResponseBody> exportTableData(
            @PathVariable("region") String region,
            @RequestParam(required = false) MultiValueMap<String, String> valueMap,
            @RequestHeader(value = HttpHeaders.ACCEPT_ENCODING,
                    required = false, defaultValue = "") String acceptEncoding) {

        Map<String, String> allParams = Objects.requireNonNull(valueMap, "params required").toSingleValueMap();

        AccountPlan accountPlan = applicationProperties.getAccountPlan();

        BigDecimal initialBalance = new BigDecimal(allParams.getOrDefault("initialBalance",
                "" + accountPlan.getAccountsPerCity()));

        int accountsPerCity = Integer.valueOf(allParams.getOrDefault("accountsPerCity",
                "" + accountPlan.getAccountsPerCity()));

        ResponseEntity.BodyBuilder bb = ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0");

        final List<City> cities = Region.joinCities(regionServiceFacade.listRegions(region));

        logger.info("Region %s cities %s".formatted(region, cities.stream().map(City::getName).toList()));

        return bb.body(outputStream -> {
            boolean gzip = false;//acceptEncoding.equalsIgnoreCase("gzip");
//            if (gzip) {
//                bb.header(HttpHeaders.CONTENT_ENCODING, "gzip");
//            }

            try (PrintWriter pw = new PrintWriter(
                    new BufferedWriter(new OutputStreamWriter(gzip
                            ? new GZIPOutputStream(outputStream, true) : outputStream)))) {
                CsvWriter<Account> accountCsvWriter = new AccountCsvWriter(pw);

                accountCsvWriter.writeHeader();

                cities.forEach(city -> {
                    final Money balance = Money.of(initialBalance, city.getCurrencyInstance());
                    generateAccounts(city, balance, accountsPerCity, accountCsvWriter::writeItem);
                });

                accountCsvWriter.writeFooter();
            }
        });
    }

    private void generateAccounts(City city,
                                  Money initialBalance,
                                  int accountsPerCity,
                                  Consumer<Account> consumer) {

        Money totalBalance = initialBalance.multiply(accountsPerCity);

        Account systemAccount = Account.builder()
                .withGeneratedId()
                .withCity(city.getName())
                .withName("system-account")
                .withAllowNegative(true)
                .withBalance(totalBalance.negate())
                .withAccountType(AccountType.LIABILITY)
                .withUpdated(LocalDateTime.now()).build();

        consumer.accept(systemAccount);

        IntStream.rangeClosed(1, accountsPerCity).forEach(value -> {
            Account userAccount = Account.builder()
                    .withGeneratedId()
                    .withCity(city.getName())
                    .withName("user-account:" + value)
                    .withAllowNegative(false)
                    .withBalance(initialBalance)
                    .withAccountType(AccountType.ASSET)
                    .withUpdated(LocalDateTime.now()).build();

            consumer.accept(userAccount);
        });
    }
}
