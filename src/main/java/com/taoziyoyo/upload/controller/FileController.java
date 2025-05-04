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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/api/files")
public class FileController {

    @Autowired
    private FileService fileService;

    @GetMapping
    public String getAllFiles(Model model) {
        List<FileInfo> files = fileService.getAllFiles();
        model.addAttribute("files", files);
        return "list";
    }

    @GetMapping("/list")
    public String getFilesList(Model model) {
        List<FileInfo> files = fileService.getAllFiles();
        model.addAttribute("files", files);
        return "list";
    }

    @PostMapping("/upload")
    @ResponseBody
    public Map<String, Object> uploadFile(@RequestParam("file") MultipartFile file,
                                          @RequestParam(value = "useCloudStorage", defaultValue = "false") boolean useCloudStorage) {
        Map<String, Object> response = new HashMap<>();

        try {
            FileInfo fileInfo = fileService.storeFile(file, useCloudStorage);

            response.put("status", "success");
            response.put("message", "File uploaded successfully");
            response.put("fileInfo", fileInfo);

            return response;
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return response;
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id, HttpServletRequest request) {
        Optional<FileInfo> fileInfoOpt = fileService.getFile(id);

        if (!fileInfoOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        FileInfo fileInfo = fileInfoOpt.get();
        Resource resource = fileService.loadFileAsResource(fileInfo);

        // Try to determine file's content type
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            // Logger: Could not determine file type.
        }

        // Fallback to the default content type if type could not be determined
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileInfo.getOriginalFilename() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public Map<String, Object> deleteFile(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();

        try {
            fileService.deleteFile(id);
            response.put("status", "success");
            response.put("message", "File deleted successfully");
            return response;
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return response;
        }
    }

    @PutMapping("/{id}")
    @ResponseBody
    public Map<String, Object> updateFile(@PathVariable Long id,
                                          @RequestParam("file") MultipartFile file,
                                          @RequestParam(value = "name", required = false) String name) {
        Map<String, Object> response = new HashMap<>();

        try {
            FileInfo fileInfo = fileService.updateFile(id, file, name);
            if (fileInfo != null) {
                response.put("status", "success");
                response.put("message", "File updated successfully");
                response.put("fileInfo", fileInfo);
            } else {
                response.put("status", "error");
                response.put("message", "File not found");
            }
            return response;
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return response;
        }
    }
}