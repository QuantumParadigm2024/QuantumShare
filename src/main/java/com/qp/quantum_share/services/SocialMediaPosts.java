package com.qp.quantum_share.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.qp.quantum_share.exception.CommonException;
import com.qp.quantum_share.response.ResponseStructure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SocialMediaPosts {
    String baseUrl = "https://graph.facebook.com/v22.0/";

    @Autowired
    RestTemplate restTemplate;

    public ResponseEntity<ResponseStructure<String>> getAllPosts(int limit, String pageId, String accessToken) {
        ResponseStructure<String> structure = new ResponseStructure<>();
        String apiUrl = baseUrl + pageId + "/feed?limit=" + limit;
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
                    JsonNode postId = post.get("id");
                    JsonNode details = getPostDetails(postId, accessToken);
                    if (details != null) {
                        list.add(details);
                    }
                }
            }
            structure.setStatus("success");
            structure.setCode(HttpStatus.OK.value());
            structure.setData(list);
            return new ResponseEntity<>(structure, HttpStatus.OK);
        }
        structure.setStatus("error");
        structure.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        structure.setMessage("Something went wrong");
        return new ResponseEntity<>(structure, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public JsonNode getPostDetails(JsonNode postId, String accessToken) {
        try {
            String apiUrl = baseUrl + postId + "?fields=id,full_picture,created_time,permalink_url,message";
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<JsonNode> response = restTemplate.exchange(apiUrl, HttpMethod.GET, requestEntity, JsonNode.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
            return null;
        } catch (Exception e) {
            throw new CommonException(e.getMessage());
        }
    }
}
