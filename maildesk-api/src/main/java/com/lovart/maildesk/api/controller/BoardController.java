package com.lovart.maildesk.api.controller;

import com.lovart.maildesk.application.dto.BoardSummaryDto;
import com.lovart.maildesk.application.board.BoardApplicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/board")
public class BoardController {

    private final BoardApplicationService board;

    public BoardController(BoardApplicationService board) {
        this.board = board;
    }

    @GetMapping
    public ResponseEntity<BoardSummaryDto> getBoard(
            @RequestParam(required = false, defaultValue = "all") String window
    ) {
        return ResponseEntity.ok(board.getBoard(window));
    }
}
