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

    @GetMapping("/list")
    public String showFilesList(Model model) {
        return "redirect:/api/files/list";
    }
}