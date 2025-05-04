package com.taoziyoyo.upload.controller;

import com.taoziyoyo.upload.model.FileInfo;
import com.taoziyoyo.upload.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/api/editor")
public class FileEditorController {

    @Autowired
    private FileService fileService;

    @GetMapping("/view/{id}")
    public String viewFile(@PathVariable Long id, Model model) {
        Optional<FileInfo> fileInfoOpt = fileService.getFile(id);

        if (!fileInfoOpt.isPresent()) {
            return "redirect:/list?error=File+not+found";
        }

        FileInfo fileInfo = fileInfoOpt.get();
        model.addAttribute("file", fileInfo);

        if (fileService.isTextFile(fileInfo)) {
            // For text files, show the editor
            String content = fileService.getFileContent(fileInfo);
            model.addAttribute("content", content);
            return "editor";
        } else if (fileService.isMediaFile(fileInfo)) {
            // For media files, show the player
            return "player";
        } else {
            // For other files, just provide download link
            return "redirect:/api/files/" + id;
        }
    }

    @PostMapping("/save/{id}")
    @ResponseBody
    public Map<String, Object> saveFile(@PathVariable Long id, @RequestParam("content") String content) {
        Map<String, Object> response = new HashMap<>();

        try {
            Optional<FileInfo> fileInfoOpt = fileService.getFile(id);

            if (!fileInfoOpt.isPresent()) {
                response.put("status", "error");
                response.put("message", "File not found");
                return response;
            }

            FileInfo fileInfo = fileInfoOpt.get();

            if (!fileService.isTextFile(fileInfo)) {
                response.put("status", "error");
                response.put("message", "Only text files can be edited");
                return response;
            }

            fileService.updateFileContent(fileInfo, content);

            response.put("status", "success");
            response.put("message", "File saved successfully");
            return response;
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return response;
        }
    }

    @GetMapping("/stream/{id}")
    public ResponseEntity<Resource> streamMedia(@PathVariable Long id) {
        Optional<FileInfo> fileInfoOpt = fileService.getFile(id);

        if (!fileInfoOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        FileInfo fileInfo = fileInfoOpt.get();

        if (!fileService.isMediaFile(fileInfo)) {
            return ResponseEntity.badRequest().build();
        }

        Resource resource = fileService.loadFileAsResource(fileInfo);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileInfo.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileInfo.getOriginalFilename() + "\"")
                .body(resource);
    }

    @GetMapping("/edit/{id}")
    public String editFile(@PathVariable Long id, Model model) {
        Optional<FileInfo> fileInfoOpt = fileService.getFile(id);
        if (!fileInfoOpt.isPresent()) {
            return "redirect:/files?error=File+not+found";
        }

        FileInfo fileInfo = fileInfoOpt.get();
        model.addAttribute("file", fileInfo);

        if (fileService.isTextFile(fileInfo)) {
            String content = fileService.getFileContent(fileInfo);
            model.addAttribute("content", content);
            return "editor";
        } else if (fileService.isMediaFile(fileInfo)) {
            return "player";
        } else {
            return "redirect:/download/" + id;
        }
    }
}