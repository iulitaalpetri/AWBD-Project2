package com.awbd.bookstore.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.SimpleMappingExceptionResolver;

import java.util.Properties;

@Configuration
public class MvcCofiguration implements WebMvcConfigurer {
    @Bean(name = "simpleMappingExceptionResolver")
    public SimpleMappingExceptionResolver
    getSimpleMappingExceptionResolver() {
        SimpleMappingExceptionResolver r =
                new SimpleMappingExceptionResolver();

        r.setDefaultErrorView("defaultException");
        r.setExceptionAttribute("ex");     // default "exception"

        Properties mappings = new Properties();
        mappings.setProperty("NumberFormatException", "numberFormatException");
        r.setExceptionMappings(mappings);

        Properties statusCodes = new Properties();
        statusCodes.setProperty("NumberFormatException", "400");
        r.setStatusCodes(statusCodes);

        return r;
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {

        registry.addRedirectViewController("/", "/welcome");
        registry.addViewController("/login").setViewName("login");
        registry.addViewController("/register").setViewName("register");
        registry.addViewController("/welcome").setViewName("welcome");
        registry.addViewController("/home").setViewName("home");
        registry.addViewController("/category").setViewName("category");
        registry.addViewController("/author").setViewName("author");
        registry.addViewController("/profile").setViewName("profile");
        registry.addViewController("/cart").setViewName("cart");
        registry.addViewController("/wishlist").setViewName("wishlist");
        registry.addViewController("/orderhistory").setViewName("orderhistory");
        registry.addViewController("/order").setViewName("order");
        registry.addViewController("/book").setViewName("book");
        registry.addViewController("/admin_author").setViewName("admin_author");
        registry.addViewController("/admin_category").setViewName("admin_category");
        registry.addViewController("/admin_book").setViewName("admin_book");
        registry.addViewController("/admin_order").setViewName("admin_order");
        registry.addViewController("/admin_user").setViewName("admin_user");
        registry.addViewController("/admin_sale").setViewName("admin_sale");
    }
}
