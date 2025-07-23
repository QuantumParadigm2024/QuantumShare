package com.qp.quantum_share.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.qp.quantum_share.dao.QuantumShareUserDao;
import com.qp.quantum_share.dto.*;
import com.qp.quantum_share.exception.CommonException;
import com.qp.quantum_share.helper.InMemoryMultipartFile;
import com.qp.quantum_share.helper.PostOnServer;
import com.qp.quantum_share.repository.DraftRepository;
import com.qp.quantum_share.response.ResponseStructure;
import com.qp.quantum_share.response.ResponseWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

@Service
public class DraftService {

    @Autowired
    QuantumShareUserDao userDao;

    @Autowired
    PostOnServer postOnServer;

    @Autowired
    DraftRepository draftRepository;

    @Autowired
    PostService postService;

    @Autowired
    InstagramService instagramService;


    public ResponseEntity<ResponseStructure<String>> saveDraft(int userId, Drafts mediaPost, MultipartFile file) {
        try {
            System.out.println("fil name : " + file.getOriginalFilename() + " " + file.getContentType());
            ResponseStructure<String> structure = new ResponseStructure<String>();
            QuantumShareUser user = userDao.fetchUser(userId);
            if (user == null) {
                structure.setCode(HttpStatus.NOT_FOUND.value());
                structure.setMessage("user doesn't exists, please login");
                structure.setStatus("error");
                structure.setData(null);
                structure.setPlatform(null);
                return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
            }
            System.out.println("file ** " + file.isEmpty());
            if (file.isEmpty()) {
                structure.setCode(HttpStatus.BAD_REQUEST.value());
                structure.setMessage("File not supported or File doesn't exists");
                structure.setStatus("error");
                structure.setData(null);
                structure.setPlatform(null);
                return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.NOT_FOUND);
            }
            String postUrl = postOnServer.uploadFile(new MultipartFile[]{file}, "qs_drafts/").get(0);
            mediaPost.setPostUrl(postUrl);
            mediaPost.setUser(user);
            mediaPost.setContentType(file.getContentType());
            mediaPost.setFileName(file.getOriginalFilename());
            draftRepository.save(mediaPost);

            structure.setCode(HttpStatus.OK.value());
            structure.setMessage("Draft Saved Successfully");
            structure.setStatus("sucess");
            return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            throw new CommonException(e.getMessage());
        }
    }

    public ResponseEntity<ResponseStructure<String>> getDraft(int userId) {
        try {
            ResponseStructure<String> structure = new ResponseStructure<>();
            List<DraftsResponseDto> drafts = draftRepository.findByUserId(userId);
            structure.setStatus("success");
            structure.setCode(HttpStatus.OK.value());
            structure.setData(drafts);
            return new ResponseEntity<>(structure, HttpStatus.OK);
        } catch (Exception e) {
            throw new CommonException(e.getMessage());
        }
    }

    public ResponseEntity<?> postDraftOnFacebook(int userId, int draftId) {
        Drafts draft = draftRepository.findById(draftId).orElse(null);
        ResponseStructure<String> structure = new ResponseStructure<>();
        if (draft == null) {
            structure.setCode(HttpStatus.NOT_FOUND.value());
            structure.setMessage("Draft not Exists");
            structure.setStatus("error");
            return new ResponseEntity<>(structure, HttpStatus.NOT_FOUND);
        }
        MultipartFile file = downloadFileFromUrl(draft);
        MediaPost post = new MediaPost();
        post.setCaption(draft.getCaption());
        post.setTitle(draft.getTitle());
        post.setVisibility(draft.getVisibility());
        post.setBoardName(draft.getBoardName());
        post.setUserTimeZone(draft.getUserTimeZone());
        post.setMediaPlatform("facebook");

        return postService.postOnFb(post, file, userDao.fetchUser(userId), userId);
    }

    public ResponseEntity<?> postDraftOnInstagram(int userId, int draftId) {
        Drafts draft = draftRepository.findById(draftId).orElse(null);
        ResponseStructure<String> structure = new ResponseStructure<>();
        if (draft == null) {
            structure.setCode(HttpStatus.NOT_FOUND.value());
            structure.setMessage("Draft not Exists");
            structure.setStatus("error");
            return new ResponseEntity<>(structure, HttpStatus.NOT_FOUND);
        }
        MultipartFile file = downloadFileFromUrl(draft);
        MediaPost post = new MediaPost();
        post.setCaption(draft.getCaption());
        post.setTitle(draft.getTitle());
        post.setVisibility(draft.getVisibility());
        post.setBoardName(draft.getBoardName());
        post.setUserTimeZone(draft.getUserTimeZone());
        post.setMediaPlatform("instagram");

        QuantumShareUser user = userDao.fetchUser(userId);
        SocialAccounts socialAccounts = user.getSocialAccounts();
        if (socialAccounts == null || socialAccounts.getInstagramUser() == null) {
            structure.setMessage("Please connect your Instagram account");
            structure.setCode(HttpStatus.NOT_FOUND.value());
            structure.setPlatform("instagram");
            structure.setStatus("error");
            structure.setData(null);
            return new ResponseEntity<>(structure, HttpStatus.NOT_FOUND);
        }
        return instagramService.postMediaToPage(post, new MultipartFile[]{file}, socialAccounts.getInstagramUser(), userId, draft.getPostUrl());
    }

    public ResponseEntity<?> postDraftOnTelegram(int userId, int draftId) {
        Drafts draft = draftRepository.findById(draftId).orElse(null);
        ResponseStructure<String> structure = new ResponseStructure<>();
        if (draft == null) {
            structure.setCode(HttpStatus.NOT_FOUND.value());
            structure.setMessage("Draft not Exists");
            structure.setStatus("error");
            return new ResponseEntity<>(structure, HttpStatus.NOT_FOUND);
        }
        MultipartFile file = downloadFileFromUrl(draft);
        MediaPost post = new MediaPost();
        post.setCaption(draft.getCaption());
        post.setTitle(draft.getTitle());
        post.setVisibility(draft.getVisibility());
        post.setBoardName(draft.getBoardName());
        post.setUserTimeZone(draft.getUserTimeZone());
        post.setMediaPlatform("telegram");

        return postService.postOnTelegram(post, file, userDao.fetchUser(userId), userId);
    }

    public ResponseEntity<?> postDraftOnLinkedIn(int userId, int draftId) {
        Drafts draft = draftRepository.findById(draftId).orElse(null);
        ResponseStructure<String> structure = new ResponseStructure<>();
        if (draft == null) {
            structure.setCode(HttpStatus.NOT_FOUND.value());
            structure.setMessage("Draft not Exists");
            structure.setStatus("error");
            return new ResponseEntity<>(structure, HttpStatus.NOT_FOUND);
        }
        MultipartFile file = downloadFileFromUrl(draft);
        MediaPost post = new MediaPost();
        post.setCaption(draft.getCaption());
        post.setTitle(draft.getTitle());
        post.setVisibility(draft.getVisibility());
        post.setBoardName(draft.getBoardName());
        post.setUserTimeZone(draft.getUserTimeZone());
        post.setMediaPlatform("LinkedIn");
        QuantumShareUser user = userDao.fetchUser(userId);
        return postService.prePostOnLinkedIn(post, file, user, userId);
    }

    public ResponseEntity<?> postDraftOnYouTube(int userId, int draftId) {
        Drafts draft = draftRepository.findById(draftId).orElse(null);
        ResponseStructure<String> structure = new ResponseStructure<>();
        if (draft == null) {
            structure.setCode(HttpStatus.NOT_FOUND.value());
            structure.setMessage("Draft not Exists");
            structure.setStatus("error");
            return new ResponseEntity<>(structure, HttpStatus.NOT_FOUND);
        }
        MultipartFile file = downloadFileFromUrl(draft);
        MediaPost post = new MediaPost();
        post.setCaption(draft.getCaption());
        post.setTitle(draft.getTitle());
        post.setVisibility("public");
        post.setBoardName(draft.getBoardName());
        post.setUserTimeZone(draft.getUserTimeZone());
        post.setMediaPlatform("youtube");
        QuantumShareUser user = userDao.fetchUser(userId);
        return postService.postOnYoutube(post, file, user.getSocialAccounts(), userId);
    }

    public ResponseEntity<?> postDraftOnReddit(int userId, int draftId, String subReddit) {
        Drafts draft = draftRepository.findById(draftId).orElse(null);
        ResponseStructure<String> structure = new ResponseStructure<>();
        if (draft == null) {
            structure.setCode(HttpStatus.NOT_FOUND.value());
            structure.setMessage("Draft not Exists");
            structure.setStatus("error");
            return new ResponseEntity<>(structure, HttpStatus.NOT_FOUND);
        }
        MultipartFile file = downloadFileFromUrl(draft);
        MediaPost post = new MediaPost();
        post.setCaption(draft.getCaption());
        post.setTitle(draft.getTitle());
        post.setVisibility(draft.getVisibility());
        post.setBoardName(draft.getBoardName());
        post.setUserTimeZone(draft.getUserTimeZone());
        post.setMediaPlatform("Reddit");
        QuantumShareUser user = userDao.fetchUser(userId);
        return postService.PostOnReddit(subReddit, user.getSocialAccounts(), post, new MultipartFile[]{file}, user);
    }

    public ResponseEntity<?> postDraftOnPinterest(int userId, int draftId) {
        Drafts draft = draftRepository.findById(draftId).orElse(null);
        ResponseStructure<String> structure = new ResponseStructure<>();
        if (draft == null) {
            structure.setCode(HttpStatus.NOT_FOUND.value());
            structure.setMessage("Draft not Exists");
            structure.setStatus("error");
            return new ResponseEntity<>(structure, HttpStatus.NOT_FOUND);
        }
        MultipartFile file = downloadFileFromUrl(draft);
        MediaPost post = new MediaPost();
        post.setCaption(draft.getCaption());
        post.setTitle(draft.getTitle());
        post.setVisibility(draft.getVisibility());
        post.setBoardName(draft.getBoardName());
        post.setUserTimeZone(draft.getUserTimeZone());
        post.setMediaPlatform("pinterest");
        QuantumShareUser user = userDao.fetchUser(userId);
        return postService.postOnPinterest(post, file, user.getSocialAccounts(), userId);
    }

    private MultipartFile downloadFileFromUrl(Drafts draft) {
        try {
            String fileUrl = draft.getPostUrl();
            fileUrl = fileUrl.replace(" ", "%20");
            URL url = new URL(fileUrl);
            try (InputStream in = url.openStream();
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                byte[] fileBytes = out.toByteArray();
                MultipartFile mediaFile = new InMemoryMultipartFile(draft.getFileName(), draft.getFileName(), draft.getContentType(), fileBytes);
                return mediaFile;
            }
        } catch (Exception e) {
            throw new CommonException(e.getMessage());
        }
    }

    public ResponseEntity<?> deleteDraft(int userId, int draftId) {
        try {
            ResponseStructure<String> structure = new ResponseStructure<>();
            Drafts draft = draftRepository.findById(draftId).orElse(null);
            if (draft == null) {
                structure.setCode(HttpStatus.NOT_FOUND.value());
                structure.setMessage("Draft not Exists");
                structure.setStatus("error");
                return new ResponseEntity<>(structure, HttpStatus.NOT_FOUND);
            }
            postOnServer.deleteFile(draft.getPostUrl(), "qs_drafts/");
            draftRepository.delete(draft);
            structure.setCode(HttpStatus.OK.value());
            structure.setMessage("Draft Removed Successfully");
            structure.setStatus("success");
            return new ResponseEntity<>(structure, HttpStatus.OK);
        } catch (Exception e) {
            throw new CommonException(e.getMessage());
        }
    }

    public ResponseEntity<?> modifyDraft(int userId, int draftId, Drafts drafts) {
        Drafts oldDrft = draftRepository.findById(draftId).orElse(null);
        ResponseStructure<String> structure = new ResponseStructure<>();
        if (oldDrft == null) {
            structure.setCode(HttpStatus.NOT_FOUND.value());
            structure.setMessage("Draft not Exists");
            structure.setStatus("error");
            return new ResponseEntity<>(structure, HttpStatus.NOT_FOUND);
        }
        if (drafts.getCaption() != null) {
            oldDrft.setCaption(drafts.getCaption());
        }
        if (drafts.getTitle() != null) {
            oldDrft.setTitle(drafts.getTitle());
        }
        if (drafts.getBoardName() != null) {
            oldDrft.setBoardName(drafts.getBoardName());
        }
        draftRepository.save(oldDrft);
        structure.setStatus("success");
        structure.setMessage("draft updated");
        structure.setCode(HttpStatus.OK.value());
        return new ResponseEntity<>(structure, HttpStatus.OK);
    }


}
