package parkingos.com.bolink.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import parkingos.com.bolink.dao.mybatis.mapper.BolinkDataMapper;
import parkingos.com.bolink.dao.mybatis.mapper.OrderMapper;
import parkingos.com.bolink.dao.mybatis.mapper.OrderTbMapper;
import parkingos.com.bolink.dao.spring.CommonDao;
import parkingos.com.bolink.enums.BolinkAccountTypeEnum;
import parkingos.com.bolink.models.OrderTb;
import parkingos.com.bolink.orderserver.OrderServer;
import parkingos.com.bolink.service.CityOrderAnlysisService;
import parkingos.com.bolink.service.CommonService;
import parkingos.com.bolink.service.OrderService;
import parkingos.com.bolink.service.SupperSearchService;
import parkingos.com.bolink.utils.Check;
import parkingos.com.bolink.utils.StringUtils;
import parkingos.com.bolink.utils.TimeTools;

import java.util.*;

@Service
public class CityOrderanlysisServiceImpl implements CityOrderAnlysisService {

    Logger logger = LoggerFactory.getLogger(CityOrderanlysisServiceImpl.class);

    @Autowired
    private BolinkDataMapper bolinkDataMapper;
    @Autowired
    OrderMapper orderMapper;
    @Autowired
    CommonService commonService;
    @Autowired
    OrderServer orderServer;

    @Override
    public JSONObject selectResultByConditions(Map<String, String> reqmap) {

        String str = "{\"page\":1,\"rows\":[]}";
        JSONObject result = JSONObject.parseObject(str);

        Long groupid = Long.parseLong(reqmap.get("groupid"));



        String date = StringUtils.decodeUTF8(StringUtils.decodeUTF8(reqmap.get("date")));

        Long btime = null;
        Long etime = null;
        int isToday = 1;
        if(date==null||"".equals(date)){
//            btime = TimeTools.getToDayBeginTime()-86400*9;
            btime = TimeTools.getToDayBeginTime();
            etime =TimeTools.getToDayBeginTime()+86399;
        }else {
            String[] dateArr = date.split("至");
            String start =dateArr[0];
            String end = dateArr[1];
            btime = TimeTools.getLongMilliSecondFrom_HHMMDDHHmmss(start);
            etime = TimeTools.getLongMilliSecondFrom_HHMMDDHHmmss(end);
        }

        if(btime<TimeTools.getToDayBeginTime()){
            isToday=0;
        }

        Double act_money = 0.0d;//所有的收入金额
        Double cash_money=0.0d;//所有的现金结算
        Double ele_money=0.0d;//所有的电子支付金额
        Double ele_total=0.0d;//所有的电子结算金额  支付-支出
        Double outMoney = 0.0d;//所有的支出金额
        Double free_money=0.0d;//所有的免费金额

        List<Map<String,String>> backList = new ArrayList<>();


        if(isToday==1){

            List<Long> list = orderMapper.getComlistByGroupid(groupid);
            if(list==null||list.isEmpty()){
                return result;
            }
            reqmap.put("comList",JSON.toJSONString(list));

            Long cityid=orderMapper.getCityIdByGroupId(groupid);
            String tableName = "order_tb_new";
            if(cityid!=null&&cityid>-1){
                reqmap.put("cityId",cityid+"");
                tableName +="_"+cityid%100;
            }
            reqmap.put("tableName",tableName);
            reqmap.put("end_time","between");

            reqmap.put("end_time_start",btime+"");
            reqmap.put("end_time_end",etime+"");

            List<Map<String,String>> cashList = orderServer.selectCityDayAnlysis(reqmap);

            logger.info("===>>>>cashList:"+cashList);

            String bolinkTableName = commonService.getTableNameByGroupId(groupid,1);
            List<Map<String, Object>> inTransactions = new ArrayList<>();
            List<Map<String, Object>> outTransactions = new ArrayList<>();

            inTransactions = bolinkDataMapper.getDailyTransactionsByGroupId(bolinkTableName, btime, etime, groupid);

            bolinkTableName = commonService.getTableNameByGroupId(groupid,2);

            outTransactions= bolinkDataMapper.getDailyOutTransactionsByGroupId(bolinkTableName, btime, etime, groupid);


            logger.info("===>>>>inTransactions:"+inTransactions);
            logger.info("===>>>>outTransactions:"+outTransactions);
            List<String> dateList = new ArrayList<>();
            if(cashList!=null&&cashList.size()>0){
                for(Map<String,String> map:cashList){
                    String time = map.get("time");
                    dateList.add(time);
                }
            }

            if(inTransactions!=null&&inTransactions.size()>0){
                for(Map<String,Object> map:inTransactions){
                    String time = map.get("time")+"";
                    if(!dateList.contains(time)) {
                        dateList.add(time);
                    }
                }
            }

            if(outTransactions!=null&&outTransactions.size()>0){
                for(Map<String,Object> map:outTransactions){
                    String time = map.get("time")+"";
                    if(!dateList.contains(time)) {
                        dateList.add(time);
                    }
                }
            }
            Collections.sort(dateList);

            if(dateList!=null&&dateList.size()>0){
                for (String time:dateList){
                    //根据userId获取名称
                    Map<String,String> resultMap = new HashMap<>();
                    resultMap.put("time",time);
                    resultMap.put("cash_pay",StringUtils.formatDouble(0.0d)+"");
                    resultMap.put("ele_pay",StringUtils.formatDouble(0.0d)+"");
                    resultMap.put("act_total",StringUtils.formatDouble(0.0d)+"");
                    resultMap.put("out_money",StringUtils.formatDouble(0.0d)+"");
                    resultMap.put("free_pay",StringUtils.formatDouble(0.0d)+"");

                    Double actReceive = 0.0d;
                    Double cashTotal = 0.0d;
                    Double eleTotal = 0.0d;
                    Double outTotal = 0.0d;
                    if(cashList!=null&&cashList.size()>0){
                        for(Map<String,String>cashMap:cashList){
                            if(cashMap.get("time").equals(time)){
                                cashTotal+=StringUtils.formatDouble(cashMap.get("cash_pay"));
                                actReceive+=StringUtils.formatDouble(cashMap.get("cash_pay"));
                                cash_money+=StringUtils.formatDouble(cashMap.get("cash_pay"));
                                free_money+=StringUtils.formatDouble(cashMap.get("free_pay"));
                                resultMap.put("free_pay",cashMap.get("free_pay"));
                            }
                        }
                    }

                    if (inTransactions!=null&&inTransactions.size()>0){
                        for(Map<String,Object> inMap:inTransactions){
                            String inTime = inMap.get("time")+"";
                            if(inTime.equals(time)){
                                int type = (int) inMap.get("type");
                                if(type== BolinkAccountTypeEnum.CASH_PREPAY.type) {
                                    cashTotal+=StringUtils.formatDouble(inMap.get("pay_money"));
                                    actReceive += StringUtils.formatDouble(inMap.get("pay_money"));
                                    cash_money += StringUtils.formatDouble(inMap.get("pay_money"));
                                }
                                else {
                                    actReceive += StringUtils.formatDouble(inMap.get("pay_money"));
                                    eleTotal+=StringUtils.formatDouble(inMap.get("pay_money"));
                                    ele_money+=StringUtils.formatDouble(inMap.get("pay_money"));
                                }
                            }
                        }
                    }

                    if (outTransactions!=null&&outTransactions.size()>0){
                        for(Map<String,Object> outMap:outTransactions){
                            String outTime = outMap.get("time")+"";
                            if(outTime.equals(time)){
                                outMoney+=StringUtils.formatDouble(outMap.get("pay_money"));
                                outTotal += StringUtils.formatDouble(outMap.get("pay_money"));
                            }
                        }
                    }
                    act_money+=StringUtils.formatDouble(actReceive);
                    resultMap.put("ele_total",StringUtils.formatDouble(eleTotal-outTotal)+"");
                    resultMap.put("act_total",StringUtils.formatDouble(actReceive)+"");
                    resultMap.put("cash_pay",StringUtils.formatDouble(cashTotal)+"");
                    resultMap.put("ele_pay",StringUtils.formatDouble(eleTotal)+"");
                    resultMap.put("out_money",StringUtils.formatDouble(outTotal)+"");
                    backList.add(resultMap);
                }
            }

        }else{
            List<Map<String,Object>> parkAnlys = bolinkDataMapper.getGroupDaylyAnly(groupid,btime,etime);
            logger.info("===>>>>>parkAnlys:"+parkAnlys);

            if(parkAnlys==null||parkAnlys.isEmpty()){
                return result;
            }


            for(Map<String,Object> map:parkAnlys){
                String dateStr = map.get("pay_time_day_str")+"";

                Map<String,String> resultMap = new HashMap<>();

                cash_money+=StringUtils.formatDouble(map.get("cash_pay"));
                ele_money+=StringUtils.formatDouble(map.get("ele_pay"));
                free_money+=StringUtils.formatDouble(map.get("reduce"));
                act_money+=StringUtils.formatDouble(map.get("act_total"));
                outMoney+=StringUtils.formatDouble(map.get("out_total"));
                ele_total+=StringUtils.formatDouble(map.get("ele_total"));


                resultMap.put("time",dateStr);
                resultMap.put("ele_total",StringUtils.formatDouble(map.get("ele_total"))+"");
                resultMap.put("act_total",StringUtils.formatDouble(map.get("act_total"))+"");
                resultMap.put("cash_pay",StringUtils.formatDouble(map.get("cash_pay"))+"");
                resultMap.put("ele_pay",StringUtils.formatDouble(map.get("ele_pay"))+"");
                resultMap.put("out_money",StringUtils.formatDouble(map.get("out_total"))+"");
                resultMap.put("free_pay",StringUtils.formatDouble(map.get("reduce"))+"");
                backList.add(resultMap);
            }
        }




        Map<String,String> resultMap = new HashMap<>();
        resultMap.put("time","合计");
        resultMap.put("ele_total",StringUtils.formatDouble(ele_total)+"");
        resultMap.put("cash_pay",StringUtils.formatDouble(cash_money)+"");
        resultMap.put("ele_pay",StringUtils.formatDouble(ele_money)+"");
        resultMap.put("act_total",StringUtils.formatDouble(act_money)+"");
        resultMap.put("out_money",StringUtils.formatDouble(outMoney)+"");
        resultMap.put("free_pay",StringUtils.formatDouble(free_money)+"");
        backList.add(resultMap);





        result.put("rows",JSON.toJSON(backList));
        return result;
    }

    @Override
    public List<List<Object>> exportExcel(Map<String, String> reqParameterMap) {

        //删除分页条件  查询该条件下所有  不然为一页数据
        reqParameterMap.remove("orderby");

        //获得要导出的结果
        JSONObject result = selectResultByConditions(reqParameterMap);

        List<Object> resList = JSON.parseArray(result.get("rows").toString());

        logger.error("=========>>>>>>.导出订单" + resList.size());
        List<List<Object>> bodyList = new ArrayList<List<Object>>();
        if (resList != null && resList.size() > 0) {
            for (Object object : resList) {
                Map<String,Object> map = (Map)object;
                List<Object> values = new ArrayList<Object>();
                values.add(map.get("time"));
//                values.add(map.get("comid"));
//                values.add(map.get("amount_receivable"));
                values.add(map.get("cash_pay"));
                values.add(map.get("ele_pay"));
                values.add(map.get("act_total"));
                values.add(map.get("out_money"));
                values.add(map.get("ele_total"));
                values.add(map.get("free_pay"));
                bodyList.add(values);
            }
        }
        return bodyList;
    }
}
