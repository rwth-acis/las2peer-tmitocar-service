package services.tmitocar.controller;


import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import java.util.Base64;
import java.util.List;

import java.nio.file.Files;
import java.nio.file.Paths;

import services.tmitocar.pojo.LrsCredentials;
import services.tmitocar.pojo.TmitocarResponse;
import services.tmitocar.pojo.TmitocarText;
import services.tmitocar.model.CourseFAQ;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.google.gson.Gson;

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.model.Filters;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

import services.tmitocar.service.TmitocarService;
import services.tmitocar.model.WritingTask;

import org.bson.BsonObjectId;
import org.bson.types.ObjectId;

import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.media.multipart.FormDataParam;


@Tag(name="TmitocarService", description= "A tmitocar wrapper service for analyzing/evaluating texts.")
@RestController
@RequestMapping("/")
public class TmitocarServiceController {
	@Autowired
	private TmitocarService service;

	@GetMapping("/swagger.json")
	public ResponseEntity<JSONObject> getSwagger() {
		JSONObject swaggerJson = service.getSwagger();
		return ResponseEntity.ok(swaggerJson);
	}

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
	 * @param file  the InputStream containing the text to compare
	 * @param type            the type of text (text, pdf, docx)
	 * @return id of the stored file
	 * @throws ParseException if there is an error parsing the input parameters
	 * @throws IOException    if there is an error reading the input stream
	 */
	@Operation(tags = {"analyzeText"}, description = "Analyzes a text and generates a PDF report")
	@ApiResponses({ 
		@ApiResponse(responseCode = "200" , description = "",content = {@Content(mediaType = "application/json")} ),
		@ApiResponse(responseCode = "500", description = "Response failed.") 
	})
	@PostMapping(value = "/{label1}", consumes = MediaType.MULTIPART_FORM_DATA, produces = MediaType.APPLICATION_JSON)
	public ResponseEntity<String> analyzeText(@PathVariable("label1") String label1,
			@FormDataParam("file") MultipartFile file,
			// @FormDataParam("file") FormDataContentDisposition textFileDetail, 
			@FormDataParam("type") String type,
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
		String encodedByteString = service.convertInputStreamToBase64(file.getInputStream());
		TmitocarText tmitoBody = new TmitocarText();
		tmitoBody.setTopic(topic);
		tmitoBody.setType(type);
		tmitoBody.setWordSpec(wordSpec);
		tmitoBody.setTemplate(template);
		tmitoBody.setText(encodedByteString);
		service.processSingleText(label1, topic, template, tmitoBody);

		byte[] bytes = Base64.getDecoder().decode(encodedByteString);
		String fname = file.getOriginalFilename();
		String fileEnding = "txt";
		int dotIndex = fname.lastIndexOf('.');
		if (dotIndex > 0 && dotIndex < fname.length() - 1) {
			fileEnding = fname.substring(dotIndex + 1);
		}
		ObjectId uploaded = service.storeFile(topic+"."+fileEnding, bytes);
		if (uploaded == null) {
			return ResponseEntity.badRequest().body("Could not store file " + fname);
		}
		// try {
		// 	textInputStream.close();
		// } catch (IOException e) {
		// 	e.printStackTrace();
		// }
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
	@GetMapping(value = "{fileId}/commonWords", produces = MediaType.APPLICATION_JSON)
	public ResponseEntity<String> getCommonWords(@PathVariable("fileId") String fileId) throws ParseException, IOException {
			
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

	/**
	 * Anaylze text
	 */
	@Operation(tags = {"label2Words"}, description = "Analyzes a text and generates a PDF report")
	@ApiResponses({ 
		@ApiResponse(responseCode = "200" , description = "Success.",content = {@Content(mediaType = "application/json")} ),
		@ApiResponse(responseCode = "500", description = "Response failed.") 
	})
	@GetMapping(value= "/{fileId}/label2Words", produces = MediaType.APPLICATION_JSON)
	public ResponseEntity<String> getLabel2Words(@PathVariable("fileId") String fileId) throws ParseException, IOException {
			
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
			gridFSBucket.downloadToStream(file.getObjectId(), baos);
			String jsonStr = baos.toString();
			JSONParser parser = new JSONParser(JSONParser.MODE_PERMISSIVE);

			// Parse the JSON string into a JSONObject
			JSONObject jsonObject = (JSONObject) parser.parse(jsonStr);
			String formattedMessage = "Übrigens gibt es noch folgende Begriffe, die im Expertentext genannt wurden, aber noch nicht in deinem Text auftauchen::\n";
			formattedMessage += "-------------------------\n";
			JSONArray bDiffArray = (JSONArray) jsonObject.get("BegriffeDiffB");
			formattedMessage += service.formatJSONArray(bDiffArray);
			formattedMessage += "Überleg nochmal, welche davon du sinnvoll in deinen Text einbauen kannst und möchtest.";
			JSONObject resBody = new JSONObject();
			resBody.put("formattedMessage", service.formatJSONArray(bDiffArray));
			return ResponseEntity.ok(resBody.toString());
		} catch (Exception e) {
			System.err.println(e);
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Something went wrong getting label2 words for analysis: "+fileId);
	}


	// @Tag(name = "Feedback Resource")
	// // @SwaggerDefinition(info = @Info(title = "Feedback Resource", version = "1.0.0", description = "This API is responsible for handling text documents in txt, pdf or docx format and sending them to T-MITOCAR for processing. The feedback is then saved in a MongoDB and the document IDs are returned.", termsOfService = "https://tech4comp.de/", contact = @Contact(name = "Alexander Tobias Neumann", url = "https://tech4comp.dbis.rwth-aachen.de/", email = "neumann@dbis.rwth-aachen.de"), license = @License(name = "ACIS License (BSD3)", url = "https://github.com/rwth-acis/las2peer-tmitocar-Service/blob/master/LICENSE")))
	// @RequestMapping("/feedback")
	// public static class Feedback {
	// 	// TmitocarServiceController service = (TmitocarServiceController) Context.get().getService();

	/**
	 * Anaylze text
	 *
	 * @param label1          the first label (user text)
	 * @param file the InputStream containing the text to compare
	 * @param type            the type of text (text, pdf or docx)
	 * @param topic           the topic of the text (e.g. BiWi 5)
	 * @param template        the template to use for the PDF report
	 * @param wordSpec        the word specification for the PDF report
	 * @return the id of the stored file
	 * @throws ParseException if there is an error parsing the input parameters
	 * @throws IOException    if there is an error reading the input stream
	 */
	@Operation(tags = {"analyzeText"}, description = "Analyzes a text and generates a PDF report")
	@ApiResponses({ 
		@ApiResponse(responseCode = "200" , description = "",content = {@Content(mediaType = "application/json")} ),
		@ApiResponse(responseCode = "500", description = "Response failed.") 
	})
	@PostMapping(value = "/{label}", consumes = MediaType.MULTIPART_FORM_DATA, produces = MediaType.APPLICATION_JSON)
	public ResponseEntity<String> analyzeText(@PathVariable("label") String label1,
			@FormDataParam("file") MultipartFile file,
			// @FormDataParam("file") FormDataContentDisposition textFileDetail,
			@FormDataParam("type") String type,
			@FormDataParam("topic") String topic, @FormDataParam("template") String template,
			@FormDataParam("wordSpec") String wordSpec,@FormDataParam("email") String email,@FormDataParam("courseId") int courseId,@FormDataParam("task") int task, @FormDataParam("sbfmURL") String sbfmURL) throws ParseException, IOException {
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
		String encodedByteString = service.convertInputStreamToBase64(file.getInputStream());
		TmitocarText tmitoBody = new TmitocarText();
		tmitoBody.setTopic(topic);
		tmitoBody.setType(type);
		tmitoBody.setWordSpec(wordSpec);
		tmitoBody.setTemplate(template);
		tmitoBody.setText(encodedByteString);
		service.processSingleText(label1, topic, template, tmitoBody);

		byte[] bytes = Base64.getDecoder().decode(encodedByteString);
		String fname = file.getOriginalFilename();

		String fileEnding = "txt";
		int dotIndex = fname.lastIndexOf('.');
		if (dotIndex > 0 && dotIndex < fname.length() - 1) {
			fileEnding = fname.substring(dotIndex + 1);
		}

		ObjectId uploaded = service.storeFile(topic+"."+fileEnding, bytes);
		if (uploaded == null) {
			return ResponseEntity.badRequest().body("Could not store file " + fname);
		}
		// try {
		// 	textInputStream.close();
		// } catch (IOException e) {
		// 	e.printStackTrace();
		// }
		TmitocarResponse response = new TmitocarResponse(uploaded.toString());
		String uuid = service.getUuidByEmail(email);
		if (uuid!=null){
			// user has accepted
			LrsCredentials lrsCredentials = service.getLrsCredentialsByCourse(courseId);
			if(lrsCredentials!=null){
				JSONObject xapi = service.prepareXapiStatement(uuid, "uploaded_task", topic, courseId, task, uploaded.toString(),null,null);
				String toEncode = lrsCredentials.getClientKey()+":"+lrsCredentials.getClientSecret();
				String encodedString = Base64.getEncoder().encodeToString(toEncode.getBytes());

				service.sendXAPIStatement(xapi, encodedString);
			}
		}
		Gson g = new Gson();
		return ResponseEntity.ok(g.toJson(response));
	}

	@Operation(tags = {"getAnalyzedText"}, description = "Returns analyzed text report (PDF)")
	@ApiResponses({ 
		@ApiResponse(responseCode = "200" , description = "Success.",content = {@Content(mediaType = "application/json")} ),
		@ApiResponse(responseCode = "500", description = "Response failed.") 
	})
	@GetMapping(value= "/{label1}", produces = MediaType.APPLICATION_JSON)
	public ResponseEntity<String> getAnalyzedText(@PathVariable("label1") String label1) throws ParseException, IOException {
		if (!service.isActive.containsKey(label1)) {
			JSONObject err = new JSONObject();
			err.put("errorMessage", "User: " + label1 + " not found.");
			err.put("error", true);
			return new ResponseEntity<String>(err.toJSONString(), HttpStatus.NOT_FOUND);
		}
		if (service.isActive.get(label1)) {
			JSONObject err = new JSONObject();
			err.put("errorMessage", "User: " + label1 + " currently busy.");
			err.put("error", true);
			return new ResponseEntity<String>(err.toJSONString(), HttpStatus.NOT_FOUND);
		}
		JSONObject err = new JSONObject();
		ObjectId fileId = null;
		if (service.userTexts.get(label1) != null) {
			System.out.println("Storing PDF to mongodb...");
			try {
				byte[] pdfByte = Files.readAllBytes(
						Paths.get("tmitocar/texts/" + label1 + "/" + label1 + "-modell" + ".pdf"));
				fileId = service.storeFile(label1 + "-feedback.pdf", pdfByte);
				Files.delete(Paths.get("tmitocar/texts/" + label1 + "/" + label1 + "-modell" + ".pdf"));
			} catch (Exception e) {
				e.printStackTrace();
				service.isActive.put(label1, false);
				System.out.println("Failed storing PDF.");
			}
		} else {
			err.put("errorMessage", "Something went wrong storing the feedback for " + label1);
			err.put("error", true);
			return ResponseEntity.badRequest().body(err.toJSONString());
		}

		if (fileId == null) {
			err.put("errorMessage", "Something went wrong storing the feedback for " + label1);
			err.put("error", true);
			return ResponseEntity.badRequest().body(err.toJSONString());
		}
		TmitocarResponse response = new TmitocarResponse(null, fileId.toString());
		Gson g = new Gson();
		return ResponseEntity.ok(g.toJson(response));
	}

	/**
	 * Compare text
	 *
	 * @param label1          the first label (user text)
	 * @param label2          the second label (expert or second user text)
	 * @param file the InputStream containing the text to compare
	 * @param type            the type of text (text, pdf or docx)
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
			@FormDataParam("file") MultipartFile file,
			// @FormDataParam("file") FormDataContentDisposition textFileDetail, 
			@FormDataParam("type") String type, @FormDataParam("template") String template,
			@FormDataParam("wordSpec") String wordSpec,@FormDataParam("email") String email,@FormDataParam("courseId") int courseId, @FormDataParam("sbfmURL") String sbfmURL) throws ParseException, IOException {
		if (service.isActive.getOrDefault(label1, false)) {
			JSONObject err = new JSONObject();
			err.put("errorMessage", "User: " + label1 + " currently busy.");
			err.put("error", true);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err.toJSONString());
		}
		
		File templatePath = new File("./tmitocar/templates/" + template);
		if (!templatePath.exists()){
			JSONObject err = new JSONObject();
			err.put("errorMessage", "Template: " + template + " not found.");
			err.put("error", true);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err.toJSONString());
		}
		
		service.isActive.put(label1, true);

		String topic = service.getTaskNameByIds(courseId, Integer.parseInt(label2));

		InputStream textInputStream = file.getInputStream();
		String encodedByteString = service.convertInputStreamToBase64(textInputStream);
		TmitocarText tmitoBody = new TmitocarText();
		tmitoBody.setTopic(topic);
		tmitoBody.setType(type);
		tmitoBody.setWordSpec(wordSpec);
		tmitoBody.setTemplate(template);
		tmitoBody.setText(encodedByteString);
		tmitoBody.setUuid(email);
		byte[] bytes = Base64.getDecoder().decode(encodedByteString);
		String fname = file.getOriginalFilename();
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
		String uuid = service.getUuidByEmail(email);
		if (uuid!=null){
			// user has accepted
			LrsCredentials lrsCredentials = service.getLrsCredentialsByCourse(courseId);
			if(lrsCredentials!=null){
				JSONObject xapi = service.prepareXapiStatement(uuid, "uploaded_task", topic, courseId, Integer.parseInt(label2), uploaded.toString(),null,null);
				String toEncode = lrsCredentials.getClientKey()+":"+lrsCredentials.getClientSecret();
				String encodedString = Base64.getEncoder().encodeToString(toEncode.getBytes());
				service.sendXAPIStatement(xapi, encodedString);
			}
		}
		Gson g = new Gson();
		return ResponseEntity.ok(g.toJson(response));
	}

	@Operation(tags = {"getComparedText"}, description =  "Returns compared text report (PDF)")
	@ApiResponses({ 
		@ApiResponse(responseCode = "200" , description = "",content = {@Content(mediaType = "application/json")} ),
		@ApiResponse(responseCode = "500", description = "Response failed.") 
	})
	@GetMapping(value = "/{label1}/compare/{label2}" , consumes = MediaType.MULTIPART_FORM_DATA, produces = MediaType.APPLICATION_JSON)
	public ResponseEntity<String> getComparedText(@PathVariable("label1") String label1, @PathVariable("label2") String label2)
			throws ParseException, IOException {
		if (!service.isActive.containsKey(label1)) {
			JSONObject err = new JSONObject();
			err.put("errorMessage", "User: " + label1 + " not found.");
			err.put("error", true);
			return new ResponseEntity<String>(err.toJSONString(), HttpStatus.NOT_FOUND);
		}
		if (service.isActive.get(label1)) {
			JSONObject err = new JSONObject();
			err.put("errorMessage", "User: " + label1 + " currently busy.");
			err.put("error", true);
			return new ResponseEntity<String>(err.toJSONString(), HttpStatus.NOT_FOUND);
		}
		JSONObject err = new JSONObject();
		ObjectId feedbackFileId = null;
		ObjectId graphFileId = null;
		if (service.userTexts.get(label1) != null) {
			System.out.println("Storing PDF to mongodb...");
			feedbackFileId = service.storeLocalFileRemote("comparison_" + label1 + "_vs_" + label2 + ".pdf");
			graphFileId = service.storeLocalFileRemote("comparison_" + label1 + "_vs_" + label2 + ".json");

		} else {
			err.put("errorMessage", "Something went wrong storing the feedback for " + label1);
			err.put("error", true);
			return ResponseEntity.badRequest().body(err.toJSONString());
		}

		if (feedbackFileId == null) {
			err.put("errorMessage", "Something went wrong storing the feedback for " + label1);
			err.put("error", true);
			return ResponseEntity.badRequest().body(err.toJSONString());
		}
		if (graphFileId == null) {
			err.put("errorMessage", "Something went wrong storing the graph for " + label1);
			err.put("error", true);
			return ResponseEntity.badRequest().body(err.toJSONString());
		}
		TmitocarResponse response = new TmitocarResponse(null, feedbackFileId.toString(), graphFileId.toString());
		Gson g = new Gson();
		return ResponseEntity.ok(g.toJson(response));
	}

	/**
	 * Compare text
	 *
	 * @param label1          the first label (user text)
	 * @param label2          the second label (expert or second user text)
	 * @param file the InputStream containing the text to compare
	 * @param type            the type of text (text, pdf or docx)
	 * @param template        the template to use for the PDF report
	 * @param wordSpec        the word specification for the PDF report
	 * @return the id of the stored file
	 * @throws ParseException if there is an error parsing the input parameters
	 * @throws IOException    if there is an error reading the input stream
	 */
	@Operation(tags = {"compareTextWithLLM"}, description = "Compares two texts and generates a PDF report")
	@ApiResponses({ 
		@ApiResponse(responseCode = "200" , description = "",content = {@Content(mediaType = "application/json")} ),
		@ApiResponse(responseCode = "500", description = "Response failed.") 
	})
	@PostMapping(value = "/{label1}/compareWithLLM/{label2}" , consumes = MediaType.MULTIPART_FORM_DATA, produces = MediaType.APPLICATION_JSON)
	public ResponseEntity<String> compareTextWithLLM(@PathVariable("label1") String label1, @PathVariable("label2") String label2,
			@FormDataParam("file") MultipartFile file,
			// @FormDataParam("file") FormDataContentDisposition textFileDetail, 
			@FormDataParam("type") String type, @FormDataParam("template") String template,
			@FormDataParam("wordSpec") String wordSpec,@FormDataParam("email") String email,@FormDataParam("courseId") int courseId, @FormDataParam("sbfmURL") String sbfmURL) throws ParseException, IOException {
		if (service.isActive.getOrDefault(label1, false)) {
			JSONObject err = new JSONObject();
			err.put("errorMessage", "User: " + label1 + " currently busy.");
			err.put("error", true);
			return new ResponseEntity<String>(err.toJSONString(), HttpStatus.NOT_FOUND);
		}
		
		File templatePath = new File("tmitocar/templates/" + template);
		if (!templatePath.exists()){
			JSONObject err = new JSONObject();
			err.put("errorMessage", "Template: " + template + " not found.");
			err.put("error", true);
			return new ResponseEntity<String>(err.toJSONString(), HttpStatus.NOT_FOUND);
		}
		
		service.isActive.put(label1, true);

		String topic = service.getTaskNameByIds(courseId, Integer.parseInt(label2));


		String encodedByteString = service.convertInputStreamToBase64(file.getInputStream());
		TmitocarText tmitoBody = new TmitocarText();
		tmitoBody.setTopic(topic);
		tmitoBody.setType(type);
		tmitoBody.setWordSpec(wordSpec);
		tmitoBody.setTemplate(template);
		tmitoBody.setText(encodedByteString);
		tmitoBody.setUuid(email);
		byte[] bytes = Base64.getDecoder().decode(encodedByteString);
		String fname = file.getOriginalFilename();
		String fileEnding = "txt";
		int dotIndex = fname.lastIndexOf('.');
		if (dotIndex > 0 && dotIndex < fname.length() - 1) {
			fileEnding = fname.substring(dotIndex + 1);
		}
		ObjectId uploaded = service.storeFile(topic +  "." + fileEnding, bytes);
		if (uploaded == null) {
			return ResponseEntity.badRequest().body("Could not store file " + fname);
		}
		// try {
		// 	textInputStream.close();
		// } catch (IOException e) {
		// 	e.printStackTrace();
		// }
		boolean comparing = service.llm_feedback(label1, courseId + "-"+ label2, template, tmitoBody, sbfmURL,uploaded.toString());

		if (!comparing) {
			service.isActive.put(label1, false);
			return ResponseEntity.badRequest().body("Something went wrong: " + label1 + ".");
		}

		
		TmitocarResponse response = new TmitocarResponse(uploaded.toString());
		response.setLabel1(label1);
		response.setLabel2(courseId + "-"+ label2);
		String uuid = service.getUuidByEmail(email);
		if (uuid!=null){
			// user has accepted
			LrsCredentials lrsCredentials = service.getLrsCredentialsByCourse(courseId);
			if(lrsCredentials!=null){
				JSONObject xapi = service.prepareXapiStatement(uuid, "uploaded_task", topic, courseId, Integer.parseInt(label2), uploaded.toString(),null,null);
				String toEncode = lrsCredentials.getClientKey()+":"+lrsCredentials.getClientSecret();
				String encodedString = Base64.getEncoder().encodeToString(toEncode.getBytes());
				service.sendXAPIStatement(xapi, encodedString);
			}
		}
		Gson g = new Gson();
		return ResponseEntity.ok(g.toJson(response));
	}

	@Operation(tags = {"getComparedTextWithLLM"}, description =  "Returns compared text report (PDF)")
	@ApiResponses({ 
		@ApiResponse(responseCode = "200" , description = "",content = {@Content(mediaType = "application/json")} ),
		@ApiResponse(responseCode = "500", description = "Response failed.") 
	})
	@GetMapping(value = "/{label1}/compareWithLLM/{label2}" , consumes = MediaType.MULTIPART_FORM_DATA, produces = MediaType.APPLICATION_JSON)
	public ResponseEntity<String> getComparedTextWithLLM(@PathVariable("label1") String label1, @PathVariable("label2") String label2)
			throws ParseException, IOException {
		if (!service.isActive.containsKey(label1)) {
			JSONObject err = new JSONObject();
			err.put("errorMessage", "User: " + label1 + " not found.");
			err.put("error", true);
			return new ResponseEntity<String>(err.toJSONString(), HttpStatus.NOT_FOUND);
		}
		if (service.isActive.get(label1)) {
			JSONObject err = new JSONObject();
			err.put("errorMessage", "User: " + label1 + " currently busy.");
			err.put("error", true);
			return new ResponseEntity<String>(err.toJSONString(), HttpStatus.NOT_FOUND);
		}
		JSONObject err = new JSONObject();
		ObjectId feedbackFileId = null;
		ObjectId graphFileId = null;
		if (service.userTexts.get(label1) != null) {
			System.out.println("Storing PDF to mongodb...");
			feedbackFileId = service.storeLocalFileRemote("comparison_" + label1 + "_vs_" + label2 + ".pdf");
			graphFileId = service.storeLocalFileRemote("comparison_" + label1 + "_vs_" + label2 + ".json");

		} else {
			err.put("errorMessage", "Something went wrong storing the feedback for " + label1);
			err.put("error", true);
			return ResponseEntity.badRequest().body(err.toJSONString());
		}

		if (feedbackFileId == null) {
			err.put("errorMessage", "Something went wrong storing the feedback for " + label1);
			err.put("error", true);
			return ResponseEntity.badRequest().body(err.toJSONString());
		}
		if (graphFileId == null) {
			err.put("errorMessage", "Something went wrong storing the graph for " + label1);
			err.put("error", true);
			return ResponseEntity.badRequest().body(err.toJSONString());
		}
		TmitocarResponse response = new TmitocarResponse(null, feedbackFileId.toString(), graphFileId.toString());
		Gson g = new Gson();
		return ResponseEntity.ok(g.toJson(response));
	}

	// @Api(value = "FAQ Resource")
	// @SwaggerDefinition(info = @Info(title = "FAQ Resource", version = "1.0.0", description = "Todo.", termsOfService = "https://tech4comp.de/", contact = @Contact(name = "Alexander Tobias Neumann", url = "https://tech4comp.dbis.rwth-aachen.de/", email = "neumann@dbis.rwth-aachen.de"), license = @License(name = "ACIS License (BSD3)", url = "https://github.com/rwth-acis/las2peer-tmitocar-Service/blob/master/LICENSE")))
	// @Path("/faq")
	// public static class FAQ {
	// 	// TmitocarServiceController service = (TmitocarServiceController) Context.get().getService();
		
	@Operation(tags = {"getAllQuestions"}, description = "Returns all questions")
	@ApiResponses({ 
		@ApiResponse(responseCode = "200" , description = "",content = {@Content(mediaType = "application/json")} ),
		@ApiResponse(responseCode = "500", description = "Response failed.") 
	})
	@GetMapping(value = "/getFAQ" , produces = MediaType.APPLICATION_JSON)
	public ResponseEntity<String> getFAQ(@RequestParam("courseId") int courseId) {
		List<CourseFAQ> courseFAQs = null;
		JSONArray jsonArray = new JSONArray();
		JSONArray interactiveElements = new JSONArray();
		String chatMessage = "";
		try {
			if (courseId == 0) {
				courseFAQs = service.getCourseFAQs();
			} else {
				courseFAQs = service.findFAQsByCourseId(courseId);
			}

			for(CourseFAQ faq : courseFAQs) {

				courseId = faq.getCourseId();
				String text = faq.getAnswer();
				String intent = faq.getIntent();

				JSONObject jsonObject = new JSONObject();
				jsonObject.put("courseId", courseId);
				jsonObject.put("text", text);
				jsonObject.put("intent", intent);

				jsonArray.add(jsonObject);
				jsonObject = new JSONObject();
				jsonObject.put("intent", intent);
				jsonObject.put("label", "Question "+ intent);
				jsonObject.put("isFile", false);

				interactiveElements.add(jsonObject);
				chatMessage += intent+": "+text+"\n<br>\n";
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

	@Operation(tags = {"getAllFAQbyIntent"}, description = "Returns all FAQ by intent")
	@ApiResponses({ 
		@ApiResponse(responseCode = "200" , description = "",content = {@Content(mediaType = "application/json")} ),
		@ApiResponse(responseCode = "500", description = "Response failed.") 
	})
	@GetMapping(value = "/{intent}", produces = MediaType.APPLICATION_JSON)
	public ResponseEntity<String> getFAQByIntent(@PathVariable("intent") String intent, @RequestParam("courseId") int courseId) {
		List<CourseFAQ> courseFAQs = null;
		String chatMessage = "";
		
		try {
			if (courseId == 0) {
				courseFAQs = service.findFAQsByIntent(intent);
			} else {
				courseFAQs = service.findFAQsByCourseIdAndIntent(courseId, intent);
			};

			for (CourseFAQ faq : courseFAQs) {
				String text = faq.getAnswer();
				//String title = rs.getString("title");

				chatMessage += text + "\n<br>";
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (chatMessage == "") {
			try{
				if (courseId == 0) {
					courseFAQs = service.findFAQsByIntent("default");
				} else {
					courseFAQs = service.findFAQsByCourseIdAndIntent(courseId, "default");
				}

				for (CourseFAQ faq : courseFAQs) {
					String text = faq.getAnswer();
					//String title = rs.getString("title");

					chatMessage += text + "\n<br>";
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}
		JSONObject response = new JSONObject();
		response.put("chatMessage", chatMessage);
		return  ResponseEntity.ok(response.toString());
	}

	// @Api(value = "Credits Resource")
	// @SwaggerDefinition(info = @Info(title = "Credits Resource", version = "1.0.0", description = "Todo.", termsOfService = "https://tech4comp.de/", contact = @Contact(name = "Alexander Tobias Neumann", url = "https://tech4comp.dbis.rwth-aachen.de/", email = "neumann@dbis.rwth-aachen.de"), license = @License(name = "ACIS License (BSD3)", url = "https://github.com/rwth-acis/las2peer-tmitocar-Service/blob/master/LICENSE")))
	// @Path("/credits")
	// public static class Credits {
	// 	// TmitocarServiceController service = (TmitocarServiceController) Context.get().getService();
	@Operation(tags = {"getCreditsOfUser"}, description = "Returns Credits of User by course")
	@ApiResponses({ 
		@ApiResponse(responseCode = "200" , description = "",content = {@Content(mediaType = "application/json")} ),
		@ApiResponse(responseCode = "500", description = "Response failed.") 
	})
	@GetMapping(value = "/getCreditsByUser", produces = MediaType.APPLICATION_JSON)
	public ResponseEntity<String> getCreditsByUser(@RequestParam("email") String email, @RequestParam("courseId") int courseId) {
		String user = service.getUuidByEmail(email);
		JSONObject jsonBody = new JSONObject();
		JSONParser p = new JSONParser(JSONParser.MODE_PERMISSIVE);
		if(courseId != 6 && courseId != 2 && courseId != 11){
			JSONObject error = new JSONObject();
			error.put("chateMessage","Keine Credits für deinen Kurs :).");
			return ResponseEntity.ok(error.toString());
		}
		try{
			JSONObject acc = (JSONObject) p.parse(new String("{'account': { 'name': '" + user
				+ "', 'homePage': '"+ service.xapiHomepage + "'}}"));
			
			LrsCredentials res = service.getLrsCredentialsByCourse(courseId);
			URL url = new URL(service.xapiUrl + "/data/xAPI/statements?agent=" + acc.toString());
			if(res==null){
				return ResponseEntity.ok("problem");
			}
			String toEncode = res.getClientKey()+":"+res.getClientSecret();
			String encodedString = Base64.getEncoder().encodeToString(toEncode.getBytes());
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
			conn.setRequestProperty("X-Experience-API-Version", "1.0.3");
			conn.setRequestProperty("Authorization", "Basic " + encodedString);
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
		
			jsonBody = (JSONObject) p.parse(response.toString());
			JSONArray statements = (JSONArray) jsonBody.get("statements");
			// Check statements with matching actor
			// 12 Values as 12 assignments right?
			int[] assignments = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
			for (Object index : statements) {
				JSONObject jsonIndex = (JSONObject) index;
				JSONObject actor = (JSONObject) jsonIndex.get("actor");
				JSONObject verb = (JSONObject) jsonIndex.get("verb");
				JSONObject account = (JSONObject) actor.get("account");
				if (account.get("name").toString().equals(user)
						|| account.get("name").toString().equals(user)) {
						System.out.println("name check passed");
					// JSONObject object = (JSONObject) jsonIndex.get("object");
					JSONObject context = (JSONObject) jsonIndex.get("context");
					// JSONObject definition = (JSONObject) object.get("definition");
					JSONObject extensions = (JSONObject) context.get("extensions");// assignmentNumber
					// check if its not a delete statement
					if (extensions.get(service.xapiUrl + "/definitions/mwb/extensions/context/activity_data") != null && verb.get("id").toString().contains("uploaded_task")) {
						JSONObject fileDetails = (JSONObject) extensions
								.get(service.xapiUrl + "/definitions/mwb/extensions/context/activity_data");
						if (fileDetails.get("taskNr") != null) {
							String assignmentName = fileDetails.get("taskNr").toString();
							// JSONObject name = (JSONObject) definition.get("name");
							// String assignmentName = name.getAsString("en-US");
							try {
								// int assignmentNumber = Integer.valueOf(assignmentName.split("t")[1]);
								int assignmentNumber = Integer.valueOf(assignmentName);
								assignments[assignmentNumber - 1]++;
							} catch (Exception e) {
								e.printStackTrace();
							}
							// System.out.println("Extracted actor is " + name.getAsString("en-US"));
						}
					}
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
					System.out.println("20");
					msg += "Schreibaufgabe " + number + ": " + String.valueOf(assignments[i]) + "<br>";
				}
				// How are the credits calculated?
				msg += "Das hei\u00DFt, du hast bisher *" + credits * 2 + "* Leistunsprozente gesammelt. ";
				jsonBody = new JSONObject();
				jsonBody.put("text", msg);
		} catch (Exception e){
			e.printStackTrace();
		}		
		return ResponseEntity.ok(jsonBody.toString());
	}
}

