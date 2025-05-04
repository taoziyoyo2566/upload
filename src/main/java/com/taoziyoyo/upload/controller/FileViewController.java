package com.taoziyoyo.upload.controller;

import com.taoziyoyo.upload.service.FileService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class FileViewController {

    private final FileService fileService;

    private final FileController fileController;

    public FileViewController(FileService fileService, FileController fileController) {
        this.fileService = fileService;
        this.fileController = fileController;
    }

    @GetMapping("/edit/{id}")
    public String editFile(@PathVariable Long id, Model model) {
        return "forward:/api/editor/view/" + id;
    }

    @GetMapping("/play/{id}")
    public String playMedia(@PathVariable Long id, Model model) {
        return "forward:/api/editor/view/" + id;
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id, HttpServletRequest request) {
        return fileController.downloadFile(id, request);
    }
}