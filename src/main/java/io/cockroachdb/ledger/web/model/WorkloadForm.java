package io.cockroachdb.ledger.web.model;

import jakarta.validation.constraints.NotNull;

public class WorkloadForm {
    @NotNull
    private String title;

    public WorkloadForm(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
