package com.qp.quantum_share.controller;

import com.qp.quantum_share.dto.Drafts;
import com.qp.quantum_share.helper.CommonMethod;
import com.qp.quantum_share.response.ResponseStructure;
import com.qp.quantum_share.services.DraftService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/quantum-share")
public class DraftController {

    @Autowired
    CommonMethod commonMethod;

    @Autowired
    DraftService draftService;


    @PostMapping("/post/saveDraft")
    public ResponseEntity<ResponseStructure<String>> saveDraft(MultipartFile mediaFile, @ModelAttribute Drafts mediaPost, @RequestHeader HttpHeaders request) {
        Object userId = commonMethod.validateToken(request.get("authorization").get(0));
        return draftService.saveDraft(Integer.parseInt(userId.toString()), mediaPost, mediaFile);
    }

    @GetMapping("get/drafts")
    public ResponseEntity<ResponseStructure<String>> getDraft(@RequestHeader HttpHeaders request) {
        Object userId = commonMethod.validateToken(request.get("authorization").get(0));
        return draftService.getDraft(Integer.parseInt(userId.toString()));
    }

    @DeleteMapping("/delete/draft")
    public ResponseEntity<?> deleteDraft(@RequestParam int draftId, @RequestHeader HttpHeaders request) {
        Object userId = commonMethod.validateToken(request.get("authorization").get(0));
        return draftService.deleteDraft(Integer.parseInt(userId.toString()), draftId);
    }

    @PostMapping("/update/draft")
    public ResponseEntity<?> modifyDraft(@RequestParam int draftId, @RequestHeader HttpHeaders request, @ModelAttribute Drafts drafts) {
        Object userId = commonMethod.validateToken(request.get("authorization").get(0));
        return draftService.modifyDraft(Integer.parseInt(userId.toString()), draftId, drafts);
    }

    @PostMapping("post/draft/facebook")
    public ResponseEntity<?> postDraftFacebook(@RequestParam int draftId, @RequestHeader HttpHeaders request) {
        Object userId = commonMethod.validateToken(request.get("authorization").get(0));
        return draftService.postDraftOnFacebook(Integer.parseInt(userId.toString()), draftId);
    }

    @PostMapping("post/draft/instagram")
    public ResponseEntity<?> postDraftInstagram(@RequestParam int draftId, @RequestHeader HttpHeaders request) {
        Object userId = commonMethod.validateToken(request.get("authorization").get(0));
        return draftService.postDraftOnInstagram(Integer.parseInt(userId.toString()), draftId);
    }

    @PostMapping("post/draft/telegram")
    public ResponseEntity<?> postDraftTelegram(@RequestParam int draftId, @RequestHeader HttpHeaders request) {
        Object userId = commonMethod.validateToken(request.get("authorization").get(0));
        return draftService.postDraftOnTelegram(Integer.parseInt(userId.toString()), draftId);
    }

    @PostMapping("post/draft/linkedIn")
    public ResponseEntity<?> postDraftLinkedIn(@RequestParam int draftId, @RequestHeader HttpHeaders request) {
        Object userId = commonMethod.validateToken(request.get("authorization").get(0));
        return draftService.postDraftOnLinkedIn(Integer.parseInt(userId.toString()), draftId);
    }

    @PostMapping("post/draft/youtube")
    public ResponseEntity<?> postDraftYoutube(@RequestParam int draftId, @RequestHeader HttpHeaders request) {
        Object userId = commonMethod.validateToken(request.get("authorization").get(0));
        return draftService.postDraftOnYouTube(Integer.parseInt(userId.toString()), draftId);
    }

    @PostMapping("post/draft/reddit")
    public ResponseEntity<?> postDraftReddit(@RequestParam int draftId, @RequestHeader HttpHeaders request, @RequestParam(name = "sr") String subreddit) {
        Object userId = commonMethod.validateToken(request.get("authorization").get(0));
        return draftService.postDraftOnReddit(Integer.parseInt(userId.toString()), draftId, subreddit);
    }

    @PostMapping("post/draft/pinterest")
    public ResponseEntity<?> postDraftPinterest(@RequestParam int draftId, @RequestHeader HttpHeaders request) {
        Object userId = commonMethod.validateToken(request.get("authorization").get(0));
        return draftService.postDraftOnPinterest(Integer.parseInt(userId.toString()), draftId);
    }


}
