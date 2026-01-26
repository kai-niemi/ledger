package io.cockroachdb.ledger.web.model;

import jakarta.validation.constraints.NotNull;

public class WorkloadForm {
    @NotNull
    private String category;

    public WorkloadForm(String category) {
        this.category = category;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
