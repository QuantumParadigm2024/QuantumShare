package com.qp.quantum_share.controller;

import java.util.List;

import com.qp.quantum_share.dto.FacebookPageDetails;
import com.qp.quantum_share.response.ResponseStructure;
import com.qp.quantum_share.services.SocialMediaPosts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.qp.quantum_share.dao.QuantumShareUserDao;
import com.qp.quantum_share.dto.QuantumShareUser;
import com.qp.quantum_share.helper.CommonMethod;

@RestController
@RequestMapping("/test")
public class TestClass {
    @Autowired
    SocialMediaPosts socialMediaPosts;

    @Autowired
    CommonMethod commonMethod;

    @Autowired
    QuantumShareUserDao userDao;

//    @GetMapping("/fetch/posts")
//    public ResponseEntity<ResponseStructure<String>> test(@RequestHeader HttpHeaders headers) {
//        Object userId = commonMethod.validateToken(headers.get("authorization").get(0));
//        int id = Integer.parseInt(userId.toString());
//        QuantumShareUser user = userDao.fetchUser(id);
//        List<FacebookPageDetails> pageDetails = user.getSocialAccounts().getFacebookUser().getPageDetails();
//        FacebookPageDetails page = pageDetails.get(0);
//        return socialMediaPosts.getAllPosts(15, page.getFbPageId(), page.getFbPageAceessToken());
//    }

//    @GetMapping("/analytics")
//    public ResponseEntity<ResponseStructure<String>> test1(@RequestParam String postId, @RequestParam String type) {
//
//        return socialMediaPosts.getAnalytics(postId, type, "");
//    }
}
