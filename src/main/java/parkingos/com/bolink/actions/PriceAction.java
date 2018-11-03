package parkingos.com.bolink.actions;


import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import parkingos.com.bolink.models.ParkLogTb;
import parkingos.com.bolink.models.PriceTb;
import parkingos.com.bolink.service.PriceService;
import parkingos.com.bolink.service.SaveLogService;
import parkingos.com.bolink.utils.RequestUtil;
import parkingos.com.bolink.utils.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@Controller
@RequestMapping("/price")
public class PriceAction {

    Logger logger = LoggerFactory.getLogger(PriceAction.class);

    @Autowired
    private PriceService priceService;
    @Autowired
    private SaveLogService saveLogService;

    @RequestMapping(value = "/query")
    public String query(HttpServletRequest request, HttpServletResponse resp){

        Map<String, String> reqParameterMap = RequestUtil.readBodyFormRequset(request);

        JSONObject result = priceService.selectResultByConditions(reqParameterMap);
        //把结果返回页面
        StringUtils.ajaxOutput(resp,result.toJSONString());
        return null;
    }

    @RequestMapping(value = "/add")
    public String createPrice(HttpServletRequest request, HttpServletResponse resp){
        Long comid = RequestUtil.getLong(request,"comid",-1L);
        String nickname = StringUtils.decodeUTF8(RequestUtil.getString(request,"nickname1"));
        Long uin = RequestUtil.getLong(request, "loginuin", -1L);

        Long createTime = System.currentTimeMillis()/1000;
        String describe =RequestUtil.getString(request,"describe");
        String carTypeZh =RequestUtil.getString(request,"car_type_zh");
        Long id  = priceService.getId();
        PriceTb priceTb = new PriceTb();
        priceTb.setComid(comid);
        priceTb.setId(id);
        priceTb.setCarTypeZh(carTypeZh);
        priceTb.setDescribe(describe);
        priceTb.setCreateTime(createTime);
        priceTb.setPriceId(id+"");

        JSONObject result = priceService.createPrice(priceTb);

        if((Integer)result.get("state")==1){
            ParkLogTb parkLogTb = new ParkLogTb();
            parkLogTb.setOperateUser(nickname);
            parkLogTb.setOperateTime(System.currentTimeMillis()/1000);
            parkLogTb.setOperateType(1);
            parkLogTb.setContent(uin+"("+nickname+")"+"增加了价格"+id);
            parkLogTb.setType("price");
            parkLogTb.setParkId(comid);
            saveLogService.saveLog(parkLogTb);
        }
        StringUtils.ajaxOutput(resp,result.toJSONString());
        return null;
    }


    @RequestMapping(value = "/edit")
    public String editPrice(HttpServletRequest request, HttpServletResponse resp){
        Long comid = RequestUtil.getLong(request,"comid",-1L);
        String nickname = StringUtils.decodeUTF8(RequestUtil.getString(request,"nickname1"));
        Long uin = RequestUtil.getLong(request, "loginuin", -1L);

        Long id = RequestUtil.getLong(request,"id",-1L);
        String describe =RequestUtil.getString(request,"describe");
        String carTypeZh =RequestUtil.getString(request,"car_type_zh");
        Long updateTime = System.currentTimeMillis()/1000;

        PriceTb priceTb = new PriceTb();
        priceTb.setDescribe(describe);
        priceTb.setId(id);
        priceTb.setCarTypeZh(carTypeZh);
        priceTb.setUpdateTime(updateTime);

        JSONObject result = priceService.updatePrice(priceTb);
        if((Integer)result.get("state")==1){
            ParkLogTb parkLogTb = new ParkLogTb();
            parkLogTb.setOperateUser(nickname);
            parkLogTb.setOperateTime(System.currentTimeMillis()/1000);
            parkLogTb.setOperateType(2);
            parkLogTb.setContent(uin+"("+nickname+")"+"修改了价格"+id);
            parkLogTb.setType("price");
            parkLogTb.setParkId(comid);
            saveLogService.saveLog(parkLogTb);
        }

        StringUtils.ajaxOutput(resp,result.toJSONString());
        return null;
    }

    @RequestMapping(value = "/delete")
    public String deletePrice(HttpServletRequest request, HttpServletResponse resp){
        Long comid = RequestUtil.getLong(request,"comid",-1L);
        String nickname = StringUtils.decodeUTF8(RequestUtil.getString(request,"nickname1"));
        Long uin = RequestUtil.getLong(request, "loginuin", -1L);

        Long id = RequestUtil.getLong(request,"id",-1L);

        PriceTb priceTb = new PriceTb();
        priceTb.setId(id);
        priceTb.setIsDelete(1L);

        JSONObject result = priceService.deletePrice(priceTb);
        if((Integer)result.get("state")==1){
            ParkLogTb parkLogTb = new ParkLogTb();
            parkLogTb.setOperateUser(nickname);
            parkLogTb.setOperateTime(System.currentTimeMillis()/1000);
            parkLogTb.setOperateType(3);
            parkLogTb.setContent(uin+"("+nickname+")"+"删除了价格"+id);
            parkLogTb.setType("price");
            parkLogTb.setParkId(comid);
            saveLogService.saveLog(parkLogTb);
        }
        StringUtils.ajaxOutput(resp,result.toJSONString());
        return null;
    }

}
