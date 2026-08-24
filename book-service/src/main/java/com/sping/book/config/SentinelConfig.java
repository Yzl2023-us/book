package com.sping.book.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class SentinelConfig {

    @PostConstruct
    public void initFlowRules() {
        List<FlowRule> rules = new ArrayList<>();

        FlowRule publishRule = new FlowRule();
        publishRule.setResource("addBook");
        publishRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        publishRule.setCount(5);
        rules.add(publishRule);

        FlowRule searchRule = new FlowRule();
        searchRule.setResource("searchBook");
        searchRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        searchRule.setCount(20);
        rules.add(searchRule);

        FlowRuleManager.loadRules(rules);
    }
}