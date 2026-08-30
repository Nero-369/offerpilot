package com.offerpilot.api;

import com.offerpilot.domain.Offer;
import com.offerpilot.repository.OfferRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/offers")
public class OfferController {
    private final OfferRepository repository;
    public OfferController(OfferRepository repository){this.repository=repository;}
    @GetMapping public List<Offer> list(){return repository.findAll();}
    @GetMapping("/{id}") public Offer get(@PathVariable UUID id){return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Offer not found"));}
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Offer create(@Valid @RequestBody CreateOfferRequest request){
        return repository.save(new Offer(request.company(),request.role(),request.city(),request.monthlySalary(),request.salaryMonths(),request.annualBonus(),request.annualHousingCost(),request.jobDescription()));
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable UUID id){repository.deleteById(id);}
}
