package se.cockroachdb.ledger.web.front;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import se.cockroachdb.ledger.model.City;
import se.cockroachdb.ledger.model.CityModel;
import se.cockroachdb.ledger.model.RegionModel;
import se.cockroachdb.ledger.service.RegionServiceFacade;
import se.cockroachdb.ledger.service.ReportingServiceFacade;
import se.cockroachdb.ledger.util.Money;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@Controller
@RequestMapping("/region")
public class RegionController {
    @Autowired
    private ReportingServiceFacade reportingServiceFacade;

    @Autowired
    private RegionServiceFacade regionServiceFacade;

    @GetMapping
    public Callable<String> indexPage(Model model) {
        List<RegionModel> regions = new ArrayList<>();

        regionServiceFacade
                .listAllRegions()
                .forEach(region -> {
                    Map<String, CityModel> cityModels = new HashMap<>();

                    // parallel fork+join
                    reportingServiceFacade.getAccountSummary(region.getCities())
                            .forEach(accountSummary -> {
                                CityModel cityModel = cityModels.computeIfAbsent(
                                        accountSummary.getCity(), x -> new CityModel());
                                cityModel.setName(accountSummary.getCity());
                                cityModel.setUpdatedAt(accountSummary.getUpdatedAt());
                                cityModel.setNumberOfAccounts(accountSummary.getNumberOfAccounts());
                                cityModel.setMinBalance(accountSummary.getMaxBalance());
                                cityModel.setMaxBalance(accountSummary.getMaxBalance());
                                cityModel.setTotalBalance(accountSummary.getTotalBalance());
                            });

                    // parallel fork+join
                    reportingServiceFacade.getTransactionSummary(region.getCities())
                            .forEach(transferSummary -> {
                                CityModel cityModel = cityModels.get(transferSummary.getCity());
                                if (cityModel != null) {
                                    cityModel.setNumberOfTransfers(transferSummary.getNumberOfTransfers());
                                    cityModel.setNumberOfLegs(transferSummary.getNumberOfLegs());
                                    cityModel.setTotalTurnover(transferSummary.getTotalTurnover());
                                }
                            });

                    cityModels.values().forEach(cityModel -> {
                        City.findByName(region.getCities(), cityModel.getName())
                                .ifPresent(city -> {
                                    cityModel.setCountryCode(city.getCountry());
                                });
                    });

                    RegionModel regionModel = new RegionModel();
                    regionModel.setName(region.getName());
                    regionModel.setDatabaseRegions(region.getDatabaseRegions());
                    regionModel.setCityModels(cityModels
                            .values()
                            .stream()
                            .sorted(Comparator.comparing(CityModel::getName))
                            .toList());

                    regions.add(regionModel);
                });

        model.addAttribute("regions", regions);

        regionServiceFacade.getGatewayRegion().ifPresent(region -> {
            model.addAttribute("gatewayRegion", region.getName());
        });
        regionServiceFacade.getPrimaryRegion().ifPresent(region -> {
            model.addAttribute("primaryRegion", region.getName());
        });
        regionServiceFacade.getSecondaryRegion().ifPresent(region -> {
            model.addAttribute("secondaryRegion", region.getName());
        });

        return () -> "region";
    }
}
