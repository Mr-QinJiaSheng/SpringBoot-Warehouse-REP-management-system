package com.jsh.erp.service.MaterialExtend;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.jsh.erp.constants.BusinessConstants;
import com.jsh.erp.datasource.entities.MaterialExtend;
import com.jsh.erp.datasource.entities.MaterialExtendExample;
import com.jsh.erp.datasource.entities.User;
import com.jsh.erp.datasource.mappers.MaterialExtendMapper;
import com.jsh.erp.datasource.mappers.MaterialExtendMapperEx;
import com.jsh.erp.datasource.vo.MaterialExtendVo4List;
import com.jsh.erp.exception.JshException;
import com.jsh.erp.service.log.LogService;
import com.jsh.erp.service.user.UserService;
import com.jsh.erp.utils.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Service
public class MaterialExtendService {
    private Logger logger = LoggerFactory.getLogger(MaterialExtendService.class);

    @Resource
    private MaterialExtendMapper materialExtendMapper;
    @Resource
    private MaterialExtendMapperEx materialExtendMapperEx;
    @Resource
    private LogService logService;
    @Resource
    private UserService userService;
    
    public MaterialExtend getMaterialExtend(long id)throws Exception {
        MaterialExtend result=null;
        try{
            result=materialExtendMapper.selectByPrimaryKey(id);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return result;
    }
    public List<MaterialExtendVo4List> getDetailList(Long materialId) {
        List<MaterialExtendVo4List> list=null;
        try{
            list = materialExtendMapperEx.getDetailList(materialId);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public List<MaterialExtend> getListByMIds(List<Long> idList) {
        List<MaterialExtend> meList = null;
        try{
            Long [] idArray= StringUtil.listToLongArray(idList);
            if(idArray!=null && idArray.length>0) {
                meList = materialExtendMapperEx.getListByMId(idArray);
            }
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return meList;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public String saveDetials(String inserted, String deleted, String updated,Long materialId) throws Exception {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        logService.insertLog("商品价格扩展",
                BusinessConstants.LOG_OPERATION_TYPE_ADD, request);
        //转为json
        JSONArray insertedJson = JSONArray.parseArray(inserted);
        JSONArray deletedJson = JSONArray.parseArray(deleted);
        JSONArray updatedJson = JSONArray.parseArray(updated);
        if (null != insertedJson) {
            for (int i = 0; i < insertedJson.size(); i++) {
                MaterialExtend materialExtend = new MaterialExtend();
                JSONObject tempInsertedJson = JSONObject.parseObject(insertedJson.getString(i));
                materialExtend.setMaterialId(materialId);
                if (StringUtils.isNotEmpty(tempInsertedJson.getString("BarCode"))) {
                    materialExtend.setBarCode(tempInsertedJson.getString("BarCode"));
                }
                if (StringUtils.isNotEmpty(tempInsertedJson.getString("CommodityUnit"))) {
                    materialExtend.setCommodityUnit(tempInsertedJson.getString("CommodityUnit"));
                }
                if (StringUtils.isNotEmpty(tempInsertedJson.getString("PurchaseDecimal"))) {
                    materialExtend.setPurchaseDecimal(tempInsertedJson.getBigDecimal("PurchaseDecimal"));
                }
                if (StringUtils.isNotEmpty(tempInsertedJson.getString("CommodityDecimal"))) {
                    materialExtend.setCommodityDecimal(tempInsertedJson.getBigDecimal("CommodityDecimal"));
                }
                if (StringUtils.isNotEmpty(tempInsertedJson.getString("WholesaleDecimal"))) {
                    materialExtend.setWholesaleDecimal(tempInsertedJson.getBigDecimal("WholesaleDecimal"));
                }
                if (StringUtils.isNotEmpty(tempInsertedJson.getString("LowDecimal"))) {
                    materialExtend.setLowDecimal(tempInsertedJson.getBigDecimal("LowDecimal"));
                }
                this.insertMaterialExtend(materialExtend);
            }
        }
        if (null != deletedJson) {
            StringBuffer bf=new StringBuffer();
            for (int i = 0; i < deletedJson.size(); i++) {
                JSONObject tempDeletedJson = JSONObject.parseObject(deletedJson.getString(i));
                bf.append(tempDeletedJson.getLong("Id"));
                if(i<(deletedJson.size()-1)){
                    bf.append(",");
                }
            }
            this.batchDeleteMaterialExtendByIds(bf.toString(), request);
        }
        if (null != updatedJson) {
            for (int i = 0; i < updatedJson.size(); i++) {
                JSONObject tempUpdatedJson = JSONObject.parseObject(updatedJson.getString(i));
                MaterialExtend materialExtend = new MaterialExtend();
                materialExtend.setId(tempUpdatedJson.getLong("Id"));
                if (StringUtils.isNotEmpty(tempUpdatedJson.getString("BarCode"))) {
                    materialExtend.setBarCode(tempUpdatedJson.getString("BarCode"));
                }
                if (StringUtils.isNotEmpty(tempUpdatedJson.getString("CommodityUnit"))) {
                    materialExtend.setCommodityUnit(tempUpdatedJson.getString("CommodityUnit"));
                }
                if (StringUtils.isNotEmpty(tempUpdatedJson.getString("PurchaseDecimal"))) {
                    materialExtend.setPurchaseDecimal(tempUpdatedJson.getBigDecimal("PurchaseDecimal"));
                }
                if (StringUtils.isNotEmpty(tempUpdatedJson.getString("CommodityDecimal"))) {
                    materialExtend.setCommodityDecimal(tempUpdatedJson.getBigDecimal("CommodityDecimal"));
                }
                if (StringUtils.isNotEmpty(tempUpdatedJson.getString("WholesaleDecimal"))) {
                    materialExtend.setWholesaleDecimal(tempUpdatedJson.getBigDecimal("WholesaleDecimal"));
                }
                if (StringUtils.isNotEmpty(tempUpdatedJson.getString("LowDecimal"))) {
                    materialExtend.setLowDecimal(tempUpdatedJson.getBigDecimal("LowDecimal"));
                }
                this.updateMaterialExtend(materialExtend, request);
            }
        }
        return null;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int insertMaterialExtend(MaterialExtend materialExtend)throws Exception {
        User user = userService.getCurrentUser();
        materialExtend.setDeleteFlag(BusinessConstants.DELETE_FLAG_EXISTS);
        materialExtend.setCreateTime(new Date());
        materialExtend.setUpdateTime(new Date().getTime());
        materialExtend.setCreateSerial(user.getLoginame());
        materialExtend.setUpdateSerial(user.getLoginame());
        int result =0;
        try{
            result= materialExtendMapper.insertSelective(materialExtend);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int updateMaterialExtend(MaterialExtend MaterialExtend, HttpServletRequest request) throws Exception{
        User user = userService.getCurrentUser();
        MaterialExtend.setUpdateTime(new Date().getTime());
        MaterialExtend.setUpdateSerial(user.getLoginame());
        int res =0;
        try{
            res= materialExtendMapper.updateByPrimaryKeySelective(MaterialExtend);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return res;
    }

    public int checkIsExist(Long id, String MaterialExtendName)throws Exception {
        MaterialExtendExample example = new MaterialExtendExample();
        MaterialExtendExample.Criteria criteria = example.createCriteria();
        criteria.andBarCodeEqualTo(MaterialExtendName);
        if (id > 0) {
            criteria.andIdNotEqualTo(id);
        }
        List<MaterialExtend> list =null;
        try{
            list=  materialExtendMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list==null?0:list.size();
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int deleteMaterialExtend(Long id, HttpServletRequest request)throws Exception {
        int result =0;
        MaterialExtend materialExtend = new MaterialExtend();
        materialExtend.setId(id);
        materialExtend.setDeleteFlag(BusinessConstants.DELETE_FLAG_DELETED);
        Object userInfo = request.getSession().getAttribute("user");
        User user = (User)userInfo;
        materialExtend.setUpdateTime(new Date().getTime());
        materialExtend.setUpdateSerial(user.getLoginame());
        try{
            result= materialExtendMapper.updateByPrimaryKeySelective(materialExtend);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeleteMaterialExtendByIds(String ids, HttpServletRequest request) throws Exception{
        logService.insertLog("商品价格扩展",
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_DELETE).append(ids).toString(),
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        String [] idArray=ids.split(",");
        int result = 0;
        try{
            result = materialExtendMapperEx.batchDeleteMaterialExtendByIds(idArray);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    public int insertMaterialExtend(String beanJson, HttpServletRequest request) throws Exception{
        MaterialExtend materialExtend = JSONObject.parseObject(beanJson, MaterialExtend.class);
        int result=0;
        try{
            result = materialExtendMapper.insertSelective(materialExtend);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    public int updateMaterialExtend(String beanJson, Long id, HttpServletRequest request)throws Exception {
        MaterialExtend materialExtend = JSONObject.parseObject(beanJson, MaterialExtend.class);
        int result=0;
        try{
            result = materialExtendMapper.insertSelective(materialExtend);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    public List<MaterialExtend> getMaterialExtendByTenantAndTime(Long tenantId, Long lastTime, Long syncNum)throws Exception {
        List<MaterialExtend> list=new ArrayList<MaterialExtend>();
        try{
            //先获取最大的时间戳，再查两个时间戳之间的数据，这样同步能够防止丢失数据（应为时间戳有重复）
            Long maxTime = materialExtendMapperEx.getMaxTimeByTenantAndTime(tenantId, lastTime, syncNum);
            if(tenantId!=null && lastTime!=null && maxTime!=null) {
                MaterialExtendExample example = new MaterialExtendExample();
                example.createCriteria().andTenantIdEqualTo(tenantId)
                        .andUpdateTimeGreaterThan(lastTime)
                        .andUpdateTimeLessThanOrEqualTo(maxTime);
                list=materialExtendMapper.selectByExample(example);
            }
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }
}
