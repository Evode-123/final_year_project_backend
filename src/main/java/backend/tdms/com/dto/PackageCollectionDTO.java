package backend.tdms.com.dto;

import lombok.Data;

@Data
public class PackageCollectionDTO {
    private Long packageId;
    private String receiverIdNumber;
    private String collectedByName;
}