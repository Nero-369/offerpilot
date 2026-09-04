package com.offerpilot.api;

import com.offerpilot.domain.Offer;
import com.offerpilot.repository.OfferRepository;
import com.offerpilot.service.UploadedDocumentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/offers")
public class OfferController {
    private final OfferRepository repository;
    private final UploadedDocumentService uploadedDocuments;
    public OfferController(OfferRepository repository, UploadedDocumentService uploadedDocuments){this.repository=repository;this.uploadedDocuments=uploadedDocuments;}
    @GetMapping public List<Offer> list(){return repository.findAll();}
    @GetMapping("/{id}") public Offer get(@PathVariable UUID id){return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Offer not found"));}
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Offer create(@Valid @RequestBody CreateOfferRequest request){
        return repository.save(new Offer(request.company(),request.role(),request.city(),request.monthlySalary(),request.salaryMonths(),request.annualBonus(),request.annualHousingCost(),request.jobDescription()));
    }
    @PostMapping(value="/file-preview", consumes="multipart/form-data")
    public OfferFilePreview preview(@RequestPart("file") MultipartFile file){
        var parsed=uploadedDocuments.parse(file); String text=parsed.content();
        return new OfferFilePreview(parsed.filename(), parsed.content(), match(text,"(?:公司(?:名称)?|企业)[:：\\s]+([^\\n]{2,40})"),
                match(text,"(?:职位|岗位(?:名称)?)[:：\\s]+([^\\n]{2,40})"),
                match(text,"(?:工作地点|城市|地点)[:：\\s]+([^\\n]{2,30})"));
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable UUID id){repository.deleteById(id);}
    private String match(String text,String regex){Matcher m=Pattern.compile(regex,Pattern.CASE_INSENSITIVE).matcher(text);return m.find()?m.group(1).trim():null;}
    public record OfferFilePreview(String filename,String content,String company,String role,String city){}
}
