package com.example.demo.controller;

import org.springframework.web.bind.annotation.RestController;

import com.example.demo.logger.AzureLoggerComponent;
import com.example.demo.logger.SampleLogFormat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@RestController
public class HelloController {

	@Autowired
	private AzureLoggerComponent azureLoggerComponent;

	@Autowired
	private ObjectMapper objMapper;

	@RequestMapping(method = RequestMethod.POST, path = "/", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String index(@RequestBody String jsonPaylod)
			throws JsonProcessingException {
		SampleLogFormat s = new SampleLogFormat("Muthu", jsonPaylod);
		azureLoggerComponent.pushLogsToAzure(objMapper.writeValueAsString(s));
		return "Greetings from Spring Boot!";
	}
}