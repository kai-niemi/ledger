package io.cockroachdb.ledger.shell;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.shell.jline.tui.component.support.SelectorItem;

public abstract class AbstractInteractiveCommand extends AbstractShellComponent {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected Optional<Pageable> askForPage(Page<?> page) {
        if (page.isEmpty()) {
            return Pageable.unpaged().toOptional();
        }
        
        List<SelectorItem<Pageable>> items = new ArrayList<>();

        items.add(SelectorItem.of("quit", Pageable.unpaged()));

        if (page.hasNext()) {
            items.add(SelectorItem.of("Next", page.nextOrLastPageable()));
        }
        if (page.hasPrevious()) {
            items.add(SelectorItem.of("Prev", page.previousOrFirstPageable()));
        }
        if (!page.isFirst()) {
            items.add(SelectorItem.of("First", PageRequest.of(0, page.getSize())));
        }
        if (!page.isLast()) {
            items.add(SelectorItem.of("Last", PageRequest.of(page.getTotalPages() - 1, page.getSize())));
        }

        SingleItemSelector<Pageable, SelectorItem<Pageable>> component
                = new SingleItemSelector<>(getTerminal(), items, "Select page", null);
        component.setResourceLoader(getResourceLoader());
        component.setTemplateExecutor(getTemplateExecutor());

        SingleItemSelector.SingleItemSelectorContext<Pageable, SelectorItem<Pageable>> context
                = component.run(SingleItemSelector.SingleItemSelectorContext.empty());

        return context.getResultItem()
                .flatMap(si -> Optional.of(si.getItem()));
    }
}
