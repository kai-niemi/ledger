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

import io.cockroachdb.ledger.service.RegionAdminFacade;
import io.cockroachdb.ledger.service.ReportingFacade;
import io.cockroachdb.ledger.domain.BalanceSheet;
import io.cockroachdb.ledger.web.model.RegionModel;

@Controller
@RequestMapping("/region")
public class RegionController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ReportingFacade reportingFacade;

    @Autowired
    private RegionAdminFacade regionAdminFacade;

    @GetMapping
    public Callable<String> indexPage(Model model) {
        List<RegionModel> regions = new ArrayList<>();

        regionAdminFacade.getGatewayRegion().ifPresent(region -> {
            model.addAttribute("gatewayRegion", region.getName());
        });
        regionAdminFacade.getPrimaryRegion().ifPresent(region -> {
            model.addAttribute("primaryRegion", region.getName());
        });
        regionAdminFacade.getSecondaryRegion().ifPresent(region -> {
            model.addAttribute("secondaryRegion", region.getName());
        });

        regionAdminFacade
                .listAllRegions()
                .forEach(region -> {
                    RegionModel regionModel = new RegionModel();
                    regionModel.setName(region.getName());
                    regionModel.setDatabaseRegions(region.getDatabaseRegions());

                    List<BalanceSheet> balanceSheets = reportingFacade.getBalanceSheets(region.getCities())
                            .stream().sorted(Comparator.comparing(BalanceSheet::getCity)).toList();

                    regionModel.setBalanceSheets(balanceSheets);

                    regions.add(regionModel);
                });

        model.addAttribute("regions", regions);

        return () -> "region";
    }
}
