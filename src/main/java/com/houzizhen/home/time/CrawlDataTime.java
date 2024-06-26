package com.houzizhen.home.time;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.houzizhen.home.model.LotteryResult;
import com.houzizhen.home.service.LotteryResultService;
import com.houzizhen.home.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class CrawlDataTime {
    private final String KH_HQ_PATTERN = "var khHq = \\[(.*?),\\r\\n\\s+]";
    private final String KH_LQ_PATTERN = "var khLq = '(.*?)'";

    @Value("${ssq.url}")
    private String ssqUrl;

    @Autowired
    private LotteryResultService lotteryResultService;

    /**
     * 定时执行任务，用于更新彩票结果数据。
     * 该方法通过比较数据库中的最大期号和从接口获取的最新期号，如果发现有新的期号数据，则下载该期号的详细数据，并更新到数据库中。
     */
    @Scheduled(cron = "0 0/10 * * * ?")
    public void getLotteryResult() throws IOException {
        log.info("定时任务正在运行, 当前时间：{}", new Date());
        // 获取数据库的最大数据库期号
        LotteryResult lotteryResult = lotteryResultService.getLastLotteryResult();
        int issue = lotteryResult.getIssue();
        // 发送http请求,获取最近的数据期号
        String s = HttpClientUtil.sendHttpGetRequest(ssqUrl);
        JSONObject jsonObject = JSONObject.parseObject(s);
        if (jsonObject.getInteger("state") == 0) {
            JSONArray jsonArray = jsonObject.getJSONArray("result");
            for (int i = jsonArray.size() - 1; i >= 0; i--) {
                JSONObject jsonObject1 = jsonArray.getJSONObject(i);
                String code = jsonObject1.getString("code");
                int newIssue = Integer.parseInt(code);
                if (newIssue <= issue) {
                    log.info("当前期号小于等于数据库中的期号一致，无需更新");
                } else {
                    log.info("当前期号与数据库中的期号不一致，开始更新");
                    String detailsLink = jsonObject1.getString("detailsLink");
                    String string = HttpClientUtil.sendHttpGetRequest("https://www.cwl.gov.cn" + detailsLink);
                    LotteryResult lotteryResult1 = new LotteryResult();
                    // 匹配khHq
                    Pattern patternHq = Pattern.compile(KH_HQ_PATTERN, Pattern.DOTALL);
                    Matcher matcherHq = patternHq.matcher(string);
                    if (matcherHq.find()) {
                        String khHqValue = matcherHq.group(1).replace("\\'", "'").replaceAll("\\s+", "");
                        log.info("khHq: {}", khHqValue);
                        khHqValue = khHqValue.replaceAll("'", "");
                        String[] split = khHqValue.split(",");
                        List<String> list = Arrays.asList(split);
                        ArrayList<Integer> integerArrayList = new ArrayList<>();
                        for (String s1 : list) {
                            int i1 = Integer.parseInt(s1);
                            integerArrayList.add(i1);
                        }
                        if (integerArrayList.size() == 6) {
                            lotteryResult1.setIssue(newIssue);
                            lotteryResult1.setRed1(integerArrayList.get(0));
                            lotteryResult1.setRed2(integerArrayList.get(1));
                            lotteryResult1.setRed3(integerArrayList.get(2));
                            lotteryResult1.setRed4(integerArrayList.get(3));
                            lotteryResult1.setRed5(integerArrayList.get(4));
                            lotteryResult1.setRed6(integerArrayList.get(5));

                        }
                    }
                    // 匹配khLq
                    Pattern patternLq = Pattern.compile(KH_LQ_PATTERN, Pattern.DOTALL);
                    Matcher matcherLq = patternLq.matcher(string);
                    if (matcherLq.find()) {
                        String khLqValue = matcherLq.group(1);
                        log.info("khLq: {}", khLqValue);
                        int i1 = Integer.parseInt(khLqValue);
                        lotteryResult1.setBlue(i1);
                    }
                    lotteryResultService.addLotteryResult(lotteryResult1);
                }
            }
        } else {
            log.error("接口请求异常！");
        }
    }
}