package com.offerpilot.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/v1/cities")
public class CityDataController {
    @GetMapping
    public List<CityProfile> list() {
        String housingSource = "https://www.stats.gov.cn/sj/zxfbhjd/202602/t20260213_1962617.html";
        return List.of(
            new CityProfile("北京", "7270-36348元/月", "2026年7月起五险缴费基数上下限", 97.6, "2026年1月新建商品住宅价格同比指数", List.of(
                new Source("北京市人社局：2026年度社保缴费工资基数", "https://rsj.beijing.gov.cn/xxgk/2024zcjd/202608/t20260821_4831476.html", "2026-08-21", "官方"),
                new Source("国家统计局：70城住宅销售价格指数", housingSource, "2026-02-13", "官方"))),
            new CityProfile("上海", "按职工2025年月平均工资申报", "2026社保年度申报口径；最终上下限以官方公布为准", 104.2, "2026年1月新建商品住宅价格同比指数", List.of(
                new Source("上海市政府：2026社保年度缴费工资申报", "https://www.shanghai.gov.cn/gwk/search/content/ca6685bd116e4ff29a32303c6405851b", "2026-05", "官方"),
                new Source("国家统计局：70城住宅销售价格指数", housingSource, "2026-02-13", "官方"))),
            new CityProfile("杭州", "按浙江省年度社保口径核定", "职工工资低于或高于年度限额时按省级上下限执行", 102.4, "2026年1月新建商品住宅价格同比指数", List.of(
                new Source("浙江省政务资料：社保缴费基数核定规则", "https://zjjcmspublic.oss-cn-hangzhou-zwynet-d01-a.internet.cloud.zj.gov.cn/jcms_files/jcms1/web2758/site/attach/0/76d7f66fc1d744f1b4bad76f63b48727.pdf", "2026年查询", "官方资料"),
                new Source("国家统计局：70城住宅销售价格指数", housingSource, "2026-02-13", "官方"))),
            new CityProfile("深圳", "6727-33633元/月（职工医保）", "2026年1月1日至12月31日职工医保和生育保险口径", 95.1, "2026年1月新建商品住宅价格同比指数", List.of(
                new Source("深圳市政府：2026年职工医保缴费基数", "https://www.sz.gov.cn/hdjlpt/detail?pid=3336522&via=pc", "2026-01-07", "官方"),
                new Source("国家统计局：70城住宅销售价格指数", housingSource, "2026-02-13", "官方")))
        );
    }
    public record CityProfile(String city, String socialBase, String socialNote, double newHomePriceIndex, String housingNote, List<Source> sources) {}
    public record Source(String title, String url, String publishedAt, String level) {}
}
