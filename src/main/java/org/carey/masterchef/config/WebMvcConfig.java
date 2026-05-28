package org.carey.masterchef.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 静态资源映射：占位图 + AI 生成的本地菜品图。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${masterchef.images.storage-dir:./data/images}")
    private String storageDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path dir = Paths.get(storageDir).toAbsolutePath().normalize();
        registry.addResourceHandler("/images/generated/**")
                .addResourceLocations("file:" + dir + "/");
    }
}
