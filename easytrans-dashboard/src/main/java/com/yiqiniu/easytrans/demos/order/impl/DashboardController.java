package com.yiqiniu.easytrans.demos.order.impl;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.yiqiniu.easytrans.monitor.MonitorConsumerFactory;
import com.yiqiniu.easytrans.monitor.StringCodecMonitor;
import com.yiqiniu.easytrans.monitor.TransactionLogMonitor;

@Controller
public class DashboardController {

    private static final String YYYY_M_MDD_H_HMMSS = "yyyyMMddHHmmssSSS";
    private static final String VIEW_INDEX = "index";
    private static final String VIEW_STRING_TO_ID_MAP = "string2IdMap";
    private static final String VIEW_UNFINISHED_LOGS = "unfinishedLogs";
    
    @Autowired
    private MonitorConsumerFactory factory;
    
    @RequestMapping(path = "/",  method = RequestMethod.GET)
    public String index() {
        return VIEW_INDEX;
    }


    @RequestMapping(path = "/string2IdMap",  method = RequestMethod.GET)
//    @ResponseBody
    public Object getString2IdMap(@RequestParam(required=false) String appId, Model model) {
        
        model.addAttribute("appId", appId);
        
        if(appId != null) {
            Object string2IdMap = factory.getRemoteProxy(appId, StringCodecMonitor.class).getString2IdMap();
            model.addAttribute("result", string2IdMap);
        }
        return VIEW_STRING_TO_ID_MAP;
    }

    @RequestMapping(path = "/unfinishedLogs", method = RequestMethod.GET)
    public String getUnfinishedLog(@RequestParam(required=false) String appId, @RequestParam(required=false, defaultValue="10")  Integer pageSize , @RequestParam(required=false) @DateTimeFormat(pattern=YYYY_M_MDD_H_HMMSS)  Date timestamp, Model model) {
        
        model.addAttribute("appId", appId);
        model.addAttribute("pageSize", pageSize);
        if(timestamp != null) {
            model.addAttribute("timestamp", new SimpleDateFormat(YYYY_M_MDD_H_HMMSS).format(timestamp));
        }
        
        
        if(appId != null) {
            Object unfinishedLogs = factory.getRemoteProxy(appId, TransactionLogMonitor.class).getUnfinishedLogs(pageSize, timestamp==null?null:timestamp.getTime());
            model.addAttribute("result", unfinishedLogs);
        }
        return VIEW_UNFINISHED_LOGS;
    }
    
    @RequestMapping(path = "/consistentGuardian", method = RequestMethod.GET,produces="application/json")
    @ResponseBody
    public String getUnfinishedLog(@RequestParam String appId, @RequestParam  String busCode , @RequestParam long trxId) {
        factory.getRemoteProxy(appId, TransactionLogMonitor.class).consistentProcess(busCode, trxId);
        return "Success";
    }
    
    
//    public static void main(String[] args) {
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
//        try {
//            Date format = sdf.parse("20190405082912");
//            System.out.println(format);
//        } catch (ParseException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//    }

}
