package io.cockroachdb.ledger.web.api;

public abstract class LinkRelations {
    private LinkRelations() {
    }

    public static final String ACTUATORS_REL = "actuators";

    // IANA standard link relations:
    // http://www.iana.org/assignments/link-relations/link-relations.xhtml

    public static final String CURIE_NAMESPACE = "ledger";
}
