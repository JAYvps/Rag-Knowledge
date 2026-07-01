// ============ yuque/YuqueClient.java ============
package com.ragkb.yuque;

import com.ragkb.yuque.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * 语雀API客户端
 *
 * 语雀API文档: https://www.yuque.com/yuque/developer
 * 认证方式: 请求头 X-Auth-Token
 *
 * 接口：
 * 1. GET /users/:login/repos              获取知识库列表
 * 2. GET /repos/:namespace/:slug/toc       获取目录树
 * 3. GET /repos/:namespace/:slug/docs/:id  获取文档详情(含正文)
 *
 * 频率限制：约600次/小时（带Token时）
 */
@Slf4j
@Component
public class YuqueClient {

    private final RestTemplate restTemplate;
    private final String token;
    private final String baseUrl;

    public YuqueClient(
            RestTemplate restTemplate,
            @Value("${yuque.token:}") String token,
            @Value("${yuque.base-url:https://www.yuque.com/api/v2}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.token = token;
        this.baseUrl = baseUrl;
    }

    /**
     * 获取指定用户/组织的知识库列表
     *
     * API: GET /users/:login/repos
     *
     * @param namespace 语雀用户名或组织名
     * @return 知识库列表
     */
    public List<YuqueRepoDto> listRepos(String namespace) {
        String url = baseUrl + "/users/" + namespace + "/repos";
        log.info("[语雀] 获取知识库列表: namespace={}", namespace);

        try {
            ResponseEntity<YuqueApiResponse<List<YuqueRepoDto>>> resp =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            new HttpEntity<>(buildHeaders()),
                            new ParameterizedTypeReference<>() {}
                    );

            YuqueApiResponse<List<YuqueRepoDto>> body = resp.getBody();
            if (body == null || !body.isOk()) {
                String msg = (body != null) ? body.getMessage() : "响应为空";
                throw new RuntimeException("语雀获取知识库列表失败: " + msg);
            }

            List<YuqueRepoDto> repos = body.getData();
            log.info("[语雀] 获取到 {} 个知识库", repos.size());
            return repos;

        } catch (HttpClientErrorException e) {
            log.error("[语雀] 列表API错误: {} {}", e.getStatusCode(), e.getMessage());
            throw new RuntimeException(
                    "语雀API调用失败(" + e.getStatusCode() + "): "
                            + e.getResponseBodyAsString(), e);
        }
    }

    /**
     * 获取知识库目录树
     *
     * API: GET /repos/:namespace/:slug/toc
     *
     * @param namespace 命名空间
     * @param repoSlug  知识库slug
     * @return 目录树节点列表
     */
    public List<YuqueTocItem> getRepoToc(String namespace, String repoSlug) {
        String url = baseUrl + "/repos/" + namespace + "/" + repoSlug + "/toc";
        log.info("[语雀] 获取目录树: {}/{}", namespace, repoSlug);

        try {
            // TOC接口返回格式: { "data": [ {...}, {...} ] }
            // 没有ok字段，直接用data判断即可
            ResponseEntity<String> rawResp = restTemplate.exchange(
                    url, HttpMethod.GET,
                    new HttpEntity<>(buildHeaders()),
                    String.class
            );

            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root =
                    mapper.readTree(rawResp.getBody());

            com.fasterxml.jackson.databind.JsonNode dataNode = root.get("data");
            if (dataNode == null || !dataNode.isArray()) {
                throw new RuntimeException("语雀TOC响应格式异常");
            }

            List<YuqueTocItem> toc = mapper.readerForListOf(YuqueTocItem.class)
                    .readValue(dataNode);
            log.info("[语雀] 目录树共 {} 个节点", toc.size());
            return toc;

        } catch (HttpClientErrorException e) {
            log.error("[语雀] TOC API错误: {} {}", e.getStatusCode(), e.getMessage());
            throw new RuntimeException("语雀获取目录树失败(" + e.getStatusCode() + ")", e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("[语雀] TOC解析失败", e);
            throw new RuntimeException("语雀TOC解析失败: " + e.getMessage(), e);
        }
    }

//    public List<YuqueTocItem> getRepoToc(String namespace, String repoSlug) {
//        String url = baseUrl + "/repos/" + namespace + "/" + repoSlug + "/toc";
//        log.info("[语雀] 获取目录树: {}/{}", namespace, repoSlug);
//
//        try {
//            // 先用String接收，打印原始JSON看看格式
//            ResponseEntity<String> rawResp = restTemplate.exchange(
//                    url,
//                    HttpMethod.GET,
//                    new HttpEntity<>(buildHeaders()),
//                    String.class
//            );
//
//            log.info("[语雀] TOC原始响应: {}", rawResp.getBody());
//
//            // 再手动解析
//            com.fasterxml.jackson.databind.ObjectMapper mapper =
//                    new com.fasterxml.jackson.databind.ObjectMapper();
//            com.fasterxml.jackson.databind.JsonNode root =
//                    mapper.readTree(rawResp.getBody());
//
//            // 检查ok字段
//            boolean ok = root.has("ok") && root.get("ok").asBoolean();
//            log.info("[语雀] TOC ok={}", ok);
//
//            // data字段
//            com.fasterxml.jackson.databind.JsonNode dataNode = root.get("data");
//            if (dataNode == null) {
//                throw new RuntimeException("语雀TOC响应中没有data字段");
//            }
//
//            // data可能是数组，也可能是包含toc字段的对象
//            List<YuqueTocItem> toc;
//            if (dataNode.isArray()) {
//                // data直接是数组: { "data": [ {...}, {...} ] }
//                log.info("[语雀] data是数组，长度={}", dataNode.size());
//                toc = mapper.readerForListOf(YuqueTocItem.class)
//                        .readValue(dataNode);
//            } else if (dataNode.has("toc")) {
//                // data是对象: { "data": { "toc": [ {...}, {...} ] } }
//                log.info("[语雀] data.toc是数组");
//                toc = mapper.readerForListOf(YuqueTocItem.class)
//                        .readValue(dataNode.get("toc"));
//            } else {
//                log.warn("[语雀] 未知的data格式: {}", dataNode);
//                throw new RuntimeException("语雀TOC响应格式未知");
//            }
//
//            log.info("[语雀] 目录树共 {} 个节点", toc.size());
//            return toc;
//
//        } catch (HttpClientErrorException e) {
//            log.error("[语雀] TOC HTTP错误: {} {}", e.getStatusCode(), e.getMessage());
//            throw new RuntimeException("语雀获取目录树失败(" + e.getStatusCode() + ")", e);
//        } catch (RuntimeException e) {
//            throw e;
//        } catch (Exception e) {
//            log.error("[语雀] TOC解析失败", e);
//            throw new RuntimeException("语雀TOC响应解析失败: " + e.getMessage(), e);
//        }
//    }




    /**
     * 获取单篇文档详情（含Markdown正文）
     *
     * API: GET /repos/:namespace/:slug/docs/:id
     *
     * @param namespace 命名空间
     * @param repoSlug  知识库slug
     * @param docId     文档ID
     * @return 文档详情
     */
    public YuqueDocDetail getDocDetail(String namespace, String repoSlug, Long docId) {
        String url = baseUrl + "/repos/" + namespace + "/" + repoSlug + "/docs/" + docId;
        log.info("[语雀] 获取文档: {}/{}/doc={}", namespace, repoSlug, docId);

        try {
            // 先用String接收，看看原始JSON格式
            ResponseEntity<String> rawResp = restTemplate.exchange(
                    url, HttpMethod.GET,
                    new HttpEntity<>(buildHeaders()),
                    String.class
            );

            log.info("[语雀] 文档原始响应(前500字): {}",
                    rawResp.getBody() != null
                            ? rawResp.getBody().substring(0, Math.min(500, rawResp.getBody().length()))
                            : "null");

            // 手动解析
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root =
                    mapper.readTree(rawResp.getBody());

            com.fasterxml.jackson.databind.JsonNode dataNode = root.get("data");
            if (dataNode == null) {
                throw new RuntimeException("语雀文档响应中没有data字段");
            }

            YuqueDocDetail detail = mapper.treeToValue(dataNode, YuqueDocDetail.class);
            log.info("[语雀] 文档解析成功: title={}, wordCount={}",
                    detail.getTitle(), detail.getWordCount());
            return detail;

        } catch (HttpClientErrorException.TooManyRequests e) {
            log.warn("[语雀] 频率限制(429)");
            throw new YuqueRateLimitException("语雀API频率限制");

        } catch (HttpClientErrorException e) {
            log.error("[语雀] 文档API错误: {} {}", e.getStatusCode(), e.getMessage());
            throw new RuntimeException("语雀获取文档失败(" + e.getStatusCode() + ")", e);

        } catch (ResourceAccessException e) {
            log.error("[语雀] 请求超时");
            throw new RuntimeException("语雀API请求超时", e);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("[语雀] 文档解析失败", e);
            throw new RuntimeException("语雀文档解析失败: " + e.getMessage(), e);
        }
    }


    /**
     * 构建请求头
     */
    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        if (token != null && !token.isBlank()) {
            headers.set("X-Auth-Token", token);
        }
        return headers;
    }
}
