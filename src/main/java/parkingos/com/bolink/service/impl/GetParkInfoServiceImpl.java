package parkingos.com.bolink.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import parkingos.com.bolink.dao.mybatis.mapper.OrderMapper;
import parkingos.com.bolink.dao.mybatis.mapper.ParkInfoMapper;
import parkingos.com.bolink.orderserver.OrderServer;
import parkingos.com.bolink.service.CityOrderAnlysisService;
import parkingos.com.bolink.service.GetParkInfoService;
import parkingos.com.bolink.service.ParkOrderAnlysisService;
import parkingos.com.bolink.utils.TimeTools;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class GetParkInfoServiceImpl implements GetParkInfoService {

    Logger logger = LoggerFactory.getLogger(GetParkInfoServiceImpl.class);

    @Autowired
    private ParkInfoMapper parkInfoMapper;
    @Autowired
    private ParkOrderAnlysisService parkOrderanlysisService;
    @Autowired
    private CityOrderAnlysisService cityOrderanlysisService;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderServer orderServer;

    DecimalFormat af1 = new DecimalFormat("0");
    @Override
    public String getInfo(int groupid) {
        Long cityid = orderMapper.getCityIdByGroupId(Long.parseLong(groupid+""));
        String tableName = "order_tb_new";
        if (cityid!=null&&cityid>-1){
            tableName = "order_tb_new_"+cityid%100;
        }
        HashMap<String, Object> retMap = new HashMap<String, Object>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        long tday = calendar.getTimeInMillis() / 1000;
        //获取进场和离场数据

        List<Map<String, String>> entryCarList=orderServer.getEntryCar(tday, Long.parseLong(groupid + ""),tableName);
        List<Map<String, String>> outCarList=orderServer.getExitCar(tday, Long.parseLong(groupid + ""),tableName);

        int parkingtotal = parkInfoMapper.getBerthTotal(groupid);
        //获取今日电子支付，现金支付，减免金额的统计
        Map<String,String> parammap = new HashMap<String,String>();
        parammap.put("groupid",groupid+"");
        String todyyymmdd=TimeTools.getDate_YY_MM_DD();
        parammap.put("date",todyyymmdd+" 00:00:00至"+todyyymmdd+"23:59:59");
        JSONObject retjson=cityOrderanlysisService.selectResultByConditions(parammap);
        JSONArray retarry = retjson.getJSONArray("rows");
        Double cashPay=0d;
        Double electronicPay=0d;
        Double freePay=0d;
        if(retarry.size()>0){
            JSONObject object =(JSONObject)retarry.get(retarry.size()-1);
            if(object.getString("cash_pay")!=null&&!"".equals(object.getString("cash_pay"))) {
                cashPay = Double.parseDouble(object.getString("cash_pay"));
            }
            if(object.getString("electronic_pay")!=null&&!"".equals(object.getString("electronic_pay"))) {
                electronicPay = Double.parseDouble(object.getString("electronic_pay"));
            }
            if(object.getString("free_pay")!=null&&!"".equals(object.getString("free_pay"))) {
                freePay = Double.parseDouble(object.getString("free_pay"));
            }
        };
        HashMap<String, Object> cashPaymap = new HashMap<String, Object>();
        HashMap<String, Object> electronicPaymap = new HashMap<String, Object>();
        HashMap<String, Object> reduceamap = new HashMap<String, Object>();
        HashMap<String, Object> totalIncomemap = new HashMap<String, Object>();
        totalIncomemap.put("elePay", af1.format(electronicPay));
        totalIncomemap.put("cashPay", af1.format(cashPay));
        totalIncomemap.put("freePay", af1.format(freePay));
        cashPaymap.put("name", "电子");
        cashPaymap.put("value", af1.format(electronicPay));
        electronicPaymap.put("name", "现金");
        electronicPaymap.put("value", af1.format(cashPay));
        reduceamap.put("name", "减免");
        reduceamap.put("value", af1.format(freePay));
        List<HashMap<String, Object>> totalIncomPie = new ArrayList<HashMap<String, Object>>();
        totalIncomPie.add(cashPaymap);
        totalIncomPie.add(electronicPaymap);
        totalIncomPie.add(reduceamap);
        //获取收费排行数据
        List<Map<String, String>> parkRankList = orderServer.getParkRank(tday, groupid,tableName);
        //获取车辆进场，离场，在场的数量统计
        int inCars =orderServer.getEntryCount(tday, groupid,tableName);
        int outCars =orderServer.getExitCount(tday, groupid,tableName);
        int inPark =orderServer.getInparkCount(tday, groupid,tableName);
        HashMap<String, Object> countMap = new HashMap<String, Object>();
        countMap.put("inCars", inCars);
        countMap.put("outCars", outCars);
        countMap.put("inPark", inPark);
        //计算泊位使用率
        List<HashMap<String,Object>> parkidList = parkInfoMapper.getParkIdByGroupId(groupid);
        List<HashMap<String,Object>> ss =new ArrayList<HashMap<String,Object>>();
        if(parkidList!=null&&parkidList.size()>0) {
            ss = parkInfoMapper.getBerthPercent(parkidList, tday);
        }
        if(ss!=null&&ss.size()>0) {
            for (HashMap<String, Object> map : ss) {
                Long asum = (Long) map.get("asum");
                Long usum = (Long) map.get("usum");
                DecimalFormat df = new DecimalFormat("#");
                double pecent = 0d;
                if (usum != 0) {
                    pecent = (float) usum * 100 / asum;
                }
                SimpleDateFormat sdf = new SimpleDateFormat("HH");
                long time = (long) map.get("create_time");
                Date date = new Date(time * 1000);
                map.put("time", sdf.format(date));
                map.put("percent", df.format(pecent));
            }
        }
        DecimalFormat df = new DecimalFormat("#");
        double parkOnpecent=0d;
        if(inPark !=0){
            parkOnpecent =  (float)inPark*100/parkingtotal ;
        }
        Calendar calendar1 = Calendar.getInstance();
        int hour = calendar1.get(Calendar.HOUR_OF_DAY);
        HashMap<String, Object> berthPercentData = new HashMap<String, Object>();
        berthPercentData.put("time",hour);
        berthPercentData.put("percent",df.format(parkOnpecent));
        //计算车场在线
        List<HashMap<String,Object>> parkState = getParkStatus(groupid);
        //查询抬杆异常信息
        List<HashMap<String,Object>> exceptionEvents = getExceptions(groupid,"groupid",tday);
        retMap.put("inPartData", entryCarList); //存入进场车辆
        retMap.put("outPartData", outCarList); //存入离场车辆
        retMap.put("totalIncomPie", totalIncomPie); //存入金额分类统计list
        retMap.put("totalIncome", totalIncomemap);//今日收入统计
        retMap.put("parkRank", parkRankList); //收入排行
        retMap.put("inOutCarsCount", countMap);//进出车统计
        retMap.put("berthPercentData", ss);//泊位使用率
        retMap.put("parkState", parkState); //车场状态
        retMap.put("exceptionEvents", exceptionEvents);//车场异常信息
        String result = JSON.toJSON(retMap).toString();
        return result;
    }

    @Override
    public String getInfoByComid(int comid) {
        logger.info("===>>>>comid"+comid);
        Long groupid = orderMapper.getGroupIdByComId(Long.parseLong(comid+""));
        Long cityid=-1L;
        if(groupid!=null&&groupid>-1){
            cityid = orderMapper.getCityIdByGroupId(groupid);
        }else {
            cityid = orderMapper.getCityIdByComId(Long.parseLong(comid+""));
        }
        String tableName = "order_tb_new";
        if(cityid!=null&&cityid>-1){
            tableName = tableName+"_"+cityid%100;
        }
        HashMap<String, Object> retMap = new HashMap<String, Object>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        long tday = calendar.getTimeInMillis() / 1000;
        //获取车场空车位
        int parkEmpty = parkInfoMapper.getParkEmpty(comid);
        //获取车场今天新建月卡会员
        int monthTotal = parkInfoMapper.getMonthTotal(comid,tday);
        //获取今天的优惠券下发数量
        int ticketCount = parkInfoMapper.getTicketCount(comid,tday);
        //获取访客未处理
        int vistorCount = parkInfoMapper.getVisitorCount(comid);
        //获取进场和离场数据
        List<Map<String, String>> entryCarList = orderServer.getEntryCarByComid(tday, comid,tableName);//parkInfoMapper.getEntryCarByComid(tday, comid,tableName);
        List<Map<String, String>> outCarList =orderServer.getExitCarByComid(tday, comid,tableName); //parkInfoMapper.getExitCarByComid(tday, comid,tableName);
        int berthtotal = parkInfoMapper.getBerthTotalbc(comid);
        //获取今日电子支付，现金支付，减免金额的统计
        Map<String,String> parammap = new HashMap<String,String>();
        parammap.put("comid",comid+"");
        String todyyymmdd=TimeTools.getDate_YY_MM_DD();
        parammap.put("date",todyyymmdd+" 00:00:00至"+todyyymmdd+" 23:59:59");
        JSONObject retjson=parkOrderanlysisService.selectResultByConditions(parammap);
        JSONArray retarry = retjson.getJSONArray("rows");
        Double cashPay=0d;
        Double electronicPay=0d;
        Double freePay=0d;
        if(retarry!=null&&retarry.size()>0){
            JSONObject object =(JSONObject)retarry.get(retarry.size()-1);
            if(object.getString("cash_pay")!=null&&!"".equals(object.getString("cash_pay"))) {
                cashPay = Double.parseDouble(object.getString("cash_pay"));
            }
            if(object.getString("free_pay")!=null&&!"".equals(object.getString("free_pay"))) {
                freePay = Double.parseDouble(object.getString("free_pay"));
            }
            if(object.getString("electronic_pay")!=null&&!"".equals(object.getString("electronic_pay"))) {
                electronicPay = Double.parseDouble(object.getString("electronic_pay"));
            }
        }
        Map<String,Object> otherData = new HashMap<>();
        HashMap<String, Object> cashPaymap = new HashMap<String, Object>();
        HashMap<String, Object> electronicPaymap = new HashMap<String, Object>();
        HashMap<String, Object> reduceamap = new HashMap<String, Object>();
        HashMap<String, Object> totalIncomemap = new HashMap<String, Object>();
        otherData.put("receiveTotal",cashPay+electronicPay);
        otherData.put("parkEmpty",parkEmpty);
        otherData.put("monthTotal",monthTotal);
        otherData.put("ticketCount",ticketCount);
        otherData.put("vistorCount",vistorCount);
        totalIncomemap.put("elePay", af1.format(electronicPay));
        totalIncomemap.put("cashPay", af1.format(cashPay));
        totalIncomemap.put("freePay", af1.format(freePay));
        cashPaymap.put("name", "电子");
        cashPaymap.put("value", af1.format(electronicPay));
        electronicPaymap.put("name", "现金");
        electronicPaymap.put("value", af1.format(cashPay));
        reduceamap.put("name", "减免");
        reduceamap.put("value", af1.format(freePay));
        List<HashMap<String, Object>> totalIncomPie = new ArrayList<HashMap<String, Object>>();
        totalIncomPie.add(cashPaymap);
        totalIncomPie.add(electronicPaymap);
        totalIncomPie.add(reduceamap);
        //获取收费排行数据
        List<Map<String, String>> parkRankList = orderServer.getRankByout(tday, comid,tableName);
        //获取车辆进场，离场，在场的数量统计
        int inCars = orderServer.getEntryCountbc(tday, comid,tableName);
        int outCars = orderServer.getExitCountbc(tday, comid,tableName);
        int inPark = orderServer.getInparkCountbc(tday, comid,tableName);
//        int inCars = parkInfoMapper.getEntryCountbc(tday, comid,tableName);
//        int outCars = parkInfoMapper.getExitCountbc(tday, comid,tableName);
//        int inPark = parkInfoMapper.getInparkCountbc(tday, comid,tableName);
        HashMap<String, Object> countMap = new HashMap<String, Object>();
        countMap.put("inCars", inCars);
        countMap.put("outCars", outCars);
        countMap.put("inPark", inPark);
        //计算泊位使用率
        HashMap<String,Object> tempmap = new  HashMap<String,Object>();
        List<HashMap<String,Object>> parkidList = new ArrayList<HashMap<String,Object>>();
        tempmap.put("parkid",comid);
        parkidList.add(tempmap);
        List<HashMap<String,Object>> ss =parkInfoMapper.getBerthPercent(parkidList,tday);
        for(HashMap<String,Object> map :ss){
            Long asum = (Long)map.get("asum");
            Long usum = (Long) map.get("usum");
            DecimalFormat df = new DecimalFormat("#");
            double pecent=0d;
            if(usum !=0){
                pecent =  (float)usum*100/asum ;
            }
            SimpleDateFormat sdf = new SimpleDateFormat("HH");
            long time = (long) map.get("create_time");
            Date date = new Date(time * 1000);
            map.put("time", sdf.format(date));
            map.put("percent",df.format(pecent));
        }
        double parkOnpecent=0d;
        DecimalFormat df = new DecimalFormat("#");
        if(inPark !=0){
            parkOnpecent =  (float)inPark*100/berthtotal ;
        }
        Calendar calendar1 = Calendar.getInstance();
        int hour = calendar1.get(Calendar.HOUR_OF_DAY);
        HashMap<String, Object> berthPercentData = new HashMap<String, Object>();
        berthPercentData.put("time",hour);
        berthPercentData.put("percent",df.format(parkOnpecent));
        //计算车场在线
        List<HashMap<String,Object>> parkState = getParkStatusbc(comid);
        List<HashMap<String,Object>> exceptionEvents = getExceptions(comid,"comid",tday);

        retMap.put("inPartData", entryCarList); //存入进场车辆
        retMap.put("outPartData", outCarList); //存入离场车辆
        retMap.put("totalIncomPie", totalIncomPie); //存入金额分类统计list
        retMap.put("totalIncome", totalIncomemap);//今日收入统计
        retMap.put("parkRank", parkRankList); //收入排行
        retMap.put("inOutCarsCount", countMap);//进出车统计
        retMap.put("berthPercentData", ss);//泊位使用率
        retMap.put("parkState", parkState);//在线状态
        retMap.put("exceptionEvents", exceptionEvents);//车场异常信息
        retMap.put("otherData", otherData);//总收入，会员，访客，优惠券，空车位
        String result = JSON.toJSON(retMap).toString();
        return result;
    }
   private List<HashMap<String,Object>> getExceptions(int id,String sflag,long ctime){
       List<HashMap<String,Object>> exceptionEvents = new ArrayList<HashMap<String,Object>>();
        if("comid".equals(sflag)){
           exceptionEvents = parkInfoMapper.getExpByCid(id,ctime);
        }else if("groupid".equals(sflag)){
             exceptionEvents = parkInfoMapper.getExpByGid(id,ctime);
        }
       if(exceptionEvents!=null && exceptionEvents.size()>0){
           SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
           for (HashMap<String,Object> map:exceptionEvents){
               Long uin =(Long) map.get("uin");
               if(uin !=null) {
                   String username = parkInfoMapper.getUserInfo(uin);
                   if (username != null && !"".equals(username))
                       map.put("uin", username);
               }
               Long time = (Long) map.get("ctime");
               if(time !=null) {
                   Date date = new Date(time * 1000);
                   map.put("time", sdf.format(date));
                   map.remove("ctime");
               }
           }

       }
    return exceptionEvents;
   }
    /**
     * 把从数据库查出的long时间秒值转为时间：分钟格式的时间字符串
     *
     * @param list
     */
    private void parseTmtoDate(List<Map<String, String>> list) {
        logger.info("~~~~~~parseTmtoDate"+list);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        if(list!=null&&list.size()>0) {
            for (Map<String, String> map : list) {
                logger.info("========>>>>>>>>>>map"+map);
                long time = Long.parseLong(map.get("timemills"));
                Date date = new Date(time * 1000);
                map.put("timemills", sdf.format(date));
//                map.remove("timemills");
                if (map.get("comid") != null) {
                    long comid = Long.parseLong(map.get("comid"));
                    String parkName = getParkNameById(comid);
                    map.put("parkName", parkName);
                }

            }
        }

    }

    private String getParkNameById(long comid) {
        Map<String,Object> map = parkInfoMapper.getParkNameById(comid);
        if(map!=null&&map.get("name")!=null){
            return map.get("name")+"";
        }
        return "";
    }

    private List<HashMap<String, Object>> getParkStatus(int groupid) {
        List<HashMap<String,Object>> parkState = new ArrayList<HashMap<String,Object>>();
        List<HashMap<String,Object>> parkidList = parkInfoMapper.getParkIdByGroupId(groupid);
        if (parkidList != null && parkidList.size() > 0) {
            for (HashMap<String,Object> parkidmap : parkidList) {
                long  parkid = (long)parkidmap.get("parkid");
                String parkName = (String)parkidmap.get("parkName");
                HashMap<String,Object> parkstatusmap = new  HashMap<String,Object>();
                List<HashMap<String, Object>> parkLoginList = parkInfoMapper.getParkLogin(parkid + "");
                if (parkLoginList != null && parkLoginList.size() > 0) {
                    HashMap<String, Object> loginmap = parkLoginList.get(0);
                    Long beattime = (Long) loginmap.get("beattime");
                    Long logintime = (Long) loginmap.get("logintime");
                    boolean isonline = false;
                    if(beattime!=null) {
                        //心跳在60秒内证明在线
                        isonline=isParkOnline(beattime.longValue(),60);

                      if(!isonline){
                          isonline=isParkOnline(logintime.longValue(),10);
                      }
                    }
                      if(isonline){
                          parkstatusmap.put("parkName",parkName);
                          parkstatusmap.put("state",1);
                      }else{
                          parkstatusmap.put("parkName",parkName);
                          parkstatusmap.put("state",0);
                      }
                    }else{
                    parkstatusmap.put("parkName",parkName);
                    parkstatusmap.put("state",0);
                }
                parkState.add(parkstatusmap);
            }
        }
        return parkState;
    }

    private List<HashMap<String, Object>> getParkStatusbc(int parkid) {
        List<HashMap<String, Object>> parkState = new ArrayList<HashMap<String, Object>>();
        List<HashMap<String, Object>> parkLoginList = parkInfoMapper.getParkLogin(parkid + "");
        if (parkLoginList != null && parkLoginList.size() > 0) {
            for (HashMap<String, Object> loginmap : parkLoginList){
                HashMap<String, Object> parkstatusmap = new HashMap<String, Object>();
            Long beattime = (Long) loginmap.get("beattime");
            Long logintime = (Long) loginmap.get("logintime");
            String localid = (String) loginmap.get("localid");
            if(localid == null)localid="";
            boolean isonline = false;
            if (beattime != null) {
                //心跳在60秒内证明在线
                isonline = isParkOnline(beattime.longValue(), 60);

                if (!isonline) {
                    isonline = isParkOnline(logintime.longValue(), 10);
                }
            }
            if (isonline) {
                parkstatusmap.put("state", 1);
                parkstatusmap.put("localid", localid.substring(localid.indexOf("_")+1));
            } else {
                parkstatusmap.put("state", 0);
                parkstatusmap.put("localid", localid.substring(localid.indexOf("_")+1));
            }
                parkState.add(parkstatusmap);
        }
    }

        return parkState;
    }
    /**
     * 判断车场是否在线
     * @param time
     * @param delayTime
     * @return
     * @time 2017年 下午12:03:41
     * @author QuanHao
     */
    private boolean isParkOnline(long time,int delayTime){
        long curTime = System.currentTimeMillis()/1000;
        long margin = curTime-time;
        if(margin-delayTime<=0){
            return true;
        }
        return false;
    }
}

