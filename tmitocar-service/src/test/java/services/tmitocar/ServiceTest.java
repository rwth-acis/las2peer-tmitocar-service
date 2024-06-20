package services.tmitocar;

import org.springframework.boot.test.context.SpringBootTest;

import services.tmitocar.controller.TmitocarServiceController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Example Test Class demonstrating a basic JUnit test structure.
 *
 */
@SpringBootTest
public class ServiceTest {

	// private static ByteArrayOutputStream logStream;

	// private static final String testPass = "adamspass";

	// private static final String mainPath = "tmitocar/";

	void contextLoads() {
	}
	/**
	 * 
	 * Test the example method that consumes one path parameter which we give the value "testInput" in this test.
	 * 
	 */
	// @Test
	// public void testPost() {
	// 	try {
	// 		MiniClient client = new MiniClient();
	// 		client.setConnectorEndpoint(connector.getHttpEndpoint());
	// 		client.setLogin(testAgent.getIdentifier(), testPass);

	// 		// testInput is the pathParam
	// 		String content = "";
	// 		try {
	// 			content = new String(Files.readAllBytes(Paths.get("tmitocar/example.txt")));
	// 		} catch (IOException e) {
	// 			e.printStackTrace();
	// 		}

	// 		ClientResponse result = client.sendRequest("POST", mainPath + "test", content);
	// 		Assert.assertEquals(200, result.getHttpCode());
	// 	} catch (Exception e) {
	// 		e.printStackTrace();
	// 		Assert.fail(e.toString());
	// 	}
	// }

}
