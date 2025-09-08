package se.cockroachdb.ledger.web.api;

import java.io.IOException;

import org.springframework.hateoas.Link;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import se.cockroachdb.ledger.web.model.MessageModel;

@RestController
@RequestMapping(value = "/api")
public class IndexController {
    @GetMapping
    public ResponseEntity<MessageModel> index() {
        MessageModel index = new MessageModel();
        index.setMessage("Welcome to text-only accounting ledger. You are in a dark, cold lobby.");

        index.add(Link.of(ServletUriComponentsBuilder.fromCurrentContextPath()
                        .pathSegment("actuator")
                        .buildAndExpand()
                        .toUriString())
                .withRel(LinkRelations.ACTUATORS_REL)
                .withTitle("Spring boot actuators"));

        return ResponseEntity.ok(index);
    }

    @GetMapping("/fake")
    public @ResponseBody ResponseEntity<MessageModel> errorOnGet() {
        throw new FakeException("Fake exception!", new IOException("I/O disturbance!"));
    }

    @PutMapping("/fake")
    public ResponseEntity<MessageModel> errorOnPut() {
        throw new FakeException("Fake exception!", new IOException("I/O disturbance!"));
    }

    @PostMapping("/fake")
    public ResponseEntity<MessageModel> errorOnPost() {
        throw new FakeException("Fake exception!", new IOException("I/O disturbance!"));
    }

    @DeleteMapping("/fake")
    public ResponseEntity<MessageModel> errorOnDelete() {
        throw new FakeException("Fake exception!", new IOException("I/O disturbance!"));
    }
}
