package projectPortal.com.Entity;

import jakarta.persistence.*;
import lombok.Data;
import projectPortal.com.enums.CollegeStatus;

@Entity
@Table(name = "college")
@Data
public class CollegeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long collegeId;

    @OneToOne(optional = false)
    @JoinColumn(name = "college_admin_id")
    private CollegeAdminEntity collegeAdmin;

    @Column(name = "address",nullable = false)
    private String address;

    @Column(name = "city",nullable = false)
    private String city;

    @Column(name = "state",nullable = false)
    private String state;

    @Column(name = "pincode",nullable = false)
    private String pincode;

    @Column(name = "phone",nullable = false)
    private String phone;

    @Column(name="college_name",nullable = false, unique = true)
    private String collegeName;

    @Column(name="offical_domain",nullable = false, unique = true)
    private String officialDomain;

    @Enumerated(EnumType.STRING)
    @Column(name="CollegeStatus",nullable = false)
    private CollegeStatus status;
}
