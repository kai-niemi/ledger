package io.cockroachdb.ledger.web.front;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import io.cockroachdb.ledger.service.RegionServiceFacade;
import io.cockroachdb.ledger.service.ReportingServiceFacade;
import io.cockroachdb.ledger.model.BalanceSheet;
import io.cockroachdb.ledger.web.model.RegionModel;

@Controller
@RequestMapping("/region")
public class RegionController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ReportingServiceFacade reportingServiceFacade;

    @Autowired
    private RegionServiceFacade regionServiceFacade;

    @GetMapping
    public Callable<String> indexPage(Model model) {
        List<RegionModel> regions = new ArrayList<>();

        regionServiceFacade.getGatewayRegion().ifPresent(region -> {
            model.addAttribute("gatewayRegion", region.getName());
        });
        regionServiceFacade.getPrimaryRegion().ifPresent(region -> {
            model.addAttribute("primaryRegion", region.getName());
        });
        regionServiceFacade.getSecondaryRegion().ifPresent(region -> {
            model.addAttribute("secondaryRegion", region.getName());
        });

        regionServiceFacade
                .listAllRegions()
                .forEach(region -> {
                    RegionModel regionModel = new RegionModel();
                    regionModel.setName(region.getName());
                    regionModel.setDatabaseRegions(region.getDatabaseRegions());

                    List<BalanceSheet> balanceSheets = reportingServiceFacade.getBalanceSheets(region.getCities())
                            .stream().sorted(Comparator.comparing(BalanceSheet::getCity)).toList();

                    regionModel.setBalanceSheets(balanceSheets);

                    regions.add(regionModel);
                });

        model.addAttribute("regions", regions);

        return () -> "region";
    }
}
