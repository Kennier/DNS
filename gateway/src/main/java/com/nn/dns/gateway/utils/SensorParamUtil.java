package com.nn.dns.gateway.utils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class SensorParamUtil {

    public static Map<String,Object> getSensorParam(String eventName, Map properties){
        HashMap<String, Object> sensorParamMap = new HashMap<>();
        sensorParamMap.put("eventName",eventName);
        sensorParamMap.put("properties",properties);
        sensorParamMap.put("isLoginId",true);
        sensorParamMap.put("distinctId",eventName+"_distinctId");
        sensorParamMap.put("time",new Date());
        return sensorParamMap;
    }

}
