package services.tmitocar.service;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.bson.BsonDocument;
import org.bson.BsonInt64;
import org.bson.BsonObjectId;
import org.bson.Document;
import org.bson.UuidRepresentation;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import com.fasterxml.jackson.databind.JsonSerializable.Base;
import com.mongodb.BasicDBObject;
import com.mongodb.ConnectionString;
import com.mongodb.DBObject;
import com.mongodb.MongoClientSettings;
// import com.mongodb.ConnectionString;
// import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
// import com.mongodb.client.MongoClient;
// import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.model.GridFSFile;
// import com.mongodb.client.model.Filters;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import services.tmitocar.model.TmitocarFiles;
import services.tmitocar.model.WritingTask;
import services.tmitocar.pojo.LrsCredentials;
import services.tmitocar.pojo.TmitocarText;
import services.tmitocar.repository.FilesRepository;
import services.tmitocar.repository.WritingTaskRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.stereotype.Service;

@Service
public class TmitocarService {
	public String publicKey;
	public String lrsURL;

	public HashMap<String, Boolean> isActive = null;
	public HashMap<String, String> userTexts = null;

	public String xapiUrl;
	public String xapiHomepage;

	public final static String AUTH_FILE = "tmitocar/auth.json";

	@Autowired
	private WritingTaskRepository writingTaskRepository;

	@Autowired
	private GridFsTemplate gridFsTemplate;

	@Autowired
	private GridFsOperations gridFsOperations;
	
	// This is the constructor of the TmitocarService class.
	public TmitocarService(WritingTaskRepository repository) {
		
		// Set postgresql connection for WritingTasks
		this.writingTaskRepository = repository;

		// Call the setFieldValues() method to initialize some fields.
		// setFieldValues();
	
		// Call the initVariables() method to initialize some variables.
		initVariables();
	
		// Call the initAuth() method to initialize the authentication mechanism.
		// initAuth();
	
		// Call the initDB() method to initialize the database connection.
		// initDB();
	
	}
	
	// protected void initResources() {
	// 	getResourceConfig().register(this);
	// 	getResourceConfig().register(Feedback.class);
	// 	getResourceConfig().register(TMitocarText.class);
	// 	getResourceConfig().register(WritingTask.class);
	// 	getResourceConfig().register(Analysis.class);
	// 	getResourceConfig().register(FAQ.class);
	// 	getResourceConfig().register(Credits.class);
	// }

	public void initVariables() {
		if (isActive == null) {
			isActive = new HashMap<String, Boolean>();
		}
		if (userTexts == null) {
			userTexts = new HashMap<String, String>();
		}
	}

	// This is a public method of the TmitocarService class used to initialize the
	// authentication mechanism.
	public void initAuth() {
		// Create a new File object with the AUTH_FILE file path.
		File f = new File(AUTH_FILE);

		// Check if the file does not exist.
		if (Files.notExists(f.toPath())) {
			// If the file does not exist, create a new JSONObject to store the public and
			// public keys.
			JSONObject j = new JSONObject();
			j.put("ukey", publicKey);
			j.put("pkey", publicKey);

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

	@Autowired
	private MongoTemplate mongoTemplate;

	public MongoDatabase getMongoDatabase() {
		return mongoTemplate.getDb();
	}

	public List<WritingTask> getWritingTasks() {
		return (List<WritingTask>) writingTaskRepository.findAll();
	}

	public List<WritingTask> findTasksByCourseId(int courseId) {
		return writingTaskRepository.findTasksByCourseId(courseId);
	}

	public WritingTask findByCourseIdAndNr(int courseId, int nr) {
		return writingTaskRepository.findByCourseIdAndNr(courseId, nr);
	}

    public String convertInputStreamToBase64(InputStream inputStream) throws IOException {
		Encoder e = Base64.getEncoder();
		byte[] bytes = inputStream.readAllBytes();
		return e.encodeToString(bytes);
	}

	public ObjectId storeFile(String filename, byte[] bytesToStore) {
		ObjectId fileId = null;
		try {
			ByteArrayInputStream inputStream = new ByteArrayInputStream(bytesToStore);
			fileId = gridFsTemplate.store(inputStream, filename);
			System.out.println("File uploaded successfully with ID: " + fileId.toString());
			try {
				inputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (MongoException me) {
			System.err.println(me);
		}
		return fileId;
	}

	public ObjectId storeLocalFileRemote(String fileName) {
		return storeLocalFileRemote(fileName, null);
	}

	public ObjectId storeLocalFileRemote(String fileName, String renameFile) {
		ObjectId fileId = null;
		try {
			byte[] pdfByte = Files.readAllBytes(
					Paths.get("tmitocar/" + fileName));
			if(fileName==null){
				fileId = storeFile(fileName, pdfByte);
			}else{
				fileId = storeFile(renameFile, pdfByte);
			}
			Files.delete(Paths.get("tmitocar/" + fileName));
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Failed storing PDF.");
		}
		return fileId;
	}

	public TmitocarFiles getFile(ObjectId fileId) {

		GridFSFile file = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(fileId)));
		TmitocarFiles f = new TmitocarFiles();
		if (file != null) {
			f.setFilename(file.getFilename());
			f.setFile(file.getObjectId().toByteArray());
		} else {
			return null;
		}
		return f;
	}

	public static String readTxtFile(String fileName) {
		String text = "";
		try {
			text = new String(Files.readAllBytes(Paths.get(fileName)));
		} catch (IOException e) {
			e.printStackTrace();
		}

		return text;
	}

	public String readDocXFile(String fileName) {
		String parsedText = "";
		File file = new File(fileName);
		try {
			FileInputStream inputStream = new FileInputStream(file);
			XWPFDocument document = new XWPFDocument(inputStream);
			try (XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
				parsedText = extractor.getText();
			}
			inputStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return parsedText;
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

	public String createFileName(String name, String type) {
		if (type.toLowerCase().equals("text/plain") || type.toLowerCase().equals("text")) {
			return name + ".txt";
		} else if (type.toLowerCase().equals("application/pdf") || type.toLowerCase().equals("pdf")) {
			return name + ".pdf";
		} else if (type.equalsIgnoreCase("application/vnd.openxmlformats-officedocument.wordprocessingml.document") || type.equalsIgnoreCase("docx")) {
			return name + ".docx";
		}
		return name + "txt";
	}

	public boolean storeFileLocally(String name, String text, String type) {
		String textContent = "";
		// problem here with the file name no? I mean if two threads do this, we will
		// have one file overwriting the other?
		String fileName = createFileName(name, type);
		System.out.println("Write File");

		File f = new File("tmitocar/texts/" + name + "/" + fileName);
		try {
			boolean b = f.getParentFile().mkdirs();
			b = f.createNewFile();
            Decoder d = Base64.getDecoder();
			if (type.toLowerCase().equals("text/plain") || type.toLowerCase().equals("text")) {
				/*
				 * FileWriter writer = new FileWriter(f);
				 * writer.write(body.getText().toLowerCase()); writer.close();
				 */
				byte[] decodedBytes = d.decode(text);
				System.out.println(decodedBytes);
				FileUtils.writeByteArrayToFile(f, decodedBytes);
				textContent = readTxtFile("tmitocar/texts/" + name + "/" + fileName);
			} else if (type.toLowerCase().equals("application/pdf") || type.toLowerCase().equals("pdf")) {
                byte[] decodedBytes = d.decode(text);
				System.out.println(decodedBytes);
				FileUtils.writeByteArrayToFile(f, decodedBytes);
				textContent = readPDFFile("tmitocar/texts/" + name + "/" + fileName);
			} else if (type.equalsIgnoreCase("application/vnd.openxmlformats-officedocument.wordprocessingml.document") || type.equalsIgnoreCase("docx")) {
				byte[] decodedBytes = d.decode(text);
				FileUtils.writeByteArrayToFile(f, decodedBytes);
				textContent = readDocXFile("tmitocar/texts/" + name + "/" + fileName);
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

	public void deleteFileLocally(String name) {
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
		try {
			Files.delete(Paths.get("tmitocar/texts/" + name + "/" + name + ".docx"));
		} catch (IOException e) {
			e.printStackTrace();
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

	private void generateFeedback(String label1, String label2, String template, String topic, String scriptFile)
			throws InterruptedException, IOException {
		ProcessBuilder pb = new ProcessBuilder("bash", scriptFile, "-s", "-o", "pdf", "-i",
				"comparison_" + label1 + "_vs_" + label2 + ".json", "-t",
				"templates/" + template, "-S", topic);
		pb.inheritIO();
		pb.directory(new File("tmitocar"));
		Process p = pb.start();
		p.waitFor();
		cleanJSONFile("tmitocar/comparison_" + label1 + "_vs_" + label2 + ".json");
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
	public boolean compareText(@RequestParam("label1") String label1, @RequestParam("label2") String label2,
			@RequestParam("template") String template, TmitocarText body, String callbackUrl, String sourceFileId) {
		isActive.put(label1, true);
		JSONObject j = new JSONObject();
		j.put("user", label1);
		// Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_83, j.toJSONString());
		System.out.println("Block " + label1);
		JSONObject error = new JSONObject();

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

						ObjectId feedbackFileId = null;
						ObjectId graphFileId = null;
						
						System.out.println("Storing PDF to mongodb...");
						feedbackFileId = storeLocalFileRemote("comparison_" + label1 + "_vs_" + label2 + ".pdf",body.getTopic()+"-feedback.pdf");
						graphFileId = storeLocalFileRemote("comparison_" + label1 + "_vs_" + label2 + ".json",body.getTopic()+"-graph.json");

						if (feedbackFileId == null) {
							System.out.println("Something went wrong storing the feedback for " + label1);
							// err.put("errorMessage", "Something went wrong storing the feedback for " + label1);
							// err.put("error", true);
							// TODO
						}else{
							System.out.println("Feedback: "+ feedbackFileId.toString());
						}
						if (graphFileId == null) {
							System.out.println("Something went wrong storing the graph for " + label1);
							// err.put("errorMessage", "Something went wrong storing the graph for " + label1);
							// err.put("error", true);
							// TODO
						}else{
							System.out.println("Graph: "+ graphFileId.toString());
						}
						// LRS Store feedback
						String[] courseAndTask = label2.split("-");

						// String uuid = getUuidByEmail(body.getUuid());
						// 	if (uuid!=null){
						// 		// user has accepted
						// 	LrsCredentials lrsCredentials = getLrsCredentialsByCourse(Integer.parseInt(courseAndTask[0]));
						// 	if(lrsCredentials!=null){
						// 		JSONObject xapi = prepareXapiStatement(uuid, "received_feedback", body.getTopic(), Integer.parseInt(courseAndTask[0]),Integer.parseInt(courseAndTask[1]),  graphFileId.toString(), feedbackFileId.toString(), sourceFileId);
						// 		String toEncode = lrsCredentials.getClientKey()+":"+lrsCredentials.getClientSecret();
						// 		Encoder e = Base64.getEncoder();
						// 		String encodedString = e.encodeToString(toEncode.getBytes());
						// 		sendXAPIStatement(xapi, encodedString);
						// 	}
						// }

						JSONObject steve = new JSONObject();
						// example, should be replaced with actual stuff
						steve.put("graphFileId", graphFileId.toString());
						steve.put("feedbackFileId", feedbackFileId.toString());
						callBack(callbackUrl, label1, label1, label2, steve);

						isActive.put(label1, false);
					} catch (IOException e) {
						e.printStackTrace();
						isActive.put(label1, false);
						error.put("error", e.toString());
						callBack(callbackUrl, label1, label1, label2, error);
					} catch (InterruptedException e) {
						e.printStackTrace();
						isActive.put(label1, false);
						error.put("error", e.toString());
						callBack(callbackUrl, label1, label1, label2, error);
					// } catch (ParseException e) {
					// 	e.printStackTrace();
					// 	isActive.put(label1, false);
					// 	error.put("error", e.toString());
					// 	callBack(callbackUrl, label1, label1, label2, error);
					}
					catch (Exception e) {
						e.printStackTrace();
						isActive.put(label1, false);
						error.put("error", e.toString());
						callBack(callbackUrl, label1, label1, label2, error);
					}
				}
			}).start();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			isActive.put(label1, false);
			error.put("error", e.toString());
			callBack(callbackUrl, label1, label1, label2, error);
			return false;
		}
	}

	// public boolean llm_feedback(@RequestParam("label1") String label1, @RequestParam("label2") String label2,
	// 		@RequestParam("template") String template, TmitocarText body, String callbackUrl, String sourceFileId) {
	// 	// TmitocarServiceController service = (TmitocarServiceController) Context.get().getService();
	// 	isActive.put(label1, true);
	// 	JSONObject j = new JSONObject();
	// 	j.put("user", label1);
	// 	// Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_83, j.toJSONString());
	// 	System.out.println("Block " + label1);
	// 	JSONObject error = new JSONObject();
	// 	JSONObject newText = new JSONObject();
	// 	CodecRegistry pojoCodecRegistry = fromProviders(PojoCodecProvider.builder().automatic(true).build());
	// 	CodecRegistry codecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), pojoCodecRegistry);
	// 	MongoClientSettings settings = MongoClientSettings.builder()
	// 			.uuidRepresentation(UuidRepresentation.STANDARD)
	// 			.applyConnectionString(new ConnectionString(mongoUri))
	// 			.codecRegistry(codecRegistry)
	// 			.build();
	// 	// Create a new client and connect to the server
	// 	MongoClient mongoClient = MongoClients.create(settings);

	// 	try {
	// 		new Thread(new Runnable() {
	// 			@Override
	// 			public void run() {
	// 				String[] courseAndTask = label2.split("-");
	// 				int courseId = Integer.parseInt(courseAndTask[0]);
	// 				Connection conn = null;
	// 				PreparedStatement stmt = null;
	// 				ResultSet rs = null;

	// 				storeFileLocally(label1, body.getText(), body.getType());
	// 				System.out.println("Upload text");
					
	// 				try {
	// 					int basisId = 0;

	// 					conn = getConnection();
	// 					stmt = conn.prepareStatement("SELECT * FROM course WHERE id = ? ORDER BY id ASC");
	// 					stmt.setInt(1, courseId);
	// 					rs = stmt.executeQuery();
	// 					while (rs.next()) {
	// 						basisId = rs.getInt("basecourseid");
	// 						System.out.println("BasecourseID is: " + basisId);
	// 					}
	// 					String taskNr = basisId + "-" + courseAndTask[1];
	// 					System.out.println("New taskNr is: " + taskNr);
	// 					// Store usertext with label
	// 					String wordspec = body.getWordSpec();
	// 					String fileName = createFileName(label1, body.getType());
	// 					uploadToTmitocar(label1, fileName, wordspec);

	// 					System.out.println("Compare with expert.");
	// 					// compare with expert text
	// 					createComparison(label1, label2);

	// 					System.out.println("Store graph to MongoDB...");
	// 					ObjectId graphFileId = storeLocalFileRemote("comparison_" + label1 + "_vs_" + label2 + ".json",body.getTopic()+"-graph.json");

	// 					newText.put("userId", label1);

	// 					File f = new File("tmitocar/texts/" + label1 + "/"+ label1 + ".txt-cleaned.txt");
	// 					File f_send = new File("tmitocar/texts/" + label1 + "/"+ label1 + "-" + courseAndTask[1] + ".txt-cleaned.txt");
	// 					if (f.exists()) {
	// 						System.out.println("Get content from -cleaned.txt.");
	// 						newText.put("studentInput", readTxtFile("tmitocar/texts/" + label1 + "/"+ label1 + ".txt-cleaned.txt"));
	// 						f.renameTo(f_send);
	// 					} else {
	// 						newText.put("studentInput", userTexts.get(label1));
	// 					}
						
	// 					newText.put("taskNr", taskNr);
	// 					newText.put("timestamp", System.currentTimeMillis());
	// 					System.out.println("New Text is:" + newText);

	// 					try {
	// 						// get keywords from file 
	// 						MongoDatabase database = mongoClient.getDatabase(mongoDB);
	// 						GridFSBucket gridFSBucket = GridFSBuckets.create(database, "files");
	// 						gridFSBucket.find(Filters.empty());
	// 						BsonObjectId bId = new BsonObjectId(graphFileId);
	// 						GridFSFile file = gridFSBucket.find(Filters.eq(bId)).first();
	// 						if (file == null) {
	// 							System.out.println("File with ID "+graphFileId+" not found");
	// 						}
	// 						// TODO
	// 						// HttpResponse<String> response = ResponseEntity.ok(file.getObjectId().toHexString());
	// 						// response.header("Content-Disposition", "attachment; filename=\"" + file.getFilename() + "\"");
	// 						HttpHeaders headers = new HttpHeaders();
    //    						headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");

	// 						// Download the file to a ByteArrayOutputStream
	// 						ByteArrayOutputStream baos = new ByteArrayOutputStream();
	// 						gridFSBucket.downloadToStream(file.getObjectId(), baos);
	// 						String jsonStr = baos.toString();
	// 						JSONParser parser = new JSONParser(JSONParser.MODE_PERMISSIVE);
	// 						JSONObject jsonObject = (JSONObject) parser.parse(jsonStr);
	// 						JSONArray begriffeDiffB = (JSONArray) jsonObject.get("BegriffeDiffB");
	// 						newText.put("keywords", begriffeDiffB);							
	// 					} catch (MongoException me) {
	// 						System.err.println(me);
	// 					} finally {
	// 						mongoClient.close();
	// 					}

	// 					System.out.println("Get llm-generated feedback and store as markdown.");
	// 					//get LLM-generated feedback
	// 					String url = "http://16.171.64.118:8000/input/recommend";
	// 					HttpClient httpClient = HttpClient.newHttpClient();
	// 					HttpRequest httpRequest = HttpRequest.newBuilder()
	// 							.uri(UriBuilder.fromUri(url).build())
	// 							.header("Content-Type", "application/json")
	// 							.POST(HttpRequest.BodyPublishers.ofString(newText.toJSONString()))
	// 							.build();

	// 					// Send the request
	// 					HttpResponse<String> serviceResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
	// 					System.out.println("Response Code:" + serviceResponse.statusCode());
	// 					System.out.println("Response Text" + serviceResponse.body());
	// 					JSONParser parser = new JSONParser();
	// 					JSONObject responseBody = (JSONObject) parser.parse(serviceResponse.body());
						
	// 					String r = responseBody.get("response").toString();
	// 					String pattern = "\n(?!\\n)";
	// 					String r_new = r.replaceAll(pattern, "\n\n");

	// 					System.out.println("Write response to markdown.");
	// 					//store response as markdown
	// 					FileWriter writer = new FileWriter("tmitocar/comparison_" + label1 + "_vs_" + label2 + ".md");
	// 					writer.write(r_new);
	// 					writer.close();

	// 					System.out.println("Convert markdown to pdf.");
	// 					// store markdown as pdf
	// 					ProcessBuilder pb = new ProcessBuilder("pandoc", "comparison_" + label1 + "_vs_" + label2 + ".md" , "-o", "comparison_" + label1 + "_vs_" + label2 + ".pdf","--wrap=preserve");
	// 					pb.inheritIO();
	// 					pb.directory(new File("tmitocar"));
	// 					Process process2 = pb.start();
	// 					process2.waitFor();
	// 					Files.delete(Paths.get("tmitocar/comparison_" + label1 + "_vs_" + label2 + ".md"));

	// 					System.out.println("Storing PDF to mongodb...");
	// 					ObjectId feedbackFileId = storeLocalFileRemote("comparison_" + label1 + "_vs_" + label2 + ".pdf" ,body.getTopic()+"-feedback.pdf");

	// 					String uuid = getUuidByEmail(body.getUuid());
	// 						if (uuid!=null){
	// 							// user has accepted
	// 						LrsCredentials lrsCredentials = getLrsCredentialsByCourse(Integer.parseInt(courseAndTask[0]));
	// 						if(lrsCredentials!=null){
    //                             Encoder e = Base64.getEncoder();
	// 							JSONObject xapi = prepareXapiStatement(uuid, "received_feedback", body.getTopic(), Integer.parseInt(courseAndTask[0]),Integer.parseInt(courseAndTask[1]),  graphFileId.toString(), feedbackFileId.toString(), sourceFileId);
	// 							String toEncode = lrsCredentials.getClientKey()+":"+lrsCredentials.getClientSecret();
	// 							String encodedString = e.encodeToString(toEncode.getBytes());
	// 							sendXAPIStatement(xapi, encodedString);
	// 						}
	// 					}

	// 					JSONObject steve = new JSONObject();
	// 					// example, should be replaced with actual stuff
	// 					steve.put("graphFileId", graphFileId.toString());
	// 					steve.put("feedbackFileId", feedbackFileId.toString());
	// 					callBack(callbackUrl, label1, label1, label2, steve);

	// 					isActive.put(label1, false);
	// 				} catch (IOException e) {
	// 					e.printStackTrace();
	// 					isActive.put(label1, false);
	// 					error.put("error", e.toString());
	// 					callBack(callbackUrl, label1, label1, label2, error);
	// 				} catch (InterruptedException e) {
	// 					e.printStackTrace();
	// 					isActive.put(label1, false);
	// 					error.put("error", e.toString());
	// 					callBack(callbackUrl, label1, label1, label2, error);
	// 				} catch (ParseException e) {
	// 					e.printStackTrace();
	// 					isActive.put(label1, false);
	// 					error.put("error", e.toString());
	// 					callBack(callbackUrl, label1, label1, label2, error);
	// 				} catch (SQLException e) {
	// 					e.printStackTrace();
	// 				} catch (Exception e) {
	// 					e.printStackTrace();
	// 					isActive.put(label1, false);
	// 					error.put("error", e.toString());
	// 					callBack(callbackUrl, label1, label1, label2, error);
	// 				} finally {
	// 					try {
	// 						if (rs != null) {
	// 							rs.close();
	// 						}
	// 						if (stmt != null) {
	// 							stmt.close();
	// 						}
	// 						if (conn != null) {
	// 							conn.close();
	// 						}
	// 					} catch (SQLException ex) {
	// 						System.out.println(ex.getMessage());
	// 					}
	// 				}
	// 			} 
	// 		}).start();
	// 		return true;
	// 	} catch (Exception e) {
	// 		e.printStackTrace();
	// 		isActive.put(label1, false);
	// 		error.put("error", e.toString());
	// 		callBack(callbackUrl, label1, label1, label2, error);
	// 		return false;
	// 	}
	// }

	public ResponseEntity<String> processSingleText(String user, String expert, String template, TmitocarText body) {
		isActive.put(user, true);
		JSONObject j = new JSONObject();
		j.put("user", user);
		// Context.get().monitorEvent(MonitoringEvent.SERVICE_CUSTOM_MESSAGE_83, j.toJSONString());
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
						Decoder d = Base64.getDecoder();
						if (type.toLowerCase().equals("text/plain") || type.toLowerCase().equals("text")) {
							byte[] decodedBytes = d.decode(body.getText());
							FileUtils.writeByteArrayToFile(f, decodedBytes);
							textContent = readTxtFile("tmitocar/texts/" + user + "/" + fileName);
						} else if (type.toLowerCase().equals("application/pdf") || type.toLowerCase().equals("pdf")) {
							byte[] decodedBytes = d.decode(body.getText());
							System.out.println(decodedBytes);
							FileUtils.writeByteArrayToFile(f, decodedBytes);
							textContent = readPDFFile("tmitocar/texts/" + user + "/" + fileName);
						} else if (type.equalsIgnoreCase("application/vnd.openxmlformats-officedocument.wordprocessingml.document") || type.equalsIgnoreCase("docx")) {
							byte[] decodedBytes = d.decode(body.getText());
							FileUtils.writeByteArrayToFile(f, decodedBytes);
							textContent = readDocXFile("tmitocar/texts/" + user + "/" + fileName);
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
			return ResponseEntity.ok("");
		} catch (Exception e) {
			e.printStackTrace();
			isActive.put(user, false);
			JSONObject err = new JSONObject();
			err.put("errorMessage", e.getMessage());
			err.put("error", true);
			return ResponseEntity.badRequest().body(err.toJSONString());
		}
	}

	// Helper method to format a JSONArray as a string
	public String formatJSONArray(JSONArray jsonArray) {
		StringBuilder builder = new StringBuilder();
		
		for (Object value : jsonArray) {
			String strValue = (String) value;
			// currently adjusted to fit the MWB frontend
			builder.append("- ").append(strValue).append("\n");
		}
		return builder.toString();
	}
	
	public void cleanJSONFile(String path){
		String[] attributesToKeep = {"Graph1Liste", "BegriffeSchnittmenge", "BegriffeDiffB"};
		try {
            // Read the JSON file
            BufferedReader reader = new BufferedReader(new FileReader(path));
            StringBuilder jsonString = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonString.append(line);
            }
            reader.close();

            // Parse the JSON
			JSONParser parser = new JSONParser(JSONParser.MODE_PERMISSIVE);
            JSONObject json = (JSONObject) parser.parse(jsonString.toString());

            // Create a new JSON object for cleaned version
            JSONObject cleanedJson = new JSONObject();

            // Extract specific attributes
            for (String attribute : attributesToKeep) {
                if (json.containsKey(attribute)) {
                    cleanedJson.put(attribute, json.get(attribute));
                }
            }

            // Write cleaned JSON to file
            FileWriter writer = new FileWriter(path);
            writer.write(cleanedJson.toString());
            writer.close();

            System.out.println("Cleaned JSON file created successfully.");

        } catch (Exception e) {
            e.printStackTrace();
        }
	}

	public void callBack(String callbackUrl, String uuid, String label1, String label2, JSONObject body){
		try {    
			System.out.println("Starting callback to botmanager with url: " + callbackUrl+ "/" + uuid + "/" + label1 + "/" + label2 + "files");
			Client textClient = ClientBuilder.newBuilder().register(MultiPartFeature.class).build();
            FormDataMultiPart mp = new FormDataMultiPart();
			System.out.println(body);
			mp.field("files", body.toJSONString());
			WebTarget target = textClient
					.target(callbackUrl + "/" + uuid + "/" + label1 + "/" + label2 + "/files");
			Response response = target.request()
					.post(javax.ws.rs.client.Entity.entity(mp, MediaType.MULTIPART_FORM_DATA_TYPE));
					String test = response.readEntity(String.class);
			System.out.println("Finished callback to botmanager with response: " + test);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendXAPIStatement(JSONObject xAPI, String lrsAuthToken) {
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
			System.out.println("XAPI Statement sent.");

			conn.disconnect();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public String getTaskNameByIds(int course, int task){
		String res = null;
		WritingTask t = findByCourseIdAndNr(course, task);
		res = t.getTitle();
		return res;
	}

	// public String getUuidByEmail(String email){
	// 	String res = null;
	// 	try (Connection conn = getConnection();
	// 		PreparedStatement pstmt = conn.prepareStatement("SELECT pm.uuid FROM personmapping pm JOIN person p ON pm.personid = p.id WHERE p.email = ?")) {

	// 		// Set the email parameter in the prepared statement
	// 		pstmt.setString(1, email);

	// 		// Execute the query and retrieve the result set
	// 		try (ResultSet rs = pstmt.executeQuery()) {

	// 			// If the email exists in the table, the result set will contain one row with the UUID
	// 			if (rs.next()) {
	// 				res = rs.getString("uuid");
	// 			} else {
	// 				System.out.println("No UUID found for " + email);
	// 			}
	// 		}
	// 	} catch (SQLException e) {
	// 		// Handle any SQL errors
	// 		e.printStackTrace();
	// 	}
	// 	return res;
	// }

	// public LrsCredentials getLrsCredentialsByCourse(int courseId){
	// 	LrsCredentials res = null;
	// 	try (Connection conn = getConnection();
	// 		PreparedStatement pstmt = conn.prepareStatement("SELECT clientkey,clientsecret FROM lrsstoreforcourse WHERE courseid = ?")) {

	// 		// Set the email parameter in the prepared statement
	// 		pstmt.setInt(1, courseId);

	// 		// Execute the query and retrieve the result set
	// 		try (ResultSet rs = pstmt.executeQuery()) {

	// 			// If the email exists in the table, the result set will contain one row with the UUID
	// 			if (rs.next()) {
	// 				String key = rs.getString("clientkey");
	// 				String secret = rs.getString("clientsecret");
	// 				res = new LrsCredentials(key, secret);
	// 			} else {
	// 				System.out.println("No lrs information found for course " + courseId);
	// 			}
	// 		}
	// 	} catch (SQLException e) {
	// 		// Handle any SQL errors
	// 		e.printStackTrace();
	// 	}
	// 	return res;
	// }

	// public JSONObject prepareXapiStatement(String user, String verbId, String topic, int course, int taskNr, String fileId, String fileId2, String source) throws ParseException{
	// 	JSONParser p = new JSONParser(JSONParser.MODE_PERMISSIVE);
	// 	JSONObject actor = new JSONObject();
	// 	actor.put("objectType", "Agent");
	// 	JSONObject account = new JSONObject();
	// 	account.put("name", user);
	// 	account.put("homePage", xapiHomepage);
	// 	actor.put("account", account);
	// 	JSONObject verb = (JSONObject) p
	// 			.parse(new String("{'display':{'en-US':'"+verbId+"'},'id':'" + xapiUrl + "/definitions/mwb/verb/" +verbId+"'}"));
	// 	JSONObject object = (JSONObject) p
	// 			.parse(new String("{'definition':{'interactionType':'other', 'name':{'en-US':'" + topic
	// 					+ "'}, 'extensions':{'" + xapiUrl + "/definitions/mwb/object/course': {'id': " + course + "}}, 'description':{'en-US':'" + topic
	// 					+ "'}, 'type':'"+ xapiUrl + "/definitions/chat/activities/file'}, 'id':'" + xapiUrl + "/definitions/chat/activities/file/" + fileId + "','objectType':'Activity'}"));
	// 	JSONObject context = (JSONObject) p.parse(new String(
	// 			"{'extensions':{'" + xapiUrl + "/definitions/mwb/extensions/context/activity_data':{'id':'"
	// 					+ fileId + "','topic':'"
	// 					+ topic
	// 					+ "','taskNr':" + taskNr + "}}}"));
	// 					if (fileId2!= null && source != null){
	// 						context = (JSONObject) p.parse(new String(
	// 			"{'extensions':{'"+ xapiUrl + "/definitions/mwb/extensions/context/activity_data':{'graphfileId':'"
	// 					+ fileId + "','feedbackId':'"
	// 					+ fileId2 + "','source':'"
	// 					+ source + "','topic':'"
	// 					+ topic
	// 					+ "','taskNr':" + taskNr + "}}}"));
	// 					}
	// 	JSONObject xAPI = new JSONObject();

	// 	xAPI.put("authority", p.parse(
	// 			new String("{'objectType': 'Agent','name': 'New Client', 'mbox': 'mailto:hello@learninglocker.net'}")));
	// 	xAPI.put("context", context); //
	// 	// xAPI.put("timestamp", java.time.LocalDateTime.now());
	// 	xAPI.put("actor", actor);
	// 	xAPI.put("object", object);
	// 	xAPI.put("verb", verb);
	// 	return xAPI;
	// }

}
