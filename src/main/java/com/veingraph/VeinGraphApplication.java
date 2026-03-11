package com.veingraph;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * VeinGraph 应用启动入口
 * 基于 LLM 的实体提取、关系提取与 Agent 对话系统
 */
@SpringBootApplication
public class VeinGraphApplication {

    public static void main(String[] args) {
        SpringApplication.run(VeinGraphApplication.class, args);
    }
}
