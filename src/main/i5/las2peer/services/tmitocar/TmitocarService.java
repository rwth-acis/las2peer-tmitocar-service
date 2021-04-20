package i5.las2peer.services.tmitocar;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.logging.Level;

import javax.imageio.ImageIO;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.nio.file.Paths;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.java_websocket.util.Base64;

import i5.las2peer.api.Context;
import i5.las2peer.api.ManualDeployment;
import i5.las2peer.api.logging.MonitoringEvent;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.restMapper.RESTService;
import i5.las2peer.restMapper.annotations.ServicePath;
import i5.las2peer.services.tmitocar.pojo.TmitocarText;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Contact;
import io.swagger.annotations.Info;
import io.swagger.annotations.License;
import io.swagger.annotations.SwaggerDefinition;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

import org.apache.pdfbox.*;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.pdfparser.PDFParser;

/**
 * las2peer-tmitocar-Service
 * 
 * This is a wrapper for tmitocar that uses the las2peer WebConnector for
 * RESTful access to it.
 * 
 */
@Api
@SwaggerDefinition(info = @Info(title = "las2peer tmitocar Service", version = "1.0.0", description = "A las2peer tmitocar Service for evaluating texts.", termsOfService = "https://tech4comp.de/", contact = @Contact(name = "Alexander Tobias Neumann", url = "https://tech4comp.dbis.rwth-aachen.de/", email = "neumann@dbis.rwth-aachen.de"), license = @License(name = "ACIS License (BSD3)", url = "https://github.com/rwth-acis/las2peer-tmitocar-Service/blob/master/LICENSE")))
@ManualDeployment
@ServicePath("/tmitocar")
public class TmitocarService extends RESTService {
	private String publicKey;
	private String privateKey;
	private String lrsURL;
	private String lrsAuthToken;
	private static HashMap<String, Boolean> userError = null;
	private static HashMap<String, String> userTexts = null;
	private static HashMap<String, Boolean> isActive = null;
	private static HashMap<String, String> expertLabel = null;
	private static final L2pLogger logger = L2pLogger.getInstance(TmitocarService.class.getName());

	private final static String AUTH_FILE = "tmitocar/auth.json";

	public TmitocarService() {
		setFieldValues();

		if (isActive == null) {
			isActive = new HashMap<String, Boolean>();
		}
		if (expertLabel == null) {
			expertLabel = new HashMap<String, String>();
		}
		if (userTexts == null) {
			userTexts = new HashMap<String, String>();
		}

		if (userError == null) {
			userError = new HashMap<String, Boolean>();
		}

		File f = new File(AUTH_FILE);
		if (Files.notExists(f.toPath())) {
			JSONObject j = new JSONObject();
			j.put("ukey", publicKey);
			j.put("pkey", privateKey);
			try {
				FileWriter myWriter = new FileWriter(f);
				myWriter.write(j.toJSONString());
				myWriter.close();
			} catch (IOException e) {
				System.out.println("An error occurred: " + e.getMessage());
				e.printStackTrace();
			}
		}
		L2pLogger.setGlobalConsoleLevel(Level.WARNING);
	}

	/**
	 * Analyze text
	 * 
	 * @param user Name of the current user.
	 * @param body Text to be analyzed
	 * @return Returns an HTTP response with png content derived from the underlying
	 *         tmitocar service.
	 */
	@POST
	@Path("/{user}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces("text/html")
	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Text analyzed") })
	@ApiOperation(value = "Analyze Text", notes = "Sends Text to the tmitocar service and generates a visualization.")
	public Response analyzeText(@PathParam("user") String user, TmitocarText body) {
		isActive.put(user, true);
		JSONObject j = new JSONObject();
		j.put("user", user);
		j.put("text", body.getText().length());
		String wordspec = body.getWordSpec();
		Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_81, j.toJSONString());
		// TODO Handle pdfs
		try {
			new Thread(new Runnable() {
				@Override
				public void run() {
					File f = new File("tmitocar/texts/" + user + "/text.txt");
					try {
						boolean b = f.getParentFile().mkdirs();
						b = f.createNewFile();
						FileWriter writer = new FileWriter(f);
						writer.write(StringEscapeUtils.unescapeJson(body.getText()).toLowerCase());
						writer.close();
					} catch (IOException e) {
						System.out.println("An error occurred: " + e.getMessage());
						e.printStackTrace();
						isActive.put(user, false);
					}
					try {
						ProcessBuilder pb;
						if (wordspec != null && wordspec.length() > 2) {
							System.out.println("Using wordspec: " + wordspec);
							pb = new ProcessBuilder("bash", "tmitocar.sh", "-s", "-i", "texts/" + user + "/text.txt",
									"-w", wordspec);
						} else {
							pb = new ProcessBuilder("bash", "tmitocar.sh", "-s", "-i", "texts/" + user + "/text.txt");
						}
						pb.inheritIO();
						pb.directory(new File("tmitocar"));
						Process process = pb.start();
						try {
							process.waitFor();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						// convert image pngquant --force --quality=40-100 --strip --skip-if-larger
						// example-modell.png
						ProcessBuilder pb2 = new ProcessBuilder("pngquant", "--force", "--quality=40-65", "--strip",
								"--skip-if-larger", "texts/" + user + "/text-modell.png");
						pb2.inheritIO();
						pb2.directory(new File("tmitocar"));
						Process process2 = pb2.start();
						try {
							process2.waitFor();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						isActive.put(user, false);
					} catch (IOException e) {
						e.printStackTrace();

						isActive.put(user, false);

					}
				}
			}).start();
			return Response.ok().entity("").build();
		} catch (Exception e) {
			e.printStackTrace();
			isActive.put(user, false);
			return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
		}

	}

	/**
	 * Analyze text
	 * 
	 * @param user Name of the current user.
	 * @param body Text to be analyzed
	 * @return Returns an HTTP response with png content derived from the underlying
	 *         tmitocar service.
	 */
	@GET
	@Path("/{user}")
	@Produces("image/png")
	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Text analyzed") })
	@ApiOperation(value = "Analyze Text", notes = "Sends Text to the tmitocar service and generates a visualization.")
	public Response getImage(@PathParam("user") String user) {
		// TODO Handle pdfs
		try {
			System.out.println(isActive.get(user));
			BufferedImage image = ImageIO.read(new File("tmitocar/texts/" + user + "/text-modell-fs8.png"));
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(image, "png", baos);
			byte[] imageData = baos.toByteArray();

			JSONObject j = new JSONObject();
			j.put("user", user);
			Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_82, j.toJSONString());
			return Response.ok().entity(imageData).build();
		} catch (IOException e) {
			e.printStackTrace();
			return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
		}

	}

	/**
	 * Analyze text
	 * 
	 * @param user Name of the current user.
	 * @param body Text to be analyzed
	 * @return Returns an HTTP response with png content derived from the underlying
	 *         tmitocar service.
	 */
	@GET
	@Path("/{user}/compare/{expert}")
	@Produces("application/pdf")
	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Text analyzed") })
	@ApiOperation(value = "Analyze Text", notes = "Sends Text to the tmitocar service and generates a visualization.")
	public Response getPDF(@PathParam("user") String user, @PathParam("expert") String expert) {
		// TODO Handle pdfs
		try {
			System.out.println(isActive.get(user));
			File pdf = new File("tmitocar/comparison_" + expert + "_vs_" + user + expert + ".pdf");
			byte[] fileContent = Files.readAllBytes(pdf.toPath());
			JSONObject j = new JSONObject();
			j.put("user", user);
			Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_84, j.toJSONString());
			System.out.println(fileContent);
			return Response.ok().entity(fileContent).build();
		} catch (IOException e) {
			e.printStackTrace();
			return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
		}

	}

	/**
	 * Analyze text
	 * 
	 * @param user Name of the current user.
	 * @param body Text to be analyzed
	 * @return Returns an HTTP response with png content derived from the underlying
	 *         tmitocar service.
	 */
	@GET
	@Path("/{user}/status")
	@Produces("text/html")
	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Text analyzed") })
	@ApiOperation(value = "Analyze Text", notes = "Sends Text to the tmitocar service and generates a visualization.")
	public Response getTmitocarStatus(@PathParam("user") String user) {
		System.out.println(user);
		System.out.println(isActive.get(user));
		return Response.ok(isActive.get(user).toString()).build();
	}

	/**
	 * Analyze text that was send from bot, updated version to be matchable in bot
	 * action
	 * 
	 * @param user Name of the current user.
	 * @param body Text to be analyzed
	 * @return Returns an HTTP response with png content derived from the underlying
	 *         tmitocar service.
	 */
	@POST
	@Path("/getFeedback")
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Text analyzed") })
	@ApiOperation(value = "Analyze Text", notes = "Sends Text to the tmitocar service and generates a visualization.")
	public Response getFeedback(String body) throws ParseException {
		System.out.println(body);
		JSONObject jsonBody = new JSONObject();
		JSONParser p = new JSONParser(JSONParser.MODE_PERMISSIVE);
		jsonBody = (JSONObject) p.parse(body);
		boolean isActive = true;
		while (isActive) {
			isActive = this.isActive.get(jsonBody.getAsString("channel"));
			// isActive = Boolean.parseBoolean(result.getResponse());
			System.out.println(isActive);
			try {
				Thread.sleep(1000);
			} catch (Exception e) {
				jsonBody.put("text", "Exception ist passiert " + e.toString());
				e.printStackTrace();
			}
		}
		byte[] pdfByte = getPDF(jsonBody.getAsString("channel"), expertLabel.get(jsonBody.getAsString("channel")))
				.getEntity().toString().getBytes();
		String fileBody = java.util.Base64.getEncoder().encodeToString(pdfByte);
		jsonBody = new JSONObject();
		// jsonBody.put("text", "Hier deine Datei :D");
		jsonBody.put("fileBody", fileBody);
		jsonBody.put("fileType", "pdf");

		return Response.ok().entity(jsonBody).build();

	}

	/**
	 * Analyze text
	 * 
	 * @param user Name of the current user.
	 * @param body Text to be analyzed
	 * @return Returns an HTTP response with png content derived from the underlying
	 *         tmitocar service.
	 */
	@POST
	@Path("/{user}/{expert}/{template}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces("text/html")
	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Text analyzed") })
	@ApiOperation(value = "Analyze Text", notes = "Sends Text to the tmitocar service and generates a visualization.")
	public Response compareText(@PathParam("user") String user, @PathParam("expert") String expert,
			@PathParam("template") String template, TmitocarText body) {
		// TODO Handle pdfs
		isActive.put(user, true);
		expertLabel.put(user, expert);
		JSONObject j = new JSONObject();
		j.put("user", user);
		Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_83, j.toJSONString());
		System.out.println("Block user");

		// TODO Handle pdfs
		try {
			new Thread(new Runnable() {
				@Override
				public void run() {
					String textContent = "";
					// problem here with the file name no? I mean if two threads do this, we will
					// have one file overwriting the other?
					String fileName = "text.txt";
					System.out.println("Write File");
					String type = body.getType();
					String wordspec = body.getWordSpec();

					if (type.toLowerCase().equals("text/plain") || type.toLowerCase().equals("text")) {
						fileName = "text.txt";
					} else if (type.toLowerCase().equals("application/pdf") || type.toLowerCase().equals("pdf")) {
						fileName = "text.pdf";
					}
					File f = new File("tmitocar/texts/" + user + "/" + fileName);
					try {
						boolean b = f.getParentFile().mkdirs();
						b = f.createNewFile();

						if (type.toLowerCase().equals("text/plain") || type.toLowerCase().equals("text")) {
							/*
							 * FileWriter writer = new FileWriter(f);
							 * writer.write(body.getText().toLowerCase()); writer.close();
							 */
							byte[] decodedBytes = Base64.decode(body.getText());
							FileUtils.writeByteArrayToFile(f, decodedBytes);
							textContent = readTxtFile("tmitocar/texts/" + user + "/" + fileName);
						} else if (type.toLowerCase().equals("application/pdf") || type.toLowerCase().equals("pdf")) {
							byte[] decodedBytes = Base64.decode(body.getText());
							FileUtils.writeByteArrayToFile(f, decodedBytes);
							textContent = readPDFFile("tmitocar/texts/" + user + "/" + fileName);
						}
						userTexts.put(user, textContent);

					} catch (IOException e) {
						System.out.println("An error occurred: " + e.getMessage());
						e.printStackTrace();

						isActive.put(user, false);
					}

					System.out.println("Upload text");
					try {
						// Store usertext cwith label
						// bash tmitocar.sh -i texts/expert/UL_Fend_Novizentext_Eva.txt -l usertext -o
						// json -s -S
						ProcessBuilder pb;
						if (wordspec != null && wordspec.length() > 2) {
							System.out.println("Using wordspec: " + wordspec);
							pb = new ProcessBuilder("bash", "tmitocar.sh", "-s", "-i", "texts/" + user + "/" + fileName,
									"-l", user + expert, "-o", "json", "-S", "-w", wordspec);
						} else {
							pb = new ProcessBuilder("bash", "tmitocar.sh", "-s", "-i", "texts/" + user + "/" + fileName,
									"-l", user + expert, "-o", "json", "-S");
						}

						pb.inheritIO();
						pb.directory(new File("tmitocar"));
						Process process = pb.start();
						try {
							process.waitFor();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							isActive.put(user, false);
							userError.put(user, false);
						}

						System.out.println("compare with expert");
						// compare with expert text
						// bash tmitocar.sh -l usertext -c expert1 -T -s -o json
						ProcessBuilder pb2 = new ProcessBuilder("bash", "tmitocar.sh", "-s", "-l", expert, "-c",
								user + expert, "-o", "json", "-T");
						pb2.inheritIO();
						pb2.directory(new File("tmitocar"));
						Process process2 = pb2.start();
						try {
							process2.waitFor();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							userError.put(user, false);
							isActive.put(user, false);
						}

						System.out.println("gen feedback");

						// generate feedback
						// bash feedback.sh -o pdf -i comparison_usertext_vs_expert1.json -s
						ProcessBuilder pb3 = new ProcessBuilder("bash", "feedback.sh", "-s", "-o", "pdf", "-i",
								"comparison_" + expert + "_vs_" + user + expert + ".json", "-t",
								"templates/" + template, "-S", body.getTopic());
						pb3.inheritIO();
						pb3.directory(new File("tmitocar"));
						Process process3 = pb3.start();
						try {
							process3.waitFor();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							isActive.put(user, false);
						}

						// TODO
						isActive.put(user, false);
					} catch (IOException e) {
						e.printStackTrace();
						// userError.put(user, false);
						isActive.put(user, false);
					}
				}
			}).start();
			return Response.ok().entity("").build();
		} catch (Exception e) {
			e.printStackTrace();
			isActive.put(user, false);
			expertLabel.remove(user);
			return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
		}
	}

	/**
	 * Analyze text that was send from bot, updated version to be matchable in bot
	 * action
	 * 
	 * @param user Name of the current user.
	 * @param body Text to be analyzed
	 * @return Returns an HTTP response with png content derived from the underlying
	 *         tmitocar service.
	 */
	@POST
	@Path("/analyzeText")
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "Text analyzed") })
	@ApiOperation(value = "Analyze Text", notes = "Sends Text to the tmitocar service and generates a visualization.")
	public Response compareTextFromBot(String body) throws ParseException, IOException {
		System.out.println(body);
		JSONObject jsonBody = new JSONObject();
		JSONParser p = new JSONParser(JSONParser.MODE_PERMISSIVE);
		jsonBody = (JSONObject) p.parse(body);
		int taskNumber = Integer.parseInt(jsonBody.getAsString("fileName").replaceAll("[^0-9]", ""));
		String expertLabel = "t" + String.valueOf(taskNumber);
		TmitocarText tmitoBody = new TmitocarText();
		tmitoBody.setTopic(expertLabel);
		tmitoBody.setType(jsonBody.getAsString("fileType"));
		tmitoBody.setWordSpec("1200");
		tmitoBody.setText(jsonBody.getAsString("fileBody"));
		compareText(jsonBody.getAsString("channel"), expertLabel, "template_ul_Q1_2021.md", tmitoBody);
		boolean isActive = true;
		while (isActive) {
			isActive = this.isActive.get(jsonBody.getAsString("channel"));
			// isActive = Boolean.parseBoolean(result.getResponse());
			System.out.println(isActive);
			try {
				Thread.sleep(1000);
			} catch (Exception e) {
				jsonBody.put("text", "Exception ist passiert " + e.toString());
				e.printStackTrace();
			}
		}
		try {
			JSONObject xAPI = createXAPIStatement(jsonBody.getAsString("email"), expertLabel,
					jsonBody.getAsString("channel"));
			Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_3,
					xAPI.toString() + "*" + jsonBody.getAsString("email"));
		} catch (ParseException e) {
			e.printStackTrace();
			System.out.println("could not create API statement");
		}
		byte[] pdfByte = Files.readAllBytes(Paths.get("tmitocar/comparison_" + expertLabel + "_vs_"
				+ jsonBody.getAsString("channel") + expertLabel + ".pdf"));
		// byte[] pdfByte = getPDF(jsonBody.getAsString("channel"),
		// this.expertLabel.get(jsonBody.getAsString("channel")))
		// .getEntity().toString().getBytes();
		System.out.println(pdfByte);
		userTexts.remove(jsonBody.getAsString("channel"));
		String fileBody = java.util.Base64.getEncoder().encodeToString(pdfByte);
		String errorMessage = "";
		if (!userError.get(jsonBody.getAsString("channel"))) {
			if (jsonBody.get("errorMessage") != null) {
				errorMessage = jsonBody.get("errorMessage");
			} else {
				errorMessage = "Irgendwas ist schief, gelaufen :o. Die Feedback Datei konnte nicht erzeugt werden oder ist möglicherweise nicht vollständig :/";
			}

		}
		jsonBody = new JSONObject();
		// jsonBody.put("text", "Hier deine Datei :D");
		jsonBody.put("fileBody", fileBody);
		jsonBody.put("fileType", "pdf");
		jsonBody.put("fileName", "Feedback");
		sonBody.put("text", errorMessage);
		return Response.ok().entity(jsonBody).build();
	}

	@POST
	@Path("/getCredits")
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "REPLACE THIS WITH AN APPROPRIATE FUNCTION NAME", notes = "REPLACE THIS WITH YOUR NOTES TO THE FUNCTION")
	@ApiResponses(value = {
			@ApiResponse(code = HttpURLConnection.HTTP_OK, message = "REPLACE THIS WITH YOUR OK MESSAGE") })
	public Response getCredits(String body) throws ParseException, IOException {
		System.out.println(body);
		JSONObject jsonBody = new JSONObject();
		JSONParser p = new JSONParser(JSONParser.MODE_PERMISSIVE);
		jsonBody = (JSONObject) p.parse(body);
		// Copy pasted from LL service
		// Fetch ALL statements
		URL url = new URL(lrsURL + "/data/xAPI/statements");
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
		conn.setRequestProperty("X-Experience-API-Version", "1.0.3");
		conn.setRequestProperty("Authorization", "Basic " + lrsAuthToken);
		conn.setRequestProperty("Cache-Control", "no-cache");
		conn.setUseCaches(false);

		BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

		String inputLine;
		StringBuffer response = new StringBuffer();
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		conn.disconnect();

		String user = jsonBody.getAsString("email");
		jsonBody = (JSONObject) p.parse(response.toString());

		JSONArray statements = (JSONArray) jsonBody.get("statements");

		// Check statements with matching actor
		// 12 Values as 12 assignments right?
		int[] assignments = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
		for (Object index : statements) {
			JSONObject jsonIndex = (JSONObject) index;
			JSONObject actor = (JSONObject) jsonIndex.get("actor");
			JSONObject account = (JSONObject) actor.get("account");
			if (account.getAsString("name").equals(user)
					|| account.getAsString("name").equals(encryptThisString(user))) {
				JSONObject object = (JSONObject) jsonIndex.get("object");
				JSONObject definition = (JSONObject) object.get("definition");
				JSONObject name = (JSONObject) definition.get("name");
				String assignmentName = name.getAsString("en-US");
				int assignmentNumber = Integer.valueOf(assignmentName.split("t")[1]);
				assignments[assignmentNumber]++;
				System.out.println("Extracted actor is " + name.getAsString("en-US"));
			}
		}
		String msg = "";
		int credits = 0;
		for (int i = 0; i < 12; i++) {
			String number;
			if (i < 9) {
				number = "0" + String.valueOf(i + 1);
			} else {
				number = String.valueOf(i + 1);
			}
			if (assignments[i] > 0) {
				credits++;
			}
			msg += "Schreibaufgabe " + number + ": " + String.valueOf(assignments[i]) + "\n";
		}
		// How are the credits calculated?
		msg += "Das hei\u00DFt, du hast bisher *" + credits + "* Leistunsprozente gesammelt. ";
		System.out.println(msg);
		jsonBody = new JSONObject();
		jsonBody.put("text", msg);
		return Response.ok().entity(jsonBody).build();
	}

	public static String encryptThisString(String input) {
		try {
			// getInstance() method is called with algorithm SHA-384
			MessageDigest md = MessageDigest.getInstance("SHA-384");

			// digest() method is called
			// to calculate message digest of the input string
			// returned as array of byte
			byte[] messageDigest = md.digest(input.getBytes());

			// Convert byte array into signum representation
			BigInteger no = new BigInteger(1, messageDigest);

			// Convert message digest into hex value
			String hashtext = no.toString(16);

			// Add preceding 0s to make it 32 bit
			while (hashtext.length() < 32) {
				hashtext = "0" + hashtext;
			}

			// return the HashText
			return hashtext;
		}

		// For specifying wrong message digest algorithms
		catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	public String readTxtFile(String fileName) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		try {
			StringBuilder sb = new StringBuilder();
			String line = br.readLine();

			while (line != null) {
				sb.append(line);
				sb.append("\n");
				line = br.readLine();
			}
			br.close();
			return sb.toString();
		} finally {
			br.close();
		}
	}

	public String readPDFFile(String fileName) {
		org.apache.pdfbox.pdfparser.PDFParser parser = null;
		org.apache.pdfbox.pdmodel.PDDocument pdDoc = null;
		org.apache.pdfbox.cos.COSDocument cosDoc = null;
		org.apache.pdfbox.util.PDFTextStripper pdfStripper;
		String parsedText = "";
		File file = new File(fileName);
		try {
			parser = new PDFParser(new FileInputStream(file));
			parser.parse();
			cosDoc = parser.getDocument();
			pdfStripper = new org.apache.pdfbox.util.PDFTextStripper();
			pdDoc = new org.apache.pdfbox.pdmodel.PDDocument(cosDoc);
			parsedText = pdfStripper.getText(pdDoc);
		} catch (Exception e) {
			e.printStackTrace();
			try {
				if (cosDoc != null)
					cosDoc.close();
				if (pdDoc != null)
					pdDoc.close();
			} catch (Exception e1) {
				e1.printStackTrace();
			}

		}
		return parsedText;

	}

	public JSONObject createXAPIStatement(String userMail, String assignmentTitle, String text) throws ParseException {
		JSONParser p = new JSONParser(JSONParser.MODE_PERMISSIVE);
		JSONObject actor = new JSONObject();
		actor.put("objectType", "Agent");
		JSONObject account = new JSONObject();
		account.put("name", encryptThisString(userMail));
		account.put("homePage", "https://chat.tech4comp.dbis.rwth-aachen.de");
		JSONObject verb = (JSONObject) p
				.parse(new String("{'display':{'en-US':'sent_file'},'id':'https://tech4comp.de/xapi/verb/sent_file'}"));
		JSONObject object = (JSONObject) p
				.parse(new String("{'definition':{'interactionType':'other', 'name':{'en-US':'" + assignmentTitle
						+ "'}, 'description':{'en-US':'" + assignmentTitle
						+ "'}, 'type':'https://tech4comp.de/xapi/activitytype/file'},'id':'https://tech4comp.de/biwi5/file/"
						+ encryptThisString(userMail) + assignmentTitle + "', 'objectType':'Activity'}"));
		JSONObject context = (JSONObject) p.parse(new String(
				"{'extensions':{'https://tech4comp.de/xapi/context/extensions/filecontent':{'text':'" + text + "'}}}"));
		JSONObject xAPI = new JSONObject();

		xAPI.put("authority", p.parse(
				new String("{'objectType': 'Agent','name': 'New Client', 'mbox': 'mailto:hello@learninglocker.net'}")));
		xAPI.put("context", context); //
		xAPI.put("timestamp", java.time.LocalDateTime.now());
		xAPI.put("actor", actor);
		xAPI.put("object", object);
		xAPI.put("verb", verb);
		System.out.println(xAPI);
		return xAPI;
	}

}
