package io.learnk8s.knote;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;

@SpringBootApplication
@EnableConfigurationProperties(MinioProperties.class)
public class KnoteApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnoteApplication.class, args);
    }

}

interface NotesRepository extends MongoRepository<Note, String> {

}

@Document(collection = "notes")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
class Note {
    @Id
    private String id;
    private String description;

    @Override
    public String toString() {
        return description;
    }
}

@Controller
class KNoteController {

    private final NotesRepository notesRepository;
    private final MinioStorageService imageStorage;
    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer renderer = HtmlRenderer.builder().build();

    KNoteController(NotesRepository notesRepository, MinioStorageService imageStorage) {
        this.notesRepository = notesRepository;
        this.imageStorage = imageStorage;
    }


    @GetMapping("/")
    public String index(Model model) {
        getAllNotes(model);
        return "index";
    }

    @PostMapping("/note")
    public String saveNotes(@RequestParam("image") MultipartFile file,
                            @RequestParam String description,
                            @RequestParam(required = false) String publish,
                            @RequestParam(required = false) String upload,
                            Model model) throws Exception {

        if ("Publish".equals(publish)) {
            saveNote(description, model);
            getAllNotes(model);
            return "redirect:/";
        }
        if ("Upload".equals(upload)) {
            if (file != null && !file.isEmpty()) {
                uploadImage(file, description, model);
            }
            getAllNotes(model);
            return "index";
        }
        return "index";
    }

    @GetMapping("/img/{name}")
    public ResponseEntity<byte[]> getImageByName(@PathVariable String name) throws Exception {
        MinioStorageService.StoredImage image = imageStorage.get(name);
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (image.contentType() != null) {
            try {
                mediaType = MediaType.parseMediaType(image.contentType());
            } catch (IllegalArgumentException ignored) {
                // Serve unknown content types as an opaque file.
            }
        }
        return ResponseEntity.ok().contentType(mediaType).body(image.bytes());
    }


    private void getAllNotes(Model model) {
        List<Note> notes = notesRepository.findAll();
        Collections.reverse(notes);
        model.addAttribute("notes", notes);
    }

    private void uploadImage(MultipartFile file, String description, Model model) throws Exception {
        String fileId = imageStorage.save(file);
        model.addAttribute("description",
                description + " ![](/img/" + fileId + ")");
    }

    private void saveNote(String description, Model model) {
        if (description != null && !description.trim().isEmpty()) {
            //We need to translate markup to HTML
            Node document = parser.parse(description.trim());
            String html = renderer.render(document);
            notesRepository.save(new Note(null, html));
            //After publish you need to clean up the textarea
            model.addAttribute("description", "");
        }
    }

}
