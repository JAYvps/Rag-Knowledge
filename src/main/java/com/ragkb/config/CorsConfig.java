// ============ config/CorsConfig.java ============
// CORS已集成到SecurityConfig中（通过http.cors()），此处不再需要独立的CorsFilter Bean。
// 独立的CorsFilter Bean会绕过Spring Security过滤器链，导致JwtAuthFilter设置的SecurityContext被清除。
package com.ragkb.config;

// 此类已废弃，CORS配置移至SecurityConfig.corsConfigurationSource()
// @Configuration 注解已移除，防止Spring扫描到旧的CorsFilter Bean
