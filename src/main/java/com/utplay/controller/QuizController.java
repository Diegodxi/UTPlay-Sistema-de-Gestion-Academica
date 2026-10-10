package com.utplay.controller;

import com.utplay.dto.QuizResponse;
import com.utplay.dto.ResolverQuizRequest;
import com.utplay.dto.ResolverQuizResponse;
import com.utplay.service.QuizService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/quiz")
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @GetMapping("/{id}")
    public QuizResponse obtener(@PathVariable Long id, Authentication authentication) {
        return quizService.obtener(id, authentication.getName());
    }

    @PostMapping("/resolver")
    public ResolverQuizResponse resolver(
            @Valid @RequestBody ResolverQuizRequest request,
            Authentication authentication
    ) {
        return quizService.resolver(request, authentication.getName());
    }
}
