package io.cockroachdb.ledger.shell;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.jline.PromptProvider;
import org.springframework.stereotype.Component;

import io.cockroachdb.ledger.service.workload.WorkloadManager;

@Component
public class ShellPromptProvider implements PromptProvider {
    @Autowired
    private WorkloadManager workloadManager;

    @Override
    public AttributedString getPrompt() {
//        int workloads = workloadManager.getWorkloads().size();
//        if (workloads > 0) {
//            return new AttributedString("ledger:(%d)$ ".formatted(workloads),
//                    AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN));
//        }
        return new AttributedString("ledger:$ ",
                AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));
    }
}
