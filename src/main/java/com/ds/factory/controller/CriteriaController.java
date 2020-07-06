package com.ds.factory.controller;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ds.factory.constants.BusinessConstants;
import com.ds.factory.constants.ExceptionConstants;
import com.ds.factory.datasource.entities.Product_Criteria;
import com.ds.factory.datasource.entities.Raw_Materials_Criteria;
import com.ds.factory.datasource.entities.Staff;
import com.ds.factory.service.Service.LogService;
import com.ds.factory.service.Service.Product_CriteriaService;
import com.ds.factory.service.Service.Raw_Materials_CriteriaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

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

    @PostMapping("/batchDeleteProductByIds")
    @ResponseBody
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

        return result;
    }

    @PostMapping("/product_add")
    @ResponseBody
    public Object product_add(@RequestParam("info") String beanJson, HttpServletRequest request)throws Exception{
        JSONObject result = ExceptionConstants.standardSuccess();
        Product_Criteria client= JSON.parseObject(beanJson, Product_Criteria.class);
        product_criteriaService.insert(client);

        Staff sta=(Staff)request.getSession().getAttribute("user");
        logService.insertLog(BusinessConstants.LOG_MODULE_NAME_PRODUCT_CRITERIA,
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_ADD).append(", id: "+sta.getId()).toString() +
                        "添加信息："+client,
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        return result;
    }


    @PostMapping("/product_update")
    @ResponseBody
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
        return result;
    }


    @PostMapping("/batchDeleteMaterialByIds")
    @ResponseBody
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

        return result;
    }

    @PostMapping("/material_add")
    @ResponseBody
    public Object material_add(@RequestParam("info") String beanJson, HttpServletRequest request)throws Exception{
        JSONObject result = ExceptionConstants.standardSuccess();
        Raw_Materials_Criteria client= JSON.parseObject(beanJson, Raw_Materials_Criteria.class);
        raw_materials_criteriaService.insert(client);

        Staff sta=(Staff)request.getSession().getAttribute("user");
        logService.insertLog(BusinessConstants.LOG_MODULE_NAME_RAW_MATERIALS_CRITERIA,
                new StringBuffer(BusinessConstants.LOG_OPERATION_TYPE_ADD).append(", id: "+sta.getId()).toString() +
                        "添加信息："+client,
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest());
        return result;
    }


    @PostMapping("/material_update")
    @ResponseBody
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
        return result;
    }
}
