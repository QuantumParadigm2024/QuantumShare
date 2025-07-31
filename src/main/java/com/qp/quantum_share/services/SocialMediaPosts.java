package com.qp.quantum_share.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Autowired
    ObjectMapper objectMapper;

    public List<Object> getFacebookAllPosts(int limit, String pageId, String accessToken) {
        String cacheKey = "fbPosts:" + pageId;
        List<Object> cachedPosts = (List<Object>) redisTemplate.opsForValue().get(cacheKey);
        if (cachedPosts != null && !cachedPosts.isEmpty()) {
            return cachedPosts;
        }
        try {
            String apiUrl = facebookbaseUrl + pageId + "/feed?fields=id,full_picture,created_time,permalink_url,message,attachments&limit=" + limit;
            HttpHeaders httpHeaders = new HttpHeaders();
            List<Object> list = new ArrayList<>();
            httpHeaders.setBearerAuth(accessToken);
            HttpEntity<String> requestEntity = new HttpEntity<>(httpHeaders);
            ResponseEntity<JsonNode> response = restTemplate.exchange(apiUrl, HttpMethod.GET, requestEntity, JsonNode.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode body = response.getBody();
                if (body != null) {
                    JsonNode data = body.get("data");
                    if (data.isArray()) {
                        for (JsonNode post : data) {
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
                            list.add(modifiedPost);
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

    public Map<String, Object> getFacebookAnalytics(String postId, String type, String accessToken) {
        try {
            Map<String, Object> responseData = new HashMap<>();
            String metric = "";
            if (type.contains("photo") || type.contains("images") || type.contains("profile_media")) {
                metric = "post_reactions_by_type_total";
            } else if (type.contains("video")) {
                metric = "post_reactions_by_type_total,post_video_views";
            }

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

                //comment
//                String commentApi = facebookbaseUrl + postId + "/comments";
//                ResponseEntity<JsonNode> commentResponse = restTemplate.exchange(commentApi, HttpMethod.GET, requestEntity, JsonNode.class);
//
//                if (commentResponse.getStatusCode().is2xxSuccessful()) {
//                    JsonNode commentbody = commentResponse.getBody(); // 🔴 corrected from "body" to "data"
//                    if (commentbody != null) {
//                        JsonNode commentBody = commentbody.get("data");
//                        int commentCount = commentBody.size();
//                        responseData.put("commentCount", commentCount);
//
//                        List<Map<String, String>> commentList = new ArrayList<>();
//                        for (JsonNode comment : commentBody) {
//                            Map<String, String> commentInfo = new HashMap<>();
//                            commentInfo.put("name", comment.has("from") ? comment.path("from").path("name").asText() : "Anonymous");
//                            commentInfo.put("message", comment.path("message").asText());
//                            commentInfo.put("time", comment.path("created_time").asText());
//                            commentList.add(commentInfo);
//                        }
//
//                        responseData.put("commentData", commentList);
//                    }
//                }

                // shares
                String postUrl = facebookbaseUrl + postId + "?fields=shares";
                ResponseEntity<JsonNode> postResponse = restTemplate.exchange(postUrl, HttpMethod.GET, requestEntity, JsonNode.class);
                if (postResponse.getStatusCode().is2xxSuccessful()) {
                    JsonNode postData = postResponse.getBody();
                    int sharesCount = postData.path("shares").path("count").asInt(0);
                    responseData.put("shares", sharesCount);
                }
                return responseData;
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            throw new CommonException(e.getMessage());
        }
    }

    public Object getAllInstagramPosts(String instagramId, String accessToken) {
        System.out.println("getAllInstagramPosts method");
        String cacheKey = "instaPosts:" + instagramId;
        String cachedPosts = (String) redisTemplate.opsForValue().get(cacheKey);

        if (cachedPosts != null) {
            try {
                return objectMapper.readTree(cachedPosts);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
//            return cachedPosts;
        }
//            try {
//                return objectMapper.readTree(cachedPosts);
//            } catch (JsonProcessingException e) {
//                throw new RuntimeException(e);
//            }
//        }
        try {
            System.out.println("try");
            String apiUrl = facebookbaseUrl + instagramId + "/media?fields=id,caption,media_type,media_url,thumbnail_url,timestamp,permalink&limit=15";
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<JsonNode> responseEntity = restTemplate.exchange(apiUrl, HttpMethod.GET, requestEntity, JsonNode.class);
            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                JsonNode body = responseEntity.getBody();
                if (body != null) {
                    JsonNode data = body.get("data");
                    redisTemplate.opsForValue().set(cacheKey, data.toString(), Duration.ofHours(1));
                    return data;
                }
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            throw new CommonException(e.getMessage());
        }
    }

    public Map<String, Object> getInstagramAnalytics(String postId, String type, String accessToken) {
        try {
            ResponseStructure<String> structure = new ResponseStructure<>();
            String metric = "";
            if (type.toLowerCase().contains("image")) {
                metric = "likes,saved,shares,reach";
            } else if (type.toLowerCase().contains("video")) {
                metric = "likes,saved,shares,ig_reels_video_view_total_time,reach";
            }
            String apiurl = facebookbaseUrl + postId + "/insights?metric=" + metric;
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<JsonNode> response = restTemplate.exchange(apiurl, HttpMethod.GET, requestEntity, JsonNode.class);
            Map<String, Object> map = new HashMap<>();
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode body = response.getBody();
                if (body != null) {
                    JsonNode data = body.get("data");
                    for (JsonNode node : data) {
                        String name = node.path("name").asText();
                        String value = node.path("values").get(0).path("value").asText();
                        map.put(name, value);
                    }
                }
            }

            //comments
            String commentApi = facebookbaseUrl + postId + "/comments?fields=id,text,username,timestamp";
            ResponseEntity<JsonNode> responseEntity = restTemplate.exchange(commentApi, HttpMethod.GET, requestEntity, JsonNode.class);
            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                JsonNode body = responseEntity.getBody();
                if (body != null) {
                    JsonNode data = body.get("data");
                    map.put("commentData", data);
                }
            }
            return map;
        } catch (Exception e) {
            e.printStackTrace();
            throw new CommonException(e.getMessage());
        }
    }
}
