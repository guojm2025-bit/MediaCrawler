package com.gjm.pk.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Vue前端路由控制器
 * 处理Vue单页应用的路由
 * 
 * @author guojianming
 * @date 2025/09/26
 */
@Controller
public class VueController {

    /**
     * Vue应用首页
     */
    @GetMapping("/vue")
    public String vueApp() {
        return "forward:/vue-app/index.html";
    }
    
    /**
     * 处理Vue路由的前端页面
     * 当访问Vue应用的任何路由时，都返回index.html
     * 让Vue Router处理客户端路由
     */
    @GetMapping("/vue/**")
    public String vueRoutes() {
        return "forward:/vue-app/index.html";
    }
}