package i5.las2peer.services.tmitocar;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.logging.Level;
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
import org.java_websocket.util.Base64;

import com.google.gson.Gson;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;

import i5.las2peer.api.Context;
import i5.las2peer.api.ManualDeployment;
import i5.las2peer.api.logging.MonitoringEvent;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.restMapper.RESTService;
import i5.las2peer.restMapper.annotations.ServicePath;
import i5.las2peer.services.tmitocar.pojo.TmitocarResponse;
import i5.las2peer.services.tmitocar.pojo.TmitocarText;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Contact;
import io.swagger.annotations.Info;
import io.swagger.annotations.License;
import io.swagger.annotations.SwaggerDefinition;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.ParseException;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.bson.BsonDocument;
import org.bson.BsonInt64;
import org.bson.Document;
import org.bson.UuidRepresentation;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

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

	private static HashMap<String, Boolean> isActive = null;
	private static HashMap<String, String> userTexts = null;
	private static final L2pLogger logger = L2pLogger.getInstance(TmitocarService.class.getName());

	private String mongoHost;
	private String mongoUser;
	private String mongoPassword;
	private String mongoDB;
	private String mongoUri;
	private String mongoAuth = "admin";

	private final static String AUTH_FILE = "tmitocar/auth.json";

	// This is the constructor of the TmitocarService class.
	public TmitocarService() {
		// Call the setFieldValues() method to initialize some fields.
		setFieldValues();

		// Call the initVariables() method to initialize some variables.
		initVariables();

		// Call the initAuth() method to initialize the authentication mechanism.
		initAuth();

		// Call the initDB() method to initialize the database connection.
		initDB();

		// Set the logging level to WARNING for the L2pLogger global console.
		L2pLogger.setGlobalConsoleLevel(Level.WARNING);
	}

	@Override
	protected void initResources() {
		getResourceConfig().register(this);
		getResourceConfig().register(Feedback.class);
		getResourceConfig().register(TMitocarText.class);
	}

	private void initVariables() {
		if (isActive == null) {
			isActive = new HashMap<String, Boolean>();
		}
		if (userTexts == null) {
			userTexts = new HashMap<String, String>();
		}
	}

	// This is a private method of the TmitocarService class used to initialize the
	// authentication mechanism.
	private void initAuth() {
		// Create a new File object with the AUTH_FILE file path.
		File f = new File(AUTH_FILE);

		// Check if the file does not exist.
		if (Files.notExists(f.toPath())) {
			// If the file does not exist, create a new JSONObject to store the public and
			// private keys.
			JSONObject j = new JSONObject();
			j.put("ukey", publicKey);
			j.put("pkey", privateKey);

			// Try to write the JSONObject to the file.
			try {
				// Create a new FileWriter object to write to the file.
				FileWriter myWriter = new FileWriter(f);
				// Write the JSON string to the file.
				myWriter.write(j.toJSONString());
				// Close the FileWriter object.
				myWriter.close();
			} catch (IOException e) {
				// If an IOException occurs, print an error message and stack trace.
				System.out.println("An error occurred: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	private void initDB() {
		// mongo db connection for exchanging files
		mongoUri = "mongodb://" + mongoUser + ":" + mongoPassword + "@" + mongoHost + "/?authSource=" + mongoAuth;
		// Construct a ServerApi instance using the ServerApi.builder() method
		CodecRegistry pojoCodecRegistry = fromProviders(PojoCodecProvider.builder().automatic(true).build());
		CodecRegistry codecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), pojoCodecRegistry);
		MongoClientSettings settings = MongoClientSettings.builder()
				.uuidRepresentation(UuidRepresentation.STANDARD)
				.applyConnectionString(new ConnectionString(mongoUri))
				.codecRegistry(codecRegistry)
				.build();

		// Create a new client and connect to the server
		MongoClient mongoClient = MongoClients.create(settings);
		// Create a new client and connect to the server
		try {
			MongoDatabase database = mongoClient.getDatabase(mongoDB);
			// Send a ping to confirm a successful connection
			Bson command = new BsonDocument("ping", new BsonInt64(1));
			Document commandResult = database.runCommand(command);
			System.out.println("Pinged your deployment. You successfully connected to MongoDB!");
		} catch (MongoException me) {
			System.err.println(me);
		} finally {
			mongoClient.close();
		}
	}

	private void uploadToTmitocar(String label1, String fileName, String wordspec)
			throws InterruptedException, IOException {
		ProcessBuilder pb;
		if (wordspec != null && wordspec.length() > 2) {
			System.out.println("Using wordspec: " + wordspec);
			pb = new ProcessBuilder("bash", "tmitocar.sh", "-s", "-i",
					"texts/" + label1 + "/" + fileName,
					"-l", label1, "-o", "json", "-S", "-w", wordspec);

		} else {
			pb = new ProcessBuilder("bash", "tmitocar.sh", "-s", "-i",
					"texts/" + label1 + "/" + fileName,
					"-l", label1, "-o", "json", "-S");
		}

		pb.inheritIO();
		pb.directory(new File("tmitocar"));
		Process p = pb.start();
		p.waitFor();
	}

	private void createComparison(String label1, String label2) throws InterruptedException, IOException {
		ProcessBuilder pb2 = new ProcessBuilder("bash", "tmitocar.sh", "-s", "-l", label1, "-c",
				label2, "-o", "json", "-T");
		pb2.inheritIO();
		pb2.directory(new File("tmitocar"));
		Process process2 = pb2.start();
		process2.waitFor();
	}

	private void generateFeedback(String label1, String label2, String template, String topic)
			throws InterruptedException, IOException {
		ProcessBuilder pb = new ProcessBuilder("bash", "feedback.sh", "-s", "-o", "pdf", "-i",
				"comparison_" + label1 + "_vs_" + label2 + ".json", "-t",
				"templates/" + template, "-S", topic);
		pb.inheritIO();
		pb.directory(new File("tmitocar"));
		Process p = pb.start();
		p.waitFor();
	}

	/**
	 * Analyze text
	 * 
	 * @param label1 first label (source text)
	 * @param label2 second label (remote text)
	 * @param body   Text to be analyzed
	 * @return Returns an HTTP response with png content derived from the underlying
	 *         tmitocar service.
	 */
	public boolean compareText(@PathParam("label1") String label1, @PathParam("label2") String label2,
			@PathParam("template") String template, TmitocarText body) {
		// TODO Handle pdfs
		isActive.put(label1, true);
		JSONObject j = new JSONObject();
		j.put("user", label1);
		Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_83, j.toJSONString());
		System.out.println("Block " + label1);

		// TODO Handle pdfs
		try {
			new Thread(new Runnable() {
				@Override
				public void run() {

					storeFileLocally(label1, body.getText(), body.getType());

					System.out.println("Upload text");
					try {
						// Store usertext cwith label
						// bash tmitocar.sh -i texts/expert/UL_Fend_Novizentext_Eva.txt -l usertext -o
						// json -s -S
						String wordspec = body.getWordSpec();
						String fileName = createFileName(label1, body.getType());
						uploadToTmitocar(label1, fileName, wordspec);

						System.out.println("compare with expert");
						// compare with expert text
						// bash tmitocar.sh -l usertext -c expert1 -T -s -o json

						createComparison(label1, label2);
						System.out.println("gen feedback");

						// generate feedback
						// bash feedback.sh -o pdf -i comparison_usertext_vs_expert1.json -s
						generateFeedback(label1, label2, template, body.getTopic());
						
						// TODO
						isActive.put(label1, false);
					} catch (IOException e) {
						e.printStackTrace();
						// userError.put(user, false);
						isActive.put(label1, false);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						isActive.put(label1, false);
					}
				}
			}).start();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public Response processSingleText(String user, String expert, String template, TmitocarText body) {
		// TODO Handle pdfs
		isActive.put(user, true);
		JSONObject j = new JSONObject();
		j.put("user", user);
		Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_83, j.toJSONString());
		System.out.println("Block user");

		try {
			new Thread(new Runnable() {
				@Override
				public void run() {
					String textContent = "";
					// problem here with the file name no? I mean if two threads do this, we will
					// have one file overwriting the other?
					String type = body.getType();
					String fileName = createFileName(user,type);
					System.out.println("Write File");
					String wordspec = body.getWordSpec();

					
					File f = new File("tmitocar/texts/" + user + "/" + fileName);
					try {
						boolean b = f.getParentFile().mkdirs();
						b = f.createNewFile();

						if (type.toLowerCase().equals("text/plain") || type.toLowerCase().equals("text")) {
							byte[] decodedBytes = Base64.decode(body.getText());
							FileUtils.writeByteArrayToFile(f, decodedBytes);
							textContent = readTxtFile("tmitocar/texts/" + user + "/" + fileName);
						} else if (type.toLowerCase().equals("application/pdf") || type.toLowerCase().equals("pdf")) {
							byte[] decodedBytes = Base64.decode(body.getText());
							System.out.println(decodedBytes);
							FileUtils.writeByteArrayToFile(f, decodedBytes);
							textContent = readPDFFile("tmitocar/texts/" + user + "/" + fileName);
						}
						if (textContent.replaceAll("\\s", "").length() < 350) {
							System.out.println("not enough words");
							throw new IOException();
						}
						userTexts.put(user, textContent);
					} catch (IOException e) {
						System.out.println("An error occurred: " + e.getMessage());
						e.printStackTrace();
						isActive.put(user, false);
						Thread.currentThread().interrupt();
					}

					System.out.println("Upload text");
					try {
						uploadToTmitocar(user, fileName, wordspec);

						System.out.println("gen feedback");

						// generate feedback
						generateFeedback(user, expert, template, body.getTopic());
						isActive.put(user, false);
					} catch (IOException e) {
						e.printStackTrace();
						isActive.put(user, false);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
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

	@Api(value = "Text Resource")
	@SwaggerDefinition(info = @Info(title = "todo", version = "1.0.0", description = "todo.", termsOfService = "", contact = @Contact(name = "Alexander Tobias Neumann", url = "", email = "neumann@dbis.rwth-aachen.de"), license = @License(name = "", url = "")))
	@Path("/text")
	public static class TMitocarText {
		TmitocarService service = (TmitocarService) Context.get().getService();

		/**
		 * Store text
		 *
		 * @param label1          the first label (user text)
		 * @param textInputStream the InputStream containing the text to compare
		 * @param textFileDetail  the file details of the text file
		 * @param type            the type of text (txt or pdf)
		 * @return todo
		 * @throws ParseException if there is an error parsing the input parameters
		 * @throws IOException    if there is an error reading the input stream
		 */
		@POST
		@Path("/{label1}")
		@Consumes(MediaType.MULTIPART_FORM_DATA)
		@Produces(MediaType.APPLICATION_JSON)
		@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "") })
		@ApiOperation(value = "analyzeText", notes = "Analyzes a text and generates a PDF report")
		public Response analyzeText(@PathParam("label1") String label1,
				@FormDataParam("text") InputStream textInputStream,
				@FormDataParam("text") FormDataContentDisposition textFileDetail, @FormDataParam("type") String type,
				@FormDataParam("topic") String topic, @FormDataParam("template") String template,
				@FormDataParam("wordSpec") String wordSpec) throws ParseException, IOException {
			if (isActive.getOrDefault(label1, false)) {
				return Response.status(Status.BAD_REQUEST).entity("User: " + label1 + " currently busy.").build();
			}
			isActive.put(label1, true);
			String encodedByteString = convertInputStreamToBase64(textInputStream);
			TmitocarText tmitoBody = new TmitocarText();
			tmitoBody.setTopic(topic);
			tmitoBody.setType(type);
			tmitoBody.setWordSpec(wordSpec);
			tmitoBody.setTemplate(template);
			tmitoBody.setText(encodedByteString);
			service.processSingleText(label1, topic, template, tmitoBody);

			byte[] bytes = Base64.decode(encodedByteString);
			String fname = textFileDetail.getFileName();
			ObjectId uploaded = service.storeFile(label1 + "-" + fname, bytes);
			if (uploaded == null) {
				return Response.status(Status.BAD_REQUEST).entity("Could not store file " + fname).build();
			}
			try {
				textInputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			TmitocarResponse response = new TmitocarResponse(uploaded.toString());
			Gson g = new Gson();
			return Response.ok().entity(g.toJson(response)).build();
		}
	}

	@Api(value = "Feedback Resource")
	@SwaggerDefinition(info = @Info(title = "todo", version = "1.0.0", description = "todo.", termsOfService = "", contact = @Contact(name = "Alexander Tobias Neumann", url = "", email = "neumann@dbis.rwth-aachen.de"), license = @License(name = "", url = "")))
	@Path("/feedback")
	public static class Feedback {
		TmitocarService service = (TmitocarService) Context.get().getService();

		/**
		 * Anaylze text
		 *
		 * @param label1          the first label (user text)
		 * @param textInputStream the InputStream containing the text to compare
		 * @param textFileDetail  the file details of the text file
		 * @param type            the type of text (txt or pdf)
		 * @param topic           the topic of the text (e.g. BiWi 5)
		 * @param template        the template to use for the PDF report
		 * @param wordSpec        the word specification for the PDF report
		 * @return todo
		 * @throws ParseException if there is an error parsing the input parameters
		 * @throws IOException    if there is an error reading the input stream
		 */
		@POST
		@Path("/{label1}")
		@Consumes(MediaType.MULTIPART_FORM_DATA)
		@Produces(MediaType.APPLICATION_JSON)
		@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "") })
		@ApiOperation(value = "analyzeText", notes = "Analyzes a text and generates a PDF report")
		public Response analyzeText(@PathParam("label1") String label1,
				@FormDataParam("text") InputStream textInputStream,
				@FormDataParam("text") FormDataContentDisposition textFileDetail, @FormDataParam("type") String type,
				@FormDataParam("topic") String topic, @FormDataParam("template") String template,
				@FormDataParam("wordSpec") String wordSpec) throws ParseException, IOException {
			if (isActive.getOrDefault(label1, false)) {
				return Response.status(Status.BAD_REQUEST).entity("User: " + label1 + " currently busy.").build();
			}
			isActive.put(label1, true);
			String encodedByteString = convertInputStreamToBase64(textInputStream);
			TmitocarText tmitoBody = new TmitocarText();
			tmitoBody.setTopic(topic);
			tmitoBody.setType(type);
			tmitoBody.setWordSpec(wordSpec);
			tmitoBody.setTemplate(template);
			tmitoBody.setText(encodedByteString);
			service.processSingleText(label1, topic, template, tmitoBody);

			byte[] bytes = Base64.decode(encodedByteString);
			String fname = textFileDetail.getFileName();
			ObjectId uploaded = service.storeFile(label1 + "-" + fname, bytes);
			if (uploaded == null) {
				return Response.status(Status.BAD_REQUEST).entity("Could not store file " + fname).build();
			}
			try {
				textInputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			TmitocarResponse response = new TmitocarResponse(uploaded.toString());
			Gson g = new Gson();
			return Response.ok().entity(g.toJson(response)).build();
		}

		@GET
		@Path("/{label1}")
		@Produces(MediaType.APPLICATION_JSON)
		@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "") })
		@ApiOperation(value = "compareText", notes = "Returns analyzed text report (PDF)")
		public Response getAnalyzedText(@PathParam("label1") String label1) throws ParseException, IOException {
			if (isActive.getOrDefault(label1, false)) {
				return Response.status(Status.BAD_REQUEST).entity("User: " + label1 + " currently busy.").build();
			}
			JSONObject err = new JSONObject();
			ObjectId fileId = null;
			if (userTexts.get(label1) != null) {
				System.out.println("Storing PDF to mongodb...");
				try {
					byte[] pdfByte = Files.readAllBytes(
							Paths.get("tmitocar/texts/" + label1 + "/" + label1 + "-modell" + ".pdf"));
					fileId = service.storeFile(label1 + "-feedback.pdf", pdfByte);
					Files.delete(Paths.get("tmitocar/texts/" + label1 + "/" + label1 + "-modell" + ".pdf"));
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println("Failed storing PDF.");
				}
			} else {
				err.put("errorMessage", "Something went wrong storing the feedback for " + label1);
				return Response.status(Status.BAD_REQUEST).entity(err.toJSONString()).build();
			}

			if (fileId == null) {
				err.put("errorMessage", "Something went wrong storing the feedback for " + label1);
				return Response.status(Status.BAD_REQUEST).entity(err.toJSONString()).build();
			}
			TmitocarResponse response = new TmitocarResponse(null, fileId.toString());
			Gson g = new Gson();
			return Response.ok().entity(g.toJson(response)).build();
		}

		/**
		 * Compare text
		 *
		 * @param label1          the first label (user text)
		 * @param label2          the second label (expert or second user text)
		 * @param textInputStream the InputStream containing the text to compare
		 * @param textFileDetail  the file details of the text file
		 * @param type            the type of text (txt or pdf)
		 * @param topic           the topic of the text (e.g. BiWi 5)
		 * @param template        the template to use for the PDF report
		 * @param wordSpec        the word specification for the PDF report
		 * @return todo
		 * @throws ParseException if there is an error parsing the input parameters
		 * @throws IOException    if there is an error reading the input stream
		 */
		@POST
		@Path("/{label1}/compare/{label2}")
		@Consumes(MediaType.MULTIPART_FORM_DATA)
		@Produces(MediaType.APPLICATION_JSON)
		@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "") })
		@ApiOperation(value = "compareText", notes = "Compares two texts and generates a PDF report")
		public Response compareText(@PathParam("label1") String label1, @PathParam("label2") String label2,
				@FormDataParam("text") InputStream textInputStream,
				@FormDataParam("text") FormDataContentDisposition textFileDetail, @FormDataParam("type") String type,
				@FormDataParam("topic") String topic, @FormDataParam("template") String template,
				@FormDataParam("wordSpec") String wordSpec) throws ParseException, IOException {

			isActive.put(label1, true);
			String encodedByteString = convertInputStreamToBase64(textInputStream);
			TmitocarText tmitoBody = new TmitocarText();
			tmitoBody.setTopic(topic);
			tmitoBody.setType(type);
			tmitoBody.setWordSpec(wordSpec);
			tmitoBody.setTemplate(template);
			tmitoBody.setText(encodedByteString);
			boolean comparing = service.compareText(label1, label2, template, tmitoBody);
			if (!comparing) {
				isActive.put(label1, false);
				return Response.status(Status.BAD_REQUEST).entity("Something went wrong: " + label1 + ".").build();
			}

			byte[] bytes = Base64.decode(encodedByteString);
			String fname = textFileDetail.getFileName();
			ObjectId uploaded = service.storeFile(label1 + "-" + fname, bytes);
			if (uploaded == null) {
				return Response.status(Status.BAD_REQUEST).entity("Could not store file " + fname).build();
			}
			try {
				textInputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			TmitocarResponse response = new TmitocarResponse(uploaded.toString());
			Gson g = new Gson();
			return Response.ok().entity(g.toJson(response)).build();
		}

		@GET
		@Path("/{label1}/compare/{label2}")
		@Consumes(MediaType.MULTIPART_FORM_DATA)
		@Produces(MediaType.APPLICATION_JSON)
		@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "") })
		@ApiOperation(value = "compareText", notes = "Returns compared text report (PDF)")
		public Response getComparedText(@PathParam("label1") String label1, @PathParam("label2") String label2)
				throws ParseException, IOException {
			if (isActive.get(label1)) {
				return Response.status(Status.BAD_REQUEST).entity("User: " + label1 + " currently busy.").build();
			}
			JSONObject err = new JSONObject();
			ObjectId feedbackFileId = null;
			ObjectId graphFileId = null;
			if (userTexts.get(label1) != null) {
				System.out.println("Storing PDF to mongodb...");
				feedbackFileId = service.storeLocalFileRemote("comparison_" + label1 + "_vs_" + label2 + ".json");
				graphFileId = service.storeLocalFileRemote("comparison_" + label1 + "_vs_" + label2 + ".pdf");

			} else {
				err.put("errorMessage", "Something went wrong storing the feedback for " + label1);
				return Response.status(Status.BAD_REQUEST).entity(err.toJSONString()).build();
			}

			if (feedbackFileId == null) {
				err.put("errorMessage", "Something went wrong storing the feedback for " + label1);
				return Response.status(Status.BAD_REQUEST).entity(err.toJSONString()).build();
			}
			if (graphFileId == null) {
				err.put("errorMessage", "Something went wrong storing the graph for " + label1);
				return Response.status(Status.BAD_REQUEST).entity(err.toJSONString()).build();
			}
			TmitocarResponse response = new TmitocarResponse(null, feedbackFileId.toString(),graphFileId.toString());
			Gson g = new Gson();
			return Response.ok().entity(g.toJson(response)).build();
		}
	}

	private static String convertInputStreamToBase64(InputStream inputStream) throws IOException {
		byte[] bytes = inputStream.readAllBytes();
		return Base64.encodeBytes(bytes);
	}

	private ObjectId storeFile(String filename, byte[] bytesToStore) {
		CodecRegistry pojoCodecRegistry = fromProviders(PojoCodecProvider.builder().automatic(true).build());
		CodecRegistry codecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), pojoCodecRegistry);
		MongoClientSettings settings = MongoClientSettings.builder()
				.uuidRepresentation(UuidRepresentation.STANDARD)
				.applyConnectionString(new ConnectionString(mongoUri))
				.codecRegistry(codecRegistry)
				.build();
		// Create a new client and connect to the server
		MongoClient mongoClient = MongoClients.create(settings);
		ObjectId fileId = null;
		try {
			MongoDatabase database = mongoClient.getDatabase(mongoDB);
			GridFSBucket gridFSBucket = GridFSBuckets.create(database, "files");
			ByteArrayInputStream inputStream = new ByteArrayInputStream(bytesToStore);
			fileId = gridFSBucket.uploadFromStream(filename, inputStream);
			System.out.println("File uploaded successfully with ID: " + fileId.toString());
			try {
				inputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (MongoException me) {
			System.err.println(me);
		} finally {
			// Close MongoDB client
			mongoClient.close();
		}
		return fileId;
	}

	private ObjectId storeLocalFileRemote(String fileName){
		ObjectId fileId = null;
		try {
			byte[] pdfByte = Files.readAllBytes(
					Paths.get("tmitocar/"+fileName));
					fileId = storeFile(fileName, pdfByte);
			Files.delete(Paths.get("tmitocar/"+fileName));
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Failed storing PDF.");
		}
		return fileId;
	}

	private static String readTxtFile(String fileName) {
		String text = "";
		try {
			text = new String(Files.readAllBytes(Paths.get(fileName)));
		} catch (IOException e) {
			e.printStackTrace();
		}

		return text;
	}

	private String readPDFFile(String fileName) {
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

	private String createFileName(String name, String type){
		if (type.toLowerCase().equals("text/plain") || type.toLowerCase().equals("text")) {
			return name + ".txt";
		} else if (type.toLowerCase().equals("application/pdf") || type.toLowerCase().equals("pdf")) {
			return name + ".pdf";
		}
		return name + "txt";
	}

	private boolean storeFileLocally(String name, String text, String type) {
		String textContent = "";
		// problem here with the file name no? I mean if two threads do this, we will
		// have one file overwriting the other?
		String fileName = createFileName(name, type);
		System.out.println("Write File");

		File f = new File("tmitocar/texts/" + name + "/" + fileName);
		try {
			boolean b = f.getParentFile().mkdirs();
			b = f.createNewFile();

			if (type.toLowerCase().equals("text/plain") || type.toLowerCase().equals("text")) {
				/*
				 * FileWriter writer = new FileWriter(f);
				 * writer.write(body.getText().toLowerCase()); writer.close();
				 */
				byte[] decodedBytes = Base64.decode(text);
				System.out.println(decodedBytes);
				FileUtils.writeByteArrayToFile(f, decodedBytes);
				textContent = readTxtFile("tmitocar/texts/" + name + "/" + fileName);
			} else if (type.toLowerCase().equals("application/pdf") || type.toLowerCase().equals("pdf")) {
				byte[] decodedBytes = Base64.decode(text);
				System.out.println(decodedBytes);
				FileUtils.writeByteArrayToFile(f, decodedBytes);
				textContent = readPDFFile("tmitocar/texts/" + name + "/" + fileName);
			} else {
				System.out.println("wrong type");
				throw new IOException();
			}
			// spaces are not counted
			if (textContent.replaceAll("\\s", "").length() < 350) {
				System.out.println("not enough words");
				throw new IOException();
			}
			userTexts.put(name, textContent);

		} catch (IOException e) {
			System.out.println("An error occurred: " + e.getMessage());
			e.printStackTrace();
			isActive.put(name, false);
			Thread.currentThread().interrupt();
			return false;
		}
		return true;
	}

	private void deleteFileLocally(String name){
		// or should we delete the user folder afterwards? 
		try {
			Files.delete(Paths.get("tmitocar/texts/" + name + "/" + name+".txt"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			Files.delete(Paths.get("tmitocar/texts/" + name + "/" + name+".pdf"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
