package com.manual.process.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.manual.process.service.ManualProcessService;

@RestController
public class ManualProcessController {

	@Autowired
	ManualProcessService manualProcessServiceImpl;
	
	@GetMapping(path = "/manualprocess")
	public void manualPostProcessBatch() {

		manualProcessServiceImpl.manualPclCreationProcess();
	}
}
