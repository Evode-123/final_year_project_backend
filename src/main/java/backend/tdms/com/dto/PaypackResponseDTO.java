package backend.tdms.com.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class PaypackResponseDTO {
    private Integer amount;
    
    @JsonProperty("created_at")
    private String createdAt;
    
    private String kind;
    
    private String ref;
    
    private String status;
}