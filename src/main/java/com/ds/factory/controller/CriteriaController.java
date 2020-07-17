package com.ds.factory.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ds.factory.constants.BusinessConstants;
import com.ds.factory.constants.ExceptionConstants;
import com.ds.factory.datasource.entities.*;
import com.ds.factory.datasource.mappers.Product_CriteriaMapper;
import com.ds.factory.service.Service.LogService;
import com.ds.factory.service.Service.Product_CriteriaService;
import com.ds.factory.service.Service.Raw_Materials_CriteriaService;
import com.ds.factory.utils.Constants;
import com.ds.factory.utils.ErpInfo;
import com.ds.factory.utils.PageQueryInfo;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import redis.clients.jedis.Jedis;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;

import static com.ds.factory.utils.ResponseJsonUtil.returnJson;

@RestController
@RequestMapping(value = "/criteria")
public class CriteriaController {
    private Logger logger = LoggerFactory.getLogger(UserController.class);

    @Resource
    LogService logService;

    @Resource
    private Product_CriteriaService product_criteriaService;

    @Resource
    private Raw_Materials_CriteriaService raw_materials_criteriaService;

    @Resource
    private Product_CriteriaMapper product_criteriaMapper;

    @Autowired
    AmqpTemplate messageQueue;

    @ResponseBody
    @RequestMapping(value = "/pictures_url", method = RequestMethod.POST)
    @CrossOrigin
    public JsonResult<Map<String, String>> client_purchase(@RequestParam("Product_no") String Product_no, HttpServletRequest request)throws Exception{
        JsonResult<Map<String, String>> result = new JsonResult<>();
        String pictures_url="";
        Map<String, String> amountMap = new HashMap<>();

        String Jedis_String = "Pictures_URL--No:"+Product_no;
        Jedis jedis=new Jedis("192.168.216.130",6379);//原先是6379
        String jsonString=jedis.get(Jedis_String);
        if(jsonString!=null)
        {
            ObjectMapper objectMapper=new ObjectMapper();
            TypeReference<String> typeReference = new TypeReference<String>() {};
            byte[] data=jsonString.getBytes();
            try {
                pictures_url=objectMapper.readValue(data, typeReference);
                System.out.println("***** ***** ***** ***** ***** *****");
            } catch (JsonParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (JsonMappingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        else
        {
            System.out.println("##### ##### ##### ##### ##### #####");
            pictures_url=product_criteriaMapper.pictures_url(Product_no);
            ObjectMapper objectMapper=new ObjectMapper();
            try {
                String jsonString2 = objectMapper.writeValueAsString(pictures_url);
                jedis.set(Jedis_String, jsonString2);
            } catch (JsonProcessingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        pictures_url=pictures_url==null||pictures_url==""?"":pictures_url;
        amountMap.put("pictures_url", pictures_url);
        result.setData(amountMap);
        Staff sta=(Staff)request.getSession().getAttribute("user");
        logService.insertLog(BusinessConstants.LOG_MODULE_NAME_PRODUCT_CRITERIA,
                "查看产品图片, id: "+sta.getId()+"的用户查看"+Product_no+"的图片",
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        messageQueue.convertAndSend("Factory",pictures_url.toString());
        return result;
    }



    @PostMapping("/batchDeleteProductByIds")
    @ResponseBody
    @CrossOrigin
    public Object batchDeleteProductByIds(@RequestParam("ids") String ids, HttpServletRequest request)throws Exception{
        JSONObject result = ExceptionConstants.standardSuccess();
        String[] id=ids.split(",");
        for(int i=0;i<id.length;i++)
        {
            product_criteriaService.delete(id[i].trim());
        }
        Staff sta=(Staff)request.getSession().getAttribute("user");
        logService.insertLog(BusinessConstants.LOG_MODULE_NAME_PRODUCT_CRITERIA,
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_BATCH_delete).append(", id: "+sta.getId()).toString() +
                        "删除信息ID组："+ids,
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        messageQueue.convertAndSend("Factory",ids.toString());
        return result;
    }

    @PostMapping("/product_add")
    @ResponseBody
    @CrossOrigin
    public Object product_add(@RequestParam("info") String beanJson, HttpServletRequest request)throws Exception{
        JSONObject result = ExceptionConstants.standardSuccess();
        Product_Criteria client= JSON.parseObject(beanJson, Product_Criteria.class);
        product_criteriaService.insert(client);

        Staff sta=(Staff)request.getSession().getAttribute("user");
        logService.insertLog(BusinessConstants.LOG_MODULE_NAME_PRODUCT_CRITERIA,
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_ADD).append(", id: "+sta.getId()).toString() +
                        "添加信息："+client,
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        messageQueue.convertAndSend("Factory",client.toString());
        return result;
    }


    @PostMapping("/product_update")
    @ResponseBody
    @CrossOrigin
    public Object product_update(@RequestParam("info") String beanJson,@RequestParam("id") Long id,
                                 HttpServletRequest request)throws Exception{
        JSONObject result = ExceptionConstants.standardSuccess();
        Product_Criteria client= JSON.parseObject(beanJson, Product_Criteria.class);
        String Client_no=(id+"").trim();

        switch (Client_no.length()){
            case 1: Client_no="000"+Client_no;break;
            case 2: Client_no="00"+Client_no;break;
            case 3: Client_no="0"+Client_no;break;
            case 4: break;
        }
        client.setProduct_no(Client_no);
        product_criteriaService.update(client);

        Staff sta=(Staff)request.getSession().getAttribute("user");
        logService.insertLog(BusinessConstants.LOG_MODULE_NAME_PRODUCT_CRITERIA,
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_EDIT).append(", id: "+sta.getId()).toString() +
                        "修改信息："+client,
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        messageQueue.convertAndSend("Factory",client.toString());
        return result;
    }


    @PostMapping("/batchDeleteMaterialByIds")
    @ResponseBody
    @CrossOrigin
    public Object batchDeleteMaterialByIds(@RequestParam("ids") String ids, HttpServletRequest request)throws Exception{
        JSONObject result = ExceptionConstants.standardSuccess();
        String[] id=ids.split(",");
        for(int i=0;i<id.length;i++)
        {
            raw_materials_criteriaService.delete(id[i].trim());
        }
        Staff sta=(Staff)request.getSession().getAttribute("user");
        logService.insertLog(BusinessConstants.LOG_MODULE_NAME_RAW_MATERIALS_CRITERIA,
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_BATCH_delete).append(", id: "+sta.getId()).toString() +
                        "删除信息ID组："+ids,
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        messageQueue.convertAndSend("Factory",ids.toString());
        return result;
    }

    @PostMapping("/material_add")
    @ResponseBody
    @CrossOrigin
    public Object material_add(@RequestParam("info") String beanJson, HttpServletRequest request)throws Exception{
        JSONObject result = ExceptionConstants.standardSuccess();
        Raw_Materials_Criteria client= JSON.parseObject(beanJson, Raw_Materials_Criteria.class);
        raw_materials_criteriaService.insert(client);

        Staff sta=(Staff)request.getSession().getAttribute("user");
        logService.insertLog(BusinessConstants.LOG_MODULE_NAME_RAW_MATERIALS_CRITERIA,
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_ADD).append(", id: "+sta.getId()).toString() +
                        "添加信息："+client,
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        messageQueue.convertAndSend("Factory",client.toString());
        return result;
    }


    @PostMapping("/material_update")
    @ResponseBody
    @CrossOrigin
    public Object material_update(@RequestParam("info") String beanJson,@RequestParam("id") Long id,
                                  HttpServletRequest request)throws Exception{
        JSONObject result = ExceptionConstants.standardSuccess();
        Raw_Materials_Criteria client= JSON.parseObject(beanJson, Raw_Materials_Criteria.class);
        String Client_no=(id+"").trim();

        switch (Client_no.length()){
            case 1: Client_no="0000"+Client_no;break;
            case 2: Client_no="000"+Client_no;break;
            case 3: Client_no="00"+Client_no;break;
            case 4: Client_no="0"+Client_no;break;
            case 5: break;
        }
        client.setMaterial_no(Client_no);
        raw_materials_criteriaService.update(client);
        Staff sta=(Staff)request.getSession().getAttribute("user");
        logService.insertLog(BusinessConstants.LOG_MODULE_NAME_RAW_MATERIALS_CRITERIA,
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_EDIT).append(", id: "+sta.getId()).toString() +
                        "修改信息："+client,
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        messageQueue.convertAndSend("Factory",client.toString());
        return result;
    }
}
