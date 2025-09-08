package io.cockroachdb.ledger.web.front;

import java.sql.SQLException;
import java.util.concurrent.Callable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import io.cockroachdb.ledger.service.RegionServiceFacade;

@Controller
@RequestMapping("/")
public class HomeController {
    @Autowired
    private RegionServiceFacade regionServiceFacade;

    @GetMapping("/")
    public Callable<String> homePage(Model model) {
        model.addAttribute("databaseVersion", regionServiceFacade.getDatabaseVersion());
        model.addAttribute("databaseIsolation", regionServiceFacade.getDatabaseIsolation());
        return () -> "home";
    }

    @PostMapping("/inform")
    public ResponseEntity<?> informPage(Model model) {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/error-test")
    public String errorTest() {
        throw new IllegalStateException("Disturbance!");
    }

    @GetMapping("/error-test2")
    public String errorTest2() throws Exception {
        throw new SQLException("Disturbance!", "12345");
    }

    @GetMapping("/error-test3")
    public String errorTest3() {
        throw new IncorrectResultSizeDataAccessException(100, 1);
    }
}
