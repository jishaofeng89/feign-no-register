package com.a360inhands.feign.controller;

import com.a360inhands.feign.api.IFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/file")
public class FileController {

    @Autowired
    private IFileService fileService;

    @RequestMapping("/list")
    public Object list() {
        return fileService.get();
    }
}
