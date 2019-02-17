package com.dandevere.learn.dynamicdb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MainController {
	
	@Autowired
	private BookRepository bookRepository;
	@Autowired
	private DynamicDataSource dynamicDataSource;
	
	@GetMapping("/")
	public String hello() {
		dynamicDataSource.getDataSources().cleanUp();
		return "Hello";
	}
	
	@GetMapping("/{product}/book")
	public String findByTitle(@RequestParam("title")String title) {
		try {
			Book book = bookRepository.findByTitle(title);
			if(book != null) {
				return book.toString();
			}
		} catch(Exception e) {
			System.out.println("error");
		}
		return "not found";
	}
	
	@PostMapping("/{product}/book")
	public String saveBook(@RequestBody Book book) {
		bookRepository.save(book);
		return book.toString();
	}
}
