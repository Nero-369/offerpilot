package com.offerpilot.api;

import com.offerpilot.domain.Offer;
import com.offerpilot.repository.OfferRepository;
import com.offerpilot.service.KnowledgeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/chat")
public class AdvisorChatController {
    private final OfferRepository offers;
    private final CityDataController cities;
    private final KnowledgeService knowledge;
    private final ChatClient chatClient;
    private final boolean aiEnabled;
    public AdvisorChatController(OfferRepository offers,CityDataController cities,KnowledgeService knowledge,ObjectProvider<ChatClient.Builder> builders,@Value("${offerpilot.ai.enabled:false}") boolean aiEnabled){
        this.offers=offers;this.cities=cities;this.knowledge=knowledge;ChatClient.Builder builder=builders.getIfAvailable();this.chatClient=builder==null?null:builder.build();this.aiEnabled=aiEnabled&&chatClient!=null;
    }
    @PostMapping public ChatResponse ask(@Valid @RequestBody ChatRequest request){
        long started=System.nanoTime();Offer offer=request.offerId()==null?null:offers.findById(request.offerId()).orElse(null);String city=offer==null?detectCity(request.question()):offer.getCity();
        List<CityDataController.CityProfile> profiles=cities.list().stream().filter(c->city==null||c.city().equals(city)||request.question().contains(c.city())).toList();
        KnowledgeService.SearchResult retrieval=null;if(knowledge.embeddingConfigured())try{retrieval=knowledge.search(request.question(),city,5);}catch(RuntimeException ignored){}
        List<KnowledgeService.RetrievedChunk> chunks=retrieval==null?List.of():retrieval.chunks();List<Source> sources=new ArrayList<>();
        chunks.forEach(c->sources.add(new Source(c.title(),c.sourceUrl(),c.city(),c.similarity())));
        profiles.stream().flatMap(c->c.sources().stream()).forEach(s->{if(sources.stream().noneMatch(x->x.url().equals(s.url())))sources.add(new Source(s.title(),s.url(),city,1));});
        long beforeLlm=System.nanoTime();
        if(aiEnabled)try{
            String answer=chatClient.prompt().system("你是OfferPilot秋招决策Agent。严格使用RAG检索片段、城市官方资料和Offer回答。每项结论标明事实、估算或建议；资料不足时明确说不知道并列出需向HR确认的问题。引用资料使用[1][2]编号，不得虚构数字或来源，结尾给出可执行下一步。")
                .user("用户问题："+request.question()+"\n\n"+buildContext(offer,profiles,chunks)).call().content();long finished=System.nanoTime();
            return new ChatResponse(answer,sources,"QWEN_RAG",trace(retrieval,(finished-beforeLlm)/1_000_000,(finished-started)/1_000_000));
        }catch(RuntimeException e){return fallback(request,offer,sources,retrieval,started,"Qwen调用失败，已降级："+safeMessage(e));}
        return fallback(request,offer,sources,retrieval,started,"尚未启用Qwen API");
    }
    private ChatResponse fallback(ChatRequest request,Offer offer,List<Source> sources,KnowledgeService.SearchResult r,long started,String reason){
        String answer=reason+"。"+(offer==null?"请选择一个Offer以加入岗位上下文。":offer.getCompany()+"的"+offer.getRole()+"位于"+offer.getCity()+"，月薪"+offer.getMonthlySalary()+"元、"+offer.getSalaryMonths()+"薪。")+(r==null?"当前未执行向量检索。":"已从pgvector检索到"+r.chunks().size()+"个知识切片，来源在下方；为避免误导，本次不生成开放式结论。问题："+request.question());
        return new ChatResponse(answer,sources,"SAFE_FALLBACK",trace(r,0,(System.nanoTime()-started)/1_000_000));
    }
    private Trace trace(KnowledgeService.SearchResult r,long llm,long total){return new Trace(r==null?0:r.embeddingMs(),r==null?0:r.vectorSearchMs(),llm,r==null?0:r.chunks().size(),r==null?"未执行":r.embeddingModel(),"pgvector cosine/HNSW",total);}
    private String buildContext(Offer offer,List<CityDataController.CityProfile> profiles,List<KnowledgeService.RetrievedChunk> chunks){StringBuilder b=new StringBuilder("[Offer]\n").append(offer==null?"未指定":offer.getCompany()+" / "+offer.getRole()+" / "+offer.getCity()+" / 月薪"+offer.getMonthlySalary()+" / "+offer.getSalaryMonths()+"薪 / JD:"+offer.getJobDescription()).append("\n\n[城市官方资料]\n").append(profiles).append("\n\n[RAG检索片段]\n");for(int i=0;i<chunks.size();i++){var c=chunks.get(i);b.append('[').append(i+1).append("] ").append(c.title()).append(" | 相似度 ").append(c.similarity()).append(" | ").append(c.sourceUrl()).append('\n').append(c.content()).append("\n\n");}return b.toString();}
    private String detectCity(String q){return cities.list().stream().map(CityDataController.CityProfile::city).filter(q::contains).findFirst().orElse(null);}
    private String safeMessage(Throwable e){String m=e.getMessage();return m==null?e.getClass().getSimpleName():m.substring(0,Math.min(m.length(),180));}
    public record ChatRequest(@NotBlank @Size(max=2000) String question,UUID offerId){}
    public record Source(String title,String url,String city,double similarity){}
    public record Trace(long embeddingMs,long vectorSearchMs,long llmMs,int retrievedChunks,String embeddingModel,String index,long totalMs){}
    public record ChatResponse(String answer,List<Source> sources,String mode,Trace trace){}
}
