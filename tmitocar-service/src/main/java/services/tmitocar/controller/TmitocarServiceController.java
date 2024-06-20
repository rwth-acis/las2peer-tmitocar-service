package services.tmitocar.controller;

import java.util.Iterator;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.ws.rs.core.UriBuilder;
import java.util.Optional;

// import org.apache.commons.dbcp2.BasicDataSource;
// import org.apache.commons.io.FileUtils;
// // import org.java.websocket.util.Base64;

// import com.fasterxml.jackson.databind.JsonSerializable.Base;
// import com.google.gson.Gson;
// import com.mongodb.ConnectionString;
// import com.mongodb.MongoClientSettings;
// import com.mongodb.MongoException;
// import com.mongodb.client.MongoClient;
// import com.mongodb.client.MongoClients;
// import com.mongodb.client.MongoDatabase;
// import com.mongodb.client.gridfs.GridFSBucket;
// import com.mongodb.client.gridfs.GridFSBuckets;
// import com.mongodb.client.gridfs.model.GridFSFile;
// import com.mongodb.client.model.Filters;

import services.tmitocar.pojo.LrsCredentials;
import services.tmitocar.pojo.TmitocarResponse;
import services.tmitocar.pojo.TmitocarText;
import services.tmitocar.model.TmitocarFiles;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.model.Filters;

import org.springframework.web.bind.annotation.RequestBody;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import netscape.javascript.JSObject;
import services.tmitocar.pojo.LrsCredentials;
import services.tmitocar.pojo.TmitocarResponse;
import services.tmitocar.pojo.TmitocarText;
import services.tmitocar.service.TmitocarService;
import services.tmitocar.model.WritingTask;

import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.bson.BsonObjectId;
import org.bson.types.ObjectId;

import javax.ws.rs.core.MediaType;
// import org.bson.BsonDocument;
// import org.bson.BsonInt64;
// import org.bson.BsonObjectId;
// import org.bson.Document;
// import org.bson.UuidRepresentation;
// import org.bson.codecs.configuration.CodecRegistry;
// import org.bson.codecs.pojo.PojoCodecProvider;
// import org.bson.conversions.Bson;
// import org.bson.types.ObjectId;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.internal.MapPropertiesDelegate;
// import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ResourceConfig;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;


@Tag(name="TmitocarService", description= "A tmitocar wrapper service for analyzing/evaluating texts.")
@RestController
@RequestMapping("/tmitocar")
public class TmitocarServiceController {

	@Autowired
	private TmitocarService service;

 	// @Autowired
    // public TmitocarServiceController(TmitocarService service) {
    //     this.service = service;
    // }

    @Operation(tags = "getAllTasks", summary = "Returns all writing tasks")
	@ApiResponses({ 
			@ApiResponse(responseCode = "200" , description = "Get all writing tasks",content = {@Content(mediaType = "application/json")} ),
			@ApiResponse(responseCode = "500", description = "Response failed.") 
		})
	@GetMapping("/getWritingTasks")
	public ResponseEntity<String> getWritingTasks(@RequestParam(value = "courseId") int courseId) {
		JSONArray jsonArray = new JSONArray();
		JSONArray interactiveElements = new JSONArray();
		String chatMessage = "";			
		try {
			List<WritingTask> tasks = service.findTasksByCourseId(courseId);
			for(int i = 0; i<tasks.size(); i++) {
				if (tasks.get(i).getCourseId() == courseId) {
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("courseId", courseId);
					jsonObject.put("nr", tasks.get(i).getNr());
					jsonObject.put("text", tasks.get(i).getText());
					jsonObject.put("title", tasks.get(i).getTitle());
					jsonArray.add(jsonObject);
					System.out.println(jsonObject.toString());
					jsonObject.put("intent", "nummer "  + tasks.get(i).getNr());
					jsonObject.put("label", "Schreibaufgabe "+ tasks.get(i).getNr());
					jsonObject.put("isFile", false);
					interactiveElements.add(jsonObject);
					chatMessage += tasks.get(i).getNr() + ": "+ tasks.get(i).getTitle() +"\n<br>\n";
				} 
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		JSONObject response = new JSONObject();
		response.put("data", jsonArray);
		response.put("interactiveElements", interactiveElements);
		response.put("chatMessage", chatMessage);

		return ResponseEntity.ok(response.toString());
	}

    @Operation(tags = {"getTasksByNr"}, description = "Returns writing task by id")
	@ApiResponses({ 
		@ApiResponse(responseCode = "200" , description = "",content = {@Content(mediaType = "application/json")} ),
		@ApiResponse(responseCode = "500", description = "Response failed.") 
	})
	@GetMapping("/getWritingTaskByNr")
	public ResponseEntity<String> getWritingTaskByNr(@RequestParam("tasknr") int tasknr, @RequestParam("courseId") int courseId) {
		
		JSONArray jsonArray = new JSONArray();
		String chatMessage = "";
		String title = "";
		int nr = tasknr;
		try {
			WritingTask task = service.findByCourseIdAndNr(courseId, tasknr);

			JSONObject jsonObject = new JSONObject();
			jsonObject.put("courseId", task.getCourseId());
			jsonObject.put("nr", task.getNr());

			jsonObject.put("text", task.getText());
			jsonObject.put("title", task.getTitle());

			jsonArray.add(jsonObject);
			title += task.getTitle();
			chatMessage += task.getText() + "\n<br>";

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		JSONObject response = new JSONObject();
		response.put("data", jsonArray);
		response.put("chatMessage", chatMessage);
		response.put("title", title);
		response.put("nr", nr);
		return ResponseEntity.ok(response.toString());
	}

	/**
	 * Store text
	 *
	 * @param label1          the first label (user text)
	 * @param textInputStream the InputStream containing the text to compare
	 * @param textFileDetail  the file details of the text file
	 * @param type            the type of text (txt, pdf, docx)
	 * @return id of the stored file
	 * @throws ParseException if there is an error parsing the input parameters
	 * @throws IOException    if there is an error reading the input stream
	 */
	@Operation(tags = {"analyzeText"}, description = "Analyzes a text and generates a PDF report")
	@ApiResponses({ 
		@ApiResponse(responseCode = "200" , description = "",content = {@Content(mediaType = "application/json")} ),
		@ApiResponse(responseCode = "500", description = "Response failed.") 
	})
	@PostMapping(value = "/analyzeText", consumes = MediaType.MULTIPART_FORM_DATA, produces = MediaType.APPLICATION_JSON)
	public ResponseEntity<String> analyzeText(@RequestParam("label1") String label1,
			@FormDataParam("file") InputStream textInputStream,
			@FormDataParam("file") FormDataContentDisposition textFileDetail, @FormDataParam("type") String type,
			@FormDataParam("topic") String topic, @FormDataParam("template") String template,
			@FormDataParam("wordSpec") String wordSpec) throws ParseException, IOException {
		if (service.isActive.getOrDefault(label1, false)) {
			JSONObject err = new JSONObject();
			err.put("errorMessage", "User: " + label1 + " currently busy.");
			err.put("error", true);
			return ResponseEntity.badRequest().body(err.toJSONString());
		}
		File templatePath = new File("tmitocar/templates/" + template);
		if (!templatePath.exists()){
			JSONObject err = new JSONObject();
			err.put("errorMessage", "Template: " + template + " not found.");
			err.put("error", true);
			return new ResponseEntity<String>(err.toJSONString(), HttpStatus.NOT_FOUND);
		}
		service.isActive.put(label1, true);
		String encodedByteString = service.convertInputStreamToBase64(textInputStream);
		TmitocarText tmitoBody = new TmitocarText();
		tmitoBody.setTopic(topic);
		tmitoBody.setType(type);
		tmitoBody.setWordSpec(wordSpec);
		tmitoBody.setTemplate(template);
		tmitoBody.setText(encodedByteString);
		service.processSingleText(label1, topic, template, tmitoBody);

		byte[] bytes = Base64.getDecoder().decode(encodedByteString);
		String fname = textFileDetail.getFileName();
		String fileEnding = "txt";
		int dotIndex = fname.lastIndexOf('.');
		if (dotIndex > 0 && dotIndex < fname.length() - 1) {
			fileEnding = fname.substring(dotIndex + 1);
		}
		ObjectId uploaded = service.storeFile(topic+"."+fileEnding, bytes);
		if (uploaded == null) {
			return ResponseEntity.badRequest().body("Could not store file " + fname);
		}
		try {
			textInputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		TmitocarResponse response = new TmitocarResponse(uploaded.toString());
		Gson g = new Gson();
		return ResponseEntity.ok(g.toJson(response));
	}



	// @Api(value = "Analysis Resource")
	// @SwaggerDefinition(info = @Info(title = "Analysis Resource", version = "1.0.0", description = "Todo", termsOfService = "https://tech4comp.de/", contact = @Contact(name = "Alexander Tobias Neumann", url = "https://tech4comp.dbis.rwth-aachen.de/", email = "neumann@dbis.rwth-aachen.de"), license = @License(name = "ACIS License (BSD3)", url = "https://github.com/rwth-acis/las2peer-tmitocar-Service/blob/master/LICENSE")))
	// @Path("/analysis")
	// public static class Analysis {
	// 	// TmitocarServiceController service = (TmitocarServiceController) Context.get().getService();

	/**
	 * Anaylze text
	 */
	@Operation(tags = {"getCommonWords"}, description = "Analyzes a text and generates a PDF report")
	@ApiResponses({ 
		@ApiResponse(responseCode = "200" , description = "",content = {@Content(mediaType = "application/json")} ),
		@ApiResponse(responseCode = "500", description = "Response failed.") 
	})
	@GetMapping(value = "/getCommonWords", produces = MediaType.APPLICATION_JSON)
	public ResponseEntity<String> getCommonWords(@RequestParam("fileId") String fileId) throws ParseException, IOException {
			
			try {
				GridFSBucket gridFSBucket = GridFSBuckets.create(service.getMongoDatabase(), "files");
				gridFSBucket.find(Filters.empty());
				ObjectId oId = new ObjectId(fileId);
				BsonObjectId bId = new BsonObjectId(oId);
				GridFSFile file = gridFSBucket.find(Filters.eq(bId)).first();
				if (file == null) {
					return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File with ID "+fileId+" not found");
				}
				
				// Response.ResponseBuilder response = Response.ok(file.getObjectId().toHexString());
				// response.header("Content-Disposition", "attachment; filename=\"" + file.getFilename() + "\"");
				
				// Download the file to a ByteArrayOutputStream
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				gridFSBucket.downloadToStream(oId,baos);
				String jsonStr = baos.toString();
				JSONParser parser = new JSONParser(JSONParser.MODE_PERMISSIVE);

				// Parse the JSON string into a JSONObject
				JSONObject jsonObject = (JSONObject) parser.parse(jsonStr);
				String formattedMessage = "Danke, besprich das gern auch mit Kommiliton:innen und deinem/r Dozent:in. Wenn ich jetzt deinen Text und den Expertentext vergleiche, dann tauchen in beiden Texten folgende Begriffe als wesentlich auf:\n";
				JSONArray bSchnittmengeArray = (JSONArray) jsonObject.get("BegriffeSchnittmenge");
				formattedMessage += service.formatJSONArray(bSchnittmengeArray);
				formattedMessage += "Wenn du nochmal an die Aufgabenstellung denkst, fehlen Schlüsselbegriffe, die du noch ergänzen würdest? Wenn ja, welche?";
				JSONObject resBody = new JSONObject();
				resBody.put("formattedMessage", service.formatJSONArray(bSchnittmengeArray));
				return ResponseEntity.ok(resBody.toString());
			} catch (Exception e) {
				System.err.println(e);
			} 
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Something went wrong getting common words for analysis: "+fileId);
		}

	// 	/**
	// 	 * Anaylze text
	// 	 */
	// 	@GetMapping()Mapping
	// 	@Path("/{fileId}/label2Words")
	// 	@Produces(MediaType.APPLICATION_JSON)
	// 	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "") })
	// 	@Operation(value = "get", notes = "Analyzes a text and generates a PDF report")
	// 	public Response getLabel2Words(@RequestParam("fileId") String fileId) throws ParseException, IOException {

	// 		CodecRegistry pojoCodecRegistry = fromProviders(PojoCodecProvider.builder().automatic(true).build());
	// 			CodecRegistry codecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), pojoCodecRegistry);
	// 			MongoClientSettings settings = MongoClientSettings.builder()
	// 					.uuidRepresentation(UuidRepresentation.STANDARD)
	// 					.applyConnectionString(new ConnectionString(service.mongoUri))
	// 					.codecRegistry(codecRegistry)
	// 					.build();
				
	// 			// Create a new client and connect to the server
	// 			MongoClient mongoClient = MongoClients.create(settings);
				
	// 			try {
	// 				MongoDatabase database = mongoClient.getDatabase(service.mongoDB);
	// 				GridFSBucket gridFSBucket = GridFSBuckets.create(database, "files");
	// 				gridFSBucket.find(Filters.empty());
	// 				ObjectId oId = new ObjectId(fileId);
	// 				BsonObjectId bId = new BsonObjectId(oId);
	// 				GridFSFile file = gridFSBucket.find(Filters.eq(bId)).first();
	// 				if (file == null) {
	// 					return Response.status(Response.Status.NOT_FOUND).entity("File with ID "+fileId+" not found").build();
	// 				}
	// 				Response.ResponseBuilder response = Response.ok(file.getObjectId().toHexString());
	// 				response.header("Content-Disposition", "attachment; filename=\"" + file.getFilename() + "\"");
					
	// 				// Download the file to a ByteArrayOutputStream
	// 				ByteArrayOutputStream baos = new ByteArrayOutputStream();
	// 				gridFSBucket.downloadToStream(file.getObjectId(), baos);
	// 				String jsonStr = baos.toString();
	// 				JSONParser parser = new JSONParser(JSONParser.MODE_PERMISSIVE);

	// 				// Parse the JSON string into a JSONObject
	// 				JSONObject jsonObject = (JSONObject) parser.parse(jsonStr);
	// 				String formattedMessage = "Übrigens gibt es noch folgende Begriffe, die im Expertentext genannt wurden, aber noch nicht in deinem Text auftauchen::\n";
	// 				formattedMessage += "-------------------------\n";
	// 				JSONArray bDiffArray = (JSONArray) jsonObject.get("BegriffeDiffB");
	// 				formattedMessage += formatJSONArray(bDiffArray);
	// 				formattedMessage += "Überleg nochmal, welche davon du sinnvoll in deinen Text einbauen kannst und möchtest.";
	// 				JSONObject resBody = new JSONObject();
	// 				resBody.put("formattedMessage",formatJSONArray(bDiffArray));
	// 				return Response.ok(resBody.toString()).build();
	// 			} catch (MongoException me) {
	// 				System.err.println(me);
	// 			} finally {
	// 				// Close the MongoDB client
	// 				mongoClient.close();
	// 			}
	// 			return Response.status(Response.Status.BAD_REQUEST).entity("Something went wrong getting label2 words for analysis: "+fileId).build();
	// 		}
			
	// 	}


	// @Tag(name = "Feedback Resource")
	// // @SwaggerDefinition(info = @Info(title = "Feedback Resource", version = "1.0.0", description = "This API is responsible for handling text documents in txt, pdf or docx format and sending them to T-MITOCAR for processing. The feedback is then saved in a MongoDB and the document IDs are returned.", termsOfService = "https://tech4comp.de/", contact = @Contact(name = "Alexander Tobias Neumann", url = "https://tech4comp.dbis.rwth-aachen.de/", email = "neumann@dbis.rwth-aachen.de"), license = @License(name = "ACIS License (BSD3)", url = "https://github.com/rwth-acis/las2peer-tmitocar-Service/blob/master/LICENSE")))
	// @RequestMapping("/feedback")
	// public static class Feedback {
	// 	// TmitocarServiceController service = (TmitocarServiceController) Context.get().getService();

	// 	/**
	// 	 * Anaylze text
	// 	 *
	// 	 * @param label1          the first label (user text)
	// 	 * @param textInputStream the InputStream containing the text to compare
	// 	 * @param textFileDetail  the file details of the text file
	// 	 * @param type            the type of text (txt, pdf or docx)
	// 	 * @param topic           the topic of the text (e.g. BiWi 5)
	// 	 * @param template        the template to use for the PDF report
	// 	 * @param wordSpec        the word specification for the PDF report
	// 	 * @return the id of the stored file
	// 	 * @throws ParseException if there is an error parsing the input parameters
	// 	 * @throws IOException    if there is an error reading the input stream
	// 	 */
	// 	@POST
	// 	@Path("/{label1}")
	// 	@Consumes(MediaType.MULTIPART_FORM_DATA)
	// 	@Produces(MediaType.APPLICATION_JSON)
	// 	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "") })
	// 	@Operation(value = "analyzeText", notes = "Analyzes a text and generates a PDF report")
	// 	public Response analyzeText(@RequestParam("label1") String label1,
	// 			@FormDataParam("file") InputStream textInputStream,
	// 			@FormDataParam("file") FormDataContentDisposition textFileDetail, @FormDataParam("type") String type,
	// 			@FormDataParam("topic") String topic, @FormDataParam("template") String template,
	// 			@FormDataParam("wordSpec") String wordSpec,@FormDataParam("email") String email,@FormDataParam("courseId") int courseId,@FormDataParam("task") int task, @FormDataParam("sbfmURL") String sbfmURL) throws ParseException, IOException {
	// 		if (service.isActive.getOrDefault(label1, false)) {
	// 			JSONObject err = new JSONObject();
	// 			err.put("errorMessage", "User: " + label1 + " currently busy.");
	// 			err.put("error", true);
	// 			return Response.status(Status.NOT_FOUND).entity(err.toJSONString()).build();
	// 		}

			
	// 		File templatePath = new File("tmitocar/templates/" + template);
	// 		if (!templatePath.exists()){
	// 			JSONObject err = new JSONObject();
	// 			err.put("errorMessage", "Template: " + template + " not found.");
	// 			err.put("error", true);
	// 			return Response.status(Status.NOT_FOUND).entity(err.toJSONString()).build();
	// 		}
	// 		service.isActive.put(label1, true);
	// 		String encodedByteString = convertInputStreamToBase64(textInputStream);
	// 		TmitocarText tmitoBody = new TmitocarText();
	// 		tmitoBody.setTopic(topic);
	// 		tmitoBody.setType(type);
	// 		tmitoBody.setWordSpec(wordSpec);
	// 		tmitoBody.setTemplate(template);
	// 		tmitoBody.setText(encodedByteString);
	// 		service.processSingleText(label1, topic, template, tmitoBody);

	// 		byte[] bytes = Base64.decode(encodedByteString);
	// 		String fname = textFileDetail.getFileName();

	// 		String fileEnding = "txt";
	// 		int dotIndex = fname.lastIndexOf('.');
	// 		if (dotIndex > 0 && dotIndex < fname.length() - 1) {
	// 			fileEnding = fname.substring(dotIndex + 1);
	// 		}

	// 		ObjectId uploaded = service.storeFile(topic+"."+fileEnding, bytes);
	// 		if (uploaded == null) {
	// 			return Response.status(Status.BAD_REQUEST).entity("Could not store file " + fname).build();
	// 		}
	// 		try {
	// 			textInputStream.close();
	// 		} catch (IOException e) {
	// 			e.printStackTrace();
	// 		}
	// 		TmitocarResponse response = new TmitocarResponse(uploaded.toString());
	// 		String uuid = service.getUuidByEmail(email);
	// 		if (uuid!=null){
	// 			// user has accepted
	// 			LrsCredentials lrsCredentials = service.getLrsCredentialsByCourse(courseId);
	// 			if(lrsCredentials!=null){
	// 				JSONObject xapi = service.prepareXapiStatement(uuid, "uploaded_task", topic, courseId, task, uploaded.toString(),null,null);
	// 				String toEncode = lrsCredentials.getClientKey()+":"+lrsCredentials.getClientSecret();
	// 				String encodedString = Base64.encodeBytes(toEncode.getBytes());

	// 				service.sendXAPIStatement(xapi, encodedString);
	// 			}
	// 		}
	// 		Gson g = new Gson();
	// 		return Response.ok().entity(g.toJson(response)).build();
	// 	}

	// 	@GetMapping()
	// 	@Path("/{label1}")
	// 	@Produces(MediaType.APPLICATION_JSON)
	// 	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "") })
	// 	@Operation(value = "getAnalyzedText", notes = "Returns analyzed text report (PDF)")
	// 	public Response getAnalyzedText(@RequestParam("label1") String label1) throws ParseException, IOException {
	// 		if (!service.isActive.containsKey(label1)) {
	// 			JSONObject err = new JSONObject();
	// 			err.put("errorMessage", "User: " + label1 + " not found.");
	// 			err.put("error", true);
	// 			return Response.status(Status.NOT_FOUND).entity(err.toJSONString()).build();
	// 		}
	// 		if (service.isActive.get(label1)) {
	// 			JSONObject err = new JSONObject();
	// 			err.put("errorMessage", "User: " + label1 + " currently busy.");
	// 			err.put("error", true);
	// 			return Response.status(Status.NOT_FOUND).entity(err.toJSONString()).build();
	// 		}
	// 		JSONObject err = new JSONObject();
	// 		ObjectId fileId = null;
	// 		if (userTexts.get(label1) != null) {
	// 			System.out.println("Storing PDF to mongodb...");
	// 			try {
	// 				byte[] pdfByte = Files.readAllBytes(
	// 						Paths.get("tmitocar/texts/" + label1 + "/" + label1 + "-modell" + ".pdf"));
	// 				fileId = service.storeFile(label1 + "-feedback.pdf", pdfByte);
	// 				Files.delete(Paths.get("tmitocar/texts/" + label1 + "/" + label1 + "-modell" + ".pdf"));
	// 			} catch (Exception e) {
	// 				e.printStackTrace();
	// 				service.isActive.put(label1, false);
	// 				System.out.println("Failed storing PDF.");
	// 			}
	// 		} else {
	// 			err.put("errorMessage", "Something went wrong storing the feedback for " + label1);
	// 			err.put("error", true);
	// 			return Response.status(Status.BAD_REQUEST).entity(err.toJSONString()).build();
	// 		}

	// 		if (fileId == null) {
	// 			err.put("errorMessage", "Something went wrong storing the feedback for " + label1);
	// 			err.put("error", true);
	// 			return Response.status(Status.BAD_REQUEST).entity(err.toJSONString()).build();
	// 		}
	// 		TmitocarResponse response = new TmitocarResponse(null, fileId.toString());
	// 		Gson g = new Gson();
	// 		return Response.ok().entity(g.toJson(response)).build();
	// 	}

		/**
		 * Compare text
		 *
		 * @param label1          the first label (user text)
		 * @param label2          the second label (expert or second user text)
		 * @param textInputStream the InputStream containing the text to compare
		 * @param textFileDetail  the file details of the text file
		 * @param type            the type of text (txt, pdf or docx)
		 * @param template        the template to use for the PDF report
		 * @param wordSpec        the word specification for the PDF report
		 * @return the id of the stored file
		 * @throws ParseException if there is an error parsing the input parameters
		 * @throws IOException    if there is an error reading the input stream
		 */
		@Operation(tags = {"compareText"}, description = "Compares two texts and generates a PDF report")
		@ApiResponses({ 
			@ApiResponse(responseCode = "200" , description = "",content = {@Content(mediaType = "application/json")} ),
			@ApiResponse(responseCode = "500", description = "Response failed.") 
		})
		@PostMapping(value = "/compareText" , consumes = MediaType.MULTIPART_FORM_DATA, produces = MediaType.APPLICATION_JSON)
		public ResponseEntity<String> compareText(@RequestParam("label1") String label1, @RequestParam("label2") String label2,
				@FormDataParam("file") InputStream textInputStream,
				@FormDataParam("file") FormDataContentDisposition textFileDetail, @FormDataParam("type") String type, @FormDataParam("template") String template,
				@FormDataParam("wordSpec") String wordSpec,@FormDataParam("email") String email,@FormDataParam("courseId") int courseId, @FormDataParam("sbfmURL") String sbfmURL) throws ParseException, IOException {
			if (service.isActive.getOrDefault(label1, false)) {
				JSONObject err = new JSONObject();
				err.put("errorMessage", "User: " + label1 + " currently busy.");
				err.put("error", true);
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err.toJSONString());
			}
			
			File templatePath = new File("tmitocar/templates/" + template);
			if (!templatePath.exists()){
				JSONObject err = new JSONObject();
				err.put("errorMessage", "Template: " + template + " not found.");
				err.put("error", true);
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err.toJSONString());
			}
			
			service.isActive.put(label1, true);

			String topic = service.getTaskNameByIds(courseId, Integer.parseInt(label2));


			String encodedByteString = service.convertInputStreamToBase64(textInputStream);
			TmitocarText tmitoBody = new TmitocarText();
			tmitoBody.setTopic(topic);
			tmitoBody.setType(type);
			tmitoBody.setWordSpec(wordSpec);
			tmitoBody.setTemplate(template);
			tmitoBody.setText(encodedByteString);
			tmitoBody.setUuid(email);
			byte[] bytes = Base64.getDecoder().decode(encodedByteString);
			String fname = textFileDetail.getFileName();
			String fileEnding = "txt";
			int dotIndex = fname.lastIndexOf('.');
			if (dotIndex > 0 && dotIndex < fname.length() - 1) {
				fileEnding = fname.substring(dotIndex + 1);
			}
			ObjectId uploaded = service.storeFile(topic +  "." + fileEnding, bytes);
			if (uploaded == null) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Could not store file " + fname);
			}
			try {
				textInputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			boolean comparing = service.compareText(label1, courseId + "-"+ label2, template, tmitoBody, sbfmURL,uploaded.toString());

			if (!comparing) {
				service.isActive.put(label1, false);
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Something went wrong: " + label1 + ".");
			}

			
			TmitocarResponse response = new TmitocarResponse(uploaded.toString());
			response.setLabel1(label1);
			response.setLabel2(courseId + "-"+ label2);
			// String uuid = service.getUuidByEmail(email);
			// if (uuid!=null){
			// 	// user has accepted
			// 	LrsCredentials lrsCredentials = service.getLrsCredentialsByCourse(courseId);
			// 	if(lrsCredentials!=null){
			// 		JSONObject xapi = service.prepareXapiStatement(uuid, "uploaded_task", topic, courseId, Integer.parseInt(label2), uploaded.toString(),null,null);
			// 		String toEncode = lrsCredentials.getClientKey()+":"+lrsCredentials.getClientSecret();
			// 		String encodedString = Base64.encodeBytes(toEncode.getBytes());
			// 		service.sendXAPIStatement(xapi, encodedString);
			// 	}
			// }
			Gson g = new Gson();
			return ResponseEntity.ok(g.toJson(response));
		}
	}

	// 	@GetMapping()
	// 	@Path("/{label1}/compare/{label2}")
	// 	@Consumes(MediaType.MULTIPART_FORM_DATA)
	// 	@Produces(MediaType.APPLICATION_JSON)
	// 	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "") })
	// 	@Operation(value = "getComparedText", notes = "Returns compared text report (PDF)")
	// 	public Response getComparedText(@RequestParam("label1") String label1, @RequestParam("label2") String label2)
	// 			throws ParseException, IOException {
	// 		if (!service.isActive.containsKey(label1)) {
	// 			JSONObject err = new JSONObject();
	// 			err.put("errorMessage", "User: " + label1 + " not found.");
	// 			err.put("error", true);
	// 			return Response.status(Status.NOT_FOUND).entity(err.toJSONString()).build();
	// 		}
	// 		if (service.isActive.get(label1)) {
	// 			JSONObject err = new JSONObject();
	// 			err.put("errorMessage", "User: " + label1 + " currently busy.");
	// 			err.put("error", true);
	// 			return Response.status(Status.NOT_FOUND).entity(err.toJSONString()).build();
	// 		}
	// 		JSONObject err = new JSONObject();
	// 		ObjectId feedbackFileId = null;
	// 		ObjectId graphFileId = null;
	// 		if (userTexts.get(label1) != null) {
	// 			System.out.println("Storing PDF to mongodb...");
	// 			feedbackFileId = service.storeLocalFileRemote("comparison_" + label1 + "_vs_" + label2 + ".pdf");
	// 			graphFileId = service.storeLocalFileRemote("comparison_" + label1 + "_vs_" + label2 + ".json");

	// 		} else {
	// 			err.put("errorMessage", "Something went wrong storing the feedback for " + label1);
	// 			err.put("error", true);
	// 			return Response.status(Status.BAD_REQUEST).entity(err.toJSONString()).build();
	// 		}

	// 		if (feedbackFileId == null) {
	// 			err.put("errorMessage", "Something went wrong storing the feedback for " + label1);
	// 			err.put("error", true);
	// 			return Response.status(Status.BAD_REQUEST).entity(err.toJSONString()).build();
	// 		}
	// 		if (graphFileId == null) {
	// 			err.put("errorMessage", "Something went wrong storing the graph for " + label1);
	// 			err.put("error", true);
	// 			return Response.status(Status.BAD_REQUEST).entity(err.toJSONString()).build();
	// 		}
	// 		TmitocarResponse response = new TmitocarResponse(null, feedbackFileId.toString(), graphFileId.toString());
	// 		Gson g = new Gson();
	// 		return Response.ok().entity(g.toJson(response)).build();
	// 	}

	// 	/**
	// 	 * Compare text
	// 	 *
	// 	 * @param label1          the first label (user text)
	// 	 * @param label2          the second label (expert or second user text)
	// 	 * @param textInputStream the InputStream containing the text to compare
	// 	 * @param textFileDetail  the file details of the text file
	// 	 * @param type            the type of text (txt, pdf or docx)
	// 	 * @param template        the template to use for the PDF report
	// 	 * @param wordSpec        the word specification for the PDF report
	// 	 * @return the id of the stored file
	// 	 * @throws ParseException if there is an error parsing the input parameters
	// 	 * @throws IOException    if there is an error reading the input stream
	// 	 */
	// 	@POST
	// 	@Path("/{label1}/compareWithLLM/{label2}")
	// 	@Consumes(MediaType.MULTIPART_FORM_DATA)
	// 	@Produces(MediaType.APPLICATION_JSON)
	// 	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "") })
	// 	@Operation(value = "compareTextWithLLM", notes = "Compares two texts and generates a PDF report")
	// 	public Response compareTextWithLLM(@RequestParam("label1") String label1, @RequestParam("label2") String label2,
	// 			@FormDataParam("file") InputStream textInputStream,
	// 			@FormDataParam("file") FormDataContentDisposition textFileDetail, @FormDataParam("type") String type, @FormDataParam("template") String template,
	// 			@FormDataParam("wordSpec") String wordSpec,@FormDataParam("email") String email,@FormDataParam("courseId") int courseId, @FormDataParam("sbfmURL") String sbfmURL) throws ParseException, IOException {
	// 		if (service.isActive.getOrDefault(label1, false)) {
	// 			JSONObject err = new JSONObject();
	// 			err.put("errorMessage", "User: " + label1 + " currently busy.");
	// 			err.put("error", true);
	// 			return Response.status(Status.NOT_FOUND).entity(err.toJSONString()).build();
	// 		}
			
	// 		File templatePath = new File("tmitocar/templates/" + template);
	// 		if (!templatePath.exists()){
	// 			JSONObject err = new JSONObject();
	// 			err.put("errorMessage", "Template: " + template + " not found.");
	// 			err.put("error", true);
	// 			return Response.status(Status.NOT_FOUND).entity(err.toJSONString()).build();
	// 		}
			
	// 		service.isActive.put(label1, true);

	// 		String topic = service.getTaskNameByIds(courseId, Integer.parseInt(label2));


	// 		String encodedByteString = convertInputStreamToBase64(textInputStream);
	// 		TmitocarText tmitoBody = new TmitocarText();
	// 		tmitoBody.setTopic(topic);
	// 		tmitoBody.setType(type);
	// 		tmitoBody.setWordSpec(wordSpec);
	// 		tmitoBody.setTemplate(template);
	// 		tmitoBody.setText(encodedByteString);
	// 		tmitoBody.setUuid(email);
	// 		byte[] bytes = Base64.decode(encodedByteString);
	// 		String fname = textFileDetail.getFileName();
	// 		String fileEnding = "txt";
	// 		int dotIndex = fname.lastIndexOf('.');
	// 		if (dotIndex > 0 && dotIndex < fname.length() - 1) {
	// 			fileEnding = fname.substring(dotIndex + 1);
	// 		}
	// 		ObjectId uploaded = service.storeFile(topic +  "." + fileEnding, bytes);
	// 		if (uploaded == null) {
	// 			return Response.status(Status.BAD_REQUEST).entity("Could not store file " + fname).build();
	// 		}
	// 		try {
	// 			textInputStream.close();
	// 		} catch (IOException e) {
	// 			e.printStackTrace();
	// 		}
	// 		boolean comparing = service.llm_feedback(label1, courseId + "-"+ label2, template, tmitoBody, sbfmURL,uploaded.toString());

	// 		if (!comparing) {
	// 			service.isActive.put(label1, false);
	// 			return Response.status(Status.BAD_REQUEST).entity("Something went wrong: " + label1 + ".").build();
	// 		}

			
	// 		TmitocarResponse response = new TmitocarResponse(uploaded.toString());
	// 		response.setLabel1(label1);
	// 		response.setLabel2(courseId + "-"+ label2);
	// 		String uuid = service.getUuidByEmail(email);
	// 		if (uuid!=null){
	// 			// user has accepted
	// 			LrsCredentials lrsCredentials = service.getLrsCredentialsByCourse(courseId);
	// 			if(lrsCredentials!=null){
	// 				JSONObject xapi = service.prepareXapiStatement(uuid, "uploaded_task", topic, courseId, Integer.parseInt(label2), uploaded.toString(),null,null);
	// 				String toEncode = lrsCredentials.getClientKey()+":"+lrsCredentials.getClientSecret();
	// 				String encodedString = Base64.encodeBytes(toEncode.getBytes());
	// 				service.sendXAPIStatement(xapi, encodedString);
	// 			}
	// 		}
	// 		Gson g = new Gson();
	// 		return Response.ok().entity(g.toJson(response)).build();
	// 	}

	// 	@GetMapping()
	// 	@Path("/{label1}/compareWithLLM/{label2}")
	// 	@Consumes(MediaType.MULTIPART_FORM_DATA)
	// 	@Produces(MediaType.APPLICATION_JSON)
	// 	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "") })
	// 	@Operation(value = "getComparedTextWithLLM", notes = "Returns compared text report (PDF)")
	// 	public Response getComparedTextWithLLM(@RequestParam("label1") String label1, @RequestParam("label2") String label2)
	// 			throws ParseException, IOException {
	// 		if (!service.isActive.containsKey(label1)) {
	// 			JSONObject err = new JSONObject();
	// 			err.put("errorMessage", "User: " + label1 + " not found.");
	// 			err.put("error", true);
	// 			return Response.status(Status.NOT_FOUND).entity(err.toJSONString()).build();
	// 		}
	// 		if (service.isActive.get(label1)) {
	// 			JSONObject err = new JSONObject();
	// 			err.put("errorMessage", "User: " + label1 + " currently busy.");
	// 			err.put("error", true);
	// 			return Response.status(Status.NOT_FOUND).entity(err.toJSONString()).build();
	// 		}
	// 		JSONObject err = new JSONObject();
	// 		ObjectId feedbackFileId = null;
	// 		ObjectId graphFileId = null;
	// 		if (userTexts.get(label1) != null) {
	// 			System.out.println("Storing PDF to mongodb...");
	// 			feedbackFileId = service.storeLocalFileRemote("comparison_" + label1 + "_vs_" + label2 + ".pdf");
	// 			graphFileId = service.storeLocalFileRemote("comparison_" + label1 + "_vs_" + label2 + ".json");

	// 		} else {
	// 			err.put("errorMessage", "Something went wrong storing the feedback for " + label1);
	// 			err.put("error", true);
	// 			return Response.status(Status.BAD_REQUEST).entity(err.toJSONString()).build();
	// 		}

	// 		if (feedbackFileId == null) {
	// 			err.put("errorMessage", "Something went wrong storing the feedback for " + label1);
	// 			err.put("error", true);
	// 			return Response.status(Status.BAD_REQUEST).entity(err.toJSONString()).build();
	// 		}
	// 		if (graphFileId == null) {
	// 			err.put("errorMessage", "Something went wrong storing the graph for " + label1);
	// 			err.put("error", true);
	// 			return Response.status(Status.BAD_REQUEST).entity(err.toJSONString()).build();
	// 		}
	// 		TmitocarResponse response = new TmitocarResponse(null, feedbackFileId.toString(), graphFileId.toString());
	// 		Gson g = new Gson();
	// 		return Response.ok().entity(g.toJson(response)).build();
	// 	}
	// }

	// @Api(value = "FAQ Resource")
	// @SwaggerDefinition(info = @Info(title = "FAQ Resource", version = "1.0.0", description = "Todo.", termsOfService = "https://tech4comp.de/", contact = @Contact(name = "Alexander Tobias Neumann", url = "https://tech4comp.dbis.rwth-aachen.de/", email = "neumann@dbis.rwth-aachen.de"), license = @License(name = "ACIS License (BSD3)", url = "https://github.com/rwth-acis/las2peer-tmitocar-Service/blob/master/LICENSE")))
	// @Path("/faq")
	// public static class FAQ {
	// 	// TmitocarServiceController service = (TmitocarServiceController) Context.get().getService();
		
	// 	@GetMapping()
	// 	@Path("/")
	// 	@Produces(MediaType.APPLICATION_JSON)
	// 	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "") })
	// 	@Operation(value = "getAllQuestions", notes = "Returns all questions")
	// 	public Response getFAQ(@QueryParam("courseId") int courseId) {
			
	// 		Connection conn = null;
	// 		PreparedStatement stmt = null;
	// 		ResultSet rs = null;
	// 		JSONArray jsonArray = new JSONArray();
	// 		JSONArray interactiveElements = new JSONArray();
	// 		String chatMessage = "";
	// 		try {
	// 			conn = service.getConnection();
	// 			if (courseId == 0) {
	// 				stmt = conn.prepareStatement("SELECT * FROM coursefaq;");
	// 			} else {
	// 				stmt = conn.prepareStatement("SELECT * FROM coursefaq WHERE courseid = ?;");
	// 				stmt.setInt(1, courseId);
	// 			}
	// 			rs = stmt.executeQuery();

	// 			while (rs.next()) {
	// 				courseId = rs.getInt("courseid");
	// 				String text = rs.getString("answer");
	// 				String intent = rs.getString("intent");

	// 				JSONObject jsonObject = new JSONObject();
	// 				jsonObject.put("courseId", courseId);
	// 				jsonObject.put("text", text);
	// 				jsonObject.put("intent", intent);

	// 				jsonArray.add(jsonObject);
	// 				jsonObject = new JSONObject();
	// 				jsonObject.put("intent", intent);
	// 				jsonObject.put("label", "Question "+ intent);
	// 				jsonObject.put("isFile", false);

	// 				interactiveElements.add(jsonObject);
	// 				chatMessage += intent+": "+text+"\n<br>\n";
	// 			}
	// 		} catch (SQLException e) {
	// 			// TODO Auto-generated catch block
	// 			e.printStackTrace();
	// 		} finally {
	// 			try {
	// 				if (rs != null) {
	// 					rs.close();
	// 				}
	// 				if (stmt != null) {
	// 					stmt.close();
	// 				}
	// 				if (conn != null) {
	// 					conn.close();
	// 				}
	// 			} catch (SQLException ex) {
	// 				System.out.println(ex.getMessage());
	// 			}
	// 		}
	// 		JSONObject response = new JSONObject();
	// 		response.put("data", jsonArray);
	// 		response.put("interactiveElements", interactiveElements);
	// 		response.put("chatMessage", chatMessage);
	// 		return Response.ok().entity(response.toString()).build();
	// 	}


	// 	@GetMapping()
	// 	@Path("/{intent}")
	// 	@Produces(MediaType.APPLICATION_JSON)
	// 	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "") })
	// 	@Operation(value = "getAllTasks", notes = "Returns writing task by id")
	// 	public Response getFAQByIntent(@RequestParam("intent") String intent, @QueryParam("courseId") int courseId) {
			
	// 		Connection conn = null;
	// 		PreparedStatement stmt = null;
	// 		ResultSet rs = null;
	// 		JSONArray jsonArray = new JSONArray();
	// 		String chatMessage = "";
			
	// 		try {
	// 			conn = service.getConnection();
	// 			if (courseId == 0) {
	// 				stmt = conn.prepareStatement("SELECT * FROM coursefaq WHERE intent = ?");
	// 				stmt.setString(1, intent);
	// 			} else {
	// 				stmt = conn.prepareStatement("SELECT * FROM coursefaq WHERE intent = ? AND courseid = ?");
	// 				stmt.setString(1, intent);
	// 				stmt.setInt(2, courseId);
	// 			}
	// 			System.out.println("parameters are: " + intent + courseId);
	// 			System.out.println(stmt.toString());
	// 			rs = stmt.executeQuery();

	// 			while (rs.next()) {
	// 				String text = rs.getString("answer");
	// 				//String title = rs.getString("title");

	// 				chatMessage += text + "\n<br>";
	// 			}
	// 		} catch (SQLException e) {
	// 			// TODO Auto-generated catch block
	// 			e.printStackTrace();
	// 		} finally {
	// 			try {
	// 				if (rs != null) {
	// 					rs.close();
	// 				}
	// 				if (stmt != null) {
	// 					stmt.close();
	// 				}
	// 				if (conn != null) {
	// 					conn.close();
	// 				}
	// 			} catch (SQLException ex) {
	// 				System.out.println(ex.getMessage());
	// 			}
	// 		}
	// 		if (chatMessage == "") {
	// 			stmt = null;
	// 			conn = null;
	// 			rs = null;
	// 			try{
	// 				conn = service.getConnection();
	// 				if (courseId == 0) {
	// 					stmt = conn.prepareStatement("SELECT * FROM courseFAQ WHERE intent = 'default'");
	// 				} else {
	// 					stmt = conn.prepareStatement("SELECT * FROM courseFAQ WHERE intent = 'default' AND courseid = ?");
	// 					stmt.setInt(1, courseId);
	// 				}
	// 				rs = stmt.executeQuery();

	// 				while (rs.next()) {
	// 					String text = rs.getString("answer");
	// 					//String title = rs.getString("title");
	
	// 					chatMessage += text + "\n<br>";
	// 				}
	// 			} catch (SQLException e) {
	// 				// TODO Auto-generated catch block
	// 				e.printStackTrace();
	// 			} finally {
	// 				try {
	// 					if (rs != null) {
	// 						rs.close();
	// 					}
	// 					if (stmt != null) {
	// 						stmt.close();
	// 					}
	// 					if (conn != null) {
	// 						conn.close();
	// 					}
	// 				} catch (SQLException ex) {
	// 					System.out.println(ex.getMessage());
	// 				}
	// 			}

	// 		}
	// 		JSONObject response = new JSONObject();
	// 		response.put("chatMessage", chatMessage);
	// 		return Response.ok().entity(response.toString()).build();
	// 	}

	// }

	// @Api(value = "Credits Resource")
	// @SwaggerDefinition(info = @Info(title = "Credits Resource", version = "1.0.0", description = "Todo.", termsOfService = "https://tech4comp.de/", contact = @Contact(name = "Alexander Tobias Neumann", url = "https://tech4comp.dbis.rwth-aachen.de/", email = "neumann@dbis.rwth-aachen.de"), license = @License(name = "ACIS License (BSD3)", url = "https://github.com/rwth-acis/las2peer-tmitocar-Service/blob/master/LICENSE")))
	// @Path("/credits")
	// public static class Credits {
	// 	// TmitocarServiceController service = (TmitocarServiceController) Context.get().getService();

	// 	@GetMapping()
	// 	@Path("/")
	// 	@Produces(MediaType.APPLICATION_JSON)
	// 	@ApiResponses(value = { @ApiResponse(code = HttpURLConnection.HTTP_OK, message = "") })
	// 	@Operation(value = "getAllTasks", notes = "Returns writing task by id")
	// 	public Response getCreditsByUser(@QueryParam("email") String email, @QueryParam("courseId") int courseId) {
	// 		String user = service.getUuidByEmail(email);
	// 		JSONObject jsonBody = new JSONObject();
	// 		JSONParser p = new JSONParser(JSONParser.MODE_PERMISSIVE);
	// 		if(courseId != 6 && courseId != 2 && courseId != 11){
	// 			JSONObject error = new JSONObject();
	// 			error.put("chateMessage","Keine Credits für deinen Kurs :).");
	// 			return Response.ok().entity(error.toString()).build();
	// 		}
	// 		try{
	// 			JSONObject acc = (JSONObject) p.parse(new String("{'account': { 'name': '" + user
	// 				+ "', 'homePage': '"+ service.xapiHomepage + "'}}"));
				
	// 			LrsCredentials res = service.getLrsCredentialsByCourse(courseId);
	// 			URL url = new URL(service.lrsURL + "/data/xAPI/statements?agent=" + acc.toString());
	// 			if(res==null){
	// 				return Response.ok().entity("problem").build();
	// 			}
	// 			String toEncode = res.getClientKey()+":"+res.getClientSecret();
	// 			String encodedString = Base64.encodeBytes(toEncode.getBytes());
	// 			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	// 			conn.setRequestMethod("GET");
	// 			conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
	// 			conn.setRequestProperty("X-Experience-API-Version", "1.0.3");
	// 			conn.setRequestProperty("Authorization", "Basic " + encodedString);
	// 			conn.setRequestProperty("Cache-Control", "no-cache");
	// 			conn.setUseCaches(false);
	// 			BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	// 			String inputLine;
	// 			StringBuffer response = new StringBuffer();
	// 			while ((inputLine = in.readLine()) != null) {
	// 				response.append(inputLine);
	// 			}
	// 			in.close();
	// 			conn.disconnect();
			
	// 			jsonBody = (JSONObject) p.parse(response.toString());
	// 			JSONArray statements = (JSONArray) jsonBody.get("statements");
	// 			// Check statements with matching actor
	// 			// 12 Values as 12 assignments right?
	// 			int[] assignments = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
	// 			for (Object index : statements) {
	// 				JSONObject jsonIndex = (JSONObject) index;
	// 				JSONObject actor = (JSONObject) jsonIndex.get("actor");
	// 				JSONObject verb = (JSONObject) jsonIndex.get("verb");
	// 				JSONObject account = (JSONObject) actor.get("account");
	// 				if (account.get("name").toString().equals(user)
	// 						|| account.get("name").toString().equals(user)) {
	// 						System.out.println("name check passed");
	// 					// JSONObject object = (JSONObject) jsonIndex.get("object");
	// 					JSONObject context = (JSONObject) jsonIndex.get("context");
	// 					// JSONObject definition = (JSONObject) object.get("definition");
	// 					JSONObject extensions = (JSONObject) context.get("extensions");// assignmentNumber
	// 					// check if its not a delete statement
	// 					if (extensions.get(service.xapiUrl + "/definitions/mwb/extensions/context/activity_data") != null && verb.get("id").toString().contains("sent")) {
	// 						JSONObject fileDetails = (JSONObject) extensions
	// 								.get(service.xapiUrl + "/definitions/mwb/extensions/context/activity_data");
	// 						if (fileDetails.get("taskNr") != null) {
	// 							String assignmentName = fileDetails.get("taskNr").toString();
	// 							// JSONObject name = (JSONObject) definition.get("name");
	// 							// String assignmentName = name.getAsString("en-US");
	// 							try {
	// 								// int assignmentNumber = Integer.valueOf(assignmentName.split("t")[1]);
	// 								int assignmentNumber = Integer.valueOf(assignmentName);
	// 								assignments[assignmentNumber - 1]++;
	// 							} catch (Exception e) {
	// 								e.printStackTrace();
	// 							}
	// 							// System.out.println("Extracted actor is " + name.getAsString("en-US"));
	// 						}
	// 					}
	// 				}
	// 			}
	// 				String msg = "";
	// 				int credits = 0;
	// 				for (int i = 0; i < 12; i++) {
	// 					String number;
	// 					if (i < 9) {
	// 						number = "0" + String.valueOf(i + 1);
	// 					} else {
	// 						number = String.valueOf(i + 1);
	// 					}
	// 					if (assignments[i] > 0) {
	// 						credits++;
	// 					}
	// 					System.out.println("20");
	// 					msg += "Schreibaufgabe " + number + ": " + String.valueOf(assignments[i]) + "<br>";
	// 				}
	// 				// How are the credits calculated?
	// 				msg += "Das hei\u00DFt, du hast bisher *" + credits * 2 + "* Leistunsprozente gesammelt. ";
	// 				jsonBody = new JSONObject();
	// 				jsonBody.put("text", msg);
	// 		} catch (Exception e){
	// 			e.printStackTrace();
	// 		}		
	// 		return Response.ok().entity(jsonBody.toString()).build();
	// 	}
	// }



