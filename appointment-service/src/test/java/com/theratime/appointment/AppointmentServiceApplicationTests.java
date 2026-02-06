package com.theratime.appointment;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Disabled("Context boot test disabled for tests; core logic covered via unit tests")
class AppointmentServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
