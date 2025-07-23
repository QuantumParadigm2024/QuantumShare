package com.qp.quantum_share.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.qp.quantum_share.exception.CommonException;
import com.qp.quantum_share.response.ResponseStructure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SocialMediaPosts {
    String facebookbaseUrl = "https://graph.facebook.com/v22.0/";

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public List<Object> getFacebookAllPosts(int limit, String pageId, String accessToken) {
        String cacheKey = "fbPosts:" + pageId;
        List<Object> cachedPosts = (List<Object>) redisTemplate.opsForValue().get(cacheKey);
        if (cachedPosts != null && !cachedPosts.isEmpty()) {
            return cachedPosts;
        }
        try {
            String apiUrl = facebookbaseUrl + pageId + "/feed?limit=" + limit;
            HttpHeaders httpHeaders = new HttpHeaders();
            List<Object> list = new ArrayList<>();
            httpHeaders.setBearerAuth(accessToken);
            HttpEntity<String> requestEntity = new HttpEntity<>(httpHeaders);
            ResponseEntity<JsonNode> response = restTemplate.exchange(apiUrl, HttpMethod.GET, requestEntity, JsonNode.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode body = response.getBody();
                JsonNode dataArray = body.get("data");
                if (dataArray.isArray()) {
                    for (JsonNode post : dataArray) {
                        String postId = post.get("id").asText();
                        JsonNode details = getPostDetails(postId, accessToken);
                        if (details != null) {
                            list.add(details);
                        }
                    }
                }
                redisTemplate.opsForValue().set(cacheKey, list, Duration.ofHours(1));
                return list;
            }
            return null;
        } catch (Exception e) {
            throw new CommonException(e.getMessage());
        }
    }

    public JsonNode getPostDetails(String postId, String accessToken) {
        try {
            String apiUrl = facebookbaseUrl + postId + "?fields=id,full_picture,created_time,permalink_url,message,attachments";
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<JsonNode> response = restTemplate.exchange(apiUrl, HttpMethod.GET, requestEntity, JsonNode.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode post = response.getBody();
                ObjectNode modifiedPost = JsonNodeFactory.instance.objectNode();

                modifiedPost.set("id", post.path("id"));
                modifiedPost.set("full_picture", post.path("full_picture"));
                modifiedPost.set("created_time", post.path("created_time"));
                modifiedPost.set("permalink_url", post.path("permalink_url"));
                modifiedPost.set("message", post.path("message"));

                String type = "";
                if (post.has("attachments")) {
                    JsonNode attachments = post.path("attachments").path("data");
                    if (attachments.isArray() && !attachments.isEmpty()) {
                        type = attachments.get(0).path("type").asText("");
                    }
                }
                modifiedPost.put("type", type);
                return modifiedPost;
            }
            return null;
        } catch (Exception e) {
            throw new CommonException(e.getMessage());
        }
    }

    public Map<String, Object> getFacebookAnalytics(String postId, String type, String accessToken) {
        try {
            Map<String, Object> responseData = new HashMap<>();
            String metric = "";
            if (type.contains("photo") || type.contains("images")) {
                metric = "post_reactions_by_type_total";
            } else if (type.contains("video")) {
                metric = "post_reactions_by_type_total,post_video_views";
            }
            System.out.println(metric);
            //reactions
            String apiUrl = facebookbaseUrl + postId + "/insights?metric=" + metric;
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<JsonNode> response = restTemplate.exchange(apiUrl, HttpMethod.GET, requestEntity, JsonNode.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode body = response.getBody();
                if (body != null && body.has("data")) {
                    for (JsonNode metricNode : body.get("data")) {
                        String name = metricNode.path("name").asText();
                        String period = metricNode.path("period").asText();
                        JsonNode valueNode = metricNode.path("values").get(0).path("value");

                        if (name.equals("post_reactions_by_type_total")) {
                            if (valueNode.isObject()) {
                                valueNode.fields().forEachRemaining(entry -> {
                                    responseData.put(entry.getKey(), entry.getValue().asInt());
                                });
                            }
                        } else if (name.equals("post_video_views") && period.equals("lifetime")) {
                            responseData.put("video_views", valueNode.asInt());
                        }
                    }
                }
                System.out.println("reactions = " + response.getBody());

                //comment
                String commentApi = facebookbaseUrl + postId + "/comments";
                ResponseEntity<JsonNode> commentResponse = restTemplate.exchange(commentApi, HttpMethod.GET, requestEntity, JsonNode.class);

                if (commentResponse.getStatusCode().is2xxSuccessful()) {
                    JsonNode commentBody = commentResponse.getBody().path("data"); // 🔴 corrected from "body" to "data"
                    int commentCount = commentBody.size();
                    responseData.put("commentCount", commentCount);

                    List<Map<String, String>> commentList = new ArrayList<>();
                    for (JsonNode comment : commentBody) {
                        Map<String, String> commentInfo = new HashMap<>();
                        commentInfo.put("name", comment.has("from") ? comment.path("from").path("name").asText() : "Anonymous");
                        commentInfo.put("message", comment.path("message").asText());
                        commentInfo.put("time", comment.path("created_time").asText());
                        commentList.add(commentInfo);
                    }

                    responseData.put("commentData", commentList);
                    System.out.println("comment = " + commentResponse.getBody());
                }

                // shares
                String postUrl = facebookbaseUrl + postId + "?fields=shares";
                ResponseEntity<JsonNode> postResponse = restTemplate.exchange(postUrl, HttpMethod.GET, requestEntity, JsonNode.class);
                if (postResponse.getStatusCode().is2xxSuccessful()) {
                    JsonNode postData = postResponse.getBody();
                    int sharesCount = postData.path("shares").path("count").asInt(0);
                    responseData.put("shares", sharesCount);
                    System.out.println("postdata = " + postData);
                }
                return responseData;
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            throw new CommonException(e.getMessage());
        }
    }

//    public void getAllInstagramPosts(String instagramId, String accessToken) {
//        String apiUrl = facebookbaseUrl + instagramId + "/media?fields=id,caption,media_type,media_url,thumbnail_url,timestamp,permalink&limit=15";
//        HttpHeaders headers=new HttpHeaders();
//        headers.setBearerAuth(accessToken);
//        HttpEntity<String>
//    }
}
