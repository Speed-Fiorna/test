package com.example.difytest.controller;

import com.example.difytest.dao.ResponseData;
import com.example.difytest.server.AgentServer;
import com.example.difytest.util.ResponseUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
@Slf4j
public class commonController {
    @Autowired
    AgentServer agentServer;
    @ResponseBody
    @RequestMapping("/difyTest")
    public Object saveUserMsg(){
        log.info("start dify====");
        ResponseData responseData = agentServer.difyAgent();
        Map<String, String> res = new HashMap<>();
        res.put("data", responseData.getData().getOutputs().getFinalOutput());
        log.info("end dify====");
        return ResponseUtil.success(res);
    }
}
