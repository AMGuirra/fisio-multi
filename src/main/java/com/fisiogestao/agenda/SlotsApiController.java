package com.fisiogestao.agenda;

import com.fisiogestao.agenda.dto.SlotDisponivelDto;
import com.fisiogestao.agenda.service.AgendaService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/slots")
@RequiredArgsConstructor
public class SlotsApiController {

    private final AgendaService agendaService;

    @GetMapping
    public ResponseEntity<List<SlotDisponivelDto>> slots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data
    ) {
        return ResponseEntity.ok(agendaService.gerarSlotsDisponiveis(data));
    }
}
