package org.carey.masterchef;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 「一饭封神」应用入口类。
 * <p>
 * Spring Boot 启动时会自动扫描当前包及子包下的所有组件（Controller、Service、Config 等），
 * 并完成依赖注入、数据库连接、Redis 连接等初始化工作。
 * </p>
 *
 * <p><b>@SpringBootApplication</b> 是一个组合注解，等价于：</p>
 * <ul>
 *   <li>@Configuration — 标记为配置类</li>
 *   <li>@EnableAutoConfiguration — 启用 Spring Boot 自动配置</li>
 *   <li>@ComponentScan — 扫描并注册 Bean</li>
 * </ul>
 *
 * <p><b>@MapperScan</b> 告诉 MyBatis 去指定包下查找 Mapper 接口，
 * 这样 {@code RecipeMapper} 等接口才能被 Spring 管理并注入到 Service 中。</p>
 */
@SpringBootApplication
@MapperScan("org.carey.masterchef.mapper")
public class MasterChefApplication {

    /**
     * Java 程序标准入口。运行 main 方法即可启动内嵌 Tomcat，默认监听 8080 端口。
     */
    public static void main(String[] args) {
        SpringApplication.run(MasterChefApplication.class, args);
    }
}
