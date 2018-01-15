package parkingos.com.bolink.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.zld.common_dao.dao.CommonDao;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import parkingos.com.bolink.models.UserInfoTb;
import parkingos.com.bolink.service.MemberService;
import parkingos.com.bolink.service.SupperSearchService;

import java.util.Map;

@Service
public class MemberServiceImpl implements MemberService {

    Logger logger = Logger.getLogger(MemberServiceImpl.class);

    @Autowired
    private CommonDao commonDao;
    @Autowired
    private SupperSearchService<UserInfoTb> supperSearchService;

    @Override
    public JSONObject selectResultByConditions(Map<String, String> reqmap) {
        JSONObject result = supperSearchService.supperSearch(new UserInfoTb(),reqmap);
        /*String str = "{\"total\":12,\"page\":1,\"rows\":[]}";
        JSONObject result = JSONObject.parseObject(str);


        int count =0;
        List<UserInfoTb> list =null;
        List<Map<String, Object>> resList =new ArrayList<>();
        Map searchMap = supperSearchService.getBaseSearch(new UserInfoTb(),reqmap);
        logger.info(searchMap);
        if(searchMap!=null&&!searchMap.isEmpty()){
            UserInfoTb baseQuery =(UserInfoTb)searchMap.get("base");
            List<SearchBean> supperQuery = null;
            if(searchMap.containsKey("supper"))
                supperQuery = (List<SearchBean>)searchMap.get("supper");
            PageOrderConfig config = null;
            if(searchMap.containsKey("config"))
                config = (PageOrderConfig)searchMap.get("config");
            count = commonDao.selectCountByConditions(baseQuery,supperQuery);
            if(count>0){
                list = commonDao.selectListByConditions(baseQuery,supperQuery,config);

                if (list != null && !list.isEmpty()) {
                    for (UserInfoTb product : list) {
                        OrmUtil<UserInfoTb> otm = new OrmUtil<>();
                        Map<String, Object> map = otm.pojoToMap(product);
                        resList.add(map);
                    }
                    result.put("rows", JSON.toJSON(resList));
                }
            }
        }
        result.put("total",count);
        result.put("page",Integer.parseInt(reqmap.get("page")));*/
        return result;
    }

}
