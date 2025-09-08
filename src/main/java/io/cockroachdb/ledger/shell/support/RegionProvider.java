package io.cockroachdb.ledger.shell.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.CompletionContext;
import org.springframework.shell.CompletionProposal;
import org.springframework.shell.standard.ValueProvider;

import io.cockroachdb.ledger.domain.RegionCategory;
import io.cockroachdb.ledger.model.City;
import io.cockroachdb.ledger.model.Region;
import io.cockroachdb.ledger.service.RegionServiceFacade;

public class RegionProvider implements ValueProvider {
    @Autowired
    private RegionServiceFacade regionServiceFacade;

    @Override
    public List<CompletionProposal> complete(CompletionContext completionContext) {
        List<CompletionProposal> result = new ArrayList<>();

        result.add(new CompletionProposal(RegionCategory.ALL.name()).description("all region cities"));
        result.add(new CompletionProposal(RegionCategory.GATEWAY.name()).description("gateway node region cities"));
        result.add(new CompletionProposal(RegionCategory.PRIMARY.name()).description("primary region cities"));
        result.add(new CompletionProposal(RegionCategory.SECONDARY.name()).description("secondary region cities"));

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
