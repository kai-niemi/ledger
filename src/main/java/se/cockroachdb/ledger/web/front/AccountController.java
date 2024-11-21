package se.cockroachdb.ledger.web.front;

import java.util.UUID;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import se.cockroachdb.ledger.domain.Account;
import se.cockroachdb.ledger.domain.AccountType;
import se.cockroachdb.ledger.domain.Transfer;
import se.cockroachdb.ledger.service.account.AccountService;
import se.cockroachdb.ledger.service.transfer.TransferService;

@Controller
@RequestMapping("/account")
public class AccountController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransferService transferService;

    @GetMapping
    public Callable<String> listAccounts(@PageableDefault(size = 30) Pageable page, Model model) {
        return () -> {
            Page<Account> accountPage = accountService.findAll(AccountType.ASSET, page);

            model.addAttribute("accountPage", accountPage);

            return "account";
        };
    }

    @GetMapping("{id}")
    public Callable<String> accountDetails(@PathVariable("id") UUID id,
                                           @PageableDefault(size = 10) Pageable page,
                                           Model model) {
        return () -> {
            Page<Transfer> transferPage = transferService.findAllByAccountId(id, page);
            model.addAttribute("transferPage", transferPage);
            model.addAttribute("form", accountService.findById(id));
            return "account-detail";
        };
    }
}
