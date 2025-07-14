package com.qp.quantum_share.dto;

import lombok.Data;

@Data
public class DraftsResponseDto {
    private int draftId;
    private String caption;
    private String title;
    private String visibility;
    private String userTimeZone;
    private String boardName;
    private String postUrl;
    private String contentType;
    private String fileName;

    public DraftsResponseDto(int draftId, String caption, String title, String visibility,
                             String userTimeZone, String boardName, String postUrl, String contentType, String fileName) {
        this.draftId = draftId;
        this.caption = caption;
        this.title = title;
        this.visibility = visibility;
        this.userTimeZone = userTimeZone;
        this.boardName = boardName;
        this.postUrl = postUrl;
        this.contentType=contentType;
        this.fileName=fileName;
    }
}
