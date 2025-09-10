package io.cockroachdb.ledger.web.api;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

import io.cockroachdb.ledger.domain.AccountEntity;
import io.cockroachdb.ledger.domain.AccountType;
import io.cockroachdb.ledger.model.AccountPlan;
import io.cockroachdb.ledger.model.ApplicationProperties;
import io.cockroachdb.ledger.model.City;
import io.cockroachdb.ledger.service.RegionServiceFacade;
import io.cockroachdb.ledger.util.Money;

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
        boolean gzip = "gzip".equalsIgnoreCase(acceptEncoding);

        AccountPlan accountPlan = applicationProperties.getAccountPlan();

        Map<String, String> allParams = Objects.requireNonNull(valueMap, "params required")
                .toSingleValueMap();

        BigDecimal initialBalance = new BigDecimal(allParams.getOrDefault("initialBalance",
                "" + accountPlan.getAccountsPerCity()));
        int accountsPerCity = Integer.parseInt(allParams.getOrDefault("accountsPerCity",
                "" + accountPlan.getAccountsPerCity()));

        Set<City> cities = regionServiceFacade.listCities(region);

        logger.info("""
                >> Received account import request <<
                Region: %s
                Resolved %d cities: %s
                Initial account balance: %s
                Accounts per city: %,d
                Total accounts: %,d
                """.formatted(region,
                cities.size(),
                cities.stream().map(City::getName).toList(),
                initialBalance,
                accountsPerCity,
                accountsPerCity * cities.size() * 2));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header(HttpHeaders.CONTENT_ENCODING, gzip ? "gzip" : "identity")
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .body(outputStream -> {
                    try (PrintWriter pw = new PrintWriter(
                            new BufferedWriter(new OutputStreamWriter(gzip
                                    ? new GZIPOutputStream(outputStream, true) : outputStream)))) {
                        CsvWriter<AccountEntity> csvWriter = new AccountCsvWriter(pw);
                        csvWriter.writeHeader();

                        cities.forEach(city -> {
                            Money balance = Money.of(initialBalance, city.getCurrencyInstance());
                            generateAccounts(city, balance, accountsPerCity, csvWriter::writeItem);
                        });

                        csvWriter.writeFooter();
                    }
                });
    }

    private void generateAccounts(City city,
                                  Money initialBalance,
                                  int accountsPerCity,
                                  Consumer<AccountEntity> consumer) {

        Money totalBalance = initialBalance.multiply(accountsPerCity);

        AccountEntity systemAccountEntity = AccountEntity.builder()
                .withGeneratedId()
                .withCity(city.getName())
                .withName("system-account")
                .withAllowNegative(true)
                .withBalance(totalBalance.negate())
                .withAccountType(AccountType.LIABILITY)
                .withUpdated(LocalDateTime.now())
                .build();

        consumer.accept(systemAccountEntity);

        IntStream.rangeClosed(1, accountsPerCity).forEach(value -> {
            AccountEntity userAccountEntity = AccountEntity.builder()
                    .withGeneratedId()
                    .withCity(city.getName())
                    .withName("user-account:" + value)
                    .withAllowNegative(false)
                    .withBalance(initialBalance)
                    .withAccountType(AccountType.ASSET)
                    .withUpdated(LocalDateTime.now())
                    .build();

            consumer.accept(userAccountEntity);
        });
    }
}
