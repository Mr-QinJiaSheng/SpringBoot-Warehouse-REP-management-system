package com.jsh.erp.service.accountHead;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.jsh.erp.constants.BusinessConstants;
import com.jsh.erp.constants.ExceptionConstants;
import com.jsh.erp.datasource.entities.*;
import com.jsh.erp.datasource.mappers.AccountHeadMapper;
import com.jsh.erp.datasource.mappers.AccountHeadMapperEx;
import com.jsh.erp.datasource.mappers.AccountItemMapperEx;
import com.jsh.erp.exception.BusinessRunTimeException;
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
public class AccountHeadService {
    private Logger logger = LoggerFactory.getLogger(AccountHeadService.class);

    @Resource
    private AccountHeadMapper accountHeadMapper;

    @Resource
    private AccountHeadMapperEx accountHeadMapperEx;
    @Resource
    private UserService userService;
    @Resource
    private LogService logService;
    @Resource
    private AccountItemMapperEx accountItemMapperEx;

    public AccountHead getAccountHead(long id) throws Exception {
        AccountHead result=null;
        try{
            result=accountHeadMapper.selectByPrimaryKey(id);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return result;
    }

    public List<AccountHead> getAccountHead() throws Exception{
        AccountHeadExample example = new AccountHeadExample();
        List<AccountHead> list=null;
        try{
            list=accountHeadMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list;
    }

    public List<AccountHeadVo4ListEx> select(String type, String billNo, String beginTime, String endTime, int offset, int rows) throws Exception{
        List<AccountHeadVo4ListEx> resList = new ArrayList<AccountHeadVo4ListEx>();
        List<AccountHeadVo4ListEx> list=null;
        try{
            list = accountHeadMapperEx.selectByConditionAccountHead(type, billNo, beginTime, endTime, offset, rows);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        if (null != list) {
            for (AccountHeadVo4ListEx ah : list) {
                if(ah.getChangeamount() != null) {
                    ah.setChangeamount(ah.getChangeamount().abs());
                }
                if(ah.getTotalprice() != null) {
                    ah.setTotalprice(ah.getTotalprice().abs());
                }
                resList.add(ah);
            }
        }
        return resList;
    }

    public Long countAccountHead(String type, String billNo, String beginTime, String endTime) throws Exception{
        Long result=null;
        try{
            result = accountHeadMapperEx.countsByAccountHead(type, billNo, beginTime, endTime);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int insertAccountHead(String beanJson, HttpServletRequest request) throws Exception{
        AccountHead accountHead = JSONObject.parseObject(beanJson, AccountHead.class);
        int result=0;
        try{
            result = accountHeadMapper.insertSelective(accountHead);
            logService.insertLog("??????", BusinessConstants.LOG_OPERATION_TYPE_ADD, request);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int updateAccountHead(String beanJson, Long id, HttpServletRequest request)throws Exception {
        AccountHead accountHead = JSONObject.parseObject(beanJson, AccountHead.class);
        accountHead.setId(id);
        int result=0;
        try{
            result = accountHeadMapper.updateByPrimaryKeySelective(accountHead);
            logService.insertLog("??????",
                    new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_EDIT).append(id).toString(), request);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int deleteAccountHead(Long id, HttpServletRequest request)throws Exception {
        int result=0;
        try{
            result = accountHeadMapper.deleteByPrimaryKey(id);
            logService.insertLog("??????",
                    new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_DELETE).append(id).toString(), request);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeleteAccountHead(String ids, HttpServletRequest request)throws Exception {
        List<Long> idList = StringUtil.strToLongList(ids);
        AccountHeadExample example = new AccountHeadExample();
        example.createCriteria().andIdIn(idList);
        int result=0;
        try{
            result = accountHeadMapper.deleteByExample(example);
            logService.insertLog("??????", "????????????,id???:" + ids, request);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }

    public int checkIsNameExist(Long id, String name)throws Exception {
        AccountHeadExample example = new AccountHeadExample();
        example.createCriteria().andIdNotEqualTo(id).andDeleteFlagNotEqualTo(BusinessConstants.DELETE_FLAG_DELETED);
        List<AccountHead> list = null;
        try{
            list = accountHeadMapper.selectByExample(example);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return list==null?0:list.size();
    }

    public Long getMaxId()throws Exception {
        Long result = null;
        try{
            result = accountHeadMapperEx.getMaxId();
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return result;
    }

    public BigDecimal findAllMoney(Integer supplierId, String type, String mode, String endTime) {
        String modeName = "";
        if (mode.equals("??????")) {
            modeName = "ChangeAmount";
        } else if (mode.equals("??????")) {
            modeName = "TotalPrice";
        }
        BigDecimal result = null;
        try{
            result = accountHeadMapperEx.findAllMoney(supplierId, type, modeName, endTime);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        return result;
    }

    /**
     * ???????????????
     * @param getS
     * @param type
     * @param mode ??????????????????
     * @param endTime
     * @return
     */
    public BigDecimal allMoney(String getS, String type, String mode, String endTime) {
        BigDecimal allMoney = BigDecimal.ZERO;
        try {
            Integer supplierId = Integer.valueOf(getS);
            BigDecimal sum = findAllMoney(supplierId, type, mode, endTime);
            if(sum != null) {
                allMoney = sum;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //??????????????????????????????????????????
        if ((allMoney.compareTo(BigDecimal.ZERO))==-1) {
            allMoney = allMoney.abs();
        }
        return allMoney;
    }

    /**
     * ????????????????????????????????????????????????????????????????????????
     * @param supplierId
     * @param endTime
     * @param supType
     * @return
     */
    public BigDecimal findTotalPay(Integer supplierId, String endTime, String supType) {
        BigDecimal sum = BigDecimal.ZERO;
        String getS = supplierId.toString();
        int i = 1;
        if (("customer").equals(supType)) { //??????
            i = 1;
        } else if (("vendor").equals(supType)) { //?????????
            i = -1;
        }
        //???????????????
        sum = sum.add((allMoney(getS, "??????", "??????",endTime).add(allMoney(getS, "??????", "??????",endTime))).multiply(new BigDecimal(i)));
        sum = sum.subtract((allMoney(getS, "??????", "??????",endTime).add(allMoney(getS, "??????", "??????",endTime))).multiply(new BigDecimal(i)));
        sum = sum.add((allMoney(getS, "??????", "??????",endTime).subtract(allMoney(getS, "??????", "??????",endTime))).multiply(new BigDecimal(i)));
        sum = sum.subtract((allMoney(getS, "??????", "??????",endTime).subtract(allMoney(getS, "??????", "??????",endTime))).multiply(new BigDecimal(i)));
        return sum;
    }

    public List<AccountHeadVo4ListEx> getDetailByNumber(String billNo)throws Exception {
        List<AccountHeadVo4ListEx> resList = new ArrayList<AccountHeadVo4ListEx>();
        List<AccountHeadVo4ListEx> list = null;
        try{
            list = accountHeadMapperEx.getDetailByNumber(billNo);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        if (null != list) {
            for (AccountHeadVo4ListEx ah : list) {
                if(ah.getChangeamount() != null) {
                    ah.setChangeamount(ah.getChangeamount().abs());
                }
                if(ah.getTotalprice() != null) {
                    ah.setTotalprice(ah.getTotalprice().abs());
                }
                resList.add(ah);
            }
        }
        return resList;
    }
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeleteAccountHeadByIds(String ids)throws Exception {
        logService.insertLog("??????",
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_DELETE).append(ids).toString(),
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        User userInfo=userService.getCurrentUser();
        String [] idArray=ids.split(",");
        int result = 0;
        try{
            result = accountHeadMapperEx.batchDeleteAccountHeadByIds(new Date(),userInfo==null?null:userInfo.getId(),idArray);
        }catch(Exception e){
            JshException.writeFail(logger, e);
        }
        return result;
    }
    /**
     * create by: qiankunpingtai
     * website???https://qiankunpingtai.cn
     * description:
     *  ???????????????????????????????????????????????????????????????
     * create time: 2019/4/10 15:49
     * @Param: ids
     * @return int
     */
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public int batchDeleteAccountHeadByIdsNormal(String ids) throws Exception {
        /**
         * ??????
         * 1???????????????	jsh_accountitem
         * ?????????????????????
         * */
        int deleteTotal=0;
        if(StringUtils.isEmpty(ids)){
            return deleteTotal;
        }
        String [] idArray=ids.split(",");
        /**
         * ??????????????????	jsh_accountitem
         * */
        List<AccountItem> accountItemList = null;
        try{
            accountItemList = accountItemMapperEx.getAccountItemListByHeaderIds(idArray);
        }catch(Exception e){
            JshException.readFail(logger, e);
        }
        if(accountItemList!=null&&accountItemList.size()>0){
            logger.error("?????????[{}],????????????[{}],??????,HeaderIds[{}]",
                    ExceptionConstants.DELETE_FORCE_CONFIRM_CODE,ExceptionConstants.DELETE_FORCE_CONFIRM_MSG,ids);
            throw new BusinessRunTimeException(ExceptionConstants.DELETE_FORCE_CONFIRM_CODE,
                    ExceptionConstants.DELETE_FORCE_CONFIRM_MSG);
        }
        /**
         * ??????????????????????????????
         * */
        deleteTotal= batchDeleteAccountHeadByIds(ids);
        return deleteTotal;
    }
}
