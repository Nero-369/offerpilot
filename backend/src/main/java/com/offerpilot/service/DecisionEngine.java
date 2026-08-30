package com.offerpilot.service;

import com.offerpilot.api.DecisionReport;
import com.offerpilot.domain.Offer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class DecisionEngine {
    private final ChatClient chatClient;
    private final boolean aiEnabled;
    public DecisionEngine(ObjectProvider<ChatClient.Builder> builderProvider, @Value("${offerpilot.ai.enabled:false}") boolean aiEnabled) {
        ChatClient.Builder builder = builderProvider.getIfAvailable();
        this.chatClient = builder == null ? null : builder.build();
        this.aiEnabled = aiEnabled && chatClient != null;
    }
    public AiAssessment assess(Offer offer) {
        if (!aiEnabled) return demoAssessment(offer);
        try {
            AiAssessment result = chatClient.prompt()
                .system("你是OfferPilot决策Agent。只根据提供的岗位数据进行审慎分析，不编造企业事实。输出结构化结果。")
                .user("公司：%s；岗位：%s；城市：%s；JD：%s".formatted(offer.getCompany(), offer.getRole(), offer.getCity(), offer.getJobDescription()))
                .call().entity(AiAssessment.class);
            return result == null ? demoAssessment(offer) : result;
        } catch (RuntimeException ex) {
            return demoAssessment(offer);
        }
    }
    private AiAssessment demoAssessment(Offer offer) {
        int match = offer.getJobDescription() != null && offer.getJobDescription().toLowerCase().contains("spring") ? 88 : 78;
        return new AiAssessment(match, offer.getRole().contains("AI") ? 92 : 82, 76,
            "该 Offer 的现金流与岗位方向整体均衡，建议结合团队稳定性和实际工作内容继续核实。",
            List.of("岗位方向与 Java/Spring 技术栈相关", "薪资结构清晰，可进行确定性测算"),
            List.of("年终奖兑现率需要向招聘方确认", "生活成本数据为估算值"));
    }
    public record AiAssessment(int jobMatchScore, int growthScore, int stabilityScore, String recommendation, List<String> strengths, List<String> risks) {}
}
