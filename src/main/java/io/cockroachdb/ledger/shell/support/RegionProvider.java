package io.cockroachdb.ledger.shell.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.shell.core.command.completion.CompletionContext;
import org.springframework.shell.core.command.completion.CompletionProposal;
import org.springframework.shell.core.command.completion.CompletionProvider;

import io.cockroachdb.ledger.domain.RegionCategory;
import io.cockroachdb.ledger.model.City;
import io.cockroachdb.ledger.model.Region;
import io.cockroachdb.ledger.service.RegionServiceFacade;

public class RegionProvider implements CompletionProvider {
    private final RegionServiceFacade regionServiceFacade;

    private final String prefix;

    public RegionProvider(RegionServiceFacade regionServiceFacade, String prefix) {
        this.regionServiceFacade = regionServiceFacade;
        this.prefix = prefix;
    }

    @Override
    public List<CompletionProposal> apply(CompletionContext completionContext) {
        List<CompletionProposal> result = new ArrayList<>();

        result.add(new CompletionProposal(prefix + "=" + RegionCategory.ALL.name()).description("all region cities"));
        result.add(new CompletionProposal(prefix + "=" + RegionCategory.GATEWAY.name()).description("gateway node region cities"));
        result.add(new CompletionProposal(prefix + "=" + RegionCategory.PRIMARY.name()).description("primary region cities"));
        result.add(new CompletionProposal(prefix + "=" + RegionCategory.SECONDARY.name()).description("secondary region cities"));

        Optional<Region> gateway = regionServiceFacade.getGatewayRegion();
        Optional<Region> primary = regionServiceFacade.getPrimaryRegion();

        for (Region r : regionServiceFacade.listAllRegions()) {
            String desc = String.join(",", r.getCities().stream().map(City::getName).toList());
            CompletionProposal p = new CompletionProposal(prefix + "=" + r.getName())
                    .description(desc);
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
