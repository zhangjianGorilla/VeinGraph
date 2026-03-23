package com.veingraph;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import java.net.InetAddress;

import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * VeinGraph 应用启动入口
 * 基于 LLM 的实体提取、关系提取与 Agent 对话系统
 */
@Slf4j
@EnableScheduling
@SpringBootApplication
public class VeinGraphApplication {

    @SneakyThrows
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(VeinGraphApplication.class);
        ConfigurableApplicationContext application = app.run(args);
        Environment env = application.getEnvironment();
        log.info("\n----------------------------------------------------------\n\t" +
                 "Application '{}' is running! Access URLs:\n\t" +
                 "Local: \t\thttp://localhost:{}\n\t" +
                 "External: \thttp://{}:{}\n\t" +
                 "Doc: \thttp://{}:{}{}/doc.html\n" +
                 "----------------------------------------------------------",
                env.getProperty("spring.application.name"),
                env.getProperty("server.port"),
                InetAddress.getLocalHost().getHostAddress(),
                env.getProperty("server.port"),
                InetAddress.getLocalHost().getHostAddress(),
                env.getProperty("server.port"),
                env.getProperty("server.servlet.context-path"));
    }

}
