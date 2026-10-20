package com.htx.app.pdftomd.controller;

import com.htx.app.pdftomd.service.ConversionService;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@Controller
public class ConversionController {
	
    private final ConversionService conversionService;

    public ConversionController(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping(value = "/convert", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String convert(@RequestParam("file") MultipartFile file, Model model) {
        if (file.isEmpty()) {
            model.addAttribute("error", "El archivo está vacío.");
            return "index";
        }

        try {
            String markdown = conversionService.convert(file);
            model.addAttribute("markdown", markdown);
            model.addAttribute("filename", file.getOriginalFilename());
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
        } catch (Exception e) {
            model.addAttribute("error",
                    "Error durante la conversión: " + e.getMessage() +
                    ". Verifique que el archivo no esté corrupto y que su formato sea el correcto.");
        }
        return "index";
    }

    @PostMapping("/download")
    public ResponseEntity<byte[]> download(@RequestParam String markdown,
                                           @RequestParam String filename) {
        String outName = filename.replaceAll("\\.[^.]+$", "") + ".md";
        byte[] bytes = markdown.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + outName + "\"")
                .contentType(MediaType.TEXT_MARKDOWN)
                .body(bytes);
    }

}
