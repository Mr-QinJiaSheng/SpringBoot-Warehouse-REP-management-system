package com.jsh.erp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.http.Cookie;

@SpringBootApplication
@MapperScan(basePackages = {"com.jsh.erp.datasource.mappers"})
@ServletComponentScan
@EnableScheduling
public class ErpApplication{
    public static void main(String[] args) {
        SpringApplication.run(ErpApplication.class, args);

        System.out.println("==================================================================");
        System.out.println("                                                                  ");
        System.out.println("          erp系统启动成功!访问：http://localhost:8080               ");
        System.out.println("                                                                  ");
        System.out.println("==================================================================");
    }

}
