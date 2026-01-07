package projectPortal.com.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import projectPortal.com.enums.CollegeStatus;


@Data
@AllArgsConstructor
public class CollegeUpdateRequest {
    private String collegeName;
    private String officialDomain;
    private String adminEmail;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private String phone;

}
