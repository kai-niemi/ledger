package io.cockroachdb.ledger.web.front;

import java.util.UUID;
import java.util.concurrent.Callable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import io.cockroachdb.ledger.domain.AccountEntity;
import io.cockroachdb.ledger.domain.AccountType;
import io.cockroachdb.ledger.domain.TransferEntity;
import io.cockroachdb.ledger.service.account.AccountService;
import io.cockroachdb.ledger.service.transfer.TransferService;

@Controller
@RequestMapping("/account")
public class AccountController {
    @Autowired
    private AccountService accountService;

    @Autowired
    private TransferService transferService;

    @GetMapping
    public Callable<String> listAccounts(
            @RequestParam(value = "type", required = false) AccountType accountType,
            @PageableDefault(size = 30) Pageable page,
            Model model) {
        return () -> {
            AccountType type = accountType != null ?accountType : AccountType.ASSET;

            Page<AccountEntity> accountPage = accountService.findAll(type, page);

            model.addAttribute("accountPage", accountPage);
            model.addAttribute("form", new AccountFilterForm(type));

            return "account";
        };
    }

    @PostMapping
    public Callable<String> filterAccounts(@ModelAttribute("form") AccountFilterForm form,
                                           @PageableDefault(size = 10) Pageable page,
                                           Model model) {
        return () -> {
            Page<AccountEntity> accountPage = accountService.findAll(form.getAccountType(), page);

            model.addAttribute("accountPage", accountPage);
            model.addAttribute("form", form);

            return "account";
        };
    }

    @GetMapping("{id}")
    public Callable<String> accountDetails(@PathVariable("id") UUID id,
                                           @PageableDefault(size = 10) Pageable page,
                                           Model model) {
        return () -> {
            Page<TransferEntity> transferPage = transferService.findAllByAccountId(id, page);
            model.addAttribute("transferPage", transferPage);
            model.addAttribute("form", accountService.findById(id));
            return "account-detail";
        };
    }
}
