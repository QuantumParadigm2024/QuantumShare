package com.qp.quantum_share.dto;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Drafts {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int draftId;
    @Column(length = 2000)
    private String caption;
    private String title;
    private String visibility;
    private String userTimeZone;
    private String boardName;
    private String postUrl;
    private String contentType;
    private String fileName;

    @ManyToOne
    QuantumShareUser user;
}
