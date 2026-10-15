package com.acme.warehouse.web;

import com.acme.warehouse.model.Transfer;
import com.acme.warehouse.service.TransferService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transfers")
public class TransferController {

    public record TransferRequest(@NotBlank String sku, @NotBlank String fromBin, @NotBlank String toBin,
                                  @Min(1) int quantity) {
    }

    private final TransferService transfers;

    public TransferController(TransferService transfers) {
        this.transfers = transfers;
    }

    @PostMapping
    public Map<String, Long> create(@Valid @RequestBody TransferRequest request) {
        long id = transfers.reserveStock(request.sku(), request.fromBin(), request.toBin(), request.quantity());
        return Map.of("id", id);
    }

    @GetMapping("/{id}")
    public Transfer get(@PathVariable long id) {
        return transfers.findTransfer(id);
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<Void> complete(@PathVariable long id) {
        transfers.completeTransfer(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable long id) {
        transfers.cancelTransfer(id);
        return ResponseEntity.noContent().build();
    }
}
