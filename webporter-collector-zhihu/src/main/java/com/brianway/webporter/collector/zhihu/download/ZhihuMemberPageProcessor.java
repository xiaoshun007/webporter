package com.brianway.webporter.collector.zhihu.download;

import com.brianway.webporter.collector.zhihu.ZhihuConfiguration;
import com.brianway.webporter.collector.zhihu.processor.MemberURLTokenGenerator;
import com.brianway.webporter.util.StringHelper;
import com.virjar.dungproxy.client.ippool.IpPoolHolder;
import com.virjar.dungproxy.client.ippool.config.DungProxyContext;
import com.virjar.dungproxy.client.ippool.strategy.impl.WhiteListProxyStrategy;
import com.virjar.dungproxy.webmagic6.DungProxyDownloader;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.scheduler.FileCacheQueueScheduler;

/**
 * Created by brian on 16/12/19.
 *
 * 爬取每个用户的详细信息
 * step 2: 运行该类爬去用户详细数据
 */
public class ZhihuMemberPageProcessor implements PageProcessor {

    private Site site = new ZhihuConfiguration().getSite();

    public void process(Page page) {
        page.putField(ZhihuPipeline.URL, page.getUrl());
        page.putField(ZhihuPipeline.RESPONSE, page.getRawText());
    }

    public Site getSite() {
        return site;
    }

    private static String generateMemberUrl(String urlToken) {
        final String URL_TEMPLATE = "https://www.zhihu.com/api/v4/members/%s";
        final String QUERY_PARAMS = "?include=locations%2Cemployments%2Cgender%2Ceducations%2Cbusiness%2Cvoteup_count%2Cthanked_Count%2Cfollower_count%2Cfollowing_count%2Ccover_url%2Cfollowing_topic_count%2Cfollowing_question_count%2Cfollowing_favlists_count%2Cfollowing_columns_count%2Canswer_count%2Carticles_count%2Cpins_count%2Cquestion_count%2Cfavorite_count%2Cfavorited_count%2Clogs_count%2Cmarked_answers_count%2Cmarked_answers_text%2Cmessage_thread_token%2Caccount_status%2Cis_active%2Cis_force_renamed%2Cis_bind_sina%2Csina_weibo_url%2Csina_weibo_name%2Cshow_sina_weibo%2Cis_blocking%2Cis_blocked%2Cmutual_followees_count%2Cvote_to_count%2Cvote_from_count%2Cthank_to_count%2Cthank_from_count%2Cthanked_count%2Cdescription%2Chosted_live_count%2Cparticipated_live_count%2Callow_message%2Cindustry_category%2Corg_name%2Corg_homepage%2Cbadge%5B%3F(type%3Dbest_answerer)%5D.topics";

        String encoded = StringHelper.urlEncode(urlToken);
        return String.format(URL_TEMPLATE, encoded) + QUERY_PARAMS;
    }

    /**
     * 根据提取的 url_token 逐个下载用户的完整信息
     * @param args 无须其他参数
     */
    public static void main(String[] args) {
        ZhihuConfiguration configuration = new ZhihuConfiguration();
        String pipelinePath = configuration.getMemberPath();

        //以下是通过代码配置规则的方案,如果不使用配置文件,则可以解开注释,通过代码的方式
        WhiteListProxyStrategy whiteListProxyStrategy = new WhiteListProxyStrategy();
        whiteListProxyStrategy.addAllHost("www.zhihu.com");

        // Step2 创建并定制代理规则
        DungProxyContext dungProxyContext = DungProxyContext.create().setNeedProxyStrategy(whiteListProxyStrategy).setPoolEnabled(false);

        // Step3 使用代理规则初始化默认IP池
        IpPoolHolder.init(dungProxyContext);


        Spider spider = Spider.create(new ZhihuMemberPageProcessor())
                .setScheduler(new FileCacheQueueScheduler(pipelinePath))
                .addPipeline(new ZhihuPipeline(pipelinePath))
                .setDownloader(new DungProxyDownloader())
                .thread(20);

        MemberURLTokenGenerator generator = new MemberURLTokenGenerator();
        generator.generateURLTokens().stream()
                .map(ZhihuMemberPageProcessor::generateMemberUrl)
                .forEach(spider::addUrl);

        spider.run();
    }

}
