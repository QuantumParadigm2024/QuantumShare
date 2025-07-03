package com.qp.quantum_share.dto;

import lombok.Data;

@Data
public class MediaPost {
	private String mediaPlatform;
	private String caption;
	private String title;
	private String visibility;
	private String scheduledTime;
	private String userTimeZone;
	private String boardName;
}
