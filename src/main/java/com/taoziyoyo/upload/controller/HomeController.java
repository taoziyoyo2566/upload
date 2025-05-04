package com.taoziyoyo.upload.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "redirect:/upload";
    }

    @GetMapping("/upload")
    public String showUploadForm() {
        return "upload";
    }

    @GetMapping("/files")
    public String redirectToFilesList() {
        return "redirect:/api/files/list";
    }
}