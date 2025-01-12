package se.cockroachdb.ledger.web.front;

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

import se.cockroachdb.ledger.domain.Transfer;
import se.cockroachdb.ledger.domain.TransferItem;
import se.cockroachdb.ledger.domain.TransferType;
import se.cockroachdb.ledger.service.transfer.TransferService;

@Controller
@RequestMapping("/transfer")
public class TransferController {
    @Autowired
    private TransferService transferService;

    @GetMapping
    public Callable<String> listTransfers(
            @RequestParam(value = "type", required = false) TransferType transferType,
            @PageableDefault(size = 30) Pageable page, Model model) {
        return () -> {
            TransferType type = transferType != null ? transferType : TransferType.BANK;

            Page<Transfer> transferPage = transferService.findAll(type, page);

            model.addAttribute("transferPage", transferPage);
            model.addAttribute("form", new TransferFilterForm(type));

            return "transfer";
        };
    }

    @PostMapping
    public Callable<String> filterTransfers(@ModelAttribute("form") TransferFilterForm form,
                                            @PageableDefault(size = 10) Pageable page,
                                            Model model) {
        return () -> {
            Page<Transfer> transferPage = transferService.findAll(form.getTransferType(), page);

            model.addAttribute("transferPage", transferPage);
            model.addAttribute("form", form);

            return "transfer";
        };
    }

    @GetMapping("city/{city}")
    public Callable<String> listTransfersByCity(
            @ModelAttribute("form") TransferFilterForm form,
            @PathVariable("city") String city,
            @PageableDefault(size = 30) Pageable page, Model model) {
        return () -> {
            Page<Transfer> transferPage = transferService.findAllByCity(city, page);
            model.addAttribute("transferPage", transferPage);
            model.addAttribute("city", city);
            model.addAttribute("form", form);
            return "transfer";
        };
    }

    @GetMapping("{id}")
    public Callable<String> transferDetails(
            @PathVariable("id") UUID id,
            @PageableDefault(size = 30) Pageable page, Model model) {
        return () -> {
            Page<TransferItem> itemPage = transferService.findAllItems(id, page);

            model.addAttribute("form", transferService.findById(id));
            model.addAttribute("itemPage", itemPage);

            return "transfer-detail";
        };
    }
}
