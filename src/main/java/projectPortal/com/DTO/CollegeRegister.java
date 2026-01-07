package projectPortal.com.DTO;


import lombok.Data;
import projectPortal.com.enums.CollegeStatus;

@Data
public class CollegeRegister {
    private String collegeName;
    private String officialDomain;
    private CollegeStatus status;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private String phone;

}
