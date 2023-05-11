package i5.las2peer.services.tmitocar;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.logging.Level;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.dbcp2.BasicDataSource;
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
import i5.las2peer.services.tmitocar.pojo.LrsCredentials;
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
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
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
@SwaggerDefinition(info = @Info(title = "las2peer tmitocar Service", version = "1.0.0", description = "A las2peer tmitocar wrapper service for analyzing/evaluating texts.", termsOfService = "https://tech4comp.de/", contact = @Contact(name = "Alexander Tobias Neumann", url = "https://tech4comp.dbis.rwth-aachen.de/", email = "neumann@dbis.rwth-aachen.de"), license = @License(name = "ACIS License (BSD3)", url = "https://github.com/rwth-acis/las2peer-tmitocar-Service/blob/master/LICENSE")))
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

	private String pgsqlHost;
	private String pgsqlPort;
	private String pgsqlUser;
	private String pgsqlPassword;
	private String pgsqlDB;

	private static BasicDataSource dataSource;

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
		getResourceConfig().register(WritingTask.class);
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
		// postgresql 
		if (dataSource == null) {
            dataSource = new BasicDataSource();
            dataSource.setDriverClassName("org.postgresql.Driver");
            dataSource.setUrl("jdbc:postgresql://"+pgsqlHost+":"+pgsqlPort+"/"+pgsqlDB);
            dataSource.setUsername(pgsqlUser);
            dataSource.setPassword(pgsqlPassword);

            // Set connection pool properties
            dataSource.setInitialSize(5);
            dataSource.setMaxTotal(10);
        }
	}

	protected Connection getConnection() throws SQLException {
        return dataSource.getConnection();
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

	private void generateFeedback(String label1, String label2, String template, String topic, String scriptFile)
			throws InterruptedException, IOException {
		ProcessBuilder pb = new ProcessBuilder("bash", scriptFile, "-s", "-o", "pdf", "-i",
				"comparison_" + label1 + "_vs_" + label2 + ".json", "-t",
				"templates/" + template, "-S", topic);
		pb.inheritIO();
		pb.directory(new File("tmitocar"));
		Process p = pb.start();
		p.waitFor();
	}

	private void generateFeedback(String label1, String label2, String template, String topic)
			throws InterruptedException, IOException {
		generateFeedback(label1, label2, template, topic, "feedback.sh");
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
		isActive.put(label1, true);
		JSONObject j = new JSONObject();
		j.put("user", label1);
		Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_83, j.toJSONString());
		System.out.println("Block " + label1);

		try {
			new Thread(new Runnable() {
				@Override
				public void run() {

					storeFileLocally(label1, body.getText(), body.getType());

					System.out.println("Upload text");
					try {
						// Store usertext with label
						String wordspec = body.getWordSpec();
						String fileName = createFileName(label1, body.getType());
						uploadToTmitocar(label1, fileName, wordspec);

						System.out.println("Compare with expert.");
						// compare with expert text
						createComparison(label1, label2);

						System.out.println("Generate feedback.");
						// generate feedback
						generateFeedback(label1, label2, template, body.getTopic());

						isActive.put(label1, false);
					} catch (IOException e) {
						e.printStackTrace();
						isActive.put(label1, false);
					} catch (InterruptedException e) {
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
					String type = body.getType();
					String fileName = createFileName(user, type);
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
						generateFeedback(user, expert, template, body.getTopic(), "feedback_single.sh");
						isActive.put(user, false);
					} catch (IOException e) {
						e.printStackTrace();
						isActive.put(user, false);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}
			}).start();
			return Response.ok().entity("").build();
		} catch (Exception e) {
			e.printStackTrace();
			isActive.put(user, false);
			JSONObject err = new JSONObject();
			err.put("errorMessage", e.getMessage());
			err.put("error", true);
			return Response.status(Status.BAD_REQUEST).entity(err.toString()).build();
		}
	}

	@GET
    @Path("/")
    public Response getDefault() {
        try {
            // Perform any necessary operations or calculations here
            String result = "Welcome to the tmitocar service!";
            return Response.ok(result).build();
        } catch (Exception e) {
            // Handle any exceptions that occur
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

	@Api(value = "Writing Task Resource")
	@SwaggerDefinition(info = @Info(title = "Writing Task Resource", version = "1.0.0", description = "Todo.", termsOfService = "https://tech4comp.de/", contact = @Contact(name = "Alexander Tobias Neumann", url = "https://tech4comp.dbis.rwth-aachen.de/", email = "neumann@dbis.rwth-aachen.de"), license = @License(name = "ACIS License (BSD3)", url = "https://github.com/rwth-acis/las2peer-tmitocar-Service/blob/master/LICENSE")))
	@Path("/task")
	public static class WritingTask {
		TmitocarService service = (TmitocarService) Context.get().getService();

		@GET
		@Path("/{tasknr}")
		@Produces(MediaType.APPLICATION_JSON)
		@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "") })
		@ApiOperation(value = "getAllTasks", notes = "Returns writing task by id")
		public Response getWritingTaskByNr(@PathParam("tasknr") int tasknr, @QueryParam("courseId") int courseId) {
			
			Connection conn = null;
			PreparedStatement stmt = null;
			ResultSet rs = null;
			JSONArray jsonArray = new JSONArray();
			String chatMessage = "";
			try {
				conn = service.getConnection();
				if (tasknr == 0 && courseId == 0) {
					stmt = conn.prepareStatement("SELECT * FROM writingtask");
				} else if (tasknr !=0 && courseId == 0) {
					stmt = conn.prepareStatement("SELECT * FROM writingtask WHERE nr = ?");
					stmt.setInt(1, tasknr);
				} else if (tasknr ==0 && courseId != 0) {
					stmt = conn.prepareStatement("SELECT * FROM writingtask WHERE courseid = ?");
					stmt.setInt(1, courseId);
				} else {
					stmt = conn.prepareStatement("SELECT * FROM writingtask WHERE nr = ? AND courseid = ?");
					stmt.setInt(1, tasknr);
					stmt.setInt(2, courseId);
				}
				rs = stmt.executeQuery();

				while (rs.next()) {
					courseId = rs.getInt("courseid");
					int nr = rs.getInt("nr");
					String text = rs.getString("text");
					String title = rs.getString("title");

					JSONObject jsonObject = new JSONObject();
					jsonObject.put("courseId", courseId);
					jsonObject.put("nr", nr);
					jsonObject.put("text", text);
					jsonObject.put("title", title);

					jsonArray.add(jsonObject);
					chatMessage += nr+": "+title+"\n<br>\n" + text + "\n<br>";
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				try {
					if (rs != null) {
						rs.close();
					}
					if (stmt != null) {
						stmt.close();
					}
					if (conn != null) {
						conn.close();
					}
				} catch (SQLException ex) {
					System.out.println(ex.getMessage());
				}
			}
			JSONObject response = new JSONObject();
			response.put("data", jsonArray);
			response.put("chatMessage", chatMessage);
			return Response.ok().entity(response.toString()).build();
		}

	}

	@Api(value = "Text Resource")
	@SwaggerDefinition(info = @Info(title = "Text Resource", version = "1.0.0", description = "This API is responsible for storing a text file on the T-MITOCAR server for later use as a comparison text.", termsOfService = "https://tech4comp.de/", contact = @Contact(name = "Alexander Tobias Neumann", url = "https://tech4comp.dbis.rwth-aachen.de/", email = "neumann@dbis.rwth-aachen.de"), license = @License(name = "ACIS License (BSD3)", url = "https://github.com/rwth-acis/las2peer-tmitocar-Service/blob/master/LICENSE")))
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
		 * @return id of the stored file
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
				@FormDataParam("file") InputStream textInputStream,
				@FormDataParam("file") FormDataContentDisposition textFileDetail, @FormDataParam("type") String type,
				@FormDataParam("topic") String topic, @FormDataParam("template") String template,
				@FormDataParam("wordSpec") String wordSpec) throws ParseException, IOException {
			if (isActive.getOrDefault(label1, false)) {
				return Response.status(Status.BAD_REQUEST).entity("User: " + label1 + " currently busy.").build();
			}
			File templatePath = new File("tmitocar/templates/" + template);
			if (!templatePath.exists()){
				JSONObject err = new JSONObject();
				err.put("errorMessage", "Template: " + template + " not found.");
				err.put("error", true);
				return Response.status(Status.NOT_FOUND).entity(err.toJSONString()).build();
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
	@SwaggerDefinition(info = @Info(title = "Feedback Resource", version = "1.0.0", description = "This API is responsible for handling text documents in txt or pdf format and sending them to T-MITOCAR for processing. The feedback is then saved in a MongoDB and the document IDs are returned.", termsOfService = "https://tech4comp.de/", contact = @Contact(name = "Alexander Tobias Neumann", url = "https://tech4comp.dbis.rwth-aachen.de/", email = "neumann@dbis.rwth-aachen.de"), license = @License(name = "ACIS License (BSD3)", url = "https://github.com/rwth-acis/las2peer-tmitocar-Service/blob/master/LICENSE")))
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
		 * @return the id of the stored file
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
				@FormDataParam("file") InputStream textInputStream,
				@FormDataParam("file") FormDataContentDisposition textFileDetail, @FormDataParam("type") String type,
				@FormDataParam("topic") String topic, @FormDataParam("template") String template,
				@FormDataParam("wordSpec") String wordSpec,@FormDataParam("email") String email,@FormDataParam("courseId") int courseId) throws ParseException, IOException {
			if (isActive.getOrDefault(label1, false)) {
				JSONObject err = new JSONObject();
				err.put("errorMessage", "User: " + label1 + " currently busy.");
				err.put("error", true);
				return Response.status(Status.NOT_FOUND).entity(err.toJSONString()).build();
			}

			
			File templatePath = new File("tmitocar/templates/" + template);
			if (!templatePath.exists()){
				JSONObject err = new JSONObject();
				err.put("errorMessage", "Template: " + template + " not found.");
				err.put("error", true);
				return Response.status(Status.NOT_FOUND).entity(err.toJSONString()).build();
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
			String uuid = service.getUuidByEmail(email);
			if (uuid!=null){
				// user has accepted
				LrsCredentials lrsCredentials = service.getLrsCredentialsByCourse(courseId);
				if(lrsCredentials!=null){
					JSONObject xapi = service.prepareXapiStatement(uuid, topic, courseId, uploaded.toString());
					String toEncode = lrsCredentials.getClientKey()+"."+lrsCredentials.getClientSecret();
					String encodedString = Base64.encodeBytes(toEncode.getBytes());

					service.sendXAPIStatement(xapi, encodedString);
				}
			}
			Gson g = new Gson();
			return Response.ok().entity(g.toJson(response)).build();
		}

		@GET
		@Path("/{label1}")
		@Produces(MediaType.APPLICATION_JSON)
		@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "") })
		@ApiOperation(value = "getAnalyzedText", notes = "Returns analyzed text report (PDF)")
		public Response getAnalyzedText(@PathParam("label1") String label1) throws ParseException, IOException {
			if (!isActive.containsKey(label1)) {
				JSONObject err = new JSONObject();
				err.put("errorMessage", "User: " + label1 + " not found.");
				err.put("error", true);
				return Response.status(Status.NOT_FOUND).entity(err.toJSONString()).build();
			}
			if (isActive.get(label1)) {
				JSONObject err = new JSONObject();
				err.put("errorMessage", "User: " + label1 + " currently busy.");
				err.put("error", true);
				return Response.status(Status.NOT_FOUND).entity(err.toJSONString()).build();
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
				err.put("error", true);
				return Response.status(Status.BAD_REQUEST).entity(err.toJSONString()).build();
			}

			if (fileId == null) {
				err.put("errorMessage", "Something went wrong storing the feedback for " + label1);
				err.put("error", true);
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
		 * @return the id of the stored file
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
				@FormDataParam("file") InputStream textInputStream,
				@FormDataParam("file") FormDataContentDisposition textFileDetail, @FormDataParam("type") String type,
				@FormDataParam("topic") String topic, @FormDataParam("template") String template,
				@FormDataParam("wordSpec") String wordSpec,@FormDataParam("email") String email,@FormDataParam("courseId") int courseId) throws ParseException, IOException {
			if (isActive.getOrDefault(label1, false)) {
				JSONObject err = new JSONObject();
				err.put("errorMessage", "User: " + label1 + " currently busy.");
				err.put("error", true);
				return Response.status(Status.NOT_FOUND).entity(err.toJSONString()).build();
			}
			
			File templatePath = new File("tmitocar/templates/" + template);
			if (!templatePath.exists()){
				JSONObject err = new JSONObject();
				err.put("errorMessage", "Template: " + template + " not found.");
				err.put("error", true);
				return Response.status(Status.NOT_FOUND).entity(err.toJSONString()).build();
			}
			
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

			String uuid = service.getUuidByEmail(email);
			if (uuid!=null){
				// user has accepted
				LrsCredentials lrsCredentials = service.getLrsCredentialsByCourse(courseId);
				if(lrsCredentials!=null){
					JSONObject xapi = service.prepareXapiStatement(uuid, topic, courseId, uploaded.toString());
					String toEncode = lrsCredentials.getClientKey()+"."+lrsCredentials.getClientSecret();
					String encodedString = Base64.encodeBytes(toEncode.getBytes());
					service.sendXAPIStatement(xapi, encodedString);
				}
			}
			Gson g = new Gson();
			return Response.ok().entity(g.toJson(response)).build();
		}

		@GET
		@Path("/{label1}/compare/{label2}")
		@Consumes(MediaType.MULTIPART_FORM_DATA)
		@Produces(MediaType.APPLICATION_JSON)
		@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "") })
		@ApiOperation(value = "getComparedText", notes = "Returns compared text report (PDF)")
		public Response getComparedText(@PathParam("label1") String label1, @PathParam("label2") String label2)
				throws ParseException, IOException {
			if (!isActive.containsKey(label1)) {
				JSONObject err = new JSONObject();
				err.put("errorMessage", "User: " + label1 + " not found.");
				err.put("error", true);
				return Response.status(Status.NOT_FOUND).entity(err.toJSONString()).build();
			}
			if (isActive.get(label1)) {
				JSONObject err = new JSONObject();
				err.put("errorMessage", "User: " + label1 + " currently busy.");
				err.put("error", true);
				return Response.status(Status.NOT_FOUND).entity(err.toJSONString()).build();
			}
			JSONObject err = new JSONObject();
			ObjectId feedbackFileId = null;
			ObjectId graphFileId = null;
			if (userTexts.get(label1) != null) {
				System.out.println("Storing PDF to mongodb...");
				feedbackFileId = service.storeLocalFileRemote("comparison_" + label1 + "_vs_" + label2 + ".pdf");
				graphFileId = service.storeLocalFileRemote("comparison_" + label1 + "_vs_" + label2 + ".json");

			} else {
				err.put("errorMessage", "Something went wrong storing the feedback for " + label1);
				err.put("error", true);
				return Response.status(Status.BAD_REQUEST).entity(err.toJSONString()).build();
			}

			if (feedbackFileId == null) {
				err.put("errorMessage", "Something went wrong storing the feedback for " + label1);
				err.put("error", true);
				return Response.status(Status.BAD_REQUEST).entity(err.toJSONString()).build();
			}
			if (graphFileId == null) {
				err.put("errorMessage", "Something went wrong storing the graph for " + label1);
				err.put("error", true);
				return Response.status(Status.BAD_REQUEST).entity(err.toJSONString()).build();
			}
			TmitocarResponse response = new TmitocarResponse(null, feedbackFileId.toString(), graphFileId.toString());
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

	private ObjectId storeLocalFileRemote(String fileName) {
		ObjectId fileId = null;
		try {
			byte[] pdfByte = Files.readAllBytes(
					Paths.get("tmitocar/" + fileName));
			fileId = storeFile(fileName, pdfByte);
			Files.delete(Paths.get("tmitocar/" + fileName));
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

	private String createFileName(String name, String type) {
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

	private void deleteFileLocally(String name) {
		// or should we delete the user folder afterwards?
		try {
			Files.delete(Paths.get("tmitocar/texts/" + name + "/" + name + ".txt"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			Files.delete(Paths.get("tmitocar/texts/" + name + "/" + name + ".pdf"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void sendXAPIStatement(JSONObject xAPI, String lrsAuthToken) {
		// Copy pasted from LL service
		// POST statements
		try {
			URL url = new URL(lrsURL + "/data/xAPI/statements");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
			conn.setRequestProperty("X-Experience-API-Version", "1.0.3");
			conn.setRequestProperty("Authorization", "Basic " + lrsAuthToken);
			conn.setRequestProperty("Cache-Control", "no-cache");
			conn.setUseCaches(false);

			OutputStream os = conn.getOutputStream();
			os.write(xAPI.toString().getBytes("utf-8"));
			os.flush();

			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
			String line = "";
			StringBuilder response = new StringBuilder();

			while ((line = reader.readLine()) != null) {
				response.append(line);
			}
			logger.info(response.toString());

			conn.disconnect();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private String getUuidByEmail(String email){
		String res = null;
		try (Connection conn = getConnection();
			PreparedStatement pstmt = conn.prepareStatement("SELECT uuid FROM personmapping WHERE email = ?")) {

			// Set the email parameter in the prepared statement
			pstmt.setString(1, email);

			// Execute the query and retrieve the result set
			try (ResultSet rs = pstmt.executeQuery()) {

				// If the email exists in the table, the result set will contain one row with the UUID
				if (rs.next()) {
					res = rs.getString("uuid");
				} else {
					System.out.println("No UUID found for " + email);
				}
			}
		} catch (SQLException e) {
			// Handle any SQL errors
			e.printStackTrace();
		}
		return res;
	}

	private LrsCredentials getLrsCredentialsByCourse(int courseId){
		LrsCredentials res = null;
		try (Connection conn = getConnection();
			PreparedStatement pstmt = conn.prepareStatement("SELECT clientkey,clientsecret FROM lrsstoreforcourse WHERE courseid = ?")) {

			// Set the email parameter in the prepared statement
			pstmt.setInt(1, courseId);

			// Execute the query and retrieve the result set
			try (ResultSet rs = pstmt.executeQuery()) {

				// If the email exists in the table, the result set will contain one row with the UUID
				if (rs.next()) {
					String key = rs.getString("clientkey");
					String secret = rs.getString("clientsecret");
					res = new LrsCredentials(key, secret);
				} else {
					System.out.println("No lrs information found for course " + courseId);
				}
			}
		} catch (SQLException e) {
			// Handle any SQL errors
			e.printStackTrace();
		}
		return res;
	}

	private JSONObject prepareXapiStatement(String user, String topic, int course, String fileId) throws ParseException{
		JSONParser p = new JSONParser(JSONParser.MODE_PERMISSIVE);
		JSONObject actor = new JSONObject();
		actor.put("objectType", "Agent");
		JSONObject account = new JSONObject();

		account.put("name", user);
		account.put("homePage", "https://chat.tech4comp.dbis.rwth-aachen.de");
		actor.put("account", account);
		
		JSONObject verb = (JSONObject) p
				.parse(new String("{'display':{'en-US':'sent_file'},'id':'https://tech4comp.de/xapi/verb/sent_file'}"));
		JSONObject object = (JSONObject) p
				.parse(new String("{'definition':{'interactionType':'other', 'name':{'en-US':'" + topic
						+ "'}, 'description':{'en-US':'" + topic
						+ "'}, 'type':'https://tech4comp.de/xapi/activitytype/file'},'id':'https://tech4comp.de/tmitocar/file/"
						+ fileId + "', 'objectType':'Activity'}"));
		JSONObject context = (JSONObject) p.parse(new String(
				"{'extensions':{'https://tech4comp.de/xapi/context/extensions/file':{'id':'"
						+ fileId + "','topic':'"
						+ topic
						+ "','course':'" + course + "'}}}"));
		JSONObject xAPI = new JSONObject();

		xAPI.put("authority", p.parse(
				new String("{'objectType': 'Agent','name': 'New Client', 'mbox': 'mailto:hello@learninglocker.net'}")));
		xAPI.put("context", context); //
		// xAPI.put("timestamp", java.time.LocalDateTime.now());
		xAPI.put("actor", actor);
		xAPI.put("object", object);
		xAPI.put("verb", verb);
		return xAPI;
	}

}
