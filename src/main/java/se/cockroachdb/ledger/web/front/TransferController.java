package se.cockroachdb.ledger.web.front;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import se.cockroachdb.ledger.domain.Transfer;
import se.cockroachdb.ledger.domain.TransferItem;
import se.cockroachdb.ledger.service.transfer.TransferService;

@Controller
@RequestMapping("/transfer")
public class TransferController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private TransferService transferService;

    @GetMapping
    public Callable<String> listTransfers(
            @PageableDefault(size = 30) Pageable page, Model model) {
        return () -> {
            Page<Transfer> transferPage = transferService.findAll(page);
            model.addAttribute("transferPage", transferPage);
            return "transfer";
        };
    }

    @GetMapping("city/{city}")
    public Callable<String> listTransfersByCity(
            @PathVariable("city") String city,
            @PageableDefault(size = 30) Pageable page, Model model) {
        return () -> {
            Page<Transfer> transferPage = transferService.findAllByCity(city, page);
            model.addAttribute("transferPage", transferPage);
            model.addAttribute("city", city);
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
