package projectPortal.com.otp;

import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class OtpGenerator {
    public int generateOtp(){
        Random random=new Random();
        return random.nextInt(111111,666666);
    }
}
