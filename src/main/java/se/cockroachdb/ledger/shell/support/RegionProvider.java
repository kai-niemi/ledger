package se.cockroachdb.ledger.shell.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.CompletionContext;
import org.springframework.shell.CompletionProposal;
import org.springframework.shell.standard.ValueProvider;

import se.cockroachdb.ledger.model.City;
import se.cockroachdb.ledger.model.Region;
import se.cockroachdb.ledger.model.RegionCategory;
import se.cockroachdb.ledger.service.RegionServiceFacade;

public class RegionProvider implements ValueProvider {
    @Autowired
    private RegionServiceFacade regionServiceFacade;

    @Override
    public List<CompletionProposal> complete(CompletionContext completionContext) {
        List<CompletionProposal> result = new ArrayList<>();

        result.add(new CompletionProposal(RegionCategory.all.name()).description("all region cities"));
        result.add(new CompletionProposal(RegionCategory.gateway.name()).description("gateway node region cities"));
        result.add(new CompletionProposal(RegionCategory.primary.name()).description("primary region cities"));
        result.add(new CompletionProposal(RegionCategory.secondary.name()).description("secondary region cities"));

        Optional<Region> gateway = regionServiceFacade.getGatewayRegion();
        Optional<Region> primary = regionServiceFacade.getPrimaryRegion();

        for (Region r : regionServiceFacade.listAllRegions()) {
            String desc = String.join(",", r.getCities().stream().map(City::getName).toList());
            CompletionProposal p = new CompletionProposal(r.getName()).description(desc);
            if (gateway.isPresent() && gateway.get().equals(r)) {
                result.add(0, p);
            } else if (primary.isPresent() && primary.get().equals(r)) {
                result.add(0, p);
            } else {
                result.add(p);
            }
        }

        return result;
    }
}
