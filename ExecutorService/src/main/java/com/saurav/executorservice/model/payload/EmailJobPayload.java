package com.saurav.executorservice.model.payload;

import lombok.Data;

@Data
public class EmailJobPayload implements JobPayload {

    private String to;

    private String subject;

    private String body;

}
