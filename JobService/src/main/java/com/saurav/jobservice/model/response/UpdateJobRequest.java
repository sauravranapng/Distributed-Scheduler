package com.saurav.jobservice.model.response;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.saurav.jobservice.model.request.ScheduleRequest;
import com.saurav.jobservice.model.request.UpdateEmailJobRequest;
import com.saurav.jobservice.model.request.UpdateHttpJobRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "jobType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = UpdateHttpJobRequest.class, name = "HTTP"),
        @JsonSubTypes.Type(value = UpdateEmailJobRequest.class, name = "EMAIL")
})
public abstract class UpdateJobRequest extends ScheduleRequest {
}